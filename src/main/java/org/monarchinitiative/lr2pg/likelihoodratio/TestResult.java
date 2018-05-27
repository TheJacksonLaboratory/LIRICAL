package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lr2pg.hpo.HpoCase;

import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. For instance,  a GDx VCC test for
 * glaucoma may result in a measurement of 48, a value that is known to
 * have a 60% sensitivity and 97% specificity for glaucoma. In general, we need to have the sensitivity and the
 * specificity of a test result in order to perform a likelihood ratio test. The numerical value of the test
 * (in this case, 48) is not important.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @author <a href="mailto:vida.ravanmehr@jax.org">Vida Ravanmehr</a>
 * @version 0.3.3 (2018-05-15)
 */
public class TestResult implements Comparable<TestResult> {

    /**
     * A list of results for the tests performed in a {@link HpoCase} case.
     * To save space, we only record the result of the test, and we assume that the order of the test is the same
     * as indicated in the case object. In the intended use case, there will be one test result for each disease
     * that is tested for the {@link HpoCase} object.
     */
    private final ImmutableList<Double> results;
    /**
     * This is the product of the individual test results
     */
    private final double compositeLR;
    /**
     * The name of the disease that we are testing for this time.
     */
    private final String diseasename;

    private final double pretestProbability;

    /**
     * The constructor initializes the variables and calculates {@link #compositeLR}
     *
     * @param reslist list of individual test results
     * @param name    name of the disease being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(ImmutableList<Double> reslist, String name, double pretest) {
        results = reslist;
        diseasename = name;
        this.pretestProbability = pretest;
        // the composite LR is the product of the individual LR's
        compositeLR=reslist.stream().reduce(1.0, (a, b) -> a * b);
    }

    /**
     * @return the composite likelihood ratio (product of the LRs of the individual tests).
     */
    public double getCompositeLR() {
        return compositeLR;
    }

    /**
     * @return the total count of tests performed.
     */
    public int getNumberOfTests() {
        return results.size();
    }

    /**
     * @return the pretest odds.
     */
    public double pretestodds() {
        return pretestProbability / (1.0 - pretestProbability);
    }

    /**
     * @return the post-test odds
     */
    public double posttestodds() {
        double pretestodds = pretestodds();
        return pretestodds * getCompositeLR();
    }


    public double getPretestProbability() {
        return pretestProbability;
    }

    public double getPosttestProbability() {
        double po = posttestodds();
        return po / (1 + po);
    }


    /**
     * Compare two TestResult objects based on their {@link #compositeLR} value.
     * @param other the "other" TestResult being compared.
     * @return
     */
    @Override
    public int compareTo(TestResult other) {
        return Double.compare(compositeLR, other.compositeLR);
    }


    @Override
    public String toString() {
        String resultlist = results.stream().map(String::valueOf).collect(Collectors.joining(";"));
        return String.format("%s: %.2f [%s]", diseasename, compositeLR, resultlist);
    }

    /**
     * @param i index of the test we are interested in
     * @return the likelihood ratio of the i'th test
     */
    public double getRatio(int i) {
        return this.results.get(i);
    }

    /** @return name of the disease being tested. */
    public String getDiseasename() {
        return diseasename;
    }
}
