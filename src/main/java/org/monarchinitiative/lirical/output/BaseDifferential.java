package org.monarchinitiative.lirical.output;

import org.monarchinitiative.lirical.likelihoodratio.TestResult;

abstract class BaseDifferential {

    protected final static String NOT_AVAILABLE="n/a";
    private final String diseaseName;
    /**
     * The CURIE-like identifier of the disease, e.g., OMIM:600123
     */
    private final String diseaseCurie;
    private final String pretestProbability;
    private final String posttestProbability;
    /**
     * The base-10 logarithm of the likelihood ratio.
     */
    private final double compositeLR;
    private final String entrezGeneId;
    private final int rank;

    @Deprecated // use the other constructor
    protected BaseDifferential(TestResult result, int rank) {
        this.diseaseName = prettifyDiseaseName(result.getDiseaseName());
        this.diseaseCurie = result.diseaseId().getValue();
        this.posttestProbability = formatPostTestProbability(result.posttestProbability());
        this.pretestProbability = formatPreTestProbability(result.pretestProbability());
        this.compositeLR = Math.log10(result.getCompositeLR());
        this.entrezGeneId = result.genotypeLr().map(lr -> lr.geneId().getValue()).orElse(NOT_AVAILABLE);
        this.rank = rank;
    }

    protected BaseDifferential(String diseaseName,
                               String diseaseCurie,
                               String pretestProbability,
                               String posttestProbability,
                               double compositeLR,
                               String entrezGeneId,
                               int rank) {
        this.diseaseName = diseaseName;
        this.diseaseCurie = diseaseCurie;
        this.pretestProbability = pretestProbability;
        this.posttestProbability = posttestProbability;
        this.compositeLR = compositeLR;
        this.entrezGeneId = entrezGeneId;
        this.rank = rank;
    }

    protected abstract String formatPostTestProbability(double postTestProbability);

    protected abstract String formatPreTestProbability(double preTestProbability);

    public String getDiseaseName() {
        return diseaseName;
    }

    public String getDiseaseCurie() {
        return diseaseCurie;
    }

    public String getPretestprob() {
        return pretestProbability;
    }

    public String getPosttestprob() {
        return posttestProbability;
    }

    public double getCompositeLR() {
        return compositeLR;
    }

    public String getEntrezGeneId() {
        return entrezGeneId;
    }

    public int getRank() {
        return rank;
    }

    /**
     * We are getting the disease names from OMIM (actually from our small files), and so some of them are long and
     * unweildly strings such as the following:
     * {@code }#101200 APERT SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE I; ACS1;;ACS IAPERT-CROUZON DISEASE,
     * INCLUDED;;ACROCEPHALOSYNDACTYLY, TYPE II, INCLUDED;;ACS II, INCLUDED;;VOGT CEPHALODACTYLY, INCLUDED}. We want to
     * remove any leading numbers and only show the first part of the name (before the first ";;").
     *
     * @param originalName original possibly verbose disease name with synonyms
     * @return prettified disease name intended for display on HTML page
     */
    private String prettifyDiseaseName(String originalName) {
        int i = originalName.indexOf(";;");
        if (i > 0) {
            originalName = originalName.substring(0, i);
        }
        i = 0;
        while (originalName.charAt(i) == '#' || Character.isDigit(originalName.charAt(i)) || Character.isWhitespace(originalName.charAt(i))) {
            i++;
            if (i >= originalName.length()) break;
        }
        return originalName.substring(i);
    }
}
