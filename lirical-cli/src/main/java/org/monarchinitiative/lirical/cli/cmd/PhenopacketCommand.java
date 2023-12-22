package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.analysis.AnalysisInputs;
import picocli.CommandLine;

import java.nio.file.Path;

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
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    public String genomeBuild = "hg38";

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
    protected AnalysisInputs prepareAnalysisInputs() throws LiricalParseException {
        // We could have returned `data` right away, but we must ensure that VCF handed over via CLI has
        // a greater priority
        AnalysisInputs data = PhenopacketUtil.readPhenopacketData(phenopacketPath);

        String vcf = vcfPath != null
                ? vcfPath
                : data.vcf();

        return new AnalysisInputsDefault(data.sampleId(),
                data.presentHpoTerms(),
                data.excludedHpoTerms(),
                data.age(),
                data.sex(),
                vcf);
    }

}
