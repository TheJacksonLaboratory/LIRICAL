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
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    private final Map<TermId,String> geneId2symbol;
    /** Threshold to show a differential diagnosis in detail. */
    private  double THRESHOLD;
    /** This map contains the names of the top differential diagnoses that we will show as a list at the
     * top of the page together with anchors to navigate to the detailed analysis.*/
    private Map<String,String> topDiagnosisMap;
    private List<String> topDiagnosisAnchors;
    private static final String EMPTY_STRING="";
    /** FreeMarker configuration object. */
    private final Configuration cfg;

    public HtmlTemplate(HpoCase hcase,
                        HpoOntology ontology,
                        Map<TermId, Gene2Genotype> genotypeMap,
                        Map<TermId,String> geneid2sym,
                        Map<String,String> metadat,
                        double thres){
        this.THRESHOLD=thres;
        this.templateData= new HashMap<>();
        this.cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader,"");
        this.geneId2symbol=geneid2sym;
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
        templateData.put("postprobthreshold",String.format("%.1f%%",100*THRESHOLD));
        List<TermId> observedIds = hcase.getObservedAbnormalities();
        List<String> observedHPOs = new ArrayList<>();
        for (TermId id:observedIds) {
            Term term = ontology.getTermMap().get(id);
            String tstr = String.format("%s (<a href=\"https://hpo.jax.org/app/browse/term/%s\">%s</a>)",term.getName(),id.getValue(),id.getValue());
            observedHPOs.add(tstr);
        }
        this.templateData.put("observedHPOs",observedHPOs);
        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap=new HashMap<>();
        this.topDiagnosisAnchors=new ArrayList<>();
        int counter=0;

        for (TestResult result : hcase.getResults()) {
            String symbol="";
            TermId test=TermId.of("OMIM:101600");
            if (result.getDiseaseCurie().equals(test)) {
                logger.error("HTML Template Found bad entry...");
                logger.error("result..." + result.toString());
                logger.error("Gene id="+result.getEntrezGeneId().getValue());
                logger.error("Contains gene id = " +genotypeMap.containsKey(result.getEntrezGeneId()));
                logger.error("Gene symbol="+symbol);
                //System.exit(1);
            }
            if (result.getPosttestProbability() > THRESHOLD) {
                DifferentialDiagnosis ddx = new DifferentialDiagnosis(result);
                logger.trace("Diff diag for " + result.getDiseaseName());
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    Gene2Genotype g2g = genotypeMap.get(geneId);
                    if (g2g != null) {
                        symbol = g2g.getSymbol();
                        ddx.addG2G(g2g);
                    } else {
                        ddx.setNoVariantsFoundString("no variants found in " + this.geneId2symbol.get(geneId));
                        symbol="no variants found in " + this.geneId2symbol.get(geneId);// will be used by SVG
                    }
                    String expl=result.getExplanation();
                    ddx.setGenotypeScoreExplanation(expl);
                } else {
                    ddx.setNoVariantsFoundString("No known disease gene");
                }
                // now get SVG
                Lr2Svg lr2svg = new Lr2Svg(hcase, result.getDiseaseCurie(), result.getDiseaseName(), ontology, symbol);
                String svg = lr2svg.getSvgString();
                ddx.setSvg(svg);
                diff.add(ddx);
                counter++;
                String counterString=String.format("diagnosis%d",counter);
                this.topDiagnosisAnchors.add(counterString);
                ddx.setAnchor(counterString);
                this.topDiagnosisMap.put(counterString,ddx.getDiseaseName());
            } else {
                if (result.hasGenotype()) {
                    TermId geneId = result.getEntrezGeneId();
                    if (genotypeMap.containsKey(geneId)) {
                        symbol=genotypeMap.get(geneId).getSymbol();
                        int c = genotypeMap.get(geneId).getVarList().size();
                        String name = shortName(result.getDiseaseName());
                        String id = result.getDiseaseCurie().getId();// This is intended to work with OMIM
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
