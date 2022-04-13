package org.monarchinitiative.lirical.beta.cmd;

import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.exception.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.ExomiserDataResolver;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.service.ExomiserVariantMetadataService;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CommandLine.Command(name = "prioritize-squirls",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Prioritize diseases using Squirls and Exomiser.")
public class PrioritizeWithSquirls extends AbstractPrioritizeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizeWithSquirls.class);

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

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                required = true,
                description= "Path to Exomiser data directory.")
        public Path exomiserDataDirectory = null;

        @CommandLine.Option(names = {"--squirls"},
                required = true,
                description = "Path to Squirls data directory.")
        public Path squirlsDataDirectory = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        public Path backgroundFrequencyFile = null;
    }

    @Override
    protected Lirical bootstrapLirical(GenomeBuild genomeBuild) throws LiricalDataException {
        Set<DiseaseDatabase> diseaseDatabases = runConfiguration.useOrphanet
                ? DiseaseDatabase.allKnownDiseaseDatabases()
                : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);

        GenomeAssembly assembly = getGenomeAssembly(genomeBuild);
        ExomiserDataResolver exomiserDataResolver = ExomiserDataResolver.of(dataSection.exomiserDataDirectory);
        ExomiserVariantMetadataService exomiser = ExomiserVariantMetadataService.of(exomiserDataResolver.mvStorePath(),
                exomiserDataResolver.transcriptCacheForTranscript(runConfiguration.transcriptDb),
                assembly,
                VariantMetadataService.defaultOptions());

        SquirlsAwarePathogenicityService squirlsAwarePathogenicityService = SquirlsAwarePathogenicityService.of(exomiser,
                dataSection.squirlsDataDirectory,
                runConfiguration.transcriptDb,
                runConfiguration.pathogenicityThreshold);

        return LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .genomeBuild(genomeBuild)
                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
                .clearDiseaseDatabases().useDiseaseDatabases(diseaseDatabases)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .defaultVariantAlleleFrequency(runConfiguration.defaultVariantAlleleFrequency)
                .functionalVariantAnnotator(exomiser)
                .variantFrequencyService(exomiser)
                .variantPathogenicityService(squirlsAwarePathogenicityService)
                .build();
    }

    private GenomeAssembly getGenomeAssembly(GenomeBuild genomeBuild) {
        return switch (genomeBuild) {
            case HG19 -> GenomeAssembly.HG19;
            case HG38 -> GenomeAssembly.HG38;
        };
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
