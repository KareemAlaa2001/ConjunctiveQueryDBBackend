package ed.inf.adbs.minibase.evaluator;

import com.sun.org.apache.bcel.internal.Const;
import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public Tuple getNextTuple() throws IOException {

        Tuple nextTuple = null;

        Tuple innerTuple;
        Tuple outerTuple;

        boolean matchFound = false;

        while ((outerTuple = leftChild.getNextTuple()) != null) {
            rightChild.reset();

            while ((innerTuple = rightChild.getNextTuple()) != null) {
                if (passesSelectionPredicatesMultipleRelations(outerTuple, innerTuple, this.leftChildAtoms, this.rightChildAtom, this.joinConditions)) {
                    List<Constant> combinedTerms = new ArrayList<>(outerTuple.getFields());
                    combinedTerms.addAll(innerTuple.getFields());
                    nextTuple = new Tuple(combinedTerms);
                    matchFound = true;
                    break;
                }
            }

            if (matchFound) break;
        }

        return nextTuple;
    }

    @Override
    public void reset() {
        leftChild.reset();
        rightChild.reset();
    }

    public static boolean passesSelectionPredicatesMultipleRelations(Tuple leftTuple, Tuple rightTuple, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom, List<ComparisonAtom> joinConditions) {
        //  first extract the list of equalities in the relational atoms that are being checked over
        //  (where there are variables of the same name..
        //  Actually now that im thinking about it its probably a good idea to do a
        //  veryh similar evaluationutil based approach to the one in the other class

        // lmao can jsut do a conjunction here that all the selection predicates are passed but then also that the positions with the same variable name are equal!

        boolean sameNameVariableSlotsInTuplesNotEqual = rightChildAtom.getTerms().stream().anyMatch(term -> {
            if (term instanceof Constant)
                throw new IllegalArgumentException("Shouldnt be getting constants embedded in relational atoms at this stage!");

            List<Constant> instances = EvaluationUtils.getSubsForAllInstancesOfVariableInCombinedTuple(leftChildAtoms, leftTuple, (Variable) term);
            return (instances.stream().anyMatch(constant -> !constant.equals(EvaluationUtils.getVariableSubstitutionInTuple(rightChildAtom, rightTuple, (Variable) term))));
        });

        return !sameNameVariableSlotsInTuplesNotEqual && passesMultiAtomPredicateList(leftTuple, rightTuple, joinConditions, leftChildAtoms, rightChildAtom);
    }

    public static boolean passesMultiAtomPredicateList(Tuple leftTuple, Tuple rightTuple, List<ComparisonAtom> predicates, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom) {
        return predicates.stream().allMatch(pred -> passesMultiAtomPredicate(leftTuple, rightTuple, pred, leftChildAtoms, rightChildAtom));
    }

    /**
     *  given a left tuple (which could come from a varying number of relational atoms) and a right tuple (which comes from 1 relational atom),
     *  evaluate the given ComparisonAtom on the combination of those tuples. Should we be accepting single relationalAtom predicates here? (nahh, that should have been handled earlier i think)
      */

    private static boolean passesMultiAtomPredicate(Tuple leftTuple, Tuple rightTuple, ComparisonAtom comparisonAtom, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom) {
        if (QueryPlanner.isSingleAtomSelectionInRelationalAtoms(comparisonAtom, leftChildAtoms))
            throw new IllegalArgumentException(
                    "Supposedly shouldn't have any single child comparison atoms make it to this stage?" +
                " Might wanna check the comparison atom itself if this is thrown");

        //  will assume that all instances of the same variable in the left tuple are equal at this point, which means that we only need to worry about any substitution

        ComparisonAtom variablesSubstitutedFromEitherSide = getVariableSubsInComparisonAtomTwoSides(comparisonAtom, leftTuple, rightTuple, leftChildAtoms, rightChildAtom);

        return variablesSubstitutedFromEitherSide.evaluateComparison();
    }

    private static ComparisonAtom getVariableSubsInComparisonAtomTwoSides(ComparisonAtom baseComparisonAtom, Tuple leftTuple, Tuple rightTuple, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom) {
        Constant term1Sub = null;
        Constant term2Sub = null;

        if (baseComparisonAtom.getTerm1() instanceof Variable) {
            List<Constant> leftTerm1Subs = EvaluationUtils.getSubsForAllInstancesOfVariableInCombinedTuple(leftChildAtoms, leftTuple, (Variable) baseComparisonAtom.getTerm1());
            Constant rightTerm1Sub = (rightChildAtom.getTerms().contains(baseComparisonAtom.getTerm1())) ?
                    EvaluationUtils.getVariableSubstitutionInTuple(rightChildAtom, rightTuple, (Variable) baseComparisonAtom.getTerm1())
                    : null;

            if (rightTerm1Sub != null && leftTerm1Subs.stream().anyMatch(res -> !res.equals(rightTerm1Sub)))
                throw new IllegalArgumentException("Unequal subs for same variable!");

            else if (rightTerm1Sub == null) {
                if (leftTerm1Subs.isEmpty())
                    throw new IllegalArgumentException("No valid term subs found in either expression for term 1 variable!");

                else {
                    term1Sub = leftTerm1Subs.get(0);
                }
            } else {
                term1Sub = rightTerm1Sub;
            }

        } else {
            term1Sub = (Constant) baseComparisonAtom.getTerm1();
        }

        if (baseComparisonAtom.getTerm2() instanceof Variable) {
            List<Constant> leftTerm2Subs = EvaluationUtils.getSubsForAllInstancesOfVariableInCombinedTuple(leftChildAtoms, leftTuple, (Variable) baseComparisonAtom.getTerm2());

            Constant rightTerm2Sub = (rightChildAtom.getTerms().contains(baseComparisonAtom.getTerm2())) ?
                    EvaluationUtils.getVariableSubstitutionInTuple(rightChildAtom, rightTuple, (Variable) baseComparisonAtom.getTerm2())
                    : null;

            if (rightTerm2Sub != null && leftTerm2Subs.stream().anyMatch(res -> !res.equals(rightTerm2Sub)))
                throw new IllegalArgumentException("Unequal subs for same variable!");

            else if (rightTerm2Sub == null) {
                if (leftTerm2Subs.isEmpty())
                    throw new IllegalArgumentException("No valid term subs found in either expression for term 1 variable!");

                else {
                    term1Sub = leftTerm2Subs.get(0);
                }
            } else {
                term1Sub = rightTerm2Sub;
            }
        } else {
            term2Sub = (Constant) baseComparisonAtom.getTerm2();
        }

        return new ComparisonAtom(term1Sub, term2Sub, baseComparisonAtom.getOp());
    }
}
