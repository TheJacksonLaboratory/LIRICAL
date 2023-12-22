package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.FilteringStats;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.sanitize.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * The driver class for an analysis of a single individual.
 * <p>
 * This class is the superclass for {@link YamlCommand}, {@link PhenopacketCommand}, and {@link PrioritizeCommand}.
 * The subclasses must provide the input data and the driver takes care of the rest.
 *
 * @author Peter N Robinson
 * @author Daniel Danis
 */
abstract class AbstractPrioritizeCommand extends OutputCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPrioritizeCommand.class);

    @Override
    public Integer execute() {
        long start = System.currentTimeMillis();
        // 0 - check input
        List<String> errors = checkInput();
        if (!errors.isEmpty()) {
            LOGGER.error("Errors:");
            for (String error : errors)
                LOGGER.error("  {}", error);
            return 1;
        }

        try {
            GenomeBuild genomeBuild = parseGenomeBuild(getGenomeBuild());
            LOGGER.debug("Using genome build {}", genomeBuild);

            LOGGER.debug("Using {} transcripts", runConfiguration.transcriptDb);
            TranscriptDatabase transcriptDb = runConfiguration.transcriptDb;

            LOGGER.info("Parsing the analysis inputs");
            SanitationInputs inputs = procureSanitationInputs();

            // 1 - bootstrap the app
            LOGGER.info("Bootstrapping LIRICAL");
            Lirical lirical = bootstrapLirical(genomeBuild);
            LOGGER.info("Configured LIRICAL {}", lirical.version()
                    .map("v%s"::formatted)
                    .orElse(UNKNOWN_VERSION_PLACEHOLDER));

            // 2 - sanitize inputs
            InputSanitizerFactory sanitizerFactory = new InputSanitizerFactory(lirical.phenotypeService().hpo());
            InputSanitizer sanitizer = selectSanitizer(sanitizerFactory);
            SanitationResult result = sanitizer.sanitize(inputs);
            summarizeSanitationResult(result).ifPresent(LOGGER::info);

            // We abort on dry run or if the issues are above the failure policy tolerance.
            if (runConfiguration.dryRun) {
                LOGGER.info("Aborting the run due to `--dry-run` option");
                return 0;
            } else {
                switch (runConfiguration.failurePolicy) {
                    case STRICT -> {
                        if (result.hasErrorOrWarnings()) {
                            LOGGER.info("Aborting the run. Fix the errors and warnings or use more permissive failure policy");
                            return 1;
                        }
                    }
                    case LENIENT -> {
                        if (result.hasErrors()) {
                            LOGGER.info("Aborting the run. Fix the input errors before proceeding");
                            return 1;
                        }
                    }
                    case KAMIKAZE -> { /* I'm going my merry way... */ }
                    default -> throw new IllegalStateException("Unexpected value: " + runConfiguration.failurePolicy);
                }
            }

            // 3 - prepare analysis data
            AnalysisData analysisData = prepareAnalysisData(lirical, genomeBuild, transcriptDb, result.sanitizedInputs());

            // 4 - run the analysis
            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, genomeBuild, transcriptDb);
            LOGGER.info("Starting the analysis");
            AnalysisResults results;
            try (LiricalAnalysisRunner analysisRunner = lirical.analysisRunner()) {
                results = analysisRunner.run(analysisData, analysisOptions);
            }

            // 5 - write out the results
            LOGGER.info("Writing out the results");
            FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
            AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                    .setLiricalVersion(lirical.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                    .setHpoVersion(lirical.phenotypeService().hpo().version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                    .setTranscriptDatabase(transcriptDb.toString())
                    .setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                    .setExomiserPath(figureOutExomiserPath())
                    .setAnalysisDate(LocalDateTime.now().toString())
                    .setSampleName(analysisData.sampleId())
                    .setnPassingVariants(filteringStats.nPassingVariants())
                    .setnFilteredVariants(filteringStats.nFilteredVariants())
                    .setGenesWithVar(filteringStats.genesWithVariants())
                    .setGlobalMode(runConfiguration.globalAnalysisMode)
                    .build();

            OutputOptions outputOptions = createOutputOptions(output.outfilePrefix);
            AnalysisResultWriterFactory factory = lirical.analysisResultsWriterFactory();

            for (OutputFormat fmt : output.outputFormats) {
                Optional<AnalysisResultsWriter> writer = factory.getWriter(fmt);
                if (writer.isPresent())
                    writer.get().process(analysisData, results, metadata, outputOptions);
            }

            reportElapsedTime(start, System.currentTimeMillis());
        } catch (IOException | LiricalException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("More info:", e);
            return 1;
        }

        return 0;
    }

    protected abstract SanitationInputs procureSanitationInputs() throws LiricalParseException;

    private static AnalysisData prepareAnalysisData(Lirical lirical,
                                                    GenomeBuild genomeBuild,
                                                    TranscriptDatabase transcriptDb,
                                                    SanitizedInputs inputs) throws LiricalParseException {
        // Read VCF file if present.
        String sampleId;
        GenesAndGenotypes genes;
        if (inputs.vcf() == null) {
            // Use placeholder, because the user did not provide sample ID,
            // and we're running phenotype-only analysis.
            sampleId = "subject";
            genes = GenesAndGenotypes.empty();
        } else {
            SampleIdAndGenesAndGenotypes sampleAndGenotypes = readVariantsFromVcfFile(inputs.sampleId(),
                    inputs.vcf(),
                    genomeBuild,
                    transcriptDb,
                    lirical.variantParserFactory());
            sampleId = sampleAndGenotypes.sampleId();
            genes = sampleAndGenotypes.genesAndGenotypes();
        }

        // Put together the analysis data
        return AnalysisData.of(sampleId,
                inputs.age(),
                inputs.sex(),
                inputs.presentHpoTerms(),
                inputs.excludedHpoTerms(),
                genes);
    }
}
