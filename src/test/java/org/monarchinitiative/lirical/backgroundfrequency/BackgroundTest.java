package org.monarchinitiative.lirical.backgroundfrequency;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;

import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BackgroundTest {


    @Test
    void testCtor() {
        Background background = new Background(FrequencySource.GNOMAD_E_AFR);
        assertEquals(FrequencySource.GNOMAD_E_AFR,background.getFrequencySource());
    }


    @Test
    void testGetGene2Bin() {
        Background background = new Background(FrequencySource.GNOMAD_E_AFR);
        String fakeGeneSymbol = "FAKE";
        Optional<Gene2Bin> g2b = background.getGene2Bin(fakeGeneSymbol);
        assertFalse(g2b.isPresent());
    }
}
