package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.output.LrThreshold;
import org.monarchinitiative.lirical.core.output.MinDiagnosisCount;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public abstract class OutputCommand extends LiricalConfigurationCommand {

    // ---------------------------------------------- OUTPUTS ----------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Output options:%n")
    public Output output = new Output();

    public static class Output {
        @CommandLine.Option(names = {"-o", "--output-directory"},
                description = "Directory into which to write output (default: ${DEFAULT-VALUE}).")
        public Path outdir = Path.of("");

        @CommandLine.Option(names = {"-f", "--output-format"},
                arity = "0..*",
                description = {
                        "An output format to use for writing the results, can be provided multiple times.",
                        "Choose from {${COMPLETION-CANDIDATES}}",
                        "Default: ${DEFAULT-VALUE}"
                })
        public Set<OutputFormat> outputFormats = Set.of(OutputFormat.HTML);
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

    protected List<String> checkInput() {
        List<String> errors = super.checkInput();

        // thresholds
        if (output.lrThreshold != null && output.minDifferentialsToShow != null) {
            String msg = "Only one of the options -t/--threshold and -m/--mindiff can be used at once.";
            errors.add(msg);
        }
        if (output.lrThreshold != null) {
            if (output.lrThreshold < 0.0 || output.lrThreshold > 1.0) {
                String msg = "Post-test probability (-t/--threshold) must be between 0.0 and 1.0.";
                errors.add(msg);
            }
        }
        return errors;
    }

    protected OutputOptions createOutputOptions(String prefix) {
        LrThreshold lrThreshold = output.lrThreshold == null ? LrThreshold.notInitialized() : LrThreshold.setToUserDefinedThreshold(output.lrThreshold);
        MinDiagnosisCount minDiagnosisCount = output.minDifferentialsToShow == null ? MinDiagnosisCount.notInitialized() : MinDiagnosisCount.setToUserDefinedMinCount(output.minDifferentialsToShow);
        return new OutputOptions(lrThreshold, minDiagnosisCount, runConfiguration.pathogenicityThreshold,
                output.displayAllVariants, runConfiguration.showDiseasesWithNoDeleteriousVariants,
                output.outdir, prefix);
    }
}
