package org.monarchinitiative.lirical.core.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AgeParserTest {

    @ParameterizedTest
    @CsvSource({
            "P0D,         0,    1",
            "P1D,         1,    1",

            "P0M,         0,   30",
            "P1M,        31,   30",

            "P0Y,         0,  365",
            "P1Y,       366,  365",

            "P1Y0M0D,   366,    1",
            "P1Y0D,     366,    1",
            "P1Y,       366,  365",
            "P1Y,       366,  365",

            // the part after `T` is ignored
            "P1YT1H,    366,  365",
            "P1YT1M,    366,  365",
            "P1YT1S,    366,  365",
    })
    public void parse_postnatal(String payload, int days, int length) throws Exception {
        Age age = AgeParser.parse(payload);

        assertThat(age.days(), equalTo(days));
        assertThat(age.length(), equalTo(length));
        assertThat(age.isPostnatal(), equalTo(true));
    }

    @ParameterizedTest
    @CsvSource({
            "P0W,       0,    7",
            "P1W,       7,    7",
            "P2W,      14,    7",
            "P1W0D,     7,    1",
            "P1W1D,     8,    1",
            "P0W0D,     0,    1", // the only way how to get gestational age of 0 days
            "P0W1D,     1,    1", // the only way how to get gestational age of 1 days

            // the part after `T` is ignored
            "P0W1DT1H,  1,    1",
            "P0W1DT1M,  1,    1",
            "P0W1DT1S,  1,    1",
    })
    public void parse_gestational(String payload, int days, int length) throws Exception {
        Age age = AgeParser.parse(payload);

        assertThat(age.days(), equalTo(days));
        assertThat(age.length(), equalTo(length));
        assertThat(age.isPostnatal(), equalTo(false));
    }

    @Test
    public void nullThrowsAnException() {
        assertThrows(NullPointerException.class, () -> AgeParser.parse(null));
    }

    @ParameterizedTest
    @CsvSource({
            "PP1Y",
            "PT1M",
    })
    public void invalidFormatThrowsAnException(String payload) {
        AgeParseException e = assertThrows(AgeParseException.class, () -> AgeParser.parse(payload));
        assertThat(e.getMessage(), containsString("Invalid age format %s".formatted(payload)));
    }

    @ParameterizedTest
    @CsvSource({
            "P1Y1W",
            "P1M1W",
    })
    public void invalidTemporalFieldsThrowAnException(String payload) {
        AgeParseException e = assertThrows(AgeParseException.class, () -> AgeParser.parse(payload));
        assertThat(e.getMessage(), containsString("Invalid age format %s".formatted(payload)));
    }
}