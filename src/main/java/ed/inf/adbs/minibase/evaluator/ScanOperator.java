package ed.inf.adbs.minibase.evaluator;

import ed.inf.adbs.minibase.base.Constant;
import ed.inf.adbs.minibase.dbstructures.Schema;
import ed.inf.adbs.minibase.dbstructures.Tuple;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements the scan operator which is reponsible for emitting the relevant Tuple(s) from the assigned table
 */
public class ScanOperator extends Operator {

    BufferedReader bufferedReader;
    String fileName;
    Schema schema;

    public ScanOperator(String fileName, Schema schema) throws FileNotFoundException {
        if (!dbFileValid(fileName,schema)) throw new IllegalArgumentException("Illegal dbfile passed in!");
        this.bufferedReader = new BufferedReader(new FileReader(fileName));
        this.schema = schema;
        this.fileName = fileName;
    }

    @Override
    public Tuple getNextTuple() throws IOException {

        String nextLine = bufferedReader.readLine();

        if (nextLine != null) {
            return this.parseTupleFromDBLine(nextLine);
        }
        else return null;
    }

    @Override
    public void reset() {
        try {
            this.bufferedReader = new BufferedReader(new FileReader(this.fileName));
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException("Somehow this was caught here and not in the constructor!");
        }
    }

    @Override
    public void dump() {
        String currLine;
        try {
            while ((currLine = bufferedReader.readLine()) != null) {
                System.out.println(currLine);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private Tuple parseTupleFromDBLine(String dbLine) {
        String[] cells = dbLine.split(",");
        if (this.schema.getDataTypes().size() != cells.length) throw new IllegalArgumentException("The number of columns and the number of classes parsed from the schema don't match!");


        return new Tuple(IntStream.range(0, cells.length)
                .mapToObj(i -> {
                    try {
                        Constructor<? extends Constant> constructor = this.schema.getDataTypes().get(i).getDeclaredConstructor(String.class);
                        return constructor.newInstance(cells[i]);
                    } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Something has gone wrong in the method invocation!");
                    }

                }).collect(Collectors.toList()));
    }

    private boolean dbFileValid(String dbFileName, Schema schema) {
        File dbFile = new File(dbFileName);
        int dotIndex = dbFile.getName().lastIndexOf(".");
        String extension =  dbFile.getName().substring(dotIndex+1);
        String relName =  dbFile.getName().substring(0,dotIndex);
        return (dbFile.isFile() && extension.equals("csv") && relName.equals(schema.getName()) && dbFile.getParentFile().getName().equals("files"));
    }
}
