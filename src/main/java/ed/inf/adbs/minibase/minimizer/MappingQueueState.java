package ed.inf.adbs.minibase.minimizer;

import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.Term;
import ed.inf.adbs.minibase.base.Variable;

import java.util.Map;
import java.util.Set;

public class MappingQueueState {

    private final Set<RelationalAtom> currentQuery;
    private final Map<Variable, Set<Term>> remainingTransformations;

    public MappingQueueState(Set<RelationalAtom> currentQuery, Map<Variable, Set<Term>> remainingTransformations) {
        this.currentQuery = currentQuery;
        this.remainingTransformations = remainingTransformations;
    }

    public Set<RelationalAtom> getCurrentQuery() {
        return currentQuery;
    }

    public Map<Variable, Set<Term>> getRemainingTransformations() {
        return remainingTransformations;
    }

    @Override
    public String toString() {
        return currentQuery.toString() + remainingTransformations.toString();
    }
}
