package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.base.RelationalAtom;
import ed.inf.adbs.minibase.dbstructures.Schema;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements the scan operator which is responsible for emitting all of the Tuple(s) from the assigned table
 */
public class ScanOperator extends Operator {

    BufferedReader bufferedReader;
    String fileName;
    Schema schema;
    RelationalAtom baseRelationalAtom;

    /**
     * ScanOperator constructor which checks that the given file name matches the expected location and that its name matches the name of the associated relation in the schema
     *
     * @param fileName the relative filename and path for the csv file containing the tuples in the table being scanned
     * @param schema the schema entry for the table in question
     * @param baseRelationalAtom the base relational atom from the query on which this scan is being evaluated
     * @throws FileNotFoundException thrown if the passed in filename does not correspond to an existing file in the filesystem
     */
    public ScanOperator(String fileName, Schema schema, RelationalAtom baseRelationalAtom) throws FileNotFoundException {
        if (!dbFileValid(fileName,schema)) throw new IllegalArgumentException("Illegal dbfile passed in!");
        this.bufferedReader = new BufferedReader(new FileReader(fileName));
        this.schema = schema;
        this.fileName = fileName;
        this.baseRelationalAtom = baseRelationalAtom;
    }

    //  parses and returns the next line in the table's csv as a tuple
    @Override
    public Tuple getNextTuple() throws IOException {

        String nextLine = bufferedReader.readLine();

        if (nextLine != null) {
            return this.parseTupleFromDBLine(nextLine);
        }
        else return null;
    }

    //  resets the operator state by resetting the readers reading the file
    @Override
    public void reset() {
        try {
            this.bufferedReader = new BufferedReader(new FileReader(this.fileName));
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException("Somehow this was caught here and not in the constructor!");
        }
    }

    /**
     * parses a csv line into a tuple using the object's associated schema.
     * This is done by iterating over the cells obtained from splitting the line on the commas, then creating the respective constant type using the relevant entry in the schema.
     *
     * @param dbLine the csv line to take in as a string-based input
     * @return the resultant tuple from parsing this line
     */
    private Tuple parseTupleFromDBLine(String dbLine) {
        String[] cells = dbLine.split(",");
        if (this.schema.getDataTypes().size() != cells.length) throw new IllegalArgumentException("The number of columns and the number of classes parsed from the schema don't match!");


        //  utilises the list of classnames representing the sequence of column types in the schema to instantiate the relevant Constant subclass given the cell content.
        return new Tuple(IntStream.range(0, cells.length)
                .mapToObj(i -> {
                    try {
                        //  utilises the String-based constructor available in both Constant subclasses to instantiate the contents of cell i.
                        //  Calls the constructor of the relevant entry in the column data types list in the schema
                        Constructor<? extends Constant> constructor = this.schema.getDataTypes().get(i).getDeclaredConstructor(String.class);
                        return constructor.newInstance(cells[i].trim().replaceAll("'", ""));
                    } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Something has gone wrong in the method invocation!");
                    }

                }).collect(Collectors.toList()));
    }

    //  checks that the passed in filename corresponds to a csv with the same name as the name of the table in the schema
    private boolean dbFileValid(String dbFileName, Schema schema) {
        File dbFile = new File(dbFileName);
        int dotIndex = dbFile.getName().lastIndexOf(".");
        String extension =  dbFile.getName().substring(dotIndex+1);
        String relName =  dbFile.getName().substring(0,dotIndex);
        return (dbFile.isFile() && extension.equals("csv") && relName.equals(schema.getName()) && dbFile.getParentFile().getName().equals("files"));
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public void setBufferedReader(BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public RelationalAtom getBaseRelationalAtom() {
        return baseRelationalAtom;
    }

    public void setBaseRelationalAtom(RelationalAtom baseRelationalAtom) {
        this.baseRelationalAtom = baseRelationalAtom;
    }
}
