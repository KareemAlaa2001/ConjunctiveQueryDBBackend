package ed.inf.adbs.minibase.evaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

//  singleton filewriter that handles writing to the output file.
public class OutputWriter {

    private static FileWriter fileWriter;

    public static FileWriter getFileWriter() {
        if (fileWriter != null)
            return fileWriter;

        else throw new UnsupportedOperationException("The file writer still hasn't been initialised!! ");
    }

    public static void initialiseOutputWriter(String outputFileName) throws IOException {
        File outFile = Paths.get(outputFileName).toFile();
        outFile.createNewFile();

        fileWriter = new FileWriter(outputFileName);
    }

    private OutputWriter() {

    }

    public static boolean outputWriterInitialised() {
        return fileWriter != null;
    }
}
