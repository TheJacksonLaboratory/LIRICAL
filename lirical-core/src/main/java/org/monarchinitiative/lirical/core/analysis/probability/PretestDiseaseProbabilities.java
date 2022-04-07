package org.monarchinitiative.lirical.core.analysis.probability;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.Map;

/**
 * A collection of {@link PretestDiseaseProbability} instances.
 */
public class PretestDiseaseProbabilities {

    private PretestDiseaseProbabilities() {
    }

    public static PretestDiseaseProbability uniform(HpoDiseases diseases) {
        Map<TermId, Double> pretestProbabilities = new HashMap<>(diseases.size());

        double prob = 1.0 / diseases.size();
        for (HpoDisease disease : diseases) {
            pretestProbabilities.put(disease.id(), prob);
        }

        return PretestDiseaseProbability.of(pretestProbabilities);
    }
}
