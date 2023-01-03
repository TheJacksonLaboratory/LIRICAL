package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserMvStoreMetadataServiceFactory;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.LiricalDataResolver;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.io.service.JannovarFunctionalVariantAnnotatorService;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
    private final Map<GenomeBuild, Path> exomiserVariantDatabasePaths = new HashMap<>(2);
    private PhenotypeService phenotypeService = null;
    private BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory = null;

    private VariantMetadataServiceFactory variantMetadataServiceFactory = null;
    private FunctionalVariantAnnotatorService functionalVariantAnnotatorService = null;

    public static LiricalBuilder builder(Path liricalDataDirectory) throws LiricalDataException {
        return new LiricalBuilder(liricalDataDirectory);
    }

    private LiricalBuilder(Path dataDirectory) throws LiricalDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
        this.liricalDataResolver = LiricalDataResolver.of(dataDirectory);
    }

    /**
     * @deprecated use {@link #exomiserVariantDbPath(GenomeBuild, Path)} instead.
     */
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    public LiricalBuilder exomiserVariantDatabase(Path exomiserVariantDatabase) {
        LOGGER.warn("Setting path to Exomiser database has been deprecated. Use `exomiserVariantDbPath(GenomeBuild genomeBuild, Path exomiserVariantDatabase)` to set path to database for a genome build!");
        return this;
    }

    /**
     * Set path to exomiser variant database for given {@link GenomeBuild}.
     * @return the builder
     */
    public LiricalBuilder exomiserVariantDbPath(GenomeBuild genomeBuild, Path exomiserVariantDatabase) {
        if (genomeBuild == null) {
            LOGGER.warn("Genome build must not be null: {}", exomiserVariantDatabase);
            return this;
        }
        this.exomiserVariantDatabasePaths.put(genomeBuild, exomiserVariantDatabase);
        return this;
    }

    public LiricalBuilder clearExomiserVariantDatabaseForGenomeBuild(GenomeBuild genomeBuild) {
        if (genomeBuild == null) {
            LOGGER.warn("Cannot clear database for null genome build!");
            return this;
        }
        this.exomiserVariantDatabasePaths.remove(genomeBuild);
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder genomeBuild(GenomeBuild genomeBuild) {
        LOGGER.warn("Setting genome build has been deprecated. Set the desired genome build via AnalysisOptions!");
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder backgroundVariantFrequency(Path backgroundVariantFrequency) {
        LOGGER.warn("Setting path to background variant frequency has been deprecated! Set backgroundVariantFrequencyServiceFactory instead!");
        return this;
    }

    public LiricalBuilder backgroundVariantFrequencyServiceFactory(BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory) {
        this.backgroundVariantFrequencyServiceFactory = backgroundVariantFrequencyServiceFactory;
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder transcriptDatabase(TranscriptDatabase transcriptDatabase) {
        LOGGER.warn("Setting transcript database has been deprecated. Set the desired database build via AnalysisOptions!");
        return this;
    }

    /**
     * @param defaultVariantAlleleFrequency default variant allele frequency to set.
     *                                      The frequency is only used if
     *                                      {@link #variantMetadataService(VariantMetadataService)} is unset.
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder defaultVariantAlleleFrequency(float defaultVariantAlleleFrequency) {
        LOGGER.warn("Setting default variant allele frequency has been deprecated. Set the desired value via AnalysisOptions!");
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder genotypeLrProperties(GenotypeLrProperties genotypeLrProperties) {
        LOGGER.warn("Setting genotype LR properties has been deprecated. Set the desired value via AnalysisOptions!");
        return this;
    }

    public LiricalBuilder phenotypeService(PhenotypeService phenotypeService) {
        this.phenotypeService = phenotypeService;
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder clearDiseaseDatabases() {
        LOGGER.warn("Setting disease databases has been deprecated. Set the desired disease databases via AnalysisOptions");
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder addDiseaseDatabases(DiseaseDatabase... diseaseDatabases) {
        return addDiseaseDatabases(Arrays.asList(diseaseDatabases));
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder addDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
        LOGGER.warn("Setting disease databases has been deprecated. Set the desired disease databases via AnalysisOptions");
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder setDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
        LOGGER.warn("Setting disease databases has been deprecated. Set the desired disease databases via AnalysisOptions");
        return this;
    }

    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder phenotypeLikelihoodRatio(PhenotypeLikelihoodRatio phenotypeLikelihoodRatio) {
        LOGGER.warn("Setting phenotype LR has been deprecated. Set the desired value via AnalysisOptions!");
        return this;
    }

    /**
     * @deprecated the option does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder genotypeLikelihoodRatio(GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        LOGGER.warn("Setting genotype LR has been deprecated. Set the desired value via AnalysisOptions!");
        return this;
    }

    /**
     * @deprecated pretest probability does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-SNAPSHOT")
    public LiricalBuilder pretestDiseaseProbability(PretestDiseaseProbability pretestDiseaseProbability) {
        LOGGER.warn("Setting pretest disease probability has been deprecated. Set the desired value via AnalysisOptions!");
        return this;
    }

    /**
     * @deprecated setting variant metadata service has been deprecated.
     * Use {@link #variantMetadataServiceFactory(VariantMetadataServiceFactory)} instead.
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    public LiricalBuilder variantMetadataService(VariantMetadataService variantMetadataService) {
        LOGGER.warn("Setting variant metadata service has been deprecated.");
        return this;
    }

    public LiricalBuilder variantMetadataServiceFactory(VariantMetadataServiceFactory variantMetadataServiceFactory) {
        this.variantMetadataServiceFactory = variantMetadataServiceFactory;
        return this;
    }

    /**
     * @deprecated functional variant annotation configuration does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-SNAPSHOT")
    public LiricalBuilder functionalVariantAnnotator(FunctionalVariantAnnotator functionalVariantAnnotator) {
        LOGGER.warn("Setting functional variant annotator has been deprecated.");
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
            if (exomiserVariantDatabasePaths.isEmpty()) {
                LOGGER.debug("Path to Exomiser database is unset. Variants will not be annotated.");
                this.variantMetadataServiceFactory = VariantMetadataServiceFactory.noOpFactory();
                variantParserFactory = null;
            } else {
                String summary = exomiserVariantDatabasePaths.entrySet().stream()
                        .map(e -> "%s -> %s".formatted(e.getKey(), e.getValue().toAbsolutePath()))
                        .collect(Collectors.joining(", ", "{", "}"));
                LOGGER.debug("Using Exomiser variant databases: {}", summary);
                this.variantMetadataServiceFactory = ExomiserMvStoreMetadataServiceFactory.of(exomiserVariantDatabasePaths);
                variantParserFactory = VcfVariantParserFactory.of(functionalVariantAnnotatorService, variantMetadataServiceFactory);
            }
        } else {
            variantParserFactory = VcfVariantParserFactory.of(functionalVariantAnnotatorService, variantMetadataServiceFactory);
        }

        // Analysis result writer factory
        AnalysisResultWriterFactory analysisResultWriterFactory = new AnalysisResultWriterFactoryImpl(phenotypeService.hpo(), phenotypeService.diseases());

        return Lirical.of(
                variantParserFactory, // nullable
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataServiceFactory,
                analysisResultWriterFactory,
                LIRICAL_VERSION);
    }

    private static PhenotypeService configurePhenotypeService(Path dataDirectory, HpoDiseaseLoaderOptions options) throws LiricalDataException {
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(dataDirectory);
        Ontology hpo = LoadUtils.loadOntology(liricalDataResolver.hpoJson());
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
