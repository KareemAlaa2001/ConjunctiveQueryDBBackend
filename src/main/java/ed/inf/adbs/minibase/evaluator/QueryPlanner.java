package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.DatabaseCatalog;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class QueryPlanner {
    Operator root;
    Query inputQuery;

    public QueryPlanner(Query inputQuery) {
        this.inputQuery = inputQuery;
        constructQueryTree();
    }

    /**
     *  given an inputQuery (inputQuery), construct a query plan tree which utilises scan operators at the leaves,
     *  Uses a left-deep join tree structure and pushes selections down as far as possible.
     *  Uses projection as the root of the tree.
     *
     *  Sets the QueryPlanner's root to be the relevant operator in the end.
     */
    public void constructQueryTree() {
        //  first extracts the constants embedded in relational atoms in the body to their own comparison atoms, then continues with that processed query
        Query query = EvaluationUtils.extractConstantsToComparisonAtomsFromRelationalAtomBodies(inputQuery);

        // getting the list of the relational atoms in the query, which are utilised for a variety of operators
        List<RelationalAtom> relationalAtoms = query.getBody().stream()
                .filter(RelationalAtom.class::isInstance)
                .map(RelationalAtom.class::cast).collect(Collectors.toList());

        //  have now formed the leaves of the query tree
        List<ScanOperator> scanOperators = constructScans(relationalAtoms);

        //  collecting the list of comparison atoms in a similar vein
        List<ComparisonAtom> comparisonAtoms = query.getBody().stream()
                .filter(ComparisonAtom.class::isInstance)
                .map(ComparisonAtom.class::cast).collect(Collectors.toList());

        //  filtering out the comparison atoms that could be applied right above the scan operators in the leaf of the tree by checking whether they can be applied at a leaf
        List<ComparisonAtom> nonJoinComparisonAtoms = comparisonAtoms.stream().filter(comparisonAtom -> isSingleAtomSelection(comparisonAtom, scanOperators) || canBeAppliedAtLeaf(comparisonAtom, scanOperators)).collect(Collectors.toList());

        //  creating a map mapping scan operators to their respective relevant comparison atoms that will be used as leaf level selection predicates
        Map<ScanOperator, List<ComparisonAtom>> singleAtomSelectionPredicates = new HashMap<>();

        nonJoinComparisonAtoms.forEach(comparisonAtom -> {
            Set<ScanOperator> scanOperatorSet = getRelevantScanOperators(comparisonAtom, scanOperators);
            scanOperatorSet.forEach(scanOperator -> {
                if ((singleAtomSelectionPredicates.containsKey(scanOperator))) {
                    singleAtomSelectionPredicates.get(scanOperator).add(comparisonAtom);
                } else {
                    singleAtomSelectionPredicates.put(scanOperator, new ArrayList<ComparisonAtom>() {{
                        add(comparisonAtom);
                    }});
                }
            });
        });

        //  adding selectOperator parents to the scan operators that appear in the above map, passing in the respective comparison atoms as predicates
        List<Operator> childrenOfJoins = scanOperators.stream()
                .map(scanOperator -> (singleAtomSelectionPredicates.containsKey(scanOperator)) ?
                        new SelectOperator(scanOperator, scanOperator.getBaseRelationalAtom(), singleAtomSelectionPredicates.get(scanOperator))
                        : scanOperator)
                .collect(Collectors.toList());

        //  if this is a single atom query, then we want to just go straight to the projection
        if (childrenOfJoins.size() == 1) {
            Operator childOperator = childrenOfJoins.get(0);

            RelationalAtom relationalAtom = getRelationalAtomFromSelectOrScanOperator(childOperator);

            this.root =  new ProjectOperator(childOperator, query
                    .getHead()
                    .getTerms()
                    .stream()
                    .map(Variable.class::cast)
                    .collect(Collectors.toList())
                    , relationalAtom);

            return;
        }

        //  getting the list of comparison atoms that contain conditions applying over multiple relational atoms
        List<ComparisonAtom> joinConditions = comparisonAtoms.stream().filter(comparisonAtom -> !isSingleAtomSelection(comparisonAtom, scanOperators)).collect(Collectors.toList());

        //  next step is to construct a join tree using childrenOfJoins
        //  we know here that there are at least 2 children in the list.
        //  here we remove the operators from the list of children of joins since we use that list to track "remaining" operators to contsruct joins over
        Operator firstOperator = childrenOfJoins.remove(0);
        Operator secondOperator = childrenOfJoins.remove(0);

        //  generating a map of relationalAtom -> list of comparison atoms that use this relational atom as the "last" relevant atom from the list of relational atoms.
        //  This is necessary in order to figure out which join each comparison atom belongs to.
        Map<RelationalAtom, List<ComparisonAtom>> joinConditionMap = joinConditions.stream().collect(Collectors.groupingBy(comparisonAtom -> getLastRelationalAtomUtilisingComparison(comparisonAtom, scanOperators)));

        //  initialises the list tracking the "current" left child relational atoms with the relational atom from the first operator.
        List<RelationalAtom> leftRelationalAtoms = new ArrayList<RelationalAtom>() {{
            add(getRelationalAtomFromSelectOrScanOperator(firstOperator));
        }};

        //  gets the right relational atom for the first join
        RelationalAtom rightRelationalAtom =  getRelationalAtomFromSelectOrScanOperator(secondOperator);

        //  initialising the bottom left-most join in the tree with the current leftrelational atoms, right relational atom, leftChild and rightChild operators
        JoinOperator currentLeftChild = new JoinOperator(firstOperator, secondOperator, new ArrayList<>(leftRelationalAtoms), rightRelationalAtom, joinConditionMap.getOrDefault(rightRelationalAtom, new ArrayList<>()));

        //  adding the relational atom from the second operator to the current left relationalatoms for use in the next join.
        leftRelationalAtoms.add(rightRelationalAtom);

        //  iteratively constructing join operators using the respective first remaining operator in childrenOfJoins as the rightChild operator until the list is exhausted.
        //  the current left child join operator is used as the left child operator of the next join to be constructed, which in turn becomes the current left child
        while (!childrenOfJoins.isEmpty()) {
            Operator rightChild = childrenOfJoins.remove(0);

            rightRelationalAtom = getRelationalAtomFromSelectOrScanOperator(rightChild);

            currentLeftChild = new JoinOperator(currentLeftChild, rightChild, new ArrayList<>(leftRelationalAtoms), rightRelationalAtom, joinConditionMap.getOrDefault(rightRelationalAtom, new ArrayList<>()));

            leftRelationalAtoms.add(rightRelationalAtom);
        }

        //  having built the join tree, this constructs the projection operator that forms the root of the query plan.
        //  note that projection operators are designed to accept cases where both there is a projection to be done and when the tuples are passed through unaffected.
        this.root = new ProjectOperator(currentLeftChild,
                query.getHead()
                .getTerms()
                .stream()
                .map(Variable.class::cast)
                .collect(Collectors.toList()), leftRelationalAtoms);

    }

    //  given an operator that should be either a select or scan operator, this returns the base relational atom that this operator works over in the query.
    private static RelationalAtom getRelationalAtomFromSelectOrScanOperator(Operator selectOrScan) {
        if (selectOrScan instanceof SelectOperator) {
            return ((SelectOperator) selectOrScan).getBaseRelationalAtom();
        } else if (selectOrScan instanceof ScanOperator) {
            return ((ScanOperator) selectOrScan).getBaseRelationalAtom();
        } else throw new IllegalArgumentException("Invalid operator type passed in here!!");
    }

    //  gets a set of scan operators containing any of the variables in the comparison atom
    public static Set<ScanOperator> getRelevantScanOperators(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        if (!isSingleAtomSelection(comparisonAtom, scanOperators)) throw new IllegalArgumentException("This method is only callable for comparison atoms which are not join conditions!");

        return scanOperators.stream()
                .filter(scanOperator ->
                        (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1()) && comparisonAtom.getTerm1() instanceof Variable)
                                ||
                                (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2()) && comparisonAtom.getTerm2() instanceof Variable))
                .collect(Collectors.toSet());
    }

    //  uses the database catalog to construct a list of scan operators over the passed in relational atoms
    private static List<ScanOperator> constructScans(List<RelationalAtom> relationalAtoms) {
        DatabaseCatalog catalog = DatabaseCatalog.getCatalog();

        return relationalAtoms.stream()
                .map(relationalAtom -> {
                    try {
                        return new ScanOperator(catalog.getRelationMap().get(relationalAtom.getName()).getFileLocation(),catalog.getRelationMap().get(relationalAtom.getName()).getSchema(), relationalAtom);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    //  checks if a given comparison atom is a "single atom selection", as in it doesn't make a check that spans across multiple relational atoms
    public static boolean isSingleAtomSelectionInRelationalAtoms(ComparisonAtom comparisonAtom, List<RelationalAtom> relationalAtoms) {
        int numVariables = getNumVariablesInComparisonAtom(comparisonAtom);
        if (numVariables == 0 || numVariables == 1) return true;

        else {
            return relationalAtoms.stream().noneMatch(relationalAtom -> relationalAtomHasOnlyOneTerm(relationalAtom, comparisonAtom.getTerm1(), comparisonAtom.getTerm2()));
        }
    }

    //  this can be used to identify whether a comparison atom found in the body of the query can be pushed down right above the leaf of the tree, or if it needs to be used as a join condition
    private static boolean isSingleAtomSelection(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        int numVariables = getNumVariablesInComparisonAtom(comparisonAtom);
        if (numVariables == 0 || numVariables == 1) return true;

        else {
            return scanOperators.stream().noneMatch(scanOperator -> relationalAtomHasOnlyOneTerm(scanOperator.getBaseRelationalAtom(), comparisonAtom.getTerm1(), comparisonAtom.getTerm2()));
        }
    }

    //  can use this when organising join conditions since the one with that atom as the right atom will be the relevant join for the given comparisonAtom
    private static RelationalAtom getLastRelationalAtomUtilisingComparison(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        if (comparisonAtom.getTerm1() instanceof Constant || comparisonAtom.getTerm2() instanceof Constant) throw new IllegalArgumentException("Can only call this function on a comparison atom with two variables!");
        if (isSingleAtomSelection(comparisonAtom, scanOperators)) throw new UnsupportedOperationException("This function is only relevant for the case where the comparison atom is relevant across multiple relational atoms!");
        List<ScanOperator> reversedCopy = new ArrayList<>(scanOperators);
        Collections.reverse(reversedCopy);

        Optional<ScanOperator> lastWithEitherVariable = reversedCopy.stream().filter(scanOperator -> (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1()) || scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2()))).findFirst();

        if (!lastWithEitherVariable.isPresent()) throw new IllegalArgumentException("The received list of scan operators doesn't contain any of the variables in the comparison atom!");

        return  lastWithEitherVariable.get().getBaseRelationalAtom();
    }

    //  quick helper to get the number of variables in a given comparison atom
    private static int getNumVariablesInComparisonAtom(ComparisonAtom comparisonAtom) {
        int result = 0;
        if (comparisonAtom.getTerm1() instanceof Variable) result++;
        if (comparisonAtom.getTerm2() instanceof Variable) result++;
        return result;
    }

    //  helper to check that a relational atom contains only one of the given terms, relevant for checking that a comparison operator is a single atom comparison
    private static boolean relationalAtomHasOnlyOneTerm(RelationalAtom relationalAtom, Term term1, Term term2) {
        return (relationalAtom.getTerms().contains(term1) ^ relationalAtom.getTerms().contains(term2));
    }

    //  checks if a comparison atom can be applied right at the leaf of the query plan tree
    private static boolean canBeAppliedAtLeaf(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        int numVariables = getNumVariablesInComparisonAtom(comparisonAtom);
        if (numVariables == 0 || numVariables == 1) return true;

        else
            return scanOperators.stream().anyMatch(scanOperator -> scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1()) && scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2()));
    }

    public Operator getRoot() {
        return root;
    }

    public void setRoot(Operator root) {
        this.root = root;
    }

    public Query getInputQuery() {
        return inputQuery;
    }

    public void setInputQuery(Query inputQuery) {
        this.inputQuery = inputQuery;
    }
}
