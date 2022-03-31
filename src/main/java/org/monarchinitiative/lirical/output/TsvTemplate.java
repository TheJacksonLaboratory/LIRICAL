package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.configuration.LiricalProperties;
import org.monarchinitiative.lirical.model.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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

    /**
     * Constructor for when we do the analysis without genetic data
     * @param hcase The current HPO case whose results we are about to output
     * @param ontology Reference to HPO Ontology object
     * @param geneById
     * @param metadata Reference to a map of "metadata"-information we will use for the output file
     */
    public TsvTemplate(LiricalProperties liricalProperties,
                       HpoCase hcase,
                       Ontology ontology,
                       Map<TermId, Gene2Genotype> geneById,
                       Map<String, String> metadata,
                       String prefix,
                       Path outdir) {
        this(liricalProperties, hcase, ontology, geneById, metadata, outdir, prefix);
    }

    public TsvTemplate(LiricalProperties liricalProperties,
                       HpoCase hcase,
                       Ontology ontology,
                       Map<TermId, Gene2Genotype> geneById,
                       Map<String, String> metadata,
                       Path outdir, String prefix) {
        super(liricalProperties, hcase, ontology, geneById, metadata, outdir, prefix);
        cfg.setClassLoaderForTemplateLoading(TsvTemplate.class.getClassLoader(),"");
        templateData.put("header", String.join("\t",tsvHeader));
        AtomicInteger rank = new AtomicInteger();
        List<TsvDifferential> diff = new ArrayList<>();
        hcase.results().resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    int current = rank.incrementAndGet();
                    List<VisualizableVariant> variants = result.genotypeLr()
                            .map(GenotypeLrWithExplanation::geneId)
                            .map(geneId -> geneById.get(geneId.id()).variants())
                            .orElse(Stream.empty())
                            .map(toVisualizableVariant())
                            .toList();
                    TsvDifferential tsvdiff = new TsvDifferential(hcase.sampleId(), result, current, variants);
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
