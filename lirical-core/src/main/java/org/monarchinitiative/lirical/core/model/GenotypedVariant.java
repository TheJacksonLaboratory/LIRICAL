package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GenotypedVariant {

    static GenotypedVariant of(GenomeBuild genomeBuild,
                               GenomicVariant variant,
                               Map<String, AlleleCount> genotypes,
                               boolean passedFilters) {
        return new GenotypedVariantDefault(genomeBuild, variant, genotypes, passedFilters);
    }

    GenomeBuild genomeBuild();

    GenomicVariant variant();

    Set<String> sampleNames();

    Optional<AlleleCount> alleleCount(String sample);

    /**
     * @return true if the variant passed the filters in the variant source
     */
    boolean passedFilters();
}
