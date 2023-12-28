package org.monarchinitiative.lirical.io.output;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TemplateBasedAnalysisResultsWriter implements AnalysisResultsWriter {

    private final OutputFormat format;
    private final MinimalOntology hpo;
    private final HpoDiseases diseases;

    public TemplateBasedAnalysisResultsWriter(OutputFormat format, MinimalOntology hpo, HpoDiseases diseases) {
        this.format = Objects.requireNonNull(format);
        this.hpo = Objects.requireNonNull(hpo);
        this.diseases = Objects.requireNonNull(diseases);
    }

    @Override
    public void process(AnalysisData analysisData,
                        AnalysisResults analysisResults,
                        AnalysisResultsMetadata metadata,
                        OutputOptions options) {
        Optional<LiricalTemplate> template = switch (format) {
            case TSV -> Optional.of(new TsvTemplate(hpo, diseases, analysisData, analysisResults, metadata, options));
            case HTML -> Optional.of(new HtmlTemplate(hpo, diseases, analysisData, analysisResults, metadata, options, List.of(), Set.of()));
            default -> Optional.empty();
        };
        template.ifPresent(LiricalTemplate::outputFile);
    }

}
