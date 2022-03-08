package ed.inf.adbs.minibase.minimizer;

import ed.inf.adbs.minibase.base.*;

import java.util.*;
import java.util.stream.Collectors;

public class MinimizationHelpers {

    /**
     * Minimises the given input query and updates the respective atoms.
     *
     * Uses the algorithm described in Chapter 17 of the Principles of Databases textbook,
     * available online here: https://github.com/pdm-book/community
     *
     * @param query the input query to be minimised. The changes are done in place on this variable
     */
    public static void executeMinimizeCQBody(Query query) {
        /*
        Algorithm high level description

        Minimization(Q(x1,...,xk) :- body)

        Repeat until no change
            choose an atom α ∈ body such that the variables x1,...,xk appear in body ∖ {α}
            if there is a query homomorphism from Q(x1,...,xk) :- body to Q(x1,...,xk) :- body ∖ {α}
            then body := body ∖ {α}
        Return Q(x1,...,xk) :- body
         */

        boolean changeMade = false;

        do {
            Atom atomToRemove = null;
            changeMade = false;
            for (Atom atom: query.getBody()) {

                List<Atom> bodyWithoutAtom = new ArrayList<>(query.getBody());
                bodyWithoutAtom.remove(atom);
                if (validQueryHomomorphismFound(query.getBody(), bodyWithoutAtom, (RelationalAtom) atom, query.getHead())) {

                    //  then update body to bodyWithoutAtom and continue to the next iteration of the doWhile loop
                    changeMade = true;
                    atomToRemove = atom;
                    break;
                }
            }

            if (atomToRemove != null) {
                if (!changeMade) throw new IllegalStateException("This flag should have been set to true if the atom to remove has been identified");

                query.getBody().remove(atomToRemove);
            }

        } while(changeMade);
    }

    public static boolean validQueryHomomorphismFound(List<Atom> baseQuery, List<Atom> targetQuery, RelationalAtom atomInQuestion, RelationalAtom head) {
        //  idea is essentially: to result in the valid targetQuery, we're focusing on transforming just the variables in the atom.
        //  So we can recursively backtrack on the possible mappings we can do to the variables in the atom.
        //  Will need to apply a transformation to the entire query when completing the variable mapping.

        Set<Term> validTransformationTargets = targetQuery.stream().map(RelationalAtom.class::cast)
                .filter(relationalAtom -> relationalAtom.getName().equals(atomInQuestion.getName()))
                .flatMap(currAtom -> currAtom.getTerms().stream())
                .collect(Collectors.toSet());

        List<Variable> variablesToMap = atomInQuestion.getTerms().stream()
                .filter(MinimizationHelpers::isVariable)
                .map(Variable.class::cast).filter(term -> !head.getTerms().contains(term)).collect(Collectors.toList());

        HashMap<Variable, Set<Term>> transformationsToAttempt = new HashMap<>();

        variablesToMap.forEach(var ->
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
}
