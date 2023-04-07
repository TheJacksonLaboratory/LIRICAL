package org.monarchinitiative.lirical.core.likelihoodratio.poisson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class and the other classes in this package were adapted and mainly copied from the
 * Apache Math package. There were two goals of the adaptation -- to allow double values
 * (apache just allows integers) and to avoid having to import the entire Apache package
 * into LIRICAL.
 */
public class PoissonDistribution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoissonDistribution.class);

    private final double mean;

    public PoissonDistribution(double mean) {
        this.mean=mean;
    }

    /**
     * Get the mean for the distribution.
     *
     * @return the mean for the distribution.
     */
    public double getMean() {
        return mean;
    }

    public double probability(double x) {
        final double logProbability = logProbability(x);
        return logProbability == Double.NEGATIVE_INFINITY ? 0 : Math.exp(logProbability);
    }


    private double logProbability(double x) {
        if (x < 0 || x == Integer.MAX_VALUE) {
            return Double.NEGATIVE_INFINITY;
        } else if (x == 0) {
            return -mean;
        } else {
            try {
                return -SaddlePointExpansion.getStirlingError(x) -
                                SaddlePointExpansion.getDeviancePart(x, mean) -
                                0.5 * Math.log(SaddlePointExpansion.TWO_PI) - 0.5 * Math.log(x);
            } catch (NumberIsTooSmallException | NumberIsTooLargeException e) {
                LOGGER.warn("{}", e.getMessage());
                LOGGER.debug("{}", e.getMessage(), e);
                return Double.NEGATIVE_INFINITY;
            }
        }
    }
}
