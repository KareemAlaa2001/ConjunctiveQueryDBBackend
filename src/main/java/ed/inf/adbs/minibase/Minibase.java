package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.DatabaseCatalog;
import ed.inf.adbs.minibase.evaluator.Operator;
import ed.inf.adbs.minibase.evaluator.ScanOperator;
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


//            scanOperators.forEach(Operator::dump);


            //  TODO
            //  first generate relevant artefacts from the databaseDir (so that the DB class representations are created)
            //      the above will also involve parsing the database schema
            //  then evaluate the query by constructing the tree and making relative calls as they come in...

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }



    private static List<Operator> constructScans(List<RelationalAtom> relationalAtoms, DatabaseCatalog catalog) {
        return relationalAtoms.stream()
                .map(relationalAtom -> {
                    try {
                        return new ScanOperator(catalog.getRelationMap().get(relationalAtom.getName()).getFileLocation(),catalog.getRelationMap().get(relationalAtom.getName()).getSchema());
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
