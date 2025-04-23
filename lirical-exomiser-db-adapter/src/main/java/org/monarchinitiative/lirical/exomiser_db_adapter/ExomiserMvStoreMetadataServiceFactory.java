package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.core.service.VariantMetadataServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class ExomiserMvStoreMetadataServiceFactory implements VariantMetadataServiceFactory, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExomiserMvStoreMetadataServiceFactory.class);

    /* Cache size in MB. */
    static final int CACHE_SIZE = 16;

    private final Map<GenomeBuild, Path> alleleStorePaths;
    private final Map<GenomeBuild, Path> clinvarStorePaths;

    private final Map<GenomeBuild, MVStore> alleleStores = new HashMap<>();
    private final Map<GenomeBuild, MVStore> clinvarStores = new HashMap<>();

    /**
     * @param exomiserDbPaths map with {@link Path} to variant database for a {@link GenomeBuild}.
     *                        Usually, this file is called <code>2309_hg38_variants.mv.db</code> or similar.
     * @deprecated Since Exomiser <code>14.0.0</code>, the pathogenicity scores, allele frequencies data and clinvar
     * are distributed in separate MV stores. Use {@link #fromResources(Map)} instead
     * and provide {@link ExomiserResources} with paths to variant and clinvar database files
     * for each {@link GenomeBuild}.
     * To be removed in <em>3.0.0</em>.
     */
    // TODO: remove in 3.0.0
    @Deprecated(forRemoval = true, since = "2.0.3")
    public ExomiserMvStoreMetadataServiceFactory(Map<GenomeBuild, Path> exomiserDbPaths) {
        this(exomiserDbPaths, Map.of());
    }

    private ExomiserMvStoreMetadataServiceFactory(
            Map<GenomeBuild, Path> alleleStorePaths,
            Map<GenomeBuild, Path> clinvarStorePaths
    ) {
        this.alleleStorePaths = Map.copyOf(alleleStorePaths);
        this.clinvarStorePaths = Map.copyOf(clinvarStorePaths);
    }

    /**
     * @deprecated Since Exomiser <code>14.0.0</code>, the pathogenicity scores, allele frequencies data and clinvar
     * are distributed in separate MV stores. Use {@link #fromResources(Map)} instead
     * and provide {@link ExomiserResources} with paths to variant and clinvar database files
     * for each {@link GenomeBuild}.
     * To be removed in <em>3.0.0</em>.
     */
    // TODO: remove in 3.0.0
    @Deprecated(forRemoval = true, since = "2.0.3")
    public static ExomiserMvStoreMetadataServiceFactory of(Map<GenomeBuild, Path> exomiserDbPaths) {
        return new ExomiserMvStoreMetadataServiceFactory(exomiserDbPaths);
    }

    /**
     * Create {@link ExomiserMvStoreMetadataServiceFactory} from mappings
     * from {@link ExomiserResources} for a {@link GenomeBuild}.
     *
     * @param resources  map with {@link ExomiserResources} for a {@link GenomeBuild}.
     * @return the newly created factory.
     */
    public static ExomiserMvStoreMetadataServiceFactory fromResources(
            Map<GenomeBuild, ExomiserResources> resources
    ) {
        Map<GenomeBuild, Path> allelePaths = new HashMap<>();
        Map<GenomeBuild, Path> clinvarPaths = new HashMap<>();
        resources.forEach((gb, er) -> {
            Path alleleDbPath = er.exomiserAlleleDb();
            if (alleleDbPath != null)
                allelePaths.put(gb, alleleDbPath);

            Path clinvarDbPath = er.exomiserClinvarDb();
            if (clinvarDbPath != null)
                clinvarPaths.put(gb, clinvarDbPath);
        });

        return new ExomiserMvStoreMetadataServiceFactory(allelePaths, clinvarPaths);
    }

    @Override
    public Optional<VariantMetadataService> getVariantMetadataService(GenomeBuild genomeBuild) {
        Optional<MVStore> alleleStore = retrieveMVStore(genomeBuild, Resource.ALLELES, alleleStores, alleleStorePaths);
        Optional<MVStore> clinvarStore = retrieveMVStore(genomeBuild, Resource.CLINVAR, clinvarStores, clinvarStorePaths);

        if (alleleStore.isPresent() || clinvarStore.isPresent()) {
            // We need at least one resource to return some service.
            // Note, the map will be empty if the resource is not configured.
            Map<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap = alleleStore
                    .map(mvStore -> (Map<AlleleProto.AlleleKey, AlleleProto.AlleleProperties>) MvStoreUtil.openAlleleMVMap(mvStore))
                    .orElseGet(Map::of);
            LOGGER.debug("Using allele map with {} entries", alleleMap.size());

            Map<AlleleProto.AlleleKey, AlleleProto.ClinVar> clinvarMap = clinvarStore
                    .map(mvStore -> (Map<AlleleProto.AlleleKey, AlleleProto.ClinVar>) MvStoreUtil.openClinVarMVMap(mvStore))
                    .orElseGet(Map::of);
            LOGGER.debug("Using clinvar map with {} entries", clinvarMap.size());

            return Optional.of(ExomiserMvStoreMetadataService.of(alleleMap, clinvarMap));
        } else {
            return Optional.empty();
        }
    }

    private static synchronized Optional<MVStore> retrieveMVStore(
            GenomeBuild genomeBuild,
            Resource resource,
            Map<GenomeBuild, MVStore> stores,
            Map<GenomeBuild, Path> storePaths
    ) {
        MVStore store = stores.get(genomeBuild);
        if (store == null) {
            Path storePath = storePaths.get(genomeBuild);
            if (storePath == null) {
                LOGGER.debug("Missing database path for {} {}", genomeBuild, resource.name);
                // The user did not configure LIRICAL with path for this resource.
                return Optional.empty();
            } else {
                LOGGER.debug("Opening MVStore for {} {} at {}", genomeBuild, resource.name, storePath.toAbsolutePath());
                store = new MVStore.Builder()
                        .fileName(storePath.toAbsolutePath().toString())
                        .readOnly()
                        .cacheSize(CACHE_SIZE)
                        .open();

                stores.put(genomeBuild, store);
            }
        }

        return Optional.of(store);
    }

    @Override
    public void close() {
        for (Map.Entry<GenomeBuild, MVStore> e : alleleStores.entrySet()) {
            LOGGER.debug("Closing allele store for {}", e.getKey());
            e.getValue().close();
        }

        for (Map.Entry<GenomeBuild, MVStore> e : clinvarStores.entrySet()) {
            LOGGER.debug("Closing clinvar store for {}", e.getKey());
            e.getValue().close();
        }
    }

    private enum Resource {
        ALLELES("alleles"),
        CLINVAR("clinvar");
        private final String name;

        Resource(String name) {
            this.name = name;
        }
    }

}
