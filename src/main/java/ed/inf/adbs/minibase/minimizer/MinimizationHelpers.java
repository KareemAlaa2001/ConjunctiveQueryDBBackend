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

    /**
     * Attempts to find a valid query homomorphism from a given base query body to a given target query body.
     * This is done by constraining the possible mappings to only those that can be made using the terms inside the atomInQuestion.
     * Returns whether a valid query homomorphism has been found given those constraints
     *
     * @param baseQuery the starting query
     * @param targetQuery the target query that transformed versions of the base query are checked against for equality
     * @param atomInQuestion the atom from which the map of possible variable mappings is generated.
     * @param head the head of the query in question.
     *             This is necessary when filtering out the variables being added to the transformation map in order to not map out a variable that's in the query head
     * @return returns a boolean representing whether a valid query homomorphism is found
     */
    public static boolean validQueryHomomorphismFound(List<Atom> baseQuery, List<Atom> targetQuery, RelationalAtom atomInQuestion, RelationalAtom head) {
        //  idea is essentially: to result in the valid targetQuery, we're focusing on transforming just the variables in the atom.
        //  So we can recursively backtrack on the possible mappings we can do to the variables in the atom.
        //  Will need to apply a transformation to the entire query when completing the variable mapping.

        //  obtaining a set of valid transformation target terms for the variables in the atomInQuestion
        //  by getting a set of all the terms in the relational atoms with the same name in the ttarget query
        Set<Term> validTransformationTargets = targetQuery.stream().map(RelationalAtom.class::cast)
                .filter(relationalAtom -> relationalAtom.getName().equals(atomInQuestion.getName()))
                .flatMap(currAtom -> currAtom.getTerms().stream())
                .collect(Collectors.toSet());

        //  creating a list of variables that could be mapped out from the atomInQuestion by getting the variables in its terms and filtering out the ones that are in the query's head
        List<Variable> variablesToMap = atomInQuestion.getTerms().stream()
                .filter(MinimizationHelpers::isVariable)
                .map(Variable.class::cast).filter(term -> !head.getTerms().contains(term)).collect(Collectors.toList());

        //  creating map of all the possible variable mappings that could be done on the baseQuery
        //  this is done by adding an entry mapping each variable from the list above to the set of transformation targets
        //  (minus any instances of the variable itself in order to prevent calls to map a variable to itself)
        HashMap<Variable, Set<Term>> transformationsToAttempt = new HashMap<>();

        variablesToMap.forEach(variable ->
                transformationsToAttempt.put(variable,
                        validTransformationTargets.stream()
                        .filter((term -> !term.equals(variable)))
                        .collect(Collectors.toSet())));

        //  converting the base and target query bodies to sets in order to make query equivalence and containment checks easier, and in order to comply with the type specification in the backtrackThroughTransformations function
        Set<RelationalAtom> baseQuerySet = baseQuery.stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());
        Set<RelationalAtom> targetQuerySet = targetQuery.stream().map(RelationalAtom.class::cast).collect(Collectors.toSet());

        //  we have a bunch of valid possible transformations now, we need to backtrack through these and either find a valid result or return nothing found
        return backtrackThroughTransformations(baseQuerySet, targetQuerySet, transformationsToAttempt);
    }

    /**
     *  given a starting query body, a target query body and a map of transformations to attempt,
     *  this function explores the search tree of possible output queries when applying different combinations of homomorphism mappings.
     *  For each resultant query, it terminates if it is equivalent to the target query.
     *  The execution of this function follows a recursive pattern that relies on a queue of "MappingQueueState"s, which maintain the state of the iteration with the current query and remaining transformations
     *
     * Relies on two base cases at its core, either returning true when the current base query and the target query are equivalent,
     * or returning false if that equality fails and there are no remaining transformations to attempt
     *
     * @param baseQuery the base query that the search space is being built up from
     * @param targetQuery the target query that equivalence is checked against to terminate execution and return true
     * @param transformationsToAttempt a Map mapping variables to the possible terms they could be mapped to
     * @return boolean describing whether any of the possible transformation combinations resulted in a query equivalent to the target query.
     *
      */
    public static boolean backtrackThroughTransformations(Set<RelationalAtom> baseQuery, Set<RelationalAtom> targetQuery, Map<Variable, Set<Term>> transformationsToAttempt) {
        if (queryBodiesEquivalent(baseQuery, targetQuery)) return true;

        if (transformationsToAttempt.isEmpty()) return false;

        //  maintains a queue of the next queue states to try
        List<MappingQueueState> nextStatesToTry = new ArrayList<>();

        //  appending the result every possible single homomorphism mapping from the transformationsToAttempt to the queue, adding both the result of the mapping and the remaining transformations when removing the base variable
        transformationsToAttempt.forEach(((variable, terms) -> {
            Map<Variable, Set<Term>> remainingWithoutVariable = new HashMap<>(transformationsToAttempt);
            remainingWithoutVariable.remove(variable);

            terms.forEach(term -> nextStatesToTry.add(new MappingQueueState(getHomomorphismMappingResult(variable, term, baseQuery), remainingWithoutVariable)));
        }));

        //  making the recursive calls to try all of the accumulated states in the queue, and returning true overall if any of them are true
        return nextStatesToTry.stream().anyMatch(state -> backtrackThroughTransformations(state.getCurrentQuery(), targetQuery, state.getRemainingTransformations()));
    }

    //  applies a "homomorphism mapping" to a query body, where every instance of the passed in base variable is replaced with the target term in the query body, and that new query body is returned
    public static Set<RelationalAtom> getHomomorphismMappingResult(Variable baseVar, Term target, Set<RelationalAtom> baseQuery) {
        return baseQuery.stream().map(atom -> new RelationalAtom(atom.getName(), atom.getTerms().stream()
                        .map(term -> (isVariable(term) && term.equals(baseVar)) ? target : term).collect(Collectors.toList()))).collect(Collectors.toSet());
    }

    //  wrapper for checking if a term is a variable
    public static boolean isVariable(Term term) {
        return term.getClass().equals(Variable.class);
    }

    //  checking if the two passed in query bodies are equivalent by sorting both bodies by hashcode and checking that the adjacent relational atoms are equivalent
    public static boolean queryBodiesEquivalent(Set<RelationalAtom> firstQuerySet, Set<RelationalAtom> secondQuerySet) {
        if (firstQuerySet.size() != secondQuerySet.size()) return false;
        List<RelationalAtom> firstQueryList = new ArrayList<>(firstQuerySet);
        List<RelationalAtom> secondQueryList = new ArrayList<>(secondQuerySet);

        firstQueryList.sort(Comparator.comparing(RelationalAtom::hashCode));
        secondQueryList.sort(Comparator.comparing(RelationalAtom::hashCode));

        for (int i = 0; i < firstQueryList.size(); i++ ) {
            RelationalAtom firstAtom = firstQueryList.get(i);
            RelationalAtom secondAtom = secondQueryList.get(i);

            if (!relationalAtomsEquivalent(firstAtom, secondAtom)) {
                if (firstAtom.getName().equals(secondAtom.getName()))
                    return false;
            }
        }
        return true;
    }

    //  checking that two relational atoms are equivalent by checking both the names of the atoms and the terms within
    public static boolean relationalAtomsEquivalent(RelationalAtom firstAtom, RelationalAtom secondAtom) {
        if ((!firstAtom.getName().equals(secondAtom.getName())) || (firstAtom.getTerms().size() != secondAtom.getTerms().size())) return false;

        for (int j = 0; j < firstAtom.getTerms().size(); j++) {
            if (!termsEquivalent(firstAtom.getTerms().get(j),secondAtom.getTerms().get(j))) return false;
        }

        return true;
    }

    //  wrapper for terms' equality checks. Leverages the overridden implementations of the equals() method in the different Term subclasses for the equality (depending on the passed in class)
    private static boolean termsEquivalent(Term firstTerm, Term secondTerm) {
        if (!firstTerm.getClass().equals(secondTerm.getClass())) return false;

        return firstTerm.equals(secondTerm);
    }
}
