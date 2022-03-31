package org.monarchinitiative.lirical.output;

import com.google.common.collect.ImmutableList;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.monarchinitiative.lirical.configuration.*;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.model.HpoCase;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.model.LiricalVariant;
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

/**
 * This is the superclass for {@link TsvTemplate} and {@link HtmlTemplate}, and provides common methods for
 * setting up the data prior to output as either tab-separated values (TSV) or HTML.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(LiricalFactory.class);

    protected final LiricalProperties liricalProperties;
    private final HpoCase hpoCase;
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
                           HpoCase hpoCase,
                           Ontology ontology,
                           Map<TermId, Gene2Genotype> geneById,
                           Map<String, String> metadata,
                           Path outdir,
                           String prefix) {
        this.liricalProperties = Objects.requireNonNull(liricalProperties);
        this.hpoCase = Objects.requireNonNull(hpoCase);
        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneById = geneById;
        this.outputPath = createOutputFile(outdir, prefix, outputFormatString());
        initTemplateData(hpoCase,ontology,metadata);
    }

    abstract public void outputFile();
    abstract public void outputFile(String fname);

    private void initTemplateData(HpoCase hpoCase, Ontology ontology, Map<String,String> metadata) {
        templateData.putAll(metadata);
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:hpoCase.getObservedAbnormalities()) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);
        List<String> excludedHpos = new ArrayList<>();
        for (TermId id:hpoCase.getExcludedAbnormalities()) {
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

    protected abstract String outputFormatString();

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
        return lv -> new VisualizableVariantDefault(hpoCase.sampleId(), lv, isInPathogenicBin(lv));
    }

    private boolean isInPathogenicBin(LiricalVariant lv) {
        return lv.pathogenicity() >= liricalProperties.genotypeLrProperties().pathogenicityThreshold();
    }

    // TODO - should we replace HpoCase with AnalysisData?
    public static Builder builder(LiricalProperties liricalProperties, HpoCase hpoCase, Ontology hpo, Map<TermId, Gene2Genotype> genesById, Map<String, String> metadata) {
        return new Builder(liricalProperties, hpoCase, hpo, genesById, metadata);
    }

    public static class Builder {
        private final LiricalProperties liricalProperties;
        private final HpoCase hpoCase;
        private final Ontology hpo;
        private final Map<TermId, Gene2Genotype> genesById;
        private final Map<String,String> metadata;
        private List<String> errors= ImmutableList.of();
        private Set<String> symbolsWithoutIds;
        private LrThreshold thres;
        private MinDiagnosisCount minDifferentials;
        String outfileprefix = "lirical";
        private Path outdir = null;


        private Builder(LiricalProperties liricalProperties,
                        HpoCase hpoCase,
                        Ontology hpo,
                        Map<TermId, Gene2Genotype> genesById,
                        Map<String,String> metadata){
            this.liricalProperties = liricalProperties;
            this.hpoCase = hpoCase;
            this.hpo = hpo;
            this.genesById = genesById;
            this.metadata = metadata;
        }

        public Builder threshold(LrThreshold t) { this.thres=t;return this; }
        public Builder mindiff(MinDiagnosisCount md){ this.minDifferentials=md; return this; }
        public Builder prefix(String p){ this.outfileprefix = p; return this; }
        public Builder outDirectory(Path od){ this.outdir=od; return this; }
        public Builder errors(List<String> e) { this.errors = e; return this; }
        public Builder symbolsWithOutIds(Set<String> syms) { this.symbolsWithoutIds = syms; return this; }

        public HtmlTemplate buildPhenotypeHtmlTemplate() {
            return new HtmlTemplate(liricalProperties,
                    hpoCase,
                    hpo,
                    genesById,
                    metadata,
                    thres,
                    minDifferentials,
                    outdir,
                    outfileprefix,
                    errors,
                    Set.of());
        }

        public HtmlTemplate buildGenoPhenoHtmlTemplate() {
            return new HtmlTemplate(liricalProperties,
                    hpoCase,
                    hpo,
                    genesById,
                    metadata,
                    thres,
                    minDifferentials,
                    outdir,
                    outfileprefix,
                    errors,
                    symbolsWithoutIds);
        }

        public TsvTemplate buildPhenotypeTsvTemplate() {
            return new TsvTemplate(liricalProperties,
                    hpoCase,
                    hpo,
                    genesById,
                    metadata,
                    outfileprefix,
                    outdir);
        }

        public TsvTemplate buildGenoPhenoTsvTemplate() {
            return new TsvTemplate(liricalProperties,
                    hpoCase,
                    hpo,
                    genesById,
                    metadata,
                    outdir,
                    outfileprefix
            );

        }


    }


}
