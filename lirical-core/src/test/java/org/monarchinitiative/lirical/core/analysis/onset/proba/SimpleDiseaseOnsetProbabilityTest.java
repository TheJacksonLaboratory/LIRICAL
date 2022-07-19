package org.monarchinitiative.lirical.core.analysis.onset.proba;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.lirical.core.TestResources;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.annotations.base.temporal.ConfidenceRange;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class SimpleDiseaseOnsetProbabilityTest {

    private static final HpoDiseases DISEASES = TestResources.hpoDiseases();
    private static final double ERROR = 1E-12;

    private static final TermId BOBOPHOBIA_A = TermId.of("OMIM:100000");
    private static final TermId BOBOPHOBIA_B = TermId.of("OMIM:200000");
    private static final boolean STRICT = false;

    @ParameterizedTest
    @CsvSource({
            "0,  0,  0,      0.00000001",    // congenital
            "0,  0, 28,      0.00000001",    // neonatal
            "0,  0, 29,      0.99999999",    // infantile
            "0,  1,  0,      0.99999999",    // infantile
    })
    public void onsetProbability_subtypeA(int years, int months, int days, double expected) {
        SimpleDiseaseOnsetProbability dop = new SimpleDiseaseOnsetProbability(DISEASES, STRICT);

        TemporalInterval age = Age.postnatal(years, months, days, ConfidenceRange.of(0, 1));

        double actual = dop.diseaseObservableGivenAge(BOBOPHOBIA_A, age);
        assertThat(actual, closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "0,  0,  0,      0.99999999",    // congenital
            "0,  0, 29,      0.99999999",    // neonatal
            "0,  1,  0,      0.99999999",    // infantile
    })
    public void onsetProbability_subtypeB(int years, int months, int days, double expected) {
        HpoDiseases diseases = TestResources.hpoDiseases();
        SimpleDiseaseOnsetProbability dop = new SimpleDiseaseOnsetProbability(diseases, STRICT);

        TemporalInterval age = Age.postnatal(years, months, days, ConfidenceRange.of(0, 1));

        double actual = dop.diseaseObservableGivenAge(BOBOPHOBIA_B, age);
        assertThat(actual, closeTo(expected, ERROR));
    }


    @ParameterizedTest
    @CsvSource({
            "0,  0,  0,      0.799999994",    // congenital
            "0,  0, 28,      0.799999994",    // neonatal
            "0,  0, 29,      0.99999999",     // infantile
            "0,  1,  0,      0.99999999",     // infantile
    })
    public void notOnsetProbability_subtypeA(int years, int months, int days, double expected) {
        HpoDiseases diseases = TestResources.hpoDiseases();
        SimpleDiseaseOnsetProbability dop = new SimpleDiseaseOnsetProbability(diseases, STRICT);

        TemporalInterval age = Age.postnatal(years, months, days, ConfidenceRange.of(0, 1));

        double actual = dop.diseaseNotObservableGivenAge(BOBOPHOBIA_A, age);
        assertThat(actual, closeTo(expected, ERROR));
    }
}