package org.monarchinitiative.lirical.likelihoodratio;



import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Simple test that we get the right frequencies. There are 37 annotations in the file
 * small.hpoa. Each annotation is unique.
 */
public class PhenotypeLikelihoodRatioTest {

    private static PhenotypeLikelihoodRatio phenotypeLrCalculator =null;

    private static Map<TermId, HpoDisease> diseaseMap;


    private static final double EPSILON=0.000001;


    @BeforeAll
    static void setup() throws NullPointerException {
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        URL url = classLoader.getResource("hp.small.obo");
        Objects.requireNonNull(url);
        String hpoPath = url.getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        // The HPO is in the default  curie map and only contains known relationships / HP terms
        Ontology ontology = OntologyLoader.loadOntology(new File(hpoPath));
        diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(annotationPath,ontology);
        phenotypeLrCalculator =new PhenotypeLikelihoodRatio(ontology,diseaseMap);
    }


    @Test
    void notNullTest() {
        assertNotNull(phenotypeLrCalculator);
    }

    /**
     * $ cut -f 2 small_phenotype.hpoa | sort | uniq | wc -l
     3
     */
    @Test
    void testGetNumberOfDiseases() {
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
    void testFrequency1() {
        TermId tid = TermId.of("HP:0000028");
        double expected = (double)1/3;
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }

    @Test
    void testFrequency2() {
        TermId tid = TermId.of("HP:0000047");
        double expected = (double)1/3;
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }


    /** HP:0000035 is an ancestor of "HP:0000028" (which has an explicit annotation in small_phenotyoe.hpoa),
     *  and of "HP:0000047" (but both in the same disease), and therefore
     * its background frequency should be 1/3
     */
    @Test
    void testFrequency3() {
        TermId tid = TermId.of("HP:0000035");
        double expected = (double)1/3;
        assertEquals(expected, phenotypeLrCalculator.getBackgroundFrequency(tid),EPSILON);
    }


    /**
     * The term HP:0001265 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    void testGetFrequencyOfTermInDieases1_1() {
        TermId tid = TermId.of("HP:0000185");
        TermId diseaseName = TermId.of("OMIM:216300");
        HpoDisease disease = diseaseMap.get(diseaseName);
        assertNotNull(disease);
        double expected =1.0;
        double frq = disease.getAnnotation(tid).getFrequency();
        assertEquals(expected,  frq,EPSILON);
    }



}