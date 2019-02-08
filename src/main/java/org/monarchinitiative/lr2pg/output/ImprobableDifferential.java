package org.monarchinitiative.lr2pg.output;

import java.text.DecimalFormat;

/**
 * This class arranges data for genes that do not have a probable differential diagnosis. The FreeMarker template
 * will show all of them as a table.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class ImprobableDifferential {

    private final String diseaseName;
    private final String diseaseId;
    private final String geneName;
    private final String posttestProbability;
    private final int varcount;


    /**
     * Construct a data opbject for display in FreeMarker template (which requires a public constructor)
     * @param name Name of the disease
     * @param id database id of the disease
     * @param gene affected gene
     * @param ptprob posterior probability
     * @param count number of variants in {@code gene}
     */
    public ImprobableDifferential(String name, String id, String gene, double ptprob, int count) {
        this.diseaseName=name;
        this.diseaseId=id;
        this.geneName=gene.toUpperCase();
        this.posttestProbability=formatter2(ptprob);
        this.varcount=count;
    }


    public String getDiseaseName() {
        return diseaseName;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public String getGeneName() {
        return geneName;
    }

    public String getPosttestProbability() {
        return posttestProbability;
    }

    public int getVarcount() {
        return varcount;
    }

    private String formatter(double number){
        final DecimalFormat formatter = new DecimalFormat("0.00E00");
        String fnumber = formatter.format(number);
        if (!fnumber.contains("E-")) { //don't blast a negative sign
            fnumber = fnumber.replace("E", "E+");
        }
        return fnumber;
    }


    private String formatter2(double number) {
        String s = formatter(number);
        String A[] = s.split("E");
        if (A.length!=2) return s;
        return String.format("%s x 10<sup>%s</sup>",A[0],A[1]);
    }
}
