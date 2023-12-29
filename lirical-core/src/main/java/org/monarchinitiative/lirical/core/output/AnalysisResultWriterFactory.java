package org.monarchinitiative.lirical.core.output;

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

}
