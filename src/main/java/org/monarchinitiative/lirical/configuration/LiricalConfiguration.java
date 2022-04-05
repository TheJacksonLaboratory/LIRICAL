package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.io.*;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParserFactory;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.model.GenomeBuild;
import org.monarchinitiative.lirical.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.service.ExomiserVariantMetadataService;
import org.monarchinitiative.lirical.service.NoOpVariantMetadataService;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.lirical.service.VariantMetadataService;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationLoader;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseAnnotationLoader;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        this.assembly = parseExomiserAssembly(properties.genomeBuild());

        GenomicAssembly genomicAssembly = parseSvartGenomicAssembly(properties.genomeBuild());
        LiricalDataResolver liricalDataResolver = LiricalDataResolver.of(properties.liricalDataDirectory());

        VariantParserFactory variantParserFactory;
        if (properties.exomiserDataDirectory().isPresent()) {
            VariantMetadataService variantMetadataService = createVariantMetadataService(properties, new VariantMetadataService.Options(properties.defaultVariantFrequency()));
            variantParserFactory = new VcfVariantParserFactory(genomicAssembly, variantMetadataService);
        } else {
            LOGGER.info("Path to Exomiser data is unset, variants will not be included into the analysis");
            variantParserFactory = null;
        }



        Ontology hpo = loadOntology(liricalDataResolver.hpoJson());
        HpoDiseases diseases = loadHpoDiseases(liricalDataResolver.phenotypeAnnotations(), hpo);
        HpoAssociationData associationData = loadAssociationData(hpo, liricalDataResolver.homoSapiensGeneInfo(), liricalDataResolver.mim2geneMedgen(), liricalDataResolver.phenotypeAnnotations());
        PhenotypeService phenotypeService = PhenotypeService.of(hpo, diseases, associationData);


        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(phenotypeService.diseases());
        LiricalAnalysisRunner liricalAnalysisRunner = createLiricalAnalyzer(phenotypeService, pretestDiseaseProbability);

        AnalysisResultWriterFactory analysisResultWriterFactory = new AnalysisResultWriterFactory(phenotypeService, properties);

        this.lirical = Lirical.of(variantParserFactory, phenotypeService, liricalAnalysisRunner, analysisResultWriterFactory);
    }

    private static GenomeAssembly parseExomiserAssembly(GenomeBuild build) {
        switch (build) {
            case HG19:
                LOGGER.debug("Using GRCh37 assembly");
                return GenomeAssembly.HG19;
            default:
                LOGGER.warn("Unknown assembly {}, falling back to GRCh38", build);
            case HG38:
                LOGGER.debug("Using GRCh38 assembly");
                return GenomeAssembly.HG38;
        }
    }

    private static GenomicAssembly parseSvartGenomicAssembly(GenomeBuild genomeAssembly) {
        switch (genomeAssembly) {
            case HG19:
                return GenomicAssemblies.GRCh37p13();
            default:
                LOGGER.warn("Unknown genome assembly {}. Falling back to GRCh38", genomeAssembly);
            case HG38:
                return GenomicAssemblies.GRCh38p13();
        }
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


    private static Ontology loadOntology(Path ontologyPath) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO from {}", ontologyPath.toAbsolutePath());
            return OntologyLoader.loadOntology(ontologyPath.toFile());
        } catch (PhenolRuntimeException e) {
            throw new LiricalDataException(e);
        }
    }

    private HpoDiseases loadHpoDiseases(Path annotationPath, Ontology hpo) throws LiricalDataException {
        try {
            LOGGER.debug("Loading HPO annotations from {}", annotationPath.toAbsolutePath());
            return HpoDiseaseAnnotationLoader.loadHpoDiseases(annotationPath, hpo, properties.diseaseDatabases());
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    private HpoAssociationData loadAssociationData(Ontology hpo,
                                                   Path homoSapiensGeneInfo,
                                                   Path mim2geneMedgen,
                                                   Path phenotypeHpoa) throws LiricalDataException {
        try {
            return HpoAssociationLoader.loadHpoAssociationData(hpo,
                    homoSapiensGeneInfo,
                    mim2geneMedgen,
                    null,
                    phenotypeHpoa,
                    properties.diseaseDatabases());
        } catch (IOException e) {
            throw new LiricalDataException(e);
        }
    }

    private LiricalAnalysisRunner createLiricalAnalyzer(PhenotypeService phenotypeService,
                                                        PretestDiseaseProbability pretestDiseaseProbability) throws LiricalDataException {
        PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = new PhenotypeLikelihoodRatio(phenotypeService.hpo(), phenotypeService.diseases().diseaseById());
        GenotypeLikelihoodRatio genotypeLrEvaluator = createGenotypeLrEvaluator();

        return LiricalAnalysisRunnerImpl.of(phenotypeService,
                pretestDiseaseProbability,
                phenotypeLikelihoodRatio,
                genotypeLrEvaluator);
    }

    private GenotypeLikelihoodRatio createGenotypeLrEvaluator() throws LiricalDataException {
        LiricalProperties.GenotypeLrProperties gtLrProperties = properties.genotypeLrProperties();
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
            try (BufferedReader reader = openBundledBackgroundFrequencyFile(assembly)) {
                background = GenotypeDataIngestor.parse(reader);
            } catch (IOException e) {
                throw new LiricalDataException(e);
            }
        }
        return new GenotypeLikelihoodRatio(background, options);
    }

    private static BufferedReader openBundledBackgroundFrequencyFile(GenomeAssembly assembly) throws LiricalDataException {
        String name = switch (assembly) {
            case HG19 -> "/background/background-hg19.tsv";
            case HG38 -> "/background/background-hg38.tsv";
        };
        InputStream is = LiricalConfiguration.class.getResourceAsStream(name);
        if (is == null)
            throw new LiricalDataException("Background file for " + assembly + " is not present at '" + name + '\'');
        LOGGER.debug("Loading bundled background variant frequencies from {}", name);
        return new BufferedReader(new InputStreamReader(is));
    }

    public Lirical getLirical() {
        return lirical;
    }

}
