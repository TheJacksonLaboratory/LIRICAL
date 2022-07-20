package org.monarchinitiative.lirical.core.likelihoodratio;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.TestResources;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Simple test that we get the right frequencies. There are 37 annotations in the file
 * small.hpoa. Each annotation is unique.
 */
public class PhenotypeLikelihoodRatioTest {

    private static PhenotypeLikelihoodRatio phenotypeLrCalculator =null;

    private static final HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private static final Map<TermId, HpoDisease> diseaseMap = hpoDiseases.diseaseById();


    private static final double ERROR = 5E-6;


    @BeforeAll
    public static void setup() {
        phenotypeLrCalculator =new PhenotypeLikelihoodRatio(TestResources.hpo(), hpoDiseases);
    }


    @Test
    public void notNullTest() {
        assertNotNull(phenotypeLrCalculator);
    }

    /**
     * cut -f 5 small_phenoannot.tab | sort
     * shows that there is only one instance of Atrial cardiomyopathy, HP:0200127. The frequency should
     * be 1/196 -- there are no frequency modifiers in {@code small_phenoannot.tab}, so we do not need to worry about
     * weighting
     */
    @Test
    public void testFrequency1() {
        TermId tid = TermId.of("HP:0000028");
        assertEquals(.2, phenotypeLrCalculator.getBackgroundFrequency(tid), ERROR);
    }

    @Test
    public void testFrequency2() {
        TermId tid = TermId.of("HP:0000047");
        assertEquals(.2, phenotypeLrCalculator.getBackgroundFrequency(tid), ERROR);
    }


    /** HP:0000035 is an ancestor of "HP:0000028" (which has an explicit annotation in small_phenotyoe.hpoa),
     *  and of "HP:0000047" (but both in the same disease), and therefore
     * its background frequency should be 1/3
     */
    @Test
    public void testFrequency3() {
        TermId tid = TermId.of("HP:0000035");
        assertEquals(.2, phenotypeLrCalculator.getBackgroundFrequency(tid), ERROR);
    }


    /**
     * The term HP:0001265 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_1() {
        TermId tid = TermId.of("HP:0000185");
        TermId diseaseName = TermId.of("OMIM:216300");
        HpoDisease disease = diseaseMap.get(diseaseName);
        assertNotNull(disease);
        double frq = disease.getAnnotation(tid).map(HpoDiseaseAnnotation::frequency).get();
        assertEquals(1.0, frq, ERROR);
    }



}