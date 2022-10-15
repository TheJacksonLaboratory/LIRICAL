package org.monarchinitiative.lirical.core.output;

import java.nio.file.Path;

public record OutputOptions(LrThreshold lrThreshold,
                            MinDiagnosisCount minDiagnosisCount,
                            float pathogenicityThreshold,
                            boolean displayAllVariants,
                            Path outputDirectory,
                            String prefix) {
}
