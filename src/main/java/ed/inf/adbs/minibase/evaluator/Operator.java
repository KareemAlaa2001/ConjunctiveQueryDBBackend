package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;

public abstract class Operator {

    public abstract Tuple getNextTuple() throws IOException;

    public abstract void reset();

    public void dump() {
        Tuple nextTuple;
        try {
            while ((nextTuple = getNextTuple()) != null) {
                System.out.println(nextTuple);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
