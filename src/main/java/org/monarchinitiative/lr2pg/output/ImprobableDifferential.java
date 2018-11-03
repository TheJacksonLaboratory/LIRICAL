package org.monarchinitiative.lr2pg.output;

/**
 * This class arranges data for genes that do not have a probable differential diagnosis. The FreeMarker template
 * will show all of them as a table.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
class ImprobableDifferential {

    private final String diseaseName;
    private final String diseaseId;
    private final String geneName;
    private final double posttestProbability;
    private final int varcount;



    ImprobableDifferential(String name, String id,String gene, double ptprob, int count) {
        this.diseaseName=name;
        this.diseaseId=id;
        this.geneName=gene;
        this.posttestProbability=ptprob;
        this.varcount=count;
    }

    public String getDiseaseName() {
        return String.format("<a href=\"omim.org/%s\">%s</a>",diseaseId,diseaseName);
    }

    public String getGeneName() {
        return geneName;
    }

    public double getPosttestProbability() {
        return posttestProbability;
    }

    public int getVarcount() {
        return varcount;
    }
}
