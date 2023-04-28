package org.monarchinitiative.lirical.core.likelihoodratio;

import org.monarchinitiative.phenol.annotations.constants.hpo.HpoModeOfInheritanceTermIds;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

/**
 * Results of genotype likelihood ratio evaluation for a single gene.
 */
public class GenotypeLrWithExplanation  {
    private final GeneIdentifier geneId;
    private final GenotypeLrMatchType matchType;
    /** The untransformed likelihood ratio of the genotype. */
    private final double lr;
    private final String explanation;

    static GenotypeLrWithExplanation noVariantsDetectedAutosomalRecessive(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. No variants detected with autosomal recessive disease.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.NO_VARIANTS_DETECTED_AR, ratio, expl);
    }

    static GenotypeLrWithExplanation noVariantsDetectedAutosomalDominant(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. No variants detected.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.NO_VARIANTS_DETECTED_AD, ratio, expl);
    }

    static GenotypeLrWithExplanation twoPathClinVarAllelesRecessive(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. Two pathogenic ClinVar variants detected with autosomal recessive disease.",  Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.TWO_DELETERIOUS_CLINVAR_VARIANTS_IN_AR, ratio, expl);
    }

    static GenotypeLrWithExplanation pathClinVar(GeneIdentifier geneId, double ratio) {
        final String expl = String.format("log<sub>10</sub>(LR)=%.3f. Pathogenic ClinVar variant detected.", Math.log10(ratio));
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.ONE_DELETERIOUS_CLINVAR_VARIANT_IN_AD, ratio, expl);
    }

     static GenotypeLrWithExplanation explainOneAlleleRecessive(GeneIdentifier geneId, double ratio, double observedWeightedPathogenicVariantCount, double lambda_background) {
        int lambda_disease = 2;
        String expl = String.format("log<sub>10</sub>(LR)=%.3f. One pathogenic allele detected with autosomal recessive disease. " +
             "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), observedWeightedPathogenicVariantCount, lambda_disease, lambda_background);
         return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.ONE_DELETERIOUS_VARIANT_IN_AR, ratio, expl);
    }


    static GenotypeLrWithExplanation explainPathCountAboveLambdaB(GeneIdentifier geneId, double ratio, TermId MoI, double lambda_background, double observedWeightedPathogenicVariantCount) {
        int lambda_disease = 1;
        if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE) || MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String expl = String.format("log<sub>10</sub>(LR)=%.3f. %s. Heuristic for high number of observed predicted pathogenic variants. "
                        + "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), getMoIString(MoI), observedWeightedPathogenicVariantCount, lambda_disease, lambda_background);
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.HIGH_NUMBER_OF_OBSERVED_PREDICTED_PATHOGENIC_VARIANTS, ratio, expl);
    }

    static GenotypeLrWithExplanation explanation(GeneIdentifier geneId, double ratio, TermId modeOfInh, double lambda_b, double D, double B, double observedWeightedPathogenicVariantCount) {
        int lambda_disease = 1;
        if (modeOfInh.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE) || modeOfInh.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String msg = String.format("P(G|D)=%.4f. P(G|&#172;D)=%.4f", D, B);
        msg = String.format("log<sub>10</sub>(LR)=%.3f %s. %s. Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f.",
                Math.log10(ratio), msg, getMoIString(modeOfInh), observedWeightedPathogenicVariantCount,  lambda_disease, lambda_b);
        return new GenotypeLrWithExplanation(geneId, GenotypeLrMatchType.LIRICAL_GT_MODEL, ratio, msg);
    }

    private static String getMoIString(TermId MoI) {
        if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_DOMINANT)) {
            return " Mode of inheritance: autosomal dominant";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.AUTOSOMAL_RECESSIVE)) {
            return " Mode of inheritance: autosomal recessive";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_RECESSIVE)) {
            return " Mode of inheritance: X-chromosomal recessive";
        } else if (MoI.equals(HpoModeOfInheritanceTermIds.X_LINKED_DOMINANT)) {
            return " Mode of inheritance: X-chromosomal recessive";
        }
        return " Mode of inheritance: not available"; // should never happen
    }

    /**
     * @deprecated the method has been deprecated and will be removed in <code>v3.0.0</code>.
     * Use {@link #of(GeneIdentifier, GenotypeLrMatchType, double, String)} instead.
     */
    @Deprecated(forRemoval = true, since = "v2.0.0-RC3")
    public static GenotypeLrWithExplanation of(GeneIdentifier geneId, double lr, String explanation) {
        return of(geneId, GenotypeLrMatchType.UNKNOWN, lr, explanation);
    }

    public static GenotypeLrWithExplanation of(GeneIdentifier geneId, GenotypeLrMatchType matchType, double lr, String explanation) {
        return new GenotypeLrWithExplanation(geneId, matchType, lr, explanation);
    }

    private GenotypeLrWithExplanation(GeneIdentifier geneId, GenotypeLrMatchType matchType, double lr, String explanation) {
        this.geneId = Objects.requireNonNull(geneId);
        this.matchType = Objects.requireNonNull(matchType);
        this.lr = lr;
        this.explanation = Objects.requireNonNull(explanation, "Explanation must not be null");
    }

    /**
     * Get the gene identifier for this genotype LR.
     */
    public GeneIdentifier geneId() {
        return geneId;
    }

    /**
     * Get the genotype LR match type.
     */
    public GenotypeLrMatchType matchType() {
        return matchType;
    }

    /**
     * Get the genotype likelihood ratio for the gene. Use {@link #log10Lr()} to get the log LR.
     *
     * @return the genotype likelihood ratio
     */
    public double lr() {
        return lr;
    }

    /**
     * Get the log<sub>10</sub> LR for the gene. Use {@link #lr()} to get the non-transformed value.
     *
     * @return the log<sub>10</sub> of the genotype LR
     */
    public double log10Lr() {
        return Math.log10(lr);
    }

    /**
     * @return an explanation of the genotype likelihood ratio
     */
    public String explanation() {
        return explanation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenotypeLrWithExplanation that = (GenotypeLrWithExplanation) o;
        return Double.compare(that.lr, lr) == 0 && Objects.equals(geneId, that.geneId) && matchType == that.matchType && Objects.equals(explanation, that.explanation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geneId, matchType, lr, explanation);
    }

    @Override
    public String toString() {
        return "GenotypeLrWithExplanation{" +
                "geneId=" + geneId +
                ", matchType=" + matchType +
                ", lr=" + lr +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}
