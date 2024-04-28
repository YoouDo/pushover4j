package de.kleinkop.pushover4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {
    public static String readResourceAsString(String resourceName) {
        try {
            final Path path = Paths.get(TestUtils.class.getResource(resourceName).toURI());
            String result;
            try (Stream<String> lines = Files.lines(path)) {
                result = lines.collect(Collectors.joining("\n"));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Could not read resource " + resourceName, e);
        }
    }
}
