package org.monarchinitiative.lirical.cmd;

import htsjdk.variant.vcf.VCFFileReader;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.configuration.*;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.Age;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.GenotypedVariantParser;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.lirical.io.vcf.VcfGenotypedVariantParser;
import org.monarchinitiative.lirical.io.vcf.VcfVariantParser;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.model.LiricalVariant;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.service.VariantMetadataService;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
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

    // ---------------------------------------------- INPUTS -----------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading="Resource paths:%n")
    public DataSection dataSection;
    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        protected Path liricalDataDirectory = Path.of("data");

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
        @CommandLine.Option(names = {"--assembly"},
                paramLabel = "{HG19,HG38}",
                description = "Genome assembly (default: ${DEFAULT-VALUE}).")
        protected GenomeAssembly genomeAssembly = GenomeAssembly.HG38;
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

    protected void checkThresholds() {
        if (runConfiguration.lrThreshold != null && runConfiguration.minDifferentialsToShow != null) {
            LOGGER.error("Only one of the options -t/--threshold and -m/--mindiff can be used at once.");
            throw new LiricalRuntimeException("Only one of the options -t/--threshold and -m/--mindiff can be used at once.");
        }
        if (runConfiguration.lrThreshold != null) {
            if (runConfiguration.lrThreshold < 0.0 || runConfiguration.lrThreshold > 1.0) {
                LOGGER.error("Post-test probability (-t/--threshold) must be between 0.0 and 1.0.");
                throw new LiricalRuntimeException("Post-test probability (-t/--threshold) must be between 0.0 and 1.0.");
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        // 0. - check input
        checkThresholds();

        // 1. - bootstrap the app
        LOGGER.info("Spooling up Lirical v{}", LIRICAL_VERSION);
        Lirical lirical = getLirical();
        LOGGER.info("Preparing analysis data");
        AnalysisData analysisData = prepareAnalysisData(lirical);
        if (analysisData.presentPhenotypeTerms().isEmpty() && analysisData.negatedPhenotypeTerms().isEmpty()) {
            LOGGER.warn("No phenotype terms were provided. Aborting..");
            return 1;
        }

        AnalysisOptions analysisOptions = prepareAnalysisOptions();
        LOGGER.info("Starting the analysis");
        LiricalAnalysisRunner analyzer = lirical.analyzer();
        AnalysisResults results = analyzer.run(analysisData, analysisOptions);

        // TODO - richer metadata
        LOGGER.info("Writing out the results");
        HpoCase hpoCase = new HpoCase.Builder(analysisData.presentPhenotypeTerms())
                .excluded(analysisData.negatedPhenotypeTerms())
                .age(analysisData.age())
                .sex(analysisData.sex())
                .results(results.resultsByDiseaseId())
                .build();

        Map<String, String> metadata = Map.of("analysis_date", getTodaysDate(),
                "sample_name", analysisData.sampleId());
        LiricalTemplate.builder(hpoCase, lirical.phenotypeService().hpo(), metadata)
                .outDirectory(output.outdir)
                .prefix(output.outfilePrefix)
                .threshold(runConfiguration.lrThreshold == null ? LrThreshold.notInitialized() : LrThreshold.setToUserDefinedThreshold(runConfiguration.lrThreshold))
                .mindiff(runConfiguration.minDifferentialsToShow == null ? MinDiagnosisCount.notInitialized() : MinDiagnosisCount.setToUserDefinedMinCount(runConfiguration.minDifferentialsToShow))
                .buildPhenotypeHtmlTemplate()
                .outputFile();

        return 0;
    }

    private Lirical getLirical() throws LiricalDataException {
        // TODO - provide background file - builder?
        LiricalConfiguration factory = LiricalConfiguration.of(dataSection.liricalDataDirectory, dataSection.exomiserDataDirectory);
        return factory.getLirical();
    }

    protected abstract AnalysisData prepareAnalysisData(Lirical lirical);

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

    protected static GenesAndGenotypes readVariantsFromVcfFile(Path vcfPath,
                                                               GenomeAssembly genomeAssembly,
                                                               VariantMetadataService metadataService,
                                                               HpoAssociationData associationData) {
        Map<String, GeneIdentifier> symbolToGeneId = associationData.geneIdentifiers().stream()
                .collect(Collectors.toMap(GeneIdentifier::symbol, Function.identity()));
        GenomicAssembly genomicAssembly = parseGenomicAssembly(genomeAssembly);
        try (VCFFileReader reader = new VCFFileReader(vcfPath)) {
            GenotypedVariantParser parser = new VcfGenotypedVariantParser(genomicAssembly, reader);
            VariantParser variantParser = new VcfVariantParser(parser, metadataService);
            LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
            List<LiricalVariant> variants = variantParser.variantStream().toList();
            LOGGER.info("Read {} variants", variants.size());

            Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
            for (LiricalVariant variant : variants) {
                for (TranscriptAnnotation annotation : variant.annotations()) {
                    String geneSymbol = annotation.getGeneSymbol();
                    GeneIdentifier identifier = symbolToGeneId.get(geneSymbol);
                    if (identifier == null) {
                        LOGGER.warn("Skipping unknown gene {}", geneSymbol);
                        continue;
                    }
                    gene2Genotype.computeIfAbsent(identifier, e -> new LinkedList<>()).add(variant);
                }
            }

            List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                    .map(e -> Gene2Genotype.of(e.getKey(), e.getValue()))
                    .toList();

            return GenesAndGenotypes.of(g2g);
        }
    }

    private static GenomicAssembly parseGenomicAssembly(GenomeAssembly genomeAssembly) {
        return switch (genomeAssembly) {
            case HG19 -> GenomicAssemblies.GRCh37p13();
            case HG38 -> GenomicAssemblies.GRCh38p13();
        };
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
