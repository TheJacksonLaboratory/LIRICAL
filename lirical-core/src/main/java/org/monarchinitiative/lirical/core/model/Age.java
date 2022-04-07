package org.monarchinitiative.lirical.core.model;

import java.time.Period;
import java.util.Objects;

/**
 * Convenience class to represent the age of a proband. Note that if (@link #initialized} is false,
 * then we are representing the fact that we do not know the age and we will disregard the feature
 * in our calculations. We will represent prenatal age as negative values.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Age {
    private final boolean initialized;
    private final int years;
    private final int months;
    private final int days;
    /** Used as a constant if we do not have information about the age of a proband. */
    private final static Age NOT_KNOWN = new Age();

    private Age(int years, int months, int days) {
        this.years=years;
        this.months=months;
        this.days=days;
        initialized=true;
    }

    private Age() {
        this.years=0;
        this.months=0;
        this.days=0;
        initialized=false;
    }

    public static Age ageNotKnown() {
        return NOT_KNOWN;
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public static Age ageInYears(int y) {
        return of(y,0,0);
    }

    public static Age ageInMonths(int m) {
        return of(0,m,0);
    }

    public static Age ageInDays(int d) {
        return of(0,0,d);
    }

    public static Age parse(Period period) {
        Period normalized = period.normalized();
        return of(normalized.getYears(), normalized.getMonths(), normalized.getDays());
    }

    public static Age of(int years, int months, int days) {
        return new Age(years, months, days);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Age age = (Age) o;
        return initialized == age.initialized &&
                years == age.years &&
                months == age.months &&
                days == age.days;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialized, years, months, days);
    }

    @Override
    public String toString() {
        return "Age{" +
                "initialized=" + initialized +
                ", years=" + years +
                ", months=" + months +
                ", days=" + days +
                '}';
    }
}
