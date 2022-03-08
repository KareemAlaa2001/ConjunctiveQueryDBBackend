package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

public class JoinOperator extends Operator {
    Operator leftChild;
    Operator rightChild;

    //  TODO
    @Override
    public Tuple getNextTuple() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void dump() {

    }
}
