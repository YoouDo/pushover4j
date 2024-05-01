package de.kleinkop.pushover4j;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestUtils {
    public static URL getResource(String resource) {
        return Objects.requireNonNull(TestUtils.class.getResource(resource));
    }

    public static String readResourceAsString(String resourceName) {
        try {
            final Path path = Paths.get(getResource(resourceName).toURI());
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
