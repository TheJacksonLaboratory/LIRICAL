package org.monarchinitiative.lirical.hpo;


import com.google.common.collect.ImmutableList;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.text.DecimalFormat;
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

    private final Map<TermId,TestResult> disease2resultMap;

    private HpoCase(List<TermId> observedAbn,  List<TermId> excludedAbn, Map<TermId,TestResult> d2rmap, Sex sex, Age age) {
        this.observedAbnormalities=observedAbn;
        this.excludedAbnormalities=excludedAbn;
        this.disease2resultMap=d2rmap;
        this.sex=sex;
        this.age=age;
    }


    /** @return A list of the HPO terms representing the phenotypic abnormalities in the person being evaluated.*/
    public List<TermId> getObservedAbnormalities() { return observedAbnormalities;  }
    /** @return A list of the HPO terms representing abnormalities that were excluded in the person being evaluated.*/
    public List<TermId> getExcludedAbnormalities() {  return excludedAbnormalities;  }
    /** @return the sex of the person being evaluated. */
    public Sex getSex() { return sex;  }
    /** The {@link Age} of the person being evaluated.*/
    public Age getAge() { return age; }
    /** @return Sort List of {@link TestResult} objects for each diseases in the differential diagnosis. */
    public List<TestResult> getResults() {
        List<TestResult> trlist = new ArrayList<>(this.disease2resultMap.values());
        trlist.sort(Collections.reverseOrder());
        return trlist;
    }
    /** * @return total number of positive and negative phenotype observations for this case.*/
    public int getNumberOfObservations() {
        return observedAbnormalities.size() + excludedAbnormalities.size();
    }

    public TestResult getResult(TermId diseaseId) {
        return this.disease2resultMap.get(diseaseId);
    }

    /**
     * The argument to the function is the TermId (e.g., OMIM:600100) of a disease. This function checks to
     * see what rank the disease was assigned by LIRICAL; the ranks are stored in {@link #disease2resultMap}.
     * Note that in some cases, the correct disease may be completely removed from the list of results. This can
     * be the case, fo instance, if we are using the {@code strict} option and only return diseases for which
     * LIRICAL finds a predicted pathogenic variant in a gene associated with the disease.
     * Therefore, we return an Optional. If it is not-present, then the disease was not found.
     * @param diseaseId CURIE (e.g., OMIM:600100) of the disease whose rank we want to know
     * @return the rank of the disease within all of the test results.
     *
     */
    public Optional<Integer> getRank(TermId diseaseId){
        TestResult result = this.disease2resultMap.get(diseaseId);
        if (result==null) {
            return Optional.empty();
        }
        return Optional.of(result.getRank());
    }

    /**
     * If a disease is unranked, then it is tied for the rank after the last rank of the ranked diseases
     * @return (tied) rank of an unranked disease
     */
    public int getRankOfUnrankedDisease() {
        return 1 + this.disease2resultMap.size();
    }


//    private String niceFormat(double d) {
//        DecimalFormat df = new DecimalFormat("0.000E0");
//        if (d > 1.0) {
//            return String.format("%.2f", d);
//        } else if (d > 0.005) {
//            return String.format("%.4f", d);
//        } else {
//            return df.format(d);
//        }
//    }

    @Override
    public String toString() {
        String observed=this.observedAbnormalities.
                stream().
                map(TermId::getValue).
                collect(Collectors.joining("; "));
        String excluded=this.excludedAbnormalities.stream().
                map(TermId::getValue).
                collect(Collectors.joining("; "));
        int n_results=this.getResults().size();
        return "HPO Case\n" + "observed: " + observed +"\nexcluded: " + excluded +"\nTests: n="+n_results;

    }


    public double getBestPosteriorProbability() {
        return disease2resultMap.values().stream().
                max(Comparator.comparing(TestResult::getPosttestProbability)).
                get().getPosttestProbability();
    }


    /** Convenience class to construct an {@link HpoCase} object. */
    public static class Builder {
        /** List of Hpo terms for our case. */
        private final List<TermId> observedAbnormalities;
        /** List of excluded Hpo terms for our case. */
        private List<TermId> excludedAbnormalities;
        /** List of results . */
        private Map<TermId,TestResult> testResultMap;
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

        public Builder results(Map<TermId,TestResult> trlist) {
            this.testResultMap =trlist;
            return this;
        }

        public HpoCase build() {
            Objects.requireNonNull(testResultMap);
            return new HpoCase(observedAbnormalities,excludedAbnormalities, testResultMap,sex,age);
        }
    }

}
