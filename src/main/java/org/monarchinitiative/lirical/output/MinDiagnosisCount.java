package org.monarchinitiative.lirical.output;

import java.util.Objects;

/**
 * This class encapsulates the minimum number of diagnoses to show, which is an optional user argument (-m).
 * If the user has set a value, then we show at least this number of diagnoses in the detailed HTML output.
 */
public class MinDiagnosisCount {
    /**
     * Default number of differentials to show on the HTML output.
     */
    private static final int DEFAULT_MIN_DIFFERENTIALS = 10;

    /** True if the user passed a -m/--mindiff flag */
    private final boolean setByUser;

    private final int minToShow;


    private MinDiagnosisCount(int minD, boolean setByUser) {
        this.setByUser = setByUser;
        this.minToShow = minD;
    }

    public static MinDiagnosisCount notInitialized() {
        return new MinDiagnosisCount(DEFAULT_MIN_DIFFERENTIALS, false);
    }

    public static MinDiagnosisCount setToUserDefinedMinCount(int m) {
        return new MinDiagnosisCount(m, true);
    }

    public boolean isSetByUser() {
        return setByUser;
    }

    public int getMinToShow() {
        return minToShow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinDiagnosisCount that = (MinDiagnosisCount) o;
        return setByUser == that.setByUser && minToShow == that.minToShow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(setByUser, minToShow);
    }

    @Override
    public String toString() {
        return "MinDiagnosisCount{" +
                "setByUser=" + setByUser +
                ", minToShow=" + minToShow +
                '}';
    }
}
