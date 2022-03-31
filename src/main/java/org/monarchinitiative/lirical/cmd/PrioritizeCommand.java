package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.model.Sex;
import org.monarchinitiative.lirical.io.HpoTermSanitizer;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@CommandLine.Command(name = "prioritize",
        aliases = {"R"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Prioritize diseases based on observed/negated phenotype terms and a VCF file.")
public class PrioritizeCommand extends AbstractPrioritizeCommand {

    @CommandLine.Option(names = {"-p", "--observed-phenotype"},
            arity = "0..*",
            description = "Observed phenotype terms (can be specified multiple times).")
    public List<String> observed = List.of();

    @CommandLine.Option(names = {"-n", "--negated-phenotype"},
            arity = "0..*",
            description = "Negated phenotype terms (can be specified multiple times).")
    public List<String> negated = List.of();

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

        List<TermId> observedTerms = observed.stream()
                .map(TermId::of)
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        List<TermId> negatedTerms = negated.stream()
                .map(TermId::of)
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        GenesAndGenotypes genes;
        if (vcfPath == null || lirical.variantParserFactory().isEmpty()) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory().get(), lirical.phenotypeService().associationData());
        }

        return AnalysisData.of(sampleId, parseAge(age), sex, observedTerms, negatedTerms, genes);
    }

}
