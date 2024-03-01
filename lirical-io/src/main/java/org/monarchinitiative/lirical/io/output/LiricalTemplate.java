package org.monarchinitiative.lirical.io.output;

import freemarker.template.Configuration;
import freemarker.template.Version;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This is the superclass for {@link TsvTemplate} and {@link HtmlTemplate}, and provides common methods for
 * setting up the data prior to output as either tab-separated values (TSV) or HTML.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class LiricalTemplate {

    private final AnalysisData analysisData;
    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    protected Path outputPath;
    private final float pathogenicityThreshold;
    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    protected Map<String,String> topDiagnosisMap;
    /** Anchors that are used in the HTML output to navigate to the top differential diagnoses. */
    protected List<String> topDiagnosisAnchors;
    protected final Map<TermId, Gene2Genotype> geneById;

    protected LiricalTemplate(MinimalOntology hpo,
                              AnalysisData analysisData,
                              AnalysisResultsMetadata resultsMetadata,
                              OutputOptions outputOptions) {
        this.analysisData = Objects.requireNonNull(analysisData);
        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneById = analysisData.genes().genes().collect(Collectors.toMap(g -> g.geneId().id(), Function.identity()));
        this.outputPath = createOutputFile(outputOptions.outputDirectory(), outputOptions.prefix(), outputFormatString());
        this.pathogenicityThreshold = outputOptions.pathogenicityThreshold();
        initTemplateData(analysisData, hpo, resultsMetadata);
    }

    abstract void outputFile();

    protected abstract String outputFormatString();

    private void initTemplateData(AnalysisData analysisData,
                                  MinimalOntology ontology,
                                  AnalysisResultsMetadata resultsMetadata) {
        templateData.put("resultsMeta", resultsMetadata);
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:analysisData.presentPhenotypeTerms()) {
            String termName = ontology.termForTermId(id)
                    .map(Term::getName)
                    .orElse("UNKNOWN");
            String tstr = String.format("%s <a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>",termName,id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        templateData.put("observedHPOs",observedHPOs);
        List<String> excludedHpos = new ArrayList<>();
        for (TermId id:analysisData.negatedPhenotypeTerms()) {
            String termName = ontology.termForTermId(id)
                    .map(Term::getName)
                    .orElse("UNKNOWN");
            String tstr = String.format("%s <a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>",termName,id.getValue(),id.getValue());
            excludedHpos.add(tstr);
        }
        templateData.put("excludedHPOs",excludedHpos);
        // Indicates that LIRICAL was run without a VCF file.
        templateData.put("phenotypeOnly", analysisData.genes().size() == 0);

    }

    /** Some of our name strings contain multiple synonyms. This function removes all but the first.*/
    protected String shortName(String name) {
        int i = name.indexOf(';');
        if (i>0)
            return name.substring(0,i);
        else
            return name;
    }

    protected static Path createOutputFile(Path outdir, String prefix, String format) {
        if (!Files.isDirectory(outdir))
            mkdirIfNotExist(outdir);
        return outdir.resolve(String.format(format, prefix));
    }

    protected static void mkdirIfNotExist(Path dir) {
        if (Files.exists(dir)) {
            if (Files.isDirectory(dir)) {
                return;
            } else {
                throw new LiricalRuntimeException("Cannot create directory since file of same name exists already: " + dir);
            }
        }
        // if we get here, we need to make the directory
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new LiricalRuntimeException("Unable to make directory: " + dir, e);
        }
    }

    protected Function<LiricalVariant, VisualizableVariant> toVisualizableVariant() {
        return lv -> VisualizableVariant.of(analysisData.sampleId(), lv, isPassingPathogenicThreshold(lv));
    }

    private boolean isPassingPathogenicThreshold(LiricalVariant lv) {
        return lv.pathogenicityScore().orElse(0f) >= pathogenicityThreshold;
    }

    /**
     * Include the result from the differential diagnoses
     * IF we are interested the diseases with no deleterious variants
     * OR if the genotype LR represents a situation where some pathogenic variants were found in the associated gene
     * OR if genotype LR is missing (phenotype only mode).
     *
     * @param showDiseasesWithNoDeleteriousVariants set to {@code true} if you wish to see the differential diagnoses
     *                                              with no deleterious variants regardless of anything.
     */
    static Predicate<TestResult> handleCasesWithNoDeleteriousVariants(boolean showDiseasesWithNoDeleteriousVariants) {
        return r ->
                showDiseasesWithNoDeleteriousVariants
                        || r.genotypeLr()
                        .map(g -> g.matchType().hasDeleteriousVariants())
                        .orElse(true);
    }

}
