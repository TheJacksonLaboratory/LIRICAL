package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.svg.Lr2Svg;
import org.monarchinitiative.lirical.svg.Posttest2Svg;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates getting the data from the analysis into the FreeMark org.monarchinitiative.lirical.output templates.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HtmlTemplate extends LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(HtmlTemplate.class);
    /**
     * Threshold posterior probability to show a differential diagnosis in detail.
     */
    private final Double THRESHOLD;
    /**
     * Have the HTML output show at least this many differntials (default: 5).
     */
    private final Integer MIN_DIAGNOSES_TO_SHOW;


    /**
     * Constructor to initialize the data that will be needed to output an HTML page.
     *
     * @param hcase       The individual (case) represented in the VCF file
     * @param ontology    The HPO ontology
     * @param genotypeMap A map of genotypes for all genes with variants in the VCF file
     * @param geneid2sym  A map from the Entrez Gene id to the gene symbol
     * @param metadat     Metadata about the analysis.
     * @param thres       threshold posterior probability to show differential in detail
     */
    public HtmlTemplate(HpoCase hcase, Ontology ontology, Map<TermId, Gene2Genotype> genotypeMap, Map<TermId, String> geneid2sym, Map<String, String> metadat, Double thres, Integer minDifferentials, String prefix, String outdir, List<String> errs) {
        super(hcase, ontology, genotypeMap, geneid2sym, metadat);
        initpath(prefix, outdir);
        this.THRESHOLD = thres;
        this.MIN_DIAGNOSES_TO_SHOW = minDifferentials;
        this.templateData.put("errorlist", errs);
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap = new HashMap<>();
        this.topDiagnosisAnchors = new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader, "");
        templateData.put("postprobthreshold", String.format("%.1f%%", 100 * THRESHOLD));
        int N = totalDetailedDiagnosesToShow(hcase.getResults());
        List<SparklinePacket> sparklinePackets = SparklinePacket.sparklineFactory(hcase, N, geneid2sym, ontology);
        this.templateData.put("sparkline", sparklinePackets);
        this.templateData.put("hasGenotypes", "true");
        int counter = 0;
        for (TestResult result : hcase.getResults()) {
            String symbol = EMPTY_STRING;
            if (passesThreshold(counter, result.getPosttestProbability())) {
                DifferentialDiagnosis ddx = new DifferentialDiagnosis(result);
                logger.trace("Diff diag for " + result.getDiseaseName());
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    Gene2Genotype g2g = genotypeMap.get(geneId);
                    if (g2g != null) {
                        symbol = g2g.getSymbol();
                        ddx.addG2G(g2g);
                    } else {
                        ddx.setGenotypeExplanation("no variants found in " + this.geneId2symbol.get(geneId));
                        symbol = "no variants found in " + this.geneId2symbol.get(geneId);// will be used by SVG
                    }
                    String expl = result.getGenotypeExplanation();
                    ddx.setGenotypeExplanation(expl);
                } else {
                    ddx.setGenotypeExplanation("No known disease gene");
                }
                //ddx.setPhenotypeExplanation(result.getPhenotypeExplanation());
                // now get SVG
                Lr2Svg lr2svg = new Lr2Svg(hcase, result.getDiseaseCurie(), result.getDiseaseName(), ontology, symbol);
                String svg = lr2svg.getSvgString();
                ddx.setSvg(svg);
                diff.add(ddx);
                counter++;
                String counterString = String.format("diagnosis%d", counter);
                this.topDiagnosisAnchors.add(counterString);
                ddx.setAnchor(counterString);
                this.topDiagnosisMap.put(counterString, ddx.getDiseaseName());
            } else {
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    if (genotypeMap.containsKey(geneId)) {
                        symbol = genotypeMap.get(geneId).getSymbol();
                        int c = genotypeMap.get(geneId).getVarList().size();
                        String name = shortName(result.getDiseaseName());
                        String id = result.getDiseaseCurie().getId();// This is intended to work with OMIM
                        if (name == null) {
                            logger.error("Got null string for disease name from result=" + result.toString());
                            name = EMPTY_STRING;// avoid errors
                        }
                        ImprobableDifferential ipd = new ImprobableDifferential(name, id, symbol, result.getPosttestProbability(), c);
                        improbdiff.add(ipd);
                    }
                }
            }
        }
        this.templateData.put("improbdiff", improbdiff);
        this.templateData.put("diff", diff);
    }

    private void initpath(String prefix, String outdir) {
        this.outpath = String.format("%s.html", prefix);
        if (outdir != null) {
            File dir = mkdirIfNotExist(outdir);
            this.outpath = Paths.get(dir.getAbsolutePath(), this.outpath).toString();
        }
    }


    /**
     * Constructor to initialize the data that will be needed to output an HTML page.
     * Used for when we have no genetic data
     *
     * @param hcase    The individual (case) represented in the VCF file
     * @param ontology The HPO ontology
     * @param metadat  Metadata about the analysis.
     * @param thres    threshold posterior probability to show differential in detail
     */
    public HtmlTemplate(HpoCase hcase, Ontology ontology, Map<String, String> metadat, double thres, int minDifferentials, String prefix, String outdir, List<String> errs) {
        super(hcase, ontology, metadat);
        initpath(prefix, outdir);
        this.THRESHOLD = thres;
        this.MIN_DIAGNOSES_TO_SHOW = minDifferentials;
        this.templateData.put("errorlist", errs);
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap = new HashMap<>();
        this.topDiagnosisAnchors = new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader, "");
        templateData.put("postprobthreshold", String.format("%.1f%%", 100 * THRESHOLD));
        // Get SVG for post-test probability list
        int N = totalDetailedDiagnosesToShow(hcase.getResults());
        Posttest2Svg pt2svg = new Posttest2Svg(hcase.getResults(), THRESHOLD, N);
        String posttestSVG = pt2svg.getSvgString();
        this.templateData.put("posttestSVG", posttestSVG);
        List<SparklinePacket> sparklinePackets = SparklinePacket.sparklineFactory(hcase, N, ontology);
        this.templateData.put("sparkline", sparklinePackets);


        int counter = 0;
        for (TestResult result : hcase.getResults()) {
            String symbol = EMPTY_STRING;
            if (passesThreshold(counter, result.getPosttestProbability())) {
                DifferentialDiagnosis ddx = new DifferentialDiagnosis(result);
                logger.trace("Diff diag for " + result.getDiseaseName());
                ddx.setGenotypeExplanation("Genetic data not available");
                // now get SVG
                Lr2Svg lr2svg = new Lr2Svg(hcase, result.getDiseaseCurie(), result.getDiseaseName(), ontology, symbol);
                String svg = lr2svg.getSvgString();
                ddx.setSvg(svg);
                diff.add(ddx);
                counter++;
                String counterString = String.format("diagnosis%d", counter);
                this.topDiagnosisAnchors.add(counterString);
                ddx.setAnchor(counterString);
                this.topDiagnosisMap.put(counterString, ddx.getDiseaseName());
            } else {
                TermId geneId = result.getEntrezGeneId();
                String name = shortName(result.getDiseaseName());
                String id = result.getDiseaseCurie().getId();// This is intended to work with OMIM
                if (name == null) {
                    logger.error("Got null string for disease name from result=" + result.toString());
                    name = EMPTY_STRING;// avoid errors
                }
                int c = 0;
                ImprobableDifferential ipd = new ImprobableDifferential(name, id, symbol, result.getPosttestProbability(), c);
                improbdiff.add(ipd);
            }
        }
        this.templateData.put("improbdiff", improbdiff);
        this.templateData.put("diff", diff);

    }


    @Override
    public void outputFile() {
        logger.info("Writing HTML file to {}", this.outpath);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(this.outpath))) {
            Template template = cfg.getTemplate("liricalHTML.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }

    @Override
    public void outputFile(String fname) {
        logger.info("Writing HTML file to {}", fname);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(fname))) {
            Template template = cfg.getTemplate("liricalTSV.html");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }

    /**
     * Return the total number of diseases that we will show in detail in the HTML output.
     *
     * @param results a sorted list of results
     * @return number of diseases to show
     */
    private int totalDetailedDiagnosesToShow(List<TestResult> results) {
        int n = 0;
        for (TestResult res : results) {
            if (res.getPosttestProbability() > THRESHOLD) {
                n++;
            } else {
                break;
            }
        }
        initializeTopDifferentialCount(n);
        return Math.max(n, MIN_DIAGNOSES_TO_SHOW);
    }

    /**
     * Add a message to the template for display in the HTML output
     *
     * @param N Total count of above-threshold diseases
     */
    private void initializeTopDifferentialCount(int N) {
        if (N == 0) {
            String message = String.format("No diseases had an above threshold (&gt; %.2f) post-test probability.", THRESHOLD);
            this.templateData.put("topdifferentialcount", message);
        } else if (N == 1) {
            String message = String.format("One disease had an above threshold (&gt; %.2f) post-test probability.", THRESHOLD);
            this.templateData.put("topdifferentialcount", message);
        } else {
            String message = String.format("%d diseases had an above threshold (&gt; %.2f) post-test probability.", N, THRESHOLD);
            this.templateData.put("topdifferentialcount", message);
        }
    }

    /**
     * Does the current entry pass the threshold for being shown in detail?
     * Note that EITHER we are looking to show a minimum number of differentials and we ask if k<=m
     * OR we show candidates with a likelihood ratio that is >= THRESHOLD.
     * @param k Current count of this diagnosis
     * @param lr likelihood ratio of this diagnosis
     * @return True if we pass the appropriate threshold
     */
    private boolean passesThreshold(int k, double lr) {
        if (this.MIN_DIAGNOSES_TO_SHOW != null) {
            return k <= MIN_DIAGNOSES_TO_SHOW;
        }
        if (this.THRESHOLD != null) {
            return lr <= THRESHOLD;
        }
        // we should never get here
        logger.error("Both MIN_DIAGNOSES_TO_SHOW and THRESHOLD were null");
        return false;
    }



}
