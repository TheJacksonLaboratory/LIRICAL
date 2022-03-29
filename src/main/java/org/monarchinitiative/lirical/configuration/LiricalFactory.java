package org.monarchinitiative.lirical.configuration;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.jannovar.InvalidFileFormatException;
import org.monarchinitiative.exomiser.core.genome.jannovar.JannovarDataProtoSerialiser;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.io.GenotypeDataIngestor;
import org.monarchinitiative.lirical.io.YamlParser;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.vcf.SimpleVariant;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Not a full implementation of the factory pattern but rather a convenience class to create objects of various
 * classes that we need as singletons with the various commands.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @deprecated use {@link LiricalConfiguration}
 */
@Deprecated
public class LiricalFactory {
    private static final Logger logger = LoggerFactory.getLogger(LiricalFactory.class);
    private static final String DEFAULT_OUTFILE_PREFIX = "lirical";

    /**
     * UCSC or RefSeq.
     */
    private final TranscriptDatabase transcriptdatabase;
    /**
     * Path to the VCF file that is to be evaluated.
     */
    private final Path vcfPath;
    /**
     * List of HPO terms (phenotypic abnormalities) observed in the person being evaluated.
     */
    private final List<TermId> hpoIdList;
    /**
     * List of HPO terms that were excluded in the person being evaluated.
     */
    private final List<TermId> negatedHpoIdList;
    /**
     * The directory with the Exomiser database and Jannovar transcript files.
     */
    private final Path exomiserPath;
    /**
     * Number of variants that were not removed because of the quality filter.
     */
    private int n_good_quality_variants = 0;
    /**
     * Number of variants that were removed because of the quality filter.
     */
    private int n_filtered_variants = 0;
    /**
     * Prefix for output files. For example, if outfilePrefix is ABC, then the HTML outfile would be ABC.html.
     */
    private final String outfilePrefix;
    /**
     * Path to the directory where the output files should be written (by default, this is null and the files are
     * written to the directory in which LIRICAL is run.
     */
    private final Path outdir;
    /**
     * LR threshold to include/show a candidate in the differential diagnosis. This option is incompatible with the
     * {@link #minDifferentials} option, at most one can be non-null
     */
    private LrThreshold lrThreshold;
    /**
     * Minimum number of differentials to show in detail in the HTML output. This option is incompatible with the
     * {@link #lrThreshold} option, at most one can be non-null
     */
    private MinDiagnosisCount minDifferentials;

    private final GenomeAssembly assembly;

    private final Ontology ontology;

    /**
     * If global is set to true, then LIRICAL will not discard candidate diseases with no known disease gene or
     * candidatesfor which no predicted pathogenic variant was found in the VCF.
     */
    private final boolean globalAnalysisMode;

    private String hpoVersion = "n/a";

    /**
     * An object representing the Exomiser database.
     */
    private final MVStore mvstore;

    /**
     * There are gene symbols returned by Jannovar for which we cannot find a geneId. This issues seems to be related
     * to the input files used by Jannovar from UCSC ( knownToLocusLink.txt.gz has links between ucsc ids, e.g.,
     * uc003fts.3, and NCBIGene ids (earlier known as locus link), e.g., 1370).
     */
    private Set<String> symbolsWithoutGeneIds;
    /**
     * Key: the TermId of a gene. Value. Its background frequency in the current genome build. This variable
     * is only initialized for runs with a VCF file.
     */
    private Map<TermId, Double> gene2backgroundFrequency = null;
    /**
     * Name of sample in VCF file, if any. The default value is n/a to indicate this field has not been initiatilized.
     */
    private String sampleName = "n/a";

    private final Map<TermId, Collection<TermId>> geneToDiseases;
    private final Map<TermId, Collection<GeneIdentifier>> diseaseToGenes;
    private final Map<TermId, String> geneToSymbol;
    private final JannovarData jannovarData;

    private LiricalFactory(Builder builder) {
        this.ontology = Objects.requireNonNull(builder.ontology, "HPO ontology must not be null");
        this.geneToDiseases = Objects.requireNonNull(builder.geneToDiseases, "Gene to diseases must not be null");
        this.diseaseToGenes = Objects.requireNonNull(builder.diseaseToGenes, "Disease to genes must not be null");
        this.geneToSymbol = Objects.requireNonNull(builder.geneToSymbol, "Gene to symbol must not be null");
        Map<String, String> ontologyMetainfo = ontology.getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.hpoVersion = ontologyMetainfo.get("data-version");
        }
        this.exomiserPath = builder.exomiserDataDir; // nullable
        if (exomiserPath == null) {
            // We don't have Exomiser resources
            mvstore = null;
            jannovarData = null;
        } else {
            // We don't have Exomiser resources
            // TODO - extract function?
            // Remove the trailing directory slash if any
            Path mvStorePath = exomiserPath.resolve("variants.mv.db");
            if (!Files.isRegularFile(mvStorePath)) {
                throw new LiricalRuntimeException("[FATAL] Could not find Exomiser database file/variants.mv.db at " + mvStorePath.toAbsolutePath());
            }

            mvstore = new MVStore.Builder()
                    .fileName(mvStorePath.toAbsolutePath().toString())
                    .readOnly()
                    .open();

            // TODO jannovar

            // Remove the trailing directory slash if any
            String fullpath = switch (builder.transcriptdatabase) {
                case REFSEQ -> {
                    String refseqfilename = null; // TODO - fix - String.format("%s_transcripts_refseq.ser", basename);
                    yield String.format("%s%s%s", exomiserPath, File.separator, refseqfilename);
                }
                case UCSC -> {
                    String ucscfilename = null; // TODO - fix - String.format("%s_transcripts_ucsc.ser", basename);
                    yield String.format("%s%s%s", exomiserPath, File.separator, ucscfilename);
                }
            };

            File f = new File(fullpath);
            if (!f.isFile()) {
                throw new LiricalRuntimeException("[FATAL] Could not find Jannovar transcript file at " + fullpath);
            }
            JannovarData jd = null;
            try {
                jd = JannovarDataProtoSerialiser.load(f.toPath());
            } catch (InvalidFileFormatException e) {
                logger.warn("Could not deserialize Jannovar file with Protobuf deserializer, trying legacy deserializer...");
            }
            if (jd == null) {
                try {
                    jd = new JannovarDataSerializer(fullpath).load();
                } catch (SerializationException e) {
                    logger.error("Could not deserialize Jannovar file with legacy deserializer...");
                    throw new LiricalRuntimeException(String.format("Could not load Jannovar data from %s (%s)",
                            fullpath, e.getMessage()));
                }
            }
            jannovarData = jd;
        }
        this.assembly = builder.getAssembly();
        if (assembly.equals(GenomeAssembly.HG19) || assembly.equals(GenomeAssembly.HG38)) {
            // This will set up UCSCS output URLs for variants
            SimpleVariant.setGenomeBuildForUrl(assembly);
        }
        Path backgroundFrequencyPath;
        if (builder.backgroundFrequencyPath != null) {
            backgroundFrequencyPath = builder.backgroundFrequencyPath;
        } else {
            // Note-- background files for hg19 and hg38 are stored in src/main/resources/background
            // and are included in the resources by the maven resource plugin
            if (assembly.equals(GenomeAssembly.HG19)) {
                backgroundFrequencyPath = Path.of("background/background-hg19.tsv");
            } else if (assembly.equals(GenomeAssembly.HG38)) {
                backgroundFrequencyPath = Path.of("background/background-hg38.tsv");
            } else {
                logger.error("Did not recognize genome assembly: {}", assembly);
                throw new LiricalRuntimeException("Did not recognize genome assembly: " + assembly);
            }
        }
        try (BufferedReader reader = Files.newBufferedReader(backgroundFrequencyPath)) {
            this.gene2backgroundFrequency = GenotypeDataIngestor.parse(reader);
        } catch (IOException e) {
            logger.error("Error during reading background frequency file at {}", backgroundFrequencyPath.toAbsolutePath());
            throw new LiricalRuntimeException("Error during reading background frequency file");
        }


        // by the time we get here, we are guaranteed that at once one of the following two
        // thresholds are non-null (see checkThresholds function in PrioritizeCommand.java).
        // We have also checked that if present, the threshold is in [0,1]
        // the YAML parser performs analogous checks.
        if (builder.minDifferentials == null) {
            this.minDifferentials = MinDiagnosisCount.notInitialized();
        } else {
            this.minDifferentials = MinDiagnosisCount.setToUserDefinedMinCount(builder.minDifferentials);
        }
        if (builder.lrThreshold == null) {
            this.lrThreshold = LrThreshold.notInitialized();
        } else {
            this.lrThreshold = LrThreshold.setToUserDefinedThreshold(builder.lrThreshold);
        }
        this.transcriptdatabase = builder.transcriptdatabase;
        this.vcfPath = builder.vcfPath;
        this.outfilePrefix = builder.outfilePrefix;
        this.outdir = Path.of(builder.outputDirectory);


        this.hpoIdList = builder.observedHpoTerms.stream().map(TermId::of).toList();
        this.negatedHpoIdList = builder.negatedHpoTerms.stream().map(TermId::of).toList();

        this.globalAnalysisMode = builder.global;
//        Set<DiseaseDatabase> desiredDatabasePrefixes;
//        if (builder.useOrphanet) {
//            desiredDatabasePrefixes = Set.of(DiseaseDatabase.ORPHANET);
//        } else {
//            desiredDatabasePrefixes = Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER);
//        }

    }

    public String getOutfilePrefix() {
        return outfilePrefix;
    }

    public Path getOutdir() {
        return outdir;
    }

    /**
     * @return an {@link LrThreshold} object representing the likelihood ratio threshold chosen by the user (or default).
     */
    public LrThreshold getLrThreshold() {
        return lrThreshold;
    }

    /**
     * @return a list of observed HPO terms (from the YAML/Phenopacket file)
     * @throws LiricalException if one of the terms is not in the HPO Ontology
     */
    public List<TermId> observedHpoTerms() throws LiricalException {
        for (TermId hpoId : hpoIdList) {
            if (!this.ontology.getTermMap().containsKey(hpoId)) {
                throw new LiricalException("Could not find HPO term " + hpoId.getValue() + " in ontology");
            }
        }
        return hpoIdList;
    }

    /**
     * @return a list of observed HPO terms (from the YAML/Phenopacket file)
     * @throws LiricalException if one of the terms is not in the HPO Ontology
     */
    public List<TermId> negatedHpoTerms() throws LiricalException {
        for (TermId hpoId : negatedHpoIdList) {
            if (!this.ontology.getTermMap().containsKey(hpoId)) {
                throw new LiricalException("Could not find HPO term " + hpoId.getValue() + " in ontology");
            }
        }
        return negatedHpoIdList;
    }


    /**
     * @return the genome assembly corresponding to the VCF file. Can be null.
     */
    public GenomeAssembly getAssembly() {
        return assembly;
    }

    /**
     * @return HpoOntology object.
     */
    public Ontology hpoOntology() {
        return ontology;
    }

    /**
     * returns "n/a if {@link #transcriptdatabase} was not initialized (should not happen).
     */
    public String transcriptdb() {
        return this.transcriptdatabase != null ? this.transcriptdatabase.toString() : "n/a";
    }

    public String getSampleName() {
        return sampleName;
    }

    public Optional<Path> getExomiserPath() {
        return Optional.ofNullable(this.exomiserPath);
    }

    public String getHpoVersion() {
        return hpoVersion;
    }

    public MinDiagnosisCount getMinDifferentials() {
        return this.minDifferentials;
    }

    public Path getVcfPath() {
        if (this.vcfPath == null)
            throw new LiricalRuntimeException("VCF path not initialized");
        return vcfPath;
    }

    public boolean hasVcf() {
        return this.vcfPath != null;
    }


    /**
     * @return MVStore object with Exomiser data on variant pathogenicity and frequency.
     */
    public Optional<MVStore> mvStore() {
        return Optional.ofNullable(mvstore);
    }


    /**
     * @return a multimap with key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123.
     */
    public Map<TermId, Collection<TermId>> gene2diseaseMultimap() {
        return geneToDiseases;
    }

    /**
     * @return multimap with key:disease CURIEs such as OMIM:600123; value: a collection of gene CURIEs such as NCBIGene:123.
     */
    public Map<TermId, Collection<GeneIdentifier>> disease2geneMultimap() {
        return diseaseToGenes;
    }

    /**
     * @return a map with key:a gene id, e.g., NCBIGene:2020; value: the corresponding gene symbol.
     */
    public Map<TermId, String> geneId2symbolMap() {
        return geneToSymbol;
    }


    private static Path getPathWithoutTrailingSeparatorIfPresent(String path) {
        String sep = File.separator;
        if (path.endsWith(sep)) {
            int i = path.lastIndexOf(sep);
            return Path.of(path.substring(0, i));
        } else {
            return Path.of(path);
        }
    }

    /**
     * Create a {@link GenotypeLikelihoodRatio} object that will be used to calculated genotype likelhood ratios.
     * A runtime exception will be thrown if the file cannot be found.
     *
     * @return a {@link GenotypeLikelihoodRatio} object
     */
    public GenotypeLikelihoodRatio getGenotypeLR() {
        boolean strict = !globalAnalysisMode;
        float pathoThreshold = 0.8f;
        GenotypeLikelihoodRatio.Options options = new GenotypeLikelihoodRatio.Options(pathoThreshold, strict);
        return new GenotypeLikelihoodRatio(gene2backgroundFrequency, options);
    }


    /**
     * Deserialize the Jannovar transcript data file that comes with Exomiser. Note that Exomiser
     * uses its own ProtoBuf serializetion and so we need to use its Deserializser. In case the user
     * provides a standard Jannovar serialzied file, we try the legacy deserializer if the protobuf
     * deserializer doesn't work.
     *
     * @return the object created by deserializing a Jannovar file.
     */
    public Optional<JannovarData> jannovarData() {
        return Optional.ofNullable(jannovarData);
    }

    /**
     * @return a map with key: a disease id (e.g., OMIM:654321) and key the corresponding {@link HpoDisease} object.
     */
    public Map<TermId, HpoDisease> diseaseMap(Ontology ontology) {
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    public Map<TermId, Gene2Genotype> getGene2GenotypeMap() {
        return getGene2GenotypeMap(getVcfPath());
    }

    public Set<String> getSymbolsWithoutGeneIds() {
        return symbolsWithoutGeneIds;
    }

    public Map<TermId, Gene2Genotype> getGene2GenotypeMap(Path vcfPath) {
        if (mvstore == null || jannovarData == null) {
            return Map.of();
        }
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcfPath,
                jannovarData,
                mvstore,
                getAssembly(),
                geneId2symbolMap());
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.sampleName = vcf2geno.getSamplename();
        this.n_filtered_variants = vcf2geno.getN_filtered_variants();
        this.n_good_quality_variants = vcf2geno.getN_good_quality_variants();
        this.symbolsWithoutGeneIds = vcf2geno.getSymbolsWithoutGeneIds();
        return genotypeMap;
    }

    /**
     * @return a string with today's date in the format yyyy/MM/dd.
     */
    public String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public int getN_good_quality_variants() {
        return n_good_quality_variants;
    }

    public int getN_filtered_variants() {
        return n_filtered_variants;
    }

    /**
     * If true, then LIRICAL will not discard candidate diseases with no known disease gene or
     * candidatesfor which no predicted pathogenic variant was found in the VCF.
     */
    public boolean global() {
        return globalAnalysisMode;
    }

    @Deprecated(forRemoval = true)
    public void qcExomiserFiles() {
        if (exomiserPath == null) {
            logger.error("Exomiser data directory is not set");
            throw new LiricalRuntimeException("Exomiser data directory is not set");
        }
        if (!Files.isDirectory(exomiserPath)) {
            logger.error("Could not find Exomiser data directory at {}", exomiserPath);
            throw new LiricalRuntimeException(String.format("Could not find Exomiser data directory at %s", exomiserPath));
        } else {
            logger.trace("Exomiser data: {}", exomiserPath.toAbsolutePath());
        }
//        File mvStoreFile = new File(this.mvStorePath);
//        if (!mvStoreFile.exists()) {
//            logger.error("Could not find Exomiser database file at {}", this.mvStorePath);
//            throw new LiricalRuntimeException(String.format("Could not find Exomiser database file at %s", mvStorePath));
//        }
    }

    /**
     * This method checks whether the background frequency data was initialized
     */
    private void qcBackgroundFrequency() {

        if (!this.gene2backgroundFrequency.isEmpty()) {
            logger.error("background frequency was not initialized ");
            throw new LiricalRuntimeException("background frequency was not initialized ");
        } else {
            logger.trace("Background frequency initialized for {} genes", this.gene2backgroundFrequency.size());
        }
    }


    public void qcYaml() {
        qcExomiserFiles();
        qcBackgroundFrequency();
    }

    /**
     * Perform Q/C of the input variables to try to ensure that the correct (matching) genome build is being used.
     */
    public void qcGenomeBuild() {
        String pathString = exomiserPath.toAbsolutePath().toString();
        if (this.assembly.equals(GenomeAssembly.HG19)) {
            if (!pathString.contains("hg19")) {
                throw new LiricalRuntimeException(String.format("Use of non-matching Exomiser database (%s) for genome assembly hg19. Consider adding the option -g/--genome <...>.", this.exomiserPath));
            }
        } else if (this.assembly.equals(GenomeAssembly.HG38)) {
            if (!pathString.contains("hg38")) {
                throw new LiricalRuntimeException(String.format("Use of non-matching Exomiser database (%s) for genome assembly hg38. Consider adding the option -g/--genome <...>.", this.exomiserPath));
            }
        } else {
            logger.trace("Genome assembly: {}", this.assembly);
        }
    }

    public void qcVcfFile() {
        if (vcfPath == null) {
            throw new LiricalRuntimeException("VCF file was not initialzed");
        }
        if (!Files.isRegularFile(vcfPath)) {
            throw new LiricalRuntimeException("We did not find a VCF file at \"" + vcfPath + "\"");
        }
        logger.trace("VCF File: {}", this.vcfPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A convenience Builder class for creating {@link LiricalFactory} objects
     */
    public static class Builder {
        private Ontology ontology = null;
        private Map<TermId, Collection<TermId>> geneToDiseases;
        private Map<TermId, Collection<GeneIdentifier>> diseaseToGenes;
        private Map<TermId, String> geneToSymbol;
        private Path exomiserDataDir = null;

        private Path backgroundFrequencyPath = null;
        private Path vcfPath = null;
        private GenomeAssembly genomeAssembly = null;
        private boolean global = false;
        // TODO - fix
        private boolean useOrphanet = false;
        private TranscriptDatabase transcriptdatabase = TranscriptDatabase.UCSC;
        private List<String> observedHpoTerms = List.of();
        private List<String> negatedHpoTerms = List.of();
        private String outfilePrefix = DEFAULT_OUTFILE_PREFIX;
        private String outputDirectory = null;
        private Double lrThreshold = null;
        private Integer minDifferentials = null;

        /**
         * If this constructor is used, the the build method will attempt to load the HPO
         * based on its file location in datadir. If it is not possible, we will die gracefully.
         */
        private Builder() {
        }

        public Builder ontology(Ontology ontology) {
            this.ontology = ontology;
            return this;
        }

        public Builder geneToDiseases(Map<TermId, Collection<TermId>> geneToDiseases) {
            this.geneToDiseases = geneToDiseases;
            return this;
        }

        public Builder diseaseToGenes(Map<TermId, Collection<GeneIdentifier>> diseaseToGenes) {
            this.diseaseToGenes = diseaseToGenes;
            return this;
        }

        public Builder geneToSymbol(Map<TermId, String> geneToSymbol) {
            this.geneToSymbol = geneToSymbol;
            return this;
        }

        /**
         * Create a Builder object from the YAML Parser. By default, phenotypeOnly mode is set to false
         *
         * @param yp YamlParser
         * @return a Builder object
         */
        public Builder yaml(YamlParser yp) {
            return yaml(yp, false);
        }

        /**
         * Create a Builder object from the YAML Parser.
         *
         * @param yp            YamlParser
         * @param phenotypeOnly If false, expect to see information about the VCF file and Exomiser build.
         * @return Builder object
         */
        public Builder yaml(YamlParser yp, boolean phenotypeOnly) {
            this.observedHpoTerms = yp.getHpoTermList();
            this.negatedHpoTerms = yp.getNegatedHpoTermList();
            switch (yp.transcriptdb().toUpperCase()) {
                case "REFSEQ":
                    this.transcriptdatabase = TranscriptDatabase.REFSEQ;
                    break;
                case "UCSC":
                default:
                    this.transcriptdatabase = TranscriptDatabase.UCSC;
            }
            if (yp.getPrefix() != null) {
                this.outfilePrefix = yp.getPrefix();
            }
            Optional<Double> threshold = yp.threshold();
            threshold.ifPresent(d -> this.lrThreshold = d);
            if (yp.mindiff().isPresent()) {
                this.minDifferentials = yp.mindiff().get();
            }
            this.outputDirectory = yp.getOutDirectory().orElse(null);
            if (phenotypeOnly) return this;
            // if we get here, then we add stuff that is relevant to VCF analysis
            this.exomiserDataDir = Path.of(yp.getExomiserDataDir());
            this.genomeAssembly = parseAssembly(yp.getGenomeAssembly());
            Optional<Path> vcfOpt = yp.getOptionalVcfPath();
            if (vcfOpt.isPresent()) {
                this.vcfPath = vcfOpt.get();
            } else {
                vcfPath = null;
            }
            yp.getBackgroundPath().ifPresent(s -> this.backgroundFrequencyPath = Path.of(s));

            this.global = yp.global();
            return this;
        }

        private GenomeAssembly parseAssembly(String genomeAssembly) {
            return switch (genomeAssembly.toLowerCase()) {
                case "grch37", "hg19" -> GenomeAssembly.HG19;
                default -> GenomeAssembly.HG38;
            };
        }

        @Deprecated(forRemoval = true)
        public Builder orphanet(boolean b) {
            this.useOrphanet = b;
            return this;
        }

        public Builder global(boolean global) {
            this.global = global;
            return this;
        }


        public Builder genomeAssembly(GenomeAssembly genomeAssembly) {
            this.genomeAssembly = genomeAssembly;
            return this;
        }

        public Builder transcriptdatabase(TranscriptDatabase transcriptdatabase) {
            this.transcriptdatabase = transcriptdatabase;
            return this;
        }


        /**
         * @return an {@link org.monarchinitiative.exomiser.core.genome.GenomeAssembly} object representing the genome build.
         */
        GenomeAssembly getAssembly() {
            return genomeAssembly;
        }


        public Builder backgroundFrequency(Path bf) {
            this.backgroundFrequencyPath = bf;
            return this;
        }

        public Builder lrThreshold(Double d) {
            this.lrThreshold = d;
            return this;
        }

        public Builder minDiff(Integer n) {
            this.minDifferentials = n;
            return this;
        }


        public Builder vcf(Path vcf) {
            this.vcfPath = vcf;
            return this;
        }

        public Builder exomiser(Path exomiser) {
            this.exomiserDataDir = exomiser;
            return this;
        }


        public Builder observedHpoTerms(String[] terms) {
            this.observedHpoTerms = new ArrayList<>();
            Collections.addAll(observedHpoTerms, terms);
            return this;
        }

        public LiricalFactory build() {
            return new LiricalFactory(this);
        }


        public LiricalFactory buildForGenomicDiagnostics() {
            LiricalFactory factory = new LiricalFactory(this);
            factory.qcExomiserFiles();
            return factory;
        }


        public LiricalFactory buildForPhenotypeOnlyDiagnostics() {
            LiricalFactory factory = new LiricalFactory(this);
            return factory;
        }


    }
}
