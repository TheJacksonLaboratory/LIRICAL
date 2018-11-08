package org.monarchinitiative.lr2pg.output;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates getting the data from the analysis into the FreeMark output templates.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HtmlTemplate {
    private static final Logger logger = LogManager.getLogger();
    /** Map of data that will be used for the FreeMark template. */
    private final Map<String, Object> templateData;
    /** Threshold to show a differential diagnosis in detail. */
    private static final double THRESHOLD = 0.01;
    private static final String EMPTY_STRING="";
    /** FreeMarker configuration object. */
    private final Configuration cfg;

    public HtmlTemplate(HpoCase hcase, HpoOntology ontology, Map<TermId, Gene2Genotype> genotypeMap, Map<String,String> metadat){
        this.templateData= new HashMap<>();
        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader,"");

        initTemplateData(hcase,ontology,genotypeMap,metadat);



        try (BufferedWriter out = new BufferedWriter(new FileWriter("myout.html"))) {
            Template template = cfg.getTemplate("lrhtml.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }


    }


    private void initTemplateData(HpoCase hcase, HpoOntology ontology,Map<TermId, Gene2Genotype> genotypeMap,Map<String,String> metadat) {
        for(Map.Entry<String,String> entry : metadat.entrySet()) {
            templateData.put(entry.getKey(),entry.getValue());
        }
        templateData.put("postprobthreshold",String.valueOf(THRESHOLD));
        List<TermId> observedIds = hcase.getObservedAbnormalities();
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:observedIds) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getIdWithPrefix(),id.getIdWithPrefix());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        String symbol="";
        for (TestResult result : hcase.getResults()) {
            if (result.getPosttestProbability() > THRESHOLD) {
                DifferentialDiagnosis ddx = new DifferentialDiagnosis(result);
                logger.error("Diff diag for " + result.getDiseaseName());
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    Gene2Genotype g2g = genotypeMap.get(geneId);
                    if (g2g != null) {
                        symbol = g2g.getSymbol();
                        ddx.addG2G(g2g);
                    }
                }
                // now get SVG
                Lr2Svg lr2svg = new Lr2Svg(hcase, result.getDiseaseCurie(), result.getDiseaseName(), ontology, symbol);
                String svg = lr2svg.getSvgString();
                ddx.setSvg(svg);
                diff.add(ddx);
            } else {
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    if (genotypeMap.containsKey(geneId)) {
                        symbol=genotypeMap.get(geneId).getSymbol();
                        int c = genotypeMap.get(geneId).getVarList().size();
                        String name = shortName(result.getDiseaseName());
                        String id = result.getDiseaseCurie().getIdWithPrefix();
                        if (name==null) {
                            logger.error("Got null string for disease name from result="+result.toString());
                            name=EMPTY_STRING;// avoid errors
                        }
                        ImprobableDifferential ipd = new ImprobableDifferential(name,id,symbol,result.getPosttestProbability(),c);
                        improbdiff.add(ipd);
                    }
                }
            }
        }
        this.templateData.put("improbdiff",improbdiff);
        this.templateData.put("diff",diff);

    }

    /** Some of our name strings contain multiple synonyms. This function removes all but the first.*/
    private String shortName(String name) {
        int i = name.indexOf(';');
        if (i>0)
            return name.substring(0,i);
        else
            return name;
    }

}
