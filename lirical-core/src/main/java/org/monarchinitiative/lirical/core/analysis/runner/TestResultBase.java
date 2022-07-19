package org.monarchinitiative.lirical.core.analysis.runner;

import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * Base {@link TestResult} that stores LR components, including observed and negated phenotype terms, and genotype LR.
 */
abstract class TestResultBase implements TestResult {

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

    protected TestResultBase(TermId diseaseId,
                             double pretestProbability,
                             List<LrWithExplanation> observedResults,
                             List<LrWithExplanation> excludedResults,
                             GenotypeLrWithExplanation genotypeLr) {
        this.diseaseId = Objects.requireNonNull(diseaseId);
        this.pretestProbability = pretestProbability;
        this.observedResults = Objects.requireNonNull(observedResults);
        this.excludedResults = Objects.requireNonNull(excludedResults);
        this.genotypeLr = genotypeLr; // nullable
    }

    /**
     * @return name of the disease being tested.
     */
    @Override
    public TermId diseaseId() {
        return diseaseId;
    }

    @Override
    public List<LrWithExplanation> observedResults() {
        return observedResults;
    }

    @Override
    public List<LrWithExplanation> excludedResults() {
        return excludedResults;
    }

    @Override
    public double pretestProbability() {
        return pretestProbability;
    }

    @Override
    public double getCompositeLR() {
        return calculateCompositeLR();
    }

    @Override
    public Optional<GenotypeLrWithExplanation> genotypeLr() {
        return Optional.ofNullable(genotypeLr);
    }

    protected abstract DoubleStream compositeLikelihoodRatioOperands();

    /**
     * Get a {@link Stream} of likelihood ratios corresponding to
     * <ul>
     *     <li>observed phenotype terms,</li>
     *     <li>excluded phenotype terms, and</li>
     *     <li>genotype likelihood ratio</li>
     * </ul>
     */
    protected DoubleStream observedAndExcludedPhenotypesAndGenotypeLr() {
        return DoubleStream.of(
                observedResults.stream()
                        .map(LrWithExplanation::lr)
                        .reduce(1.0, (a, b) -> a * b),
                excludedResults.stream()
                        .map(LrWithExplanation::lr)
                        .reduce(1., (a, b) -> a * b),
                genotypeLr == null
                        ? 1
                        : genotypeLr.lr()
        );
    }

    private double calculateCompositeLR() {
        return compositeLikelihoodRatioOperands()
                .reduce(1., (l, r) -> l * r);
    }

    @Override
    public String toString() {
        String resultlist = observedResults.stream().map(String::valueOf).collect(Collectors.joining(";"));
        String genoResult = genotypeLr == null ? "no genotype LR" : String.format("genotype LR: %.4f", genotypeLr.lr());
        return String.format("%s: %.2f [%s] %s", diseaseId, getCompositeLR(), resultlist, genoResult);
    }
}
