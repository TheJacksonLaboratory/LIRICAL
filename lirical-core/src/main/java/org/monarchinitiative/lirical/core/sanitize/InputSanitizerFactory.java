package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

/**
 * Get the input sanitizer with required level
 */
public class InputSanitizerFactory {

    private final MinimalOntology hpo;

    public InputSanitizerFactory(MinimalOntology hpo) {
        this.hpo = hpo;
    }

    public InputSanitizer forType(SanitizerType type) {
        return switch (type) {
            case COMPREHENSIVE -> new ComprehensiveInputSanitizer(hpo);
            case MINIMAL -> new MinimalInputSanitizer(hpo);
        };
    }
}
