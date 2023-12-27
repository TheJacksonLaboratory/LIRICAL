package org.monarchinitiative.lirical.cli.yaml;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.cli.TestResources;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class YamlParserTest {
    private static final Path TEST_YAML_DIR = TestResources.LIRICAL_TEST_BASE.resolve("yaml");
    // Paths to the example YAML files in src/test/resources/yaml/
    private static final Path example1path = TEST_YAML_DIR.resolve("example1.yml");
    private static final Path example2path = TEST_YAML_DIR.resolve("example2.yml");

    @Test
    public void testGetHpoIds() throws Exception {
        YamlConfig config = YamlParser.parse(example2path);
        String[] expected = {"HP:0001363", "HP:0011304", "HP:0010055"};
        List<String> hpos = config.getHpoIds();
        assertThat(hpos, hasSize(expected.length));
        assertThat(hpos, hasSize(3));
        assertThat(hpos, hasItems(expected));
    }

    @Test
    public void testNegatedHpoIds1() throws Exception {
        // example 1 has no negated HPOs
        YamlConfig config = YamlParser.parse(example1path);
        assertThat(config.getNegatedHpoIds(), is(empty()));
    }

    @Test
    public void testNegatedHpoIds2() throws Exception {
        // example 2 has one negated id
        YamlConfig config = YamlParser.parse(example2path);
        String termid = "HP:0001328"; // the negated term
        assertThat(config.getNegatedHpoIds(), hasItem(termid));
    }

}
