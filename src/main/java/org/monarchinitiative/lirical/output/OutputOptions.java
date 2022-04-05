package org.monarchinitiative.lirical.output;

import org.monarchinitiative.lirical.configuration.LrThreshold;
import org.monarchinitiative.lirical.configuration.MinDiagnosisCount;

import java.nio.file.Path;

public record OutputOptions(LrThreshold lrThreshold,
                            MinDiagnosisCount minDiagnosisCount,
                            Path outputDirectory,
                            String prefix,
                            Iterable<OutputFormat> outputFormats) {
}
