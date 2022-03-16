package ed.inf.adbs.minibase.base;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConstantTests {
    private final Constant stringConstantX1 = new StringConstant("X");
    private final Constant stringConstantX2 = new StringConstant("X");

    private final Constant integerConstant69TheFirst = new IntegerConstant(69);
    private final Constant integerConstant69TheSecond = new IntegerConstant(69);

    private final Constant integerConstant420 = new IntegerConstant(420);
    private final Constant stringConstantXXX = new StringConstant("XXX");

    @Test
    public void test_equals_FalseOnDifferentConstantClasses() {

        assertFalse(stringConstantX1.equals(integerConstant69TheFirst));
    }

    @Test
    public void test_equals_FalseOnDifferentConstantClassesSwitchedOrder() {

        assertFalse(integerConstant69TheFirst.equals(stringConstantX1));
    }

    @Test
    public void test_equals_trueForEqualStringConstants() {

        assertTrue(stringConstantX2.equals(stringConstantX1));
    }

    @Test
    public void test_equals_trueForEqualIntegerConstants() {

        assertTrue(integerConstant69TheSecond.equals(integerConstant69TheFirst));
    }

    @Test
    public void test_equals_falseForUnequalIntegerConstants() {

        assertFalse(integerConstant69TheSecond.equals(integerConstant420));
    }

    @Test
    public void test_equals_falseForUnequalStringConstants() {

        assertFalse(stringConstantX1.equals(stringConstantXXX));
    }

}
