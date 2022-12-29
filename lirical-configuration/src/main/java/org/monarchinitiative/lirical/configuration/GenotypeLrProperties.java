package org.monarchinitiative.lirical.configuration;

import java.util.Objects;

public final class GenotypeLrProperties {
    private final float pathogenicityThreshold;
    private final double defaultVariantBackgroundFrequency;
    private final boolean strict;

    public GenotypeLrProperties(float pathogenicityThreshold, double defaultVariantBackgroundFrequency, boolean strict) {
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
        this.strict = strict;
    }

    public float pathogenicityThreshold() {
        return pathogenicityThreshold;
    }

    /**
     * @deprecated use {@link #defaultVariantBackgroundFrequency()} instead.
     */
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    public double defaultVariantFrequency() {
        return defaultVariantBackgroundFrequency;
    }

    public double defaultVariantBackgroundFrequency() {
        return defaultVariantBackgroundFrequency;
    }

    public boolean strict() {
        return strict;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GenotypeLrProperties) obj;
        return Float.floatToIntBits(this.pathogenicityThreshold) == Float.floatToIntBits(that.pathogenicityThreshold) &&
                Double.doubleToLongBits(this.defaultVariantBackgroundFrequency) == Double.doubleToLongBits(that.defaultVariantBackgroundFrequency) &&
                this.strict == that.strict;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathogenicityThreshold, defaultVariantBackgroundFrequency, strict);
    }

    @Override
    public String toString() {
        return "GenotypeLrProperties[" +
                "pathogenicityThreshold=" + pathogenicityThreshold + ", " +
                "defaultVariantBackgroundFrequency=" + defaultVariantBackgroundFrequency + ", " +
                "strict=" + strict + ']';
    }

}
