package org.monarchinitiative.lirical.io.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class coordinates the output of a TSV file that contains a suymmary of the analysis results.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class TsvTemplate extends LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(TsvTemplate.class);

    private static final String[] tsvHeader = {"rank", "diseaseName", "diseaseCurie", "pretestprob", "posttestprob",
            "compositeLR", "entrezGeneId", "variants"};

    TsvTemplate(Ontology hpo,
                HpoDiseases diseases,
                AnalysisData analysisData,
                AnalysisResults analysisResults,
                AnalysisResultsMetadata resultsMetadata,
                OutputOptions outputOptions) {
        super(hpo, analysisData, resultsMetadata, outputOptions);
        cfg.setClassForTemplateLoading(TsvTemplate.class, "");
        templateData.put("header", String.join("\t", tsvHeader));
        AtomicInteger rank = new AtomicInteger();
        Map<TermId, HpoDisease> diseaseById = diseases.diseaseById();
        List<TsvDifferential> diff = new ArrayList<>();
        analysisResults.resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    int current = rank.incrementAndGet();
                    List<VisualizableVariant> variants = result.genotypeLr()
                            .map(GenotypeLrWithExplanation::geneId)
                            .map(geneId -> geneById.get(geneId.id()).variants())
                            .orElse(Stream.empty())
                            .map(toVisualizableVariant())
                            .toList();
                    HpoDisease disease = diseaseById.get(result.diseaseId());
                    TsvDifferential tsvdiff = new TsvDifferential(analysisData.sampleId(), disease.id(), disease.diseaseName(), result, current, variants);
                    diff.add(tsvdiff);
                });
        this.templateData.put("diff", diff);
    }


    @Override
    public void outputFile() {
        logger.info("Writing TSV file to {}", outputPath.toAbsolutePath());
        try (BufferedWriter out = Files.newBufferedWriter(outputPath)) {
            Template template = cfg.getTemplate("liricalTSV.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }

    @Override
    protected String outputFormatString() {
        return "%s.tsv";
    }
}
