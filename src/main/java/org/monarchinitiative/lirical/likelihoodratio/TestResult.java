package org.monarchinitiative.lirical.likelihoodratio;


import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. The class is intended to be used together
 * with the class {@link HpoCase}, which contains lists of observed and excluded HPO terms. For each
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
     * A list of results for the tests performed on observed phenotypes for {@link #disease}.
     */
    private final List<LrWithExplanation> results;
    /**
     * A list of test results for phenotypes that were excluded.
     */
    private final List<LrWithExplanation> excludedResults;
    /**
     * Gene id and the result of the likelhood ratio test for the genotype.
     */
    private final GenotypeLrWithExplanation genotypeLr;
    /**
     * This is the product of the individual test results.
     */
    private final double compositeLR;
    /**
     * Reference to the disease that we are testing (e.g., OMIM:600100).
     */
    // TODO - do we need an entire disease here? How about just ID, or nothing at all?
    private final HpoDisease disease;
    /**
     * The probability of some result before the first test is done.
     */
    private final double pretestProbability;
    /**
     * The probability of some result after testing.
     */
    private final double posttestProbability;
    /**
     * The overall rank of the result within the differential diagnosis.
     */
    // TODO - we should remove the rank - not a business of this class
    private int rank;

    public static TestResult of(List<LrWithExplanation> observed,
                                List<LrWithExplanation> excluded,
                                HpoDisease disease,
                                double pretestProbability,
                                GenotypeLrWithExplanation genotypeLr) {
        return new TestResult(observed, excluded, disease, pretestProbability, genotypeLr);
    }

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     *
     * @param observed           list of individual test results for observed phenotypes
     * @param excluded           list of individual test results for excluded phenotypes
     * @param disease            name of the disease being tested
     * @param pretestProbability pretest probability of the disease
     * @param genotypeLr         LR result for the genotype
     */
    private TestResult(List<LrWithExplanation> observed,
                       List<LrWithExplanation> excluded,
                       HpoDisease disease,
                       double pretestProbability,
                       GenotypeLrWithExplanation genotypeLr) {
        this.results = Objects.requireNonNull(observed);
        this.excludedResults = Objects.requireNonNull(excluded);
        this.disease = Objects.requireNonNull(disease);
        this.compositeLR = calculateCompositeLR(observed, excluded, genotypeLr);
        this.pretestProbability = pretestProbability;
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

    /**
     * @return the composite likelihood ratio (product of the LRs of the individual tests).
     */
    public double getCompositeLR() {
        return compositeLR;
    }

    /**
     * @return the total count of tests performed (excluding genotype).
     */
    public int getNumberOfTests() {
        return results.size() + excludedResults.size();
    }

    /**
     * @return the pretest odds.
     */
    public double pretestodds() {
        return pretestProbability / (1.0 - pretestProbability);
    }

    /**
     * @return the post-test odds.
     */
    public double posttestodds() {
        double pretestodds = pretestodds();
        return pretestodds * getCompositeLR();
    }


    public double getPretestProbability() {
        return pretestProbability;
    }

    public double calculatePosttestProbability() {
        double po = posttestodds();
        return po / (1 + po);
    }

    @Deprecated(forRemoval = true) // not a business of this class
    public int getRank() {
        return rank;
    }

    @Deprecated(forRemoval = true) // not a business of this class
    public void setRank(int rank) {
        this.rank = rank;
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
        String resultlist = results.stream().map(String::valueOf).collect(Collectors.joining(";"));
        String genoResult = hasGenotypeLR() ? String.format("genotype LR: %.4f", genotypeLr.lr()) : "no genotype LR";
        return String.format("%s: %.2f [%s] %s", disease.id(), getCompositeLR(), resultlist, genoResult);
    }

    /**
     * @param i index of the test we are interested in for an observed phenotype
     * @return the likelihood ratio of the i'th test
     */
    public double getObservedPhenotypeRatio(int i) {
        return this.results.get(i).lr();
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
    public TermId diseaseId() {
        return disease.getDiseaseDatabaseId();
    }

    /**
     * @return the name of the disease, e.g., Marfan syndrome.
     * @deprecated use {@link #diseaseId()}
     */
    @Deprecated(forRemoval = true)
    public String getDiseaseName() {
        return disease.getDiseaseName();
    }

    /**
     * @return true if a genotype likelihood ratio was assigned to this test result.
     */
    @Deprecated(forRemoval = true)
    public boolean hasGenotypeLR() {
        return false;
    }

    public Optional<GenotypeLrWithExplanation> genotypeLr() {
        return Optional.ofNullable(genotypeLr);
    }

    public Optional<String> getGenotypeExplanation() {
        return genotypeLr().map(GenotypeLrWithExplanation::explanation);
    }

    @Deprecated(forRemoval = true) // get explanations from results
    public List<String> getObservedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return results.stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    @Deprecated(forRemoval = true) // get explanations from excludedResults
    public List<String> getExcludedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return excludedResults.stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    @Deprecated(forRemoval = true)
    public boolean hasGenotypeExplanation() {
        return genotypeLr != null;
    }

    /**
     * Calculate the maximum absolute value of any individual likelihood ratio. This is used to help layout the SVG
     *
     * @return maximum abs(LR)
     */
    public double getMaximumIndividualLR() {
        double m1 = this.results.stream()
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
