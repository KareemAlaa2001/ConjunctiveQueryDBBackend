package ed.inf.adbs.minibase.base;

import java.util.Objects;

public class StringConstant extends Constant {
    private String value;

    public StringConstant(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "'" + value + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        else return ((StringConstant) o).getValue().equals(this.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }
}