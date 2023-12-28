package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;

import java.util.Set;

record AnalysisOptionsDefault(
        GenomeBuild genomeBuild,
        TranscriptDatabase transcriptDatabase,
        Set<DiseaseDatabase> diseaseDatabases,
        float variantDeleteriousnessThreshold,
        double defaultVariantBackgroundFrequency,
        boolean useStrictPenalties,
        boolean useGlobal,
        PretestDiseaseProbability pretestDiseaseProbability,
        boolean includeDiseasesWithNoDeleteriousVariants
) implements AnalysisOptions {
}
