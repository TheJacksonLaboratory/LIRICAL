package org.monarchinitiative.lirical.core.likelihoodratio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GenotypeLrMatchTypeTest {

    @ParameterizedTest
    @CsvSource(
            {
                    "NO_VARIANTS_DETECTED_AD,                                  false",
                    "NO_VARIANTS_DETECTED_AR,                                  false",
                    "UNKNOWN,                                                  false",
                    "ONE_P_OR_LP_CLINVAR_ALLELE_IN_AD,                         true",
                    "LIRICAL_GT_MODEL,                                         true",
                    "ONE_DELETERIOUS_CLINVAR_VARIANT_IN_AD,                    true",
                    "TWO_DELETERIOUS_CLINVAR_VARIANTS_IN_AR,                   true",
                    "TWO_P_OR_LP_CLINVAR_ALLELES_IN_AR,                        true",
                    "ONE_DELETERIOUS_VARIANT_IN_AR,                            true",
                    "HIGH_NUMBER_OF_OBSERVED_PREDICTED_PATHOGENIC_VARIANTS,    true",
            }
    )
    public void hasDeleteriousVariants(String value, boolean expected) {
        // We depend on correctness of `hasDeleteriousVariants` in HTML and TSV report generators,
        // where we may choose to only show the diff dgs with a deleterious variant.
        GenotypeLrMatchType glmt = GenotypeLrMatchType.valueOf(value);

        assertThat(glmt.hasDeleteriousVariants(), equalTo(expected));
    }
}