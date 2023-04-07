package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.core.service.VariantMetadataServiceFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ExomiserMvStoreMetadataServiceFactory implements VariantMetadataServiceFactory {

    private final Map<GenomeBuild, Path> exomiserDbPaths;

    public ExomiserMvStoreMetadataServiceFactory(Map<GenomeBuild, Path> exomiserDbPaths) {
        this.exomiserDbPaths = Objects.requireNonNull(exomiserDbPaths);
    }

    public static ExomiserMvStoreMetadataServiceFactory of(Map<GenomeBuild, Path> exomiserDbPaths) {
        return new ExomiserMvStoreMetadataServiceFactory(exomiserDbPaths);
    }

    @Override
    public Optional<VariantMetadataService> getVariantMetadataService(GenomeBuild genomeBuild) {
        Path path = exomiserDbPaths.get(genomeBuild);
        return path != null
                ? Optional.of(ExomiserMvStoreMetadataService.of(path))
                : Optional.empty();
    }
}
