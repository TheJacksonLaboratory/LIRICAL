package org.monarchinitiative.lirical.core.sanitize;

/**
 * An issue that was found in the analysis input.
 *
 * @param level    severity of the issue.
 * @param message  description of the issue for humans.
 * @param solution the proposed solution or {@code null} if N/A.
 *
 * @author Daniel Danis
 */
public record SanityIssue(SanityLevel level, String message, String solution) {
    public static SanityIssue error(String message, String solution) {
        return new SanityIssue(SanityLevel.ERROR, message, solution);
    }

    public static SanityIssue warning(String message, String solution) {
        return new SanityIssue(SanityLevel.WARNING, message, solution);
    }
}
