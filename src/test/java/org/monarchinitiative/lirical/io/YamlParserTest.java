package org.monarchinitiative.lirical.io;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.WINDOWS;


class YamlParserTest {
    // Paths to the example YAML files in src/test/resources/yaml/
    private static String example1path;
    private static String example2path;

    private static final double EPSILON=0.000001;

    @BeforeAll
    static void init() throws FileNotFoundException{
        ClassLoader classLoader = YamlParserTest.class.getClassLoader();
        URL resource = classLoader.getResource("yaml/example1.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find example1.yml file");
        }
        example1path = resource.getFile();
        resource = classLoader.getResource("yaml/example2.yml");
        if (resource==null){
            throw new FileNotFoundException("Could not find example2.yml file");
        }
        example2path = resource.getFile();
    }

    @Test
    void testExample1YamlFile() {
        YamlParser parser = new YamlParser(example1path);
        String expected = String.format("%s%s%s","data", File.separator,"hp.obo");
        assertEquals(expected,parser.getHpOboPath());
    }

    /**
     * This test is disabled on windows because it depends on the File separator (/ vs \).
     */
    @Test @DisabledOnOs(WINDOWS)
    void testMvStorePath() {
        YamlParser parser = new YamlParser(example1path);
        String expected="/path/to/1802_hg19/1802_hg19_variants.mv.db";
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
        YamlParser parser = new YamlParser(example1path);
        String expectedMvStore = "/path/to/1802_hg19/1802_hg19_variants.mv.db";
        assertEquals(expectedMvStore,parser.getMvStorePath());
        String expectedJannovar = "/path/to/1802_hg19/1802_hg19_transcripts_ucsc.ser";
        assertEquals(expectedJannovar,parser.jannovarFile());
    }

    /**
     * The default path for the background frequency is src/main/resources/background/ but it can be
     * overrridden in the YAML file
     */
    @Test
    void testBackFrequencyPath1() {
        YamlParser parser = new YamlParser(example1path);
        // We do not provide the background frequency path in this YAML file
        // therefore, the value should be not present
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertFalse(backgroundOpt.isPresent());
    }

    /**
     * The default path for the background frequency is src/main/resources/background/ but it can be
     * overrridden in the YAML file
     */
    @Test
    void testBackFrequencyPath2() {
        YamlParser parser = new YamlParser(example2path);
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertTrue(backgroundOpt.isPresent());
        String expected="/path/to/custom_location2";
        assertEquals(expected,backgroundOpt.get());
    }

    @Test
    void testDefaultDataPath() {
        YamlParser parser = new YamlParser(example1path);
        String datadir = parser.getDataDir();
        String expected = "data";
        assertEquals(expected,datadir);
    }


    @Test
    void testCustomDataPath2() {
        YamlParser parser = new YamlParser(example2path);
        String datadir = parser.getDataDir();
        String expected = "/path/to/custom_location1";
        assertEquals(expected,datadir);
    }

    /** example2.yml does not indicate the background frequency and thus isPresent should be false.*/
    @Test
    void testBackFrequencyPathNotPresent() {
        YamlParser parser = new YamlParser(example1path);
        Optional<String> backgroundOpt = parser.getBackgroundPath();
        assertFalse(backgroundOpt.isPresent());
    }

    @Test
    void testGetPrefix() {
        YamlParser yparser = new YamlParser(example2path); // prefix is pfeiffer1 for this YAML file
        assertEquals("example2",yparser.getPrefix());
    }

    @Test
    void testGetHpoIds() {
        YamlParser yparser = new YamlParser(example2path); // [ 'HP:0001363', 'HP:0011304', 'HP:0010055']
        String [] expected = {"HP:0001363", "HP:0011304", "HP:0010055"};
        List<String> hpos =yparser.getHpoTermList();
        assertEquals(expected.length,hpos.size());
        assertEquals(3,hpos.size());
        assertEquals(expected[1],hpos.get(1));
        assertEquals(expected[2],hpos.get(2));
    }

    @Test
    void testNegatedHpoIds1() {
        // example 1 has no negated HPOs
        YamlParser yparser = new YamlParser(example1path);
        List<TermId> emptyList = ImmutableList.of();
        assertEquals(emptyList,yparser.getNegatedHpoTermList());
    }

    @Test
    void testNegatedHpoIds2() {
        // example 2 has one negated id
        YamlParser yparser = new YamlParser(example2path);
        String termid = "HP:0001328"; // the negated term
        List<String> expected = ImmutableList.of(termid);
        assertEquals(expected,yparser.getNegatedHpoTermList());
    }

    @Test
    void testOutDir1() {
        // example 1 does not have an out directory
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.getOutDirectory().isPresent());
    }

    @Test
    void testOutDir2() {
        // example 2 has myoutdirectory
        YamlParser yparser = new YamlParser(example2path);
        String expected="myoutdirectory";
        assertTrue(yparser.getOutDirectory().isPresent());
        assertEquals(expected,yparser.getOutDirectory().get());
    }

    @Test
    void testGlobal1() {
        // example 1 does not have a keep entry
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.global());
    }

    @Test
    void testGlobal2() {
        // example 2 has keep=true
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.global());
    }


    @Test
    void testMinDiff1() {
        //example 1 has no mindiff entry,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.mindiff().isPresent());
    }

    @Test
    void testMinDiff2() {
        //example 2 has  mindiff:50
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.mindiff().isPresent());
        int expected=50;
        assertEquals(expected,yparser.mindiff().get());
    }

    @Test
    void testThreshold1() {
        //example 1 has no threshold entry,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.threshold().isPresent());
    }

    @Test
    void testThreshold2() {
        //example 2 has threshold: 0.05,
        // the optional element should be empty
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.threshold().isPresent());
        double expected = 0.05;
        assertEquals(expected,yparser.threshold().get(),EPSILON);
    }

    @Test
    void testTsv1() {
        //example 1 has no tsv entry,
        YamlParser yparser = new YamlParser(example1path);
        assertFalse(yparser.doTsv());
    }

    @Test
    void testTsv2() {
        //example 2 has  tsv: true,
        YamlParser yparser = new YamlParser(example2path);
        assertTrue(yparser.doTsv());
    }


}
