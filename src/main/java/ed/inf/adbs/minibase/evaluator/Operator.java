package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.FileWriter;
import java.io.IOException;

public abstract class Operator {

    public abstract Tuple getNextTuple() throws IOException;

    public abstract void reset();

    public void dump() {
        Tuple nextTuple;
        FileWriter outWriter = null;
        if (OutputWriter.outputWriterInitialised()) {
            outWriter = OutputWriter.getFileWriter();
        }
        try {
            while ((nextTuple = getNextTuple()) != null) {

                if (outWriter != null) {
                    System.out.println("Writing tuple to file: " + nextTuple);
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
