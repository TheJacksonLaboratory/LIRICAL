package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;

import java.util.Optional;

/**
 * Factory class for getting {@link AnalysisResultsWriter}s.
 */
public interface AnalysisResultWriterFactory {

    /**
     * Get {@link AnalysisResultsWriter} or an empty {@link Optional} if the factory does not know about
     * an {@link AnalysisResultsWriter} for the given {@link OutputFormat}.
     */
    Optional<AnalysisResultsWriter> getWriter(OutputFormat outputFormat);

    /**
     * Since deprecation, the method always throws a {@link RuntimeException}.
     *
     * @deprecated use {@link #getWriter(OutputFormat)} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    default AnalysisResultsWriter getWriter(AnalysisData analysisData,
                                              AnalysisResults analysisResults,
                                              AnalysisResultsMetadata metadata) {
        throw new RuntimeException("The method has been deprecated.");
    }

}
