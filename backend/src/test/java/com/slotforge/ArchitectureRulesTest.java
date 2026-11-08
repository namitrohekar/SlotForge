package com.slotforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static final Path PLANNING = Path.of("src/main/java/com/slotforge/planning");

    /** The engine stays plain Java, so all of it can be tested without Spring or a database. */
    @Test
    void theEngineImportsOnlyTheJdkAndItself() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String area : List.of("model", "graph", "algorithm", "allocation")) {
            try (Stream<Path> files = Files.walk(PLANNING.resolve(area))) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Files.readAllLines(file).stream()
                            .filter(line -> line.startsWith("import "))
                            .filter(line -> !line.startsWith("import java.") && !line.startsWith("import static java.")
                                    && !line.startsWith("import com.slotforge.planning."))
                            .forEach(line -> offenders.add(file.getFileName() + ": " + line));
                }
            }
        }

        assertThat(offenders).isEmpty();
    }
}
