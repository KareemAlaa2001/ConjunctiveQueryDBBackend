package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.DatabaseCatalog;
import ed.inf.adbs.minibase.parser.QueryParser;
import org.graalvm.compiler.api.replacements.Snippet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueryPlanner {
    Operator root;
    Query inputQuery;

    public QueryPlanner(Query inputQuery) {
        this.inputQuery = inputQuery;
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

        List<Operator> scanOperators = constructScans(relationalAtoms); //  have now formed the leaves of the query tree

        Map<Operator, List<List<Operator>>> selectionOperatorChildren = query.getBody().stream()
                .filter(ComparisonAtom.class::isInstance)
                .map(ComparisonAtom.class::cast)
                .map(comparisonAtom -> (comparisonAtom, getRelevantScanOperators(comparisonAtom, scanOperators)))
    }


    //  TODO refactor this to construct a tree with one root operator (projection if present) and form the selection positions for the joins
    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
        try {
            Query baseQuery = QueryParser.parse(Paths.get(inputFile));

            Query query = EvaluationUtils.extractConstantsToComparisonAtomsFromRelationalAtomBodies(baseQuery);

            //  first implementing the construction of scn operators for each of the relations
            List<RelationalAtom> relationalAtoms = query.getBody().stream()
                    .filter(RelationalAtom.class::isInstance)
                    .map(RelationalAtom.class::cast).collect(Collectors.toList());

            if (relationalAtoms.size() > 1) throw new UnsupportedOperationException("No support for embedded joins in this function!");

            DatabaseCatalog catalog = DatabaseCatalog.getCatalog();

            catalog.constructRelations(databaseDir);

            List<Operator> scanOperators = constructScans(relationalAtoms);

            List<ComparisonAtom> comparisonAtoms = query.getBody().stream()
                    .filter(ComparisonAtom.class::isInstance)
                    .map(ComparisonAtom.class::cast).collect(Collectors.toList());


            // starting with a naive select-scan which sets up a scan for each relationalAtom, then sets a selection operator as a parent for each, calling the selection dump
            List<SelectOperator> selectOperators = scanOperators.stream()
                    .map(ScanOperator.class::cast)
                    .map(scanOperator -> new SelectOperator(scanOperator, scanOperator.getBaseRelationalAtom(), comparisonAtoms.stream()
                            .filter(comparisonAtom -> SelectOperator.relationalAtomContainsPredicateVariables(scanOperator.getBaseRelationalAtom(),comparisonAtom))
                            .collect(Collectors.toList())))
                    .collect(Collectors.toList());

            List<ProjectOperator> projectOperators = selectOperators.stream()
                    .map(selectOperator -> new ProjectOperator(selectOperator, query.getHead().getTerms().stream()
                            .map(Variable.class::cast)
                            .collect(Collectors.toList())
                            , selectOperator.getRelationalAtom()))
                    .collect(Collectors.toList());

            projectOperators.forEach(ProjectOperator::dump);



        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static List<Operator> constructScans(List<RelationalAtom> relationalAtoms) {
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

    private static boolean isSingleAtomSelection(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        int numVariables = getNumVariablesInComparisonAtom(comparisonAtom);
        if (numVariables == 0 || numVariables == 1) return true;

        else {
            return scanOperators.stream().anyMatch(scanOperator -> relationalAtomHasOnlyOneTerm(scanOperator.getBaseRelationalAtom(), comparisonAtom.getTerm1(), comparisonAtom.getTerm2()));
        }
    }

    private static List<List<Operator>> getRelevantScanOperators(ComparisonAtom comparisonAtom, List<ScanOperator> scanOperators) {
        int numVariables = getNumVariablesInComparisonAtom(comparisonAtom);
        if (numVariables == 0) return new ArrayList<>();
        else if (numVariables == 1) {
            Variable comparisonAtomVariable = (comparisonAtom.getTerm1() instanceof Variable) ? (Variable) comparisonAtom.getTerm1(): (Variable) comparisonAtom.getTerm2();
            return scanOperators.stream()
                    .filter(operator -> operator
                            .getBaseRelationalAtom()
                            .getTerms()
                            .contains(comparisonAtomVariable))
                    .map(scanOperator -> new ArrayList<Operator>() {{
                        add(scanOperator);
                    }})
                    .collect(Collectors.toList());
        } else if (numVariables == 2) {
            Set<Integer> firstVarScanOperatorIndices = IntStream.range(0, scanOperators.size()).filter(i -> scanOperators.get(i).getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm1())).boxed().collect(Collectors.toSet());

            Set<Integer> secondVarScanOperatorIndices = IntStream.range(0, scanOperators.size()).filter(i -> scanOperators.get(i).getBaseRelationalAtom().getTerms().contains(comparisonAtom.getTerm2())).boxed().collect(Collectors.toSet());

            List<List<Operator>> relevantOperatorRanges = new ArrayList<>();


        }
        else throw new IllegalArgumentException("Somehow more than 2 variables were extracted from a single comparisonAtom?!");
    }

    private static int getNumVariablesInComparisonAtom(ComparisonAtom comparisonAtom) {
        int result = 0;
        if (comparisonAtom.getTerm1() instanceof Variable) result++;
        if (comparisonAtom.getTerm2() instanceof Variable) result++;
        return result;
    }

    private static boolean relationalAtomHasOnlyOneTerm(RelationalAtom relationalAtom, Term term1, Term term2) {
        return (relationalAtom.getTerms().contains(term1) && !relationalAtom.getTerms().contains(term2)) || (!relationalAtom.getTerms().contains(term1) && relationalAtom.getTerms().contains(term2));
    }
}
