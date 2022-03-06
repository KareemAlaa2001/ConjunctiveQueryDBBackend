package ed.inf.adbs.minibase.base;

import java.util.Objects;

public class Variable extends Term {
    private String name;

    public Variable(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) return false;

        return ((Variable) object).getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
