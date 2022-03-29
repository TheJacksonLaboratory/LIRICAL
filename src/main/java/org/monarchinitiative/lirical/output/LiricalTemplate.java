package org.monarchinitiative.lirical.output;

import com.google.common.collect.ImmutableList;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.configuration.LrThreshold;
import org.monarchinitiative.lirical.configuration.MinDiagnosisCount;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This is the superclass for {@link TsvTemplate} and {@link HtmlTemplate}, and provides common methods for
 * setting up the data prior to output as either tab-separated values (TSV) or HTML.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class LiricalTemplate {
    private static final Logger logger = LoggerFactory.getLogger(LiricalFactory.class);

    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";

    protected Path outpath;
    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    protected Map<String,String> topDiagnosisMap;
    /** Anchors that are used in the HTML output to navigate to the top differential diagnoses. */
    protected List<String> topDiagnosisAnchors;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected final Map<TermId,String> geneId2symbol;



    /** This version of the constructor should be used for cases without genotype data
     * @param hcase Data representing the case
     * @param ontology reference to HP ontology
     * @param metadata Metadata about the analysis
     */
    public LiricalTemplate(HpoCase hcase,
                           Ontology ontology,
                           Map<String,String> metadata){
        this(hcase, ontology, Map.of(), metadata);
    }

    public LiricalTemplate(HpoCase hcase,
                           Ontology ontology,
                           Map<TermId,String> geneIdToSymbol,
                           Map<String,String> metadata){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol=geneIdToSymbol;
        initTemplateData(hcase,ontology,metadata);
    }

    abstract public void outputFile();
    abstract public void outputFile(String fname);

    private void initTemplateData(HpoCase hcase, Ontology ontology, Map<String,String> metadata) {
        templateData.putAll(metadata);
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:hcase.getObservedAbnormalities()) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);
        List<String> excludedHpos = new ArrayList<>();
        for (TermId id:hcase.getExcludedAbnormalities()) {
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

    public Path getOutPath() { return outpath;}

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

    public static Builder builder(HpoCase hpoCase, Ontology hpo, Map<String, String> metadata) {
        return new Builder(hpoCase, hpo, metadata);
    }

    public static class Builder {
        private final HpoCase hpoCase;
        private final Ontology hpo;
        private final Map<String,String> metadata;
        private Map<TermId, Gene2Genotype> genotypeMap;
        private Map<TermId,String> geneid2sym;
        private List<String> errors= ImmutableList.of();
        private Set<String> symbolsWithoutIds;
        private LrThreshold thres;
        private MinDiagnosisCount minDifferentials;
        String outfileprefix = "lirical";
        private Path outdir = null;


        private Builder(HpoCase hpoCase, Ontology hpo, Map<String,String> metadata){
            this.hpoCase = hpoCase;
            this.hpo = hpo;
            this.metadata = metadata;
        }


        public Builder genotypeMap(Map<TermId, Gene2Genotype> gm){ this.genotypeMap=gm; return this; }
        public Builder geneid2symMap(Map<TermId,String> gsm) { this.geneid2sym=gsm; return this; }
        public Builder threshold(LrThreshold t) { this.thres=t;return this; }
        public Builder mindiff(MinDiagnosisCount md){ this.minDifferentials=md; return this; }
        public Builder prefix(String p){ this.outfileprefix = p; return this; }
        public Builder outDirectory(Path od){ this.outdir=od; return this; }
        public Builder errors(List<String> e) { this.errors = e; return this; }
        public Builder symbolsWithOutIds(Set<String> syms) { this.symbolsWithoutIds = syms; return this; }

        public HtmlTemplate buildPhenotypeHtmlTemplate() {

            return new HtmlTemplate(this.hpoCase,
                    this.hpo,
                    this.metadata,
                    this.thres,
                    this.minDifferentials,
                    this.outfileprefix,
                    this.outdir,
                    this.errors);
        }

        public HtmlTemplate buildGenoPhenoHtmlTemplate() {
            return new HtmlTemplate(this.hpoCase,
                    this.hpo,
                    this.genotypeMap,
                    this.geneid2sym,
                    this.metadata,
                    this.thres,
                    this.minDifferentials,
                    this.outfileprefix,
                    this.outdir,
                    this.errors,
                    this.symbolsWithoutIds);
        }

        public TsvTemplate buildPhenotypeTsvTemplate() {
            return new TsvTemplate(this.hpoCase,
                    this.hpo,
                    this.metadata,
                    this.outfileprefix,
                    this.outdir);
        }

        public TsvTemplate buildGenoPhenoTsvTemplate() {

            return new TsvTemplate(this.hpoCase,
                    this.hpo,
                    this.genotypeMap,
                    this.geneid2sym,
                    this.metadata,
                    this.outfileprefix,
                    this.outdir);

        }


    }


}
