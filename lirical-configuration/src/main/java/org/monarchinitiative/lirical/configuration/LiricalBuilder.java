package org.monarchinitiative.lirical.configuration;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserMvStoreMetadataService;
import org.monarchinitiative.lirical.io.GenotypeDataIngestor;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.LiricalDataResolver;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.io.service.JannovarFunctionalVariantAnnotator;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LiricalBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalBuilder.class);

    private final Path dataDirectory;
    private final LiricalDataResolver liricalDataResolver;
    private final Set<DiseaseDatabase> diseaseDatabases = new HashSet<>(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER));
    private GenomeBuild genomeBuild = GenomeBuild.HG38;
    private Path exomiserVariantDatabase = null;
    private Path backgroundVariantFrequency = null;
    private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;
    private float defaultVariantAlleleFrequency = VariantMetadataService.DEFAULT_FREQUENCY;
    private GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(.8f, .1, false);
    private PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = null;
    private GenotypeLikelihoodRatio genotypeLikelihoodRatio = null;
    private PretestDiseaseProbability pretestDiseaseProbability = null;
    private PhenotypeService phenotypeService = null;

    private VariantMetadataService variantMetadataService = null;
    private FunctionalVariantAnnotator functionalVariantAnnotator = null;

    public static LiricalBuilder builder(Path liricalDataDirectory) throws LiricalDataException {
        return new LiricalBuilder(liricalDataDirectory);
    }

    private LiricalBuilder(Path dataDirectory) throws LiricalDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
        this.liricalDataResolver = LiricalDataResolver.of(dataDirectory);
    }

    public LiricalBuilder exomiserVariantDatabase(Path exomiserVariantDatabase) {
        this.exomiserVariantDatabase = exomiserVariantDatabase;
        return this;
    }

    public LiricalBuilder genomeBuild(GenomeBuild genomeBuild) {
        if (genomeBuild == null) {
            LOGGER.warn("Cannot set genome build to null. Retaining {}", this.genomeBuild);
            return this;
        }
        this.genomeBuild = genomeBuild;
        return this;
    }

    public LiricalBuilder backgroundVariantFrequency(Path backgroundVariantFrequency) {
        this.backgroundVariantFrequency = backgroundVariantFrequency;
        return this;
    }

    public LiricalBuilder transcriptDatabase(TranscriptDatabase transcriptDatabase) {
        if (transcriptDatabase == null) {
            LOGGER.warn("Cannot set transcript database to null");
            return this;
        }
        this.transcriptDatabase = transcriptDatabase;
        return this;
    }

    /**
     * @param defaultVariantAlleleFrequency default variant allele frequency to set.
     *                                      The frequency is only used if
     *                                      {@link #variantMetadataService(VariantMetadataService)} is unset.
     */
    public LiricalBuilder defaultVariantAlleleFrequency(float defaultVariantAlleleFrequency) {
        this.defaultVariantAlleleFrequency = defaultVariantAlleleFrequency;
        return this;
    }

    public LiricalBuilder genotypeLrProperties(GenotypeLrProperties genotypeLrProperties) {
        if (genotypeLrProperties == null) {
            LOGGER.warn("Cannot set genotype likelihood ratio properties to null");
            return this;
        }
        this.genotypeLrProperties = genotypeLrProperties;
        return this;
    }

    public LiricalBuilder phenotypeService(PhenotypeService phenotypeService) {
        this.phenotypeService = phenotypeService;
        return this;
    }

    public LiricalBuilder clearDiseaseDatabases() {
        this.diseaseDatabases.clear();
        return this;
    }

    public LiricalBuilder addDiseaseDatabases(DiseaseDatabase... diseaseDatabases) {
        return addDiseaseDatabases(Arrays.asList(diseaseDatabases));
    }

    public LiricalBuilder addDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
        if (diseaseDatabases == null) {
            LOGGER.warn("Disease databases should not be null!");
            return this;
        }
        this.diseaseDatabases.addAll(diseaseDatabases);
        return this;
    }

    public LiricalBuilder setDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
        if (diseaseDatabases == null) {
            LOGGER.warn("Disease databases should not be null!");
            return this;
        }
        this.diseaseDatabases.clear();
        this.diseaseDatabases.addAll(diseaseDatabases);
        return this;
    }

    public LiricalBuilder phenotypeLikelihoodRatio(PhenotypeLikelihoodRatio phenotypeLikelihoodRatio) {
        this.phenotypeLikelihoodRatio = phenotypeLikelihoodRatio;
        return this;
    }

    public LiricalBuilder genotypeLikelihoodRatio(GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        this.genotypeLikelihoodRatio = genotypeLikelihoodRatio;
        return this;
    }

    /**
     * @deprecated pretest probability does not belong to the global configuration but to per-sample config (to be removed in v2.0.0).
     * @return the builder
     */
    @Deprecated(forRemoval = true, since = "2.0.0-SNAPSHOT")
    public LiricalBuilder pretestDiseaseProbability(PretestDiseaseProbability pretestDiseaseProbability) {
        this.pretestDiseaseProbability = pretestDiseaseProbability;
        return this;
    }

    public LiricalBuilder variantMetadataService(VariantMetadataService variantMetadataService) {
        this.variantMetadataService = variantMetadataService;
        return this;
    }

    public LiricalBuilder functionalVariantAnnotator(FunctionalVariantAnnotator functionalVariantAnnotator) {
        this.functionalVariantAnnotator = functionalVariantAnnotator;
        return this;
    }

    public Lirical build() throws LiricalDataException {
        // First, services
        if (phenotypeService == null) {
            HpoDiseaseLoaderOptions diseaseLoaderOptions = HpoDiseaseLoaderOptions.of(diseaseDatabases, true, HpoDiseaseLoaderOptions.DEFAULT_COHORT_SIZE);
            phenotypeService = configurePhenotypeService(dataDirectory, diseaseLoaderOptions);
        }

        if (functionalVariantAnnotator == null) {
            LOGGER.debug("Functional variant annotator is unset. Loading Jannovar transcript database for {} transcripts.", transcriptDatabase);
            JannovarData jannovarData = loadJannovarData(liricalDataResolver, genomeBuild, transcriptDatabase);
            functionalVariantAnnotator = JannovarFunctionalVariantAnnotator.of(jannovarData, phenotypeService.associationData().getGeneIdentifiers());
        }

        if (variantMetadataService == null) {
            LOGGER.debug("Variant metadata service is unset. Trying to create the service from frequency service and pathogenicity service.");
            variantMetadataService = ExomiserMvStoreMetadataService.of(exomiserVariantDatabase, new VariantMetadataService.Options(defaultVariantAlleleFrequency));
        }

        // Variant parser factory
        GenomicAssembly genomicAssembly = LoadUtils.parseSvartGenomicAssembly(genomeBuild);
        VariantParserFactory variantParserFactory = VcfVariantParserFactory.of(genomicAssembly, functionalVariantAnnotator, variantMetadataService);


        // Lirical analysis runner
        if (pretestDiseaseProbability == null) {
            // TODO - remove
            LOGGER.debug("Using uniform pretest disease probabilities.");
            pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(phenotypeService.diseases());
        }

        if (phenotypeLikelihoodRatio == null) {
            phenotypeLikelihoodRatio = new PhenotypeLikelihoodRatio(phenotypeService.hpo(), phenotypeService.diseases());
        }

        if (genotypeLikelihoodRatio == null)
            genotypeLikelihoodRatio = configureGenotypeLikelihoodRatio(backgroundVariantFrequency, genomeBuild, genotypeLrProperties);

        LiricalAnalysisRunner analyzer = LiricalAnalysisRunnerImpl.of(phenotypeService, pretestDiseaseProbability, phenotypeLikelihoodRatio, genotypeLikelihoodRatio);

        // Analysis result writer factory
        AnalysisResultWriterFactory analysisResultWriterFactory = new AnalysisResultWriterFactory(phenotypeService.hpo(), phenotypeService.diseases());

        return Lirical.of(variantParserFactory,
                phenotypeService,
                functionalVariantAnnotator,
                variantMetadataService,
                analyzer,
                analysisResultWriterFactory);
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

    private static JannovarData loadJannovarData(LiricalDataResolver liricalDataResolver,
                                                 GenomeBuild genomeBuild,
                                                 TranscriptDatabase transcriptDatabase) throws LiricalDataException {
        Path txDatabasePath = liricalDataResolver.transcriptCacheFor(genomeBuild, transcriptDatabase);
        LOGGER.info("Loading transcript database from {}", txDatabasePath.toAbsolutePath());
        try {
            return new JannovarDataSerializer(txDatabasePath.toAbsolutePath().toString()).load();
        } catch (SerializationException e) {
            throw new LiricalDataException(e);
        }
    }

    private static GenotypeLikelihoodRatio configureGenotypeLikelihoodRatio(Path backgroundVariantFrequency, GenomeBuild genomeBuild, GenotypeLrProperties genotypeLrProperties) throws LiricalDataException {
        BackgroundVariantFrequencyService backgroundVariantFrequencyService;
        try (BufferedReader br = backgroundVariantFrequency == null
                ? LoadUtils.openBundledBackgroundFrequencyFile(genomeBuild)
                : Files.newBufferedReader(backgroundVariantFrequency)) {
            Map<TermId, Double> frequencyMap = GenotypeDataIngestor.parse(br);
            backgroundVariantFrequencyService = BackgroundVariantFrequencyService.of(frequencyMap, genotypeLrProperties.defaultVariantFrequency());
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
        GenotypeLikelihoodRatio.Options options = new GenotypeLikelihoodRatio.Options(genotypeLrProperties.pathogenicityThreshold(), genotypeLrProperties.strict());
        return new GenotypeLikelihoodRatio(backgroundVariantFrequencyService, options);
    }


}
