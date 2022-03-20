package ed.inf.adbs.minibase.evaluator;

import com.sun.org.apache.bcel.internal.Const;
import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JoinOperator extends Operator {

    Operator leftChild;
    Operator rightChild;

    List<RelationalAtom> leftChildAtoms;
    RelationalAtom rightChildAtom;
    List<ComparisonAtom> joinConditions;

    public JoinOperator(Operator leftChild, Operator rightChild, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom, List<ComparisonAtom> joinConditions) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.leftChildAtoms = leftChildAtoms;
        this.rightChildAtom = rightChildAtom;
        this.joinConditions = joinConditions;


    }

    //  TODO
    @Override
    public Tuple getNextTuple() throws IOException {
        return null;
    }

    @Override
    public void reset() {

    }

    public static boolean passesSelectionPredicatesMultipleRelations(Tuple leftTuple, Tuple rightTuple, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom, List<ComparisonAtom> joinConditions) {
        //  first extract the list of equalities in the relational atoms that are being checked over
        //  (where there are variables of the same name..
        //  Actually now that im thinking about it its probably a good idea to do a
        //  veryh similar evaluationutil based approach to the one in the other class

        // lmao can jsut do a conjunction here that all the selection predicates are passed but then also that the positions with the same variable name are equal!

        boolean sameNameVariableSlotsInTuplesNotEqual = rightChildAtom.getTerms().stream().anyMatch(term -> {
            if (term instanceof Constant) throw new IllegalArgumentException("Shouldnt be getting constants embedded in relational atoms at this stage!");

            List<Constant> instances = EvaluationUtils.getSubsForAllInstancesOfVariableInCombinedTuple(leftChildAtoms, leftTuple, (Variable) term);
            return (instances.stream().distinct().count() > 1);
        });

        if (sameNameVariableSlotsInTuplesNotEqual) return false;


    }
}
