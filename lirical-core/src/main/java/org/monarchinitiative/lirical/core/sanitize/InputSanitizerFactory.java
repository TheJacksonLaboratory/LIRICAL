package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

public class InputSanitizerFactory {

    private final MinimalOntology hpo;

    public InputSanitizerFactory(MinimalOntology hpo) {
        this.hpo = hpo;
    }

    public InputSanitizer forType(SanitizerType type) {
        return switch (type) {
            case DEFAULT -> new DefaultInputSanitizer(hpo);
            case NOOP -> NoOpInputSanitizer.getInstance();
        };
    }
}
