package org.monarchinitiative.lirical.cli.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.cli.configuration.LiricalProperties;
import org.monarchinitiative.lirical.core.service.PhenotypeService;

import java.util.Map;

public class AnalysisResultWriterFactory {

    private final PhenotypeService phenotypeService;
    private final LiricalProperties liricalProperties;

    public AnalysisResultWriterFactory(PhenotypeService phenotypeService,
                                       LiricalProperties liricalProperties) {
        this.phenotypeService = phenotypeService;
        this.liricalProperties = liricalProperties;
    }


    public AnalysisResultsWriter getWriter(AnalysisData analysisData,
                                           AnalysisResults analysisResults,
                                           Map<String, String> metadata) {
        return new AnalysisResultsWriter(phenotypeService, liricalProperties, analysisData, analysisResults, metadata);
    }

}
