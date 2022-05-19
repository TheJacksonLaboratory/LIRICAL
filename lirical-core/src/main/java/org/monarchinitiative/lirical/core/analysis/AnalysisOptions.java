package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;

import java.util.Objects;

/**
 * A container for analysis-specific settings, i.e. settings that need to be changed for analysis of each sample.
 */
public interface AnalysisOptions {

    static AnalysisOptions of(boolean useGlobal, PretestDiseaseProbability pretestDiseaseProbability) {
        Objects.requireNonNull(pretestDiseaseProbability);
        return new AnalysisOptionsDefault(useGlobal, pretestDiseaseProbability);
    }

    /**
     * @return <code>true</code> if the <em>global</em> analysis mode should be used.
     */
    boolean useGlobal();

    /**
     * @return pretest disease probability container.
     */
    PretestDiseaseProbability pretestDiseaseProbability();

}
