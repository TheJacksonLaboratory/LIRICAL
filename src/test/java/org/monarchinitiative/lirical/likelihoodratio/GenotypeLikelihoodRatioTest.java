package org.monarchinitiative.lirical.likelihoodratio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.model.Gene2Genotype;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.monarchinitiative.phenol.annotations.formats.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_DOMINANT;
import static org.monarchinitiative.phenol.annotations.formats.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE;

public class GenotypeLikelihoodRatioTest {

    private static final double EPSILON=0.0001;

    private static final String SAMPLE_ID = "JIM";
    private static final float PATHOGENICITY_THRESHOLD = .8f;
    private static final GenotypeLikelihoodRatio.Options OPTIONS = new GenotypeLikelihoodRatio.Options(PATHOGENICITY_THRESHOLD, false);
    private static final TermId autosomalDominant = TermId.of("HP:0000006");
    private static final TermId autosomalRecessive = TermId.of("HP:0000007");


    private static Gene2Genotype setupGeneToGenotype(int variantCount, int pathogenicClinvarCount, double sumOfPathBinScores) {
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.id()).thenReturn(TermId.of("Fake:123"));
        when(g2g.variantCount()).thenReturn(variantCount);
        when(g2g.pathogenicClinVarCount(SAMPLE_ID)).thenReturn(pathogenicClinvarCount);
        when(g2g.hasVariants()).thenReturn(variantCount != 0);
        when(g2g.getSumOfPathBinScores(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(sumOfPathBinScores); // mock that we find no pathogenic variant
        return g2g;
    }

    /**
     * If we find one variant that is listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000 to 1.
     */
    @Test
    public void testOneClinVarVariant() {
        Gene2Genotype g2g = setupGeneToGenotype(1, 1, 0.8);
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(Map.of(), OPTIONS);
        double result = genoLRmap.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_DOMINANT)).lr();
        double expected = 1000;
        Assertions.assertEquals(expected,result,EPSILON);
    }


    /**
     * If we find two variants listed as pathogenic in ClinVar, then we return the genotype
     * likelihood ratio of 1000*1000 to 1.
     */
    @Test
    public void testTwoClinVarVariants() {
        Gene2Genotype g2g = setupGeneToGenotype(2, 2, 1.6);
        GenotypeLikelihoodRatio genoLRmap = new GenotypeLikelihoodRatio(Map.of(), OPTIONS);
        double result = genoLRmap.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE)).lr();
        double expected = (double)1000*1000;
        Assertions.assertEquals(expected,result,EPSILON);
    }


    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 1, and a lambda-background of 8.7. This numbers are taken from the HLA-B gene.
     */
    @Test
    public void testHLA_Bsituation() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId HLAB = TermId.of("NCBIGene:3106");
        g2background.put(HLAB,8.7418); // very high lambda-background for HLAB
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(g2background, OPTIONS);
        Gene2Genotype g2g = setupGeneToGenotype(0, 0, 0.);
        double score = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(autosomalDominant)).lr();
        double expected = 0.05; // heuristic score
        Assertions.assertEquals(expected,score,EPSILON);
    }

    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 2, and a lambda-background of 8.7. This numbers are taken from a made-up  gene.
     */
    @Test
    public void testRecessiveManyCalledPathVariants() {
        // create a background map with just one gene for testing
        Map <TermId,Double> g2background = new HashMap<>();
        TermId madeUpGene = TermId.of("NCBIGene:42");
        g2background.put(madeUpGene,8.7418); // very high lambda-background for TTN
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(g2background, OPTIONS);
        Gene2Genotype g2g = setupGeneToGenotype(0, 0, 0.);
        double score = glr.evaluateGenotype(SAMPLE_ID, g2g,List.of(autosomalRecessive)).lr();
        double expected = 0.05*0.05; // heuristic score for AR
        Assertions.assertEquals(expected,score,EPSILON);
    }
}
