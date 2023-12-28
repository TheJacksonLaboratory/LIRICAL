package org.monarchinitiative.lirical.core.analysis.probability;

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
