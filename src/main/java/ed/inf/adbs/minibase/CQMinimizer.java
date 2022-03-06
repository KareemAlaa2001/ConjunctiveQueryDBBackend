package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.Atom;
import ed.inf.adbs.minibase.base.Query;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.parser.QueryParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static ed.inf.adbs.minibase.Utils.validQueryHomomorphismFound;

/**
 *
 * Minimization of conjunctive queries
 *
 */
public class CQMinimizer {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.err.println("Usage: CQMinimizer input_file output_file");
            return;
        }

        String inputFile = args[0];
        String outputFile = args[1];

        minimizeCQ(inputFile, outputFile);
        //  TODO investigate why this doesnt work for example 3...
//        parsingExample(inputFile);
    }

    /**
     * CQ minimization procedure
     *
     * Assume the body of the query from inputFile has no comparison atoms
     * but could potentially have constants in its relational atoms.
     *
     */
    public static void minimizeCQ(String inputFile, String outputFile) {
        try {
            Query query = QueryParser.parse(Paths.get(inputFile));

            System.out.println("Input query: " + query);

            executeMinimizeCQBody(query);

            System.out.println("Output query: " + query);
            outputQueryToFile(query, outputFile);


        } catch (Exception e) {
            System.err.println("Exception occurred during parsing");
            e.printStackTrace();
        }
    }

    /**
     * Minimises the given input query and updates the respective atoms.
     *
     * Uses the algorithm described in Chapter 17 of the Principles of Databases textbook,
     * available online here: https://github.com/pdm-book/community
     *
     * @param query the input query to be minimised. The changes are done in place on this variable
     */
    public static void executeMinimizeCQBody(Query query) {
        /*
        Algorithm high level description

        Minimization(Q(x1,...,xk) :- body)

        Repeat until no change
            choose an atom α ∈ body such that the variables x1,...,xk appear in body ∖ {α}
            if there is a query homomorphism from Q(x1,...,xk) :- body to Q(x1,...,xk) :- body ∖ {α}
            then body := body ∖ {α}
        Return Q(x1,...,xk) :- body
         */

        boolean changeMade = false;

        do {
            Atom atomToRemove = null;
            changeMade = false;
            for (Atom atom: query.getBody()) {

                List<Atom> bodyWithoutAtom = new ArrayList<>(query.getBody());
                bodyWithoutAtom.remove(atom);
                if (validQueryHomomorphismFound(query.getBody(), bodyWithoutAtom, atom, query.getHead())) {

                    //  then update body to bodyWithoutAtom and continue to the next iteration of the doWhile loop
                    changeMade = true;
                    atomToRemove = atom;
                    break;
                }
            }

            if (atomToRemove != null) {
                if (!changeMade) throw new IllegalStateException("This flag should have been set to true if the atom to remove has been identified");

                query.getBody().remove(atomToRemove);
            }

        } while(changeMade);
    }

    public static void outputQueryToFile(Query outputQuery, String outputFileName) throws IOException {
        File outFile = Paths.get(outputFileName).toFile();
        outFile.createNewFile();

        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write(outputQuery.toString());
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
//            Query query = QueryParser.parse("Q(x, y) :- R(x, z), S(y, z, w)");
//            Query query = QueryParser.parse("Q() :- R(x, 'z'), S(4, z, w)");

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
