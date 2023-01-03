package org.monarchinitiative.lirical.core.model;

import java.time.Period;
import java.util.Objects;

/**
 * Convenience class to represent the age of a proband. Note that if (@link #initialized} is false,
 * then we are representing the fact that we do not know the age we will disregard the feature
 * in our calculations. We will represent prenatal age as number of completed gestational weeks and days,
 * and {@link #isGestational()} flag will be set.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Age {
    private final boolean isUnknown;
    private final boolean isGestational;
    private final int years;
    private final int months;
    private final int weeks;
    private final int days;
    /** Used as a constant if we do not have information about the age of a proband. */
    private final static Age NOT_KNOWN = new Age();

    private Age(int years, int months, int weeks, int days) {
        this.years=years;
        this.months=months;
        this.weeks=weeks;
        this.days=days;
        this.isUnknown = false;
        this.isGestational = weeks != 0;
    }

    private Age() {
        this.years=0;
        this.months=0;
        this.weeks=0;
        this.days=0;
        this.isUnknown = true;
        this.isGestational = false;
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

    public int getWeeks() {
        return weeks;
    }

    public int getDays() {
        return days;
    }

    public boolean isUnknown() {
        return isUnknown;
    }

    public boolean isGestational() {
        return isGestational;
    }

    public boolean isPostnatal() {
        return !isGestational;
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

    /**
     * @param period representing <em>postnatal</em> (<em>not</em> gestational) age.
     * @return age object
     */
    public static Age parse(Period period) {
        Period normalized = period.normalized();
        return of(normalized.getYears(), normalized.getMonths(), normalized.getDays());
    }

    public static Age gestationalAge(int weeks, int days) {
        return new Age(0, 0, weeks, days);
    }

    /**
     * Create a <em>postnatal</em> age from given inputs.
     */
    public static Age of(int years, int months, int days) {
        return new Age(years, months, 0, days);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Age age = (Age) o;
        return isUnknown == age.isUnknown &&
                years == age.years &&
                months == age.months &&
                weeks == age.weeks &&
                days == age.days;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isUnknown, years, months, weeks, days);
    }

    @Override
    public String toString() {
        return "Age{" +
                "isUnknown=" + isUnknown +
                ", years=" + years +
                ", months=" + months +
                ", weeks=" + weeks +
                ", days=" + days +
                '}';
    }
}
