package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;

public class JoinOperator extends Operator {
    Operator leftChild;
    Operator rightChild;
    RelationalAtom leftChildAtom;
    RelationalAtom rightChildAtom;

    public JoinOperator(Operator leftChild, Operator rightChild, RelationalAtom leftChildAtom, RelationalAtom rightChildAtom) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.leftChildAtom = leftChildAtom;
        this.rightChildAtom = rightChildAtom;
    }

    //  TODO
    @Override
    public Tuple getNextTuple() throws IOException {
        return null;
    }

    @Override
    public void reset() {

    }
}
