package org.monarchinitiative.lirical.vcf;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;


import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class SimpleVariantTest {
    private final static double EPSILON=0.000001;
    private static SimpleVariant svHet, svHom, svClinVarPath, getSvClinVarLikelyPath;
    private static SimpleVariant svRareVariant, svMediumRareVariant, svCommonVariant;

    @BeforeAll
    static void init() {
        // The following annotation is completely made up, but don't worry it has the right syntax
        TranscriptAnnotation annot = Mockito.mock(TranscriptAnnotation.class);
       //ta.getAccession(),ta.getHgvsCdna(),ta.getHgvsProtein()
        when(annot.getAccession()).thenReturn("uc001lfg.4");
        when(annot.getHgvsCdna()).thenReturn("c.518A>C");
        when(annot.getHgvsProtein()).thenReturn("p.(E173A)");
        when(annot.toString()).thenReturn("uc001lfg.4: c.518A>C p.(E173A) MISSENSE_VARIANT");
        List<TranscriptAnnotation> annotlist = ImmutableList.of(annot);// not needed
        String genotypeString="0/1";
        svHet = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 0.01f, genotypeString);
        genotypeString="1/1";
        svHom = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 0.01f, genotypeString);
        svClinVarPath = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 0.01f, genotypeString, ClinVarData.ClinSig.PATHOGENIC);
        getSvClinVarLikelyPath = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 0.01f, genotypeString, ClinVarData.ClinSig.LIKELY_PATHOGENIC);
        svRareVariant = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 0.00f, genotypeString);
        svMediumRareVariant = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 1.00f, genotypeString);
        svCommonVariant = new SimpleVariant(2, 23333, "A", "T", annotlist, 0.9f, 5.00f, genotypeString);
    }


    @Test
    void testHetVariantUnphased() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="0/1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HETEROZYGOUS, sv.getGtype());
    }


    @Test
    void testHetVariantPhased() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="0|1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HETEROZYGOUS, sv.getGtype());
    }


    @Test
    void testHomVariant() {
        assertEquals(SimpleGenotype.HOMOZYGOUS_ALT, svHom.getGtype());
    }

    /** Test {@link org.monarchinitiative.lirical.vcf.SimpleVariant#isClinVarPathogenic()}  for pathogenic variants. */
    @Test
    void tesClinVarPath() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="1/1";
        ClinVarData.ClinSig clinvarsig = ClinVarData.ClinSig.PATHOGENIC;
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertTrue(sv.isClinVarPathogenic());
        clinvarsig = ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC;
        sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertTrue(sv.isClinVarPathogenic());
        clinvarsig = ClinVarData.ClinSig.LIKELY_PATHOGENIC;
        sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertTrue(sv.isClinVarPathogenic());
    }

    /** Test {@link org.monarchinitiative.lirical.vcf.SimpleVariant#isClinVarPathogenic()}  for benign variants. */
    @Test
    void tesClinVarBenign() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="1/1";
        ClinVarData.ClinSig clinvarsig = ClinVarData.ClinSig.BENIGN;
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertFalse(sv.isClinVarPathogenic());
        clinvarsig = ClinVarData.ClinSig.BENIGN_OR_LIKELY_BENIGN;
        sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertFalse(sv.isClinVarPathogenic());
        clinvarsig = ClinVarData.ClinSig.LIKELY_BENIGN;
        sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertFalse(sv.isClinVarPathogenic());
    }

    @Test
    void testXYMchroms() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="1/1";
        ClinVarData.ClinSig clinvarsig = ClinVarData.ClinSig.BENIGN;
        SimpleVariant sv = new SimpleVariant(23, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertEquals("chrX",sv.getChromosome());
        sv = new SimpleVariant(24, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertEquals("chrY",sv.getChromosome());
        sv = new SimpleVariant(25, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString,clinvarsig);
        assertEquals("chrM",sv.getChromosome());
        assertEquals(23333, sv.getPosition());
    }

    @Test
    void test_n_a_clinvar() {
        assertEquals("n/a", svHet.getClinvar());
        assertEquals("PATHOGENIC", svClinVarPath.getClinvar());
        assertEquals("LIKELY_PATHOGENIC", getSvClinVarLikelyPath.getClinvar());
    }

    @Test
    void toStringTest() {
        String expected = "chr2:23333A>T uc001lfg.4:c.518A>C:p.(E173A) pathogenicity:0.9 [HOMOZYGOUS_ALT]";
        assertEquals(expected,svClinVarPath.toString());
    }

    @Test
    void testPathScoreRareMediumCommonVar() {
        // rare variant; path=0.9, freq = 0
        // frequency score should be 1 * 0.9
        double expected = 0.9;
        assertEquals(expected,svRareVariant.getPathogenicityScore(),EPSILON);
        // medium variant path=0.9, frequency = 1%
        // frequency score is 1.13533f - (0.13533f * (float) Math.exp(frequency));
        //10.9*(1.13533 - (0.13533 * exp(1.0)))
        //[1] 0.6907184
        expected = 0.6907184;
        assertEquals(expected,svMediumRareVariant.getPathogenicityScore(),EPSILON);
        // high frequency variant
        expected = 0.0; // because frequency is 5%
        assertEquals(expected,svCommonVariant.getPathogenicityScore(),EPSILON);
    }

    @Test
    void testGetRef() {
        assertEquals("A",svRareVariant.getRef());
    }

    @Test
    void testGetAlt() {
        assertEquals("T",svRareVariant.getAlt());
    }

    @Test
    void testGetAnnotation() {
        List<TranscriptAnnotation> annotlist = svHet.getAnnotationList();
        assertEquals(1,annotlist.size());
        TranscriptAnnotation annot = annotlist.get(0);
        assertEquals("c.518A>C",annot.getHgvsCdna());
    }

    @Test
    void testGetFrequency() {
        double expected=0.01;// 0.01%
        assertEquals(expected,svHet.getFrequency(),EPSILON);
    }


    @Test
    void testCountTwoPathAllelesWithHomozygousVariant() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="1/1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HOMOZYGOUS_ALT, sv.getGtype());
        assertEquals(2,sv.pathogenicAlleleCount());
    }

    @Test
    void testCountOnePathAllelesWithHeterozygousVariant() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="0/1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HETEROZYGOUS, sv.getGtype());
        assertEquals(1,sv.pathogenicAlleleCount());
    }



}
