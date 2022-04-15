package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;
import org.monarchinitiative.lirical.io.*;
import org.monarchinitiative.lirical.io.service.ExomiserVariantMetadataService;
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
    private GenomeBuild genomeBuild = GenomeBuild.HG38;
    private Path exomiserDataDirectory = null;
    private Path backgroundVariantFrequency = null;

    private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;
    private float defaultVariantAlleleFrequency = VariantMetadataService.DEFAULT_FREQUENCY;

    private PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = null;
    private GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(.8f, .1, false);
    private GenotypeLikelihoodRatio genotypeLikelihoodRatio = null;

    private PretestDiseaseProbability pretestDiseaseProbability = null;

    private final Set<DiseaseDatabase> diseaseDatabases = new HashSet<>(Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER));
    private PhenotypeService phenotypeService = null;

    private VariantMetadataService variantMetadataService = null;
    private FunctionalVariantAnnotator functionalVariantAnnotator = null;
    private VariantFrequencyService variantFrequencyService = null;
    private VariantPathogenicityService variantPathogenicityService = null;

    public static LiricalBuilder builder(Path liricalDataDirectory) {
        return new LiricalBuilder(liricalDataDirectory);
    }

    private LiricalBuilder(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory);
    }

    public LiricalBuilder exomiserDataDirectory(Path exomiserDataDirectory) {
        this.exomiserDataDirectory = exomiserDataDirectory;
        return this;
    }

    public LiricalBuilder genomeBuild(GenomeBuild genomeBuild) {
        if (genomeBuild == null) {
            LOGGER.warn("Cannot set genome build to null");
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

    public LiricalBuilder defaultVariantAlleleFrequency(float defaultVariantAlleleFrequency) {
        this.defaultVariantAlleleFrequency = defaultVariantAlleleFrequency;
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

    public LiricalBuilder useDiseaseDatabases(DiseaseDatabase... diseaseDatabases) {
        return useDiseaseDatabases(Arrays.asList(diseaseDatabases));
    }

    public LiricalBuilder useDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
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

    public LiricalBuilder variantFrequencyService(VariantFrequencyService variantFrequencyService) {
        this.variantFrequencyService = variantFrequencyService;
        return this;
    }

    public LiricalBuilder variantPathogenicityService(VariantPathogenicityService variantPathogenicityService) {
        this.variantPathogenicityService = variantPathogenicityService;
        return this;
    }

    public Lirical build() throws LiricalDataException {
        // First, services
        if (phenotypeService == null) {
            HpoDiseaseLoaderOptions diseaseLoaderOptions = HpoDiseaseLoaderOptions.of(diseaseDatabases, true, HpoDiseaseLoaderOptions.DEFAULT_COHORT_SIZE);
            phenotypeService = configurePhenotypeService(dataDirectory, diseaseLoaderOptions);
        }

        if (variantMetadataService == null) {
            LOGGER.debug("Variant metadata service is unset. Trying to create the service from variant annotator, frequency service, and pathogenicity service.");
            variantMetadataService = configureVariantMetadataService();
        }

        // Variant parser factory
        GenomicAssembly genomicAssembly = LoadUtils.parseSvartGenomicAssembly(genomeBuild);
        VariantParserFactory variantParserFactory = VcfVariantParserFactory.of(genomicAssembly, variantMetadataService);


        // Lirical analysis runner
        if (pretestDiseaseProbability == null) {
            LOGGER.debug("Using uniform pretest disease probabilities");
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

        return Lirical.of(variantParserFactory, phenotypeService, analyzer, analysisResultWriterFactory);
    }

    private VariantMetadataService configureVariantMetadataService() throws LiricalDataException {
        ExomiserVariantMetadataService exomiserVariantMetadataService = null;
        if (functionalVariantAnnotator == null) {
            if (exomiserDataDirectory != null) {
                exomiserVariantMetadataService = loadExomiserVariantMetadataService(exomiserDataDirectory);
                functionalVariantAnnotator = exomiserVariantMetadataService;
            } else {
                // Right now we cannot do anything else but fail
                throw new LiricalDataException("Functional variant annotator must not be null");
            }
        } else {
            LOGGER.debug("Found variant functional annotator.");
        }
        if (variantFrequencyService == null) {
            if (exomiserDataDirectory != null) {
                if (exomiserVariantMetadataService == null)
                    exomiserVariantMetadataService = loadExomiserVariantMetadataService(exomiserDataDirectory);
                variantFrequencyService = exomiserVariantMetadataService;
            } else {
                // Right now we cannot do anything else but fail
                throw new LiricalDataException("Variant frequency service must not be null");
            }
        } else {
            LOGGER.debug("Found variant frequency service.");
        }

        if (variantPathogenicityService == null) {
            if (exomiserDataDirectory != null) {
                if (exomiserVariantMetadataService == null)
                    exomiserVariantMetadataService = loadExomiserVariantMetadataService(exomiserDataDirectory);
                variantPathogenicityService = exomiserVariantMetadataService;
            } else {
                // Right now we cannot do anything else but fail
                throw new LiricalDataException("Variant pathogenicity service must not be null");
            }
        } else {
            LOGGER.debug("Found variant pathogenicity service.");
        }

        return CompoundVariantMetadataService.of(functionalVariantAnnotator, variantFrequencyService, variantPathogenicityService);
    }

    private ExomiserVariantMetadataService loadExomiserVariantMetadataService(Path exomiserDataDirectory) throws LiricalDataException {
        ExomiserDataResolver resolver = ExomiserDataResolver.of(exomiserDataDirectory);
        Path transcriptCache = resolver.transcriptCacheForTranscript(transcriptDatabase);
        GenomeAssembly genomeAssembly = LoadUtils.parseExomiserAssembly(genomeBuild);
        VariantMetadataService.Options options = new VariantMetadataService.Options(defaultVariantAlleleFrequency);
        return ExomiserVariantMetadataService.of(resolver.mvStorePath(), transcriptCache, genomeAssembly, options);
    }

    private static PhenotypeService configurePhenotypeService(Path dataDirectory, HpoDiseaseLoaderOptions options) throws LiricalDataException {
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(dataDirectory);
        Ontology hpo = LoadUtils.loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = LoadUtils.loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo, options);
        HpoAssociationData associationData = LoadUtils.loadAssociationData(hpo, liricalDataResolver.homoSapiensGeneInfo(), liricalDataResolver.mim2geneMedgen(), diseases);
        return PhenotypeService.of(hpo, diseases, associationData);
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
