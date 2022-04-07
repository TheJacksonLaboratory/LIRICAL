package org.monarchinitiative.lirical.core.analysis.probability;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Optional;

/**
 * Implementors provide pretest probability for observing a disease.
 */
public interface PretestDiseaseProbability {

    static PretestDiseaseProbability of(Map<TermId, Double> probabilities) {
        return new PretestDiseaseProbabilityDefault(probabilities);
    }

    /**
     * @param diseaseId Disease ID, e.g. OMIM:147000
     * @return pretest probability of observing a disease or empty optional if the disease is not known
     */
    Optional<Double> pretestProbability(TermId diseaseId);
}
