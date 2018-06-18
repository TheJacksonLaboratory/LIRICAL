package org.monarchinitiative.lr2pg.poisson;

import static org.monarchinitiative.lr2pg.poisson.SaddlePointExpansion.TWO_PI;

/**
 * This class and the other classes in this package were adapted and mainly copied from the
 * Apache Math package. There were two goals of the adaptation -- to allow double values
 * (apache just allows integers) and to avoid having to import the entire Apache package
 * into LR2PG.
 */
public class PoissonDistribution {

    private final double mean;

    public PoissonDistribution(double m) {
        this.mean=m;
    }







    /**
     * Get the mean for the distribution.
     *
     * @return the mean for the distribution.
     */
    public double getMean() {
        return mean;
    }

    /** {@inheritDoc} */
    public double probability(double x) {
        final double logProbability = logProbability(x);
        return logProbability == Double.NEGATIVE_INFINITY ? 0 : Math.exp(logProbability);
    }


    public double logProbability(double x) {
        double ret;
        if (x < 0 || x == Integer.MAX_VALUE) {
            ret = Double.NEGATIVE_INFINITY;
        } else if (x == 0) {
            ret = -mean;
        } else {
            try {
                ret = -SaddlePointExpansion.getStirlingError(x) -
                        SaddlePointExpansion.getDeviancePart(x, mean) -
                        0.5 * Math.log(TWO_PI) - 0.5 * Math.log(x);
            } catch (NumberIsTooSmallException | NumberIsTooLargeException e) {
                e.printStackTrace();
                return Double.NEGATIVE_INFINITY;
            }
        }
        return ret;
    }
}
