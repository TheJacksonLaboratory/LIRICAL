package org.monarchinitiative.lirical.core.analysis.runner;

import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;

/**
 * A {@link org.monarchinitiative.lirical.core.analysis.TestResult} that knows about {@link #onsetLr()}.
 */
class OnsetAwareTestResult extends TestResultBase {

    /**
     * Likelihood ratio of a disease being observable at the age provided in the
     * {@link org.monarchinitiative.lirical.core.analysis.AnalysisData}.
     */
    private final double onsetLr;

    protected OnsetAwareTestResult(TermId diseaseId,
                                   double pretestProbability,
                                   List<LrWithExplanation> observedResults,
                                   List<LrWithExplanation> excludedResults,
                                   GenotypeLrWithExplanation genotypeLr,
                                   Double onsetLr) {
        super(diseaseId, pretestProbability, observedResults, excludedResults, genotypeLr);
        this.onsetLr = onsetLr; // nullable
    }

    @Override
    protected DoubleStream compositeLikelihoodRatioOperands() {
        return DoubleStream.concat(
                observedAndExcludedPhenotypesAndGenotypeLr(),
                DoubleStream.of(onsetLr)
        );
    }
    /**
     * Likelihood ratio of a disease being observable at the age provided in the
     * {@link org.monarchinitiative.lirical.core.analysis.AnalysisData}.
     */
    public double onsetLr() {
        return onsetLr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnsetAwareTestResult that = (OnsetAwareTestResult) o;
        return Double.compare(that.onsetLr, onsetLr) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(onsetLr);
    }

    @Override
    public String toString() {
        return "OnsetAwareTestResult{" +
                "onsetLr=" + onsetLr +
                "} " + super.toString();
    }
}
