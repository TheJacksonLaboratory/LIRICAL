package org.monarchinitiative.lirical.core.output;

import java.nio.file.Path;
import java.util.Objects;

public final class OutputOptions {
    private final LrThreshold lrThreshold;
    private final MinDiagnosisCount minDiagnosisCount;
    private final float pathogenicityThreshold;
    private final boolean displayAllVariants;
    private final Path outputDirectory;
    private final String prefix;

    public OutputOptions(LrThreshold lrThreshold,
                         MinDiagnosisCount minDiagnosisCount,
                         float pathogenicityThreshold,
                         boolean displayAllVariants,
                         Path outputDirectory,
                         String prefix) {
        this.lrThreshold = lrThreshold;
        this.minDiagnosisCount = minDiagnosisCount;
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.displayAllVariants = displayAllVariants;
        this.outputDirectory = outputDirectory;
        this.prefix = prefix;
    }

    public LrThreshold lrThreshold() {
        return lrThreshold;
    }

    public MinDiagnosisCount minDiagnosisCount() {
        return minDiagnosisCount;
    }

    public float pathogenicityThreshold() {
        return pathogenicityThreshold;
    }

    public boolean displayAllVariants() {
        return displayAllVariants;
    }

    public Path outputDirectory() {
        return outputDirectory;
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (OutputOptions) obj;
        return Objects.equals(this.lrThreshold, that.lrThreshold) &&
                Objects.equals(this.minDiagnosisCount, that.minDiagnosisCount) &&
                Float.floatToIntBits(this.pathogenicityThreshold) == Float.floatToIntBits(that.pathogenicityThreshold) &&
                this.displayAllVariants == that.displayAllVariants &&
                Objects.equals(this.outputDirectory, that.outputDirectory) &&
                Objects.equals(this.prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lrThreshold, minDiagnosisCount, pathogenicityThreshold, displayAllVariants, outputDirectory, prefix);
    }

    @Override
    public String toString() {
        return "OutputOptions[" +
                "lrThreshold=" + lrThreshold + ", " +
                "minDiagnosisCount=" + minDiagnosisCount + ", " +
                "pathogenicityThreshold=" + pathogenicityThreshold + ", " +
                "displayAllVariants=" + displayAllVariants + ", " +
                "outputDirectory=" + outputDirectory + ", " +
                "prefix=" + prefix + ']';
    }

}
