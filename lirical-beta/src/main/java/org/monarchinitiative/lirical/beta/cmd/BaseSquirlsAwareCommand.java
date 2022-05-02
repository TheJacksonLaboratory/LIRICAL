package org.monarchinitiative.lirical.beta.cmd;

import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserMvStoreMetadataService;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Set;

public abstract class BaseSquirlsAwareCommand extends AbstractPrioritizeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSquirlsAwareCommand.class);

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public PrioritizeWithSquirlsCommand.DataSection dataSection = new PrioritizeWithSquirlsCommand.DataSection();
    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                required = true,
                description = "Path to Exomiser variant database.")
        public Path exomiserVariantDatabase = null;

        @CommandLine.Option(names = {"--squirls"},
                required = true,
                description = "Path to Squirls data directory.")
        public Path squirlsDataDirectory = null;

        @CommandLine.Option(names = {"--cap-squirls-deleterious-variants"},
                description = "Ensure all splice variants are labeled as deleterious.")
        public boolean capSquirlsDeleterious = false;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        public Path backgroundFrequencyFile = null;
    }

    @Override
    protected Lirical bootstrapLirical(GenomeBuild genomeBuild) throws LiricalDataException {
        Set<DiseaseDatabase> diseaseDatabases = runConfiguration.useOrphanet
                ? DiseaseDatabase.allKnownDiseaseDatabases()
                : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);

        VariantMetadataService exomiserMetadataService = ExomiserMvStoreMetadataService.of(dataSection.exomiserVariantDatabase,
                new VariantMetadataService.Options(runConfiguration.defaultVariantAlleleFrequency));

        SquirlsAwareVariantMetadataService squirlsAwareVariantMetadataService = SquirlsAwareVariantMetadataService.of(exomiserMetadataService,
                dataSection.squirlsDataDirectory,
                runConfiguration.transcriptDb,
                runConfiguration.pathogenicityThreshold,
                dataSection.capSquirlsDeleterious);

        return LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .genomeBuild(genomeBuild)
                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
                .setDiseaseDatabases(diseaseDatabases)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .variantMetadataService(squirlsAwareVariantMetadataService)
                .build();
    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
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

    protected AnalysisResultsMetadata.Builder fillDataSection(AnalysisResultsMetadata.Builder builder) {
        return builder.setLiricalPath(dataSection.liricalDataDirectory.toAbsolutePath().toString())
                .setExomiserPath(dataSection.exomiserVariantDatabase == null ? "" : dataSection.exomiserVariantDatabase.toAbsolutePath().toString());
    }
}
