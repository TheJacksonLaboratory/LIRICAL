package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.configuration.*;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This is a common superclass for {@link YamlCommand} and {@link PhenopacketCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by both of the subclasses.
 *
 * @author Peter N Robinson
 */
abstract class AbstractPrioritizeCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPrioritizeCommand.class);
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();
    private static final Properties PROPERTIES = readProperties();
    private static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "unknown version");

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        protected Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                description = "Path to the Exomiser variant database.")
        protected Path exomiserDatabase = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        protected Path backgroundFrequencyFile = null;
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
        protected boolean globalAnalysisMode = false;

        @CommandLine.Option(names = {"-t", "--threshold"},
                description = "Minimum post-test probability to show diagnosis in HTML output. The value should range between [0,1].")
        protected Double lrThreshold = null;

        @CommandLine.Option(names = {"-m", "--mindiff"},
                description = "Minimal number of differential diagnoses to show.")
        protected Integer minDifferentialsToShow = null;

        @CommandLine.Option(names = {"--transcript-db"},
                paramLabel = "{REFSEQ,UCSC}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        protected TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        protected boolean useOrphanet = false;

        @CommandLine.Option(names = {"--strict"},
                // TODO - add better description
                description = "Strict mode (default: ${DEFAULT-VALUE}).")
        private boolean strict = false;

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
        private float pathogenicityThreshold = .8f;

        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = "Variant with greater allele frequency in at least one population is considered common (default: ${DEFAULT-VALUE}).")
        private float defaultAlleleFrequency = 1E-5f;
    }

    // ---------------------------------------------- OUTPUTS ----------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Output options:%n")
    public Output output = new Output();

    public static class Output {
        @CommandLine.Option(names = {"-o", "--output-directory"},
                description = "Directory into which to write output (default: ${DEFAULT-VALUE}).")
        protected Path outdir = Path.of("");

        @CommandLine.Option(names = {"-f", "--output-format"},
                paramLabel = "{html,tsv}",
                description = "Comma separated list of output formats to use for writing the results (default: ${DEFAULT-VALUE}).")
        public String outputFormats = "html";
        /**
         * Prefix of the output file. For instance, if the user enters {@code -x sample1} and an HTML file is output,
         * the name of the HTML file will be {@code sample1.html}. If a TSV file is output, the name of the file will
         * be {@code sample1.tsv}.
         */
        @CommandLine.Option(names = {"-x", "--prefix"},
                description = "Prefix of outfile (default: ${DEFAULT-VALUE}).")
        protected String outfilePrefix = "lirical";

        @CommandLine.Option(names = {"--display-all-variants"},
                description = "Display all variants in output, not just variants passing pathogenicity threshold (default ${DEFAULT-VALUE})")
        protected boolean displayAllVariants = false;
    }

    @Override
    public Integer call() throws Exception {
        // 0 - check input
        int status = checkInput();
        if (status != 0)
            return status;

        // 1 - bootstrap the app
        LOGGER.info("Spooling up Lirical v{}", LIRICAL_VERSION);
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(getGenomeBuild());
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + getGenomeBuild() + "'");

        GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(runConfiguration.pathogenicityThreshold, runConfiguration.defaultVariantBackgroundFrequency, runConfiguration.strict);
        Lirical lirical = LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .exomiserVariantDatabase(dataSection.exomiserDatabase)
                .genomeBuild(genomeBuildOptional.get())
                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
                .setDiseaseDatabases(runConfiguration.useOrphanet
                        ? DiseaseDatabase.allKnownDiseaseDatabases()
                        : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER))
                .genotypeLrProperties(genotypeLrProperties)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
                .build();

        // 2 - prepare inputs
        LOGGER.info("Preparing the analysis data");
        AnalysisData analysisData = prepareAnalysisData(lirical);
        if (analysisData.presentPhenotypeTerms().isEmpty() && analysisData.negatedPhenotypeTerms().isEmpty()) {
            LOGGER.warn("No phenotype terms were provided. Aborting..");
            return 1;
        }

        // 3 - run the analysis
        AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);
        LOGGER.info("Starting the analysis");
        LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
        AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);

        // 4 - write out the results
        LOGGER.info("Writing out the results");
        FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
        AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                .setLiricalVersion(LIRICAL_VERSION)
                .setHpoVersion(lirical.phenotypeService().hpo().getMetaInfo().getOrDefault("release", "UNKNOWN RELEASE"))
                .setTranscriptDatabase(runConfiguration.transcriptDb.toString())
                .setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                .setExomiserPath(dataSection.exomiserDatabase == null ? "" : dataSection.exomiserDatabase.toAbsolutePath().toString())
                .setAnalysisDate(getTodaysDate())
                .setSampleName(analysisData.sampleId())
                .setnGoodQualityVariants(filteringStats.nGoodQualityVariants())
                .setnFilteredVariants(filteringStats.nFilteredVariants())
                .setGenesWithVar(0) // TODO
                .setGlobalMode(runConfiguration.globalAnalysisMode)
                .build();

        OutputOptions outputOptions = createOutputOptions();
        lirical.analysisResultsWriterFactory()
                .getWriter(analysisData, results, metadata)
                .process(outputOptions);

        return 0;
    }

    protected int checkInput() {
        // resources
        if (dataSection.liricalDataDirectory == null) {
            LOGGER.error("Path to Lirical data directory must be provided via `-d | --data` option");
            return 1;
        }

        // thresholds
        if (runConfiguration.lrThreshold != null && runConfiguration.minDifferentialsToShow != null) {
            LOGGER.error("Only one of the options -t/--threshold and -m/--mindiff can be used at once.");
            return 1;
        }
        if (runConfiguration.lrThreshold != null) {
            if (runConfiguration.lrThreshold < 0.0 || runConfiguration.lrThreshold > 1.0) {
                LOGGER.error("Post-test probability (-t/--threshold) must be between 0.0 and 1.0.");
                return 1;
            }
        }
        return 0;
    }

    protected abstract String getGenomeBuild();

    protected abstract AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException;

    private AnalysisOptions prepareAnalysisOptions(Lirical lirical) {
        LOGGER.debug("Using uniform pretest disease probabilities.");
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(lirical.phenotypeService().diseases());
        return AnalysisOptions.of(runConfiguration.globalAnalysisMode, pretestDiseaseProbability);
    }

    protected OutputOptions createOutputOptions() {
        LrThreshold lrThreshold = runConfiguration.lrThreshold == null ? LrThreshold.notInitialized() : LrThreshold.setToUserDefinedThreshold(runConfiguration.lrThreshold);
        MinDiagnosisCount minDiagnosisCount = runConfiguration.minDifferentialsToShow == null ? MinDiagnosisCount.notInitialized() : MinDiagnosisCount.setToUserDefinedMinCount(runConfiguration.minDifferentialsToShow);
        List<OutputFormat> outputFormats = parseOutputFormats(output.outputFormats);
        return new OutputOptions(lrThreshold, minDiagnosisCount, runConfiguration.pathogenicityThreshold,
                output.displayAllVariants, output.outdir, output.outfilePrefix, outputFormats);
    }

    protected static Age parseAge(String age) {
        if (age == null) {
            LOGGER.debug("The age was not provided");
            return Age.ageNotKnown();
        }
        try {
            Period period = Period.parse(age);
            LOGGER.info("Using age {}", period);
            return Age.parse(period);
        } catch (DateTimeParseException e) {
            throw new LiricalRuntimeException("Unable to parse age '" + age + "': " + e.getMessage(), e);
        }
    }

    protected static GenesAndGenotypes readVariantsFromVcfFile(String sampleId,
                                                               Path vcfPath,
                                                               VariantParserFactory parserFactory) throws LiricalParseException {
        if (parserFactory == null) {
            LOGGER.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return GenesAndGenotypes.empty();
        }

        try (VariantParser variantParser = parserFactory.forPath(vcfPath)) {
            // Ensure the VCF file contains the sample
            if (!variantParser.sampleNames().contains(sampleId))
                throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
            LOGGER.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

            // Read variants
            LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter();
            List<LiricalVariant> variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();

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
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    private static Consumer<LiricalVariant> logProgress(AtomicInteger counter) {
        return v -> {
            int current = counter.incrementAndGet();
            if (current % 5000 == 0)
                LOGGER.info("Read {} variants", NF.format(current));
        };
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = AbstractPrioritizeCommand.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    /**
     * @return a string with today's date in the format yyyy/MM/dd.
     */
    private static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    private List<OutputFormat> parseOutputFormats(String outputFormats) {
        return Arrays.stream(outputFormats.split(","))
                .map(String::trim)
                .map(toOutputFormat())
                .flatMap(Optional::stream)
                .toList();
    }

    private static Function<String, Optional<OutputFormat>> toOutputFormat() {
        return payload -> switch (payload.toUpperCase()) {
            case "HTML" -> Optional.of(OutputFormat.HTML);
            case "TSV" -> Optional.of(OutputFormat.TSV);
            default -> {
                LOGGER.warn("Unknown output format {}", payload);
                yield Optional.empty();
            }
        };
    }

}
