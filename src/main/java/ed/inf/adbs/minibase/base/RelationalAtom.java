package ed.inf.adbs.minibase.base;

import ed.inf.adbs.minibase.Utils;

import javax.management.relation.Relation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        if (this == object) return true;

        if (object == null) return false;

        if (getClass() != object.getClass()) return false;

        RelationalAtom relationalAtom = (RelationalAtom) object;

        return relationalAtom.getName().equals(this.getName()) && relationalAtom.getTerms().equals(this.getTerms());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.terms);
    }
}
