package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.DatabaseCatalog;
import ed.inf.adbs.minibase.evaluator.*;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

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

        try {
            DatabaseCatalog catalog = DatabaseCatalog.getCatalog();
            catalog.constructRelations(databaseDir);

            Query baseQuery = QueryParser.parse(Paths.get(inputFile));

            QueryPlanner planner = new QueryPlanner(baseQuery);

//            QueryPlanner.evaluateCQ(databaseDir, inputFile, outputFile);
            OutputWriter.initialiseOutputWriter(outputFile);

            planner.getRoot().dump();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


    }




    /**
     * Example method for getting started with the parser.
     * Reads CQ from a file and prints it to screen, then extracts Head and Body
     * from the query and prints them to screen.
     */

    public static void parsingExample(String filename) {
        try {
            Query query = QueryParser.parse(Paths.get(filename));

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
