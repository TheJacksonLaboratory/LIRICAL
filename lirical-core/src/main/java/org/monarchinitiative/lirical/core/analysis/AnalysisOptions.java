package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;

import java.util.Objects;

/**
 * A container for analysis-specific settings, i.e. settings that need to be changed for analysis of each sample.
 */
public interface AnalysisOptions {

    /**
     * @deprecated to be removed in <code>2.0.0</code>, use {@link #of(boolean, PretestDiseaseProbability, boolean, float)} instead.
     */
    @Deprecated(forRemoval = true)
    static AnalysisOptions of(boolean useGlobal, PretestDiseaseProbability pretestDiseaseProbability) {
        return of(useGlobal, pretestDiseaseProbability, false);
    }

    /**
     * @deprecated to be removed in <code>2.0.0</code>, use {@link #of(boolean, PretestDiseaseProbability, boolean, float)} instead.
     */
    @Deprecated(forRemoval = true)
    static AnalysisOptions of(boolean useGlobal,
                              PretestDiseaseProbability pretestDiseaseProbability,
                              boolean disregardDiseaseWithNoDeleteriousVariants) {
        Objects.requireNonNull(pretestDiseaseProbability);
        return new AnalysisOptionsDefault(useGlobal, pretestDiseaseProbability, disregardDiseaseWithNoDeleteriousVariants, .8f);
    }

    static AnalysisOptions of(boolean useGlobal,
                              PretestDiseaseProbability pretestDiseaseProbability,
                              boolean disregardDiseaseWithNoDeleteriousVariants,
                              float pathogenicityThreshold) {
        Objects.requireNonNull(pretestDiseaseProbability);
        return new AnalysisOptionsDefault(useGlobal, pretestDiseaseProbability, disregardDiseaseWithNoDeleteriousVariants, pathogenicityThreshold);
    }

    /**
     * @return <code>true</code> if the <em>global</em> analysis mode should be used.
     */
    boolean useGlobal();

    /**
     * @return pretest disease probability container.
     */
    PretestDiseaseProbability pretestDiseaseProbability();

    /**
     * Disregard a disease if no known or predicted deleterious variants are found in the gene associated
     * with the disease. The option is used only if the variants are available for the investigated individual.
     *
     * @return <code>true</code> if the candidate disease should be disregarded.
     */
    boolean disregardDiseaseWithNoDeleteriousVariants();

    /**
     * Variant with pathogenicity value greater or equal to this threshold is considered deleterious.
     *
     * @return variant pathogenicity threshold value.
     */
    float pathogenicityThreshold();

}
