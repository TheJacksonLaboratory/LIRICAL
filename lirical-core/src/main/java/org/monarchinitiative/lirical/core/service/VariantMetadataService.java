package org.monarchinitiative.lirical.core.service;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;

public interface VariantMetadataService {

    /**
     * We will assume a frequency of 1:100,000 if no frequency data is available.
     * <p>
     * Note that the frequency is stored as a percentage.
     */
    float DEFAULT_FREQUENCY = 1e-3f;

    VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects);

}
