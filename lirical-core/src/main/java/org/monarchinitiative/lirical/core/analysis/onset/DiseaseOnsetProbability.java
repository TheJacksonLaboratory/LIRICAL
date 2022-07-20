package org.monarchinitiative.lirical.core.analysis.onset;

import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * An interface for calculating disease onset likelihood ratio components.
 */
public interface DiseaseOnsetProbability {

    /**
     * Calculate probability of a disease onset at given {@code age}, and thus disease being the cause of the
     * of investigation.
     *
     * @param diseaseId {@link TermId} representing ID of the disease (e.g. OMIM:256000).
     * @param age       {@link TemporalInterval} representing the age of proband at the time of investigation.
     * @return probability of observing the disease onset latest at {@code age}.
     */
    double diseaseObservableGivenAge(TermId diseaseId, TemporalInterval age);

    /**
     * Calculate probability of a disease not being the cause at the time of investigation.
     *
     * @param diseaseId {@link TermId} representing ID of the disease (e.g. OMIM:256000).
     * @param age       {@link TemporalInterval} representing the age of proband at the time of investigation.
     * @return probability of <em>not</em> observing the disease onset latest at {@code age}.
     */
    double diseaseNotObservableGivenAge(TermId diseaseId, TemporalInterval age);

}
