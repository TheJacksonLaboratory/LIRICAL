package org.monarchinitiative.lirical.output;


/**
 * This class encapsulates information about the result of LIRICAL analysis, roughly corresponding
 * to one line of the TSV output file with the rank of the originally simulated disease. This
 * class is intended to be used only for the Vcf/Phenopacket simulations.
 */
public class LiricalRanking {

    private final int rank;

    private final String diseaseName;
    private final String diseaseCurie;
    private final String pretest;
    private final  String posttest;
    private final String compositeLR;
    private final String entrezID ;
    private final  String var;
    private String additionalExplanation;

    //(rank, diseaseName,diseaseCurie,pretest,posttest,compositeLR,entrezID,var);

    public LiricalRanking(int r,String name, String curie, String pretest, String posttest, String lr, String entrez, String var) {
        rank=r;
        this.diseaseName=name;
        this.diseaseCurie=curie;
        this.pretest=pretest;
        this.posttest=posttest;
        this.compositeLR=lr;
        this.entrezID=entrez;
        this.var=var;
    }

    public void addExplanation(String e) {
        this.additionalExplanation = e;
    }


    @Override
    public String toString() {
        additionalExplanation=additionalExplanation==null?"-":additionalExplanation;
        return String.join("\t",String.valueOf(rank),diseaseName,diseaseCurie,pretest,posttest, compositeLR,entrezID,additionalExplanation,var);
    }

    public static String header() {
        return "#"+String.join("\t","rank","name","curie","pretest-prob",
            "posttest-prob", "compositeLR","entrezID","explanation","var");
    }


    public int getRank() { return rank; }

}
