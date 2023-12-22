package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.*;

class LiricalVariantDefault implements LiricalVariant {

    private final GenotypedVariant genotypedVariant;
    private final List<TranscriptAnnotation> annotations;
    private final VariantMetadata variantMetadata;

    LiricalVariantDefault(GenotypedVariant genotypedVariant,
                          List<TranscriptAnnotation> annotations,
                          VariantMetadata variantMetadata) {
        this.genotypedVariant = Objects.requireNonNull(genotypedVariant);
        this.annotations = Objects.requireNonNull(annotations);
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
    public Optional<AlleleCount> alleleCount(String sampleId) {
        return genotypedVariant.alleleCount(sampleId);
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
    public Optional<ClinVarAlleleData> clinVarAlleleData() {
        return variantMetadata.clinVarAlleleData();
    }

    @Override
    public List<TranscriptAnnotation> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiricalVariantDefault that = (LiricalVariantDefault) o;
        return Objects.equals(genotypedVariant, that.genotypedVariant) && Objects.equals(annotations, that.annotations) && Objects.equals(variantMetadata, that.variantMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genotypedVariant, annotations, variantMetadata);
    }

    @Override
    public String toString() {
        return "LiricalVariantDefault{" +
                "genotypedVariant=" + genotypedVariant +
                ", annotations=" + annotations +
                ", variantMetadata=" + variantMetadata +
                '}';
    }
}
