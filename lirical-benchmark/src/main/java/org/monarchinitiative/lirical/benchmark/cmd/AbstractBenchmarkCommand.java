package org.monarchinitiative.lirical.benchmark.cmd;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public abstract class AbstractBenchmarkCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBenchmarkCommand.class);


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
        protected boolean strict = false;

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
        protected float pathogenicityThreshold = .8f;

        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = "Variant with greater allele frequency in at least one population is considered common (default: ${DEFAULT-VALUE}).")
        protected float defaultAlleleFrequency = 1E-5f;
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

    protected record BenchmarkData(TermId diseaseId, AnalysisData analysisData) {
    }
}
