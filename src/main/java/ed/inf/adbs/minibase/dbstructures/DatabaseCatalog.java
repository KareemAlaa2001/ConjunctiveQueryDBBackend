package ed.inf.adbs.minibase.dbstructures;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps track of db information such as where files are located for the respective relations, different relationships' schemas etc.
 * Implemented as a singleton
 */
public class DatabaseCatalog {

    //  TODO manage way around Relation vs Schema
    private Map<Relation, String> fileMappings;

    public static DatabaseCatalog catalog = new DatabaseCatalog();

    private DatabaseCatalog() {}

    public DatabaseCatalog getCatalog() {
        return catalog;
    }

    public Map<Relation, String> getFileMappings() {
        return fileMappings;
    }

    public void setFileMappings(Map<Relation, String> fileMappings) {
        this.fileMappings = fileMappings;
    }

    //  TODO is this even necessary?
    public Schema getRelationSchema(Relation relation) {
        return relation.getSchema();
    }
}
