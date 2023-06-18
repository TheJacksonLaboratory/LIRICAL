package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.output.*;
import org.monarchinitiative.lirical.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * This is a common superclass for {@link YamlCommand}, {@link PhenopacketCommand}, and {@link PrioritizeCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by all the subclasses.
 *
 * @author Peter N Robinson
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

            // 1 - bootstrap the app
            Lirical lirical = bootstrapLirical(genomeBuild);
            LOGGER.info("Configured LIRICAL {}", lirical.version()
                    .map("v%s"::formatted)
                    .orElse(UNKNOWN_VERSION_PLACEHOLDER));

            // 2 - prepare inputs
            LOGGER.info("Preparing the analysis data");
            AnalysisData analysisData = prepareAnalysisData(lirical, genomeBuild, transcriptDb);
            if (analysisData.presentPhenotypeTerms().isEmpty() && analysisData.negatedPhenotypeTerms().isEmpty()) {
                LOGGER.warn("No phenotype terms were provided. Aborting..");
                return 1;
            }

            // 3 - run the analysis
            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, genomeBuild, transcriptDb);
            LOGGER.info("Starting the analysis");
            LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
            AnalysisResults results = analysisRunner.run(analysisData, analysisOptions);

            // 4 - write out the results
            LOGGER.info("Writing out the results");
            FilteringStats filteringStats = analysisData.genes().computeFilteringStats();
            AnalysisResultsMetadata metadata = AnalysisResultsMetadata.builder()
                    .setLiricalVersion(lirical.version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                    .setHpoVersion(lirical.phenotypeService().hpo().version().orElse(UNKNOWN_VERSION_PLACEHOLDER))
                    .setTranscriptDatabase(transcriptDb.toString())
                    .setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                    .setExomiserPath(dataSection.exomiserDatabase == null ? "" : dataSection.exomiserDatabase.toAbsolutePath().toString())
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

    protected abstract AnalysisData prepareAnalysisData(Lirical lirical, GenomeBuild genomeBuild, TranscriptDatabase transcriptDb) throws LiricalParseException;

}
