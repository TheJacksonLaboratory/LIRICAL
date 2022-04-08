package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AnalysisResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResultsWriter.class);

    private final Ontology hpo;
    private final HpoDiseases diseases;
    private final AnalysisData analysisData;
    private final AnalysisResults analysisResults;
    private final Map<String, String> metadata;


    AnalysisResultsWriter(Ontology hpo,
                          HpoDiseases diseases,
                          AnalysisData analysisData,
                          AnalysisResults analysisResults,
                          Map<String, String> metadata) {
        this.hpo = hpo;
        this.diseases = diseases;
        this.analysisData = analysisData;
        this.analysisResults = analysisResults;
        this.metadata = metadata;

    }

    public void process(OutputOptions options) {
        for (OutputFormat format : options.outputFormats()) {
            templateForFormat(format, options)
                    .ifPresent(LiricalTemplate::outputFile);
        }
    }

    private Optional<LiricalTemplate> templateForFormat(OutputFormat format, OutputOptions options) {
        // TODO - finalize template creation
        return switch (format) {
            case TSV -> Optional.of(new TsvTemplate(hpo, diseases, analysisData, analysisResults, metadata, options));
            case HTML -> Optional.of(new HtmlTemplate(hpo, diseases, analysisData, analysisResults, metadata, options, List.of(), Set.of()));
        };
    }

}
