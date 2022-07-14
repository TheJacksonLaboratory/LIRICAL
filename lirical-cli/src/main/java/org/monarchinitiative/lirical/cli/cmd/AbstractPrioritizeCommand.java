package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

/**
 * This is a common superclass for {@link YamlCommand}, {@link PhenopacketCommand}, and {@link PrioritizeCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by all the subclasses.
 *
 * @author Peter N Robinson
 */
abstract class AbstractPrioritizeCommand extends BaseLiricalCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPrioritizeCommand.class);

    // ---------------------------------------------- OUTPUTS ----------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Output options:%n")
    public Output output = new Output();

    public static class Output {
        @CommandLine.Option(names = {"-o", "--output-directory"},
                description = "Directory into which to write output (default: ${DEFAULT-VALUE}).")
        public Path outdir = Path.of("");

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
        public String outfilePrefix = "lirical";

        @CommandLine.Option(names = {"-t", "--threshold"},
                description = "Minimum post-test probability to show diagnosis in HTML output. The value should range between [0,1].")
        public Double lrThreshold = null;

        @CommandLine.Option(names = {"-m", "--mindiff"},
                description = "Minimal number of differential diagnoses to show.")
        public Integer minDifferentialsToShow = null;

        @CommandLine.Option(names = {"--display-all-variants"},
                description = "Display all variants in output, not just variants passing pathogenicity threshold (default ${DEFAULT-VALUE})")
        public boolean displayAllVariants = false;
    }

    @Override
    public Integer call() throws Exception {
        printBanner();
        long start = System.currentTimeMillis();
        // 0 - check input
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // 1 - bootstrap the app
        Lirical lirical = bootstrapLirical();

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

        reportElapsedTime(start, System.currentTimeMillis());
        return 0;
    }

    protected List<String> checkInput() {
        List<String> errors = super.checkInput();

        // thresholds
        if (output.lrThreshold != null && output.minDifferentialsToShow != null) {
            String msg = "Only one of the options -t/--threshold and -m/--mindiff can be used at once.";
            LOGGER.error(msg);
            errors.add(msg);
        }
        if (output.lrThreshold != null) {
            if (output.lrThreshold < 0.0 || output.lrThreshold > 1.0) {
                String msg = "Post-test probability (-t/--threshold) must be between 0.0 and 1.0.";
                LOGGER.error(msg);
                errors.add(msg);
            }
        }
        return errors;
    }

    protected abstract AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException;

    protected OutputOptions createOutputOptions() {
        LrThreshold lrThreshold = output.lrThreshold == null ? LrThreshold.notInitialized() : LrThreshold.setToUserDefinedThreshold(output.lrThreshold);
        MinDiagnosisCount minDiagnosisCount = output.minDifferentialsToShow == null ? MinDiagnosisCount.notInitialized() : MinDiagnosisCount.setToUserDefinedMinCount(output.minDifferentialsToShow);
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
