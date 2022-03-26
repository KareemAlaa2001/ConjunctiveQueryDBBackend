package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Tuple {
    private List<Constant> fields;

    public Tuple(List<Constant> fields) {
        this.fields = fields;
    }

    public List<Constant> getFields() {
        return fields;
    }

    public void setFields(List<Constant> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return this.fields.stream().map(Constant::toString).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return fields.equals(tuple.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }
}
