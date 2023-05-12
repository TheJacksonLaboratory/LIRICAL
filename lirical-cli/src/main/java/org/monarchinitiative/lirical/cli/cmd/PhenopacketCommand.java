package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.*;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
    public Path vcfPath;

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical,
                                               GenomeBuild genomeBuild,
                                               TranscriptDatabase transcriptDb) throws LiricalParseException {
        PhenopacketData data = PhenopacketUtil.readPhenopacketData(phenopacketPath);

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        // Read VCF file.
        GenesAndGenotypes genes;
        // Path to VCF set via CLI has priority.
        Path vcf = this.vcfPath != null
                ? this.vcfPath
                : data.getVcfPath().orElse(null);
        String sampleId = data.getSampleId();
        if (vcf == null) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcf, genomeBuild, transcriptDb, lirical.variantParserFactory());
        }
        return AnalysisData.of(sampleId,
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                presentTerms,
                excludedTerms,
                genes);
    }

}
