package org.monarchinitiative.lirical.benchmark.cmd;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.configuration.GenotypeLrProperties;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
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
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@CommandLine.Command(name = "benchmark",
        mixinStandardHelpOptions = true,
        description = "Benchmark LIRICAL by analyzing a phenopacket (with or without VCF)")
public class BenchmarkCommand extends AbstractBenchmarkCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkCommand.class);
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            required = true,
            description = "Path to phenopacket JSON file.")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath;

    @CommandLine.Option(names = {"--phenotype-only"},
            description = "run simulations with phenotypes only?")
    protected boolean phenotypeOnly = false;

    @CommandLine.Option(names = {"-o", "--output"},
            required = true,
            description = "Where to write the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputPath;

    @Override
    public Integer call() throws Exception {

        // 0 - check input.
        int status = checkInput();
        if (status != 0)
            return status;

        // 1 - bootstrap LIRICAL.
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(genomeBuild);
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + genomeBuild + "'");

        GenotypeLrProperties genotypeLrProperties = new GenotypeLrProperties(runConfiguration.pathogenicityThreshold, runConfiguration.defaultVariantBackgroundFrequency, runConfiguration.strict);
        Lirical lirical = LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .exomiserVariantDatabase(dataSection.exomiserDatabase)
                .genomeBuild(genomeBuildOptional.get())
                .backgroundVariantFrequency(dataSection.backgroundFrequencyFile)
                .setDiseaseDatabases(runConfiguration.useOrphanet
                        ? DiseaseDatabase.allKnownDiseaseDatabases()
                        : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER))
                .genotypeLrProperties(genotypeLrProperties)
                .transcriptDatabase(runConfiguration.transcriptDb)
                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
                .build();

        // 2 - prepare the simulation input.
        BenchmarkData benchmarkData = prepareBenchmarkData(lirical);
        AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);

        // 3 - run the analysis.
        LOGGER.info("Starting the analysis: {}", analysisOptions);
        LiricalAnalysisRunner analysisRunner = lirical.analysisRunner();
        AnalysisResults results = analysisRunner.run(benchmarkData.analysisData(), analysisOptions);
        LOGGER.info("Done!");

        // 4 - summarize the results.
        LOGGER.info("Writing results to {}", outputPath.toAbsolutePath());
        String phenopacketName = phenopacketPath.toFile().getName();
        String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
        writeResults(outputPath, benchmarkData, results, phenopacketName, backgroundVcf);
        return 0;
    }

    private BenchmarkData prepareBenchmarkData(Lirical lirical) throws LiricalParseException {
        LOGGER.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());
        PhenopacketData data = readPhenopacketData(phenopacketPath);

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        GenesAndGenotypes genes;
        if (phenotypeOnly) {
            // We omit the VCF even if provided.
            if (vcfPath != null)
                LOGGER.warn("The provided VCF file will not be used in `--phenotype-only` mode");
            genes = GenesAndGenotypes.empty();
        } else {
            if (vcfPath == null) // No VCF file.
                genes = GenesAndGenotypes.empty();
            else {
                // Annotate the causal variants found in the phenopacket.
                FunctionalVariantAnnotator annotator = lirical.functionalVariantAnnotator();
                VariantMetadataService metadataService = lirical.variantMetadataService();
                List<LiricalVariant> variants = new ArrayList<>();
                for (GenotypedVariant variant : data.getVariants()) {
                    List<TranscriptAnnotation> annotations = annotator.annotate(variant.variant());
                    List<VariantEffect> effects = annotations.stream()
                            .map(TranscriptAnnotation::getVariantEffects)
                            .flatMap(Collection::stream)
                            .distinct()
                            .toList();
                    VariantMetadata metadata = metadataService.metadata(variant.variant(), effects);

                    LiricalVariant lv = LiricalVariant.of(variant, annotations, metadata);
                    variants.add(lv);
                }

                // Read the VCF file.
                genes = readVariantsFromVcfFile(vcfPath, variants, lirical.variantParserFactory().orElse(null));
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
            LOGGER.info("Success!");
        } catch (Exception e) {
            LOGGER.info("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.info("Success!");
            } catch (IOException e) {
                LOGGER.info("Unable to parser as v1 phenopacket.");
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

    private static GenesAndGenotypes readVariantsFromVcfFile(Path vcfPath,
                                                             List<LiricalVariant> causal,
                                                             VariantParserFactory parserFactory) throws LiricalParseException {
        if (parserFactory == null) {
            LOGGER.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return GenesAndGenotypes.empty();
        }
        try (VariantParser variantParser = parserFactory.forPath(vcfPath)) {
            // Read variants
            LOGGER.info("Reading variants from {}.", vcfPath.toAbsolutePath());
            AtomicInteger counter = new AtomicInteger();
            LinkedList<LiricalVariant> variants = variantParser.variantStream()
                    .peek(logProgress(counter))
                    .collect(Collectors.toCollection(LinkedList::new));
            LOGGER.info("Read {} variants.", NF.format(variants.size()));

            LOGGER.info("Adding {} causal variants.", causal.size());
            variants.addAll(causal);

            // Group variants by Entrez ID.
            Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
            for (LiricalVariant variant : variants) {
                variant.annotations().stream()
                        .map(TranscriptAnnotation::getGeneId)
                        .distinct()
                        .forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new LinkedList<>()).add(variant));
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

    private static Consumer<LiricalVariant> logProgress(AtomicInteger counter) {
        return v -> {
            int current = counter.incrementAndGet();
            if (current % 5000 == 0)
                LOGGER.info("Read {} variants", NF.format(current));
        };
    }

    private AnalysisOptions prepareAnalysisOptions(Lirical lirical) {
        LOGGER.debug("Using uniform pretest disease probabilities.");
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(lirical.phenotypeService().diseases());
        return AnalysisOptions.of(runConfiguration.globalAnalysisMode, pretestDiseaseProbability, runConfiguration.disregardDiseaseWithNoDeleteriousVariants);
    }


    private static void writeResults(Path outputPath,
                              BenchmarkData benchmarkData,
                              AnalysisResults results,
                              String phenopacketName,
                              String backgroundVcfName) throws IOException {
        try (BufferedWriter writer = openWriter(outputPath);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "background_vcf", "sample_id", "rank", "is_causal", "disease_id", "post_test_proba");
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
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

}
