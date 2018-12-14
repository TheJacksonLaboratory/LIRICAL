package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleVariantTest {


    @Test
    public void testHetVariantUnphased() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="0/1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HETEROZYGOUS, sv.getGtype());
    }


    @Test
    public void testHetVariantPhased() {
        List<TranscriptAnnotation> emptylist = ImmutableList.of();// not needed
        String genotypeString="0|1";
        SimpleVariant sv = new SimpleVariant(2, 23333, "A", "T", emptylist, 0.9f, 0.01f, genotypeString);
        assertEquals(SimpleGenotype.HETEROZYGOUS, sv.getGtype());
    }



}
