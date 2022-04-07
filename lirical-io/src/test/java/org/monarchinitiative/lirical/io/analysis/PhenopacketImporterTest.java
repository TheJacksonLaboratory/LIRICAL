package org.monarchinitiative.lirical.io.analysis;



import com.google.protobuf.util.JsonFormat;

import org.junit.jupiter.api.Assertions;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PhenopacketImporterTest {

    @TempDir
    static Path tempDir;

    private static final String fakeGenomeAssembly = "GRCH_37";

    private static Phenopacket ppacket;

    private static String phenopacketAbsolutePathOfTempFile;

    private static OntologyClass ontologyClass(String id, String label ){
        return OntologyClass.newBuilder().
                setId(id).setLabel(label).
                build();
    }

    @BeforeAll
    public static void init() throws IOException {

        OntologyClass homoSapiens = ontologyClass("NCBITaxon:9606","Homo sapiens");
        Gene kmt2d = Gene.newBuilder().setId("ENTREZ:8085").setSymbol("KMT2D").build();
        Individual subject = Individual.newBuilder().
                setId("proband a").
                setAgeAtCollection(Age.newBuilder().setAge("P3M").build()).
                setSex(Sex.MALE).
                setTaxonomy(homoSapiens).
                build();
        Evidence evi = Evidence.newBuilder().
                setEvidenceCode(ontologyClass("ECO:0000033","author statement supported by traceable reference")).
                setReference(ExternalReference.newBuilder().setId("PMID:30509212")).
                build();
        PhenotypicFeature depressedNasalTip = PhenotypicFeature.newBuilder().
                setType(ontologyClass("HP:0000437","Depressed nasal tip")).
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
        OntologyClass heterozygous = ontologyClass( "GENO:0000135", "heterozygous");
        Variant var = Variant.newBuilder().setVcfAllele(allele).setZygosity(heterozygous).build();
        Disease kabuki2 = Disease.newBuilder().setTerm(ontologyClass("OMIM:300867","KABUKI SYNDROME 2")).build();

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
        Path output = Files.createFile(tempDir.resolve("temp_output.txt"));
        phenopacketAbsolutePathOfTempFile = output.toAbsolutePath().toString();
        try (BufferedWriter br = Files.newBufferedWriter(output)) {
            String jsonString = JsonFormat.printer().includingDefaultValueFields().print(tmppacket);
            br.write(jsonString);
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(phenopacketAbsolutePathOfTempFile))) {
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(reader, phenoPacketBuilder);
            ppacket = phenoPacketBuilder.build();
        }
    }

    @Test
    public void testTempFileWritten( ) {
        File f = new File(phenopacketAbsolutePathOfTempFile);
        Assertions.assertTrue(f.exists());
    }

    @Test
    public void testInputOfTempPhenopacket() {
        Assertions.assertNotNull(ppacket);
    }

    @Test
    public void testNumberOfHpoTermsImported() {
        int expected =5;  // we have 4 non-negated (observed) HPO terms and one negated term
        Assertions.assertEquals(expected,ppacket.getPhenotypicFeaturesCount());
    }

    @Test
    public void testNumberOfObservedTerms() {
        PhenopacketImporter importer = PhenopacketImporter.of(ppacket);
        int expected=4;
        Assertions.assertEquals(expected,importer.getHpoTerms().toList().size());
    }

    @Test
    public void testNumberOfNegatedTerms() {
        PhenopacketImporter importer = PhenopacketImporter.of(ppacket);
        int expected=1;
        Assertions.assertEquals(expected,importer.getNegatedHpoTerms().toList().size());
    }

    @Test
    public void testIdentifyOfNegatedTerm() {
        TermId tid = TermId.of("HP:0031508");
        PhenopacketImporter importer = PhenopacketImporter.of(ppacket);
        Assertions.assertTrue(importer.getNegatedHpoTerms().toList().contains(tid));
    }

    @Test
    public void testIdentifyObservedTerm() {
        TermId tid = TermId.of("HP:0031508");
        PhenopacketImporter importer = PhenopacketImporter.of(ppacket);
        assertFalse(importer.getHpoTerms().toList().contains(tid)); // should not include negated term
        TermId tid2 = TermId.of("HP:0001510"); // this os one of the observed terms
        Assertions.assertTrue(importer.getHpoTerms().toList().contains(tid2));
    }

    @Test
    public void testGetVcfFile() {
        PhenopacketImporter importer = PhenopacketImporter.of(ppacket);
        Assertions.assertTrue(importer.hasVcf());

        Optional<Path> vcfPath = importer.getVcfPath();
        assertThat(vcfPath.isPresent(), equalTo(true));
        Assertions.assertEquals(Path.of("/home/user/example.vcf"), vcfPath.get());

        Optional<String> assembly = importer.getGenomeAssembly();
        assertThat(assembly.isPresent(), equalTo(true));
        Assertions.assertEquals(fakeGenomeAssembly, assembly.get());
    }




}
