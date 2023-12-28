package org.monarchinitiative.lirical.io.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.io.TestResources;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PhenopacketV2ImporterTest {

    private PhenopacketV2Importer instance;

    @BeforeEach
    public void setUp() {
        instance = PhenopacketV2Importer.instance();
    }

    @ParameterizedTest
    @CsvSource({
            "pfeiffer.v2.json",
            "pfeiffer.v2.pb",
            "pfeiffer.v2.yaml"
    })
    public void readPfeiffer(String phenopacketName) throws Exception {
        File phenopacketPath = TestResources.LIRICAL_TEST_BASE.resolve("analysis").resolve(phenopacketName).toFile();
        PhenopacketData data;
        try (InputStream is = new BufferedInputStream(new FileInputStream(phenopacketPath))) {
            data = instance.read(is);
        }

        assertThat(data.sampleId(), equalTo("II:3/Family 2"));
        assertThat(data.parseAge().isPresent(), equalTo(true));
        assertThat(data.parseAge().get(), equalTo(Age.of(12, 3, 0)));
        assertThat(data.parseSex().isPresent(), equalTo(true));
        assertThat(data.parseSex().get(), equalTo(Sex.UNKNOWN));

        assertThat(data.presentHpoTermIds().map(TermId::getValue).toList(), hasItems("HP:0009719", "HP:0010614", "HP:0012736", "HP:0001250", "HP:0100804", "HP:0009721", "HP:0012733"));
        assertThat(data.excludedHpoTermIds().count(), equalTo(0L));

        assertThat(data.diseaseIds().stream().map(TermId::getValue).toList(), hasItems("OMIM:191100"));

        assertThat(data.vcf(), equalTo("file:/path/to/Pfeiffer.vcf"));

        assertThat(data.genomeAssembly().isPresent(), equalTo(true));
        assertThat(data.genomeAssembly().get(), equalTo("GRCh37"));
    }
}