package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;

class SanitationResultNotRun implements SanitationResult {

    private final SanitizedInputs sanitizedInputs;

    private static Age parseAge(String input) {
        if (input == null)
            return null;
        try {
            return Age.parse(Period.parse(input));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    SanitationResultNotRun(SanitationInputs inputs) {
        sanitizedInputs = new SanitizedInputs(inputs.sampleId(),
                inputs.presentHpoTerms().stream().map(TermId::of).toList(),
                inputs.excludedHpoTerms().stream().map(TermId::of).toList(),
                parseAge(inputs.age()),
                Sex.valueOf(inputs.sex()),
                inputs.vcf() == null ? null : Path.of(inputs.vcf())
        );
    }


    @Override
    public SanitizedInputs sanitizedInputs() {
        return sanitizedInputs;
    }

    @Override
    public Collection<SanityIssue> issues() {
        return List.of();
    }
}
