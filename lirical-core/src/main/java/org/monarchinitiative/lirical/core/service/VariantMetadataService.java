package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

public interface VariantMetadataService {

    /**
     * We will assume a frequency of 1:100,000 if no frequency data is available.
     */
    float DEFAULT_FREQUENCY = 0.00001F;
    
    static Options defaultOptions() {
        return new Options(DEFAULT_FREQUENCY);
    }

    VariantMetadata metadata(GenomicVariant variant);


    record Options(float defaultFrequency) {
    }
}
