package org.monarchinitiative.lirical.core.sanitize;

/**
 * Sanitize the user input before running the analysis.
 */
public interface InputSanitizer {

    SanitationResult sanitize(SanitationInputs inputs);
}
