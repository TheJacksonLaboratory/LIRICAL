package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.configuration.*;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.Age;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.model.GenomeBuild;
import org.monarchinitiative.lirical.model.LiricalVariant;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This is a common superclass for {@link YamlCommand} and {@link PhenopacketCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by both of the subclasses.
 *
 * @author Peter N Robinson
 */
abstract class AbstractPrioritizeCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPrioritizeCommand.class);
    private static final Properties PROPERTIES = readProperties();
    private static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "unknown version");

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading="Resource paths:%n")
    public DataSection dataSection = new DataSection();
    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        protected Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                description = "Path to the Exomiser data directory.")
        protected Path exomiserDataDirectory = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        protected Path backgroundFrequencyFile = null;
    }

    // ---------------------------------------------- CONFIGURATION ----------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading="Configuration options:%n")
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

        @CommandLine.Option(names={"--transcript-db"},
                paramLabel = "{REFSEQ,UCSC}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        protected TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        protected boolean useOrphanet = false;

        @CommandLine.Option(names = {"--strict"},
                // TODO - add better description
                description = "Strict mode (default: ${DEFAULT-VALUE}).")
        private boolean strict = false;

        @CommandLine.Option(names = {"--pathogenicity-threshold"},
                description = "Variant with pathogenicity score greater than the threshold is considered deleterious (default: ${DEFAULT-VALUE}).")
        private float pathogenicityThreshold = .8f;

        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = "Variant with pathogenicity score greater than the threshold is considered deleterious (default: ${DEFAULT-VALUE}).")
        private float defaultAlleleFrequency = 1E-5f;
    }

    // ---------------------------------------------- OUTPUTS ----------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading="Output options:%n")
    public Output output = new Output();
    public static class Output {
        @CommandLine.Option(names = {"-o", "--output-directory"},
                description = "Directory into which to write output (default: ${DEFAULT-VALUE}).")
        protected Path outdir = Path.of("");

        @CommandLine.Option(names = {"-f", "--output-format"},
                paramLabel = "{html,tsv}",
                description = "Comma separated list of output formats to use for writing the results (default: ${DEFAULT-VALUE}).")
        public String outputFormats = "html";
        /**
         * Prefix of the output file. For instance, if the user enters {@code -x sample1} and an HTML file is output,
         * the name of the HTML file will be {@code sample1.html}. If a TSV file is output, the name of the file will
         * be {@code sample1.tsv}.
         */
        @CommandLine.Option(names = {"-x", "--prefix"},
                description = "Prefix of outfile (default: ${DEFAULT-VALUE}).")
        protected String outfilePrefix = "lirical";
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

    @Override
    public Integer call() throws Exception {
        // 0. - check input
        int status = checkInput();
        if (status != 0)
            return status;

        // 1. - bootstrap the app
        LOGGER.info("Spooling up Lirical v{}", LIRICAL_VERSION);
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(getGenomeBuild());
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + getGenomeBuild() + "'");

        LiricalProperties liricalProperties = LiricalProperties.builder(dataSection.liricalDataDirectory)
                .exomiserDataDirectory(dataSection.exomiserDataDirectory)
                .genomeAssembly(genomeBuildOptional.get())
                .backgroundFrequencyFile(dataSection.backgroundFrequencyFile)
                // CONFIGURATION
                .diseaseDatabases(runConfiguration.useOrphanet
                        ? DiseaseDatabase.allKnownDiseaseDatabases()
                        : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER))
                .genotypeLrProperties(runConfiguration.strict, runConfiguration.pathogenicityThreshold)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .defaultVariantFrequency(runConfiguration.defaultAlleleFrequency)
                .build();
        LiricalConfiguration factory = LiricalConfiguration.of(liricalProperties);
        Lirical lirical = factory.getLirical();

        LOGGER.info("Preparing the analysis data");
        AnalysisData analysisData = prepareAnalysisData(lirical);
        if (analysisData.presentPhenotypeTerms().isEmpty() && analysisData.negatedPhenotypeTerms().isEmpty()) {
            LOGGER.warn("No phenotype terms were provided. Aborting..");
            return 1;
        }

        AnalysisOptions analysisOptions = prepareAnalysisOptions();
        LOGGER.info("Starting the analysis");
        LiricalAnalysisRunner analyzer = lirical.analyzer();
        AnalysisResults results = analyzer.run(analysisData, analysisOptions);

        // TODO - Do we need HpoCase or we can get along with AnalysiData and pass it
        LOGGER.info("Writing out the results");
        HpoCase hpoCase = new HpoCase.Builder(analysisData.sampleId(), analysisData.presentPhenotypeTerms())
                .excluded(analysisData.negatedPhenotypeTerms())
                .age(analysisData.age())
                .sex(analysisData.sex())
                .results(results)
                .build();

        // TODO - richer metadata
        Map<String, String> metadata = Map.of("analysis_date", getTodaysDate(),
                "sample_name", analysisData.sampleId());
        Map<TermId, Gene2Genotype> geneById = analysisData.genes().genes()
                .collect(Collectors.toMap(g -> g.geneId().id(), Function.identity()));
        LiricalTemplate.builder(liricalProperties, hpoCase, lirical.phenotypeService().hpo(), geneById, metadata)
                .outDirectory(output.outdir)
                .prefix(output.outfilePrefix)
                .threshold(runConfiguration.lrThreshold == null ? LrThreshold.notInitialized() : LrThreshold.setToUserDefinedThreshold(runConfiguration.lrThreshold))
                .mindiff(runConfiguration.minDifferentialsToShow == null ? MinDiagnosisCount.notInitialized() : MinDiagnosisCount.setToUserDefinedMinCount(runConfiguration.minDifferentialsToShow))
                .buildPhenotypeHtmlTemplate()
                .outputFile();

        return 0;
    }

    protected abstract String getGenomeBuild();
    protected abstract AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException;

    protected AnalysisOptions prepareAnalysisOptions() {
        return new AnalysisOptions(runConfiguration.globalAnalysisMode);
    }


    protected static Age parseAge(String age) {
        if (age == null) {
            LOGGER.debug("The age was not provided");
            return Age.ageNotKnown();
        }
        try {
            Period period = Period.parse(age);
            LOGGER.info("Using age {}", period);
            return Age.parse(period);
        } catch (DateTimeParseException e) {
            throw new LiricalRuntimeException("Unable to parse age '" + age + "': " + e.getMessage(), e);
        }
    }

    protected static GenesAndGenotypes readVariantsFromVcfFile(String sampleId,
                                                               Path vcfPath,
                                                               VariantParserFactory parserFactory,
                                                               HpoAssociationData associationData) throws LiricalParseException {
        // TODO - RNR1 is an example of a gene with 2 NCBIGene IDs.
        Map<String, List<GeneIdentifier>> symbolToGeneId = associationData.geneIdentifiers().stream()
                .collect(Collectors.groupingBy(GeneIdentifier::symbol));

        try (VariantParser variantParser = parserFactory.forPath(vcfPath)) {
            // Ensure the VCF file contains the sample
            if (!variantParser.sampleNames().contains(sampleId))
                throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
            LOGGER.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

            // Read variants
            LOGGER.info("Reading variants");
            List<LiricalVariant> variants = variantParser.variantStream().toList();
            LOGGER.info("Read {} variants", variants.size());

            // Group variants by gene symbol. It would be better to group the variants by e.g. Entrez ID,
            // but the ID is not available from TranscriptAnnotation
            Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
            for (LiricalVariant variant : variants) {
                variant.annotations().stream()
                        .map(TranscriptAnnotation::getGeneSymbol)
                        .distinct()
                        .forEach(geneSymbol -> {
                            List<GeneIdentifier> identifiers = symbolToGeneId.getOrDefault(geneSymbol, List.of());
                            if (identifiers.isEmpty()) {
                                LOGGER.warn("Skipping unknown gene {}", geneSymbol);
                                return;
                            }
                            for (GeneIdentifier identifier : identifiers) {
                                gene2Genotype.computeIfAbsent(identifier, e -> new LinkedList<>()).add(variant);
                            }
                        });
            }

            // Collect the variants into Gene2Genotype container
            List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                    .map(e -> Gene2Genotype.of(e.getKey(), e.getValue()))
                    .toList();

            return GenesAndGenotypes.of(g2g);
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = AbstractPrioritizeCommand.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    /**
     * @return a string with today's date in the format yyyy/MM/dd.
     */
    private static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
