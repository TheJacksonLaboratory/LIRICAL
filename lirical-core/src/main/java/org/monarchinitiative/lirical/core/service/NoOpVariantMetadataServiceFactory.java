package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.GenomeBuild;

import java.util.Optional;

class NoOpVariantMetadataServiceFactory implements VariantMetadataServiceFactory {

    private static final NoOpVariantMetadataServiceFactory INSTANCE = new NoOpVariantMetadataServiceFactory();

    static NoOpVariantMetadataServiceFactory getInstance() {
        return INSTANCE;
    }

    private NoOpVariantMetadataServiceFactory() {
    }

    @Override
    public Optional<VariantMetadataService> getVariantMetadataService(GenomeBuild genomeBuild) {
        return Optional.empty();
    }
}
