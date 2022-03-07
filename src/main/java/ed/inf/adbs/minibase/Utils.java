package ed.inf.adbs.minibase;

import ed.inf.adbs.minibase.base.Query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {}

    public static String join(Collection<?> c, String delimiter) {
        return c.stream()
                .map(Object::toString)
                .collect(Collectors.joining(delimiter));
    }

    public static void outputQueryToFile(Query outputQuery, String outputFileName) throws IOException {
        File outFile = Paths.get(outputFileName).toFile();
        outFile.createNewFile();

        try (FileWriter writer = new FileWriter(outputFileName)) {
            writer.write(outputQuery.toString());
        }

    }
}

