package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;

import java.util.List;

record AnalysisInputsDefault(
        String sampleId,
        List<String> presentHpoTerms,
        List<String> excludedHpoTerms,
        String age,
        String sex,
        String vcf
) implements AnalysisInputs {
}
