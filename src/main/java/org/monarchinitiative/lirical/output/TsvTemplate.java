package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.configuration.LiricalProperties;
import org.monarchinitiative.lirical.model.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * This class coordinates the output of a TSV file that contains a suymmary of the analysis results.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class TsvTemplate extends LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(TsvTemplate.class);

    private static final String[] tsvHeader={"rank","diseaseName","diseaseCurie","pretestprob","posttestprob",
            "compositeLR","entrezGeneId","variants"};

    public TsvTemplate(LiricalProperties liricalProperties,
                       HpoCase hpoCase,
                       PhenotypeService phenotypeService,
                       Map<TermId, Gene2Genotype> geneById,
                       Map<String, String> metadata,
                       Path outdir,
                       String prefix) {
        super(liricalProperties, hpoCase, phenotypeService, geneById, metadata, outdir, prefix);
        cfg.setClassLoaderForTemplateLoading(TsvTemplate.class.getClassLoader(),"");
        templateData.put("header", String.join("\t",tsvHeader));
        AtomicInteger rank = new AtomicInteger();
        Map<TermId, HpoDisease> diseaseById = phenotypeService.diseases().diseaseById();
        List<TsvDifferential> diff = new ArrayList<>();
        hpoCase.results().resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    int current = rank.incrementAndGet();
                    List<VisualizableVariant> variants = result.genotypeLr()
                            .map(GenotypeLrWithExplanation::geneId)
                            .map(geneId -> geneById.get(geneId.id()).variants())
                            .orElse(Stream.empty())
                            .map(toVisualizableVariant())
                            .toList();
                    HpoDisease disease = diseaseById.get(result.diseaseId());
                    TsvDifferential tsvdiff = new TsvDifferential(hpoCase.sampleId(), disease.id(), disease.getDiseaseName(), result, current, variants);
                    diff.add(tsvdiff);
                });
        this.templateData.put("diff",diff);
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
    public void outputFile(String fname) {
        logger.info("Writing TSV file to {}",fname);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(fname))) {
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
