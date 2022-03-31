package ed.inf.adbs.minibase.base;

import java.util.Objects;

//  Represents ComparisonAtoms, which encode a boolean predicate between two terms
public class ComparisonAtom extends Atom {

    private Term term1;

    private Term term2;

    private ComparisonOperator op;

    public ComparisonAtom(Term term1, Term term2, ComparisonOperator op) {
        this.term1 = term1;
        this.term2 = term2;
        this.op = op;
    }

    public Term getTerm1() {
        return term1;
    }

    public Term getTerm2() {
        return term2;
    }

    public ComparisonOperator getOp() {
        return op;
    }

    @Override
    public String toString() {
        return term1 + " " + op + " " + term2;
    }


    private int evaluateConstantComparison(Constant constant1, Constant constant2) {
        if (!constant1.getClass().equals(constant2.getClass())) throw new IllegalArgumentException("Incompatible constant types passed in!");

        Class<? extends Constant> constClass = constant1.getClass();

        if (constClass.equals(IntegerConstant.class)) {
            return ((IntegerConstant) constant1).compareTo((IntegerConstant) constant2);

        } else if (constClass.equals(StringConstant.class)) {
            return ((StringConstant) constant1).compareTo((StringConstant) constant2);

        } else throw new UnsupportedOperationException("Unsupported constant type!");
    }

    public boolean compareConstants(Constant const1, Constant const2) {

        if (getOp().equals(ComparisonOperator.EQ)) return const1.equals(const2);
        if (getOp().equals(ComparisonOperator.NEQ)) return !const1.equals(const2);

        int comparisonResult = evaluateConstantComparison(const1, const2);

        if (comparisonResult > 0) {
            return (getOp().equals(ComparisonOperator.GT) || getOp().equals(ComparisonOperator.GEQ));
        } else if (comparisonResult < 0) {
            return (getOp().equals(ComparisonOperator.LT) || getOp().equals(ComparisonOperator.LEQ));
        } else {
            return (getOp().equals(ComparisonOperator.GEQ) || getOp().equals(ComparisonOperator.LEQ));
        }
    }

    //  public method which evaluates the predicate encoded by this comparison atom and returns whether it is true. It is only callable when both terms are constants rather than variables
    //  variables are expected to have been replaced with the relevant constants before this call is made.
    public boolean evaluateComparison() {
        if (this.term1 instanceof Variable || this.term2 instanceof Variable) throw new UnsupportedOperationException("Can't invoke this on the current atom unless both terms are constants!");

        else return compareConstants(((Constant) getTerm1()), ((Constant) getTerm2()));
    }

    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) return false;

        ComparisonAtom comparisonAtom = (ComparisonAtom) object;

        return this.term1.equals(comparisonAtom.getTerm1()) && this.term2.equals(comparisonAtom.getTerm2()) && this.getOp().equals(comparisonAtom.getOp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTerm1(), this.getTerm2(), this.getOp());
    }
}
