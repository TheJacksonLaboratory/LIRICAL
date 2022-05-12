package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "prioritize",
        aliases = {"R"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Prioritize diseases based on observed/negated phenotype terms and a VCF file.")
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
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF file (optional).")
    public Path vcfPath = null;

    @CommandLine.Option(names = {"--sample-id"},
            description = "Proband's identifier (default: ${DEFAULT-VALUE}).")
    public String sampleId = "Sample";

    @CommandLine.Option(names = {"--age"},
            description = "Proband's age.")
    public String age = null;

    @CommandLine.Option(names = {"--sex"},
            paramLabel = "{MALE,FEMALE,UNKNOWN}",
            description = "Proband's sex (default: ${DEFAULT-VALUE}).")
    public Sex sex = Sex.UNKNOWN;


    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException {
        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());

        List<TermId> observedTerms;
        if (observed != null)
            observedTerms = Arrays.stream(observed.split(","))
                    .map(String::trim)
                    .map(TermId::of)
                    .map(sanitizer::replaceIfObsolete)
                    .flatMap(Optional::stream)
                    .distinct()
                    .toList();
        else
            observedTerms = List.of();

        List<TermId> negatedTerms;
        if (negated != null)
            negatedTerms = Arrays.stream(negated.split(","))
                    .map(String::trim)
                    .map(TermId::of)
                    .map(sanitizer::replaceIfObsolete)
                    .flatMap(Optional::stream)
                    .distinct()
                    .toList();
        else
            negatedTerms = List.of();

        GenesAndGenotypes genes;
        if (vcfPath == null) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory().orElse(null));
        }

        return AnalysisData.of(sampleId, parseAge(age), sex, observedTerms, negatedTerms, genes);
    }

}
