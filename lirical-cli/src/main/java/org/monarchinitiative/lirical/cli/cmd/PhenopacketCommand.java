package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.sanitize.SanitationInputs;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

/**
 * Run LIRICAL from a Phenopacket -- with or without accompanying VCF file.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter N Robinson</a>
 */

@CommandLine.Command(name = "phenopacket",
        aliases = {"P"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from a Phenopacket.")
public class PhenopacketCommand extends AbstractPrioritizeCommand {

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = {
                    "Genome build.",
                    "Leave unset to run in phenotype-only mode.",
                    "Default: ${DEFAULT-VALUE}"
            })
    public String genomeBuild = null;

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            required = true,
            description = "Path to phenopacket file in JSON, YAML or protobuf format.")
    public Path phenopacketPath;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to a VCF file. This path has priority over any VCF files described in phenopacket.")
    public String vcfPath;

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected SanitationInputs procureSanitationInputs() throws LiricalParseException {
        // We could have returned `data` right away, but we must ensure that VCF handed over via CLI has
        // a greater priority
        SanitationInputs data = PhenopacketUtil.readPhenopacketData(phenopacketPath);

        String vcf = vcfPath != null
                ? vcfPath
                : data.vcf();

        return new SanitationInputsDefault(data.sampleId(),
                data.presentHpoTerms(),
                data.excludedHpoTerms(),
                data.age(),
                data.sex(),
                vcf);
    }

    @Override
    protected List<String> checkInput() {
        List<String> errors = super.checkInput();
        if (genomeBuild == null && vcfPath != null) {
            String msg = "The --vcf is set but --assembly is not specified. "
                    + "Proceed either with genotype-aware analysis with both --vcf and --assembly options, "
                    + "or run a phenotype-only analysis without the --vcf and --assembly options.";
            errors.add(msg);
        }
        return errors;
    }

}
