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
     * @param sampleId String with sample identifier.
     * @return optional with the allele count or an empty optional if data for the sample is missing.
     */
    Optional<AlleleCount> alleleCount(String sampleId);

    /**
     * @return {@code true} if the variant <em>passed</em> the filters, according to the variant source (e.g. VCF file).
     */
    boolean passedFilters();

    /**
     * @return {@code true} if the variant <em>failed</em> the filters, according to the variant source (e.g. VCF file).
     */
    default boolean failedFilters() {
        return !passedFilters();
    }
}
