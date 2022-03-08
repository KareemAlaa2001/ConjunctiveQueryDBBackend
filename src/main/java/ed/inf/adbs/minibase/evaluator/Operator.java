package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

public abstract class Operator {

    public abstract Tuple getNextTuple();

    public abstract void reset();

    public abstract void dump();
}
