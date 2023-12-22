package org.monarchinitiative.lirical.core.analysis;

import java.util.List;

public interface AnalysisInputs {
    String sampleId();

    List<String> presentHpoTerms();

    List<String> excludedHpoTerms();

    String age();

    String sex();

    String vcf();
}
