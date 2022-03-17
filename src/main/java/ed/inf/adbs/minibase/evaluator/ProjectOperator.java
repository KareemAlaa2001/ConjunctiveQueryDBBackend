package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Variable;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectOperator extends Operator {

    private Operator child;
    private List<Variable> outputVariables;
    private RelationalAtom baseRelationalAtom;
    private final boolean projectionNeeded;
    private Set<Tuple> tuplesOutputSoFar;

    public ProjectOperator(Operator child, List<Variable> outputVariables, RelationalAtom baseRelationalAtom) {
        if (!baseRelationalAtom.getTerms().containsAll(outputVariables)) throw new IllegalArgumentException("Attempting to project out to a variable that doesnt exit in the input!!");
        this.child = child;
        this.outputVariables = outputVariables;
        this.baseRelationalAtom = baseRelationalAtom;
        this.projectionNeeded = baseRelationalAtom.getTerms().size() != outputVariables.size();
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
        if (inputTuple.getFields().size() != baseRelationalAtom.getTerms().size()) throw new IllegalArgumentException("Mismatched input tuple and relationalAtom sizes!!");

        return new Tuple(
                outputVariables.stream()
                        .map(variable -> EvaluationUtils.getVariableSubstitutionInTuple(this.getBaseRelationalAtom(), inputTuple, variable))
                        .collect(Collectors.toList())
        );
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
