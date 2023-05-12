package org.monarchinitiative.lirical.cli.cmd.experimental;

import org.monarchinitiative.lirical.cli.cmd.OutputCommand;
import org.monarchinitiative.lirical.cli.cmd.PhenopacketUtil;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.FilteringStats;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
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

        // Prepare objects required for the overall analysis
        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());

        // 2 - process phenopackets
        LOGGER.info("Processing {} phenopacket(s)", phenopacketPaths.size());
        for (Path phenopacketPath : phenopacketPaths) {
            try {
                // prepare analysis data
                LOGGER.info("Preparing analysis data for {}", phenopacketPath.toAbsolutePath());
                PhenopacketData data = PhenopacketUtil.readPhenopacketData(phenopacketPath);

                List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
                List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
                if (presentTerms.isEmpty() && excludedTerms.isEmpty()) {
                    LOGGER.warn("No phenotype terms were provided. Skipping..");
                    continue;
                }

                AnalysisData analysisData = AnalysisData.of(data.getSampleId(),
                        data.getAge().orElse(null),
                        data.getSex().orElse(null),
                        presentTerms,
                        excludedTerms,
                        GenesAndGenotypes.empty());

                LOGGER.info("Running the analysis");
                AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, genomeBuild, runConfiguration.transcriptDb);
                LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
                AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);

                LOGGER.info("Writing out the results");
                FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
                AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                        .setLiricalVersion(lirical.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                        .setHpoVersion(lirical.phenotypeService().hpo().version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                        .setTranscriptDatabase(runConfiguration.transcriptDb.toString())
                        .setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                        .setExomiserPath(dataSection.exomiserDatabase == null ? "" : dataSection.exomiserDatabase.toAbsolutePath().toString())
                        .setAnalysisDate(LocalDateTime.now().toString())
                        .setSampleName(analysisData.sampleId())
                        .setnGoodQualityVariants(filteringStats.nGoodQualityVariants())
                        .setnFilteredVariants(filteringStats.nFilteredVariants())
                        .setGenesWithVar(0) // TODO
                        .setGlobalMode(runConfiguration.globalAnalysisMode)
                        .build();

                AnalysisResultWriterFactory factory = lirical.analysisResultsWriterFactory();

                OutputOptions outputOptions = createOutputOptions(data.getSampleId());
                for (OutputFormat fmt : output.outputFormats) {
                    Optional<AnalysisResultsWriter> writer = factory.getWriter(fmt);
                    if (writer.isPresent()) {
                        writer.get().process(analysisData, results, metadata, outputOptions);
                    }
                }
            } catch (IOException | LiricalException e) {
                LOGGER.error("Error processing {}: {}", phenopacketPath.toAbsolutePath(), e.getMessage());
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
}
