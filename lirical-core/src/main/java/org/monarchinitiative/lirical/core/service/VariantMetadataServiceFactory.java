package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.GenomeBuild;

import java.util.Optional;

/**
 * {@linkplain VariantMetadataServiceFactory} knows about {@link VariantMetadataService}s for all genome builds
 * that have been configured for a given {@link org.monarchinitiative.lirical.core.Lirical} instance.
 */
public interface VariantMetadataServiceFactory {

    static VariantMetadataServiceFactory noOpFactory() {
        return NoOpVariantMetadataServiceFactory.getInstance();
    }

    Optional<VariantMetadataService> getVariantMetadataService(GenomeBuild genomeBuild);

}
