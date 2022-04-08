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
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.lirical.core.service.NoOpVariantMetadataService;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.*;
import org.monarchinitiative.lirical.io.service.ExomiserVariantMetadataService;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class LiricalConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalConfiguration.class);

    private final LiricalProperties properties;
    private final GenomeAssembly assembly;
    private final Lirical lirical;

    public static LiricalConfiguration of(LiricalProperties properties) throws LiricalDataException {
        return new LiricalConfiguration(properties);
    }

    private LiricalConfiguration(LiricalProperties properties) throws LiricalDataException {
        this.properties = Objects.requireNonNull(properties);
        this.assembly = LoadUtils.parseExomiserAssembly(properties.genomeBuild());

        GenomicAssembly genomicAssembly = LoadUtils.parseSvartGenomicAssembly(properties.genomeBuild());
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(properties.liricalDataDirectory());

        VariantParserFactory variantParserFactory;
        if (properties.exomiserDataDirectory().isPresent()) {
            VariantMetadataService variantMetadataService = createVariantMetadataService(properties, new VariantMetadataService.Options(properties.defaultVariantFrequency()));
            variantParserFactory = new VcfVariantParserFactory(genomicAssembly, variantMetadataService);
        } else {
            LOGGER.info("Path to Exomiser data is unset, variants will not be included into the analysis");
            variantParserFactory = null;
        }



        Ontology hpo = LoadUtils.loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = LoadUtils.loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo, properties.diseaseDatabases());
        HpoAssociationData associationData = LoadUtils.loadAssociationData(hpo, liricalDataResolver.homoSapiensGeneInfo(), liricalDataResolver.mim2geneMedgen(), liricalDataResolver.phenotypeAnnotations(), properties.diseaseDatabases());
        PhenotypeService phenotypeService = PhenotypeService.of(hpo, diseases, associationData);


        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(phenotypeService.diseases());
        LiricalAnalysisRunner liricalAnalysisRunner = createLiricalAnalyzer(phenotypeService, pretestDiseaseProbability);

        AnalysisResultWriterFactory analysisResultWriterFactory = new AnalysisResultWriterFactory(hpo, diseases);

        this.lirical = Lirical.of(variantParserFactory, phenotypeService, liricalAnalysisRunner, analysisResultWriterFactory);
    }

    private VariantMetadataService createVariantMetadataService(LiricalProperties properties,
                                                                VariantMetadataService.Options options) throws LiricalDataException {
        Optional<Path> exomiserDataDirectory = properties.exomiserDataDirectory();
        if (exomiserDataDirectory.isPresent()) {
            ExomiserDataResolver resolver = ExomiserDataResolver.of(exomiserDataDirectory.get());
            Optional<GenomeBuild> buildOptional = resolver.genomeBuild();
            if (buildOptional.isPresent()) {
                GenomeBuild build = buildOptional.get();
                if (!build.equals(properties.genomeBuild()))
                    LOGGER.warn("Genome build mismatch between the indicated genome build ({}) and genome build of the Exomiser resources ({})",
                            properties.genomeBuild(), build);
            }
            return ExomiserVariantMetadataService.of(resolver.mvStorePath(), resolver.refseqTranscriptCache(), assembly, options);
        } else {
            LOGGER.info("Exomiser data directory is not available, variant data will not be used");
            return NoOpVariantMetadataService.instance();
        }
    }

    private LiricalAnalysisRunner createLiricalAnalyzer(PhenotypeService phenotypeService,
                                                        PretestDiseaseProbability pretestDiseaseProbability) throws LiricalDataException {
        PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = new PhenotypeLikelihoodRatio(phenotypeService.hpo(), phenotypeService.diseases());
        GenotypeLikelihoodRatio genotypeLrEvaluator = createGenotypeLrEvaluator();

        return LiricalAnalysisRunnerImpl.of(phenotypeService,
                pretestDiseaseProbability,
                phenotypeLikelihoodRatio,
                genotypeLrEvaluator);
    }

    private GenotypeLikelihoodRatio createGenotypeLrEvaluator() throws LiricalDataException {
        GenotypeLrProperties gtLrProperties = properties.genotypeLrProperties();
        GenotypeLikelihoodRatio.Options options = new GenotypeLikelihoodRatio.Options(gtLrProperties.pathogenicityThreshold(), gtLrProperties.strict());

        Map<TermId, Double> background;
        Optional<Path> backgroundFreqFileOptional = properties.backgroundFrequencyFile();
        if (backgroundFreqFileOptional.isPresent()) {
            Path backgroundFrequencyPath = backgroundFreqFileOptional.get();
            LOGGER.debug("Loading background variant frequencies from {}", backgroundFrequencyPath.toAbsolutePath());
            try (BufferedReader reader = Files.newBufferedReader(backgroundFrequencyPath)) {
                background = GenotypeDataIngestor.parse(reader);
            } catch (IOException e) {
                throw new LiricalDataException(e);
            }
        } else {
            try (BufferedReader reader = LoadUtils.openBundledBackgroundFrequencyFile(properties.genomeBuild())) {
                background = GenotypeDataIngestor.parse(reader);
            } catch (IOException e) {
                throw new LiricalDataException(e);
            }
        }
        BackgroundVariantFrequencyService backgroundVariantFrequencyService = BackgroundVariantFrequencyService.of(background, gtLrProperties.defaultVariantFrequency());
        return new GenotypeLikelihoodRatio(backgroundVariantFrequencyService, options);
    }

    public Lirical getLirical() {
        return lirical;
    }

}
