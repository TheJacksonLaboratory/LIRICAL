package org.monarchinitiative.lirical.output;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

import java.io.File;
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

    protected String outpath;
    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    protected Map<String,String> topDiagnosisMap;
    /** Anchors that are used in the HTML output to navigate to the top differential diagnoses. */
    protected List<String> topDiagnosisAnchors;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected final Map<TermId,String> geneId2symbol;



    public LiricalTemplate(HpoCase hcase,
                           Ontology ontology,
                           Map<TermId, Gene2Genotype> genotypeMap,
                           Map<TermId,String> geneid2sym,
                           Map<String,String> metadat){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol=geneid2sym;
        initTemplateData(hcase,ontology,metadat);
    }

    /** This version of the constructor should be used for cases without genotype data
     * @param hcase Data representing the case
     * @param ontology reference to HP ontology
     * @param metadat Metadata about the analysis
     */
    public LiricalTemplate(HpoCase hcase,
                           Ontology ontology,
                           Map<String,String> metadat){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol= ImmutableMap.of(); // not needed -- make empty make
        initTemplateData(hcase,ontology,metadat);
    }

    abstract public void outputFile();
    abstract public void outputFile(String fname);

    private void initTemplateData(HpoCase hcase, Ontology ontology, Map<String,String> metadat) {
        for(Map.Entry<String,String> entry : metadat.entrySet()) {
            templateData.put(entry.getKey(),entry.getValue());
        }
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

    public String getOutPath() { return outpath;}


    protected File mkdirIfNotExist(String dir) {
        File f = new File(dir);
        if (f.exists()) {
            if (f.isDirectory()) {
                return f;
            } else {
                throw new LiricalRuntimeException("Cannot create directory since file of same name exists already: " + dir);
            }
        }
        // if we get here, we need to make the directory
        boolean success = f.mkdir();
        if (!success) {
            throw new LiricalRuntimeException("Unable to make directory: " + dir);
        } else {
            return f;
        }
    }

    public static class Builder {
        private final HpoCase hcase;
        private final Ontology ontology;
        private final Map<String,String> metadata;
        private Map<TermId, Gene2Genotype> genotypeMap;
        private Map<TermId,String> geneid2sym;
        private List<String> errors= ImmutableList.of();
        private Set<String> symbolsWithoutIds;
        private LrThreshold thres;
        private MinDiagnosisCount minDifferentials;
        String outfileprefix = "lirical";
        String outdir = null;


        public Builder(HpoCase hcase, Ontology ont, Map<String,String> mdata){
            this.hcase=hcase;
            this.ontology=ont;
            this.metadata=mdata;
        }


        public Builder genotypeMap(Map<TermId, Gene2Genotype> gm){ this.genotypeMap=gm; return this; }
        public Builder geneid2symMap(Map<TermId,String> gsm) { this.geneid2sym=gsm; return this; }
        public Builder threshold(LrThreshold t) { this.thres=t;return this; }
        public Builder mindiff(MinDiagnosisCount md){ this.minDifferentials=md; return this; }
        public Builder prefix(String p){ this.outfileprefix = p; return this; }
        public Builder outdirectory(String od){ this.outdir=od; return this; }
        public Builder errors(List<String> e) { this.errors = e; return this; }
        public Builder symbolsWithOutIds(Set<String> syms) { this.symbolsWithoutIds = syms; return this; }

        public HtmlTemplate buildPhenotypeHtmlTemplate() {

            return new HtmlTemplate(this.hcase,
                    this.ontology,
                    this.metadata,
                    this.thres,
                    this.minDifferentials,
                    this.outfileprefix,
                    this.outdir,
                    this.errors);
        }

        public HtmlTemplate buildGenoPhenoHtmlTemplate() {
            return new HtmlTemplate(this.hcase,
                    this.ontology,
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
            return new TsvTemplate(this.hcase,
                    this.ontology,
                    this.metadata,
                    this.outfileprefix,
                    this.outdir);
        }

        public TsvTemplate buildGenoPhenoTsvTemplate() {

            return new TsvTemplate(this.hcase,
                    this.ontology,
                    this.genotypeMap,
                    this.geneid2sym,
                    this.metadata,
                    this.outfileprefix,
                    this.outdir);

        }


    }


}
