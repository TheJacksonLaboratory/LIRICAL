package org.monarchinitiative.lirical.model;

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

    GenotypedVariantDefault(GenomeBuild genomeBuild,
                            GenomicVariant variant,
                            Map<String, AlleleCount> genotypes) {
        this.genomeBuild = genomeBuild;
        this.variant = variant;
        this.genotypes = genotypes;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenotypedVariantDefault that = (GenotypedVariantDefault) o;
        return Objects.equals(genomeBuild, that.genomeBuild) && Objects.equals(variant, that.variant) && Objects.equals(genotypes, that.genotypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genomeBuild, variant, genotypes);
    }

    @Override
    public String toString() {
        return "GenotypedVariantDefault{" +
                "genomeBuild=" + genomeBuild +
                ", variant=" + variant +
                ", genotypes=" + genotypes +
                '}';
    }
}
