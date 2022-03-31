package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvaluationUtils {

    //  given a relational atom, a tuple that it maps over and a variable to substitute out, this function returns the constant in the tuple that this variable points to
    public static Constant getVariableSubstitutionInTuple(RelationalAtom sourceAtom, Tuple tuple, Variable variable) {
        int index = sourceAtom.getTerms().indexOf(variable);
        if (index >= 0)
            return tuple.getFields().get(index);

        else throw new IllegalArgumentException("This tuple does not contain this variable and therefore should not be allowing such a low level execution call!");
    }


    /**
     * extends the above functionality to a tuple is resultant from joining multiple relational atoms together.
     * First ensures the length of the tuple is equal to the sum of the lengths of the respective term lists.
     *
     * @param leftChildAtoms the list of relational atoms representing the combined tuple in query form
     * @param combinedTuple the combined tuple in question
     * @param variable the variable being checked across the relational atoms
     * @return a list of constants representing every instance in the tuple mapped to by the given variable's occurrences in the relational atom list
     */
    public static List<Constant> getSubsForAllInstancesOfVariableInCombinedTuple(List<RelationalAtom> leftChildAtoms, Tuple combinedTuple, Variable variable) {
        if (leftChildAtoms.stream().map(RelationalAtom::getTerms).map(List::size).reduce(0, Integer::sum) != combinedTuple.getFields().size())
            throw new IllegalArgumentException("The left child atoms list should have a term size sum equivalent to the number of fields in the combined tuple!");

        int offset = 0;

        List<Constant> relevantConstants = new ArrayList<>();

        for (RelationalAtom sourceAtom: leftChildAtoms) {
            int index = sourceAtom.getTerms().indexOf(variable);

            if (index >= 0) {
                relevantConstants.add(combinedTuple.getFields().get(index + offset));
            }

            offset += sourceAtom.getTerms().size();
        }

        return relevantConstants;
    }

    /**
     * Processes an input query containing constants placed inside relational atoms in the body by extracting the implied variable equivalence to those constants into explicit comparison atoms.
     * Replaces the extracted constants in the relational atom with a placeholder variable name that is also used in the comparison atom
     *
     * @param inputQuery the input unprocessed query with embedded constants in the relational atoms
     * @return a query with all of the embedded constants extracted to explicit comparison atoms
     */
    public static Query extractConstantsToComparisonAtomsFromRelationalAtomBodies(Query inputQuery) {
        if (inputQuery.getBody().stream().noneMatch(atom -> atom instanceof RelationalAtom && ((RelationalAtom) atom).getTerms().stream().anyMatch(Constant.class::isInstance))) return inputQuery;

        //  maintaining a set of the names that appear in the query body so that generated placeholder variable names don't clash with existing variable names
        Set<String> bodyVariableNames = inputQuery.getBody().stream()
                .filter(RelationalAtom.class::isInstance)
                .map(RelationalAtom.class::cast)
                .flatMap(relationalAtom ->
                        relationalAtom.getTerms().stream()
                                .filter(Variable.class::isInstance)
                                .map(Variable.class::cast)).map(Variable::getName)
                .collect(Collectors.toSet());

        List<ComparisonAtom> comparisonAtomsToAdd = new ArrayList<>();

        //  generating the list of the modified relational atoms that will form the body of the output query
        //  this loop also populates the list of comparison atoms to add with the equality comparison atoms containing the relevant constants and placeholder variable names
        List<Atom> bodyWithoutNewComparisonAtoms = inputQuery.getBody().stream().filter(RelationalAtom.class::isInstance)
                .map(RelationalAtom.class::cast).map(relationalAtom -> {
                    if (relationalAtom.getTerms().stream().noneMatch(Constant.class::isInstance)) return relationalAtom;

                    List<Term> newTermsList = IntStream.range(0, relationalAtom.getTerms().size())
                            .mapToObj(i -> {
                                if (relationalAtom.getTerms().get(i) instanceof Variable)
                                    return relationalAtom.getTerms().get(i);

                                else {
                                    String newVariableName = generateNewVariableName(bodyVariableNames, relationalAtom, i);
                                    Variable variableToAdd = new Variable(newVariableName);
                                    bodyVariableNames.add(variableToAdd.getName());
                                    comparisonAtomsToAdd.add(new ComparisonAtom(variableToAdd, relationalAtom.getTerms().get(i), ComparisonOperator.EQ));

                                    return variableToAdd;
                                }
                            }).collect(Collectors.toList());

                    return new RelationalAtom(relationalAtom.getName(), newTermsList);
                }).collect(Collectors.toList());

        //  adding both the new and old comparison atoms to the body of the new query
        bodyWithoutNewComparisonAtoms.addAll(comparisonAtomsToAdd);
        bodyWithoutNewComparisonAtoms.addAll(inputQuery.getBody().stream().filter(ComparisonAtom.class::isInstance).collect(Collectors.toList()));

        //  returning a new query using the old head with the new list of atoms
        return new Query(inputQuery.getHead(), bodyWithoutNewComparisonAtoms);
    }

    //  generates a new variable name to be used in place of the constant being extracted from a given relational atom.
    //  Utilises the index of the constant to be extracted alongside the name of the relational atom to generate the name.
    //  Uses existingVariableNames to make sure there are no clashes with this new name.
    //  if there are, a 0 is added between the relation name and the index and the new name is checked for clashes. This is repeated until a unique name is found.
    private static String generateNewVariableName(Set<String> existingVariableNames, RelationalAtom relationalAtom, int index) {
        String candidateName = relationalAtom.getName() + index;
        while (existingVariableNames.contains(candidateName)) {
            candidateName = candidateName.substring(0, candidateName.length() - 1) + "0" + index;
        }

        return candidateName;
    }
}
