package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.analysis.ProgressReporter;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.background.CustomBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class that describes data and configuration sections of the CLI, and contains common functionalities.
 */
abstract class LiricalConfigurationCommand extends BaseCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalConfigurationCommand.class);

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory = null;

            @CommandLine.Option(names = {"-e", "--exomiser"},
                description = {"Path to the Exomiser variant database.", "DEPRECATED - use -e19 or -e38 instead"})
        public Path exomiserDatabase = null;

        @CommandLine.Option(names = {"-e19", "--exomiser-hg19"},
                description = "Path to the Exomiser variant database for hg19.")
        public Path exomiserHg19Database = null;

        @CommandLine.Option(names = {"-e38", "--exomiser-hg38"},
                description = "Path to the Exomiser variant database for hg38.")
        public Path exomiserHg38Database = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        public Path backgroundFrequencyFile = null;
    }


    // ---------------------------------------------- CONFIGURATION ----------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Configuration options:%n")
    public RunConfiguration runConfiguration = new RunConfiguration();

    public static class RunConfiguration {
        /**
         * If global is set to true, then LIRICAL will not discard candidate diseases with no known disease gene or
         * candidates for which no predicted pathogenic variant was found in the VCF.
         */
        @CommandLine.Option(names = {"-g", "--global"},
                description = "Global analysis (default: ${DEFAULT-VALUE}).")
        public boolean globalAnalysisMode = false;

        @CommandLine.Option(names = {"--ddndv"},
                description = "Disregard a disease if no deleterious variants are found in the gene associated with the disease. "
                        + "Used only if running with a VCF file. (default: ${DEFAULT-VALUE})")
        public boolean disregardDiseaseWithNoDeleteriousVariants = true;

        @CommandLine.Option(names = {"--transcript-db"},
                paramLabel = "{REFSEQ,UCSC}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        public TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        public boolean useOrphanet = false;

        @CommandLine.Option(names = {"--strict"},
                description = "Use strict penalties if the genotype does not match the disease model in terms " +
                        "of number of called pathogenic alleles. (default: ${DEFAULT-VALUE}).")
        public boolean useStrictPenalties = false;

        @CommandLine.Option(names = {"--variant-background-frequency"},
                description = {
                "Default frequency of called-pathogenic variants in the general population (gnomAD).",
                        "In the vast majority of cases, we can derive this information from gnomAD.",
                        "This constant is used if for whatever reason, data was not available for a gene.",
                        "(default: ${DEFAULT-VALUE})."})
        public double defaultVariantBackgroundFrequency = 0.1;

        @CommandLine.Option(names = {"--pathogenicity-threshold"},
                description = "Variant with greater pathogenicity score is considered deleterious (default: ${DEFAULT-VALUE}).")
        public float pathogenicityThreshold = .8f;

        // REMOVE(v2.0.0)
        @Deprecated(forRemoval = true, since = "2.0.0-RC2")
        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = {
                "Variant with greater allele frequency in at least one population is considered common.",
                        "NOTE: the option has been DEPRECATED"
        })
        public float defaultAlleleFrequency = Float.NaN;
    }

    protected List<String> checkInput() {
        List<String> errors = new LinkedList<>();
        // resources
        if (dataSection.liricalDataDirectory == null) {
            Path codeHomeParent = codeHomeDir();
            LOGGER.debug("Data directory is unset, searching in the code home directory at: {}", codeHomeParent);
            Path codeHomeDataDir = codeHomeParent.resolve("data");
            if (Files.isDirectory(codeHomeDataDir)) {
                LOGGER.debug("Found data folder at: {}", codeHomeDataDir.toAbsolutePath());
                dataSection.liricalDataDirectory = codeHomeDataDir;
            } else {
                String msg = "Path to Lirical data directory must be provided via `-d | --data` option";
                LOGGER.error(msg);
                errors.add(msg);
            }
        }

        // Obsolete options must/should not be used
        if (dataSection.exomiserDatabase != null) {
            // Check the obsolete `-e | --exomiser` option is not being used.
            String msg = "`-e | --exomiser` option has been deprecated. Use `-e19 or -e38` to set paths to Exomiser variant databases for hg19 and hg38, respectively";
            LOGGER.error(msg);
            errors.add(msg);
        }

        if (!Float.isNaN(runConfiguration.defaultAlleleFrequency)) {
            String msg = "`--default-allele-frequency` option has been deprecated.";
            LOGGER.error(msg);
        }

        Optional<GenomeBuild> genomeBuild = GenomeBuild.parse(getGenomeBuild());
        if (genomeBuild.isEmpty()) {
            // We must have genome build!
            String msg = "Genome build must be set";
            LOGGER.error(msg);
            errors.add(msg);
        } else {
            // Check Exomiser db seem to match the genome build.
            switch (genomeBuild.get()) {
                case HG19 -> {
                    if (dataSection.exomiserHg19Database == null && dataSection.exomiserHg38Database != null) {
                        String msg = "Genome build set to %s but Exomiser variant database is set for %s: %s".formatted(GenomeBuild.HG19, GenomeBuild.HG38, dataSection.exomiserHg38Database.toAbsolutePath());
                        LOGGER.error(msg);
                        errors.add(msg);
                    }
                }
                case HG38 -> {
                    if (dataSection.exomiserHg38Database == null && dataSection.exomiserHg19Database != null) {
                        String msg = "Genome build set to %s but Exomiser variant database is set for %s: %s".formatted(GenomeBuild.HG38, GenomeBuild.HG19, dataSection.exomiserHg19Database.toAbsolutePath());
                        LOGGER.error(msg);
                        errors.add(msg);
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Build {@link Lirical} for a {@link GenomeBuild} based on {@link DataSection} and {@link RunConfiguration} sections.
     */
    protected Lirical bootstrapLirical(GenomeBuild genomeBuild) throws LiricalDataException {
        LiricalBuilder builder = LiricalBuilder.builder(dataSection.liricalDataDirectory);

        switch (genomeBuild) {
            case HG19 -> {
                if (dataSection.exomiserHg19Database != null)
                    builder.exomiserVariantDbPath(GenomeBuild.HG19, dataSection.exomiserHg19Database);
            }
            case HG38 -> {
                if (dataSection.exomiserHg38Database != null)
                    builder.exomiserVariantDbPath(GenomeBuild.HG38, dataSection.exomiserHg38Database);
            }
        }

        if (dataSection.backgroundFrequencyFile != null) {
            LOGGER.debug("Using custom deleterious variant background frequency file at {} for {}",
                    dataSection.backgroundFrequencyFile.toAbsolutePath(),
                    genomeBuild);
            Map<GenomeBuild, Path> backgroundFrequencies = Map.of(genomeBuild, dataSection.backgroundFrequencyFile);
            CustomBackgroundVariantFrequencyServiceFactory backgroundFreqFactory = CustomBackgroundVariantFrequencyServiceFactory.of(backgroundFrequencies);
            builder.backgroundVariantFrequencyServiceFactory(backgroundFreqFactory);
        }

        return builder.build();
    }

    protected abstract String getGenomeBuild();

    protected GenomeBuild parseGenomeBuild(String genomeBuild) throws LiricalDataException {
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(genomeBuild);
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + genomeBuild + "'");
        return genomeBuildOptional.get();
    }

    protected AnalysisOptions prepareAnalysisOptions(Lirical lirical, GenomeBuild genomeBuild, TranscriptDatabase transcriptDb) {
        AnalysisOptions.Builder builder = AnalysisOptions.builder();

        // Genome build
        builder.genomeBuild(genomeBuild);

        // Tx databases
        builder.transcriptDatabase(transcriptDb);

        // Disease databases
        Set<DiseaseDatabase> diseaseDatabases = runConfiguration.useOrphanet
                ? DiseaseDatabase.allKnownDiseaseDatabases()
                : Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);
        String usedDatabasesSummary = diseaseDatabases.stream().map(DiseaseDatabase::name).collect(Collectors.joining(", ", "[", "]"));
        LOGGER.debug("Using disease databases {}", usedDatabasesSummary);
        builder.setDiseaseDatabases(diseaseDatabases);

        // The rest..
        LOGGER.debug("Variants with pathogenicity score >{} are considered deleterious", runConfiguration.pathogenicityThreshold);
        builder.variantDeleteriousnessThreshold(runConfiguration.pathogenicityThreshold);

        LOGGER.debug("Variant background frequency is set to {}", runConfiguration.defaultVariantBackgroundFrequency);
        builder.defaultVariantBackgroundFrequency(runConfiguration.defaultVariantBackgroundFrequency);

        LOGGER.debug("Using strict penalties if the genotype does not match the disease model " +
                "in terms of number of called pathogenic alleles? {}", runConfiguration.useStrictPenalties);
        builder.useStrictPenalties(runConfiguration.useStrictPenalties);

        LOGGER.debug("Running in global mode? {}", runConfiguration.globalAnalysisMode);
        builder.useGlobal(runConfiguration.globalAnalysisMode);

        LOGGER.debug("Using uniform pretest disease probabilities.");
        List<String> diseaseDatabasePrefixes = diseaseDatabases.stream()
                .map(DiseaseDatabase::prefix)
                .sorted()
                .toList();

        List<TermId> diseaseIds = lirical.phenotypeService().diseases().stream()
                .map(Identified::id)
                .filter(diseaseId -> Collections.binarySearch(diseaseDatabasePrefixes, diseaseId.getPrefix()) > 0)
                .toList();
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(diseaseIds);
        builder.pretestProbability(pretestDiseaseProbability);

        LOGGER.debug("Disregarding diseases with no deleterious variants? {}", runConfiguration.disregardDiseaseWithNoDeleteriousVariants);
        builder.disregardDiseaseWithNoDeleteriousVariants(runConfiguration.disregardDiseaseWithNoDeleteriousVariants);

        return builder.build();
    }

    protected static GenesAndGenotypes readVariantsFromVcfFile(String sampleId,
                                                               Path vcfPath,
                                                               GenomeBuild genomeBuild,
                                                               TranscriptDatabase transcriptDatabase,
                                                               VariantParserFactory parserFactory) throws LiricalParseException {
        if (parserFactory == null) {
            LOGGER.warn("Cannot process the provided VCF file {}, resources are not set.", vcfPath.toAbsolutePath());
            return GenesAndGenotypes.empty();
        }

        Optional<VariantParser> parser = parserFactory.forPath(vcfPath, genomeBuild, transcriptDatabase);
        if (parser.isEmpty()) {
            LOGGER.warn("Cannot obtain parser for processing the VCF file {} with {} {} due to missing resources",
                    vcfPath.toAbsolutePath(), genomeBuild, transcriptDatabase);
            return GenesAndGenotypes.empty();
        }
        List<LiricalVariant> variants;
        try (VariantParser variantParser = parser.get()) {
            // Ensure the VCF file contains the sample
            if (!variantParser.sampleNames().contains(sampleId))
                throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
            LOGGER.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

            // Read variants
            LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter();
            variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }

        return prepareGenesAndGenotypes(variants);
    }

    protected static GenesAndGenotypes prepareGenesAndGenotypes(List<LiricalVariant> variants) {
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
    }

    protected static void reportElapsedTime(long startTime, long stopTime) {
        int elapsedTime = (int)((stopTime - startTime)*(1.0)/1000);
        if (elapsedTime > 3599) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            int elapsedHours = elapsedTime/3600;
            LOGGER.info(String.format("Elapsed time %d:%2d%2d",elapsedHours,elapsedMinutes,elapsedSeconds));
        }
        else if (elapsedTime>59) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            LOGGER.info(String.format("Elapsed time %d min, %d sec",elapsedMinutes,elapsedSeconds));
        } else {
            LOGGER.info("Elapsed time " + (stopTime - startTime) * (1.0) / 1000 + " seconds.");
        }
    }

    private static Path codeHomeDir() {
        String codePath = LiricalConfigurationCommand.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        LOGGER.debug("Found code path at {}", codePath);
        return Path.of(codePath).toAbsolutePath().getParent();
    }

}
