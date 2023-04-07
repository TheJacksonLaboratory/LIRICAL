package org.monarchinitiative.lirical.core.analysis.probability;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of {@link PretestDiseaseProbability} instances.
 */
public class PretestDiseaseProbabilities {

    private PretestDiseaseProbabilities() {
    }

    /**
     * @deprecated use {@link #uniform(Collection)} instead
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
     * Prepare uniform pretest disease probabilities for given disease identifiers.
     *
     * @return uniform pretest disease probabilities.
     */
    public static PretestDiseaseProbability uniform(Collection<TermId> diseaseIds) {
        Map<TermId, Double> pretestProbabilities = new HashMap<>(diseaseIds.size());
        double proba = 1.0 / diseaseIds.size();
        for (TermId diseaseId : diseaseIds) {
            pretestProbabilities.put(diseaseId, proba);
        }

        return PretestDiseaseProbability.of(pretestProbabilities);
    }
}
