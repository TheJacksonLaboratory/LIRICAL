package org.monarchinitiative.lirical.core.output;

import java.nio.file.Path;
import java.util.Objects;

public final class OutputOptions {
    private final LrThreshold lrThreshold;
    private final MinDiagnosisCount minDiagnosisCount;
    private final float pathogenicityThreshold;
    // If set to true, all variants, pathogenic, VUSs, and benign will be shown in the report.
    private final boolean displayAllVariants;
    // If set to true, all differential diagnoses, even those with no deleterious variants in the associated gene
    // will be shown in the report.
    private final boolean showDiseasesWithNoDeleteriousVariants;
    private final Path outputDirectory;
    private final String prefix;

    public OutputOptions(LrThreshold lrThreshold,
                         MinDiagnosisCount minDiagnosisCount,
                         float pathogenicityThreshold,
                         boolean displayAllVariants,
                         boolean showDiseasesWithNoDeleteriousVariants,
                         Path outputDirectory,
                         String prefix) {
        this.lrThreshold = lrThreshold;
        this.minDiagnosisCount = minDiagnosisCount;
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.displayAllVariants = displayAllVariants;
        this.showDiseasesWithNoDeleteriousVariants = showDiseasesWithNoDeleteriousVariants;
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

    public boolean showDiseasesWithNoDeleteriousVariants() {
        return showDiseasesWithNoDeleteriousVariants;
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
                this.showDiseasesWithNoDeleteriousVariants == that.showDiseasesWithNoDeleteriousVariants &&
                Objects.equals(this.outputDirectory, that.outputDirectory) &&
                Objects.equals(this.prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lrThreshold, minDiagnosisCount, pathogenicityThreshold, displayAllVariants, showDiseasesWithNoDeleteriousVariants, outputDirectory, prefix);
    }

    @Override
    public String toString() {
        return "OutputOptions[" +
                "lrThreshold=" + lrThreshold + ", " +
                "minDiagnosisCount=" + minDiagnosisCount + ", " +
                "pathogenicityThreshold=" + pathogenicityThreshold + ", " +
                "displayAllVariants=" + displayAllVariants + ", " +
                "showDiseasesWithNoDeleteriousVariants=" + showDiseasesWithNoDeleteriousVariants + ", " +
                "outputDirectory=" + outputDirectory + ", " +
                "prefix=" + prefix + ']';
    }

}
