package org.monarchinitiative.lirical.core.analysis;

/**
 * The interface describes API of the result of one or more likelihood ratio (LR) tests. The API also provides default
 * implementations of LR-related values, such as {@link #pretestOdds()}, {@link #posttestProbability()}, calculated from
 * {@link #pretestProbability()} and {@link #getCompositeLR()}.
 */
public interface LRTestResult extends Comparable<TestResult> {

    /**
     * The probability of some result before the first test is done.
     */
    double pretestProbability();

    /**
     * @return the composite likelihood ratio (product of the LRs of the individual tests).
     */
    double getCompositeLR();

    /* ****************************** DERIVED METHODS *************************************************************** */

    /**
     * @return the pretest odds.
     */
    default double pretestOdds() {
        return pretestProbability() / (1.0 - pretestProbability());
    }

    /**
     * @return the post-test odds.
     */
    default double posttestOdds() {
        return pretestOdds() * getCompositeLR();
    }

    /**
     * The probability of some result after testing.
     */
    default double posttestProbability() {
        double po = posttestOdds();
        return po / (1 + po);
    }

    /**
     * Compare two {@link LRTestResult} objects based on their {@link #posttestProbability()} value.
     *
     * @param other the "other" {@link LRTestResult} being compared.
     * @return comparison result
     */
    @Override
    default int compareTo(TestResult other) {
        return Double.compare(this.posttestProbability(), other.posttestProbability());
    }

}
