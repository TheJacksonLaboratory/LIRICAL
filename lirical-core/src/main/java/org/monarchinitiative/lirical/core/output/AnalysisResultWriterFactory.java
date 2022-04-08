package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;
import java.util.Objects;

public class AnalysisResultWriterFactory {

    private final Ontology hpo;
    private final HpoDiseases diseases;

    public AnalysisResultWriterFactory(Ontology hpo, HpoDiseases diseases) {
        this.hpo = Objects.requireNonNull(hpo);
        this.diseases = Objects.requireNonNull(diseases);
    }


    public AnalysisResultsWriter getWriter(AnalysisData analysisData,
                                           AnalysisResults analysisResults,
                                           Map<String, String> metadata) {
        return new AnalysisResultsWriter(hpo, diseases, analysisData, analysisResults, metadata);
    }

}
