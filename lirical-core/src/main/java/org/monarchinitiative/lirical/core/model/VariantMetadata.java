package org.monarchinitiative.lirical.core.model;

import java.util.Optional;

public interface VariantMetadata {

    static VariantMetadata empty() {
        return VariantMetadataDefault.empty();
    }

    static VariantMetadata of(float frequency,
                              float pathogenicity,
                              ClinvarClnSig clinvarClnSig) {
        return new VariantMetadataDefault(frequency,
                pathogenicity,
                clinvarClnSig);
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
        return clinvarClnSig().isPathogenicOrLikelyPathogenic()
                ? Optional.of(1f)
                : frequencyScore().map(fs -> fs * pathogenicity());
    }

    /**
     * @return Clinvar clinical significance category.
     */
    ClinvarClnSig clinvarClnSig();

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


    static int compareByPathogenicity(VariantMetadata left, VariantMetadata right) {
        return Float.compare(left.pathogenicity(), right.pathogenicity());
    }

}
