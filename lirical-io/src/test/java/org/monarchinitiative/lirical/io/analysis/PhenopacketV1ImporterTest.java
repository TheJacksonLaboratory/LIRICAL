package org.monarchinitiative.lirical.io.analysis;


import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PhenopacketV1ImporterTest {
    private static byte[] DATA;
    private static final String fakeGenomeAssembly = "GRCH_37";

    private static OntologyClass ontologyClass(String id, String label) {
        return OntologyClass.newBuilder().
                setId(id).setLabel(label).
                build();
    }

    private PhenopacketData data;

    @BeforeAll
    public static void init() throws IOException {
        OntologyClass homoSapiens = ontologyClass("NCBITaxon:9606", "Homo sapiens");
        Gene kmt2d = Gene.newBuilder().setId("ENTREZ:8085").setSymbol("KMT2D").build();
        Individual subject = Individual.newBuilder().
                setId("proband a").
                setAgeAtCollection(Age.newBuilder().setAge("P3M").build()).
                setSex(Sex.MALE).
                setTaxonomy(homoSapiens).
                build();
        Evidence evi = Evidence.newBuilder().
                setEvidenceCode(ontologyClass("ECO:0000033", "author statement supported by traceable reference")).
                setReference(ExternalReference.newBuilder().setId("PMID:30509212")).
                build();
        PhenotypicFeature depressedNasalTip = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0000437", "Depressed nasal tip")).
                addEvidence(evi).
                build();
        PhenotypicFeature growthDelay = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0001510", "Growth delay")).
                addEvidence(evi).
                build();
        PhenotypicFeature lowSetEars = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0000369", "Low-set ears")).
                addEvidence(evi).
                build();
        PhenotypicFeature breechPresentation = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0001623", "Breech presentation")).
                addEvidence(evi).
                build();
        PhenotypicFeature abnormalThyroidHormone = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0031508", "Abnormal thyroid hormone level")).
                setNegated(true).
                addEvidence(evi).
                build();

        VcfAllele allele = VcfAllele.newBuilder().
                setGenomeAssembly(fakeGenomeAssembly).
                setChr("17").
                setPos(29665775).
                setRef("T").
                setAlt("A").
                build();
        HtsFile vcfFile = HtsFile.newBuilder()
                .setUri("file:/home/user/example.vcf")
                .setHtsFormat(HtsFile.HtsFormat.VCF)
                .setGenomeAssembly(fakeGenomeAssembly)
                .build();
        OntologyClass heterozygous = ontologyClass("GENO:0000135", "heterozygous");
        Variant var = Variant.newBuilder().setVcfAllele(allele).setZygosity(heterozygous).build();
        Disease kabuki2 = Disease.newBuilder().setTerm(ontologyClass("OMIM:300867", "KABUKI SYNDROME 2")).build();

        Phenopacket tmppacket = Phenopacket.newBuilder().
                setSubject(subject).
                addPhenotypicFeatures(depressedNasalTip).
                addPhenotypicFeatures(growthDelay).
                addPhenotypicFeatures(lowSetEars).
                addPhenotypicFeatures(breechPresentation).
                addPhenotypicFeatures(abnormalThyroidHormone).
                addGenes(kmt2d).
                addVariants(var).
                addHtsFiles(vcfFile).
                addDiseases(kabuki2).
                build();

        // arrange
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            String jsonString = JsonFormat.printer().includingDefaultValueFields().print(tmppacket);
            os.write(jsonString.getBytes(StandardCharsets.UTF_8));
            DATA = os.toByteArray();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        PhenopacketV1Importer instance = PhenopacketV1Importer.instance();
        data = instance.read(new ByteArrayInputStream(DATA));
    }

    @Test
    public void testNumberOfObservedTerms() {
        Assertions.assertEquals(4, data.getHpoTerms().count());
    }

    @Test
    public void testNumberOfNegatedTerms() {
        Assertions.assertEquals(1, data.getNegatedHpoTerms().count());
    }

    @Test
    public void testIdentifyOfNegatedTerm() {
        TermId tid = TermId.of("HP:0031508");
        Assertions.assertTrue(data.getNegatedHpoTerms().anyMatch(t -> t.equals(tid)));
    }

    @Test
    public void testIdentifyObservedTerm() {
        TermId tid = TermId.of("HP:0031508");
        assertFalse(data.getHpoTerms().toList().contains(tid)); // should not include negated term
        TermId tid2 = TermId.of("HP:0001510"); // this os one of the observed terms
        Assertions.assertTrue(data.getHpoTerms().toList().contains(tid2));
    }

    @Test
    public void testGetVcfFile() {
        Assertions.assertTrue(data.getVcfPath().isPresent());

        Optional<Path> vcfPath = data.getVcfPath();
        assertThat(vcfPath.isPresent(), equalTo(true));
        Assertions.assertEquals(Path.of("/home/user/example.vcf"), vcfPath.get());

        Optional<String> assembly = data.getGenomeAssembly();
        assertThat(assembly.isPresent(), equalTo(true));
        Assertions.assertEquals(fakeGenomeAssembly, assembly.get());
    }

}
