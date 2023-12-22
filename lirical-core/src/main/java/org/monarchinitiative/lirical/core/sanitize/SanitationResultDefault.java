package org.monarchinitiative.lirical.core.sanitize;

import java.util.Collection;

/**
 * Results of the sanitation of {@link org.monarchinitiative.lirical.core.analysis.AnalysisInputs} by {@link InputSanitizer}.
 * <p>
 * The {@link #sanitized()} provides data that were sanitized to the greatest extent possible. The data may be invalid,
 * however, further sanitation is impossible without manual intervention.
 *
 * @param sanitized the sanitized data.
 * @param issues a collection of issues found during sanitation.
 */
record SanitationResultDefault(SanitizedInputs sanitized,
                               Collection<SanityIssue> issues) implements SanitationResult {

}
