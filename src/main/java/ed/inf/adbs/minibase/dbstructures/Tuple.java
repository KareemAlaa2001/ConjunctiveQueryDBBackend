package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;

import java.util.List;

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
}
