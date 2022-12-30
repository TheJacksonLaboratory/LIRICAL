package org.monarchinitiative.lirical.core.service;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;

public interface VariantMetadataService {

    /**
     * We will assume a frequency of 1:100,000 if no frequency data is available.
     */
    float DEFAULT_FREQUENCY = 1e-5f;

    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    static Options defaultOptions() {
        return new Options(DEFAULT_FREQUENCY);
    }

    VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects);

    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    record Options(float defaultFrequency) {
    }
}
