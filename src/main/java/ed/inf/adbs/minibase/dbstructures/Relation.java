package ed.inf.adbs.minibase.dbstructures;

public class Relation {

    private String name;
//    private Schema schema;
    private String fileLocation;

    public Relation(String name, Schema schema, String fileLocation) {
        this.name = name;
//        this.schema = schema;
        this.fileLocation = fileLocation;
    }

//    public Schema getSchema() {
//        return schema;
//    }
//
//    public void setSchema(Schema schema) {
//        this.schema = schema;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }
}
