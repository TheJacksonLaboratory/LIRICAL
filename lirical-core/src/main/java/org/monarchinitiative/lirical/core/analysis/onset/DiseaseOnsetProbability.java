package org.monarchinitiative.lirical.core.analysis.onset;

import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * An interface for calculating disease onset likelihood ratio components.
 */
public interface DiseaseOnsetProbability {

    /**
     * Calculate probability of a disease onset between {@link Age#lastMenstrualPeriod()}
     * and given {@code age}, and thus disease being observable at the time of investigation.
     *
     * @param diseaseId {@link TermId} representing ID of the disease (e.g. OMIM:256000).
     * @param age {@link Age} representing the age of proband at the time of investigation.
     * @return probability of observing the disease onset latest at {@code age}.
     */
    double diseaseObservableGivenAge(TermId diseaseId, Age age);

    /**
     * Calculate probability of a disease not being observable at the time of investigation.
     * @param diseaseId {@link TermId} representing ID of the disease (e.g. OMIM:256000).
     * @param age {@link Age} representing the age of proband at the time of investigation.
     * @return probability of <em>not</em> observing the disease onset latest at {@code age}.
     */
    // TODO - the javadocs may be actually mis-defined.
    double diseaseNotObservableGivenAge(TermId diseaseId, Age age);

}
