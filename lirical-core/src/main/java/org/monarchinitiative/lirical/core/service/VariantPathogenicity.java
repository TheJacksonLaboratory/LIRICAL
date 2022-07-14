package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.ClinvarClnSig;

import java.util.Objects;
import java.util.Optional;

/**
 * Class to represent pathogenicity data available for a genomic variant.
 * The pathogenicity data usually represents the highest value of algorithms (i.e. PolyPhen2, Mutation Taster).
 * <p>
 * The {@link #empty()} variant pathogenicity should be used when the pathogenicity data is not available
 * (i.e. the database is not available during runtime). This is in contrast with case when database is available and
 * pathogenicity score for a variant is missing. The score is set to zero in this case.
 */
public class VariantPathogenicity {

    private static final VariantPathogenicity EMPTY = new VariantPathogenicity(Float.NaN, null);

    private final float pathogenicity;
    private final ClinvarClnSig clinvarClnSig;

    public static VariantPathogenicity empty() {
        return EMPTY;
    }

    public static VariantPathogenicity of(float pathogenicity, ClinvarClnSig clinvarClnSig) {
        if (Float.isNaN(pathogenicity))
            throw new IllegalArgumentException("Pathogenicity must not be NaN");
        if (pathogenicity < 0. || pathogenicity > 1.)
            throw new IllegalArgumentException("Pathogenicity score must be in range of [0, 1]");

        return new VariantPathogenicity(pathogenicity, clinvarClnSig);
    }

    private VariantPathogenicity(float pathogenicity, ClinvarClnSig clinvarClnSig) {
        this.pathogenicity = pathogenicity;
        this.clinvarClnSig = clinvarClnSig; // nullable
    }

    /**
     * @return true if the pathogenicity data was not available, and we cannot say if the variant <em>is</em>
     * or is <em>not</em> pathogenic.
     */
    public boolean isEmpty() {
        return Float.isNaN(pathogenicity);
    }

    public Optional<ClinvarClnSig> clinvarClnSig() {
        return Optional.ofNullable(clinvarClnSig);
    }

    public float pathogenicity() {
        return pathogenicity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (VariantPathogenicity) obj;
        return Float.floatToIntBits(this.pathogenicity) == Float.floatToIntBits(that.pathogenicity) &&
                Objects.equals(this.clinvarClnSig, that.clinvarClnSig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathogenicity, clinvarClnSig);
    }

    @Override
    public String toString() {
        return "VariantPathogenicity[" +
                "pathogenicity=" + pathogenicity + ", " +
                "clinvarClnSig=" + clinvarClnSig + ']';
    }

}
