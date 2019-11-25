package org.monarchinitiative.lirical.configuration;

import javax.validation.constraints.Min;

/**
 * This class encapsulates the minimum number of diagnoses to show, which is an optional user argument (-m).
 * If the user has set a value, then we show at least this number of diagnoses in the detailed HTML output.
 */
public class MinDiagnosisCount {
    /** True if the user passed a -m/--mindiff flag */
    private final boolean setByUser;

    private final int minToShow;


    private MinDiagnosisCount(int minD, boolean setByUser) {
        this.setByUser = setByUser;
        this.minToShow = minD;
    }

    static MinDiagnosisCount notInitialized() {
        return new MinDiagnosisCount(LiricalFactory.DEFAULT_MIN_DIFFERENTIALS, false);
    }

    static MinDiagnosisCount setToUserDefinedMinCount(int m) {
        return new MinDiagnosisCount(m, true);
    }

    public boolean isSetByUser() {
        return setByUser;
    }

    public int getMinToShow() {
        return minToShow;
    }
}
