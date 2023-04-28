package org.monarchinitiative.lirical.core.likelihoodratio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.monarchinitiative.phenol.annotations.constants.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_DOMINANT;
import static org.monarchinitiative.phenol.annotations.constants.hpo.HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE;

public class GenotypeLikelihoodRatioTest {

    private static final double EPSILON = 0.0001;

    private static final String SAMPLE_ID = "JIM";
    private static final float PATHOGENICITY_THRESHOLD = .8f;
    private static final GeneIdentifier MADE_UP_GENE = GeneIdentifier.of(TermId.of("Fake:123"), "FAKE_SYMBOL");
    private static final GenotypeLikelihoodRatio.Options OPTIONS = new GenotypeLikelihoodRatio.Options(PATHOGENICITY_THRESHOLD, false);


    private static Gene2Genotype setupGeneToGenotype(GeneIdentifier geneId,
                                                     int variantCount,
                                                     int pathogenicClinvarCount,
                                                     double sumOfPathBinScores) {
        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.geneId()).thenReturn(geneId);
        when(g2g.hasVariants()).thenReturn(variantCount != 0);
        when(g2g.variantCount()).thenReturn(variantCount);
        when(g2g.pathogenicClinVarCount(SAMPLE_ID)).thenReturn(pathogenicClinvarCount);
        when(g2g.getSumOfPathBinScores(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(sumOfPathBinScores);
        return g2g;
    }

    /**
     * If we find a variant listed as pathogenic in ClinVar in a gene associated with the autosomal dominant disease,
     * then we return the genotype likelihood ratio of 1000 to 1.
     */
    @Test
    public void testOneClinVarVariant() {
        Gene2Genotype g2g = setupGeneToGenotype(MADE_UP_GENE, 1, 1, 0.8);
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        GenotypeLrWithExplanation gle = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_DOMINANT));
        assertThat(gle.matchType(), equalTo(GenotypeLrMatchType.ONE_DELETERIOUS_CLINVAR_VARIANT_IN_AD));
        Assertions.assertEquals(1000, gle.lr(), EPSILON);
    }


    /**
     * If we find two variants listed as pathogenic in ClinVar in a gene associated with autosomal recessive disease,
     * then we return the genotype likelihood ratio of 1000*1000 to 1.
     */
    @Test
    public void testTwoClinVarVariants() {
        Gene2Genotype g2g = setupGeneToGenotype(MADE_UP_GENE, 2, 2, 1.6);
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(Map.of(), 0.1), OPTIONS);
        GenotypeLrWithExplanation gle = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE));

        assertThat(gle.matchType(), equalTo(GenotypeLrMatchType.TWO_DELETERIOUS_CLINVAR_VARIANTS_IN_AR));
        Assertions.assertEquals(1000. * 1000, gle.lr(), EPSILON);
    }


    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 1, and a lambda-background of 8.7. This numbers are taken from the HLA-B gene.
     */
    @Test
    public void testHLA_Bsituation() {
        GeneIdentifier hlabId = GeneIdentifier.of(TermId.of("NCBIGene:3106"), "HLAB");
        Gene2Genotype g2g = setupGeneToGenotype(MADE_UP_GENE, 0, 0, 0.);

        // create a background map with just one gene for testing
        Map<TermId, Double> background = Map.of(hlabId.id(), 8.7418); // very high lambda-background for HLAB
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(background, 0.1), OPTIONS);
        GenotypeLrWithExplanation gle = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_DOMINANT));
        // heuristic score
        assertThat(gle.matchType(), equalTo(GenotypeLrMatchType.NO_VARIANTS_DETECTED_AD));
        Assertions.assertEquals(0.05, gle.lr(), EPSILON);
    }

    /**
     * We want to test what happens with a gene that has lots of variants but a pathogenic variant count sum of zero,
     * a lambda-disease of 2, and a lambda-background of 8.7. This numbers are taken from a made-up  gene.
     */
    @Test
    public void testRecessiveManyCalledPathVariants() {
        GeneIdentifier geneId = GeneIdentifier.of(TermId.of("NCBIGene:42"), "TTN");
        Gene2Genotype g2g = setupGeneToGenotype(geneId, 0, 0, 0.);

        // create a background map with just one gene for testing
        Map<TermId, Double> g2background = Map.of(geneId.id(), 8.7418); // very high lambda-background for TTN
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(g2background, 0.1), OPTIONS);
        GenotypeLrWithExplanation gle = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE));
        // heuristic score for AR
        assertThat(gle.matchType(), equalTo(GenotypeLrMatchType.NO_VARIANTS_DETECTED_AR));
        Assertions.assertEquals(0.05 * 0.05, gle.lr(), EPSILON);
    }

    @Test
    public void thrbExample() {
        GeneIdentifier thrbId = GeneIdentifier.of(TermId.of("NCBIGene:7068"), "THRB");

        Gene2Genotype g2g = mock(Gene2Genotype.class);
        when(g2g.geneId()).thenReturn(thrbId);
        when(g2g.hasVariants()).thenReturn(true);
        when(g2g.pathogenicClinVarCount(SAMPLE_ID)).thenReturn(0);
        when(g2g.pathogenicAlleleCount(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(56);
        when(g2g.getSumOfPathBinScores(SAMPLE_ID, PATHOGENICITY_THRESHOLD)).thenReturn(44.80000);

        Map<TermId, Double> gene2Background = Map.of(thrbId.id(), 0.006973);
        GenotypeLikelihoodRatio glr = new GenotypeLikelihoodRatio(BackgroundVariantFrequencyService.of(gene2Background, 0.1), OPTIONS);

        GenotypeLrWithExplanation gle = glr.evaluateGenotype(SAMPLE_ID, g2g, List.of(AUTOSOMAL_RECESSIVE));

        // TODO - check
        assertThat(gle.geneId(), equalTo(thrbId));
        assertThat(gle.matchType(), equalTo(GenotypeLrMatchType.LIRICAL_GT_MODEL));
        assertThat(gle.lr(), is(closeTo(1.719420800179587e109, EPSILON)));
        assertThat(gle.explanation(), equalTo("log<sub>10</sub>(LR)=109.235 P(G|D)=0.0000. P(G|&#172;D)=0.0000.  Mode of inheritance: autosomal recessive. Observed weighted pathogenic variant count: 44.80. &lambda;<sub>disease</sub>=2. &lambda;<sub>background</sub>=0.0070."));
    }
}
