package org.monarchinitiative.lirical.core.sanitize;

public record SanityIssue(SanityLevel level, String message, String solution) {
    public static SanityIssue error(String message, String solution) {
        return new SanityIssue(SanityLevel.ERROR, message, solution);
    }

    public static SanityIssue warning(String message, String solution) {
        return new SanityIssue(SanityLevel.WARNING, message, solution);
    }
}
