package org.monarchinitiative.lr2pg.analysis;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Gene2GenotypeTest {

    private final static double EPSILON=0.000001;

    @Test
    void testVariantPathogenicityGetSet() {
        String symbol="NRAS";
        TermId geneId = TermId.of("NCBIGene:4893");
        Gene2Genotype g2g = new Gene2Genotype(geneId,symbol);
        List<TranscriptAnnotation> emptyList = ImmutableList.of();
        float pathscore=0.95f;
        g2g.addVariant(1,114713908, "A","G",emptyList,"0/1",pathscore,0.001f);
        assertEquals(pathscore,g2g.getSumOfPathBinScores(),EPSILON);
    }


}
