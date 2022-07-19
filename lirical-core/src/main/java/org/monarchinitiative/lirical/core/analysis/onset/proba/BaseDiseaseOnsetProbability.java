package org.monarchinitiative.lirical.core.analysis.onset.proba;

import org.monarchinitiative.lirical.core.analysis.onset.DiseaseOnsetProbability;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

/**
 * Base {@link DiseaseOnsetProbability} that provides implementation of {@link #diseaseNotObservableGivenAge(TermId, Age)}.
 */
abstract class BaseDiseaseOnsetProbability implements DiseaseOnsetProbability {

    protected final HpoDiseases diseases;
    protected final boolean strict;

    protected BaseDiseaseOnsetProbability(HpoDiseases diseases, boolean strict) {
        this.diseases = Objects.requireNonNull(diseases);
        this.strict = strict;
    }

    @Override
    public double diseaseNotObservableGivenAge(TermId diseaseId, Age age) {
        double sum = diseases.hpoDiseases()
                .mapToDouble(d -> diseaseObservableGivenAge(d.id(), age))
                .sum();
        return sum / diseases.size();
    }

}
