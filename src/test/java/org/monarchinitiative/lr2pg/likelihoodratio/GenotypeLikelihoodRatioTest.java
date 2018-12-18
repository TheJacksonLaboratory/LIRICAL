package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class GenotypeLikelihoodRatioTest {

    private static final double EPSILON=0.0001;


    /**
     * If we find one variant that is listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000 to 1.
     */
    @Test
    void testOneClinVarVariant() {
        Gene2Genotype g2g = mock(Gene2Genotype.class);

        when(g2g.hasPathogenicClinvarVar()).thenReturn(true);
        when(g2g.pathogenicClinVarCount()).thenReturn(1);
        Map<TermId,Double> emptyMap = ImmutableMap.of();
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(emptyMap);
        TermId fakeGeneId = TermId.of("Fake:123");
       List<TermId> emptyList= ImmutableList.of();
       Optional<Double> lrOption = genoLRmap.evaluateGenotype( g2g, emptyList, fakeGeneId);
        assertTrue(lrOption.isPresent());
        double result = lrOption.get();
        double expected = (double)1000;
        assertEquals(expected,result,EPSILON);
    }


    /**
     * If we find two variants listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000*1000 to 1.
     */
    @Test
    void testTwoClinVarVariants() {
        Gene2Genotype g2g = mock(Gene2Genotype.class);

        when(g2g.hasPathogenicClinvarVar()).thenReturn(true);
        when(g2g.pathogenicClinVarCount()).thenReturn(2);
        Map<TermId,Double> emptyMap = ImmutableMap.of();
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(emptyMap);
        TermId fakeGeneId = TermId.of("Fake:123");
        List<TermId> emptyList= ImmutableList.of();
        Optional<Double> lrOption = genoLRmap.evaluateGenotype( g2g, emptyList, fakeGeneId);
        assertTrue(lrOption.isPresent());
        double result = lrOption.get();
        double expected = (double)1000*1000;
        assertEquals(expected,result,EPSILON);
    }
}
