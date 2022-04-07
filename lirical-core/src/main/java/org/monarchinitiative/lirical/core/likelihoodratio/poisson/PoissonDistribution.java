package org.monarchinitiative.lirical.core.likelihoodratio.poisson;

import static org.monarchinitiative.lirical.core.likelihoodratio.poisson.SaddlePointExpansion.TWO_PI;

/**
 * This class and the other classes in this package were adapted and mainly copied from the
 * Apache Math package. There were two goals of the adaptation -- to allow double values
 * (apache just allows integers) and to avoid having to import the entire Apache package
 * into LIRICAL.
 */
public class PoissonDistribution {

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


    public double logProbability(double x) {
        if (x < 0 || x == Integer.MAX_VALUE) {
            return Double.NEGATIVE_INFINITY;
        } else if (x == 0) {
            return -mean;
        } else {
            try {
                return -SaddlePointExpansion.getStirlingError(x) -
                                SaddlePointExpansion.getDeviancePart(x, mean) -
                                0.5 * Math.log(TWO_PI) - 0.5 * Math.log(x);
            } catch (NumberIsTooSmallException | NumberIsTooLargeException e) {
                e.printStackTrace();
                return Double.NEGATIVE_INFINITY;
            }
        }
    }
}
