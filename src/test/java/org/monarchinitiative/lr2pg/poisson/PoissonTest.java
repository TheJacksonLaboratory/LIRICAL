package org.monarchinitiative.lr2pg.poisson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This is just testing that our adaptation of the Apache math Poisson implemention is working.
 * The expected values were calculated in R with commands such as {@code dpois(3,2.2)}.
 */
public class PoissonTest {

    private final double EPSILON=0.00001;

    @Test
    public void testPoisson1() {
        double lambda = 2.2;
        double observedCount = 3;
        double expectedProbability = 0.1966387;
        PoissonDistribution poissonDistribution = new PoissonDistribution(lambda);
        double prob = poissonDistribution.probability(observedCount);
        assertEquals(expectedProbability,prob,EPSILON);
    }

    @Test
    public void testPoisson2() {
        double lambda = 3.2;
        double observedCount = 3;
        double expectedProbability = 0.222616;
        PoissonDistribution poissonDistribution = new PoissonDistribution(lambda);
        double prob = poissonDistribution.probability(observedCount);
        assertEquals(expectedProbability,prob,EPSILON);
    }


    @Test
    public void testPoisson3() {
        double lambda = 0.0001;
        double observedCount = 1;
        double expectedProbability = 9.999e-05;
        PoissonDistribution poissonDistribution = new PoissonDistribution(lambda);
        double prob = poissonDistribution.probability(observedCount);
        assertEquals(expectedProbability,prob,EPSILON);
    }
}
