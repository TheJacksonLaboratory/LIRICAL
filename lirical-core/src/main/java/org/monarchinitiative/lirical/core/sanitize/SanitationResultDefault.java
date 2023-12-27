package org.monarchinitiative.lirical.core.sanitize;

import java.util.Collection;

/**
 * Results of the sanitation of {@link SanitationInputs} by {@link InputSanitizer}.
 * <p>
 * The {@link #sanitizedInputs()} provides data that were sanitized to the greatest extent possible.
 *
 * @param sanitizedInputs the sanitized data.
 * @param issues a collection of issues found during sanitation.
 */
record SanitationResultDefault(SanitizedInputs sanitizedInputs,
                               Collection<SanityIssue> issues) implements SanitationResult {

}
