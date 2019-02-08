package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;


import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class SimpleVariantTest {


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
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="1/1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HOMOZYGOUS_ALT, sv.getGtype());
    }

    /** Test {@link org.monarchinitiative.lr2pg.vcf.SimpleVariant#isClinVarPathogenic()}  for pathogenic variants. */
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

    /** Test {@link org.monarchinitiative.lr2pg.vcf.SimpleVariant#isClinVarPathogenic()}  for benign variants. */
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



}
