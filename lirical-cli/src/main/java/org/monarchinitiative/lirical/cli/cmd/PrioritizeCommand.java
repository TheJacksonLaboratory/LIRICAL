package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(name = "prioritize",
        aliases = {"R"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from CLI arguments.")
public class PrioritizeCommand extends AbstractPrioritizeCommand {

    @CommandLine.Option(names = {"-p", "--observed-phenotypes"},
            description = "Comma-separated IDs of the observed phenotype terms.")
    public String observed;

    @CommandLine.Option(names = {"-n", "--negated-phenotypes"},
            description = "Comma-separated IDs of the negated/excluded phenotype terms.")
    public String negated;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    public String genomeBuild = "hg38";

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF file (optional).")
    public String vcfPath = null;

    @CommandLine.Option(names = {"--sample-id"},
            description = "Proband's identifier (default: ${DEFAULT-VALUE}).")
    public String sampleId = null;

    @CommandLine.Option(names = {"--age"},
            description = "Proband's age.")
    public String age = null;

    @CommandLine.Option(names = {"--sex"},
            paramLabel = "{MALE,FEMALE,UNKNOWN}",
            description = "Proband's sex (default: ${DEFAULT-VALUE}).")
    public String sex = "UNKNOWN";


    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisInputs prepareAnalysisInputs() {
        List<String> presentTerms = new ArrayList<>();
        if (observed != null) {
            Arrays.stream(observed.split(","))
                    .map(String::trim)
                    .distinct()
                    .forEachOrdered(presentTerms::add);
        }

        List<String> excludedTerms = new ArrayList<>();
        if (negated != null) {
            Arrays.stream(negated.split(","))
                    .map(String::trim)
                    .distinct()
                    .forEachOrdered(excludedTerms::add);
        }

        return new AnalysisInputsDefault(sampleId, presentTerms, excludedTerms, age, sex, vcfPath);
    }

}
