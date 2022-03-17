package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;

public class JoinOperator extends Operator {
    Operator leftChild;
    Operator rightChild;

    //  TODO
    @Override
    public Tuple getNextTuple() throws IOException {
        return null;
    }

    @Override
    public void reset() {

    }
}
