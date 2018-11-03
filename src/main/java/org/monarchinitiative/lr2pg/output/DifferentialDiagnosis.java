package org.monarchinitiative.lr2pg.output;

import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.vcf.SimpleVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * This class stores all the information we need for a detailed differential diagnosis -- the major
 * candidates. It is intended to be used as a Java Bean for the output template to store the
 * information needed in a convenient way.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class DifferentialDiagnosis {


    private final String diseaseName;
    private final String diseaseCurie;
    private final int rank;
    private final double pretestprob;
    private final double posttestprob;
    private final double compositeLR;
    private final String entrezGeneId;
    private String svg;
    private List<SimpleVariant> varlist;
    /** Set this to yes as a flag for the template to indicate we can show some variants. */
    private String hasVariants="No";

    private String geneSymbol=null;

    DifferentialDiagnosis(TestResult result) {
        this.diseaseName=result.getDiseaseName();
        this.diseaseCurie=result.getDiseaseCurie().getIdWithPrefix();
        this.rank=result.getRank();
        this.posttestprob=result.getPosttestProbability();
        this.pretestprob=result.getPretestProbability();
        this.compositeLR=result.getCompositeLR();
        if (result.hasGenotype()) {
            this.entrezGeneId = result.getEntrezGeneId().getIdWithPrefix();
        } else {
            this.entrezGeneId=null;
        }

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

    public int getRank() {
        return rank;
    }

    public double getPretestprob() {
        return pretestprob;
    }

    public double getPosttestprob() {
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

    public String getHasVariants() {
        return hasVariants;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }
}
