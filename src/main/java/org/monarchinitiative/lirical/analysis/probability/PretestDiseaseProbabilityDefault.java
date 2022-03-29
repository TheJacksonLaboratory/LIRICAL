package org.monarchinitiative.lirical.analysis.probability;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link PretestDiseaseProbability} implementation backed by a {@link Map}.
 */
class PretestDiseaseProbabilityDefault implements PretestDiseaseProbability {

    private final Map<TermId, Double> pretestProbabilities;

    PretestDiseaseProbabilityDefault(Map<TermId, Double> pretestProbabilities) {
        this.pretestProbabilities = Map.copyOf(Objects.requireNonNull(pretestProbabilities));
    }

    @Override
    public Optional<Double> pretestProbability(TermId diseaseId) {
        return Optional.ofNullable(pretestProbabilities.get(diseaseId));
    }
}
