package org.monarchinitiative.lr2pg.output;

import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.vcf.SimpleVariant;


import java.util.List;

/**
 * This class stores all the information we need for a detailed differential diagnosis -- the major
 * candidates. It is intended to be used as a Java Bean for the output template to store the
 * information needed in a convenient way.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class DifferentialDiagnosis {

    private final static String EMPTY_STRING="";
    private final String diseaseName;
    private final String diseaseCurie;
    /** This is the annchor that will be used on the HTML page. */
    private  String anchor=EMPTY_STRING;
    private final int rank;
    private final String pretestprob;
    private final String posttestprob;
    private final double compositeLR;
    private final String entrezGeneId;
    private final String url;
    private String svg;
    private List<SimpleVariant> varlist;
    /** Set this to yes as a flag for the template to indicate we can show some variants. */
    private String hasVariants="No";

    private String geneSymbol=EMPTY_STRING;

    private String noVariantsFound=EMPTY_STRING;

    private String genotypeScoreExplanation=null;



    DifferentialDiagnosis(TestResult result) {
        this.diseaseName=result.getDiseaseName();
        this.diseaseCurie=result.getDiseaseCurie().getIdWithPrefix();
        this.rank=result.getRank();
        if (result.getPosttestProbability()>0.9999) {
            this.posttestprob=String.format("%.5f%%",100*result.getPosttestProbability());
        } else if (result.getPosttestProbability()>0.999) {
            this.posttestprob=String.format("%.4f%%",100*result.getPosttestProbability());
        } else if (result.getPosttestProbability()>0.99) {
            this.posttestprob=String.format("%.3f%%",100*result.getPosttestProbability());
        } else {
            this.posttestprob=String.format("%.2f%%",100*result.getPosttestProbability());
        }

        double ptp=result.getPretestProbability();
        if (ptp < 0.001) {
            this.pretestprob = String.format("1/%d",Math.round(1.0/ptp));
        } else {
            this.pretestprob = String.format("%.6f",ptp);
        }
        this.compositeLR=result.getCompositeLR();
        if (result.hasGenotype()) {
            this.entrezGeneId = result.getEntrezGeneId().getIdWithPrefix();
        } else {
            this.entrezGeneId=null;
        }
        url=String.format("https://omim.org/%s",result.getDiseaseCurie().getId());
        System.out.println("dn="+this.diseaseName);
    }

    void addG2G(Gene2Genotype g2g) {
        this.geneSymbol=g2g.getSymbol();
        this.hasVariants="yes";
        this.varlist =g2g.getVarList();
    }

    public void setSvg(String s) { this.svg=s; }
    public String getSvg() { return this.svg; }


    public String getDiseaseName() {
        return diseaseName;
    }

    public String getDiseaseCurie() {
        return diseaseCurie;
    }

    public String getUrl(){ return url;}

    public int getRank() {
        return rank;
    }

    public String getPretestprob() {
        return pretestprob;
    }

    public String getPosttestprob() {
        return posttestprob;
    }

    public double getCompositeLR() {
        return compositeLR;
    }

    public String getEntrezGeneId() {
        return entrezGeneId;
    }

    public List<SimpleVariant> getVarlist() {
        return varlist;
    }

    /** The answer to this string is used by the FreeMarker template to determine whether or not to show
     * a table with variants.
     * @return "yes" if this differential diagnosis has variants.
     */
    public String getHasVariants() {
        return hasVariants;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public void setNoVariantsFoundString(String s) {
        noVariantsFound=s;
        System.err.println(noVariantsFound);
    }

    public String getNoVariantsFound() {
        return noVariantsFound;
    }

    public void setAnchor(String a) { this.anchor=a;}
    public String getAnchor(){ return anchor;}

    public String getExplanation() {
        return genotypeScoreExplanation;
    }

    public void setGenotypeScoreExplanation(String genotypeScoreExplanation) {
        this.genotypeScoreExplanation = genotypeScoreExplanation;
    }

    public boolean hasExplanation() { return this.genotypeScoreExplanation != null;}
}
