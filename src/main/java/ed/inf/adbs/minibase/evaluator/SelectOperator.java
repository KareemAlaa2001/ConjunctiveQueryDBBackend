package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of the selection operator, which emits a tuple only if it passes all of the relevant selection predicates.
 * Only applicable when looking at tuples originating from one relational atom.
 */
public class SelectOperator extends Operator {

    private Operator child;
    private RelationalAtom baseRelationalAtom;
    private List<ComparisonAtom> selectionPredicates;

    /**
     *
     * @param child the child operator that this pulls tuples from
     * @param baseRelationalAtom the relational atom representing the tuples in question in the query
     * @param selectionPredicates the selection predicates on which tuples are evaluated
     */
    public SelectOperator(Operator child, RelationalAtom baseRelationalAtom, List<ComparisonAtom> selectionPredicates) {
        if (baseRelationalAtom.getTerms().stream().anyMatch(Constant.class::isInstance)) throw new UnsupportedOperationException("Can't support constants embedded in selection relational atoms yet!");

        this.child = child;
        this.baseRelationalAtom = baseRelationalAtom;
        this.selectionPredicates = selectionPredicates;
    }

    /**
     * emits the next tuple that passes the selection predicates
     *
     * @return next tuple to emit. Emits null if none such remaining tuples exist
     * @throws IOException
     */
    @Override
    public Tuple getNextTuple() throws IOException {

        Tuple nextTuple = null;
        Tuple tupleInQuestion;

        while ((tupleInQuestion  = child.getNextTuple()) != null) {
            if (passesSelectionPredicates(tupleInQuestion, this.selectionPredicates ,this.baseRelationalAtom)) {
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

    //  checks if all the predicates are true
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
            throw new UnsupportedOperationException("Still haven't decided what to do with queries with complex predicates outside the scope of the base relationalatom!");
        }
    }

    /**
     * returns a comparison atom with all of the variables in the baseComparisonAtom subbed out for the relevant constants in the tuple.
     * @param tuple the tuple in question
     * @param baseComparisonAtom the base comparison atom that contains the unmodified terms
     * @param sourceAtom the relational atom that encodes where the variables that appear in the baseComparisonAtom map to in the tuple
     * @return the comparison atom with two relevant constants as its terms in place of any variables
     */
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

    //  checks that any variables that appear in the comparison atom also appear in the relational atom
    public static boolean relationalAtomContainsPredicateVariables(RelationalAtom relationalAtom, ComparisonAtom comparisonAtom) {
        if (comparisonAtom.getTerm1() instanceof Variable && !relationalAtom.getTerms().contains(comparisonAtom.getTerm1())) return false;
        else return !(comparisonAtom.getTerm2() instanceof Variable) || relationalAtom.getTerms().contains(comparisonAtom.getTerm2());
    }

    public Operator getChild() {
        return child;
    }

    public void setChild(Operator child) {
        this.child = child;
    }

    public RelationalAtom getBaseRelationalAtom() {
        return baseRelationalAtom;
    }

    public void setBaseRelationalAtom(RelationalAtom baseRelationalAtom) {
        this.baseRelationalAtom = baseRelationalAtom;
    }

    public List<ComparisonAtom> getSelectionPredicates() {
        return selectionPredicates;
    }

    public void setSelectionPredicates(List<ComparisonAtom> selectionPredicates) {
        this.selectionPredicates = selectionPredicates;
    }
}
