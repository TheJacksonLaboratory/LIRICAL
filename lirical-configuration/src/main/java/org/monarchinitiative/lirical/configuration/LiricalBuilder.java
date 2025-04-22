package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.LiricalOptions;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserMvStoreMetadataServiceFactory;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserResources;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.LiricalDataResolver;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.io.service.JannovarFunctionalVariantAnnotatorService;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LiricalBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalBuilder.class);
    private static final Properties PROPERTIES = readProperties();
    private static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "UNKNOWN VERSION");

    private final Path dataDirectory;
    private final LiricalDataResolver liricalDataResolver;
    private final Map<GenomeBuild, ExomiserResources> exomiserResources = new HashMap<>(2);
    private PhenotypeService phenotypeService = null;
    private BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory = null;

    private VariantMetadataServiceFactory variantMetadataServiceFactory = null;
    private FunctionalVariantAnnotatorService functionalVariantAnnotatorService = null;

    private int parallelism = 1;

    public static LiricalBuilder builder(Path liricalDataDirectory) throws LiricalDataException {
        return new LiricalBuilder(liricalDataDirectory);
    }

    private LiricalBuilder(Path dataDirectory) throws LiricalDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
        this.liricalDataResolver = LiricalDataResolver.of(dataDirectory);
    }

    /**
     * Set path to exomiser variant database for given {@link GenomeBuild}.
     *
     * @deprecated use {@link #exomiserResources(GenomeBuild, ExomiserResources)} instead
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.3")
    public LiricalBuilder exomiserVariantDbPath(GenomeBuild genomeBuild, Path exomiserVariantDatabase) {
        if (genomeBuild == null) {
            LOGGER.warn("Genome build must not be null: {}", exomiserVariantDatabase);
            return this;
        }
        this.exomiserResources.put(genomeBuild, new ExomiserResources(exomiserVariantDatabase, null));
        return this;
    }

    public LiricalBuilder clearExomiserVariantDatabaseForGenomeBuild(GenomeBuild genomeBuild) {
        if (genomeBuild == null) {
            LOGGER.warn("Cannot clear database for null genome build!");
            return this;
        }
        this.exomiserResources.remove(genomeBuild);
        return this;
    }

    /**
     * Add paths for Exomiser resources required to support the {@code genomeBuild}.
     */
    public LiricalBuilder exomiserResources(GenomeBuild genomeBuild, ExomiserResources resources) {
        if (genomeBuild == null) {
            LOGGER.warn("Genome build must not be null: {}", resources);
            return this;
        }
        this.exomiserResources.put(genomeBuild, resources);
        return this;
    }

    public LiricalBuilder backgroundVariantFrequencyServiceFactory(BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory) {
        this.backgroundVariantFrequencyServiceFactory = backgroundVariantFrequencyServiceFactory;
        return this;
    }

    public LiricalBuilder phenotypeService(PhenotypeService phenotypeService) {
        this.phenotypeService = phenotypeService;
        return this;
    }

    public LiricalBuilder variantMetadataServiceFactory(VariantMetadataServiceFactory variantMetadataServiceFactory) {
        this.variantMetadataServiceFactory = variantMetadataServiceFactory;
        return this;
    }

    /**
     * Set the number threads/workers in the LIRICAL worker pool.
     */
    public LiricalBuilder parallelism(int parallelism) {
        if (parallelism <= 0)
            throw new IllegalArgumentException("Parallelism %d must be greater than 0".formatted(parallelism));
        this.parallelism = parallelism;
        return this;
    }

    public Lirical build() throws LiricalDataException {
        // First, services
        if (phenotypeService == null) {
            HpoDiseaseLoaderOptions diseaseLoaderOptions = HpoDiseaseLoaderOptions.of(DiseaseDatabase.allKnownDiseaseDatabases(), true, HpoDiseaseLoaderOptions.DEFAULT_COHORT_SIZE);
            phenotypeService = configurePhenotypeService(dataDirectory, diseaseLoaderOptions);
        }

        if (backgroundVariantFrequencyServiceFactory == null) {
            LOGGER.debug("Using bundled variant background frequencies");
            backgroundVariantFrequencyServiceFactory = BundledBackgroundVariantFrequencyServiceFactory.getInstance();
        }

        if (functionalVariantAnnotatorService == null) {
            LOGGER.debug("Functional variant annotator service is unset. Creating the service using resources in {}.", liricalDataResolver.dataDirectory().toAbsolutePath());
            functionalVariantAnnotatorService = JannovarFunctionalVariantAnnotatorService.of(liricalDataResolver, phenotypeService.associationData().getGeneIdentifiers());
        }

        // VariantMetadataService and VariantParserFactory.
        VariantParserFactory variantParserFactory;
        if (this.variantMetadataServiceFactory == null) {
            LOGGER.debug("Variant metadata service is unset.");
            if (exomiserResources.isEmpty()) {
                LOGGER.debug("Exomiser resources are unset. Variants will not be annotated.");
                this.variantMetadataServiceFactory = VariantMetadataServiceFactory.noOpFactory();
                variantParserFactory = VariantParserFactory.noOpFactory();
            } else {
                String summary = exomiserResources.entrySet().stream()
                        .map(e -> "%s -> alleles=%s, clinvar=%s".formatted(e.getKey(), e.getValue().exomiserAlleleDb(), e.getValue().exomiserClinvarDb()))
                        .collect(Collectors.joining(", ", "{", "}"));
                LOGGER.debug("Using Exomiser variant databases: {}", summary);
                this.variantMetadataServiceFactory = ExomiserMvStoreMetadataServiceFactory.fromResources(exomiserResources);
                variantParserFactory = VcfVariantParserFactory.of(functionalVariantAnnotatorService, variantMetadataServiceFactory);
            }
        } else {
            variantParserFactory = VcfVariantParserFactory.of(functionalVariantAnnotatorService, variantMetadataServiceFactory);
        }

        // Analysis result writer factory
        AnalysisResultWriterFactory analysisResultWriterFactory = new AnalysisResultWriterFactoryImpl(phenotypeService.hpo(), phenotypeService.diseases());

        // Last, the global options.
        LiricalOptions options = new LiricalOptions(LIRICAL_VERSION, parallelism);

        return Lirical.of(
                variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataServiceFactory,
                functionalVariantAnnotatorService,
                analysisResultWriterFactory,
                options);
    }

    private static PhenotypeService configurePhenotypeService(Path dataDirectory, HpoDiseaseLoaderOptions options) throws LiricalDataException {
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(dataDirectory);
        MinimalOntology hpo = LoadUtils.loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = LoadUtils.loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo, options);
        HpoAssociationData associationData = HpoAssociationData.builder(hpo)
                .hgncCompleteSetArchive(liricalDataResolver.hgncCompleteSet())
                .mim2GeneMedgen(liricalDataResolver.mim2geneMedgen())
                .hpoDiseases(diseases)
                .build();
        return PhenotypeService.of(hpo, diseases, associationData);
    }


    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = LiricalBuilder.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }
}
