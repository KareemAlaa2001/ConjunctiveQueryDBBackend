package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.DatabaseCatalog;
import ed.inf.adbs.minibase.evaluator.Operator;
import ed.inf.adbs.minibase.evaluator.ProjectOperator;
import ed.inf.adbs.minibase.evaluator.ScanOperator;
import ed.inf.adbs.minibase.evaluator.SelectOperator;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory database system
 *
 */
public class Minibase {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("Usage: Minibase database_dir input_file output_file");
            return;
        }

        String databaseDir = args[0];
        String inputFile = args[1];
        String outputFile = args[2];


        evaluateCQ(databaseDir, inputFile, outputFile);

        parsingExample(inputFile);
    }

    public static void evaluateCQ(String databaseDir, String inputFile, String outputFile) {
        try {
            Query query = QueryParser.parse(Paths.get(inputFile));

            //  first implementing the construction of scn operators for each of the relations
            List<RelationalAtom> relationalAtoms = query.getBody().stream()
                    .filter(RelationalAtom.class::isInstance)
                    .map(RelationalAtom.class::cast).collect(Collectors.toList());

            DatabaseCatalog catalog = DatabaseCatalog.getCatalog();

            catalog.constructRelations(databaseDir);

            List<Operator> scanOperators = constructScans(relationalAtoms, catalog);

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



    private static List<Operator> constructScans(List<RelationalAtom> relationalAtoms, DatabaseCatalog catalog) {
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
//
//    private static void constructArtefacts() {
//
//    }

    /**
     * Example method for getting started with the parser.
     * Reads CQ from a file and prints it to screen, then extracts Head and Body
     * from the query and prints them to screen.
     */

    public static void parsingExample(String filename) {
        try {
            Query query = QueryParser.parse(Paths.get(filename));
//            Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w), z < w");
//            Query query = QueryParser.parse("Q(x, w) :- R(x, 'z'), S(4, z, w), 4 < 'test string' ");

            System.out.println("Entire query: " + query);
            RelationalAtom head = query.getHead();
            System.out.println("Head: " + head);
            List<Atom> body = query.getBody();
            System.out.println("Body: " + body);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

}
