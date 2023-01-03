package org.monarchinitiative.lirical.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AgeTest {

    @Test
    public void testDays() {
        Age twoDays = Age.ageInDays(2);
        assertEquals(0, twoDays.getYears());
        assertEquals(0,twoDays.getMonths());
        assertEquals(2, twoDays.getDays());
        assertTrue(twoDays.isPostnatal());
        assertFalse(twoDays.isGestational());
    }

    @Test
    public void testMonths() {
        Age age = Age.ageInMonths(3);
        assertEquals(0, age.getDays());
        assertEquals(3, age.getMonths());
        assertEquals(0, age.getYears());
        assertTrue(age.isPostnatal());
        assertFalse(age.isGestational());
    }

    @Test
    public void testYears() {
        Age age = Age.ageInYears(13);
        assertEquals(0, age.getDays());
        assertEquals(0, age.getMonths());
        assertEquals(13, age.getYears());
        assertTrue(age.isPostnatal());
        assertFalse(age.isGestational());
    }

    @Test
    public void gestational() {
        Age age = Age.gestationalAge(30, 2);
        assertEquals(0, age.getYears());
        assertEquals(0, age.getMonths());
        assertEquals(30, age.getWeeks());
        assertEquals(2, age.getDays());
        assertFalse(age.isPostnatal());
        assertTrue(age.isGestational());
    }

    @Test
    public void testEquality() {
        Age a = Age.ageInYears(13);
        Age b = Age.ageInYears(13);
        Age c = Age.ageInYears(12);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
