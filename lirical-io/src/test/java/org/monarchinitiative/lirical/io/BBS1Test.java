package org.monarchinitiative.lirical.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.io.analysis.*;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class tests the ingest of the BBS1.json (phenotpacket) and BBS1.yml files.
 * They should provide equivalent information (although the Phenopacket provides richer information)
 */
public class BBS1Test {
    private static YamlConfig yamlConfig;
    private static PhenopacketData DATA;

    private static final Path TEST_PHENOPACKET_DIR = TestResources.LIRICAL_TEST_BASE.resolve("phenopacket");
    private static final Path TEST_YAML_DIR = TestResources.LIRICAL_TEST_BASE.resolve("yaml");


    private final String expectedId = "IV-5/family A";
    private final Path expectedVcf=Path.of("/path/to/examples/BBS1.vcf");


    @BeforeAll
    public static void init() throws Exception {
        yamlConfig = YamlParser.parse(TEST_YAML_DIR.resolve("BBS1.yml"));

        try (InputStream is = new BufferedInputStream(new FileInputStream(TEST_PHENOPACKET_DIR.resolve("BBS1.json").toFile()))) {
            DATA = PhenopacketImporters.v1().read(is);
        }
    }

    @Test
    public void getIdPhenopacket() {
        assertEquals(expectedId,DATA.getSampleId());
    }

    @Test
    public void getIdYaml() {
        assertEquals(expectedId, yamlConfig.getSampleId());
    }

    @Test
    public void getGenomeAssemblyPhenopacket() {
        Optional<String> assemblyOptional = DATA.getGenomeAssembly();
        assertThat(assemblyOptional.isPresent(), equalTo(true));
        String expectedGenomeAssembly = "GRCh37";
        assertEquals(expectedGenomeAssembly, assemblyOptional.get());
    }

    @Test
    public void testGetVcfPhenopacket() {
        Optional<Path> vcfPath = DATA.getVcfPath();
        assertThat(vcfPath.isPresent(), equalTo(true));
        assertThat(vcfPath.get(), equalTo(expectedVcf));
    }

    @Test
    public void testGetVcfYaml() {
        Optional<Path> vcfOpt = yamlConfig.vcfPath();
        assertTrue(vcfOpt.isPresent());
        assertEquals(expectedVcf, vcfOpt.get());
    }

    @Test
    public void testGetHpoIdsYaml() {
        String [] expected = {"HP:0007843","HP:0001513","HP:0000608","HP:0000486"};
        List<String> termList = yamlConfig.getHpoIds();
        assertEquals(4,termList.size());
        assertEquals(termList.get(0),expected[0]);
        assertEquals(termList.get(1),expected[1]);
        assertEquals(termList.get(2),expected[2]);
        assertEquals(termList.get(3),expected[3]);
    }

    @Test
    public void testGetHpoIdsPhenopacket() {
        List<TermId> terms = DATA.getHpoTerms().toList();
        TermId expected1 = TermId.of("HP:0007843");
        TermId expected2 = TermId.of("HP:0001513");
        TermId expected3 = TermId.of("HP:0000608");
        TermId expected4 = TermId.of("HP:0000486");
        assertTrue(terms.contains(expected1));
        assertTrue(terms.contains(expected2));
        assertTrue(terms.contains(expected3));
        assertTrue(terms.contains(expected4));
    }

    @Test
    public void testGetExlucedTermYaml() {
        final String expected = "HP:0001328"; // only one excluded term
        final List<String> excludedlist = yamlConfig.getNegatedHpoIds();
        assertEquals(1,excludedlist.size());
        assertEquals(expected,excludedlist.get(0));
    }

    @Test
    public void testGetExcludedTermsPhenopacket() {
        TermId expected = TermId.of("HP:0001328"); // only one excluded term
        List<TermId> excluded = DATA.getNegatedHpoTerms().toList();
        assertEquals(1,excluded.size());
        assertEquals(expected,excluded.get(0));
    }


}
