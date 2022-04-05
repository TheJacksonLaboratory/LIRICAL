package org.monarchinitiative.lirical.output;

import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.configuration.LiricalProperties;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AnalysisResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResultsWriter.class);

    private final PhenotypeService phenotypeService;
    private final LiricalProperties properties;
    private final AnalysisData analysisData;
    private final AnalysisResults analysisResults;
    private final Map<String, String> metadata;

    public AnalysisResultsWriter(PhenotypeService phenotypeService,
                                 LiricalProperties properties,
                                 AnalysisData analysisData,
                                 AnalysisResults analysisResults,
                                 Map<String, String> metadata) {
        this.phenotypeService = phenotypeService;
        this.analysisData = analysisData;
        this.analysisResults = analysisResults;
        this.metadata = metadata;
        this.properties = properties;

    }

    public void process(OutputOptions options) {
        for (OutputFormat format : options.outputFormats()) {
            templateForFormat(format, options)
                    .ifPresent(LiricalTemplate::outputFile);
        }
    }

    private Optional<LiricalTemplate> templateForFormat(OutputFormat format, OutputOptions options) {
         // TODO - finalize
        return switch (format) {
            case TSV -> Optional.of(new TsvTemplate(properties, phenotypeService, analysisData, analysisResults, metadata, options));
            case HTML -> Optional.of(new HtmlTemplate(properties, phenotypeService, analysisData, analysisResults, metadata, options, List.of(), Set.of()));
        };
    }

}
