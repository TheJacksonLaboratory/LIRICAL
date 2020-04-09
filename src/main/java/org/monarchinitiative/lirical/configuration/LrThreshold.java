package org.monarchinitiative.lirical.configuration;

/**
 * This class encapsulates the likelihood ratio threshold, which is an optional user argument (-t).
 * If the user has set a value, then we only show d iagnoses in the detailed HTML output if it exceeds
 * or equals this threshold. There is an overall minimum number to show (3).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class LrThreshold {
    /** True if the user passed a -m/--mindiff flag */
    private final boolean setByUser;

    private final double threshold;
    /** If the user sets -t, we still show at least this amount even if fewer candidates are above threshold. */
    final private static int MINIMUM_TO_SHOW_IN_THRESHOLD_MODE = 3;

    private LrThreshold(double t, boolean setByUser) {
        this.setByUser = setByUser;
        this.threshold = t;
    }

    static LrThreshold notInitialized() {
        return new LrThreshold(LiricalFactory.DEFAULT_LR_THRESHOLD, false);
    }

    static LrThreshold setToUserDefinedThreshold(double t) {
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
}
