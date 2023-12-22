package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.lirical.core.util.BinarySearch;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link GenotypedVariant} with genotypes stored in an array.
 */
class GenotypedVariantDefault implements GenotypedVariant {

    private final GenomeBuild genomeBuild;
    private final GenomicVariant variant;
    private final SampleAlleleCount[] alleleCounts;
    private final boolean passedFilters;

    static GenotypedVariantDefault of(GenomeBuild genomeBuild,
                                      GenomicVariant variant,
                                      Collection<SampleAlleleCount> alleleCounts,
                                      boolean passedFilters) {
        // We sort the counts by sample id to take advantage of the binary search.
        SampleAlleleCount[] counts = alleleCounts.stream()
                .sorted(Comparator.comparing(SampleAlleleCount::getSampleId))
                .toArray(SampleAlleleCount[]::new);
        return new GenotypedVariantDefault(genomeBuild, variant, counts, passedFilters);
    }

    GenotypedVariantDefault(GenomeBuild genomeBuild,
                            GenomicVariant variant,
                            SampleAlleleCount[] alleleCounts,
                            boolean passedFilters) {
        this.genomeBuild = Objects.requireNonNull(genomeBuild);
        this.variant = Objects.requireNonNull(variant);
        this.alleleCounts = Objects.requireNonNull(alleleCounts);
        this.passedFilters = passedFilters;
    }


    @Override
    public GenomeBuild genomeBuild() {
        return genomeBuild;
    }

    @Override
    public GenomicVariant variant() {
        return variant;
    }

    @Override
    public Set<String> sampleNames() {
        return Arrays.stream(alleleCounts)
                .map(SampleAlleleCount::getSampleId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<AlleleCount> alleleCount(String sampleId) {
        if (sampleId == null)
            return Optional.empty();
        return BinarySearch.binarySearch(alleleCounts, SampleAlleleCount::getSampleId, sampleId)
                .map(SampleAlleleCount::getAlleleCount);
    }

    @Override
    public boolean passedFilters() {
        return passedFilters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenotypedVariantDefault that = (GenotypedVariantDefault) o;
        return genomeBuild == that.genomeBuild && Objects.equals(variant, that.variant) && Arrays.equals(alleleCounts, that.alleleCounts) && passedFilters == that.passedFilters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(genomeBuild, variant, Arrays.hashCode(alleleCounts), passedFilters);
    }

    @Override
    public String toString() {
        return "GenotypedVariantDefault{" +
                "genomeBuild=" + genomeBuild +
                ", variant=" + variant +
                ", alleleCounts=" + Arrays.toString(alleleCounts) +
                ", passedFilters=" + passedFilters +
                '}';
    }
}
