package org.monarchinitiative.lirical.core.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GenomeBuildTest {

    @ParameterizedTest
    @CsvSource({
            "GRCh37.p13,    HG19",
            "GRCh37,        HG19",
            "hg19,          HG19",

            "GRCh38.p13,    HG38",
            "GRCh38,        HG38",
            "hg38,          HG38",
    })
    public void parse(String payload, GenomeBuild expected) {
        Optional<GenomeBuild> optionalGb = GenomeBuild.parse(payload);
        assertThat(optionalGb.isPresent(), equalTo(true));
        assertThat(optionalGb.get(), equalTo(expected));
    }
}