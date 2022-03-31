package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.Utils;

import java.util.List;
import java.util.Objects;

//  A class for representing relational atoms in a query.
//  Each has an associated name and list of terms. Contains implementations for toString, equals and hashCode for outputting, equality checks and use in Sets respectively
public class RelationalAtom extends Atom {
    private String name;

    private List<Term> terms;

    public RelationalAtom(String name, List<Term> terms) {
        this.name = name;
        this.terms = terms;
    }

    public String getName() {
        return name;
    }

    public List<Term> getTerms() {
        return terms;
    }

    @Override
    public String toString() {
        return name + "(" + Utils.join(terms, ", ") + ")";
    }

    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) return false;

        RelationalAtom relationalAtom = (RelationalAtom) object;

        return relationalAtom.getName().equals(this.getName()) && relationalAtom.getTerms().equals(this.getTerms());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.terms);
    }
}
