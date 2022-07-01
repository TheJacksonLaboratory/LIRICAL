package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;

record AnalysisOptionsDefault(
        boolean useGlobal,
        PretestDiseaseProbability pretestDiseaseProbability,
        boolean disregardDiseaseWithNoDeleteriousVariants
) implements AnalysisOptions {

}
