package org.monarchinitiative.lirical.cli.output;

import freemarker.template.Configuration;
import freemarker.template.Version;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.cli.configuration.*;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This is the superclass for {@link TsvTemplate} and {@link HtmlTemplate}, and provides common methods for
 * setting up the data prior to output as either tab-separated values (TSV) or HTML.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(LiricalTemplate.class);

    protected final LiricalProperties liricalProperties;
    private final AnalysisData analysisData;
    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    protected Path outputPath;
    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    protected Map<String,String> topDiagnosisMap;
    /** Anchors that are used in the HTML output to navigate to the top differential diagnoses. */
    protected List<String> topDiagnosisAnchors;
    protected final Map<TermId, Gene2Genotype> geneById;

    public LiricalTemplate(LiricalProperties liricalProperties,
                           PhenotypeService phenotypeService,
                           AnalysisData analysisData,
                           Map<String, String> metadata,
                           OutputOptions outputOptions) {
        this.liricalProperties = Objects.requireNonNull(liricalProperties);
        this.analysisData = Objects.requireNonNull(analysisData);
        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneById = analysisData.genes().genes().collect(Collectors.toMap(g -> g.geneId().id(), Function.identity()));
        this.outputPath = createOutputFile(outputOptions.outputDirectory(), outputOptions.prefix(), outputFormatString());
        initTemplateData(analysisData, phenotypeService.hpo(), metadata);
    }

    abstract void outputFile();

    protected abstract String outputFormatString();

    private void initTemplateData(AnalysisData analysisData, Ontology ontology, Map<String,String> metadata) {
        templateData.putAll(metadata);
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:analysisData.presentPhenotypeTerms()) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);
        List<String> excludedHpos = new ArrayList<>();
        for (TermId id:analysisData.negatedPhenotypeTerms()) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            excludedHpos.add(tstr);
        }
        this.templateData.put("excludedHPOs",excludedHpos);
        // This is a flag for the output to only show the list if there are some phenotypes that were excluded in the
        // proband.
        if (excludedHpos.size()>0) {
            this.templateData.put("hasExcludedHPOs","true");
        }
    }

    /** Some of our name strings contain multiple synonyms. This function removes all but the first.*/
    protected String shortName(String name) {
        int i = name.indexOf(';');
        if (i>0)
            return name.substring(0,i);
        else
            return name;
    }

    public Path getOutPath() { return outputPath;}

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
        return lv -> new VisualizableVariantDefault(analysisData.sampleId(), lv, isInPathogenicBin(lv));
    }

    private boolean isInPathogenicBin(LiricalVariant lv) {
        return lv.pathogenicity() >= liricalProperties.genotypeLrProperties().pathogenicityThreshold();
    }

}
