package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.List;

public class SelectOperator extends Operator {
    Operator child;
    RelationalAtom childAtom;

    public SelectOperator(Operator child, RelationalAtom childAtom) {
        this.child = child;
        this.childAtom = childAtom;
    }

    @Override
    public Tuple getNextTuple() throws IOException {
//        Tuple nextTuple = null;
//        Tuple tupleInQuestion = null;
//
//        while ((tupleInQuestion  = child.getNextTuple()) != null && tupleInQuestion) {
//
//        }
//
//        return nextTuple;
    }

    @Override
    public void reset() {

    }

    @Override
    public void dump() {

    }

    public static boolean passesSelectionPredicates(Tuple tuple, List<ComparisonAtom> comparisonAtomList, RelationalAtom sourceAtom) {
        return comparisonAtomList.stream().allMatch(comparisonAtom -> passesPredicate(tuple, comparisonAtom, sourceAtom));
    }

    public static boolean passesPredicate(Tuple tuple, ComparisonAtom comparisonAtom, RelationalAtom sourceAtom) {
        if (tuple.getFields().size() != sourceAtom.getTerms().size()) throw new IllegalArgumentException("Mismatched tuple length with relationalatom!");

        //  currently have a function that takes in a comparisonAtom, ex y > 5. Relational atom - ex R(x,y,z), and tuple (1,2,3)
        //  to check if the comparisonAtom predicate is passed, we need to extract the relevant constant for each variable in the relational atom.
        //  We can then use the new form of the comparison atom to evaluate whether the predicate is true

        ComparisonAtom variablesSubstituted = substituteVariablesForTupleConstants(tuple, comparisonAtom, sourceAtom);

        return variablesSubstituted.evaluateComparison();
    }

    public static ComparisonAtom substituteVariablesForTupleConstants(Tuple tuple, ComparisonAtom baseComparisonAtom, RelationalAtom sourceAtom) {

        Constant term1Sub = null;
        Constant term2Sub = null;

        if (baseComparisonAtom.getTerm1() instanceof Variable) {
            term1Sub = getVariableSubstitutionInTuple(sourceAtom, tuple, (Variable) baseComparisonAtom.getTerm1());
        } else {
            term1Sub = (Constant) baseComparisonAtom.getTerm1();
        }

        if (baseComparisonAtom.getTerm2() instanceof Variable) {
            term2Sub = getVariableSubstitutionInTuple(sourceAtom, tuple, (Variable) baseComparisonAtom.getTerm2());
        } else {
            term2Sub = (Constant) baseComparisonAtom.getTerm2();
        }

        return new ComparisonAtom(term1Sub, term2Sub, baseComparisonAtom.getOp());
    }

    public static Constant getVariableSubstitutionInTuple(RelationalAtom sourceAtom, Tuple tuple, Variable variable) {
        int index = sourceAtom.getTerms().indexOf(variable);
        return tuple.getFields().get(index);
    }

}
