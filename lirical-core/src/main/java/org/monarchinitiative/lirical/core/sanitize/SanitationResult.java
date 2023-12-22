package org.monarchinitiative.lirical.core.sanitize;

import java.util.Collection;

/**
 * Result of the input sanitation.
 * <p>
 * The result consists of the inputs that were sanitized to the greatest extent possible
 * and of the collection of issues that were found. Note that the sanitized data may be invalid
 * even after the sanitation if further sanitation is impossible without manual intervention.
 */
public interface SanitationResult {

    /**
     * @return the inputs sanitized to the greatest extent possible.
     */
    SanitizedInputs sanitizedInputs();

    /**
     * @return a collection with sanity issues found in the input data.
     */
    Collection<SanityIssue> issues();

    /**
     * @return {@code true} if there is at least one issue in the analysis inputs.
     */
    default boolean hasErrorOrWarnings() {
        return !issues().isEmpty();
    }

    /**
     * @return {@code true} if there is at least one serious issue/error in the analysis inputs.
     */
    default boolean hasErrors() {
        return issues().stream()
                .anyMatch(i -> i.level().equals(SanityLevel.ERROR));
    }

}
