package org.monarchinitiative.lirical.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatioTest;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.WINDOWS;


class YamlParserTest {

    private static String demo1path;
    private static String demo3path;

    @BeforeAll
    static void init() throws FileNotFoundException{
        ClassLoader classLoader = YamlParserTest.class.getClassLoader();
        URL resource = classLoader.getResource("yaml/demo1.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find demo1.yml file");
        }
        demo1path = resource.getFile();
        resource = classLoader.getResource("yaml/demo3.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find demo3.yml file");
        }
        demo3path = resource.getFile();
    }

    @Test
    void testDemo1YamlFile() throws LiricalException {
        YamlParser parser = new YamlParser(demo1path);
        String expected = String.format("%s%s%s","data", File.separator,"hp.obo");
        assertEquals(expected,parser.getHpOboPath());
        expected = String.format("%s%s%s","data", File.separator,"mim2gene_medgen");
        assertEquals(expected,parser.getMedgen());
        expected = String.format("%s%s%s","data", File.separator,"Homo_sapiens_gene_info.gz");
        assertEquals(expected,parser.getGeneInfo());
        expected = String.format("%s%s%s","data", File.separator,"phenotype.hpoa");
        assertEquals(expected,parser.phenotypeAnnotation());
    }

    /**
     * This test is disabled on windows because it depends on the File separator (/ vs \).
     */
    @Test @DisabledOnOs(WINDOWS)
    void testMvStorePath() {
        YamlParser parser = new YamlParser(demo1path);
        String expected="/home/robinp/data/exomiserdata/1802_hg19/1802_hg19_variants.mv.db";
        assertEquals(expected,parser.getMvStorePath());
    }

    @Test
    void testBadMvStorePath() {
        String badPath="/nonexistant/path/1802_hg19_variants.mv.db";
        Assertions.assertThrows(PhenolRuntimeException.class, () -> {
            YamlParser parser = new YamlParser(badPath);
        });
    }

    /**
     * In the YAML file, the exomiser path is given as
     * exomiser: /home/robinp/data/exomiserdata/1811_hg19.
     * Here we test if we can extract the correct mvstore and Jannovar files
     * This test is disabled on windows because it depends on the File separator (/ vs \).
     */
    @Test @DisabledOnOs(WINDOWS)
    void testExomiserData()throws LiricalException {
        YamlParser parser = new YamlParser(demo1path);
        String expectedMvStore = "/home/robinp/data/exomiserdata/1802_hg19/1802_hg19_variants.mv.db";
        assertEquals(expectedMvStore,parser.getMvStorePath());
        String expectedJannovar = "/home/robinp/data/exomiserdata/1802_hg19/1802_hg19_transcripts_refseq.ser";
        assertEquals(expectedJannovar,parser.jannovarFile());
    }

    /**
     * The default path for the background frequency is src/main/resources/background/ but it can be
     * overrridden in the YAML file
     */
    @Test
    void testBackFrequencyPath() {
        YamlParser parser = new YamlParser(demo1path);
        String expected="data/background-hg38.txt";
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertTrue(backgroundOpt.isPresent());
        assertEquals(expected,backgroundOpt.get());
    }

    /** demo3.yml does not indicate the background frequency and thus isPresent should be false.*/
    @Test
    void testBackFrequencyPathNotPresent() {
        YamlParser parser = new YamlParser(demo3path);
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertFalse(backgroundOpt.isPresent());
    }

    @Test
    void testGetPrexif() {
        YamlParser yparser = new YamlParser(demo3path); // prefix is pfeiffer1 for this YAML file
        assertEquals("pfeiffer1",yparser.getPrefix());
    }

    @Test
    void testGetHpoIds() {
        YamlParser yparser = new YamlParser(demo3path); // [ 'HP:0001363', 'HP:0011304', 'HP:0010055']
        String [] expected = {"HP:0001363", "HP:0011304", "HP:0010055"};
        String [] hpos =yparser.getHpoTermList();
        assertEquals(expected.length,hpos.length);
        assertEquals(3,hpos.length);
        assertEquals(expected[0],hpos[0]);
        assertEquals(expected[1],hpos[1]);
        assertEquals(expected[2],hpos[2]);
    }


}
