package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2018-04-04)
 */
public final class HpoCase {
    private static final Logger logger = LogManager.getLogger();
    /** List of Hpo terms for our case. */
    private final List<TermId> observedAbnormalities;
    /** List of excluded Hpo terms for our case. */
    private final List<TermId> excludedAbnormalities;
    /** One of Male, Female, Unknown. See {@link Sex}. */
    private final Sex sex;
    /** Age of the proband, if known. */
    private final Age age;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private final List<TestResult> results;

    private HpoCase(List<TermId> observedAbn,  List<TermId> excludedAbn, Sex sex, Age age) {
        this.observedAbnormalities=observedAbn;
        this.excludedAbnormalities=excludedAbn;
        this.sex=sex;
        this.age=age;
        this.results=new ArrayList<>();
    }


    /** @return A list of the HPO terms representing the phenotypic abnormalities in the person being evaluated.*/
    public List<TermId> getObservedAbnormalities() { return observedAbnormalities;  }
    /** @return A list of the HPO terms representing abnormalities that were excluded in the person being evaluated.*/
    public List<TermId> getExcludedAbnormalities() {  return excludedAbnormalities;  }
    /** @return the sex of the person being evaluated. */
    public Sex getSex() { return sex;  }
    /** The {@link Age} of the person being evaluated.*/
    public Age getAge() { return age; }
    /** @return List of {@link TestResult} objects for each diseases in the differential diagnosis. */
    public List<TestResult> getResults() { return results; }
    /** * @return total number of positive and negative phenotype observations for this case.*/
    public int getNumberOfObservations() {
        return observedAbnormalities.size() + excludedAbnormalities.size();
    }






    /** Convenience class to construct an {@link HpoCase} object. */
    public static class Builder {
        /** List of Hpo terms for our case. */
        private final List<TermId> observedAbnormalities;
        /** List of excluded Hpo terms for our case. */
        private List<TermId> excludedAbnormalities;
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

        public HpoCase build() {
            return new HpoCase(observedAbnormalities,excludedAbnormalities,sex,age);
        }
    }

}
