package org.monarchinitiative.lirical.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.lirical.core.model.ClinvarClnSig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VariantPathogenicityTest {

    @ParameterizedTest
    @CsvSource({
            "0.,  BENIGN",
            " .5, CONFLICTING_PATHOGENICITY_INTERPRETATIONS",
            "1.0, PATHOGENIC",
    })
    public void normal(float pathogenicity, ClinvarClnSig clnSig) {
        VariantPathogenicity vp = VariantPathogenicity.of(pathogenicity, clnSig);

        assertThat(vp.isEmpty(), equalTo(false));
        assertThat((double) vp.pathogenicity(), is(closeTo(pathogenicity, 1E-5)));
        assertThat(vp.clinvarClnSig().isPresent(), equalTo(true));
        assertThat(vp.clinvarClnSig().get(), equalTo(clnSig));
    }

    @Test
    public void missingClinvarSignificance() {
        VariantPathogenicity vp = VariantPathogenicity.of(.5f, null);

        assertThat(vp.clinvarClnSig().isEmpty(), equalTo(true));
    }

    @Test
    public void isEmpty() {
        VariantPathogenicity empty = VariantPathogenicity.empty();

        assertThat(empty.isEmpty(), equalTo(true));
        assertThat(empty.clinvarClnSig().isEmpty(), equalTo(true));
        assertThat((double) empty.pathogenicity(), is(notANumber()));
    }

    @Test
    public void usingFloatNanThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> VariantPathogenicity.of(Float.NaN, null));
        assertThat(e.getMessage(), equalTo("Pathogenicity must not be NaN"));
    }

    @ParameterizedTest
    @CsvSource({
            "-0.0000001",
            " 1.0000001",
    })
    public void pathogenicityOutsideOfBoundsThrows(float pathogenicity) {
        IllegalArgumentException over = assertThrows(IllegalArgumentException.class, () -> VariantPathogenicity.of(pathogenicity, null));
        assertThat(over.getMessage(), equalTo("Pathogenicity score must be in range of [0, 1]"));

        IllegalArgumentException under = assertThrows(IllegalArgumentException.class, () -> VariantPathogenicity.of(pathogenicity, null));
        assertThat(under.getMessage(), equalTo("Pathogenicity score must be in range of [0, 1]"));
    }
}