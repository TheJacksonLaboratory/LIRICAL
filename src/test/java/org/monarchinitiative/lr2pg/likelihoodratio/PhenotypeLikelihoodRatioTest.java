package org.monarchinitiative.lr2pg.likelihoodratio;


import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple test that we get the right frequencies. There are 37 annotations in the file
 * small.hpoa. Each annotation is unique.
 */
public class PhenotypeLikelihoodRatioTest {

    private static PhenotypeLikelihoodRatio phenotypeLrCalculator =null;

    private static Map<TermId, HpoDisease> diseaseMap;


    private static final double EPSILON=0.000001;


    @BeforeClass
    public static void setup() throws PhenolException, FileNotFoundException,NullPointerException {
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        HpOboParser parser = new HpOboParser(new File(hpoPath));
        HpoOntology ontology = parser.parse();

        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationPath,ontology);
        diseaseMap=annotationParser.parse();
        phenotypeLrCalculator =new PhenotypeLikelihoodRatio(ontology,diseaseMap);
    }


    @Test
    public void notNullTest() {
        assertNotNull(phenotypeLrCalculator);
    }

    /**
     * $ cut -f 2 small_phenotype.hpoa | sort | uniq | wc -l
     3
     */
    @Test
    public void testGetNumberOfDiseases() {
        int expected = 3;
        assertEquals(expected, phenotypeLrCalculator.getNumberOfDiseases());
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
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }

    @Test
    public void testFrequency2() {
        TermId tid = TermId.constructWithPrefix("HP:0000047");
        double expected = (double)1/3;
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }


    /** HP:0000035 is an ancestor of "HP:0000028" (which has an explicit annotation in small_phenotyoe.hpoa), and therefore
     * its background frequency should also be 1/3
     */
    @Test
    public void testFrequency3() {
        TermId tid = TermId.constructWithPrefix("HP:0000035");
        double expected = (double)1/3;
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }


    /**
     * The term HP:0001265 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_1() {
        TermId tid = TermId.constructWithPrefix("HP:0000185");
        TermId diseaseName = TermId.constructWithPrefix("OMIM:216300");
        HpoDisease disease = diseaseMap.get(diseaseName);
        assertNotNull(disease);
        double expected =1.0;
        assertEquals(expected, phenotypeLrCalculator.getFrequencyOfTermInDisease(disease,tid),EPSILON);
    }



}