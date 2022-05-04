package org.monarchinitiative.lirical.background.backgroundfrequency;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.exomiser_db_adapter.model.frequency.FrequencySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BackgroundTest {


    @Test
    public void testCtor() {
        Background background = new Background(FrequencySource.GNOMAD_E_AFR);
        assertEquals(FrequencySource.GNOMAD_E_AFR,background.getFrequencySource());
    }


    @Test
    public void testGetGene2Bin() {
        Background background = new Background(FrequencySource.GNOMAD_E_AFR);
        String fakeGeneSymbol = "FAKE";
        Optional<Gene2Bin> g2b = background.getGene2Bin(fakeGeneSymbol);
        assertFalse(g2b.isPresent());
    }
}
