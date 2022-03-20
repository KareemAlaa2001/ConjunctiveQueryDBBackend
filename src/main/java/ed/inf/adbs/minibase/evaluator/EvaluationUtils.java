package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.*;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvaluationUtils {

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

    public static Constant getVariableSubstitutionInTuple(RelationalAtom sourceAtom, Tuple tuple, Variable variable) {
        int index = sourceAtom.getTerms().indexOf(variable);
        if (index >= 0)
            return tuple.getFields().get(index);

        else throw new IllegalArgumentException("This tuple does not contain this variable and therefore should not be allowing such a low level execution call!");
    }

    public static Query extractConstantsToComparisonAtomsFromRelationalAtomBodies(Query inputQuery) {
        if (inputQuery.getBody().stream().noneMatch(atom -> atom instanceof RelationalAtom && ((RelationalAtom) atom).getTerms().stream().anyMatch(Constant.class::isInstance))) return inputQuery;

        Set<String> bodyVariableNames = inputQuery.getBody().stream()
                .filter(RelationalAtom.class::isInstance)
                .map(RelationalAtom.class::cast)
                .flatMap(relationalAtom ->
                        relationalAtom.getTerms().stream()
                                .filter(Variable.class::isInstance)
                                .map(Variable.class::cast)).map(Variable::getName)
                .collect(Collectors.toSet());

        List<ComparisonAtom> comparisonAtomsToAdd = new ArrayList<>();

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

        bodyWithoutNewComparisonAtoms.addAll(comparisonAtomsToAdd);
        bodyWithoutNewComparisonAtoms.addAll(inputQuery.getBody().stream().filter(ComparisonAtom.class::isInstance).collect(Collectors.toList()));

        return new Query(inputQuery.getHead(), bodyWithoutNewComparisonAtoms);
    }

    private static String generateNewVariableName(Set<String> existingVariableNames, RelationalAtom relationalAtom, int index) {
        String candidateName = relationalAtom.getName() + index;
        while (existingVariableNames.contains(candidateName)) {
            candidateName = candidateName.substring(0, candidateName.length() - 1) + "0" + index;
        }

        return candidateName;
    }
}
