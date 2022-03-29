package org.monarchinitiative.lirical.hpo;


import com.google.common.collect.ImmutableList;

import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.stream.Collectors;


/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2018-04-04)
 */
public final class HpoCase {
    private static final Logger logger = LoggerFactory.getLogger(HpoCase.class);
    /** List of Hpo terms for our case. */
    private final List<TermId> observedAbnormalities;
    /** List of excluded Hpo terms for our case. */
    private final List<TermId> excludedAbnormalities;
    /** One of Male, Female, Unknown. See {@link Sex}. */
    private final Sex sex;
    /** Age of the proband, if known. */
    private final Age age;

    private final AnalysisResults results;

    private HpoCase(List<TermId> observedTerms, List<TermId> excludedTerms, AnalysisResults results, Sex sex, Age age) {
        this.observedAbnormalities = observedTerms;
        this.excludedAbnormalities = excludedTerms;
        this.results = results;
        this.sex = sex;
        this.age = age;
    }


    /** @return A list of the HPO terms representing the phenotypic abnormalities in the person being evaluated.*/
    public List<TermId> getObservedAbnormalities() { return observedAbnormalities;  }
    /** @return A list of the HPO terms representing abnormalities that were excluded in the person being evaluated.*/
    public List<TermId> getExcludedAbnormalities() {  return excludedAbnormalities;  }
    /** @return the sex of the person being evaluated. */
    public Sex getSex() { return sex;  }
    /** The {@link Age} of the person being evaluated.*/
    public Age getAge() { return age; }

    public AnalysisResults results() {
        return results;
    }

    /** * @return total number of positive and negative phenotype observations for this case.*/
    public int getNumberOfObservations() {
        return observedAbnormalities.size() + excludedAbnormalities.size();
    }

    public double calculatePosttestProbability(TermId diseaseId) {
        return this.results.resultByDiseaseId(diseaseId)
                .map(TestResult::posttestProbability)
                .orElse(0.);
    }

    @Override
    public String toString() {
        String observed=this.observedAbnormalities.
                stream().
                map(TermId::getValue).
                collect(Collectors.joining("; "));
        String excluded=this.excludedAbnormalities.stream().
                map(TermId::getValue).
                collect(Collectors.joining("; "));
        int n_results=results.size();
        return "HPO Case\n" + "observed: " + observed +"\nexcluded: " + excluded +"\nTests: n="+n_results;

    }

    /** Convenience class to construct an {@link HpoCase} object. */
    public static class Builder {
        /** List of Hpo terms for our case. */
        private final List<TermId> observedAbnormalities;
        /** List of excluded Hpo terms for our case. */
        private List<TermId> excludedAbnormalities;
        /** List of results . */
        private AnalysisResults analysisResults;
        /** One of Male, Female, Unknown. See {@link Sex}. */
        private Sex sex;
        /** Age of the proband, if known. */
        private Age age;

        public Builder(List<TermId> abnormalPhenotypes) {
            this.observedAbnormalities = ImmutableList.copyOf(abnormalPhenotypes);
            excludedAbnormalities=ImmutableList.of(); // default empty list
            sex=Sex.UNKNOWN;
            age=Age.ageNotKnown();
        }

        public Builder excluded(List<TermId> excludedPhenotypes) {
            this.excludedAbnormalities = ImmutableList.copyOf(excludedPhenotypes);
            return this;
        }

        public Builder sex(Sex s) {
            this.sex=s;
            return this;
        }

        public Builder age(Age a) {
            this.age=a;
            return this;
        }

        public Builder results(AnalysisResults analysisResults) {
            this.analysisResults = analysisResults;
            return this;
        }

        public HpoCase build() {
            Objects.requireNonNull(analysisResults);
            return new HpoCase(observedAbnormalities,excludedAbnormalities, analysisResults, sex, age);
        }
    }

}
