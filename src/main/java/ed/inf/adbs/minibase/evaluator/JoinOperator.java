package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.ComparisonAtom;
import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//  JoinOperator that implements a tuple nested loop join over tuples coming from left and right child operators
public class JoinOperator extends Operator {

    Operator leftChild;
    Operator rightChild;

    List<RelationalAtom> leftChildAtoms;
    RelationalAtom rightChildAtom;
    List<ComparisonAtom> joinConditions;

    Tuple outerTuple;
    Tuple innerTuple;

    public JoinOperator(Operator leftChild, Operator rightChild, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom, List<ComparisonAtom> joinConditions) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.leftChildAtoms = leftChildAtoms;
        this.rightChildAtom = rightChildAtom;
        this.joinConditions = joinConditions;
        this.innerTuple = null;
        this.outerTuple = null;
    }

    //
    /**
     * returns the next tuple that passes the join conditions and other equality checks from the current nested loop iterating over the tuples from the children
     * if the current outer tuple and current inner tuple have already been initialised, it first runs through the remaining inner tuples before getting the next outer tuple
     * to find matches, it iterates over the tuples in the left child, where for each tuple it resets the right child then iterates through all its tuples until finding a match that passes all of the predicates
     *
     * @return the next tuple matching the predicates, null if none such exist
     * @throws IOException
     */
    @Override
    public Tuple getNextTuple() throws IOException {

        if (outerTuple != null && innerTuple != null) {

            while ((innerTuple = rightChild.getNextTuple()) != null) {

                if (passesSelectionPredicatesMultipleRelations(outerTuple, innerTuple, this.leftChildAtoms, this.rightChildAtom, this.joinConditions)) {
                    List<Constant> combinedTerms = new ArrayList<>(outerTuple.getFields());
                    combinedTerms.addAll(innerTuple.getFields());
                    return new Tuple(combinedTerms);
                }
            }
        }


        while ((outerTuple = leftChild.getNextTuple()) != null) {
            rightChild.reset();

            while ((innerTuple = rightChild.getNextTuple()) != null) {

                if (passesSelectionPredicatesMultipleRelations(outerTuple, innerTuple, this.leftChildAtoms, this.rightChildAtom, this.joinConditions)) {
                    List<Constant> combinedTerms = new ArrayList<>(outerTuple.getFields());
                    combinedTerms.addAll(innerTuple.getFields());
                    return new Tuple(combinedTerms);
                }
            }
        }

        return null;
    }

    @Override
    public void reset() {
        leftChild.reset();
        rightChild.reset();
    }

    /**
     * First checks that the constants in the positions corresponding to all respective occurrences of all the variables in the right child atom across the tuples being joined are equal.
     * Then checks that the combination of the left and tuples as well as the corresponding relational atoms pass all of the join condition comparison atom predicates
     *
     * @param leftTuple the left tuple resultant from combining tuples based off the left child relational atoms list
     * @param rightTuple the right tuple coming from the right relational atom
     * @param leftChildAtoms the list of relational atoms that form up the left tuple
     * @param rightChildAtom the relational atom corresponding to the right tuple
     * @param joinConditions the list of explicit join conditions
     * @return boolean representing whether this combination of tuples passes the above checks
     */
    public static boolean passesSelectionPredicatesMultipleRelations(Tuple leftTuple, Tuple rightTuple, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom, List<ComparisonAtom> joinConditions) {

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
     *  evaluate the given ComparisonAtom on the combination of those tuples.
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

    /**
     * Given a base comparison atom with embedded variables as input, subsitutes the variables with their corresponding values across the tuples
     *
     * @param baseComparisonAtom the original comparison atom
     * @param leftTuple the left tuple, formed from the combination of tuples corresponding to the left relational atoms
     * @param rightTuple the right tuple mapped over by the right relational atom
     * @param leftChildAtoms the list of relational atoms forming the query representation of the left tuple
     * @param rightChildAtom the query representation of the right tuple
     * @return the comparison atom with the variables substituted for Constants accordingly
     */
    private static ComparisonAtom getVariableSubsInComparisonAtomTwoSides(ComparisonAtom baseComparisonAtom, Tuple leftTuple, Tuple rightTuple, List<RelationalAtom> leftChildAtoms, RelationalAtom rightChildAtom) {
        Constant term1Sub;
        Constant term2Sub;

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
                    term2Sub = leftTerm2Subs.get(0);
                }
            } else {
                term2Sub = rightTerm2Sub;
            }
        } else {
            term2Sub = (Constant) baseComparisonAtom.getTerm2();
        }

        return new ComparisonAtom(term1Sub, term2Sub, baseComparisonAtom.getOp());
    }
}
