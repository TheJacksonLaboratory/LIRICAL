package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.List;
import java.util.Objects;

/**
 * This class stores all the information we need for a detailed differential diagnosis -- the major
 * candidates. It is intended to be used as a Java Bean for the FreeMarker HTML template.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class DifferentialDiagnosis extends BaseDifferential {

    /** This is the anchor that will be used on the HTML page. */
    private String anchor = EMPTY_STRING;

    private final String url;
    /** SVG string illustrating the contributions of each feature to the overall score. */
    private final String svg;

    private final String genotypeExplanation;

    DifferentialDiagnosis(String sampleId,
                          TermId diseaseId,
                          String diseaseName,
                          TestResult result,
                          int rank,
                          List<VisualizableVariant> variants,
                          String genotypeExplanation,
                          String svg) {
        super(sampleId, diseaseId, diseaseName, result, rank, variants);
        url=String.format("https://hpo.jax.org/app/browse/disease/%s",result.diseaseId().getValue());
        this.genotypeExplanation = genotypeExplanation; // nullable
        this.svg = Objects.requireNonNull(svg);
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

    public String getSvg() { return this.svg; }

    public String getUrl(){ return url;}

    public String getGeneUrl() {
        return geneId == null
                ? ""
                : String.format("https://hpo.jax.org/app/browse/gene/%s", geneId.id().getId());
    }

    public List<VisualizableVariant> getVisualizableVariants() {
        return variants;
    }

    /** @param a An HTML anchor that is used for the HTML template. */
    public void setAnchor(String a) { this.anchor=a;}
    /** @return An HTML anchor that is used for the HTML template. */
    public String getAnchor(){ return anchor;}
    public boolean hasGenotypeExplanation() {
        return genotypeExplanation != null && !genotypeExplanation.equals(EMPTY_STRING);
    }
    /** @return An genotypeExplanation of how the genotype score was calculated (for the HTML template). */
    public String getGenotypeExplanation() { return genotypeExplanation == null ? EMPTY_STRING : genotypeExplanation; }

}
