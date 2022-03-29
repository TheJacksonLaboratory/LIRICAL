package org.monarchinitiative.lirical.model;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;

import java.util.List;
import java.util.Optional;

public interface VariantMetadata {

    static VariantMetadata empty() {
        return VariantMetadataDefault.empty();
    }

    static VariantMetadata of(float frequency,
                              float pathogenicity,
                              boolean isClinvarPathogenic,
                              List<TranscriptAnnotation> annotations) {
        return new VariantMetadataDefault(frequency,
                pathogenicity,
                isClinvarPathogenic,
                annotations);
    }

    Optional<Float> frequency();

    float pathogenicityScore();

    boolean isClinVarPathogenic();

    List<TranscriptAnnotation> annotations();

    /**
     * This is the frequency factor used for the Exomiser like pathogenicity score. It penalizes variants that have a higher
     * population frequency, with anything above 2% getting a factor of zero.
     * @return The Exomiser-style frequency factor
     */
    // TODO - why is this not used?
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
        return Float.compare(left.pathogenicityScore(), right.pathogenicityScore());
    }

}
