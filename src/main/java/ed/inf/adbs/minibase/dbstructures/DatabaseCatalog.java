package ed.inf.adbs.minibase.dbstructures;

import java.util.Map;

/**
 * Keeps track of db information such as where files are located for the respective relations, different relationships' schemas etc.
 * Implemented as a singleton
 */
public class DatabaseCatalog {

    public static DatabaseCatalog catalog = null;

    public DatabaseCatalog getCatalog() {
        if (catalog != null) return catalog;
        catalog = new DatabaseCatalog();
        return catalog;
    }

    private Map<String, Relation> relationMap;

    public Map<String, Relation> getRelationMap() {
        return relationMap;
    }

    public void setRelationMap(Map<String, Relation> relationMap) {
        this.relationMap = relationMap;
    }

    //  TODO is this even necessary?
    public Schema getRelationSchema(Relation relation) {
        return relation.getSchema();
    }

    public void initialiseIfNotPresent(String relName) {
        if (!this.getRelationMap().containsKey(relName)) initialiseRelation(relName);
    }

    private void initialiseRelation(String relName) {
//        Relation relation = new Relation();
    }
}
