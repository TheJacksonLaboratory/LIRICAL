package org.monarchinitiative.lirical.output;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.vcf.SimpleVariant;

import java.util.List;
import java.util.stream.Collectors;

public class TsvDifferential extends BaseDifferential {

    private final static String EMPTY_STRING="";

    private List<SimpleVariant> varlist;
    private String varString=NOT_AVAILABLE;
    /** Set this to yes as a flag for the template to indicate we can show some variants. */
    private String hasVariants="No";

    private String geneSymbol=EMPTY_STRING;

    public TsvDifferential(TestResult result, int rank) {
        super(result, rank);
    }

    @Override
    protected String formatPostTestProbability(double postTestProbability) {
        if (postTestProbability >0.9999) {
            return String.format("%.5f%%",100* postTestProbability);
        } else if (postTestProbability >0.999) {
            return String.format("%.4f%%",100* postTestProbability);
        } else if (postTestProbability >0.99) {
            return String.format("%.3f%%",100* postTestProbability);
        } else {
            return String.format("%.2f%%",100* postTestProbability);
        }
    }

    @Override
    protected String formatPreTestProbability(double preTestProbability) {
        if (preTestProbability < 0.001) {
            return String.format("1/%d",Math.round(1.0/ preTestProbability));
        } else {
            return String.format("%.6f", preTestProbability);
        }
    }

    void addG2G(Gene2Genotype g2g) {
        this.geneSymbol=g2g.getSymbol();
        this.hasVariants="yes";
        this.varlist =g2g.getVarList();
        this.varString=varlist.stream().map(SimpleVariant::toString).collect(Collectors.joining("; "));
    }

    public String getVarString() {
        return varString;
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
