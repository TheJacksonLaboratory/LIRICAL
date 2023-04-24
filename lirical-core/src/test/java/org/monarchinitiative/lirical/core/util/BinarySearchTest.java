package org.monarchinitiative.lirical.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class BinarySearchTest {
    @ParameterizedTest
    @CsvSource({
            "1|2, 1",
            "1|2, 2",

            "1|2|3|4, 1",
            "1|2|3|4, 2",
            "1|2|3|4, 3",
            "1|2|3|4, 4",
    })
    public void binarySearch_evenItemCount(String payload, int key) {
        String[] array = payload.split("\\|");
        Optional<String> resultArray = BinarySearch.binarySearch(array, Integer::parseInt, key);

        assertThat(resultArray.isPresent(), equalTo(true));
        assertThat(resultArray.get(), equalTo(String.valueOf(key)));
    }

    @ParameterizedTest
    @CsvSource({
            "1|3, 0",
            "1|3, 2",
            "1|3, 4",
    })
    public void binarySearch_evenItemCount_notPresent(String payload, int key) {
        String[] array = payload.split("\\|");
        Optional<String> resultArray = BinarySearch.binarySearch(array, Integer::parseInt, key);

        assertThat(resultArray.isEmpty(), equalTo(true));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 1",

            "1|2|3, 1",
            "1|2|3, 2",
            "1|2|3, 3",

            "1|2|3|4|5, 1",
            "1|2|3|4|5, 2",
            "1|2|3|4|5, 3",
            "1|2|3|4|5, 4",
            "1|2|3|4|5, 5",
    })
    public void binarySearch_oddItemCount(String payload, int key) {
        String[] array = payload.split("\\|");
        Optional<String> resultArray = BinarySearch.binarySearch(array, Integer::parseInt, key);

        assertThat(resultArray.isPresent(), equalTo(true));
        assertThat(resultArray.get(), equalTo(String.valueOf(key)));
    }

    @ParameterizedTest
    @CsvSource({
            "1|3|5, 0",
            "1|3|5, 2",
            "1|3|5, 4",
            "1|3|5, 6",
    })
    public void binarySearch_oddItemCount_notPresent(String payload, int key) {
        String[] array = payload.split("\\|");
        Optional<String> resultArray = BinarySearch.binarySearch(array, Integer::parseInt, key);

        assertThat(resultArray.isEmpty(), equalTo(true));
    }

    @Test
    public void binarySearch_emptyCollection() {
        Optional<String> resultArray = BinarySearch.binarySearch(new String[0], Integer::parseInt, 1);
        assertThat(resultArray.isEmpty(), equalTo(true));
    }
}