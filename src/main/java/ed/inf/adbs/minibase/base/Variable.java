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
        if (this == object) return true;

        if (object == null) return false;

        if (getClass() != object.getClass()) return false;

        Variable variable = (Variable) object;

        return variable.getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
