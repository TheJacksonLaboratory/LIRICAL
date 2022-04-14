package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.*;

class LiricalVariantDefault implements LiricalVariant {

    private final GenotypedVariant genotypedVariant;
    private final VariantMetadata variantMetadata;

    LiricalVariantDefault(GenotypedVariant genotypedVariant, VariantMetadata variantMetadata) {
        this.genotypedVariant = Objects.requireNonNull(genotypedVariant);
        this.variantMetadata = Objects.requireNonNull(variantMetadata);
    }

    @Override
    public GenomeBuild genomeBuild() {
        return genotypedVariant.genomeBuild();
    }

    @Override
    public GenomicVariant variant() {
        return genotypedVariant.variant();
    }

    @Override
    public Set<String> sampleNames() {
        return genotypedVariant.sampleNames();
    }

    @Override
    public Optional<AlleleCount> alleleCount(String sample) {
        return genotypedVariant.alleleCount(sample);
    }

    @Override
    public boolean passedFilters() {
        return genotypedVariant.passedFilters();
    }

    @Override
    public Optional<Float> frequency() {
        return variantMetadata.frequency();
    }

    @Override
    public float pathogenicity() {
        return variantMetadata.pathogenicity();
    }

    @Override
    public ClinvarClnSig clinvarClnSig() {
        return variantMetadata.clinvarClnSig();
    }

    @Override
    public List<TranscriptAnnotation> annotations() {
        return variantMetadata.annotations();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiricalVariantDefault that = (LiricalVariantDefault) o;
        return Objects.equals(genotypedVariant, that.genotypedVariant) && Objects.equals(variantMetadata, that.variantMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genotypedVariant, variantMetadata);
    }

    @Override
    public String toString() {
        return "LiricalVariantDefault{" +
                "genotypedVariant=" + genotypedVariant +
                ", variantMetadata=" + variantMetadata +
                '}';
    }
}
