package org.monarchinitiative.lr2pg.output;

import freemarker.template.Configuration;
import freemarker.template.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the superclass for {@link TsvTemplate} and {@link HtmlTemplate}, and provides common methods for
 * setting up the data prior to output as either tab-separated values (TSV) or HTML.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public abstract class Lr2pgTemplate {
    private static final Logger logger = LogManager.getLogger();

    /** Map of data that will be used for the FreeMark template. */
    protected final Map<String, Object> templateData= new HashMap<>();
    /** FreeMarker configuration object. */
    protected final Configuration cfg;

    protected static final String EMPTY_STRING="";


    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    protected Map<String,String> topDiagnosisMap;
    /** Anchors that are used in the HTML output to navigate to the top differential diagnoses. */
    protected List<String> topDiagnosisAnchors;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected final Map<TermId,String> geneId2symbol;

    public Lr2pgTemplate(HpoCase hcase,
                         HpoOntology ontology,
                         Map<TermId, Gene2Genotype> genotypeMap,
                         Map<TermId,String> geneid2sym,
                         Map<String,String> metadat){

        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        this.geneId2symbol=geneid2sym;

        initTemplateData(hcase,ontology,genotypeMap,metadat);
    }


    abstract public void outputFile();

    private void initTemplateData(HpoCase hcase, HpoOntology ontology, Map<TermId, Gene2Genotype> genotypeMap, Map<String,String> metadat) {
        for(Map.Entry<String,String> entry : metadat.entrySet()) {
            templateData.put(entry.getKey(),entry.getValue());
        }

        List<TermId> observedIds = hcase.getObservedAbnormalities();
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:observedIds) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);





    }

    /** Some of our name strings contain multiple synonyms. This function removes all but the first.*/
    protected String shortName(String name) {
        int i = name.indexOf(';');
        if (i>0)
            return name.substring(0,i);
        else
            return name;
    }


}
