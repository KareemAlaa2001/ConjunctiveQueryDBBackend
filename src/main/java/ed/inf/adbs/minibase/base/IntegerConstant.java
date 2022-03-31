package ed.inf.adbs.minibase.base;

import java.util.Objects;

//  a class to represent integer constants in queries and tuples, has implementations of toString, equals, hashCode and compareTo which utilise the value of the integer
public class IntegerConstant extends Constant implements Comparable<IntegerConstant> {

    private Integer value;

    public IntegerConstant(Integer value) {
        this.value = value;
    }

    public IntegerConstant(String value) {
        this.value = Integer.valueOf(value);
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;

        else return ((IntegerConstant) o).getValue().equals(this.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    @Override
    public int compareTo(IntegerConstant integerConstant) {
        return this.getValue().compareTo(integerConstant.getValue());
    }
}
