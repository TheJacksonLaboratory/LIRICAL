package org.monarchinitiative.lirical.core.analysis;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. The class is intended to be used together
 * with the class {@link AnalysisData}, which contains lists
 * of observed and excluded HPO terms. For each
 * disease in the database, the likelihood ratios of these phenotypes is calculated, and the result
 * for each disease is stored in an object of this class.
 * This object can include the result of a likelihood ratio for a genotype test. However,
 * not every disease is associated with a known disease gene. Therefore, if no genotype is available,
 * {@link #genotypeLr} is null.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.4.5 (2019-10-28)
 */
public class TestResult implements Comparable<TestResult> {
    /**
     * Reference to the disease that we are testing (e.g., OMIM:600100).
     */
    private final TermId diseaseId;
    /**
     * The probability of some result before the first test is done.
     */
    private final double pretestProbability;
    /**
     * A list of results for the tests performed on observed phenotypes for {@link #diseaseId}.
     */
    private final List<LrWithExplanation> observedResults;
    /**
     * A list of test results for phenotypes that were excluded.
     */
    private final List<LrWithExplanation> excludedResults;
    /**
     * Gene id and the result of the likelihood ratio test for the genotype.
     * Null if there is no known gene for the disease.
     */
    private final GenotypeLrWithExplanation genotypeLr;
    /**
     * This is the product of the individual test results.
     */
    private final double compositeLR;
    /**
     * The probability of some result after testing.
     */
    private final double posttestProbability;

    public static TestResult of(TermId diseaseId,
                                double pretestProbability,
                                List<LrWithExplanation> observedResults,
                                List<LrWithExplanation> excludedResults,
                                GenotypeLrWithExplanation genotypeLr) {
        return new TestResult(diseaseId, pretestProbability, observedResults, excludedResults, genotypeLr);
    }

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     *
     * @param diseaseId          ID of the disease being tested
     * @param pretestProbability pretest probability of the disease
     * @param observedResults    list of individual test results for observed phenotypes
     * @param excludedResults    list of individual test results for excluded phenotypes
     * @param genotypeLr         LR result for the genotype
     */
    private TestResult(TermId diseaseId,
                       double pretestProbability,
                       List<LrWithExplanation> observedResults,
                       List<LrWithExplanation> excludedResults,
                       GenotypeLrWithExplanation genotypeLr) {
        this.diseaseId = Objects.requireNonNull(diseaseId);
        this.pretestProbability = pretestProbability;
        this.observedResults = Objects.requireNonNull(observedResults);
        this.excludedResults = Objects.requireNonNull(excludedResults);
        this.compositeLR = calculateCompositeLR(observedResults, excludedResults, genotypeLr);
        this.posttestProbability = calculatePosttestProbability();
        this.genotypeLr = genotypeLr; // nullable
    }

    private static double calculateCompositeLR(List<LrWithExplanation> observed, List<LrWithExplanation> excluded, GenotypeLrWithExplanation genotypeLR) {
        // the composite ratio is equal to the product of the phenotype LR's
        // multiplied by the genotype LR.
        double observedLr = observed.stream().map(LrWithExplanation::lr).reduce(1.0, (a, b) -> a * b);
        double excludedLr = excluded.stream().map(LrWithExplanation::lr).reduce(1.0, (a, b) -> a * b);
        double genotypeLrForCalculationOfCompositeLr = genotypeLR == null ? 1 : genotypeLR.lr();
        return observedLr * excludedLr * genotypeLrForCalculationOfCompositeLr;
    }

    @JsonGetter(value = "observedPhenotypicFeatures")
    public List<LrWithExplanation> observedResults() {
        return observedResults;
    }

    public List<TermId> observedTerms() {
        return observedResults.stream().map(LrWithExplanation::queryTerm).toList();
    }

    @JsonGetter(value = "excludedPhenotypicFeatures")
    public List<LrWithExplanation> excludedResults() {
        return excludedResults;
    }

    public List<TermId> excludedTerms() {
        return excludedResults.stream().map(LrWithExplanation::queryTerm).toList();
    }

    /**
     * @return the composite likelihood ratio (product of the LRs of the individual tests).
     */
    @JsonGetter
    public double getCompositeLR() {
        return compositeLR;
    }

    /**
     * @return the total count of tests performed (excluding genotype).
     */
    @JsonIgnore
    public int getNumberOfTests() {
        return observedResults.size() + excludedResults.size();
    }

    /**
     * @return the pretest odds.
     */
    public double pretestOdds() {
        return pretestProbability / (1.0 - pretestProbability);
    }

    /**
     * @return the post-test odds.
     */
    public double posttestOdds() {
        return pretestOdds() * getCompositeLR();
    }

    @JsonGetter
    public double pretestProbability() {
        return pretestProbability;
    }

    private double calculatePosttestProbability() {
        double po = posttestOdds();
        return po / (1 + po);
    }

    @JsonGetter
    public double posttestProbability() {
        return posttestProbability;
    }

    /**
     * Compare two TestResult objects based on their {@link #compositeLR} value.
     *
     * @param other the "other" TestResult being compared.
     * @return comparison result
     */
    @Override
    public int compareTo(TestResult other) {
        return Double.compare(posttestProbability, other.posttestProbability);
    }

    @Override
    public String toString() {
        String resultlist = observedResults.stream().map(String::valueOf).collect(Collectors.joining(";"));
        String genoResult = genotypeLr == null ? "no genotype LR" : String.format("genotype LR: %.4f", genotypeLr.lr());
        return String.format("%s: %.2f [%s] %s", diseaseId, getCompositeLR(), resultlist, genoResult);
    }

    /**
     * @param i index of the test we are interested in for an observed phenotype
     * @return the likelihood ratio of the i'th test
     */
    public double getObservedPhenotypeRatio(int i) {
        return this.observedResults.get(i).lr();
    }

    /**
     * @param i index of the test we are interested in for an excluded phenotype
     * @return the likelihood ratio of the i'th test
     */
    public double getExcludedPhenotypeRatio(int i) {
        return this.excludedResults.get(i).lr();
    }

    /**
     * @return name of the disease being tested.
     */
    @JsonGetter
    public TermId diseaseId() {
        return diseaseId;
    }

    /**
     * @return true if a genotype likelihood ratio was assigned to this test result.
     */
    @Deprecated(forRemoval = true)
    // REMOVE(v2.0.0)
    public boolean hasGenotypeLR() {
        return false;
    }

    @JsonGetter(value = "genotypeLR")
    public Optional<GenotypeLrWithExplanation> genotypeLr() {
        return Optional.ofNullable(genotypeLr);
    }

    @JsonIgnore
    @Deprecated(forRemoval = true) // get explanations from results
    // REMOVE(v2.0.0)
    public List<String> getObservedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return observedResults.stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    @JsonIgnore
    @Deprecated(forRemoval = true) // get explanations from excludedResults
    // REMOVE(v2.0.0)
    public List<String> getExcludedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return excludedResults.stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    /**
     * Calculate the maximum absolute value of any individual likelihood ratio. This is used to help layout the SVG
     *
     * @return maximum abs(LR)
     */
    @JsonIgnore
    public double getMaximumIndividualLR() {
        double m1 = this.observedResults.stream()
                .map(LrWithExplanation::lr)
                .map(Math::abs)
                .max(Double::compare)
                .orElse(0.0);
        double m2 = this.excludedResults.stream()
                .map(LrWithExplanation::lr)
                .map(Math::abs)
                .max(Double::compare)
                .orElse(0.0);
        double m3 = this.genotypeLr != null ? Math.abs(genotypeLr.lr()) : 0.0;

        return Math.max(m1, Math.max(m2, m3));
    }


}
