package org.monarchinitiative.lirical.cli.cmd;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Benchmark command runs LIRICAL on one or more phenopackets and writes prioritization results into a CSV table.
 * Optionally, a VCF file with background variants can be provided to run variant-aware prioritization.
 * In presence of variants, the benchmark spikes the variants from phenopacket into the background variants
 * and runs prioritization on phenotype terms and variants.
 */
@CommandLine.Command(name = "benchmark",
        hidden = true,
        mixinStandardHelpOptions = true,
        sortOptions = false,
        description = "Benchmark LIRICAL by analyzing a phenopacket (with or without VCF)")
public class BenchmarkCommand extends LiricalConfigurationCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkCommand.class);

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected List<Path> phenopacketPaths;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath; // nullable

    @CommandLine.Option(names = {"-o", "--output"},
            required = true,
            description = "Where to write the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputPath;

    @CommandLine.Option(names = {"--phenotype-only"},
            description = "Run the benchmark with phenotypes only (default: ${DEFAULT-VALUE})")
    protected boolean phenotypeOnly = false;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @Override
    public Integer execute() {
        long start = System.currentTimeMillis();
        // The benchmark has a logic of its own, hence the `execute()` method is overridden.
        // 0 - check input
        List<String> errors = checkInput();
        if (!errors.isEmpty()) {
            LOGGER.error("Errors:");
            for (String error : errors)
                LOGGER.error("  {}", error);
            return 1;
        }

        try {
            GenomeBuild genomeBuild = parseGenomeBuild(getGenomeBuild());
            TranscriptDatabase transcriptDb = runConfiguration.transcriptDb;

            // 1 - bootstrap LIRICAL.
            Lirical lirical = bootstrapLirical(genomeBuild);

            // 2 - prepare the simulation data shared by all phenopackets.
            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical, genomeBuild, transcriptDb);
            List<LiricalVariant> backgroundVariants = readBackgroundVariants(lirical, genomeBuild, transcriptDb);

            try (BufferedWriter writer = openWriter(outputPath);
                 CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                printer.printRecord("phenopacket", "background_vcf", "sample_id", "rank",
                        "is_causal", "disease_id", "post_test_proba"); // header

                for (Path phenopacketPath : phenopacketPaths) {
                    // 3 - prepare benchmark data per phenopacket
                    BenchmarkData benchmarkData = prepareBenchmarkData(lirical, genomeBuild, backgroundVariants, phenopacketPath);

                    // 4 - run the analysis.
                    LOGGER.info("Starting the analysis: {}", analysisOptions);
                    LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
                    AnalysisResults results = analysisRunner.run(benchmarkData.analysisData(), analysisOptions);

                    // 5 - summarize the results.
                    String phenopacketName = phenopacketPath.toFile().getName();
                    String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                    writeResults(phenopacketName, backgroundVcf, benchmarkData, results, printer);
                }
            }
        } catch (IOException | LiricalException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("More info:", e);
            return 1;
        }
        LOGGER.info("Benchmark results were stored to {}", outputPath.toAbsolutePath());

        reportElapsedTime(start, System.currentTimeMillis());
        return 0;
    }

    protected List<String> checkInput() {
        List<String> errors = super.checkInput();

        // Check if all phenopackets are valid and die quickly if not.
        LOGGER.info("Checking validity of {} phenopackets", phenopacketPaths.size());
        for (Path phenopacketPath : phenopacketPaths) {
            try {
                readPhenopacketData(phenopacketPath);
            } catch (LiricalParseException e) {
                errors.add("Invalid phenopacket %s: %s".formatted(phenopacketPath.toAbsolutePath(), e.getMessage()));
            }
        }

        return errors;
    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    private List<LiricalVariant> readBackgroundVariants(Lirical lirical,
                                                        GenomeBuild genomeBuild,
                                                        TranscriptDatabase transcriptDatabase) throws LiricalParseException {
        if (vcfPath == null) {
            LOGGER.info("Path to VCF file was not provided.");
            return List.of();
        }

        Optional<VariantParser> parser = lirical.variantParserFactory().forPath(vcfPath, genomeBuild, transcriptDatabase);
        if (parser.isEmpty()) {
            LOGGER.warn("Cannot obtain parser for processing the VCF file {} with {} {} due to missing resources",
                    vcfPath.toAbsolutePath(), genomeBuild, transcriptDatabase);
            return List.of();
        }

        try (VariantParser variantParser = parser.get()) {
            // Read variants
            LOGGER.info("Reading background variants from {}.", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter(10_000, "variants");
            List<LiricalVariant> variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();
            return variants;
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    private BenchmarkData prepareBenchmarkData(Lirical lirical,
                                               GenomeBuild genomeBuild,
                                               List<LiricalVariant> backgroundVariants,
                                               Path phenopacketPath) throws LiricalParseException {
        LOGGER.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());
        PhenopacketData data = readPhenopacketData(phenopacketPath);

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        GenesAndGenotypes genes;
        if (phenotypeOnly) {
            // We omit the VCF even if provided.
            if (!backgroundVariants.isEmpty())
                LOGGER.warn("The provided VCF file will not be used in `--phenotype-only` mode");
            genes = GenesAndGenotypes.empty();
        } else {
            if (backgroundVariants.isEmpty()) // None or empty VCF file.
                genes = GenesAndGenotypes.empty();
            else {
                // Annotate the causal variants found in the phenopacket.
                FunctionalVariantAnnotator annotator = lirical.functionalVariantAnnotator();
                VariantMetadataService metadataService = lirical.variantMetadataService();
                List<LiricalVariant> backgroundAndCausal = new ArrayList<>(backgroundVariants.size() + 10);
                for (GenotypedVariant variant : data.getVariants()) {
                    List<TranscriptAnnotation> annotations = annotator.annotate(variant.variant());
                    List<VariantEffect> effects = annotations.stream()
                            .map(TranscriptAnnotation::getVariantEffects)
                            .flatMap(Collection::stream)
                            .distinct()
                            .toList();
                    VariantMetadata metadata = metadataService.metadata(variant.variant(), effects);

                    LiricalVariant lv = LiricalVariant.of(variant, annotations, metadata);
                    backgroundAndCausal.add(lv);
                }

                // Read the VCF file.
                genes = prepareGenesAndGenotypes(backgroundAndCausal);
            }
        }

        AnalysisData analysisData = AnalysisData.of(data.getSampleId(),
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                presentTerms,
                excludedTerms,
                genes);

        return new BenchmarkData(data.getDiseaseIds().get(0), analysisData);
    }

    private static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
        PhenopacketData data = null;
        try (InputStream is = Files.newInputStream(phenopacketPath)) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("Unable to parser as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        // Check we have exactly one disease ID.
        if (data.getDiseaseIds().isEmpty())
            throw new LiricalParseException("Missing disease ID which is required for the benchmark!");
        else if (data.getDiseaseIds().size() > 1)
            throw new LiricalParseException("Saw >1 disease IDs {}, but we need exactly one for the benchmark!");
        return data;
    }

    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     String backgroundVcfName,
                                     BenchmarkData benchmarkData,
                                     AnalysisResults results,
                                     CSVPrinter printer) {
        AtomicInteger rankCounter = new AtomicInteger();
        results.resultsWithDescendingPostTestProbability()
                .forEachOrdered(result -> {
                    int rank = rankCounter.incrementAndGet();
                    try {
                        printer.print(phenopacketName);
                        printer.print(backgroundVcfName);
                        printer.print(benchmarkData.analysisData().sampleId());
                        printer.print(rank);
                        printer.print(result.diseaseId().equals(benchmarkData.diseaseId()));
                        printer.print(result.diseaseId());
                        printer.print(result.posttestProbability());
                        printer.println();
                    } catch (IOException e) {
                        LOGGER.error("Error writing results for {}: {}", result.diseaseId(), e.getMessage(), e);
                    }
                });
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

    private record BenchmarkData(TermId diseaseId, AnalysisData analysisData) {
    }
}
