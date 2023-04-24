package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.*;

/**
 * A description of variant coordinates, sample genotypes, and filtering status for LIRICAL analysis.
 * <p>
 * The variant has a {@link #genomeBuild()} to describe the reference system.
 * The {@link #variant()} provides variant coordinates using Svart's {@link GenomicVariant} data structure.
 * The variant genotypes for a set of samples can be accessed via {@link #alleleCount(String)}.
 * Last, LIRICAL uses the variants that passed all filters in the analysis ({@link #passedFilters()}).
 * However, we need to retain the failed variants too to report the passed/failed variants in the report.
 */
public interface GenotypedVariant {

    /**
     * @deprecated deprecated in {@code v2.0.0} and subject to removal in {@code v3.0.0}.
     * Use {@link #of(GenomeBuild, GenomicVariant, Collection, boolean)} instead.
     */
    // REMOVE(v3.0.0)
    @Deprecated(forRemoval = true, since = "2.0.0-RC3")
    static GenotypedVariant of(GenomeBuild genomeBuild,
                               GenomicVariant variant,
                               Map<String, AlleleCount> genotypes,
                               boolean passedFilters) {
        List<SampleAlleleCount> alleleCounts = genotypes.entrySet().stream()
                .map(e -> SampleAlleleCount.of(e.getKey(), e.getValue()))
                .toList();
        return of(genomeBuild, variant, alleleCounts, passedFilters);
    }

    static GenotypedVariant of(GenomeBuild genomeBuild,
                               GenomicVariant variant,
                               Collection<SampleAlleleCount> alleleCounts,
                               boolean passedFilters) {
        return GenotypedVariantDefault.of(genomeBuild, variant, alleleCounts, passedFilters);
    }

    /**
     * @return the genome build of the variant.
     */
    GenomeBuild genomeBuild();

    /**
     * @return the variant coordinates in Svart's {@linkplain GenomicVariant}.
     */
    GenomicVariant variant();

    /**
     * @return a set of sample identifiers where we have genotype data for this variant.
     */
    Set<String> sampleNames();

    /**
     * Get allele count for given sample.
     *
     * @param sample String with sample identifier.
     * @return optional with the allele count or an empty optional if data for the sample is missing.
     */
    Optional<AlleleCount> alleleCount(String sample);

    /**
     * @return true if the variant passed the filters in the variant source
     */
    boolean passedFilters();
}
