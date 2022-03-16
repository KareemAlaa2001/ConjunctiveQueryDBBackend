package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;

public abstract class Operator {

    public abstract Tuple getNextTuple() throws IOException;

    public abstract void reset();

    public abstract void dump();
}
