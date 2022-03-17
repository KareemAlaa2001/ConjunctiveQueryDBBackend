package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.List;

public class SelectOperator extends Operator {

    private Operator child;
    private RelationalAtom relationalAtom;
    private List<ComparisonAtom> selectionPredicates;

    public SelectOperator(Operator child, RelationalAtom relationalAtom, List<ComparisonAtom> selectionPredicates) {
        this.child = child;
        this.relationalAtom = relationalAtom;
        this.selectionPredicates = selectionPredicates;
    }

    @Override
    public Tuple getNextTuple() throws IOException {

        //  TODO incorporate support for a relationalatom containing a constant (adding the equality selection predicate for an intermediate variable in its place
        if (this.relationalAtom.getTerms().stream().anyMatch(Constant.class::isInstance)) throw new UnsupportedOperationException("Can't support constatns embedded in selection relational atoms yet!");

        Tuple nextTuple = null;
        Tuple tupleInQuestion;

        while ((tupleInQuestion  = child.getNextTuple()) != null) {
            if (passesSelectionPredicates(tupleInQuestion, this.selectionPredicates ,this.relationalAtom)) {
                nextTuple = tupleInQuestion;
                break;
            }
        }

        return nextTuple;
    }

    @Override
    public void reset() {
        child.reset();
    }

    public static boolean passesSelectionPredicates(Tuple tuple, List<ComparisonAtom> comparisonAtomList, RelationalAtom sourceAtom) {
        return comparisonAtomList.stream().allMatch(comparisonAtom -> passesPredicate(tuple, comparisonAtom, sourceAtom));
    }

    public static boolean passesPredicate(Tuple tuple, ComparisonAtom comparisonAtom, RelationalAtom sourceAtom) {
        if (tuple.getFields().size() != sourceAtom.getTerms().size()) throw new IllegalArgumentException("Mismatched tuple length with relationalatom!");

        //  currently have a function that takes in a comparisonAtom, ex y > 5. Relational atom - ex R(x,y,z), and tuple (1,2,3)
        //  to check if the comparisonAtom predicate is passed, we need to extract the relevant constant for each variable in the relational atom.
        //  We can then use the new form of the comparison atom to evaluate whether the predicate is true

        if (relationalAtomContainsPredicateVariables(sourceAtom, comparisonAtom)) {
            ComparisonAtom variablesSubstituted = substituteVariablesForTupleConstants(tuple, comparisonAtom, sourceAtom);

            return variablesSubstituted.evaluateComparison();
        }

        else {
            //  TODO either integrate support for "ignoring" irrelevant conditions and returning true (bad idea) OR keep this exception and just filter out at a higher level.
            throw new UnsupportedOperationException("Still haven't decided what to do with queries with complex predicates outside the scope of the base relationalatom!");
        }
    }

    public static ComparisonAtom substituteVariablesForTupleConstants(Tuple tuple, ComparisonAtom baseComparisonAtom, RelationalAtom sourceAtom) {

        Constant term1Sub = null;
        Constant term2Sub = null;

        if (baseComparisonAtom.getTerm1() instanceof Variable) {
            term1Sub = EvaluationUtils.getVariableSubstitutionInTuple(sourceAtom, tuple, (Variable) baseComparisonAtom.getTerm1());
        } else {
            term1Sub = (Constant) baseComparisonAtom.getTerm1();
        }

        if (baseComparisonAtom.getTerm2() instanceof Variable) {
            term2Sub = EvaluationUtils.getVariableSubstitutionInTuple(sourceAtom, tuple, (Variable) baseComparisonAtom.getTerm2());
        } else {
            term2Sub = (Constant) baseComparisonAtom.getTerm2();
        }

        return new ComparisonAtom(term1Sub, term2Sub, baseComparisonAtom.getOp());
    }

    public static boolean relationalAtomContainsPredicateVariables(RelationalAtom relationalAtom, ComparisonAtom comparisonAtom) {
        if (comparisonAtom.getTerm1() instanceof Variable && !relationalAtom.getTerms().contains(comparisonAtom.getTerm1())) return false;
        else if (comparisonAtom.getTerm2() instanceof Variable && !relationalAtom.getTerms().contains(comparisonAtom.getTerm2())) return false;
        else return true;
    }

    public Operator getChild() {
        return child;
    }

    public void setChild(Operator child) {
        this.child = child;
    }

    public RelationalAtom getRelationalAtom() {
        return relationalAtom;
    }

    public void setRelationalAtom(RelationalAtom relationalAtom) {
        this.relationalAtom = relationalAtom;
    }

    public List<ComparisonAtom> getSelectionPredicates() {
        return selectionPredicates;
    }

    public void setSelectionPredicates(List<ComparisonAtom> selectionPredicates) {
        this.selectionPredicates = selectionPredicates;
    }
}
