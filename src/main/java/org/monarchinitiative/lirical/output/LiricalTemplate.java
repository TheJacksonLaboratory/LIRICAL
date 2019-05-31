package org.monarchinitiative.lirical.output;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Version;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    protected  String outpath;


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
                           Map<String,String> metadat,
                           String predix,
                           String outdir){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol=geneid2sym;
        initpath(predix,outdir);
        initTemplateData(hcase,ontology,metadat);
    }

    public LiricalTemplate(HpoCase hcase,
                           Ontology ontology,
                           Map<String,String> metadat,
                           String prefix,
                           String outdir){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol= ImmutableMap.of(); // not needed -- make empty make
        initTemplateData(hcase,ontology,metadat);
        initpath(prefix,outdir);
    }


    private void initpath(String prefix,String outdir){
        this.outpath=String.format("%s.html",prefix);
        if (outdir != null) {
            File dir = mkdirIfNotExist(outdir);
            this.outpath = Paths.get(dir.getAbsolutePath(),this.outpath).toString();
        }
    }

    /**
     * output a file (HTML or TSV)
//     * @param prefix -- prefix for the file, (e.g., sample would become sample.html or sample.tsv)
//     * @param directory -- directory to which to write the output file. Created automatically if it does not exist
//     */
//    abstract public void outputFile(String prefix, String directory);
    abstract public void outputFile();

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
        private HpoCase hcase;
        private final Ontology ontology;
        private final Map<String,String> metadata;
        private Map<TermId, Gene2Genotype> genotypeMap;
        private Map<TermId,String> geneid2sym;

        double thres=0.01;
        int minDifferentials=10;
        String outfileprefix="lirical";
        String outdir=null;


        public Builder(HpoCase hcase,Ontology ont,Map<String,String> mdata){
            this.hcase=hcase;
            this.ontology=ont;
            this.metadata=mdata;
        }


        public Builder genotypeMap(Map<TermId, Gene2Genotype> gm){this.genotypeMap=gm;return this;}
        public Builder geneid2symMap(Map<TermId,String> gsm) { this.geneid2sym=gsm;return this;}
        public Builder threshold(double t) { this.thres=t;return this;}
        public Builder mindiff(int md){ this.minDifferentials=md; return this;}
        public Builder prefix(String p){ this.outfileprefix = p; return this;}
        public Builder outdirectory(String od){ this.outdir=od;return this;}

        public HtmlTemplate buildPhenotypeHtmlTemplate() {
            /*
            HpoCase hcase,
                        Ontology ontology,
                        Map<String,String> metadat,
                        double thres,
                        int minDifferentials
             */
            // Note -- we assume client code has initialized correctly

            return new HtmlTemplate(this.hcase,
                    this.ontology,
                    this.metadata,
                    this.thres,
                    this.minDifferentials,
                    this.outfileprefix,
                    this.outdir);
        }

        public HtmlTemplate buildGenoPhenoHtmlTemplate() {
            return new HtmlTemplate(this.hcase,
                    this.ontology,
                    this.metadata,
                    this.thres,
                    this.minDifferentials,
                    this.outfileprefix,
                    this.outdir);
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
