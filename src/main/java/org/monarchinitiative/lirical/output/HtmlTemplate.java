package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LrThreshold;
import org.monarchinitiative.lirical.configuration.MinDiagnosisCount;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.svg.Lr2Svg;
import org.monarchinitiative.lirical.svg.Posttest2Svg;
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

/**
 * This class coordinates getting the data from the analysis into the FreeMark org.monarchinitiative.lirical.output templates.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HtmlTemplate extends LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(HtmlTemplate.class);
    /** Threshold posterior probability to show a differential diagnosis in detail. */
    private final LrThreshold lrThreshold;
    /** Have the HTML output show at least this many differntials (default: 5). */
    private final MinDiagnosisCount minDiagnosisToShowInDetail;


    private final int DEFAULT_MIN_DIAGNOSES_TO_SHOW = 10;

    /**
     * There are gene symbols returned by Jannovar for which we cannot find a geneId. This issues seems to be related
     * to the input files used by Jannovar from UCSC ( knownToLocusLink.txt.gz has links between ucsc ids, e.g.,
     * uc003fts.3, and NCBIGene ids (earlier known as locus link), e.g., 1370).
     */
    protected Set<String> symbolsWithoutGeneIds;

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
    public HtmlTemplate(HpoCase hcase,
                        Ontology ontology,
                        Map<TermId, Gene2Genotype> genotypeMap,
                        Map<TermId, String> geneid2sym,
                        Map<String, String> metadat,
                        LrThreshold thres,
                        MinDiagnosisCount minDifferentials,
                        String prefix,
                        Path outdir,
                        List<String> errs,
                        Set<String> symbolsWithoutGeneIds) {
        super(hcase, ontology, geneid2sym, metadat);
        this.outpath = createOutputFile(outdir, prefix, "%s.html");
        this.lrThreshold = thres;
        this.minDiagnosisToShowInDetail = minDifferentials;
        this.templateData.put("errorlist", errs);
        this.symbolsWithoutGeneIds = symbolsWithoutGeneIds;
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap = new HashMap<>();
        this.topDiagnosisAnchors = new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader, "");
        templateData.put("postprobthreshold", String.format("%.1f%%", 100 * lrThreshold.getThreshold()));
        AnalysisResults results = hcase.results();
        int N = totalDetailedDiagnosesToShow(results);
        List<SparklinePacket> sparklinePackets = SparklinePacket.sparklineFactory(hcase, N, geneid2sym, ontology);
        this.templateData.put("sparkline", sparklinePackets);
        this.templateData.put("hasGenotypes", "true");
        if (symbolsWithoutGeneIds==null || symbolsWithoutGeneIds.isEmpty()){
            this.templateData.put("hasGeneSymbolsWithoutIds", "false");
        } else {
            this.templateData.put("hasGeneSymbolsWithoutIds", "true");
            this.templateData.put("geneSymbolsWithoutIds", symbolsWithoutGeneIds);
        }

        AtomicInteger rank = new AtomicInteger();
        results.resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    int current = rank.incrementAndGet();
                    String symbol = EMPTY_STRING;
                    Optional<GenotypeLrWithExplanation> genotypeLr = result.genotypeLr();
                    if (current <= N) {
                        DifferentialDiagnosis ddx = new DifferentialDiagnosis(result, current);
                        if (genotypeLr.isPresent()) {
                            GenotypeLrWithExplanation genotypeLrWithExplanation = genotypeLr.get();
                            TermId geneId = genotypeLrWithExplanation.geneId();
                            Gene2Genotype g2g = genotypeMap.get(geneId);
                            if (g2g != null) {
                                symbol = g2g.getSymbol();
                                ddx.addG2G(g2g);
                            } else {
                                ddx.setGenotypeExplanation("no variants found in " + this.geneId2symbol.get(geneId));
                                symbol = "no variants found in " + this.geneId2symbol.get(geneId);// will be used by SVG
                            }
                            ddx.setGenotypeExplanation(genotypeLrWithExplanation.explanation());
                        } else {
                            ddx.setGenotypeExplanation("No known disease gene");
                        }
                        //ddx.setPhenotypeExplanation(result.getPhenotypeExplanation());
                        // now get SVG

                        Lr2Svg lr2svg = new Lr2Svg(result, current, result.diseaseId(), result.getDiseaseName(), ontology, symbol);
                        ddx.setSvg(lr2svg.getSvgString());
                        diff.add(ddx);

                        String counterString = String.format("diagnosis%d", current);
                        this.topDiagnosisAnchors.add(counterString);
                        ddx.setAnchor(counterString);
                        this.topDiagnosisMap.put(counterString, ddx.getDiseaseName());
                    } else {
                        if (genotypeLr.isPresent()) {
                            TermId geneId = genotypeLr.get().geneId();
                            if (genotypeMap.containsKey(geneId)) {
                                symbol = genotypeMap.get(geneId).getSymbol();
                                int c = genotypeMap.get(geneId).getVarList().size();
                                String name = shortName(result.getDiseaseName());
                                String id = result.diseaseId().getId();// This is intended to work with OMIM
                                if (name == null) {
                                    logger.error("Got null string for disease name from result={}", result);
                                    name = EMPTY_STRING;// avoid errors
                                }
                                ImprobableDifferential ipd = new ImprobableDifferential(name, id, symbol, result.posttestProbability(), c);
                                improbdiff.add(ipd);
                            }
                        }
                    }
                });
        this.templateData.put("improbdiff", improbdiff);
        this.templateData.put("diff", diff);
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
    public HtmlTemplate(HpoCase hcase, Ontology ontology, Map<String, String> metadat, LrThreshold thres, MinDiagnosisCount minDifferentials, String prefix, Path outdir, List<String> errs) {
        super(hcase, ontology, metadat);
        this.outpath = createOutputFile(outdir, prefix, "%s.html");
        this.lrThreshold = thres;
        this.minDiagnosisToShowInDetail = minDifferentials;
        this.templateData.put("errorlist", errs);
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap = new HashMap<>();
        this.topDiagnosisAnchors = new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader, "");
        templateData.put("postprobthreshold", String.format("%.1f%%", 100 * lrThreshold.getThreshold()));
        // Get SVG for post-test probability list
        int N = totalDetailedDiagnosesToShow(hcase.results());
        Posttest2Svg pt2svg = new Posttest2Svg(hcase.results(), lrThreshold.getThreshold(), N);
        String posttestSVG = pt2svg.getSvgString();
        this.templateData.put("posttestSVG", posttestSVG);
        List<SparklinePacket> sparklinePackets = SparklinePacket.sparklineFactory(hcase.results(), N, ontology);
        this.templateData.put("sparkline", sparklinePackets);

        AtomicInteger rank = new AtomicInteger();
        hcase.results().resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    String symbol = EMPTY_STRING;
                    int current = rank.incrementAndGet();
                    if (current <= N) {
                        DifferentialDiagnosis ddx = new DifferentialDiagnosis(result, current);
                        logger.trace("Diff diag for " + result.getDiseaseName());
                        ddx.setGenotypeExplanation("Genetic data not available");

                        // now get SVG
                        Lr2Svg lr2svg = new Lr2Svg(result, current, result.diseaseId(), result.getDiseaseName(), ontology, symbol);
                        ddx.setSvg(lr2svg.getSvgString());
                        diff.add(ddx);

                        String counterString = String.format("diagnosis%d", current);
                        this.topDiagnosisAnchors.add(counterString);
                        ddx.setAnchor(counterString);
                        this.topDiagnosisMap.put(counterString, ddx.getDiseaseName());
                    } else {
                        String name = shortName(result.getDiseaseName());
                        String id = result.diseaseId().getId();// This is intended to work with OMIM
                        if (name == null) {
                            logger.error("Got null string for disease name from result={}", result);
                            name = EMPTY_STRING;// avoid errors
                        }
                        int c = 0;
                        ImprobableDifferential ipd = new ImprobableDifferential(name, id, symbol, result.posttestProbability(), c);
                        improbdiff.add(ipd);
                    }
                });
        this.templateData.put("improbdiff", improbdiff);
        this.templateData.put("diff", diff);

    }


    @Override
    public void outputFile() {
        logger.info("Writing HTML file to {}", outpath.toAbsolutePath());
        try (BufferedWriter out = Files.newBufferedWriter(outpath)) {
            Template template = cfg.getTemplate("liricalHTML.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            logger.warn("Error writing out results {}", te.getMessage(), te);
        }
    }

    @Override
    public void outputFile(String fname) {
        logger.info("Writing HTML file to {}", fname);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(fname))) {
            Template template = cfg.getTemplate("liricalTSV.html");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            logger.warn("Error writing out results {}", te.getMessage(), te);
        }
    }

    /**
     * Return the total number of diseases that we will show in detail in the HTML output.
     * If the user does not passs -m (min diagnoses to show) or -t (threshold), then
     * we choose the default value of {@link #DEFAULT_MIN_DIAGNOSES_TO_SHOW}. If the
     * user chooses -t, we choose the number of diagnoses that have a higher post-test
     * probability, but at least 3. If the user chooses -m, we show at least m items.
     *
     * @param results a sorted list of results
     * @return number of diseases to show
     */
    private int totalDetailedDiagnosesToShow(AnalysisResults results) {
        double t = lrThreshold.getThreshold();
        int aboveThreshold = (int) results.results().filter(r -> r.posttestProbability() >= t).count();
        initializeTopDifferentialCount(aboveThreshold);
        if (!minDiagnosisToShowInDetail.isSetByUser() && !lrThreshold.isSetByUser()) {
            return DEFAULT_MIN_DIAGNOSES_TO_SHOW;
        } else if (minDiagnosisToShowInDetail.isSetByUser()) {
            return Math.max(aboveThreshold, minDiagnosisToShowInDetail.getMinToShow());
        } else {
            // if we get here, then THRESHOLD was set by the user.
            return Math.max(aboveThreshold, lrThreshold.getMinimumToShowInThresholdMode());
        }
    }

    /**
     * Add a message to the template for display in the HTML output
     *
     * @param N Total count of above-threshold diseases
     */
    private void initializeTopDifferentialCount(int N) {
        if (N == 0) {
            String message = String.format("No diseases had an above threshold (&gt; %.2f) post-test probability.", lrThreshold.getThreshold());
            this.templateData.put("topdifferentialcount", message);
        } else if (N == 1) {
            String message = String.format("One disease had an above threshold (&gt; %.2f) post-test probability.", lrThreshold.getThreshold());
            this.templateData.put("topdifferentialcount", message);
        } else {
            String message = String.format("%d diseases had an above threshold (&gt; %.2f) post-test probability.", N, lrThreshold.getThreshold());
            this.templateData.put("topdifferentialcount", message);
        }
    }

}
