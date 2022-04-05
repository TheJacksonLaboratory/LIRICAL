package org.monarchinitiative.lirical.output;

import java.nio.file.Path;

public record OutputOptions(LrThreshold lrThreshold,
                            MinDiagnosisCount minDiagnosisCount,
                            Path outputDirectory,
                            String prefix,
                            Iterable<OutputFormat> outputFormats) {
}
