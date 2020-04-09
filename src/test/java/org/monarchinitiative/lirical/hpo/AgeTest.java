package org.monarchinitiative.lirical.hpo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AgeTest {

    @Test
    void testDays() {
        Age twoDays = Age.ageInDays(2);
        assertEquals(0, twoDays.getYears());
        assertEquals(0,twoDays.getMonths());
        assertEquals(2, twoDays.getDays());
    }

    @Test
    void testMonths() {
        Age threeMonths = Age.ageInMonths(3);
        assertEquals(0, threeMonths.getDays());
        assertEquals(3, threeMonths.getMonths());
        assertEquals(0, threeMonths.getYears());
    }

    @Test
    void testYears() {
        Age thirteenYears = Age.ageInYears(13);
        assertEquals(0, thirteenYears.getDays());
        assertEquals(0, thirteenYears.getMonths());
        assertEquals(13, thirteenYears.getYears());
    }

    @Test
    void testEquality() {
        Age a = Age.ageInYears(13);
        Age b = Age.ageInYears(13);
        Age c = Age.ageInYears(12);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
