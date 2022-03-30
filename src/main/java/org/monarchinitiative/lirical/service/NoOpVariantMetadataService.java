package org.monarchinitiative.lirical.service;

import org.monarchinitiative.lirical.model.VariantMetadata;
import org.monarchinitiative.svart.GenomicVariant;

public class NoOpVariantMetadataService implements VariantMetadataService {

    private static final NoOpVariantMetadataService INSTANCE = new NoOpVariantMetadataService();

    public static NoOpVariantMetadataService instance() {
        return INSTANCE;
    }

    private NoOpVariantMetadataService() {
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant) {
        return VariantMetadata.empty();
    }

}
