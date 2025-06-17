package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.sanitize.SanitationInputs;

import java.nio.file.Path;
import java.util.List;

record SanitationInputsDefault(
        String sampleId,
        List<String> presentHpoTerms,
        List<String> excludedHpoTerms,
        String age,
        String sex,
        Path vcf
) implements SanitationInputs {
}
