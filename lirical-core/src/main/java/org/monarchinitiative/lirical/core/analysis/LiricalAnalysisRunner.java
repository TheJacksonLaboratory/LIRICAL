package org.monarchinitiative.lirical.core.analysis;

import java.io.Closeable;

/**
 * The analysis runner runs LIRICAL analysis on provided analysis subject ({@link AnalysisData}). The analysis
 * is parametrized by {@link AnalysisOptions}. The runner throws {@link LiricalAnalysisException} if the analysis
 * cannot be run as dictated by the options.
 */
public interface LiricalAnalysisRunner extends Closeable {

    /**
     * Run analysis parametrized by {@code analysisOptions} on {@code analysisData}.
     *
     * @param analysisData data representing the analysis subject.
     * @param analysisOptions analysis parameters.
     * @return a container with results for each evaluated disease
     * @throws LiricalAnalysisException if the analysis cannot be run, e.g. due to missing resource,
     * such as {@linkplain org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator} for a combination
     * of {@linkplain org.monarchinitiative.lirical.core.model.GenomeBuild}
     * and {@linkplain org.monarchinitiative.lirical.core.model.TranscriptDatabase}
     */
    AnalysisResults run(AnalysisData analysisData, AnalysisOptions analysisOptions) throws LiricalAnalysisException;

}
