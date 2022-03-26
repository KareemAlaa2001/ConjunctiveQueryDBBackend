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

public class ProjectOperator extends Operator {

    private Operator child;
    private List<Variable> outputVariables;
    private RelationalAtom baseRelationalAtom;
    private List<RelationalAtom> relationalAtomList;
    private final boolean projectionNeeded;
    private Set<Tuple> tuplesOutputSoFar;

    public ProjectOperator(Operator child, List<Variable> outputVariables, RelationalAtom baseRelationalAtom) {
        if (!baseRelationalAtom.getTerms().containsAll(outputVariables)) throw new IllegalArgumentException("Attempting to project out to a variable that doesnt exit in the input!!");
        this.child = child;
        this.outputVariables = outputVariables;
        this.baseRelationalAtom = baseRelationalAtom;
        this.projectionNeeded = getTotalNumTerms() != outputVariables.size();
        this.tuplesOutputSoFar = new HashSet<>();
    }

    public ProjectOperator(Operator child, List<Variable> outputVariables, List<RelationalAtom> childRelationalAtoms) {
        if (!childRelationalAtoms.stream().flatMap(relationalAtom -> relationalAtom.getTerms().stream()).collect(Collectors.toList()).containsAll(outputVariables)) throw new IllegalArgumentException("Attempting to project out to a variable that doesnt exit in the input!!");
        this.child = child;
        this.outputVariables = outputVariables;
        this.relationalAtomList = childRelationalAtoms;
        this.projectionNeeded = getTotalNumTerms() != outputVariables.size();
        this.tuplesOutputSoFar = new HashSet<>();
    }

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

    private Tuple getOutputTupleFromProjection(Tuple inputTuple) {
        if (inputTuple.getFields().size() != getTotalNumTerms()) throw new IllegalArgumentException("Mismatched input tuple and relationalAtom sizes!!");

        return new Tuple(
                outputVariables.stream()
                        .map(variable -> getVariableSubstitutionBasedOnState(variable, inputTuple))
                        .collect(Collectors.toList())
        );
    }

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

    private void assertEitherBaseRelationalAtomOrRelationalAtomListInitialized() {
        if (this.baseRelationalAtom == null && this.relationalAtomList == null) throw new UnsupportedOperationException("Either the base relational atom or the relational atom list has to be non-null for this to be callable!");
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
