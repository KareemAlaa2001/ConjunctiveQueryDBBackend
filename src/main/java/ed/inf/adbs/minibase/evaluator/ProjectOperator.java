package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

//   project operator implementation
public class ProjectOperator extends Operator {

    private Operator child;

    private List<Variable> outputVariables;
    private RelationalAtom baseRelationalAtom;

    private List<RelationalAtom> relationalAtomList;
    private Set<Tuple> tuplesOutputSoFar;

    //  checking that all of the requested output variables are actually inside the base relational atom before initialising the members
    //  this constructor creates a projection operator projecting over tuples emitted from a single source table (and thus relational atom)
    public ProjectOperator(Operator child, List<Variable> outputVariables, RelationalAtom baseRelationalAtom) {
        if (!baseRelationalAtom.getTerms().containsAll(outputVariables)) throw new IllegalArgumentException("Attempting to project out to a variable that doesnt exit in the input!!");

        this.child = child;
        this.outputVariables = outputVariables;
        this.baseRelationalAtom = baseRelationalAtom;

        this.tuplesOutputSoFar = new HashSet<>();
    }

    //  this constructor creates a projection operator perojecting over tuples resulting from a varying number of joins
    public ProjectOperator(Operator child, List<Variable> outputVariables, List<RelationalAtom> childRelationalAtoms) {
        if (!childRelationalAtoms.stream().flatMap(relationalAtom -> relationalAtom.getTerms().stream()).collect(Collectors.toList()).containsAll(outputVariables))
            throw new IllegalArgumentException("Attempting to project out to a variable that doesnt exit in the input!!");

        this.child = child;
        this.outputVariables = outputVariables;
        this.relationalAtomList = childRelationalAtoms;

        this.tuplesOutputSoFar = new HashSet<>();
    }

    /**
     * Gets the next tuple from the child operator and returns the relevant columns as specified in the operator
     * Incorporates set semantics by adding to an accumulator set of tuples emitted, updating the set using the tuples resultant from only keeping the required columns.
     * If the projected version of the current child tuple has already been emitted, repeat until encountering a new one. Then emit it and add it to the set of emitted tuples.
     * @return the next valid tuple. if none found, return null.
     * @throws IOException throws IOException if any exceptions bubble up from child operators
     */
    @Override
    public Tuple getNextTuple() throws IOException {

        Tuple nextChildTuple = child.getNextTuple();

        if (nextChildTuple == null) return null;

        Tuple possibleOutputTuple = this.getOutputTupleFromProjection(nextChildTuple);

        while (this.tuplesOutputSoFar.contains(possibleOutputTuple)) {
            if ((nextChildTuple = child.getNextTuple()) == null) return null;
            possibleOutputTuple = this.getOutputTupleFromProjection(nextChildTuple);
        }

        this.tuplesOutputSoFar.add(possibleOutputTuple);
        return possibleOutputTuple;
    }

    @Override
    public void reset() {
        child.reset();
    }

    //  gets a tuple with the projection result by substituting variables from the output variables
    private Tuple getOutputTupleFromProjection(Tuple inputTuple) {
        if (inputTuple.getFields().size() != getTotalNumTerms()) throw new IllegalArgumentException("Mismatched input tuple and relationalAtom sizes!!");

        return new Tuple(
                outputVariables.stream()
                        .map(variable -> getVariableSubstitutionBasedOnState(variable, inputTuple))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Extracts relevant constant from substituting the variable over the input tuple.
     * Based on whether the operator operates over a single relational atom or a list of them, the relevant substitution function call is made.
     *
     * @param variable the variable to sub out
     * @param inputTuple the input tuple
     * @return
     */
    private Constant getVariableSubstitutionBasedOnState(Variable variable, Tuple inputTuple) {
        assertEitherBaseRelationalAtomOrRelationalAtomListInitialized();

        if (this.baseRelationalAtom == null) {
            List<Constant> variableSubs = EvaluationUtils.getSubsForAllInstancesOfVariableInCombinedTuple(this.relationalAtomList, inputTuple, variable);
            if (variableSubs.stream().distinct().count() > 1) throw new IllegalArgumentException("There shouldn't be different values assigned to the same variable at this point!");

            return variableSubs.get(0);
        } else {
            return EvaluationUtils.getVariableSubstitutionInTuple(this.getBaseRelationalAtom(), inputTuple, variable);
        }
    }

    //  gets the total number of terms in the relational atom(s) over which this operator operates
    private int getTotalNumTerms() {
        assertEitherBaseRelationalAtomOrRelationalAtomListInitialized();
        if (this.baseRelationalAtom == null) {
            Optional<Integer> optionalCount = this.relationalAtomList.stream().map(relationalAtom -> relationalAtom.getTerms().size()).reduce(Integer::sum);
            if (!optionalCount.isPresent()) throw new IllegalArgumentException("Somehow no count retreived??");
            return optionalCount.get();
        } else {
            return this.baseRelationalAtom.getTerms().size();
        }
    }

    //  checks that either the base relational atom or relational atom list have been initialised
    private void assertEitherBaseRelationalAtomOrRelationalAtomListInitialized() {
        if (this.baseRelationalAtom == null && this.relationalAtomList == null)
            throw new UnsupportedOperationException("Either the base relational atom or the relational atom list has to be non-null for this to be callable!");
    }

    public Operator getChild() {
        return child;
    }

    public void setChild(Operator child) {
        this.child = child;
    }

    public List<Variable> getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(List<Variable> outputVariables) {
        this.outputVariables = outputVariables;
    }

    public RelationalAtom getBaseRelationalAtom() {
        return baseRelationalAtom;
    }

    public void setBaseRelationalAtom(RelationalAtom baseRelationalAtom) {
        this.baseRelationalAtom = baseRelationalAtom;
    }
}
