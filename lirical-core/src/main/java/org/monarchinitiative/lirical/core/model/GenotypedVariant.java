package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface GenotypedVariant {

    static GenotypedVariant of(GenomeBuild genomeBuild, GenomicVariant variant, Map<String, AlleleCount> genotypes) {
        return new GenotypedVariantDefault(genomeBuild, variant, genotypes);
    }

    GenomeBuild genomeBuild();

    GenomicVariant variant();

    Set<String> sampleNames();

    Optional<AlleleCount> alleleCount(String sample);

}
