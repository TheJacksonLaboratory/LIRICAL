package org.monarchinitiative.lirical.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.apache.commons.io.FilenameUtils;
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
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.io.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Not a full implementation of the factory pattern but rather a convenience class to create objects of various
 * classes that we need as singletons with the various commands.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class LiricalFactory {
    private static final Logger logger = LoggerFactory.getLogger(LiricalFactory.class);
    /** Path to the {@code phenotype.hpoa} file. */
    private final String phenotypeAnnotationPath;
    /** UCSC, RefSeq, Ensembl. */
    private final TranscriptDatabase transcriptdatabase;
    /** Path to the {@code Homo_sapiens_gene_info.gz} file. */
    private final String geneInfoPath;
    /** Path to the mimgene/medgen file with MIM to gene associations. */
    private final String mim2genemedgenPath;
    /** Path to the VCF file that is be evaluated. */
    private final String vcfPath;
    /** List of HPO terms (phenotypic abnormalities) observed in the person being evaluated. */
    private final List<TermId> hpoIdList;
    /** List of HPO terms that were excluded in the person being evaluated. */
    private final List<TermId> negatedHpoIdList;
    /** The directory in which several files are stored. */
    private final String datadir;
    /** The directory with the Exomiser database and Jannovar transcript files. */
    private String exomiserPath;
    /** Number of variants that were not removed because of the quality filter. */
    private int n_good_quality_variants=0;
    /** Number of variants that were removed because of the quality filter. */
    private int n_filtered_variants=0;

    private final GenomeAssembly assembly;

    private final Ontology ontology;
    /** The path to the Exomiser database file, e.g., {@code 1811_hg19_variants.mv.db}. */
    private String mvStorePath=null;
    /** genotype matching for likelihood ratio calculation". */
    private boolean strict;
    /** retain candidates even if no candidate variant is found */
    private final boolean keepIfNoCandidateVariant;

    private String hpoVersion="n/a";

    private List<String> desiredDatabasePrefixes;

    /** An object representing the Exomiser database. */
    private MVStore mvstore = null;
    private Multimap<TermId,TermId> gene2diseaseMultiMap=null;
    private Multimap<TermId,TermId> disease2geneIdMultiMap=null;
    private Map<TermId,String> geneId2SymbolMap=null;
    /** Key: the TermId of a gene. Value. Its background frequency in the current genome build. This variable
     * is only initialized for runs with a VCF file. */
    private Map<TermId, Double> gene2backgroundFrequency = null;
    /** If true, filter VCF lines by the FILTER column (variants pass if there is no entry, i.e., ".",
     * or if the value of the field is FALSE. Variant also fail if a reason for the not passing the
     * filter is given in the column, i.e., for allelic imbalance. This is true by default. Filtering
     * can be turned off by entering {@code -q false} or {@code --quality} false. */
    private final boolean filterOnFILTER;

    /** Path of the Jannovar UCSC transcript file (from the Exomiser distribution) */
    private String jannovarUcscPath=null;
    /** Path of the Jannovar Ensembl transcript file (from the Exomiser distribution) */
    private String jannovarEnsemblPath=null;
    /** Path of the Jannovar RefSeq transcript file (from the Exomiser distribution) */
    private String jannovarRefSeqPath=null;
    /** Name of sample in VCF file, if any. The default value is n/a to indicate this field has not been initiatilized. */
    private String sampleName="n/a";


    private JannovarData jannovarData=null;
    /** Used as a flag to pick the right constructor in {@link Builder#buildForGt2Git()}. */
    private enum BuildType { GT2GIT}

    /**
     * This constructor is used to build Gt2Git. The BuildType argument is used as a flag.
     */
    private LiricalFactory(Builder builder, BuildType bt){
            filterOnFILTER = false;
            keepIfNoCandidateVariant = false;
            ontology = null;
            assembly = builder.getAssembly();
            this.exomiserPath = builder.exomiserDataDir;
            if (exomiserPath!=null) {
                initializeExomiserPaths();
            }
            this.geneInfoPath = null;
            this.mim2genemedgenPath=builder.mim2genemedgenPath;

            this.phenotypeAnnotationPath = null;
            this.transcriptdatabase = builder.transcriptdatabase;
            this.vcfPath = null;
            this.datadir= builder.liricalDataDir;
            this.strict = false;
            hpoIdList = ImmutableList.of();
            negatedHpoIdList = ImmutableList.of();
    }

    private LiricalFactory(Builder builder) {
        this.ontology = builder.ontology;
        Map<String,String> ontologyMetainfo=ontology.getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.hpoVersion=ontologyMetainfo.get("data-version");
        }
        this.exomiserPath = builder.exomiserDataDir;
        if (exomiserPath!=null) {
            initializeExomiserPaths();
        }
        this.assembly=builder.getAssembly();
        if (builder.backgroundFrequencyPath!=null
                && !builder.backgroundFrequencyPath.isEmpty()) {
            this.gene2backgroundFrequency = GenotypeDataIngestor.fromPath(builder.backgroundFrequencyPath);
        } else {
            // Note-- background files for hg19 and hg38 are stored in src/main/resources/background
            // and are included in the resources by the maven resource plugin
            if (assembly.equals(GenomeAssembly.HG19)) {
                this.gene2backgroundFrequency = GenotypeDataIngestor.fromResource("background/background-hg19.tsv");
            } else if (assembly.equals(GenomeAssembly.HG38)) {
                this.gene2backgroundFrequency = GenotypeDataIngestor.fromResource("background/background-hg38.tsv");
            } else {
                logger.error("Did not recognize genome assembly: {}",assembly);
                throw new LiricalRuntimeException("Did not recognize genome assembly: "+assembly);
            }

        }

        this.geneInfoPath=builder.geneInfoPath;
        this.mim2genemedgenPath=builder.mim2genemedgenPath;

        this.phenotypeAnnotationPath=builder.phenotypeAnnotationPath;
        this.transcriptdatabase=builder.transcriptdatabase;
        this.vcfPath=builder.vcfPath;
        this.datadir=builder.liricalDataDir;
        this.strict = builder.strict;

        ImmutableList.Builder<TermId> listbuilder = new ImmutableList.Builder<>();
        for (String id : builder.observedHpoTerms) {
            TermId hpoId = TermId.of(id);
            listbuilder.add(hpoId);
        }
        this.hpoIdList=listbuilder.build();
        listbuilder = new ImmutableList.Builder<>();
        for (String id : builder.negatedHpoTerms){
            TermId negatedId = TermId.of(id);
            listbuilder.add(negatedId);
        }
        this.negatedHpoIdList = listbuilder.build();
        this.filterOnFILTER=builder.filterFILTER;
        this.keepIfNoCandidateVariant = builder.keep;
        if (builder.useOrphanet) {
            this.desiredDatabasePrefixes=ImmutableList.of("ORPHA");
        } else {
            this.desiredDatabasePrefixes=ImmutableList.of("OMIM","DECIPHER");
        }
    }


    /**
     * @return a list of observed HPO terms (from the YAML/Phenopacket file)
     * @throws LiricalException if one of the terms is not in the HPO Ontology
     */
    public List<TermId> observedHpoTerms() throws LiricalException {
        for (TermId hpoId : hpoIdList) {
            if (! this.ontology.getTermMap().containsKey(hpoId)) {
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
            if (! this.ontology.getTermMap().containsKey(hpoId)) {
                throw new LiricalException("Could not find HPO term " + hpoId.getValue() + " in ontology");
            }
        }
        return negatedHpoIdList;
    }



    /** @return the genome assembly corresponding to the VCF file. Can be null. */
    public GenomeAssembly getAssembly() {
        return assembly;
    }

    /** @return HpoOntology object. */
    public Ontology hpoOntology() {
        return ontology;
    }

    /** returns "n/a if {@link #transcriptdatabase} was not initialized (should not happen). */
    public String transcriptdb() {
            return this.transcriptdatabase!=null?this.transcriptdatabase.toString():"n/a";
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getExomiserPath() { return this.exomiserPath;}

    public String getHpoVersion() { return hpoVersion; }

    public String getVcfPath() {
        if (this.vcfPath==null) {
            throw new LiricalRuntimeException("VCF path not initialized");
        }
        return vcfPath;
    }

    public boolean hasVcf() {
        return this.vcfPath!=null;
    }

    /**
     * This is called if the user passes the {@code --exomiser/-e} option. We expect there to be
     * the Jannovar and the MVStore files in the directory and want to construct the paths here.
     */
    private void initializeExomiserPaths() {
        // Remove the trailing directory slash if any
        this.exomiserPath=getPathWithoutTrailingSeparatorIfPresent(this.exomiserPath);
        String basename=FilenameUtils.getBaseName(this.exomiserPath);
        String filename=String.format("%s_variants.mv.db", basename);
        this.mvStorePath=String.format("%s%s%s", exomiserPath,File.separator,filename);
        filename=String.format("%s_transcripts_ucsc.ser", basename);
        this.jannovarUcscPath=filename;
        filename=String.format("%s_transcripts_ensembl.ser", basename);
        this.jannovarEnsemblPath=filename;
        filename=String.format("%s_transcripts_refseq.ser", basename);
        this.jannovarRefSeqPath=filename;

    }


    /** @return MVStore object with Exomiser data on variant pathogenicity and frequency. */
    public MVStore mvStore() {
        File f = new File(this.mvStorePath);
        if (!f.exists()) {
            throw new LiricalRuntimeException("[FATAL] Could not find Exomiser database file/variants.mv.db at " + this.mvStorePath);
        }
        if (mvstore==null) {
            mvstore = new MVStore.Builder()
                    .fileName(this.mvStorePath)
                    .readOnly()
                    .open();
        }
        return mvstore;
    }





    private void parseHpoAnnotations()  {
        if (this.ontology==null) {
            hpoOntology();
        }
        if (this.geneInfoPath==null) {
            throw new LiricalRuntimeException("Path to Homo_sapiens_gene_info.gz file not found");
        }
        if (this.mim2genemedgenPath==null) {
            throw new LiricalRuntimeException("Path to mim2genemedgen file not found");
        }

        File geneInfoFile = new File(geneInfoPath);
        if (!geneInfoFile.exists()) {
            throw new LiricalRuntimeException("Could not find gene info file at " + geneInfoPath + ". Run download!");
        }
        File mim2genemedgenFile = new File(this.mim2genemedgenPath);
        if (!mim2genemedgenFile.exists()) {
            throw new LiricalRuntimeException("Could not find medgen file at " + this.mim2genemedgenPath + ". Run download!");
        }
        HpoAssociationParser assocParser = new HpoAssociationParser(geneInfoFile.getAbsolutePath(),
                mim2genemedgenFile.getAbsolutePath(),
                ontology);
        this.gene2diseaseMultiMap=assocParser.getGeneToDiseaseIdMap();
        this.disease2geneIdMultiMap=assocParser.getDiseaseToGeneIdMap();
        this.geneId2SymbolMap=assocParser.getGeneIdToSymbolMap();
    }


    /** @return a multimap with key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123. */
    public Multimap<TermId,TermId> gene2diseaseMultimap()  {
        if (this.gene2diseaseMultiMap==null) {
            parseHpoAnnotations();
        }
        return this.gene2diseaseMultiMap;
    }

    /** @return multimap with key:disease CURIEs such as OMIM:600123; value: a collection of gene CURIEs such as NCBIGene:123.  */
    public Multimap<TermId,TermId> disease2geneMultimap()  {
        if (this.disease2geneIdMultiMap==null) {
            parseHpoAnnotations();
        }
        return this.disease2geneIdMultiMap;
    }
    /** @return a map with key:a gene id, e.g., NCBIGene:2020; value: the corresponding gene symbol. */
    public Map<TermId,String> geneId2symbolMap() {
        if (this.geneId2SymbolMap==null) {
            parseHpoAnnotations();
        }
        return this.geneId2SymbolMap;
    }


    private static String getPathWithoutTrailingSeparatorIfPresent(String path) {
        String sep = File.separator;
        if (path.endsWith(sep)) {
            int i=path.lastIndexOf(sep);
            return path.substring(0,i);
        } else {
            return path;
        }
    }

    /**
     * Create a {@link GenotypeLikelihoodRatio} object that will be used to calculated genotype likelhood ratios.
     * A runtime exception will be thrown if the file cannot be found.
     * @return a {@link GenotypeLikelihoodRatio} object
     */
    public GenotypeLikelihoodRatio getGenotypeLR() {
        return new GenotypeLikelihoodRatio(this.gene2backgroundFrequency,this.strict);
    }




    /**
     * Deserialize the Jannovar transcript data file that comes with Exomiser. Note that Exomiser
     * uses its own ProtoBuf serializetion and so we need to use its Deserializser. In case the user
     * provides a standard Jannovar serialzied file, we try the legacy deserializer if the protobuf
     * deserializer doesn't work.
     * @return the object created by deserializing a Jannovar file. */
    public JannovarData jannovarData()  {
        if (jannovarData != null) return jannovarData;
        // Remove the trailing directory slash if any
        this.exomiserPath= getPathWithoutTrailingSeparatorIfPresent(this.exomiserPath);
        String basename=FilenameUtils.getBaseName(this.exomiserPath);
        String fullpath;
        switch (this.transcriptdatabase) {
            case REFSEQ:
                String refseqfilename=String.format("%s_transcripts_refseq.ser", basename);
                fullpath=String.format("%s%s%s", exomiserPath,File.separator,refseqfilename);
                break;
            case ENSEMBL:
                String ensemblfilename=String.format("%s_transcripts_ensembl.ser", basename);
                fullpath=String.format("%s%s%s", exomiserPath,File.separator,ensemblfilename);
                break;
            case UCSC:
            default:
                String ucscfilename=String.format("%s_transcripts_ucsc.ser", basename);
                fullpath=String.format("%s%s%s", exomiserPath,File.separator,ucscfilename);
                break;
        }

        File f = new File(fullpath);
        if (!f.exists()) {
            throw new LiricalRuntimeException("[FATAL] Could not find Jannovar transcript file at " + fullpath);
        }
        try {
            Path p = Paths.get(fullpath);
            this.jannovarData=JannovarDataProtoSerialiser.load(p);
            return jannovarData;
        } catch (InvalidFileFormatException e) {
            logger.warn("Could not deserialize Jannovar file with Protobuf deserializer, trying legacy deserializer...");
        }
        try {
            this.jannovarData=new JannovarDataSerializer(fullpath).load();
            return jannovarData;
        } catch (SerializationException e) {
            logger.error("Could not deserialize Jannovar file with legacy deserializer...");
            throw new LiricalRuntimeException(String.format("Could not load Jannovar data from %s (%s)",
                    fullpath, e.getMessage()));
        }
    }

    /** @return a map with key: a disease id (e.g., OMIM:654321) and key the corresponding {@link HpoDisease} object.*/
    public Map<TermId, HpoDisease> diseaseMap(Ontology ontology)  {
        if (this.phenotypeAnnotationPath==null) {
            throw new LiricalRuntimeException("Path to phenotype.hpoa file not found");
        }

        return HpoDiseaseAnnotationParser.loadDiseaseMap(phenotypeAnnotationPath,ontology,desiredDatabasePrefixes);
    }

    public  Map<TermId, Gene2Genotype> getGene2GenotypeMap() {
        return getGene2GenotypeMap(getVcfPath());
    }

    public  Map<TermId, Gene2Genotype> getGene2GenotypeMap(String vcfPath) {
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcfPath,
                jannovarData(),
                mvStore(),
                getAssembly(),
                this.filterOnFILTER);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.sampleName=vcf2geno.getSamplename();
        this.n_filtered_variants=vcf2geno.getN_filtered_variants();
        this.n_good_quality_variants=vcf2geno.getN_good_quality_variants();
        return genotypeMap;
    }

    /** @return a string with today's date in the format yyyy/MM/dd. */
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

    public boolean keepIfNoCandidateVariant() { return  keepIfNoCandidateVariant; }

    /**
     * This is used by the Builder to check that all of the necessary files in the Data directory are present.
     * It writes one line to the logger for each file it checks, and throws a RunTime exception if a file is
     * missing (in this case we cannot continue with program execution).
     */
    public void qcHumanPhenotypeOntologyFiles() {
        File datadirfile = new File(datadir);
        if (!datadirfile.exists()) {
            logger.error("Could not find LIRICAL data directory at {}",datadir);
            logger.error("Consider running download command.");
            throw new LiricalRuntimeException(String.format("Could not find LIRICAL data directory at %s",datadir));
        } else if (!datadirfile.isDirectory()) {
            logger.error("LIRICAL datadir path ({}) is not a directory.",datadir);
            throw new LiricalRuntimeException(String.format("LIRICAL datadir path (%s) is not a directory.",datadir));
        } else {
            logger.trace("LIRICAL datadirectory: {}", datadir);
        }
        File f2 = new File(this.phenotypeAnnotationPath);
        if (!f2.exists() && f2.isFile()) {
            logger.error("Could not find valid phenotype.hpoa file at {}",phenotypeAnnotationPath);
            throw new LiricalRuntimeException(String.format("Could not find valid phenotype.hpoa file at %s",phenotypeAnnotationPath));
        } else {
            logger.trace("phenotype.hpoa: {}",phenotypeAnnotationPath);
        }
    }



    public void qcExternalFilesInDataDir() {
        File f1 = new File(this.mim2genemedgenPath);
        if (!f1.exists() && f1.isFile()) {
            logger.error("Could not find valid mim2gene_medgen file at {}",mim2genemedgenPath);
            throw new LiricalRuntimeException(String.format("Could not find valid mim2gene_medgen file at %s",mim2genemedgenPath));
        } else {
            logger.trace("mim2gene_medgen: {}",mim2genemedgenPath);
        }
        File f2 = new File(this.geneInfoPath);
        if (!f2.exists() && f2.isFile()) {
            logger.error("Could not find valid Homo_sapiens_gene_info.gz file at {}",geneInfoPath);
            throw new LiricalRuntimeException(String.format("Could not find valid Homo_sapiens_gene_info.gz file at %s",geneInfoPath));
        } else {
            logger.trace("Homo_sapiens_gene_info.gz: {}",geneInfoPath);
        }
    }

    public void qcExomiserFiles() {
        File exomiserDir = new File(exomiserPath);
        if (!exomiserDir.exists()) {
            logger.error("Could not find Exomiser data directory at {}",exomiserPath);
            throw new LiricalRuntimeException(String.format("Could not find Exomiser data directory at %s",exomiserPath));
        } else if (!exomiserDir.isDirectory()) {
            logger.error("Exomiser data path ({}) is not a directory.",exomiserPath);
            throw new LiricalRuntimeException(String.format("Exomiser data path (%s) is not a directory.",exomiserPath));
        } else {
            logger.trace("Exomiser data: {}", exomiserPath);
        }
        File mvStoreFile=new File(this.mvStorePath);
        if (!mvStoreFile.exists()) {
            logger.error("Could not find Exomiser database file at {}",this.mvStorePath);
            throw new LiricalRuntimeException(String.format("Could not find Exomiser database file at %s",mvStorePath));
        }
    }

    /**
     * This method checks whether the background frequency data was initialized
     */
    private void qcBackgroundFrequency() {

        if (! this.gene2backgroundFrequency.isEmpty()) {
            logger.error("background frequency was not initialized ");
            throw new LiricalRuntimeException("background frequency was not initialized ");
        } else {
            logger.trace("Background frequency initialized for {} genes", this.gene2backgroundFrequency.size());
        }
    }



    public void qcYaml() {
        qcHumanPhenotypeOntologyFiles();
        qcExternalFilesInDataDir();
        qcExomiserFiles();
        qcBackgroundFrequency();
    }

    /**
     * Perform Q/C of the input variables to try to ensure that the correct (matching) genome build is being used.
     */
    public void qcGenomeBuild() {
        if (this.assembly.equals(GenomeAssembly.HG19)) {
            if (! this.exomiserPath.contains("hg19")) {
                throw new LiricalRuntimeException(String.format("Use of non-matching Exomiser database (%s) for genome assembly hg19", this.exomiserPath));
            }
        } else if (this.assembly.equals(GenomeAssembly.HG38)) {
            if (! this.exomiserPath.contains("hg38")) {
                throw new LiricalRuntimeException(String.format("Use of non-matching Exomiser database (%s) for genome assembly hg38", this.exomiserPath));
            }
        } else {
            logger.trace("Genome assembly: {}",this.assembly.toString());
        }
    }

    public void qcVcfFile() {
        if (this.vcfPath==null) {
            throw new LiricalRuntimeException("VCF file was not initialzed");
        }
        if (! (new File(vcfPath)).exists()) {
            throw new LiricalRuntimeException("We did not find a VCF file at \"" + vcfPath +"\"");
        }
        logger.trace("VCF File: {}",this.vcfPath);
    }


    /**
     * A convenience Builder class for creating {@link LiricalFactory} objects
     */
    public static class Builder {
        /** path to hp.obo file.*/
        private Ontology ontology = null;
        private String phenotypeAnnotationPath = null;
        private String liricalDataDir = null;
        private String exomiserDataDir = null;
        private String geneInfoPath = null;
        private String mim2genemedgenPath = null;
        private String backgroundFrequencyPath = null;
        private String vcfPath = null;
        private String genomeAssembly = null;
        private boolean filterFILTER = true;
        private boolean strict = false;
        private boolean keep = false;
        private boolean useOrphanet = false;
        /** The default transcript database is UCSC> */
        private TranscriptDatabase transcriptdatabase=  TranscriptDatabase.UCSC;
        private List<String> observedHpoTerms=ImmutableList.of();
        private List<String> negatedHpoTerms=ImmutableList.of();

        /** If this constructor is used, the the build method will attempt to load the HPO
         * based on its file location in datadir. If it is not possible, we will die gracefully.
         */
        public Builder(){
        }

        public Builder(Ontology  hpo){
            ontology = hpo;
        }

        public Builder yaml(YamlParser yp) {
            this.liricalDataDir = getPathWithoutTrailingSeparatorIfPresent(yp.getDataDir());
            initDatadirFiles();
            this.exomiserDataDir=yp.getExomiserDataDir();
            this.genomeAssembly=yp.getGenomeAssembly();
            this.observedHpoTerms=new ArrayList<>();
            this.negatedHpoTerms=new ArrayList<>();
            this.observedHpoTerms=yp.getHpoTermList();
            this.negatedHpoTerms=yp.getNegatedHpoTermList();
            switch (yp.transcriptdb().toUpperCase()) {
                case "ENSEMBL" :
                    this.transcriptdatabase=TranscriptDatabase.ENSEMBL;
                case "REFSEQ":
                    this.transcriptdatabase=TranscriptDatabase.REFSEQ;
                case "UCSC":
                default:
                    this.transcriptdatabase=TranscriptDatabase.UCSC;
            }
            Optional<String> vcfOpt=yp.getOptionalVcfPath();
            if (vcfOpt.isPresent()) {
                this.vcfPath=vcfOpt.get();
            } else {
                vcfPath=null;
            }
            Optional<String> backgroundOpt = yp.getBackgroundPath();
            backgroundOpt.ifPresent(s -> this.backgroundFrequencyPath = s);
            return this;
        }


        public Builder yaml(YamlParser yp, boolean phenotypeOnly) {
            if (!phenotypeOnly) return yaml(yp);
            this.liricalDataDir = getPathWithoutTrailingSeparatorIfPresent(yp.getDataDir());
            initDatadirFiles();
            this.observedHpoTerms=new ArrayList<>();
            this.negatedHpoTerms=new ArrayList<>();
            this.observedHpoTerms=yp.getHpoTermList();
            this.negatedHpoTerms=yp.getNegatedHpoTermList();
            return this;
        }


        public Builder strict(boolean b) {
            this.strict = b;
            return this;
        }

        public Builder orphanet(boolean b) {
            this.useOrphanet = b;
            return this;
        }

        public Builder keep(boolean b) {
            this.keep = b;
            return this;
        }


        public Builder genomeAssembly(String ga) {
            this.genomeAssembly=ga;
            return this;
        }

        public Builder transcriptdatabase(String tdb) {
            switch (tdb.toUpperCase()) {
                case "ENSEMBL" :
                    this.transcriptdatabase=TranscriptDatabase.ENSEMBL;
                    break;
                case "REFSEQ":
                    this.transcriptdatabase=TranscriptDatabase.REFSEQ;
                    break;
                case "UCSC":
                    break;
                default:
                    this.transcriptdatabase=TranscriptDatabase.UCSC;
            }
            return this;
        }


        /** @return an {@link org.monarchinitiative.exomiser.core.genome.GenomeAssembly} object representing the genome build.*/
        GenomeAssembly getAssembly() {
            if (genomeAssembly!=null) {
                switch (genomeAssembly.toLowerCase()) {
                    case "hg19":
                    case "hg37":
                    case "grch37":
                    case "grch_37":
                        return GenomeAssembly.HG19;
                    case "hg38":
                    case "grch38":
                    case "grch_38":
                        return GenomeAssembly.HG38;
                }
            }
            return GenomeAssembly.HG38; // the default.
        }


        public Builder backgroundFrequency(String bf) {
            this.backgroundFrequencyPath=bf;
            return this;
        }



        public Builder vcf(String vcf) {
            this.vcfPath=vcf;
            return this;
        }

        public Builder exomiser(String exomiser) {
            this.exomiserDataDir=exomiser;
            return this;
        }


        public Builder observedHpoTerms(String [] terms) {
            this.observedHpoTerms=new ArrayList<>();
            Collections.addAll(observedHpoTerms, terms);
            return this;
        }

        /** Initializes the paths to the four files that should be in the data directory. This method
         * should be called only after {@link #liricalDataDir} has been set.
         */
        private void initDatadirFiles() {
            this.geneInfoPath=String.format("%s%s%s",this.liricalDataDir,File.separator,"Homo_sapiens_gene_info.gz");
            this.phenotypeAnnotationPath=String.format("%s%s%s",this.liricalDataDir,File.separator,"phenotype.hpoa");
            this.mim2genemedgenPath=String.format("%s%s%s",this.liricalDataDir,File.separator,"mim2gene_medgen");
        }



        public Builder datadir(String datadir) {
            this.liricalDataDir =getPathWithoutTrailingSeparatorIfPresent(datadir);
           initDatadirFiles();
            return this;
        }

        private void ingestHpo() {
            String hpopath = String.format("%s%s%s",this.liricalDataDir,File.separator,"hp.obo");
            this.ontology = OntologyLoader.loadOntology(new File(hpopath));
            Objects.requireNonNull(this.ontology);
        }


        public LiricalFactory build() {
            if (this.ontology == null) ingestHpo();
            return new LiricalFactory(this);
        }


        public LiricalFactory buildForGt2Git() {
            return new LiricalFactory(this,BuildType.GT2GIT);
        }


        public LiricalFactory buildForGenomicDiagnostics() {
            if (this.ontology == null) ingestHpo();
            LiricalFactory factory = new LiricalFactory(this);
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            factory.qcExomiserFiles();
            return factory;
        }


        public LiricalFactory buildForPhenotypeOnlyDiagnostics() {
            if (this.ontology == null) ingestHpo();
            LiricalFactory factory = new LiricalFactory(this);
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            return factory;
        }


    }
}
