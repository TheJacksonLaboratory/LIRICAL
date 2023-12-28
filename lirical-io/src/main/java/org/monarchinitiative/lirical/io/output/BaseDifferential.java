package org.monarchinitiative.lirical.io.output;

import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;

abstract class BaseDifferential {

    protected final static String EMPTY_STRING="";
    protected final static String NOT_AVAILABLE="n/a";
    protected final String sampleId;
    /**
     * The CURIE-like identifier of the disease, e.g., OMIM:600123
     */
    private final String diseaseCurie;
    private final String diseaseName;
    private final String pretestProbability;
    private final String posttestProbability;
    /**
     * The base-10 logarithm of the likelihood ratio.
     */
    private final double compositeLR;
    protected final GeneIdentifier geneId; // nullable
    private final int rank;
    protected final List<VisualizableVariant> variants;

    @Deprecated // use the other constructor
    protected BaseDifferential(String sampleId,
                               TermId diseaseId,
                               String diseaseName,
                               TestResult result,
                               int rank,
                               List<VisualizableVariant> variants) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.diseaseCurie = diseaseId.getValue();
        this.diseaseName = prettifyDiseaseName(diseaseName);
        this.posttestProbability = formatPostTestProbability(result.posttestProbability());
        this.pretestProbability = formatPreTestProbability(result.pretestProbability());
        this.compositeLR = Math.log10(result.getCompositeLR());
        this.geneId = result.genotypeLr().map(GenotypeLrWithExplanation::geneId).orElse(null);
        this.rank = rank;
        this.variants = Objects.requireNonNull(variants);
    }

    protected BaseDifferential(String sampleId,
                               String diseaseName,
                               String diseaseCurie,
                               String pretestProbability,
                               String posttestProbability,
                               double compositeLR,
                               GeneIdentifier geneId,
                               int rank,
                               List<VisualizableVariant> variants) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.diseaseName = Objects.requireNonNull(diseaseName);
        this.diseaseCurie = Objects.requireNonNull(diseaseCurie);
        this.pretestProbability = Objects.requireNonNull(pretestProbability);
        this.posttestProbability = Objects.requireNonNull(posttestProbability);
        this.compositeLR = compositeLR;
        this.geneId = geneId; // nullable
        this.rank = rank;
        this.variants = Objects.requireNonNull(variants);
    }

    protected abstract String formatPostTestProbability(double postTestProbability);

    protected abstract String formatPreTestProbability(double preTestProbability);

    public String sampleId() {
        return sampleId;
    }

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
        return geneId == null ? NOT_AVAILABLE : geneId.id().getValue();
    }

    public String getGeneSymbol() {
        return geneId == null ? EMPTY_STRING : geneId.symbol();
    }

    /** The answer to this string is used by the FreeMarker template to determine whether or not to show
     * a table with variants.
     * @return "yes" if this differential diagnosis has variants.
     */
    public boolean hasVariants() {
        return !variants.isEmpty();
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
