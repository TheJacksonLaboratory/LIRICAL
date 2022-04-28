package org.monarchinitiative.lirical.core.model;

import java.util.List;
import java.util.Optional;

public interface VariantMetadata {

    static VariantMetadata empty() {
        return VariantMetadataDefault.empty();
    }

    static VariantMetadata of(float frequency,
                              float pathogenicity,
                              ClinvarClnSig clinvarClnSig,
                              List<TranscriptAnnotation> annotations) {
        return new VariantMetadataDefault(frequency,
                pathogenicity,
                clinvarClnSig,
                annotations);
    }

    Optional<Float> frequency();

    float pathogenicity();

    default Optional<Float> pathogenicityScore() {
        // Heuristic -- Count ClinVar pathogenic or likely pathogenic as 1.0 (maximum pathogenicity score)
        // regardless of the Exomiser pathogenicity score
        return clinvarClnSig().isPathogenicOrLikelyPathogenic()
                ? Optional.of(1f)
                : frequencyScore().map(fs -> fs * pathogenicity());
    }

    ClinvarClnSig clinvarClnSig();

    /**
     * @return list of functional variant annotations.
     */
    List<TranscriptAnnotation> annotations();

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
