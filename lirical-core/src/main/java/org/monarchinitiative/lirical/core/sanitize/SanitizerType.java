package org.monarchinitiative.lirical.core.sanitize;

/**
 * Enum to represent the existing sanitizer types.
 *
 * @author Daniel Danis
 */
public enum SanitizerType {
    /**
     * Comprehensive sanitizer performs the broadest array of checks to point out all errors and warnings.
     */
    COMPREHENSIVE,

    /**
     * Minimal sanitizer performs the minimal checks required for the analysis to runnable.
     */
    MINIMAL
}
