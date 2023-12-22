package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

/**
 * Sanitize the user input before running the analysis.
 */
public interface InputSanitizer {

    static InputSanitizer defaultSanitizer(MinimalOntology hpo) {
        return new DefaultInputSanitizer(hpo);
    }

    SanitationResult sanitize(AnalysisInputs inputs);
}
