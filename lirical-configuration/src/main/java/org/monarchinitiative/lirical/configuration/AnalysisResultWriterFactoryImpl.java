package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.TemplateBasedAnalysisResultsWriter;
import org.monarchinitiative.lirical.io.output.JsonAnalysisResultWriter;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Objects;
import java.util.Optional;

class AnalysisResultWriterFactoryImpl implements AnalysisResultWriterFactory {

    private final Ontology hpo;
    private final HpoDiseases diseases;

    private AnalysisResultsWriter html, tsv, json;

    AnalysisResultWriterFactoryImpl(Ontology hpo, HpoDiseases diseases) {
        this.hpo = Objects.requireNonNull(hpo);
        this.diseases = Objects.requireNonNull(diseases);
    }

    @Override
    public Optional<AnalysisResultsWriter> getWriter(OutputFormat outputFormat) {
        return Optional.of(getLazilyLoadedAnalysisResultsWriter(outputFormat));
    }

    private synchronized AnalysisResultsWriter getLazilyLoadedAnalysisResultsWriter(OutputFormat format) {
        return switch (format) {
            case HTML -> {
                if (html == null)
                    html = new TemplateBasedAnalysisResultsWriter(format, hpo, diseases);
                yield html;
            }
            case TSV -> {
                if (tsv == null)
                    tsv = new TemplateBasedAnalysisResultsWriter(format, hpo, diseases);
                yield tsv;
            }
            case JSON -> {
                if (json == null)
                    json = JsonAnalysisResultWriter.of();
                yield json;
            }
        };
    }

}
