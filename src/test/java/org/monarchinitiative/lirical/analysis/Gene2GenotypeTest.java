package org.monarchinitiative.lirical.analysis;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.monarchinitiative.lirical.analysis.Gene2Genotype.NO_IDENTIFIED_VARIANT;

class Gene2GenotypeTest {

    private final static double EPSILON=0.000001;
    private static Gene2Genotype nrasVar;

    @BeforeAll
    static void init() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        nrasVar = new Gene2Genotype(geneId,symbol);
        // The following annotation is completely made up, but don't worry it has the right syntax
        TranscriptAnnotation annot = Mockito.mock(TranscriptAnnotation.class);
        when(annot.toString()).thenReturn("uc001lfg.4: c.518A>C p.(E173A) MISSENSE_VARIANT");
        List<TranscriptAnnotation> annotList = ImmutableList.of(annot);
        float pathscore=0.95f;
        float frequency=0.001f;
        nrasVar.addVariant(1,114713908, "A","G",annotList,"0/1",pathscore,frequency, ClinVarData.ClinSig.NOT_PROVIDED);
    }

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


    @Test
    void testNoVarPathogenicity(){
        Gene2Genotype g2g = NO_IDENTIFIED_VARIANT;
        assertEquals(0,g2g.getSumOfPathBinScores(),EPSILON);
        assertFalse(g2g.hasPredictedPathogenicVar());
    }

    /** Test that we can retrieve ClinVar pathogenic variants, and a few other simple getters.*/
    @Test
    void testClinvarCount() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=1.0f;
        float frequency=0.01f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.PATHOGENIC);
        assertTrue(g2g.hasPathogenicClinvarVar());
        assertEquals(1,g2g.pathogenicClinVarCount());
        g2g.addVariant(14,114713908, "A","G",emptyList,"0/1",pathscore,frequency, ClinVarData.ClinSig.PATHOGENIC);
        assertEquals(2,g2g.pathogenicClinVarCount());
        assertTrue(g2g.hasPredictedPathogenicVar());
        assertEquals("NRAS",g2g.getSymbol());
        assertEquals(geneId,g2g.getGeneId());
    }

    @Test
    void testVarList() {
        assertTrue(NO_IDENTIFIED_VARIANT.getVarList().isEmpty());
        assertEquals(1,nrasVar.getVarList().size());
    }

    @Test
    void testHomozygousPathogenicVar() {
        String symbol="ATP6V0A4";
        TermId geneId = TermId.of("NCBIGene:50617");
        Gene2Genotype ATP6V0A4 = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=1.0f;
        float frequency=0.0000001f;
        ATP6V0A4.addVariant(7,138706689, "G","A",emptyList,"1/1",pathscore,frequency, ClinVarData.ClinSig.PATHOGENIC);
        // score should be 2 because it is homozgous and maximally pathogenic
        assertEquals(2.0,ATP6V0A4.getSumOfPathBinScores(),EPSILON);
    }


    @Test
    void testToString() {
        // return String.format("%s[%s]: %s",this.symbol,this.geneId.getValue(),varString);
        String expectedNras="NRAS[NCBIGene:4893]: chr1:114713908A>G null:null:null pathogenicity:0.9 [HETEROZYGOUS]";
        assertEquals(expectedNras,nrasVar.toString());
    }

}
