package org.monarchinitiative.lirical.io.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.io.TestResources;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test class tests an ingest of the BBS1 phenopacket.
 */
public class BBS1Test {
    private static PhenopacketData DATA;
    private static final Path TEST_PHENOPACKET_DIR = TestResources.LIRICAL_TEST_BASE.resolve("analysis");


    @BeforeAll
    public static void init() throws Exception {
        try (InputStream is = new BufferedInputStream(new FileInputStream(TEST_PHENOPACKET_DIR.resolve("BBS1.json").toFile()))) {
            DATA = PhenopacketImporters.v1().read(is);
        }
    }

    @Test
    public void getIdPhenopacket() {
        assertEquals("IV-5/family A", DATA.sampleId());
    }

    @Test
    public void getGenomeAssemblyPhenopacket() {
        Optional<String> assemblyOptional = DATA.genomeAssembly();
        assertThat(assemblyOptional.isPresent(), equalTo(true));
        String expectedGenomeAssembly = "GRCh37";
        assertEquals(expectedGenomeAssembly, assemblyOptional.get());
    }

    @Test
    public void testGetVcfPhenopacket() {
        String vcfPath = DATA.vcf();
        assertThat(vcfPath, is(notNullValue()));
        assertThat(vcfPath, equalTo("file:/path/to/examples/BBS1.vcf"));
    }

    @Test
    public void testGetHpoIdsPhenopacket() {
        List<TermId> terms = DATA.presentHpoTermIds().toList();

        assertTrue(terms.contains(TermId.of("HP:0007843")));
        assertTrue(terms.contains(TermId.of("HP:0001513")));
        assertTrue(terms.contains(TermId.of("HP:0000608")));
        assertTrue(terms.contains(TermId.of("HP:0000486")));
    }

    @Test
    public void testGetExcludedTermsPhenopacket() {

        List<TermId> excluded = DATA.excludedHpoTermIds().toList();

        assertEquals(1,excluded.size()); // only one excluded term
        assertEquals(TermId.of("HP:0001328"),excluded.get(0));
    }


}
