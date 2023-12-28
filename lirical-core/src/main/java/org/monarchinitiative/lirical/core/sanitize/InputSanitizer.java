package org.monarchinitiative.lirical.core.sanitize;

/**
 * Sanitize the user input before running the analysis.
 *
 * @author Daniel Danis
 */
public interface InputSanitizer {

    SanitationResult sanitize(SanitationInputs inputs);
}
