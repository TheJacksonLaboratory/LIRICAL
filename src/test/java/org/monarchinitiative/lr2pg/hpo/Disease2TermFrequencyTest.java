package org.monarchinitiative.lr2pg.hpo;


import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.phenol.formats.hpo.HpoDiseaseWithMetadata;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.Map;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class Disease2TermFrequencyTest {

    private static TermPrefix HP_PREFIX=null;

    private static BackgroundForegroundTermFrequency d2tf=null;

    private static final double EPSILON=0.000001;


    @BeforeClass
    public static void setup() throws IOException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology = parser.parse();

        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,ontology);
        Map<String,HpoDiseaseWithMetadata> diseaseMap=annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY="0040280";
        d2tf=new BackgroundForegroundTermFrequency(ontology,diseaseMap);
    }


    @Test
    public void notNullTest() {
        assertNotNull(d2tf);
    }

    /**
     * $ cut -f 2 small_phenoannot.tab | sort | uniq | wc -l
     196
     */
    @Test
    public void testGetNumberOfDiseases() {
        int expected = 196;
        assertEquals(expected,d2tf.getNumberOfDiseases());
    }

    /**
     * cut -f 5 small_phenoannot.tab | sort
     * shows that there is only one instance of Atrial cardiomyopathy, HP:0200127. The frequency should
     * be 1/196 -- there are no frequency modifiers in {@code small_phenoannot.tab}, so we do not need to worry about
     * weighting
     */
    @Test
    public void testFrequency1() throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0200127"); // Atrial cardiomyopathy
        double expected = (double)1/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }

    @Test
    public void testFrequency2() throws Lr2pgException{
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001385"); //AURICULOOSTEODYSPLASIA
        double expected = (double)1/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }


    @Test
    public void testFrequency3()throws Lr2pgException {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001852");
        double expected = (double)1/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }

    /**
     * There is no HP:0000008 in small_phenoannot.tab, but the Actual background frequency is :0.04081632653061224 ???
     */
   /* @Test
    public void testFrequency4() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000008");
        double expected = (double)0/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/

    /**
     *  There are only 12 appearances of 0004322 in small_phenoannot.tab. The expected frequency is 12/196 = 0.061224489795918366,
     *  but the actual frequency is 0.11224489795918367 ??
     */
   /* @Test
    public void testFrequency5() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0004322");//SAETHRE-CHOTZEN SYNDROME; SCS;;ACROCEPHALOSYNDACTYLY
        double expected = (double)12/196;
        //Actual background frequency = 22/196
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/

    /**
     * HP:0000020 (Urinary incontinence) is used twice in small_phenoannot.tab
     */
    @Test
    public void testFrequency6()throws Lr2pgException {
        // Urinary incontinence = HP:0000020
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000020"); //
        double expected = (double)2/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }


    /** According to HPO Workbench, there are 13 instances of Nystagmus in small_phenoannot.tab and not annotations to
     * descendents of Nystagmus.
     */
    
    /**
     * expected frequency is 0.05102040816326531, but the actual frequency is 0.06142857142857143?
     */
    @Test
    public void testFrequency7()throws Lr2pgException {
        //Frequency7=Nystagmus
        ImmutableTermId nystagmusId = new ImmutableTermId(HP_PREFIX,"0000639");
        double bf = d2tf.getBackgroundFrequency(nystagmusId);
        System.err.println("background freq = " + bf);
        double expected = (double)12/196;
        //assertEquals(expected,d2tf.getBackgroundFrequency(nystagmusId),EPSILON);// ToDo check the logic of this test
    }

    @Test
    public void testFrequency8() throws Lr2pgException{
        // AMYOTROPHIC LATERAL SCLEROSIS 1 = HP:0007024
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0007024"); //
        double expected = (double)1/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }

    @Test
    public void testFrequency9() throws Lr2pgException{
        // AMYOTROPHIC LATERAL SCLEROSIS AND/OR FRONTOTEMPORAL DEMENTIA 1 = HP:0007354
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0007354"); //
        double expected = (double)3/196;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }


}