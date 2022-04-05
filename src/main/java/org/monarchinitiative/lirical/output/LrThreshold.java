package org.monarchinitiative.lirical.output;

import java.util.Objects;

/**
 * This class encapsulates the likelihood ratio threshold, which is an optional user argument (-t).
 * If the user has set a value, then we only show d iagnoses in the detailed HTML output if it exceeds
 * or equals this threshold. There is an overall minimum number to show (3).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class LrThreshold {
    /**
     * Default threshold for showing a candidate.
     */
    private static final double DEFAULT_LR_THRESHOLD = 0.05;

    /** If the user sets -t, we still show at least this amount even if fewer candidates are above threshold. */
    private final static int MINIMUM_TO_SHOW_IN_THRESHOLD_MODE = 3;

    /** True if the user passed a -m/--mindiff flag */
    private final boolean setByUser;

    private final double threshold;


    private LrThreshold(double threshold, boolean setByUser) {
        this.setByUser = setByUser;
        this.threshold = threshold;
    }

    public static LrThreshold notInitialized() {
        return new LrThreshold(DEFAULT_LR_THRESHOLD, false);
    }

    public static LrThreshold setToUserDefinedThreshold(double t) {
        return new LrThreshold(t, true);
    }

    public boolean isSetByUser() {
        return setByUser;
    }

    public double getThreshold() {
        return threshold;
    }

    public int getMinimumToShowInThresholdMode() {
        return MINIMUM_TO_SHOW_IN_THRESHOLD_MODE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LrThreshold that = (LrThreshold) o;
        return setByUser == that.setByUser && Double.compare(that.threshold, threshold) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(setByUser, threshold);
    }

    @Override
    public String toString() {
        return "LrThreshold{" +
                "setByUser=" + setByUser +
                ", threshold=" + threshold +
                '}';
    }
}
