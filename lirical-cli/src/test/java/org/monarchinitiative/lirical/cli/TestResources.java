package org.monarchinitiative.lirical.cli;

import java.nio.file.Path;

/**
 * Utility class with lazily-loaded resources for testing
 */
public class TestResources {

    public static final Path TEST_BASE = Path.of("src/test/resources");
    public static final Path LIRICAL_TEST_BASE = TestResources.TEST_BASE.resolve("org").resolve("monarchinitiative").resolve("lirical").resolve("cli");

    private TestResources() {
    }
}
