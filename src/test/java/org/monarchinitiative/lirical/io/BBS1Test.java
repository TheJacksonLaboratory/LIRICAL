package org.monarchinitiative.lirical.io;

import com.google.protobuf.util.JsonFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class tests the ingest of the BBS1.json (phenotpacket) and BBS1.yml files.
 * They should provide equivalent information (although the Phenopacket provides richer information)
 */
class BBS1Test {
    private static YamlParser yamlparser;
    private static PhenopacketImporter phenopacketimporter;

    private final String expectedId = "IV-5/family A";
    private final String expectedGenomeAssembly = "GRCh37";
    private final String expectedVcf="/path/to/examples/BBS1.vcf";
    private final String expectedExomiser="/path/to/exomiser_data/1802_hg19";
    private static Ontology ontology = Mockito.mock(Ontology.class);


    @BeforeAll
    static void init() throws IOException {
        ClassLoader classLoader = BBS1Test.class.getClassLoader();
        URL resource = classLoader.getResource("yaml/BBS1.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find BBS1.yml file");
        }
        String yamlPath = resource.getFile();
        yamlparser = new YamlParser(yamlPath);

        resource = classLoader.getResource("phenopacket/BBS1.json");
        if (resource==null){
            throw new FileNotFoundException("Could not find BBS1.json file");
        }

        String phenopacketJsonString =  new String ( Files.readAllBytes( Paths.get(resource.getFile()) ) );

        Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
        JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
        Phenopacket ppacket = phenoPacketBuilder.build();
        phenopacketimporter = new PhenopacketImporter(ppacket,ontology);
    }

    @Test
    void getIdPhenopacket() {
        assertEquals(expectedId,phenopacketimporter.getSamplename());
    }

    @Test
    void getIdYaml() {
        assertEquals(expectedId,yamlparser.getSampleId());
    }

    @Test
    void getGenomeAssemblyPhenopacket() {
        assertEquals(expectedGenomeAssembly,phenopacketimporter.getGenomeAssembly());
    }

    @Test
    void testGetGenomeAssemblyYaml() {
        assertEquals(expectedGenomeAssembly,yamlparser.getGenomeAssembly());
    }

    @Test
    void testGetVcfPhenopacket() {
        assertEquals(expectedVcf,phenopacketimporter.getVcfPath());
    }

    @Test
    void testGetVcfYaml() {
        Optional<String> vcfOpt = yamlparser.getOptionalVcfPath();
        assertTrue(vcfOpt.isPresent());
        assertEquals(expectedVcf,vcfOpt.get());
    }

    /**
     * Note that the YAML Parser removes the trailing slash of the exomiser data directory path, if present.
     */
    @Test
    void testGetExomiserPathYaml() {
        assertEquals(expectedExomiser,yamlparser.getExomiserDataDir());
    }

    @Test
    void testGetHpoIdsYaml() {
        String [] expected = {"HP:0007843","HP:0001513","HP:0000608","HP:0000486"};
        List<String> termList = yamlparser.getHpoTermList();
        assertEquals(4,termList.size());
        assertEquals(termList.get(0),expected[0]);
        assertEquals(termList.get(1),expected[1]);
        assertEquals(termList.get(2),expected[2]);
        assertEquals(termList.get(3),expected[3]);
    }

    @Test
    void testGetHpoIdsPhenopacket() {
        List<TermId> terms = phenopacketimporter.getHpoTerms();
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
    void testGetExlucedTermYaml() {
        final String expected = "HP:0001328"; // only one excluded term
        final List<String> excludedlist = yamlparser.getNegatedHpoTermList();
        assertEquals(1,excludedlist.size());
        assertEquals(expected,excludedlist.get(0));
    }

    @Test
    void testGetExcludedTermsPhenopacket() {
        TermId expected = TermId.of("HP:0001328"); // only one excluded term
        List<TermId> excluded = phenopacketimporter.getNegatedHpoTerms();
        assertEquals(1,excluded.size());
        assertEquals(expected,excluded.get(0));
    }

    @Test
    void testGetPrefixYaml() {
        final String expectedPrefix = "BBS1";
        assertEquals(expectedPrefix,yamlparser.getPrefix());
    }



}
