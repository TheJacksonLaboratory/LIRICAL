package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;

import java.io.IOException;

/**
 * The implementors can store results of LIRICAL analysis in some {@link OutputFormat}.
 */
public interface AnalysisResultsWriter {

    /**
     * Write the provided {@link AnalysisData}, {@link AnalysisResults} and {@link AnalysisResultsMetadata} into
     * some {@link OutputFormat} as guided by {@link OutputOptions}.
     *
     * @throws IOException in case of I/O errors
     */
    void process(AnalysisData analysisData,
                 AnalysisResults analysisResults,
                 AnalysisResultsMetadata metadata,
                 OutputOptions outputOptions) throws IOException;

    /**
     * Since deprecation, the method always throws a {@link RuntimeException}.
     *
     * @deprecated use {@link #process(AnalysisData, AnalysisResults, AnalysisResultsMetadata, OutputOptions)} instead.
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    default void process(OutputOptions outputOptions) {
        throw new RuntimeException("The method has been deprecated.");
    }

}
