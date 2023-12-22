package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;

import java.util.Collection;

public interface SanitationResult {

    static SanitationResult notRun(AnalysisInputs inputs) {
        return new SanitationResultNotRun(inputs);
    }

    /**
     * @return the inputs sanitized to the greatest extent possible.
     */
    SanitizedInputs sanitized();

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

    default boolean hasWarningsOnly() {
        return !hasErrors() && issues().stream().anyMatch(i -> i.level().equals(SanityLevel.WARNING));
    }
}
