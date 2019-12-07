package org.monarchinitiative.lirical.io;



import com.google.protobuf.util.JsonFormat;

import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PhenopacketImporterTest {

    @TempDir
    static Path tempDir;

    private static final String fakeVcfPath="/home/user/example.vcf";
    private static final String fakeGenomeAssembly = "GRCH_37";

    private static Phenopacket ppacket;

    private static String phenopacketAbsolutePathOfTempFile;

    private static Ontology ontology;

    private static OntologyClass ontologyClass(String id, String label ){
        return OntologyClass.newBuilder().
                setId(id).setLabel(label).
                build();
    }

    @BeforeAll
    static void init() throws IOException {

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
        HtsFile vcfFile = HtsFile.newBuilder().
               // setFile(File.newBuilder().setPath(fakeVcfPath).build())
                setUri(fakeVcfPath)
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
        BufferedWriter br = new BufferedWriter(new FileWriter(output.toAbsolutePath().toFile()));
        String jsonString = JsonFormat.printer().includingDefaultValueFields().print(tmppacket);
        br.write(jsonString);
        br.close();

        String phenopacketJsonString =  new String ( Files.readAllBytes( Paths.get(phenopacketAbsolutePathOfTempFile) ) );

        Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
        JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
        ppacket = phenoPacketBuilder.build();
        ClassLoader classLoader = PhenopacketImporterTest.class.getClassLoader();
        String hpoPath = Objects.requireNonNull(classLoader.getResource("hp.small.obo").getFile());
        ontology = OntologyLoader.loadOntology(new java.io.File(hpoPath));
    }

    @Test
    void testTempFileWritten( ) {
        java.io.File f = new java.io.File(phenopacketAbsolutePathOfTempFile);
        assertTrue(f.exists());
    }

    @Test
    void testInputOfTempPhenopacket() {
        assertNotNull(ppacket);
    }

    @Test
    void testNumberOfHpoTermsImported() {
        int expected =5;  // we have 4 non-negated (observed) HPO terms and one negated term
        assertEquals(expected,ppacket.getPhenotypicFeaturesCount());
    }

    @Test
    void testNumberOfObservedTerms() {
        PhenopacketImporter importer = new PhenopacketImporter(ppacket,ontology);
        int expecte=4;
        assertEquals(expecte,importer.getHpoTerms().size());
    }

    @Test
    void testNumberOfNegatedTerms() {
        PhenopacketImporter importer = new PhenopacketImporter(ppacket,ontology);
        int expected=1;
        assertEquals(expected,importer.getNegatedHpoTerms().size());
    }

    @Test
    void testIdentifyOfNegatedTerm() {
        TermId tid = TermId.of("HP:0031508");
        PhenopacketImporter importer = new PhenopacketImporter(ppacket,ontology);
        assertTrue(importer.getNegatedHpoTerms().contains(tid));
    }

    @Test
    void testIdentifyObservedTerm() {
        TermId tid = TermId.of("HP:0031508");
        PhenopacketImporter importer = new PhenopacketImporter(ppacket,ontology);
        assertFalse(importer.getHpoTerms().contains(tid)); // should not include negated term
        TermId tid2 = TermId.of("HP:0001510"); // this os one of the observed terms
        assertTrue(importer.getHpoTerms().contains(tid2));
    }

    @Test
    void testGetVcfFile() {
        PhenopacketImporter importer = new PhenopacketImporter(ppacket,ontology);
        assertTrue(importer.hasVcf());
        assertEquals(fakeVcfPath, importer.getVcfPath());
        assertEquals(fakeGenomeAssembly,importer.getGenomeAssembly());
    }




}
