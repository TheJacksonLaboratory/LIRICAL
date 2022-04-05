package org.monarchinitiative.lirical.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.configuration.LiricalProperties;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.output.svg.Lr2Svg;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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
    private final LrThreshold lrThreshold;
    /**
     * Have the HTML output show at least this many differntials (default: 5).
     */
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
     * @param metadata    Metadata about the analysis.
     */
    public HtmlTemplate(LiricalProperties liricalProperties,
                        PhenotypeService phenotypeService,
                        AnalysisData analysisData,
                        AnalysisResults analysisResults,
                        Map<String, String> metadata,
                        OutputOptions outputOptions,
                        List<String> errors,
                        Set<String> symbolsWithoutGeneIds) {
        super(liricalProperties, phenotypeService, analysisData, metadata, outputOptions);
        this.lrThreshold = outputOptions.lrThreshold();
        this.minDiagnosisToShowInDetail = outputOptions.minDiagnosisCount();
        this.templateData.put("errorlist", errors);
        this.symbolsWithoutGeneIds = symbolsWithoutGeneIds;
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap = new HashMap<>();
        this.topDiagnosisAnchors = new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader, "");
        templateData.put("postprobthreshold", String.format("%.1f%%", 100 * this.lrThreshold.getThreshold()));
        int N = totalDetailedDiagnosesToShow(analysisResults);
        List<SparklinePacket> sparklinePackets = SparklinePacket.sparklineFactory(analysisResults, phenotypeService.diseases(), phenotypeService.hpo(), N);
        this.templateData.put("sparkline", sparklinePackets);
        this.templateData.put("hasGenotypes", "true");
        if (symbolsWithoutGeneIds == null || symbolsWithoutGeneIds.isEmpty()) {
            this.templateData.put("hasGeneSymbolsWithoutIds", "false");
        } else {
            this.templateData.put("hasGeneSymbolsWithoutIds", "true");
            this.templateData.put("geneSymbolsWithoutIds", symbolsWithoutGeneIds);
        }
        Map<TermId, HpoDisease> diseaseById = phenotypeService.diseases().diseaseById();

        AtomicInteger rank = new AtomicInteger();
        analysisResults.resultsWithDescendingPostTestProbability().sequential()
                .forEachOrdered(result -> {
                    int current = rank.incrementAndGet();

                    Optional<GenotypeLrWithExplanation> genotypeLrOpt = result.genotypeLr();
                    if (current <= N) {
                        // Create a full differential diagnosis

                        String symbol = genotypeLrOpt.map(GenotypeLrWithExplanation::geneId)
                                .map(GeneIdentifier::symbol)
                                .orElse(EMPTY_STRING);

                        // Remap `LiricalVariant`s to `VisualizableVariant`s
                        List<VisualizableVariant> variants = genotypeLrOpt.map(GenotypeLrWithExplanation::geneId)
                                .map(GeneIdentifier::id)
                                .map(geneById::get)
                                .map(Gene2Genotype::variants)
                                .orElse(Stream.empty())
                                .map(toVisualizableVariant())
                                .toList();

                        String genotypeExplanation = createGenotypeExplanation(genotypeLrOpt.orElse(null), variants.isEmpty());
                        HpoDisease disease = diseaseById.get(result.diseaseId());
                        Lr2Svg lr2svg = new Lr2Svg(result, current, disease.id(), disease.getDiseaseName(), phenotypeService.hpo(), symbol);
                        DifferentialDiagnosis ddx = new DifferentialDiagnosis(analysisData.sampleId(),
                                disease.id(),
                                disease.getDiseaseName(),
                                result,
                                current,
                                variants,
                                genotypeExplanation,
                                lr2svg.getSvgString());

                        String counterString = String.format("diagnosis%d", current);
                        this.topDiagnosisAnchors.add(counterString);
                        ddx.setAnchor(counterString);
                        this.topDiagnosisMap.put(counterString, ddx.getDiseaseName());
                        diff.add(ddx);
                    } else {
                        // Create an improbable diagnosis for the expandable table
                        if (genotypeLrOpt.isPresent()) {
                            GeneIdentifier geneId = genotypeLrOpt.get().geneId();
                            if (geneById.containsKey(geneId.id())) {
                                int c = this.geneById.get(geneId.id()).variantCount();
                                HpoDisease disease = diseaseById.get(result.diseaseId());
                                String name = shortName(disease.getDiseaseName());
                                String id = result.diseaseId().getId();// This is intended to work with OMIM
                                if (name == null) {
                                    logger.error("Got null string for disease name from result={}", result);
                                    name = EMPTY_STRING;// avoid errors
                                }
                                ImprobableDifferential ipd = new ImprobableDifferential(name, id, geneId.symbol(), result.posttestProbability(), c);
                                improbdiff.add(ipd);
                            }
                        }
                    }
                });
        this.templateData.put("improbdiff", improbdiff);
        this.templateData.put("diff", diff);
    }

    private static String createGenotypeExplanation(GenotypeLrWithExplanation genotypeLr, boolean noVariantsInGene) {
        if (genotypeLr != null) {
            if (noVariantsInGene) {
                GeneIdentifier geneId = genotypeLr.geneId();
                return "No variants found in %s [%s]".formatted(geneId.symbol(), geneId.id().getValue());
            } else {
                return genotypeLr.explanation();
            }
        } else {
            return "No known disease gene";
        }
    }

    @Override
    public void outputFile() {
        logger.info("Writing HTML file to {}", outputPath.toAbsolutePath());
        try (BufferedWriter out = Files.newBufferedWriter(outputPath)) {
            Template template = cfg.getTemplate("liricalHTML.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            logger.warn("Error writing out results {}", te.getMessage(), te);
        }
    }

    @Override
    protected String outputFormatString() {
        return "%s.html";
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
