package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ravanv on 1/18/18.
 */
public class Disease2TermFrequencyTest2 {
    private static TermPrefix HP_PREFIX=null;

    private static Disease2TermFrequency d2tf=null;

    private static final double EPSILON=0.000001;


    @BeforeClass
    public static void setup() throws IOException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("phenotype_annotation.tab").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology = parser.getPhenotypeSubontology();
        Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,phenotypeSubOntology,inheritanceSubontology);
        Map<String,HpoDiseaseWithMetadata> diseaseMap=annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY="0040280";
        d2tf=new Disease2TermFrequency(phenotypeSubOntology,inheritanceSubontology,diseaseMap);
    }


    @Test
    public void notNullTest() {
        assertNotNull(d2tf);
    }



    /**
     * $ cut -f 2 phenotype_annotation | sort | uniq | wc -l
     196
     */
    @Test
    public void testGetNumberOfDiseases() {
        int expected = 10262;
        assertEquals(expected,d2tf.getNumberOfDiseases());
    }



    /**
     * cut -f 5 phenotype_annotation.tab | sort
     * shows that there is only one instance of Atrial cardiomyopathy, HP:0200127. The frequency should
     * be 1/10262 -- there are no frequency modifiers in {@code phenotype_annotation.tab}, so we do not need to worry about
     * weighting
     */
    @Test
    public void testFrequency1() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0200127"); // Atrial cardiomyopathy
        double expected = (double)1/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }



    /**
     * There are 89 appearances of HP:0001385 in the phenotype_annotation.tab, the expected frequency is 89/10262 = 0.00867277,
     * but the actual frequency is 0.0039933
     */
   /* @Test
    public void testFrequency2() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001385"); //AURICULOOSTEODYSPLASIA
        double expected = (double)89/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     * There are 65 appearances of HP:0001852 in the phenotype_annotation.tab, the expected frequency is 65/10262 = 0.00633340,
     * but the actual frequency is 0.00370882868
     */
   /* @Test
    public void testFrequency4() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0001852");
        double expected = (double)65/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     * There are 20 appearances of HP:0000008 in phenotype_annotation.tab, the expected frequency is 20/10262 = 0.001948937828883,
     * but the Actual background frequency is :0.04654355876047554 ?
     */
   /* @Test
    public void testFrequency5() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000008");
        double expected = (double)20/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     *  There are  1318 appearances of HP:0004322 in phenotype_annotation.tab, the expected frequency is 1318/10262 = 0.12843500292340673,
     *  but the actual background frequency is 0.13226466575716234 ?
     */
   /* @Test
    public void testFrequency6() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0004322");//SAETHRE-CHOTZEN SYNDROME; SCS;;ACROCEPHALOSYNDACTYLY
        double expected = (double)1318/10262;
        //Actual background frequency = 22/196
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     * There are  74 appearances of HP:0000020 in phenotype_annotation.tab, the expected frequency is 74/10262 = 0.0072110, but the actual frequency
     * is 0.00584389007
     */
   /* @Test
    public void testFrequency7() {
        // Urinary incontinence = HP:0000020
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000020"); //
        double expected = (double)74/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     * There are  645 appearances of HP:0000639 in phenotype_annotation.tab, the expected frequency is 645/10262 = 0.0628532, but the actual frequency
     * is 0.049922
     */
   /* @Test
    public void testFrequency8() {
        //Frequency7=Nystagmus
        ImmutableTermId nystagmusId = new ImmutableTermId(HP_PREFIX,"0000639");
        double bf = d2tf.getBackgroundFrequency(nystagmusId);
        System.err.println("background freq = " + bf);
        double expected = (double)645/10262;

        assertEquals(expected,d2tf.getBackgroundFrequency(nystagmusId),EPSILON);
    }*/



    /**
     * There are  7 appearances of HP:0007024 in phenotype_annotation.tab, the expected frequency is 7/10262 = 6.821282401091405E-4, but the actual frequency
     * is 4.950302085363477E-4
     */
   /* @Test
    public void testFrequency9() {
        // AMYOTROPHIC LATERAL SCLEROSIS 1 = HP:0007024
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0007024"); //
        double expected = (double)7/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/



    /**
     * There are  30 appearances of HP:0007354 in phenotype_annotation.tab, the expected frequency is 30/10262 = 0.00292340674, but the actual frequency
     * is 0.00280939
     */
    /*@Test
    public void testFrequency10() {
        // AMYOTROPHIC LATERAL SCLEROSIS AND/OR FRONTOTEMPORAL DEMENTIA 1 = HP:0007354
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0007354"); //
        double expected = (double)30/10262;
        assertEquals(expected,d2tf.getBackgroundFrequency(tid),EPSILON);
    }*/


}
