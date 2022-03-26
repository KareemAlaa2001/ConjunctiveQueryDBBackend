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
     *  given an inputQuery (inputQuery), construct a query plan tree which utilises scan opoerators at the leaves,
     *  left-deep join tree structure
     *  and pushes selections down as far as possible
     *  no need to worry about pushing projections down.
     */
    public void constructQueryTree() {
        Query query = EvaluationUtils.extractConstantsToComparisonAtomsFromRelationalAtomBodies(inputQuery);

        List<RelationalAtom> relationalAtoms = query.getBody().stream()
                .filter(RelationalAtom.class::isInstance)
                .map(RelationalAtom.class::cast).collect(Collectors.toList());

        List<ScanOperator> scanOperators = constructScans(relationalAtoms); //  have now formed the leaves of the query tree

        List<ComparisonAtom> comparisonAtoms = query.getBody().stream()
                .filter(ComparisonAtom.class::isInstance)
                .map(ComparisonAtom.class::cast).collect(Collectors.toList());

        List<ComparisonAtom> nonJoinComparisonAtoms = comparisonAtoms.stream().filter(comparisonAtom -> isSingleAtomSelection(comparisonAtom, scanOperators) || canBeAppliedAtLeaf(comparisonAtom, scanOperators)).collect(Collectors.toList());

        Map<ScanOperator, List<ComparisonAtom>> singleAtomSelectionPredicates = new HashMap<>();

        nonJoinComparisonAtoms.forEach(comparisonAtom -> {
            Set<ScanOperator> relationalAtomSet = getRelevantScanOperators(comparisonAtom, scanOperators);
            relationalAtomSet.forEach(scanOperator -> {
                if ((singleAtomSelectionPredicates.containsKey(scanOperator))) {
                    singleAtomSelectionPredicates.get(scanOperator).add(comparisonAtom);
                } else {
                    singleAtomSelectionPredicates.put(scanOperator, new ArrayList<ComparisonAtom>() {{
                        add(comparisonAtom);
                    }});
                }
            });
        });

        List<Operator> childrenOfJoins = scanOperators.stream()
                .map(scanOperator -> (singleAtomSelectionPredicates.containsKey(scanOperator)) ?
                        new SelectOperator(scanOperator, scanOperator.getBaseRelationalAtom(), singleAtomSelectionPredicates.get(scanOperator))
                        : scanOperator)
                .collect(Collectors.toList());

        //  if this is a single atom query, then we want to just go straight to the projection
        if (childrenOfJoins.size() == 1) {
            Operator childOperator = childrenOfJoins.get(0);

            ProjectOperator projectOperator;

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

        //  next step is to construct a join tree using these. need to identify where join conditions lie and organise them accordingly
        //  can assume that maybe the variables with the same name in the expression have been remapped and extracted to relevant copies? nah that's dumb.

        List<ComparisonAtom> joinConditions = comparisonAtoms.stream().filter(comparisonAtom -> !isSingleAtomSelection(comparisonAtom, scanOperators)).collect(Collectors.toList());

        //  next step is to construct a join tree using childrenOfJoins
        //  we know here that there are at least 2 children in the list.

        Operator firstOperator = childrenOfJoins.remove(0);
        Operator secondOperator = childrenOfJoins.remove(0);

        Map<RelationalAtom, List<ComparisonAtom>> joinConditionMap = joinConditions.stream().collect(Collectors.groupingBy(comparisonAtom -> getLastRelationalAtomUtilisingComparison(comparisonAtom, scanOperators)));

        List<RelationalAtom> leftRelationalAtoms = new ArrayList<RelationalAtom>() {{
            add(getRelationalAtomFromSelectOrScanOperator(firstOperator));
        }};

        RelationalAtom rightRelationalAtom =  getRelationalAtomFromSelectOrScanOperator(secondOperator);


        JoinOperator currentLeftChild = new JoinOperator(firstOperator, secondOperator, new ArrayList<>(leftRelationalAtoms), rightRelationalAtom, joinConditionMap.getOrDefault(rightRelationalAtom, new ArrayList<>()));

        leftRelationalAtoms.add(rightRelationalAtom);

        while (!childrenOfJoins.isEmpty()) {
            Operator rightChild = childrenOfJoins.remove(0);

            rightRelationalAtom = getRelationalAtomFromSelectOrScanOperator(rightChild);

//            System.out.println("Do we get here before the exception call?");

            currentLeftChild = new JoinOperator(currentLeftChild, rightChild, new ArrayList<>(leftRelationalAtoms), rightRelationalAtom, joinConditionMap.getOrDefault(rightRelationalAtom, new ArrayList<>()));

            //            System.out.println("added join operator" + currentLeftChild);
            leftRelationalAtoms.add(rightRelationalAtom);
        }

        this.root = new ProjectOperator(currentLeftChild,
                query.getHead()
                .getTerms()
                .stream()
                .map(Variable.class::cast)
                .collect(Collectors.toList()), leftRelationalAtoms);

    }

    private static RelationalAtom getRelationalAtomFromSelectOrScanOperator(Operator selectOrScan) {
        if (selectOrScan instanceof SelectOperator) {
            return ((SelectOperator) selectOrScan).getBaseRelationalAtom();
        } else if (selectOrScan instanceof ScanOperator) {
            return ((ScanOperator) selectOrScan).getBaseRelationalAtom();
        } else throw new IllegalArgumentException("Invalid operator type passed in here!!");
    }

    public static Set<ScanOperator> getRelevantScanOperators(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        if (!isSingleAtomSelection(comparisonAtom, scanOperators)) throw new IllegalArgumentException("This method is only callable for comparison atoms which are not join conditions!");

        return scanOperators.stream()
                .filter(scanOperator ->
                        (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1()) && comparisonAtom.getTerm1() instanceof Variable)
                                ||
                                (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2()) && comparisonAtom.getTerm2() instanceof Variable))
                .collect(Collectors.toSet());
    }

//    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
//        try {
//            Query baseQuery = QueryParser.parse(Paths.get(inputFile));
//
//            Query query = EvaluationUtils.extractConstantsToComparisonAtomsFromRelationalAtomBodies(baseQuery);
//
//            //  first implementing the construction of scn operators for each of the relations
//            List<RelationalAtom> relationalAtoms = query.getBody().stream()
//                    .filter(RelationalAtom.class::isInstance)
//                    .map(RelationalAtom.class::cast).collect(Collectors.toList());
//
//            if (relationalAtoms.size() > 1) throw new UnsupportedOperationException("No support for embedded joins in this function!");
//
//            DatabaseCatalog catalog = DatabaseCatalog.getCatalog();
//
//            catalog.constructRelations(databaseDir);
//
//            List<ScanOperator> scanOperators = constructScans(relationalAtoms);
//
//            List<ComparisonAtom> comparisonAtoms = query.getBody().stream()
//                    .filter(ComparisonAtom.class::isInstance)
//                    .map(ComparisonAtom.class::cast).collect(Collectors.toList());
//
//
//            // starting with a naive select-scan which sets up a scan for each relationalAtom, then sets a selection operator as a parent for each, calling the selection dump
//            List<SelectOperator> selectOperators = scanOperators.stream()
//                    .map(ScanOperator.class::cast)
//                    .map(scanOperator -> new SelectOperator(scanOperator, scanOperator.getBaseRelationalAtom(), comparisonAtoms.stream()
//                            .filter(comparisonAtom -> SelectOperator.relationalAtomContainsPredicateVariables(scanOperator.getBaseRelationalAtom(),comparisonAtom))
//                            .collect(Collectors.toList())))
//                    .collect(Collectors.toList());
//
//            List<ProjectOperator> projectOperators = selectOperators.stream()
//                    .map(selectOperator -> new ProjectOperator(selectOperator, query.getHead().getTerms().stream()
//                            .map(Variable.class::cast)
//                            .collect(Collectors.toList())
//                            , selectOperator.getBaseRelationalAtom()))
//                    .collect(Collectors.toList());
//
//            projectOperators.forEach(ProjectOperator::dump);
//
//
//
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//    }

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

    //  can use this when organising join conditions since the one with that atom as the right atom
    private static RelationalAtom getLastRelationalAtomUtilisingComparison(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        if (comparisonAtom.getTerm1() instanceof Constant || comparisonAtom.getTerm2() instanceof Constant) throw new IllegalArgumentException("Can only call this function on a comparison atom with two variables!");
        if (isSingleAtomSelection(comparisonAtom, scanOperators)) throw new UnsupportedOperationException("This function is only relevant for the case where the comparison atom is relevant across multiple relational atoms!");
        List<ScanOperator> reversedCopy = new ArrayList<>(scanOperators);
        Collections.reverse(reversedCopy);

        Optional<ScanOperator> lastWithEitherVariable = reversedCopy.stream().filter(scanOperator -> (scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1()) || scanOperator.getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2()))).findFirst();

        if (!lastWithEitherVariable.isPresent()) throw new IllegalArgumentException("The received list of scan operators doesn't contain any of the variables in the comparison atom!");

        return  lastWithEitherVariable.get().getBaseRelationalAtom();
    }

    private static int getNumVariablesInComparisonAtom(ComparisonAtom comparisonAtom) {
        int result = 0;
        if (comparisonAtom.getTerm1() instanceof Variable) result++;
        if (comparisonAtom.getTerm2() instanceof Variable) result++;
        return result;
    }

    private static boolean relationalAtomHasOnlyOneTerm(RelationalAtom relationalAtom, Term term1, Term term2) {
        return (relationalAtom.getTerms().contains(term1) ^ relationalAtom.getTerms().contains(term2));
    }

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
