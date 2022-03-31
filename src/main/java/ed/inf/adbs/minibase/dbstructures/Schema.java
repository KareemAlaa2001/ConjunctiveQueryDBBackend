package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;

import java.util.List;

//  maintains a relation's schema by keeping track of the relation name and the column's types
public class Schema {

    String name;
    List<Class<? extends Constant>> dataTypes;

    public Schema(String name, List<Class<? extends Constant>> dataTypes) {
        this.name = name;
        this.dataTypes = dataTypes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Class<? extends Constant>> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<Class<? extends Constant>> dataTypes) {
        this.dataTypes = dataTypes;
    }
}
