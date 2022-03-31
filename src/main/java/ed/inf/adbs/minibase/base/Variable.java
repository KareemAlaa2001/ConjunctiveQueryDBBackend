package ed.inf.adbs.minibase.base;

import java.util.Objects;

//  the Variable class, which extends basc terms by adding a name and having an equals and hashcode that check over the value of this name as well as the classname
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
