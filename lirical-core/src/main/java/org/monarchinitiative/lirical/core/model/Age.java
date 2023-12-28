package org.monarchinitiative.lirical.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Period;
import java.util.Objects;

/**
 * Convenience class to represent the age of a subject.
 * <p>
 * We represent both <em>postnatal</em> and <em>gestational</em> age. Use {@link #isGestational()}
 * or {@link #isPostnatal()} to tell them apart.
 * <p>
 * The postnatal age has {@link #getYears()}, {@link #getMonths()}, and {@link #getDays()} fields set
 * and {@link #getWeeks()} should be ignored.
 * <p>
 * The gestational age uses {@link #getWeeks()} and {@link #getDays()} fields.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@JsonSerialize(using = AgeSerializer.class)
public class Age {
    private final boolean isGestational;
    private final int years;
    private final int months;
    private final int weeks;
    private final int days;

    private Age(int years, int months, int weeks, int days, boolean isGestational) {
        this.years=requireNonNegativeInt(years, "Years must not be negative");
        this.months=requireNonNegativeInt(months, "Months must not be negative");
        this.weeks=requireNonNegativeInt(weeks, "Weeks must not be negative");
        this.days=requireNonNegativeInt(days, "Days must not be negative");
        this.isGestational = isGestational;
    }

    @JsonIgnore
    public int getYears() {
        return years;
    }

    @JsonIgnore
    public int getMonths() {
        return months;
    }

    @JsonIgnore
    public int getWeeks() {
        return weeks;
    }

    @JsonIgnore
    public int getDays() {
        return days;
    }

    @JsonIgnore
    public boolean isGestational() {
        return isGestational;
    }

    @JsonIgnore
    public boolean isPostnatal() {
        return !isGestational;
    }

    /**
     * Create a postnatal age to represent {@code y} years of age.
     *
     * @param y a non-negative number of years.
     */
    public static Age ageInYears(int y) {
        return of(y,0,0);
    }

    /**
     * Create a postnatal age to represent {@code m} months of age.
     *
     * @param m a non-negative number of months.
     */
    public static Age ageInMonths(int m) {
        return of(0,m,0);
    }

    /**
     * Create a postnatal age to represent {@code d} days of age.
     *
     * @param d a non-negative number of days.
     */
    public static Age ageInDays(int d) {
        return of(0,0,d);
    }

    /**
     * @param period representing <em>postnatal</em> (<em>not</em> gestational) age.
     */
    public static Age parse(Period period) {
        Period normalized = period.normalized();
        return of(normalized.getYears(), normalized.getMonths(), normalized.getDays());
    }

    /**
     * Create a gestational age to represent {@code weeks} and {@code days}.
     * <p>
     * {@code weeks} should generally be not be greater than 42, and it must not be negative.
     * {@code days} must be in range {@code [0,6]}.
     *
     * @param weeks a non-negative number of completed gestational weeks.
     * @param days the number of completed gestational days.
     */
    public static Age gestationalAge(int weeks, int days) {
        return new Age(0, 0, weeks, days, true);
    }

    /**
     * Create a <em>postnatal</em> age from given inputs.
     */
    public static Age of(int years, int months, int days) {
        return new Age(years, months, 0, days, false);
    }

    private static int requireNonNegativeInt(int value, String msg) {
        if (value < 0) {
            throw new IllegalArgumentException(msg);
        } else
            return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Age age = (Age) o;
        return years == age.years &&
                months == age.months &&
                weeks == age.weeks &&
                days == age.days;
    }

    @Override
    public int hashCode() {
        return Objects.hash(years, months, weeks, days);
    }

    @Override
    public String toString() {
        return "Age{" +
                "years=" + years +
                ", months=" + months +
                ", weeks=" + weeks +
                ", days=" + days +
                '}';
    }
}
