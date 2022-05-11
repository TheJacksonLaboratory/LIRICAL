package org.monarchinitiative.lirical.core.output;

import java.nio.file.Path;

public record OutputOptions(LrThreshold lrThreshold,
                            MinDiagnosisCount minDiagnosisCount,
                            float pathogenicityThreshold,
                            float frequencyThreshold,
                            Path outputDirectory,
                            String prefix,
                            Iterable<OutputFormat> outputFormats) {
}
