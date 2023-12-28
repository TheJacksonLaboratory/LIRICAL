package org.monarchinitiative.lirical.core.model;


import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
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
 * @deprecated use {@link AnalysisData} and {@link AnalysisResults} instead.
 */
// REMOVE(v2.0.0)
@Deprecated
public final class HpoCase {
    private static final Logger logger = LoggerFactory.getLogger(HpoCase.class);
    private final String sampleId;
    /** List of Hpo terms for our case. */
    private final List<TermId> observedAbnormalities;
    /** List of excluded Hpo terms for our case. */
    private final List<TermId> excludedAbnormalities;
    /** One of Male, Female, Unknown. See {@link Sex}. */
    private final Sex sex;
    /** Age of the proband, if known. */
    private final Age age;

    private final AnalysisResults results;

    private HpoCase(String sampleId, List<TermId> observedTerms, List<TermId> excludedTerms, AnalysisResults results, Sex sex, Age age) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.observedAbnormalities = Objects.requireNonNull(observedTerms);
        this.excludedAbnormalities = Objects.requireNonNull(excludedTerms);
        this.results = Objects.requireNonNull(results);
        this.sex = Objects.requireNonNull(sex);
        this.age = age;
    }

    public String sampleId() {
        return sampleId;
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
        private final String sampleId;
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

        public Builder(String sampleId, List<TermId> abnormalPhenotypes) {
            this.sampleId = Objects.requireNonNull(sampleId);
            this.observedAbnormalities = List.copyOf(Objects.requireNonNull(abnormalPhenotypes));
            excludedAbnormalities=List.of(); // default empty list
            sex=Sex.UNKNOWN;
            age=null;
        }

        public Builder excluded(List<TermId> excludedPhenotypes) {
            this.excludedAbnormalities = List.copyOf(excludedPhenotypes);
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
            return new HpoCase(sampleId, observedAbnormalities,excludedAbnormalities, analysisResults, sex, age);
        }
    }

}
