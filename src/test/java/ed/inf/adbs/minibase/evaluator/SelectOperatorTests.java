package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.Tuple;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SelectOperatorTests {

    private final Variable variablex = new Variable("x");
    private final Variable variabley = new Variable("y");
    private final Variable variablez = new Variable("z");

    private final Constant stringConstantX1 = new StringConstant("X");
    private final Constant stringConstantX2 = new StringConstant("X");

    private final Constant integerConstant69TheFirst = new IntegerConstant(69);
    private final Constant integerConstant69TheSecond = new IntegerConstant(69);

    private final Constant integerConstant420 = new IntegerConstant(420);
    private final Constant stringConstantXXX = new StringConstant("XXX");

    private final ComparisonAtom xLTy = new ComparisonAtom(variablex, variabley, ComparisonOperator.LT);
    private final ComparisonAtom xGTy = new ComparisonAtom(variablex, variabley, ComparisonOperator.GT);
    private final ComparisonAtom xLT420 = new ComparisonAtom(variablex, integerConstant420, ComparisonOperator.LT);
    private final ComparisonAtom integer69LT420 = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.LT);

    private final List<Term> variableTerms = new ArrayList<Term>() {{
            add(variablex);
            add(variabley);
            add(variablez);
    }};

    private final RelationalAtom relationalAtom = new RelationalAtom("R", variableTerms);

    private final List<Constant> tupleContents = new ArrayList<Constant>() {{
        add(integerConstant69TheFirst);
        add(integerConstant420);
        add(stringConstantXXX);
    }};

    private final Tuple testTuple = new Tuple(tupleContents);
    private final Variable variablexCopy = new Variable("x");

    @Test
    public void test_getVariableSubstitutionInTuple_substitutesCorrectly() {

        assertEquals(integerConstant69TheFirst, SelectOperator.getVariableSubstitutionInTuple(relationalAtom, testTuple, variablexCopy));

    }

    @Test
    public void test_substituteVariablesForTupleConstants_substitutesOneCorrectly() {
        assertEquals(integer69LT420, SelectOperator.substituteVariablesForTupleConstants(testTuple, xLT420, relationalAtom));
    }

    @Test
    public void test_substituteVariablesForTupleConstants_substitutesBothCorrectly() {
        assertEquals(integer69LT420, SelectOperator.substituteVariablesForTupleConstants(testTuple, xLTy, relationalAtom));
    }

    @Test
    public void test_passesPredicate_caseWithVariablesToSub() {
        assertTrue(SelectOperator.passesPredicate(testTuple, xLTy, relationalAtom));
    }

    @Test
    public void test_passesPredicate_case2WithVariablesToSub() {
        assertFalse(SelectOperator.passesPredicate(testTuple, xGTy, relationalAtom));
    }
}
