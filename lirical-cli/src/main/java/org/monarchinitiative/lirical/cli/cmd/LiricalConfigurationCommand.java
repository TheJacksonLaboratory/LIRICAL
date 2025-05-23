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
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.sanitize.*;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserResources;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.background.CustomBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base class that describes data and configuration sections of the CLI, and contains common functionalities.
 */
abstract class LiricalConfigurationCommand extends BaseCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalConfigurationCommand.class);
    private static final Pattern EXOMISER_VARIANT_DB = Pattern.compile("(?<date>\\d{4})_(?<build>(hg19|hg38))_variants\\.mv\\.db", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXOMISER_CLINVAR_DB = Pattern.compile("(?<date>\\d{4})_(?<build>(hg19|hg38))_clinvar\\.mv\\.db", Pattern.CASE_INSENSITIVE);
    protected static final String UNKNOWN_VERSION_PLACEHOLDER = "UNKNOWN VERSION";
    private static final Pattern DISEASE_ID = Pattern.compile("^\\w+:\\w+$");

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory = null;

        @CommandLine.Option(names = {"-ed19", "--exomiser-hg19-dir"},
                description = "Path to the Exomiser data directory for hg19.")
        public Path exomiserHg19DataDirectory = null;

        @CommandLine.Option(names = {"-e19", "--exomiser-hg19", "--exomiser-hg19-variants"},
                description = "Path to the Exomiser variant database for hg19.")
        public Path exomiserHg19Database = null;

        @CommandLine.Option(names = {"-c19", "--exomiser-hg19-clinvar"},
                description = "Path to the Exomiser ClinVar database for hg19.")
        public Path exomiserHg19ClinVarDatabase = null;

        @CommandLine.Option(names = {"-ed38", "--exomiser-hg38-dir"},
                description = "Path to the Exomiser data directory for hg38.")
        public Path exomiserHg38DataDirectory = null;

        @CommandLine.Option(names = {"-e38", "--exomiser-hg38", "--exomiser-hg38-variants"},
                description = "Path to the Exomiser variant database for hg38.")
        public Path exomiserHg38Database = null;

        @CommandLine.Option(names = {"-c38", "--exomiser-hg38-clinvar"},
                description = "Path to the Exomiser ClinVar database for hg38.")
        public Path exomiserHg38ClinVarDatabase = null;

        @CommandLine.Option(names = {"-b", "--background"},
                description = "Path to non-default background frequency file.")
        public Path backgroundFrequencyFile = null;

        @CommandLine.Option(names = "--parallelism",
                description = {
                        "The number of workers/threads to use.",
                        "The value must be a positive integer.",
                        "Default: ${DEFAULT-VALUE}"})
        public int parallelism = 1;
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

        @CommandLine.Option(names = {"--sdwndv"},
                description = {
                        "Include diseases even if no deleterious variants are found in the gene associated with the disease.",
                        "Only applicable to the HTML report when running with a VCF file (genotype-aware mode).",
                        "(default: ${DEFAULT-VALUE})"
                })
        public boolean showDiseasesWithNoDeleteriousVariants = false;

        @CommandLine.Option(names = {"--transcript-db"},
                paramLabel = "{REFSEQ,REFSEQ_CURATED,UCSC,ENSEMBL}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        public TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        public boolean useOrphanet = false;

        @CommandLine.Option(names = {"--target-diseases"},
                split = ",",
                paramLabel = "disease",
                description = {
                    "Limit the analysis to the provided disease IDs. ",
                    "(default: analyze all diseases)."
                }
        )
        public List<String> targetDiseases= null;

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

        @CommandLine.Option(names = {"--validation-policy"},
                paramLabel = "{STRICT, LENIENT, MINIMAL}",
                description = {"Validation policy for the analysis", "(default: ${DEFAULT-VALUE})."})
        public ValidationPolicy validationPolicy = ValidationPolicy.MINIMAL;

        @CommandLine.Option(names = {"--dry-run"},
                description = {
                        "Validate the input, report potential issues, and exit without running the analysis.",
                        "(default ${DEFAULT-VALUE})"
                })
        public boolean dryRun = false;
    }

    protected List<String> checkInput() {
        List<String> errors = new ArrayList<>();

        // resources
        if (dataSection.liricalDataDirectory == null) {
            LOGGER.debug("Data directory is unset, searching next to the LIRICAL file");
            Path codeHomeParent = codeHomeDir();
            Path codeHomeDataDir = codeHomeParent.resolve("data");
            if (Files.isDirectory(codeHomeDataDir)) {
                dataSection.liricalDataDirectory = codeHomeDataDir;
            } else {
                String msg = "Path to LIRICAL data directory must be provided via `-d | --data` option";
                errors.add(msg);
            }
        }
        if (dataSection.liricalDataDirectory != null)
            LOGGER.info("Using data folder at {}", dataSection.liricalDataDirectory.toAbsolutePath());

        LOGGER.debug("Analysis input validation policy: {}", runConfiguration.validationPolicy.name());

        Optional<GenomeBuild> genomeBuild = GenomeBuild.parse(getGenomeBuild());
        if (genomeBuild.isEmpty()) {
            // We must have genome build!
            String msg = "Genome build must be set";
            errors.add(msg);
        } else {
            // Check Exomiser resources match the genome build.
            switch (genomeBuild.get()) {
                case HG19 -> {
                    if (dataSection.exomiserHg19DataDirectory == null
                            && (dataSection.exomiserHg19Database == null || dataSection.exomiserHg19ClinVarDatabase == null)
                    ) {
                        errors.add("Genome build set to HG19 but Exomiser resources are unset");
                    }
                }
                case HG38 -> {
                    if (dataSection.exomiserHg38DataDirectory == null
                            && (dataSection.exomiserHg38Database == null || dataSection.exomiserHg38ClinVarDatabase == null)
                    ) {
                        errors.add("Genome build set to HG38 but Exomiser resources are unset");
                    }
                }
            }
        }

        if (dataSection.parallelism <= 0) {
            String msg = "Parallelism must be a positive integer but was %d".formatted(dataSection.parallelism);
            errors.add(msg);
        }

        if (runConfiguration.targetDiseases != null
                && !runConfiguration.targetDiseases.stream()
                .allMatch(DISEASE_ID.asMatchPredicate())) {
            String failures = runConfiguration.targetDiseases.stream()
                    .filter(Predicate.not(DISEASE_ID.asMatchPredicate()))
                    .collect(Collectors.joining(","));
            String msg = "One or more target disease IDs do not look like a compact URI: %s".formatted(failures);
            errors.add(msg);
        }

        return errors;
    }

    /**
     * Build {@link Lirical} for a {@link GenomeBuild} based on {@link DataSection} and {@link RunConfiguration} sections.
     */
    protected Lirical bootstrapLirical(GenomeBuild genomeBuild) throws LiricalDataException {
        LiricalBuilder builder = LiricalBuilder.builder(dataSection.liricalDataDirectory);

        // Deal with Exomiser resources.
        ExomiserResources resources = switch (genomeBuild) {
            case HG19 -> loadExomiserResources(
                    GenomeBuild.HG19,
                    dataSection.exomiserHg19DataDirectory,
                    dataSection.exomiserHg19Database,
                    dataSection.exomiserHg19ClinVarDatabase
            );
            case HG38 -> loadExomiserResources(
                    GenomeBuild.HG38,
                    dataSection.exomiserHg38DataDirectory,
                    dataSection.exomiserHg38Database,
                    dataSection.exomiserHg38ClinVarDatabase
            );
        };
        builder.exomiserResources(genomeBuild, resources);

        // Background variant frequencies
        if (dataSection.backgroundFrequencyFile != null) {
            LOGGER.debug("Using custom deleterious variant background frequency file at {} for {}",
                    dataSection.backgroundFrequencyFile.toAbsolutePath(),
                    genomeBuild);
            Map<GenomeBuild, Path> backgroundFrequencies = Map.of(genomeBuild, dataSection.backgroundFrequencyFile);
            CustomBackgroundVariantFrequencyServiceFactory backgroundFreqFactory = CustomBackgroundVariantFrequencyServiceFactory.of(backgroundFrequencies);
            builder.backgroundVariantFrequencyServiceFactory(backgroundFreqFactory);
        }

        return builder.shouldLoadOrpha2Gene(runConfiguration.useOrphanet)
                .parallelism(dataSection.parallelism)
                .build();
    }

    /**
     * Parse paths to Exomiser data directory, Exomiser allele database, and Exomiser ClinVar database into
     * {@link ExomiserResources} for a provided {@code genomeBuild}.
     *
     * @param genomeBuild                 a desired genome build.
     * @param exomiserDataDirectory       path to directory with Exomiser allele and ClinVar database files.
     * @param exomiserAlleleDatabasePath  path to Exomiser allele database file.
     * @param exomiserClinVarDatabasePath path to Exomiser ClinVar database file.
     * @throws LiricalDataException if one or both database paths are <code>null</code> or do not point to readable file.
     */
    private static ExomiserResources loadExomiserResources(
            GenomeBuild genomeBuild,
            Path exomiserDataDirectory,
            Path exomiserAlleleDatabasePath,
            Path exomiserClinVarDatabasePath
    ) throws LiricalDataException {
        // At the end of the function, we need to possess path to allele DB and ClinVar DB files.
        //
        Path exomiserAlleleDb = null;
        Path exomiserClinVarDb = null;
        if (exomiserDataDirectory != null) {
            if (Files.isDirectory(exomiserDataDirectory)) {
                LOGGER.debug("Using exomiser data directory at {}", exomiserDataDirectory.toAbsolutePath());

                // We check that the path is a directory, hence no inspection.
                // This can still fail in case of an IO error.
                //noinspection DataFlowIssue
                for (String file : exomiserDataDirectory.toFile().list()) {
                    Matcher alleleMatcher = EXOMISER_VARIANT_DB.matcher(file);
                    if (alleleMatcher.matches()) {
                        Path path = exomiserDataDirectory.resolve(file);

                        String build = alleleMatcher.group("build");
                        if (!genomeBuild.name().equalsIgnoreCase(build)) {
                            LOGGER.warn(
                                    "Genome build {} is in use but Exomiser variant database seems to be for {}: {}",
                                    genomeBuild, build, path.toAbsolutePath()
                            );
                        }
                        LOGGER.debug("Using Exomiser variant database at {}", path.toAbsolutePath());
                        exomiserAlleleDb = path;
                        continue;
                    }

                    Matcher clinvarMatcher = EXOMISER_CLINVAR_DB.matcher(file);
                    if (clinvarMatcher.matches()) {
                        Path path = exomiserDataDirectory.resolve(file);
                        String build = clinvarMatcher.group("build");
                        if (!genomeBuild.name().equalsIgnoreCase(build)) {
                            LOGGER.warn(
                                    "Genome build {} is in use but Exomiser ClinVar database seems to be for {}: {}",
                                    genomeBuild, build, path.toAbsolutePath()
                            );
                        }
                        LOGGER.debug("Using Exomiser ClinVar database at {}", path.toAbsolutePath());
                        exomiserClinVarDb = path;
                    }

                    if (exomiserAlleleDb != null && exomiserClinVarDb != null)
                        // We found both allele and ClinVar database files
                        break;
                }
            }
        }

        // Providing explicit database paths overrides paths from the Exomiser data directory
        if (exomiserAlleleDatabasePath != null) {
            if (exomiserAlleleDb != null) {
                LOGGER.debug("Using {} instead of {} due to CLI option override",
                        exomiserAlleleDatabasePath.toAbsolutePath(), exomiserAlleleDb);
            } else {
                LOGGER.debug("Using Exomiser variant database at {}", exomiserAlleleDatabasePath.toAbsolutePath());
            }
            exomiserAlleleDb = exomiserAlleleDatabasePath;
        }
        if (exomiserClinVarDatabasePath != null) {
            if (exomiserClinVarDb != null) {
                LOGGER.debug("Using {} instead of {} due to CLI option override",
                        exomiserClinVarDatabasePath.toAbsolutePath(), exomiserAlleleDb);
            } else {
                LOGGER.debug("Using exomiser ClinVar database at {}", exomiserClinVarDatabasePath.toAbsolutePath());
            }
            exomiserClinVarDb = exomiserClinVarDatabasePath;
        }

        // Ensure paths point to readable files or complain otherwise.
        if ((exomiserAlleleDb != null && Files.isReadable(exomiserAlleleDb)) && (exomiserClinVarDb != null && Files.isReadable(exomiserClinVarDb))) {
            return new ExomiserResources(exomiserAlleleDb, exomiserClinVarDb);
        } else {
            StringBuilder builder = new StringBuilder("Exomiser database files for ")
                    .append(genomeBuild)
                    .append(" are misconfigured.");
            if (exomiserAlleleDb == null)
                builder.append(" Allele database was not provided.");
            else {
                if (!Files.isReadable(exomiserAlleleDb))
                    builder.append(" Allele database at ").append(exomiserAlleleDb).append(" is not readable.");
            }
            if (exomiserClinVarDb == null)
                builder.append("ClinVar database was not provided. ");
            else {
                if (!Files.isReadable(exomiserClinVarDb))
                    builder.append(" ClinVar database at ").append(exomiserClinVarDb).append(" is not readable.");
            }
            throw new LiricalDataException(builder.toString());
        }
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
        List<DiseaseDatabase> diseaseDatabases;
        if (runConfiguration.useOrphanet) {
            diseaseDatabases = List.of(DiseaseDatabase.OMIM, DiseaseDatabase.ORPHANET, DiseaseDatabase.DECIPHER);
            LOGGER.debug("Using disease databases [{}, {}, {}]", DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER, DiseaseDatabase.ORPHANET);
        } else {
            diseaseDatabases = List.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);
            LOGGER.debug("Using disease databases [{}, {}]", DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);
        }
        builder.addDiseaseDatabases(diseaseDatabases);

        if (runConfiguration.targetDiseases != null) {
            String usedDiseaseIds = runConfiguration.targetDiseases.stream().collect(Collectors.joining(", ", "[", "]"));
            LOGGER.debug("Limiting the analysis to the following diseases: {}", usedDiseaseIds);
            List<TermId> targetDiseases = runConfiguration.targetDiseases.stream()
                    .map(TermId::of)
                    .toList();
            builder.setTargetDiseases(targetDiseases);
        }

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
                .filter(diseaseId -> diseaseDatabasePrefixes.contains(diseaseId.getPrefix()))
                .toList();
        PretestDiseaseProbability pretestDiseaseProbability = PretestDiseaseProbabilities.uniform(diseaseIds);
        builder.pretestProbability(pretestDiseaseProbability);

        LOGGER.debug("Showing diseases with no deleterious variants in the gene associated with the disease? {}", runConfiguration.showDiseasesWithNoDeleteriousVariants);
        builder.includeDiseasesWithNoDeleteriousVariants(!runConfiguration.showDiseasesWithNoDeleteriousVariants);

        return builder.build();
    }

    protected static SampleIdAndGenesAndGenotypes readVariantsFromVcfFile(
            String sampleId,
            Path vcfPath,
            GenomeBuild genomeBuild,
            TranscriptDatabase transcriptDatabase,
            VariantParserFactory parserFactory
    ) throws LiricalParseException {
        LOGGER.debug("Getting variant parser to parse a VCF file using {} assembly and {} transcripts", genomeBuild, transcriptDatabase);
        Optional<VariantParser> parser = parserFactory.forPath(vcfPath, genomeBuild, transcriptDatabase);
        if (parser.isEmpty()) {
            throw new LiricalParseException(
                    "Cannot obtain parser for processing the VCF file %s with %s %s due to missing resources"
                            .formatted(vcfPath.toAbsolutePath(), genomeBuild, transcriptDatabase)
            );
        }

        try (VariantParser variantParser = parser.get()) {
            Collection<String> sampleNames = variantParser.sampleNames();
            String usedId = validateSampleId(sampleId, vcfPath, sampleNames);

            // Read variants
            LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
            ProgressReporter progressReporter = new ProgressReporter();
            List<LiricalVariant> variants = variantParser.variantStream()
                    .peek(v -> progressReporter.log())
                    .toList();
            progressReporter.summarize();

            GenesAndGenotypes genesAndGenotypes = GenesAndGenotypes.fromVariants(sampleNames, variants);
            return new SampleIdAndGenesAndGenotypes(usedId, genesAndGenotypes);
        } catch (Exception e) {
            throw new LiricalParseException(e);
        }
    }

    /**
     * Check if the VCF file is a single-sample or multi-sample VCF file with the given sample ID. If `sampleId` is
     * {@code null}, we can only accept a single-sample VCF, mainly as a convenience.
     *
     * @throws LiricalParseException if the VCF includes no sample data, the sample is not present,
     *                               or it is a multi-sample file and the sample ID is unset.
     */
    private static String validateSampleId(String sampleId,
                                           Path vcfPath,
                                           Collection<String> sampleNames) throws LiricalParseException {
        if (sampleNames.isEmpty())
            throw new LiricalParseException("No samples found in the VCF file at '" + vcfPath.toAbsolutePath() + '\'');
        if (sampleId == null) {
            if (sampleNames.size() != 1) {
                // The user did not provide the sample ID. We can proceed if the variant source contains 1 subject only.
                throw new LiricalParseException(("The VCF file includes %d samples but the ID of the index sample " +
                        "is unset. Set the sample ID if VCF reports >1 sample").formatted(sampleNames.size()));
            } else {
                String inferredId = sampleNames.iterator().next();
                LOGGER.info("Sample ID is unset. However, since the VCF file includes just a single sample ("
                        + inferredId + "), we will proceed with that one");
                return inferredId;
            }
        } else if (!sampleNames.contains(sampleId)) {
            String included = sampleNames.stream().collect(Collectors.joining(", ", "{", "}"));
            throw new LiricalParseException(
                    "The VCF at '%s' includes samples %s but it does not include the index sample %s"
                            .formatted(vcfPath.toAbsolutePath(), included, sampleId)
            );
        } else {
            LOGGER.debug("Found the index sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());
        }
        return sampleId;
    }

    protected static String summarizeSanitationResult(SanitationResult sanitationResult) {
        if (sanitationResult.hasErrorOrWarnings()) {
            Map<SanityLevel, List<SanityIssue>> byLevel = sanitationResult.issues().stream()
                    .collect(Collectors.groupingBy(SanityIssue::level));

            List<SanityIssue> errors = byLevel.getOrDefault(SanityLevel.ERROR, List.of());
            List<SanityIssue> warnings = byLevel.getOrDefault(SanityLevel.WARNING, List.of());

            List<String> lines = new ArrayList<>();
            lines.add("Input sanitation found %d errors and %d warnings".formatted(errors.size(), warnings.size()));
            if (!errors.isEmpty()) {
                lines.add(" Errors \uD83D\uDE31");
                for (SanityIssue issue : errors) {
                    lines.add(" - %s. %s".formatted(issue.message(), issue.solution()));
                }
            }

            if (!warnings.isEmpty()) {
                lines.add(" Warnings \uD83D\uDE27");
                for (SanityIssue issue : warnings) {
                    lines.add(" - %s. %s".formatted(issue.message(), issue.solution()));
                }
            }

            return String.join(System.lineSeparator(), lines);
        } else {
            return "Input sanitation found no issues";
        }
    }

    protected String figureOutExomiserPath() {
        if (dataSection.exomiserHg19Database == null && dataSection.exomiserHg38Database == null) {
            return "";
        } else {
            if (dataSection.exomiserHg19Database == null) {
                return dataSection.exomiserHg38Database.toAbsolutePath().toString();
            } else {
                return dataSection.exomiserHg19Database.toAbsolutePath().toString();
            }
        }
    }

    protected InputSanitizer selectSanitizer(InputSanitizerFactory factory) {
        return switch (runConfiguration.validationPolicy) {
            case STRICT, LENIENT -> factory.forType(SanitizerType.COMPREHENSIVE);
            case MINIMAL -> factory.forType(SanitizerType.MINIMAL);
        };
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
        LOGGER.info("Running LIRICAL from {}", codePath);
        return Path.of(codePath).toAbsolutePath().getParent();
    }

    protected record SampleIdAndGenesAndGenotypes(String sampleId, GenesAndGenotypes genesAndGenotypes) {
    }

}
