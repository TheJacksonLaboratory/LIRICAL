package org.monarchinitiative.lr2pg.hpo;


import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.io.File;
import java.io.IOException;
import java.util.Map;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple test that we get the right frequencies. There are 37 annotations in the file
 * small.hpoa. Each annotation is unique.
 */
public class BackgroundForegroundTermFrequencyTest {

    private static BackgroundForegroundTermFrequency backForeFreq =null;

    private static final double EPSILON=0.000001;


    @BeforeClass
    public static void setup() throws IOException,PhenolException,NullPointerException {
        ClassLoader classLoader = BackgroundForegroundTermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology = parser.parse();

        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationPath,ontology);
        Map<TermId,HpoDisease> diseaseMap=annotationParser.parse();
        backForeFreq =new BackgroundForegroundTermFrequency(ontology,diseaseMap);
    }


    @Test
    public void notNullTest() {
        assertNotNull(backForeFreq);
    }

    /**
     * $ cut -f 2 small_phenotype.hpoa | sort | uniq | wc -l
     3
     */
    @Test
    public void testGetNumberOfDiseases() {
        int expected = 3;
        assertEquals(expected, backForeFreq.getNumberOfDiseases());
    }

    /**
     * cut -f 5 small_phenoannot.tab | sort
     * shows that there is only one instance of Atrial cardiomyopathy, HP:0200127. The frequency should
     * be 1/196 -- there are no frequency modifiers in {@code small_phenoannot.tab}, so we do not need to worry about
     * weighting
     */
    @Test
    public void testFrequency1() {
        TermId tid = TermId.constructWithPrefix("HP:0000028");
        double expected = (double)1/3;
        assertEquals(expected, backForeFreq.getBackgroundFrequency(tid),EPSILON);
    }

    @Test
    public void testFrequency2() {
        TermId tid = TermId.constructWithPrefix("HP:0000047");
        double expected = (double)1/3;
        assertEquals(expected, backForeFreq.getBackgroundFrequency(tid),EPSILON);
    }


    /** HP:0000035 is an ancestor of "HP:0000028" (which has an explicit annotation in small_phenotyoe.hpoa), and therefore
     * its background frequency should also be 1/3
     */
    @Test
    public void testFrequency3() {
        TermId tid = TermId.constructWithPrefix("HP:0000035");
        double expected = (double)1/3;
        assertEquals(expected, backForeFreq.getBackgroundFrequency(tid),EPSILON);
    }



}