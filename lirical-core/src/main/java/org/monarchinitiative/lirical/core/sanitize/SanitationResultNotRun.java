package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.time.Period;
import java.util.Collection;
import java.util.List;

class SanitationResultNotRun implements SanitationResult {

    private final SanitizedInputs sanitizedInputs;

    SanitationResultNotRun(AnalysisInputs inputs) {
        List<TermId> present = inputs.presentHpoTerms().stream().map(TermId::of).toList();
        List<TermId> excluded = inputs.excludedHpoTerms().stream().map(TermId::of).toList();
        sanitizedInputs = new SanitizedInputs(inputs.sampleId(),
                present,
                excluded,
                Age.parse(Period.parse(inputs.age())),
                Sex.valueOf(inputs.sex()),
                Path.of(inputs.vcf())
        );
    }


    @Override
    public SanitizedInputs sanitized() {
        return sanitizedInputs;
    }

    @Override
    public Collection<SanityIssue> issues() {
        return List.of();
    }
}
