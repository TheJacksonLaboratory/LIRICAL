package org.monarchinitiative.lirical.core.analysis.onset.proba;

import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalOverlapType;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public class SimpleDiseaseOnsetProbability extends BaseDiseaseOnsetProbability {

    private final double epsilon;

    public SimpleDiseaseOnsetProbability(HpoDiseases diseases, boolean strict) {
        super(diseases, strict);
        this.epsilon = 1e-8;
    }

    @Override
    public double diseaseObservableGivenAge(TermId diseaseId, Age age) {
        Optional<TemporalInterval> onsetOptional = diseases.diseaseById(diseaseId)
                .flatMap(HpoDisease::diseaseOnset);

        if (onsetOptional.isEmpty())
            // An unknown disease or a disease with an unknown onset time.
            return 1 - epsilon;

        TemporalInterval diseaseOnset = onsetOptional.get();
        // We use TemporalInterval to compare onset and `age` to account for possible
        TemporalOverlapType overlapType = diseaseOnset.temporalOverlapType(age);
        return switch (overlapType) {
            // The disease onsets before the age.
            case BEFORE -> 1 - epsilon;
            // The disease onsets after the age.
            case AFTER -> epsilon;
            // Borderline cases where the outcome depends on the `strict` parameter.
            // TODO - evaluate how effective the `strict` option is.
            case BEFORE_AND_DURING, CONTAINS, CONTAINED_IN, DURING_AND_AFTER -> strict ? epsilon : 1 - epsilon;
        };
    }

}
