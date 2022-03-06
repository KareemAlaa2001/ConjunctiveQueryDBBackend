package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.*;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static String join(Collection<?> c, String delimiter) {
        return c.stream()
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    public static boolean validQueryHomomorphismFound(List<Atom> baseQuery, List<Atom> targetQuery, Atom atomInQuestion, RelationalAtom head) {
        //  idea is essentially: to result in the valid targetQuery, we're focusing on transforming just the variables in the atom.
        //  So we can recursively backtrack on the possible mappings we can do to the variables in the atom.
        //  Will need to apply a transformation to the entire query when completing the variable mapping.

        Set<Term> validTransformationTargets = targetQuery.stream()
                .flatMap((currAtom) -> ((RelationalAtom) currAtom)
                        .getTerms().stream())
                .collect(Collectors.toSet());

        List<Variable> variablesToMap = ((RelationalAtom) atomInQuestion).getTerms().stream()
                .filter(Utils::isVariable)
                .map(Variable.class::cast).filter(term -> !head.getTerms().contains(term)).collect(Collectors.toList());

        HashMap<Variable, Set<Term>> transformationsToAttempt = new HashMap<>();

        variablesToMap.forEach((var) ->
                transformationsToAttempt.put(var,
                        validTransformationTargets.stream()
                        .filter((term -> !term.equals(var)))
                        .collect(Collectors.toSet())));

        //  we have a bunch of valid possible transformations now, we need to backtrack through these and either find a valid result or return nothing found...
        return backtrackThroughMappings(baseQuery, targetQuery, transformationsToAttempt);
    }

    public static boolean backtrackThroughMappings(List<Atom> baseQuery, List<Atom> targetQuery, Map<Variable, Set<Term>> transformationsToAttempt) {

        if (transformationsToAttempt.isEmpty()) return baseQuery.equals(targetQuery);

        Set<RelationalAtom> targetQuerySet = targetQuery.stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> baseQuerySet = baseQuery.stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());

        //  create a queue of accumulatorstat, remainingTransformations
        Queue<MappingQueueState> configurationQueue = new ArrayDeque<>();
        configurationQueue.add(new MappingQueueState(baseQuerySet, transformationsToAttempt));

        while (!configurationQueue.isEmpty()) {
            MappingQueueState currState = configurationQueue.remove();
            if (currState.getCurrentQuery().equals(targetQuerySet)) return true;

            currState.getRemainingTransformations().forEach(((variable, terms) -> {

                List<Set<RelationalAtom>> validTransformationResults = terms.stream().map(term -> getHomomorphismMappingResult(variable, term, currState.getCurrentQuery())).collect(Collectors.toList());
                Map<Variable, Set<Term>> remainingValidTransformations = new HashMap<>(currState.getRemainingTransformations());

                remainingValidTransformations.remove(variable);

                validTransformationResults.forEach(resQuery -> configurationQueue.add(new MappingQueueState(resQuery, remainingValidTransformations)));
            }));
        }

        return false;
    }

    private static Set<RelationalAtom> getHomomorphismMappingResult(Variable baseVar, Term target, Set<RelationalAtom> baseQuery) {
        return baseQuery.stream().map(atom -> new RelationalAtom(atom.getName(), atom.getTerms().stream()
                        .map(term -> (isVariable(term) && term.equals(baseVar)) ? target : term).collect(Collectors.toList()))).collect(Collectors.toSet());
    }

    public static boolean isVariable(Term term) {
        return term.getClass().equals(Variable.class);
    }

    private Utils() {}
}

class MappingQueueState {

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