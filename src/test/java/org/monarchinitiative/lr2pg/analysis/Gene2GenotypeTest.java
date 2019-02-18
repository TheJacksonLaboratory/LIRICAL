package org.monarchinitiative.lr2pg.analysis;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Gene2GenotypeTest {

    private final static double EPSILON=0.000001;

    /**
     * Test calculation of pathogenicity score. This depends on the Exomiser
     * frequency score,
     * <pre>
     * private double frequencyScore() {
     *   if (frequency <= 0) {
     *     return 1f;
     *   } else if (frequency > 2) {
     *     return 0f;
     *   } else {
     *     return 1.13533f - (0.13533f * (float) Math.exp(frequency));
     *   }
     * }
     * In R, which we used to check this, we used: {@code f <- function(x) { 1.13533 - (0.13533 *exp(x))}}
     * </pre>
     */
    @Test
    void testVariantPathogenicity1() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=0.95f;
        float frequency=0.001f;
        float expected=0.9498714f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.NOT_PROVIDED);
        assertEquals(expected,g2g.getSumOfPathBinScores(),EPSILON);
    }

    @Test
    void testVariantPathogenicity2() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=0.95f;
        float frequency=0.006f;
        float expected=0.9492263f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.NOT_PROVIDED);
        assertEquals(expected,g2g.getSumOfPathBinScores(),EPSILON);
    }
    @Test
    void testVariantPathogenicity3() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=0.95f;
        float frequency=0.15f;
        float expected=0.929194f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.NOT_PROVIDED);
        assertEquals(expected,g2g.getSumOfPathBinScores(),EPSILON);
    }


    @Test
    void testVariantPathogenicity4() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=0.95f;
        float frequency=2.15f;
        float expected=0.0f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.NOT_PROVIDED);
        assertEquals(expected,g2g.getSumOfPathBinScores(),EPSILON);
    }

}
