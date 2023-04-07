package org.monarchinitiative.lirical.core.analysis.probability;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A collection of {@link PretestDiseaseProbability} instances.
 */
public class PretestDiseaseProbabilities {

    private PretestDiseaseProbabilities() {
    }

    /**
     * @deprecated use {@link #uniform(HpoDiseases, Collection)} instead
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    public static PretestDiseaseProbability uniform(HpoDiseases diseases) {
        Map<TermId, Double> pretestProbabilities = new HashMap<>(diseases.size());

        double prob = 1.0 / diseases.size();
        for (HpoDisease disease : diseases) {
            pretestProbabilities.put(disease.id(), prob);
        }

        return PretestDiseaseProbability.of(pretestProbabilities);
    }

    /**
     * Prepare uniform pretest disease probabilities for diseases from disease databases.
     * <p>
     * Note, we only use the diseases from the provided {@code diseaseDatabases}.
     *
     * @return uniform pretest disease probabilities.
     */
    public static PretestDiseaseProbability uniform(HpoDiseases diseases,
                                                    Collection<DiseaseDatabase> diseaseDatabases) {
        Set<String> diseasePrefixes = diseaseDatabases.stream()
                .map(DiseaseDatabase::prefix)
                .collect(Collectors.toSet());
        long diseaseCount = diseases.stream()
                .filter(d -> diseasePrefixes.contains(d.id().getPrefix()))
                .count();

        Map<TermId, Double> pretestProbabilities = new HashMap<>(Math.toIntExact(diseaseCount));
        double proba = 1.0 / diseaseCount;
        for (HpoDisease disease : diseases) {
            if (diseasePrefixes.contains(disease.id().getPrefix()))
                pretestProbabilities.put(disease.id(), proba);
        }

        return PretestDiseaseProbability.of(pretestProbabilities);
    }
}
