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
     * The analysis inputs are impeccable - not a single issue was found!
     *
     * @return {@code true} if the analysis inputs are impeccable and {@code false} otherwise.
     */
    default boolean hasErrorOrWarnings() {
        return issues().stream().anyMatch(i -> i.level().equals(SanityLevel.ERROR) || i.level().equals(SanityLevel.WARNING));
    }

    /**
     * The analysis is runnable unless we have a {@link SanityLevel#ERROR} issue.
     *
     * @return {@code true} if the analysis is runnable.
     */
    default boolean hasErrors() {
        return issues().stream()
                .anyMatch(i -> i.level().equals(SanityLevel.ERROR));
    }

    /**
     * @return {@code true} if the input has no errors and at least one warning.
     */
    default boolean hasWarningsOnly() {
        return !hasErrors() && issues().stream().anyMatch(i -> i.level().equals(SanityLevel.WARNING));
    }
}
