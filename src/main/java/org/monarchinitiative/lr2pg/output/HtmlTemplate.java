package org.monarchinitiative.lr2pg.output;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates getting the data from the analysis into the FreeMark org.monarchinitiative.lr2pg.output templates.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HtmlTemplate extends Lr2pgTemplate {
    private static final Logger logger = LogManager.getLogger();

    /** Threshold posterior probability to show a differential diagnosis in detail. */
    private final double THRESHOLD;

    /**
     * Constructor to initialize the data that will be needed to output an HTML page.
     * @param hcase The individual (case) represented in the VCF file
     * @param ontology The HPO ontology
     * @param genotypeMap A map of genotypes for all genes with variants in the VCF file
     * @param geneid2sym A map from the Entrez Gene id to the gene symbol
     * @param metadat Metadata about the analysis.
     * @param thres threshold posterior probability to show differential in detail
     */
    public HtmlTemplate(HpoCase hcase,
                        HpoOntology ontology,
                        Map<TermId, Gene2Genotype> genotypeMap,
                        Map<TermId,String> geneid2sym,
                        Map<String,String> metadat,
                        double thres){
        super(hcase, ontology, genotypeMap, geneid2sym, metadat);
        this.THRESHOLD=thres;

        List<DifferentialDiagnosis> diff = new ArrayList<>();
        List<ImprobableDifferential> improbdiff = new ArrayList<>();
        this.topDiagnosisMap=new HashMap<>();
        this.topDiagnosisAnchors=new ArrayList<>();
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader,"");
        templateData.put("postprobthreshold",String.format("%.1f%%",100*THRESHOLD));
        int counter=0;
        for (TestResult result : hcase.getResults()) {
            String symbol=EMPTY_STRING;
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


    @Override
    public void outputFile(){
        try (BufferedWriter out = new BufferedWriter(new FileWriter("myout.html"))) {
            Template template = cfg.getTemplate("lrhtml.ftl");
            template.process(templateData, out);
        } catch (TemplateException | IOException te) {
            te.printStackTrace();
        }
    }




}
