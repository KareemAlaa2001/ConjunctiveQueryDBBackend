package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.FileWriter;
import java.io.IOException;

//  abstract superclass for all operators that form the query evaluation tree.
//  Contains abstract definitions for getNextTuple() and reset() as well asa an implementation of dump() utilising these
public abstract class Operator {

    public abstract Tuple getNextTuple() throws IOException;

    public abstract void reset();

    //  dumps the remaining tuples that will be emitted from this operator.
    //  checks the global singleton output writer for the file writer. If initialised, this dumps the tuples to the output file.
    //  Otherwise, it prints the tuples to System.out
    public void dump() {
        Tuple nextTuple;
        FileWriter outWriter = null;
        if (OutputWriter.outputWriterInitialised()) {
            outWriter = OutputWriter.getFileWriter();
        }
        try {
            while ((nextTuple = getNextTuple()) != null) {

                if (outWriter != null) {
                    outWriter.write(nextTuple.toString() + "\n");
                } else {
                    System.out.println(nextTuple);
                }
            }
            if (outWriter != null)
                outWriter.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
