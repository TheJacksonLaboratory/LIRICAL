package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.configuration.GenotypeLrProperties;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.analysis.ProgressReporter;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Base class that describes data and configuration sections of the CLI, and contains common functionalities.
 */
abstract class BaseLiricalCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLiricalCommand.class);
    private static final Properties PROPERTIES = readProperties();
    protected static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "unknown version");

    private static final String LIRICAL_BANNER = readBanner();

    private static String readBanner() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(BaseLiricalCommand.class.getResourceAsStream("/banner.txt"), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            // swallow
            return "";
        }
    }

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                description = "Path to the Exomiser variant database.")
        public Path exomiserDatabase = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        public Path backgroundFrequencyFile = null;
    }


    // ---------------------------------------------- CONFIGURATION ----------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Configuration options:%n")
    public RunConfiguration runConfiguration = new RunConfiguration();

    public static class RunConfiguration {
        /**
         * If global is set to true, then LIRICAL will not discard candidate diseases with no known disease gene or
         * candidates for which no predicted pathogenic variant was found in the VCF.
         */
        @CommandLine.Option(names = {"-g", "--global"},
                description = "Global analysis (default: ${DEFAULT-VALUE}).")
        public boolean globalAnalysisMode = false;

        @CommandLine.Option(names = {"--ddndv"},
                description = "Disregard a disease if no deleterious variants are found in the gene associated with the disease. "
                        + "Used only if running with a VCF file. (default: ${DEFAULT-VALUE})")
        public boolean disregardDiseaseWithNoDeleteriousVariants = true;

        @CommandLine.Option(names = {"--transcript-db"},
                paramLabel = "{REFSEQ,UCSC}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        public TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        public boolean useOrphanet = false;

        @CommandLine.Option(names = {"--strict"},
                // TODO - add better description
                description = "Strict mode (default: ${DEFAULT-VALUE}).")
        public boolean strict = false;

        /* Default frequency of called-pathogenic variants in the general population (gnomAD). In the vast majority of
         * cases, we can derive this information from gnomAD. This constant is used if for whatever reason,
         * data was not available.
         */
        @CommandLine.Option(names = {"--variant-background-frequency"},
                // TODO - add better description
                description = "Default background frequency of variants in a gene (default: ${DEFAULT-VALUE}).")
        public double defaultVariantBackgroundFrequency = 0.1;

        @CommandLine.Option(names = {"--pathogenicity-threshold"},
                description = "Variant with greater pathogenicity score is considered deleterious (default: ${DEFAULT-VALUE}).")
        public float pathogenicityThreshold = .8f;

        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = "Variant with greater allele frequency in at least one population is considered common (default: ${DEFAULT-VALUE}).")
        public float defaultAlleleFrequency = 1E-5f;
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = BaseLiricalCommand.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    protected static void printBanner() {
        System.out.println(LIRICAL_BANNER);
    }

    protected List<String> checkInput() {
        List<String> errors = new LinkedList<>();
        // resources
        if (dataSection.liricalDataDirectory == null) {
            String msg = "Path to Lirical data directory must be provided via `-d | --data` option";
            LOGGER.error(msg);
            errors.add(msg);
        }
        return errors;
    }

    /**
     * Build {@link Lirical} based on {@link #dataSection} and {@link #runConfiguration} sections.
     */
    protected Lirical bootstrapLirical() throws LiricalDataException {
        LOGGER.info("Spooling up Lirical v{}", LIRICAL_VERSION);
        GenomeBuild genomeBuild = parseGenomeBuild(getGenomeBuild());

        GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(runConfiguration.pathogenicityThreshold, runConfiguration.defaultVariantBackgroundFrequency, runConfiguration.strict);
        return LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .exomiserVariantDatabase(dataSection.exomiserDatabase)
                .genomeBuild(genomeBuild)
                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
                .setDiseaseDatabases(runConfiguration.useOrphanet
                        ? DiseaseDatabase.allKnownDiseaseDatabases()
                        : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER))
                .genotypeLrProperties(genotypeLrProperties)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
                .build();
    }

    protected abstract String getGenomeBuild();

    private GenomeBuild parseGenomeBuild(String genomeBuild) throws LiricalDataException {
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(genomeBuild);
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + genomeBuild + "'");
        return genomeBuildOptional.get();
    }

    protected AnalysisOptions prepareAnalysisOptions(Lirical lirical) {
        LOGGER.debug("Using uniform pretest disease probabilities.");
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(lirical.phenotypeService().diseases());
        return AnalysisOptions.of(runConfiguration.globalAnalysisMode,
                pretestDiseaseProbability,
                runConfiguration.disregardDiseaseWithNoDeleteriousVariants,
                runConfiguration.pathogenicityThreshold);
    }

    protected static GenesAndGenotypes readVariantsFromVcfFile(String sampleId,
                                                               Path vcfPath,
                                                               VariantParserFactory parserFactory) throws LiricalParseException {
        if (parserFactory == null) {
            LOGGER.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return GenesAndGenotypes.empty();
        }

        List<LiricalVariant> variants;
        try (VariantParser variantParser = parserFactory.forPath(vcfPath)) {
            // Ensure the VCF file contains the sample
            if (!variantParser.sampleNames().contains(sampleId))
                throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
            LOGGER.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

            // Read variants
            LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter();
            variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }

        return prepareGenesAndGenotypes(variants);
    }

    protected static GenesAndGenotypes prepareGenesAndGenotypes(List<LiricalVariant> variants) {
        // Group variants by Entrez ID.
        Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
        for (LiricalVariant variant : variants) {
            variant.annotations().stream()
                    .map(TranscriptAnnotation::getGeneId)
                    .distinct()
                    .forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new LinkedList<>()).add(variant));
        }

        // Collect the variants into Gene2Genotype container
        List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                .map(e -> Gene2Genotype.of(e.getKey(), e.getValue()))
                .toList();

        return GenesAndGenotypes.of(g2g);
    }

    protected static void reportElapsedTime(long startTime, long stopTime) {
        int elapsedTime = (int)((stopTime - startTime)*(1.0)/1000);
        if (elapsedTime > 3599) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            int elapsedHours = elapsedTime/3600;
            LOGGER.info(String.format("Elapsed time %d:%2d%2d",elapsedHours,elapsedMinutes,elapsedSeconds));
        }
        else if (elapsedTime>59) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            LOGGER.info(String.format("Elapsed time %d min, %d sec",elapsedMinutes,elapsedSeconds));
        } else {
            LOGGER.info("Elapsed time " + (stopTime - startTime) * (1.0) / 1000 + " seconds.");
        }
    }

}
