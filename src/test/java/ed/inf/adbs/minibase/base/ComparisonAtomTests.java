package ed.inf.adbs.minibase.base;

import org.junit.Test;

import static org.junit.Assert.*;

public class ComparisonAtomTests {
    private final Constant stringConstantX1 = new StringConstant("X");
    private final Constant stringConstantX2 = new StringConstant("X");

    private final Constant integerConstant69TheFirst = new IntegerConstant(69);
    private final Constant integerConstant69TheSecond = new IntegerConstant(69);

    private final Constant integerConstant420 = new IntegerConstant(420);
    private final Constant stringConstantXXX = new StringConstant("XXX");

    private final Variable variablex = new Variable("x");
    private final Variable variabley = new Variable("y");

    @Test
    public void test_EvaluateComparison_ThrowsExceptionOnVariableTerms() {
        ComparisonAtom target = new ComparisonAtom(variablex, variabley, ComparisonOperator.EQ);

        assertThrows(UnsupportedOperationException.class, target::evaluateComparison);
    }

    @Test
    public void test_EvaluateComparison_ReturnsFalseOnDifferentConstantSubclassesWithEquals() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, stringConstantX2, ComparisonOperator.EQ);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparison_ReturnsTrueOnDifferentConstantSubclassesWithNEQ() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, stringConstantX2, ComparisonOperator.NEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_evaluateComparison_evaluatesNEQ_IntegerConstantTrue() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.NEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_evaluateComparison_evaluatesNEQ_StringConstantTrue() {
        ComparisonAtom target = new ComparisonAtom(stringConstantX1, stringConstantXXX, ComparisonOperator.NEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparison_ReturnsFalseOnDifferentConstantSubclassesWithoutEquals() {

        for (ComparisonOperator op: new ComparisonOperator[] {ComparisonOperator.LT, ComparisonOperator.GT, ComparisonOperator.GEQ, ComparisonOperator.LEQ}) {
            ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, stringConstantX2, op);
            assertThrows(IllegalArgumentException.class, target::evaluateComparison);
        }

    }

    @Test
    public void test_EvaluateComparisonEvaluatesGEQIntegerConstantsFalseCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.GEQ);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesGEQIntegerConstantsTrueCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant420, integerConstant69TheFirst, ComparisonOperator.GEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesGEQIntegerConstantsTrueCorrectlyForEqualValues() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheSecond, integerConstant69TheFirst, ComparisonOperator.GEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluates_GT_IntegerConstantsFalseCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.GT);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluates_GT_IntegerConstantsTrueCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant420, integerConstant69TheFirst, ComparisonOperator.GT);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesGTIntegerConstantsFalseCorrectlyForEqualValues() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheSecond, integerConstant69TheFirst, ComparisonOperator.GT);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesLEQIntegerConstantsFalseCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.LEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesLEQIntegerConstantsTrueCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant420, integerConstant69TheFirst, ComparisonOperator.LEQ);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesLEQIntegerConstantsTrueCorrectlyForEqualValues() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheSecond, integerConstant69TheFirst, ComparisonOperator.LEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluates_LT_IntegerConstantsFalseCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.LT);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluates_LT_IntegerConstantsTrueCorrectly() {
        ComparisonAtom target = new ComparisonAtom(integerConstant420, integerConstant69TheFirst, ComparisonOperator.LT);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluates_LT_IntegerConstantsFalseCorrectlyForEqualValues() {
        ComparisonAtom target = new ComparisonAtom(integerConstant69TheSecond, integerConstant69TheFirst, ComparisonOperator.LT);

        assertFalse(target.evaluateComparison());
    }


    @Test
    public void test_EvaluateComparisonEvaluatesLEQStringConstantsFalseCorrectly() {
        ComparisonAtom target = new ComparisonAtom(stringConstantXXX, stringConstantX1, ComparisonOperator.LEQ);

        assertFalse(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesLEQStringConstantsTrueCorrectly() {
        ComparisonAtom target = new ComparisonAtom(stringConstantX1, stringConstantXXX, ComparisonOperator.LEQ);

        assertTrue(target.evaluateComparison());
    }

    @Test
    public void test_EvaluateComparisonEvaluatesLEQStringConstantsTrueCorrectlyForEqualValues() {
        ComparisonAtom target = new ComparisonAtom(stringConstantX1, stringConstantX2, ComparisonOperator.LEQ);

        assertTrue(target.evaluateComparison());
    }
}
