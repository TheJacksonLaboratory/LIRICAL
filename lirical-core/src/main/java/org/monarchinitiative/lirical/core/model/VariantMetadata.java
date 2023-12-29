package org.monarchinitiative.lirical.core.model;

import java.util.Optional;

public interface VariantMetadata {

    static VariantMetadata empty() {
        return VariantMetadataDefault.empty();
    }

    static VariantMetadata of(float frequency,
                              float pathogenicity,
                              ClinVarAlleleData clinVarAlleleData) {
        return new VariantMetadataDefault(frequency, pathogenicity, clinVarAlleleData);
    }

    /**
     * Get variant frequency as a percentage or an empty optional if the variant has not been observed in any available
     * variant database.
     * <p>
     * For instance, <code>0.5</code> for a variant with frequency <code>1/500</code>.
     *
     * @return optional variant frequency as a percentage.
     */
    Optional<Float> frequency();

    /**
     * Get estimate of the overall variant pathogenicity.
     * <p>
     * The estimate must be in range <code>[0, 1]</code> where <code>0</code> and <code>1</code> represent
     * the <em>least</em> and the <em>most</em> deleterious variants.
     * <p>
     * The estimate can be an aggregate of multiple scoring tools or a product of a single tool.
     *
     * @return the overall variant pathogenicity estimate.
     */
    float pathogenicity();

    /**
     * Get aggregated variant pathogenicity score.
     * <p>
     * The Clinvar pathogenic or likely pathogenic variants are assigned a score of <code>1</code>.
     * The other variants are assigned a product of the {@link #frequencyScore()} and {@link #pathogenicity()}.
     * An empty optional is returned if the {@link #frequencyScore()} is missing.
     *
     * @return optional pathogenicity score.
     */
    default Optional<Float> pathogenicityScore() {
        // Heuristic -- Count ClinVar pathogenic or likely pathogenic as 1.0 (maximum pathogenicity score)
        // regardless of the Exomiser pathogenicity score
        return clinVarAlleleData()
                .map(a -> a.getClinvarClnSig().isPathogenicOrLikelyPathogenic())
                .orElse(false) // go to the frequencyScore branch
                ? Optional.of(1f)
                : frequencyScore().map(fs -> fs * pathogenicity());
    }

    /**
     * @deprecated since <code>2.0.0-RC3</code> and will be removed in <code>v3.0.0</code>. Use {@link #clinVarAlleleData()} instead.
     * @return Clinvar clinical significance category.
     */
    // REMOVE(v3.0.0)
    @Deprecated(forRemoval = true, since = "2.0.0-RC3")
    default ClinvarClnSig clinvarClnSig() {
        return clinVarAlleleData()
                .map(ClinVarAlleleData::getClinvarClnSig)
                .orElse(ClinvarClnSig.NOT_PROVIDED);
    }

    /**
     * @return <code>ClinvarData</code> for the variant, if available.
     */
    Optional<ClinVarAlleleData> clinVarAlleleData();

    /**
     * This is the frequency factor used for the Exomiser like pathogenicity score. It penalizes variants that have a higher
     * population frequency, with anything above 2% getting a factor of zero.
     * @return The Exomiser-style frequency factor
     */
    default Optional<Float> frequencyScore() {
        return frequency().map(frequency -> {
            if (frequency <= 0) {
                return 1f;
            } else if (frequency > 2) {
                return 0f;
            } else {
                return 1.13533f - (0.13533f * (float) Math.exp(frequency));
            }
        });
    }

    /**
     * @deprecated the function has been deprecated without replacement and will be removed in <code>v3.0.0</code>.
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC3")
    static int compareByPathogenicity(VariantMetadata left, VariantMetadata right) {
        // REMOVE(v3.0.0)
        return Float.compare(left.pathogenicity(), right.pathogenicity());
    }

}
