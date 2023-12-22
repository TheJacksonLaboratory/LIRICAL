package org.monarchinitiative.lirical.core.sanitize;

import java.util.List;

/**
 * The sanitation requirements.
 */
public interface SanitationInputs {
    /**
     * @return a string with the sample ID or {@code null} if not available.
     */
    String sampleId();

    /**
     * @return a list with CURIEs of HPO terms that represent the phenotypic features observed in the index patient.
     */
    List<String> presentHpoTerms();

    /**
     * @return a list with CURIEs of HPO terms that represent the phenotypic features that were investigated
     * and excluded in the index patient.
     */
    List<String> excludedHpoTerms();

    /**
     * @return a string with the age or {@code null} if not available.
     */
    String age();

    /**
     * @return a string with the sex or {@code null} if not available.
     */
    String sex();

    /**
     * @return a string with the path of the VCF file with variants or {@code null} if not available.
     */
    String vcf();
}
