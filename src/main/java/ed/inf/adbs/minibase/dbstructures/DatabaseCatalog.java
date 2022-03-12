package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.IntegerConstant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.StringConstant;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Keeps track of db information such as where files are located for the respective relations, different relationships' schemas etc.
 * Implemented as a singleton
 */
public class DatabaseCatalog {

    public static DatabaseCatalog catalog = null;

    //  stores a map to a relationName: relation
    private Map<String, Relation> relationMap;

    private Map<String, Schema> schemaMap;

    public static DatabaseCatalog getCatalog() {
        if (catalog != null) return catalog;
        catalog = new DatabaseCatalog();
        return catalog;
    }

    public void constructRelations(List<RelationalAtom> relationalAtoms, String databaseDir) throws IOException {
//        relationalAtoms.forEach(relationalAtom -> constructScan(relationalAtom.getName()));
        File inputFolder = new File(databaseDir + "files");

        File[] files = inputFolder.listFiles();

        File schemaFile = new File(databaseDir + "schema.txt");

        DatabaseCatalog catalog = DatabaseCatalog.getCatalog();

        //  first wanna construct the schema data, this is necessary Anyways
        this.extractSchemaFromFile(schemaFile);

        assert files != null;
        Arrays.stream(files).forEach(file -> catalog.initialiseIfNotPresent(file.getName(), databaseDir));
    }

    private void extractSchemaFromFile(File schemaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(schemaFile));

        String line;

        while ((line = reader.readLine()) != null ) {

            List<String> tableSpecList = Arrays.asList(line.split(" "));

            String relationName = tableSpecList.get(0);

            tableSpecList.remove(0);

            List<Class<? extends Constant>> relationTypes = tableSpecList.stream().map(this::getClassFromDBTypeString).collect(Collectors.toList());
            Schema classSchema = new Schema(relationName, relationTypes);
            this.getSchemaMap().put(relationName, classSchema);
        }
    }

    public void initialiseIfNotPresent(String relName, String databaseDir) {
        if (this.getRelationMap().containsKey(relName))
            return;

//        Relation relation = new Relation()
    }

    public Map<String, Relation> getRelationMap() {
        return relationMap;
    }

    public void setRelationMap(Map<String, Relation> relationMap) {
        this.relationMap = relationMap;
    }

    public Map<String, Schema> getSchemaMap() {
        return schemaMap;
    }

    private Class<? extends Constant> getClassFromDBTypeString(String typeString) {
        if (!(typeString.equals("string") || typeString.equals("int"))) throw new IllegalArgumentException("Unsupported type detected!");

        return (typeString.equals("string")) ? StringConstant.class : IntegerConstant.class;
    }
}
