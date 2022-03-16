package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.IntegerConstant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.base.StringConstant;

import java.io.*;
import java.util.*;
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
        catalog.schemaMap = new HashMap<>();
        catalog.relationMap = new HashMap<>();
        return catalog;
    }

    public void constructRelations(String databaseDir) throws IOException {

        File schemaFile = new File(databaseDir + "schema.txt");

        //  first wanna construct the schema data, this is necessary Anyways
        this.extractSchemaFromFile(schemaFile);

        File csvDirectory = new File(databaseDir + "files");

        File[] files = csvDirectory.listFiles();

        Arrays.stream(files).forEach(file -> {
            String fileName = file.getName();
            String relationName = fileName.substring(0,fileName.lastIndexOf("."));

            if (!this.getSchemaMap().containsKey(relationName)) throw new IllegalArgumentException("Extracted a CSV file with a name that doesnt match any of the parsed schema relation names!");

            Relation dbRelation = new Relation(relationName, this.schemaMap.get(relationName), file.getPath());
            this.getRelationMap().put(relationName, dbRelation);
        });
    }

    private void extractSchemaFromFile(File schemaFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(schemaFile));

        String line;

        while ((line = reader.readLine()) != null ) {

            List<String> tableSpecList = Arrays.asList(line.split(" "));

            String relationName = tableSpecList.get(0);

            List<String> typeList = new ArrayList<>(tableSpecList.subList(1, tableSpecList.size()));

            List<Class<? extends Constant>> relationTypes = typeList.stream().map(this::getClassFromDBTypeString).collect(Collectors.toList());
            Schema classSchema = new Schema(relationName, relationTypes);
            this.getSchemaMap().put(relationName, classSchema);
        }

        this.getSchemaMap().forEach((key, val) -> System.out.println(key + ": " + "(" + val.getDataTypes() + ")"));
    }

    public Map<String, Schema> getSchemaMap() {
        return schemaMap;
    }

    private Class<? extends Constant> getClassFromDBTypeString(String typeString) {
        if (!(typeString.equals("string") || typeString.equals("int"))) throw new IllegalArgumentException("Unsupported type detected!");

        return (typeString.equals("string")) ? StringConstant.class : IntegerConstant.class;
    }

    public Map<String, Relation> getRelationMap() {
        return relationMap;
    }

    public void setRelationMap(Map<String, Relation> relationMap) {
        this.relationMap = relationMap;
    }
}
