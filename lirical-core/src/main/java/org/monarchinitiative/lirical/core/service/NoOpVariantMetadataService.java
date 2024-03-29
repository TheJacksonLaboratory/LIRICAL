package org.monarchinitiative.lirical.core.service;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;

/**
 * A {@link VariantMetadataService} implementation used when the variant data is not available.
 */
public class NoOpVariantMetadataService implements VariantMetadataService {

    private static final NoOpVariantMetadataService INSTANCE = new NoOpVariantMetadataService();

    public static NoOpVariantMetadataService instance() {
        return INSTANCE;
    }

    private NoOpVariantMetadataService() {
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects) {
        return VariantMetadata.empty();
    }

}
