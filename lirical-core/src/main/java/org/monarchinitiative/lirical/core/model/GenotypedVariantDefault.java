package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link GenotypedVariant} with genotypes are stored in a {@link Map}.
 */
class GenotypedVariantDefault implements GenotypedVariant {

    private final GenomeBuild genomeBuild;
    private final GenomicVariant variant;
    private final Map<String, AlleleCount> genotypes;
    private final boolean passedFilters;

    GenotypedVariantDefault(GenomeBuild genomeBuild,
                            GenomicVariant variant,
                            Map<String, AlleleCount> genotypes,
                            boolean passedFilters) {
        this.genomeBuild = Objects.requireNonNull(genomeBuild);
        this.variant = Objects.requireNonNull(variant);
        this.genotypes = Objects.requireNonNull(genotypes);
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
        return genotypes.keySet();
    }

    @Override
    public Optional<AlleleCount> alleleCount(String sample) {
        return Optional.ofNullable(genotypes.get(sample));
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
        return genomeBuild == that.genomeBuild && Objects.equals(variant, that.variant) && Objects.equals(genotypes, that.genotypes) && passedFilters == that.passedFilters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(genomeBuild, variant, genotypes, passedFilters);
    }

    @Override
    public String toString() {
        return "GenotypedVariantDefault{" +
                "genomeBuild=" + genomeBuild +
                ", variant=" + variant +
                ", genotypes=" + genotypes +
                ", passedFilters=" + passedFilters +
                '}';
    }
}
