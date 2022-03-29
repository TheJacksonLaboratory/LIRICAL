package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
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
     * @param ontology Reference toHPO Ontology object
     * @param metadata Reference to a map of "metadata"-information we will use for the output file
     */
    public TsvTemplate(HpoCase hcase,
                       Ontology ontology,
                       Map<String, String> metadata,
                       String prefix,
                       Path outdir) {
        this(hcase, ontology, Map.of(), Map.of(), metadata, prefix, outdir);
//        super(hcase,ontology,metadata);
//        this.outpath = createOutputFile(outdir, prefix, "%s.tsv");
//        ClassLoader classLoader = TsvTemplate.class.getClassLoader();
//        cfg.setClassLoaderForTemplateLoading(classLoader,"");
//        List<TsvDifferential> diff = new ArrayList<>();
//        String header= String.join("\t",tsvHeader);
//        templateData.put("header",header);
//        int counter=0;
//        // Note the following results are already sorted
//        for (TestResult result : hcase.getResults()) {
//            TsvDifferential tsvdiff = new TsvDifferential(result);
//            diff.add(tsvdiff);
//            counter++;
//        }
//        this.templateData.put("diff",diff);
    }

    public TsvTemplate(HpoCase hcase,
                       Ontology ontology,
                       Map<TermId, Gene2Genotype> genotypeMap,
                       Map<TermId, String> geneid2sym,
                       Map<String, String> metadata,
                       String prefix,
                       Path outdir) {
        super(hcase, ontology, geneid2sym, metadata);
        this.outpath = createOutputFile(outdir, prefix, "%s.tsv");
        ClassLoader classLoader = TsvTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader,"");
        List<TsvDifferential> diff = new ArrayList<>();
        String header= String.join("\t",tsvHeader);
        templateData.put("header",header);
        // Note the following results are already sorted
        for (TestResult result : hcase.getResults()) {
            TsvDifferential tsvdiff = new TsvDifferential(result);
            Optional<GenotypeLrWithExplanation> genotypeLr = result.genotypeLr();
            if (genotypeLr.isPresent()) {
                TermId geneId = genotypeLr.get().geneId();
                Gene2Genotype g2g = genotypeMap.get(geneId);
                if (g2g != null) {
                    tsvdiff.addG2G(g2g);
                }
            }
            diff.add(tsvdiff);
        }
        this.templateData.put("diff",diff);
    }


    @Override
    public void outputFile() {
        logger.info("Writing TSV file to {}", outpath.toAbsolutePath());
        try (BufferedWriter out = Files.newBufferedWriter(outpath)) {
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
}
