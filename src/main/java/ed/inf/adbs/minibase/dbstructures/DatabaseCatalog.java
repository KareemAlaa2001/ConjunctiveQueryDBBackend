package ed.inf.adbs.minibase.dbstructures;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.IntegerConstant;
import ed.inf.adbs.minibase.base.StringConstant;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Keeps track of db information such as where files are located for the respective relations, different relationships' schemas etc.
 * Implemented as a singleton
 */
public class DatabaseCatalog {

    //  the global singleton instance
    public static DatabaseCatalog catalog = null;

    //  stores a map to a relationName: relation
    private Map<String, Relation> relationMap;

    private Map<String, Schema> schemaMap;

    //  returns if the instance if it is already initialised. Otherwise initialises a new instance and returns it.
    public static DatabaseCatalog getCatalog() {
        if (catalog != null) return catalog;
        catalog = new DatabaseCatalog();
        catalog.schemaMap = new HashMap<>();
        catalog.relationMap = new HashMap<>();
        return catalog;
    }

    /**
     * Parses through the schema file and the table files in the given database directory.
     * For the former, it constructs the schema map, which encodes a map from relation name to its schema.
     * For the latter, it utilises the entry in the schema map to construct the relevant relation object.
     *
     * @param databaseDir the directory path of the database directory
     * @throws IOException thrown in case any issues happen with file construction
     */
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

    /**
     * contstructs the catalog's schema map using the given schema file, by creating a shcema object representing each line.
     * The first entry from splitting the line over the spaces is counted as the relation name, while the rest encode the types of the tuple terms
     * @param schemaFile
     * @throws IOException
     */
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
    }

    public Map<String, Schema> getSchemaMap() {
        return schemaMap;
    }

    //  given a string representing the column type, this returns the relevant Constant subclass
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
