package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.runner.TestResultDefault;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Optional;

/**
 * {@link TestResult} organizes information about the result of a likelihood ratio test for proband and a disease.
 * The class is intended to be used together with the class {@link AnalysisData}, which contains lists of observed
 * and excluded HPO terms. For each disease in the database, the likelihood ratios of these phenotypes is calculated,
 * and {@link TestResult} for each disease is stored in an object of this class.
 * <p>
 * {@link TestResult} can include the result of a likelihood ratio for a genotype test and
 * a likelihood ratio of the disease being observable at a given age.
 * However, not every disease is associated with a known disease gene. Therefore, if no genotype is available,
 * {@link #genotypeLr} is empty.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.4.5 (2019-10-28)
 */
public interface TestResult extends LRTestResult {

    static TestResult of(TermId diseaseId,
                         double pretestProbability,
                         List<LrWithExplanation> observedResults,
                         List<LrWithExplanation> excludedResults,
                         GenotypeLrWithExplanation genotypeLr) {
        return new TestResultDefault(diseaseId, pretestProbability, observedResults, excludedResults, genotypeLr);
    }

    /**
     * @return name of the disease being tested.
     */
    TermId diseaseId();

    /**
     * A list of results for the tests performed on observed phenotypes for {@link #diseaseId}.
     */
    List<LrWithExplanation> observedResults();

    /**
     * A list of test results for phenotypes that were excluded.
     */
    List<LrWithExplanation> excludedResults();

    /**
     * {@link GenotypeLrWithExplanation} with the result of the likelihood ratio test for the genotype or
     * {@link Optional#empty()} if there is no known gene for the disease.
     */
    Optional<GenotypeLrWithExplanation> genotypeLr();


    /* ****************************** DERIVED METHODS *************************************************************** */

    default List<TermId> observedTerms() {
        return observedResults().stream()
                .map(LrWithExplanation::queryTerm)
                .toList();
    }

    default List<TermId> excludedTerms() {
        return excludedResults().stream()
                .map(LrWithExplanation::queryTerm)
                .toList();
    }

    /**
     * @param i index of the test we are interested in for an observed phenotype
     * @return the likelihood ratio of the i'th test
     */
    default double getObservedPhenotypeRatio(int i) {
        return this.observedResults().get(i).lr();
    }

    /**
     * @param i index of the test we are interested in for an excluded phenotype
     * @return the likelihood ratio of the i'th test
     */
    default double getExcludedPhenotypeRatio(int i) {
        return this.excludedResults().get(i).lr();
    }

    /**
     * @return the total count of tests performed (excluding genotype).
     */
    default int getNumberOfTests() {
        return observedResults().size() + excludedResults().size();
    }

    @Deprecated(forRemoval = true) // get explanations from results
    default List<String> getObservedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return observedResults().stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    @Deprecated(forRemoval = true) // get explanations from excludedResults
    default List<String> getExcludedPhenotypeExplanation() {
        // TODO - this may need to be provided in reverse order
        return excludedResults().stream()
                .map(LrWithExplanation::escapedExplanation)
                .toList();
    }

    /**
     * @return true if a genotype likelihood ratio was assigned to this test result.
     */
    @Deprecated(forRemoval = true)
    default boolean hasGenotypeLR() {
        return genotypeLr().isPresent();
    }

    /**
     * Calculate the maximum absolute value of any individual likelihood ratio. This is used to help layout the SVG
     *
     * @return maximum abs(LR)
     */
    default double getMaximumIndividualLR() {
        double m1 = observedResults().stream()
                .map(LrWithExplanation::lr)
                .map(Math::abs)
                .max(Double::compare)
                .orElse(0.0);
        double m2 = excludedResults().stream()
                .map(LrWithExplanation::lr)
                .map(Math::abs)
                .max(Double::compare)
                .orElse(0.0);
        double m3 = genotypeLr()
                .map(lr -> Math.abs(lr.lr()))
                .orElse(0.);

        return Math.max(m1, Math.max(m2, m3));
    }

}
