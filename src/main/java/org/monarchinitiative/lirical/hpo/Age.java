package org.monarchinitiative.lirical.hpo;

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
        return new Age();
    }

    public static Age ageInYears(int y) {
        return new Age(y,0,0);
    }

    public static Age ageInMonths(int m) {
        return new Age(0,m,0);
    }

    public static Age ageInDays(int d) {
        return new Age(0,0,d);
    }
}
