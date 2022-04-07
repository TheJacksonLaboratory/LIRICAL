package org.monarchinitiative.lirical.cli.output;

import java.nio.file.Path;

public record OutputOptions(LrThreshold lrThreshold,
                            MinDiagnosisCount minDiagnosisCount,
                            Path outputDirectory,
                            String prefix,
                            Iterable<OutputFormat> outputFormats) {
}
