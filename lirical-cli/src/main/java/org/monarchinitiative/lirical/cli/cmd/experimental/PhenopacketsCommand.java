package org.monarchinitiative.lirical.cli.cmd.experimental;

import org.monarchinitiative.lirical.cli.cmd.OutputCommand;
import org.monarchinitiative.lirical.cli.cmd.PhenopacketUtil;
import org.monarchinitiative.lirical.cli.cmd.util.Util;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.FilteringStats;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.sanitize.InputSanitizer;
import org.monarchinitiative.lirical.core.sanitize.SanitationResult;
import org.monarchinitiative.lirical.core.sanitize.SanitizedInputs;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Run LIRICAL in phenotype-only mode on a collection of phenopackets.
 */
@CommandLine.Command(name = "phenopackets",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL for several phenopackets at once in phenotype-only mode.")
public class PhenopacketsCommand extends OutputCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketsCommand.class);

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    public String genomeBuild = "hg38";

    @CommandLine.Parameters(
            paramLabel = "phenopacket file(s)",
            description = {
                    "Input phenopacket(s).",
            }
    )
    public List<Path> phenopacketPaths = null;

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected Integer execute() {
        long start = System.currentTimeMillis();
        // 0 - check input
        List<String> errors = checkInput();
        if (!errors.isEmpty()) {
            LOGGER.error("Errors:");
            for (String error : errors)
                LOGGER.error("  {}", error);
            return 1;
        }

        Lirical lirical;
        GenomeBuild genomeBuild;
        try {
            genomeBuild = parseGenomeBuild(getGenomeBuild());
            LOGGER.debug("Using genome build {}", genomeBuild);
            LOGGER.debug("Using {} transcripts", runConfiguration.transcriptDb);

            // 1 - bootstrap the app
            lirical = bootstrapLirical(genomeBuild);
            LOGGER.info("Configured LIRICAL {}", lirical.version()
                    .map("v%s"::formatted)
                    .orElse(UNKNOWN_VERSION_PLACEHOLDER));
        } catch (LiricalException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("More info:", e);
            return 1;
        }

        // 2 - sanitize the input data
        LOGGER.info("Reading and sanitizing {} phenopacket(s)", phenopacketPaths.size());

        MinimalOntology hpo = lirical.phenotypeService().hpo();
        List<SanitationResultsAndPath> sanitationResults = sanitizePhenopackets(phenopacketPaths, hpo);
        if (runConfiguration.dryRun) {
            // summarize and quit
            for (SanitationResultsAndPath result : sanitationResults) {
                LOGGER.info("Summary for {}", result.path().toAbsolutePath());
                LOGGER.info(summarizeSanitationResult(result.result()).orElse("OK"));
            }
            return 0;
        }

        // 3 - process phenopackets
        LOGGER.info("Processing phenopackets");
        AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, genomeBuild, runConfiguration.transcriptDb);
        LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
        for (SanitationResultsAndPath result : sanitationResults) {
            SanitationResult sanitationResult = result.result();
            if (!Util.phenopacketIsEligibleForAnalysis(sanitationResult, runConfiguration.failurePolicy)) {
                summarizeSanitationResult(sanitationResult)
                        .ifPresent(summary -> LOGGER.info("Skipping phenopacket {}{}{}",
                                result.path().toAbsolutePath(),
                                System.lineSeparator(),
                                summary));
            } else {
                LOGGER.info("Processing {}", result.path().toAbsolutePath());
            }
            try {
                SanitizedInputs sanitized = sanitationResult.sanitized();
                AnalysisData analysisData = AnalysisData.of(sanitized.sampleId(),
                        sanitized.age(), sanitized.sex(),
                        sanitized.presentHpoTerms(),
                        sanitized.excludedHpoTerms(),
                        GenesAndGenotypes.empty());

                LOGGER.debug("Running the analysis");
                AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);

                LOGGER.debug("Writing out the results");
                FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
                AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                        .setLiricalVersion(lirical.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                        .setHpoVersion(hpo.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                        .setTranscriptDatabase(runConfiguration.transcriptDb.toString())
                        .setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                        .setExomiserPath(figureOutExomiserPath())
                        .setAnalysisDate(LocalDateTime.now().toString())
                        .setSampleName(analysisData.sampleId())
                        .setnPassingVariants(filteringStats.nPassingVariants())
                        .setnFilteredVariants(filteringStats.nFilteredVariants())
                        .setGenesWithVar(filteringStats.genesWithVariants())
                        .setGlobalMode(runConfiguration.globalAnalysisMode)
                        .build();

                AnalysisResultWriterFactory factory = lirical.analysisResultsWriterFactory();

                OutputOptions outputOptions = createOutputOptions(sanitized.sampleId());
                for (OutputFormat fmt : output.outputFormats) {
                    Optional<AnalysisResultsWriter> writer = factory.getWriter(fmt);
                    if (writer.isPresent()) {
                        writer.get().process(analysisData, results, metadata, outputOptions);
                    }
                }
            } catch (IOException | LiricalException e) {
                LOGGER.error("Error processing {}: {}", result.path(), e.getMessage());
                LOGGER.debug("More info:", e);
                return 1;
            }
        }
        reportElapsedTime(start, System.currentTimeMillis());

        return 0;
    }

    protected List<String> checkInput() {
        List<String> errors = super.checkInput();

        if (phenopacketPaths == null || phenopacketPaths.isEmpty())
            errors.add("At least one phenopacket path must be provided");

        return errors;
    }

    private static List<SanitationResultsAndPath> sanitizePhenopackets(List<Path> phenopackets,
                                                                      MinimalOntology hpo) {
        InputSanitizer sanitizer = InputSanitizer.defaultSanitizer(hpo);
        List<SanitationResultsAndPath> sanitationResults = new ArrayList<>(phenopackets.size());
        for (Path phenopacketPath : phenopackets) {
            SanitationResultsAndPath resultAndPath;
            try {
                AnalysisInputs inputs = PhenopacketUtil.readPhenopacketData(phenopacketPath);
                SanitationResult sanitationResult = sanitizer.sanitize(inputs);
                resultAndPath = new SanitationResultsAndPath(sanitationResult, phenopacketPath);
            } catch (LiricalException e) {
                resultAndPath = new SanitationResultsAndPath(null, phenopacketPath);
            }
            sanitationResults.add(resultAndPath);
        }
        return sanitationResults;
    }

    private record SanitationResultsAndPath(SanitationResult result, Path path) {
    }
}
