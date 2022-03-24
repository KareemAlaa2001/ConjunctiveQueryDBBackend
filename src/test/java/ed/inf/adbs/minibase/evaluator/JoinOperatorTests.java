package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.Tuple;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JoinOperatorTests {
    private final Variable variablex = new Variable("x");
    private final Variable variabley = new Variable("y");
    private final Variable variablez = new Variable("z");
    private final Variable variablef = new Variable("f");
    Variable variableg = new Variable("g");
    Variable variableh = new Variable("h");
    Variable variablej = new Variable("j");
    Variable variablek = new Variable("j");


    private final Constant stringConstantX1 = new StringConstant("X");
    private final Constant stringConstantX2 = new StringConstant("X");

    private final Constant integerConstant69TheFirst = new IntegerConstant(69);
    private final Constant integerConstant69TheSecond = new IntegerConstant(69);

    private final Constant integerConstant420 = new IntegerConstant(420);
    private final Constant stringConstantXXX = new StringConstant("XXX");

    private final ComparisonAtom xLTy = new ComparisonAtom(variablex, variabley, ComparisonOperator.LT);
    private final ComparisonAtom xGTy = new ComparisonAtom(variablex, variabley, ComparisonOperator.GT);
    private final ComparisonAtom fGTy = new ComparisonAtom(variablef, variabley, ComparisonOperator.GT);
    private final ComparisonAtom xLT420 = new ComparisonAtom(variablex, integerConstant420, ComparisonOperator.LT);
    private final ComparisonAtom integer69LT420 = new ComparisonAtom(integerConstant69TheFirst, integerConstant420, ComparisonOperator.LT);

    private final List<Term> variableTerms = new ArrayList<Term>() {{
        add(variablex);
        add(variabley);
        add(variablez);
    }};

    private final RelationalAtom rightRelationalAtom = new RelationalAtom("R", variableTerms);

    private final List<Constant> tupleContents = new ArrayList<Constant>() {{
        add(integerConstant69TheFirst);
        add(integerConstant420);
        add(stringConstantXXX);
    }};

    private final Tuple testRightTuple = new Tuple(tupleContents);
    private final Variable variablexCopy = new Variable("x");

    private final List<Constant> leftTupleContents = new ArrayList<Constant>() {{
        add(integerConstant69TheSecond);
        add(stringConstantX1);
        add(integerConstant69TheFirst);
        add(stringConstantXXX);
    }};

    private final List<Term> leftRelAtom1Terms = new ArrayList<Term>() {{
        add(variablex);
        add(variablef);
    }};

    private final List<Term> leftRelAtom2Terms = new ArrayList<Term>() {{
        add(variablex);
        add(variablez);
    }};

    private final RelationalAtom leftRelationalAtom1 = new RelationalAtom("Rel1", leftRelAtom1Terms);
    private final RelationalAtom leftRelationalAtom2 = new RelationalAtom("Rel2", leftRelAtom2Terms);

    private final List<RelationalAtom> leftRelationalAtoms = new ArrayList<RelationalAtom>() {{
        add(leftRelationalAtom1);
        add(leftRelationalAtom2);
    }};

    List<Term> unrelatedTerms1 = new ArrayList<Term>() {{
        add(variableg);
        add(variableh);

    }};

    List<Term> unrelatedTerms2 = new ArrayList<Term>() {{
        add(variablej);
        add(variablek);
    }};

    RelationalAtom unrelatedRelationalatom1 = new RelationalAtom("UnRel1", unrelatedTerms1);
    RelationalAtom unrelatedRelationalatom2 = new RelationalAtom("UnRel2", unrelatedTerms2);

    List<RelationalAtom> unrelatedRelationalAtomsList = new ArrayList<RelationalAtom>() {{
        add(unrelatedRelationalatom1);
        add(unrelatedRelationalatom2);
    }};

    Tuple leftTuple = new Tuple(leftTupleContents);

    @Test
    public void test_joinAcceptedSimpleCase() {
        assertTrue(JoinOperator.passesSelectionPredicatesMultipleRelations(leftTuple, testRightTuple, leftRelationalAtoms, rightRelationalAtom, new ArrayList<>()));
    }

    @Test
    public void test_joinFailsSimpleCaseWhereVariablesMismatch() {
        List<RelationalAtom> reversedLeftRelationalAtoms = new ArrayList<>(leftRelationalAtoms);
        Collections.reverse(reversedLeftRelationalAtoms);
        assertFalse(JoinOperator.passesSelectionPredicatesMultipleRelations(leftTuple, testRightTuple, reversedLeftRelationalAtoms, rightRelationalAtom, new ArrayList<>()));
    }

    @Test
    public void test_passesSelectionPredicatesMultipleRelations_allowsCartesianProducts() {
        assertTrue(JoinOperator.passesSelectionPredicatesMultipleRelations(leftTuple, testRightTuple, unrelatedRelationalAtomsList, rightRelationalAtom, new ArrayList<>()));
    }


    @Test
    public void test_passesSelectionPredicatesMultipleRelations_correctlyEvaluatesMultiPredicateList() {
        ComparisonAtom fLTz = new ComparisonAtom(variablef, variablez, ComparisonOperator.LT);
        List<ComparisonAtom> correctSelectionPredicates = new ArrayList<ComparisonAtom>() {{
           add(fLTz);
        }};

        assertTrue(JoinOperator.passesSelectionPredicatesMultipleRelations(leftTuple, testRightTuple, leftRelationalAtoms, rightRelationalAtom, correctSelectionPredicates));
    }

    @Test
    public void test_passesSelectionPredicatesMultipleRelations_correctlyEvaluateFalseMultiPredicateList() {

        ComparisonAtom fLTz = new ComparisonAtom(variablef, variablez, ComparisonOperator.LT);

        List<ComparisonAtom> correctSelectionPredicates = new ArrayList<ComparisonAtom>() {{
            add(fLTz);
            add(xGTy);
        }};

        assertFalse(JoinOperator.passesSelectionPredicatesMultipleRelations(leftTuple, testRightTuple, leftRelationalAtoms, rightRelationalAtom, correctSelectionPredicates));

    }
}
