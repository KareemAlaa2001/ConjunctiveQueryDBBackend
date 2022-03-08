package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.Term;
import ed.inf.adbs.minibase.dbstructures.Relation;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the scan operator which is reponsible for emitting the relevant Tuple(s) from the assigned table
 */
public class ScanOperator extends Operator {

    RandomAccessFile randomAccessFile;

    public ScanOperator(String fileName, Relation relation) throws FileNotFoundException {
        this.randomAccessFile = new RandomAccessFile(fileName, "r");

    }

    @Override
    public Tuple getNextTuple() {
//        try {
//            String tupleLine = randomAccessFile.readLine();
//
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void dump() {

    }
}
