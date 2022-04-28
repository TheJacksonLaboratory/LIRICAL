package org.monarchinitiative.lirical.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VariantMetadataDefault implements VariantMetadata {

    private static final VariantMetadataDefault EMPTY = new VariantMetadataDefault(Float.NaN, Float.NaN, ClinvarClnSig.NOT_PROVIDED, List.of());

    static VariantMetadataDefault empty() {
        return EMPTY;
    }

    private final float frequency;
    private final float pathogenicity;
    private final ClinvarClnSig clinvarClnSig;
    private final List<TranscriptAnnotation> annotations;

    VariantMetadataDefault(float frequency,
                           float pathogenicity,
                           ClinvarClnSig clinvarClnSig,
                           List<TranscriptAnnotation> annotations) {
        this.frequency = frequency;
        this.pathogenicity = pathogenicity;
        this.clinvarClnSig = Objects.requireNonNull(clinvarClnSig);
        this.annotations = Objects.requireNonNull(annotations);
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
    public ClinvarClnSig clinvarClnSig() {
        return clinvarClnSig;
    }

    @Override
    public List<TranscriptAnnotation> annotations() {
        return annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantMetadataDefault that = (VariantMetadataDefault) o;
        return Float.compare(that.frequency, frequency) == 0 && Float.compare(that.pathogenicity, pathogenicity) == 0 && Objects.equals(clinvarClnSig, that.clinvarClnSig)  && Objects.equals(annotations, that.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(frequency, pathogenicity, annotations);
    }

    @Override
    public String toString() {
        return "VariantMetadataDefault{" +
                "frequency=" + frequency +
                ", pathogenicity=" + pathogenicity +
                ", clinvarClnSig=" + clinvarClnSig +
                ", annotations=" + annotations +
                '}';
    }
}
