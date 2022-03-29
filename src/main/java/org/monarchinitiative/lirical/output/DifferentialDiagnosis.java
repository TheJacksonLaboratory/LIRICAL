package org.monarchinitiative.lirical.output;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.vcf.SimpleVariant;


import java.util.List;

/**
 * This class stores all the information we need for a detailed differential diagnosis -- the major
 * candidates. It is intended to be used as a Java Bean for the FreeMarker HTML template.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class DifferentialDiagnosis extends BaseDifferential {

    private final static String EMPTY_STRING="";

    /** This is the anchor that will be used on the HTML page. */
    private  String anchor=EMPTY_STRING;

    private final String url;
    /** SVG string illustrating the contributions of each feature to the overall score. */
    private String svg;
    private List<SimpleVariant> varlist;
    /** Set this to yes as a flag for the template to indicate we can show some variants. */
    private String hasVariants="No";

    private String geneSymbol = EMPTY_STRING;

    private String genotypeExplanation = EMPTY_STRING;

    private String phenotypeExplanation = EMPTY_STRING;

    DifferentialDiagnosis(TestResult result, int rank) {
        super(result, rank);
        url=String.format("https://hpo.jax.org/app/browse/disease/%s",result.diseaseId().getValue());
    }

    @Override
    protected String formatPostTestProbability(double postTestProbability) {
        return String.format("%.1f%%", 100 * postTestProbability);
    }

    @Override
    protected String formatPreTestProbability(double preTestProbability) {
        if (preTestProbability < 0.001) {
            return String.format("1/%d",Math.round(1.0/preTestProbability));
        } else {
            return String.format("%.6f",preTestProbability);
        }
    }

    void addG2G(Gene2Genotype g2g) {
        this.geneSymbol=g2g.getSymbol();
        this.hasVariants="yes";
        this.varlist =g2g.getVarList();
    }

    public void setSvg(String s) { this.svg=s; }
    public String getSvg() { return this.svg; }

    public String getUrl(){ return url;}

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

    /** @param a An HTML anchor that is used for the HTML template. */
    public void setAnchor(String a) { this.anchor=a;}
    /** @return An HTML anchor that is used for the HTML template. */
    public String getAnchor(){ return anchor;}
    /** @return An genotypeExplanation of how the genotype score was calculated (for the HTML template). */
    public String getGenotypeExplanation() { return genotypeExplanation; }
    /** @param genotypeExplanation An genotypeExplanation of how the genotype score was calculated (for the HTML template). */
    public void setGenotypeExplanation(String genotypeExplanation) {
        this.genotypeExplanation = genotypeExplanation;
    }
    /** @return true iff this differential diagnosis has an genotypeExplanation about the genotype score. */
    public boolean hasGenotypeExplanation() { return !this.genotypeExplanation.isEmpty();}


    public void setPhenotypeExplanation(String text){ this.phenotypeExplanation=text; }
    public String getPhenotypeExplanation(){ return this.phenotypeExplanation; }
    public boolean hasPhenotypeExplanation() { return !this.phenotypeExplanation.isEmpty();}
}
