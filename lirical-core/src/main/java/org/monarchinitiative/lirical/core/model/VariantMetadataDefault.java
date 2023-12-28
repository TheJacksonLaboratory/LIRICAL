package org.monarchinitiative.lirical.core.model;

import java.util.Objects;
import java.util.Optional;

class VariantMetadataDefault implements VariantMetadata {

    private static final VariantMetadataDefault EMPTY = new VariantMetadataDefault(Float.NaN, Float.NaN, null);

    static VariantMetadataDefault empty() {
        return EMPTY;
    }

    private final float frequency;
    private final float pathogenicity;
    private final ClinVarAlleleData clinVarAlleleData;

    VariantMetadataDefault(float frequency,
                           float pathogenicity,
                           ClinVarAlleleData clinVarAlleleData) {
        this.frequency = frequency;
        this.pathogenicity = pathogenicity;
        this.clinVarAlleleData = clinVarAlleleData; // nullable
    }

    @Override
    public Optional<Float> frequency() {
        return Float.isNaN(frequency)
                ? Optional.empty()
                : Optional.of(frequency);
    }

    @Override
    public float pathogenicity() {
        return pathogenicity;
    }

    @Override
    public Optional<ClinVarAlleleData> clinVarAlleleData() {
        return Optional.ofNullable(clinVarAlleleData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantMetadataDefault that = (VariantMetadataDefault) o;
        return Float.compare(that.frequency, frequency) == 0 && Float.compare(that.pathogenicity, pathogenicity) == 0 && Objects.equals(clinVarAlleleData, that.clinVarAlleleData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, pathogenicity);
    }

    @Override
    public String toString() {
        return "VariantMetadataDefault{" +
                "frequency=" + frequency +
                ", pathogenicity=" + pathogenicity +
                ", clinvarClnSig=" + clinVarAlleleData +
                '}';
    }
}
