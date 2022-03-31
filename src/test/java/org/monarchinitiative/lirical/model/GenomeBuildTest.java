package org.monarchinitiative.lirical.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

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