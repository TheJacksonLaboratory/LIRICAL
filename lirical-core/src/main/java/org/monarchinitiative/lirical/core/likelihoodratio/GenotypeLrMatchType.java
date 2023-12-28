package org.monarchinitiative.lirical.core.likelihoodratio;

/**
 * The enum for representing the type of the genotype likelihood ratio analysis performed for a gene.
 *
 * @see GenotypeLrWithExplanation
 */
public enum GenotypeLrMatchType {

    /**
     * No variants were detected in a gene associated with a disease with autosomal dominant inheritance.
     */
    NO_VARIANTS_DETECTED_AD,

    /**
     * No variants were detected in a gene associated with a disease with autosomal recessive inheritance.
     */
    NO_VARIANTS_DETECTED_AR,

    /**
     * One ClinVar pathogenic or likely pathogenic allele discovered in a disease
     * with autosomal dominant inheritance.
     */
    ONE_P_OR_LP_CLINVAR_ALLELE_IN_AD,

    /**
     * Two ClinVar pathogenic or likely pathogenic alleles discovered in a disease
     * with autosomal recessive inheritance.
     */
    TWO_P_OR_LP_CLINVAR_ALLELES_IN_AR,

    /**
     * One deleterious allele detected with autosomal recessive disease.
     */
    ONE_DELETERIOUS_VARIANT_IN_AR,

    /**
     * Heuristic for the case where we have more called pathogenic variants than we should have
     * in a gene without a high background count -- we will model this as technical error and
     * will take the observed path weighted count to not be more than &lambda;<sub>disease</sub>.
     * this will have the effect of not down-weighting these genes
     * the user will have to judge whether one of the variants is truly pathogenic.
     */
    HIGH_NUMBER_OF_OBSERVED_PREDICTED_PATHOGENIC_VARIANTS,

    /**
     * Gene scored using LIRICAL genotype LR model.
     * <p>
     * For more details, consult the <em>Material and Methods | Likelihood Ratio for Genotypes</em> section
     * of the <a href="https://pubmed.ncbi.nlm.nih.gov/32755546/">LIRICAL manuscript</a>.
     */
    LIRICAL_GT_MODEL,

    /**
     * DO NOT USE.
     *
     * @deprecated the method has been deprecated and will be removed in <code>v3.0.0</code>.
     * Use {@link #ONE_P_OR_LP_CLINVAR_ALLELE_IN_AD} instead.
     */
    @Deprecated(forRemoval = true, since = "v2.0.0")
    // REMOVE(v3.0.0)
    ONE_DELETERIOUS_CLINVAR_VARIANT_IN_AD,

    /**
     * DO NOT USE.
     *
     * @deprecated the method has been deprecated and will be removed in <code>v3.0.0</code>.
     * Use {@link #TWO_P_OR_LP_CLINVAR_ALLELES_IN_AR} instead.
     */
    @Deprecated(forRemoval = true, since = "v2.0.0")
    // REMOVE(v3.0.0)
    TWO_DELETERIOUS_CLINVAR_VARIANTS_IN_AR,

    /**
     * DO NOT USE. A placeholder value used in the deprecated methods for backward compatibility.
     *
     * @deprecated the field will be removed in <code>v3.0.0</code>.
     */
    @Deprecated(forRemoval = true)
    // REMOVE(v3.0.0)
    UNKNOWN
}
