package org.monarchinitiative.lr2pg.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.mvstore.MVStore;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.io.PhenopacketImporter;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.monarchinitiative.lr2pg.io.PhenopacketImporter.fromJson;

/**
 * Download a number of files needed for the analysis
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Run LR2PG from a Phenopacket")
public class PhenopacketCommand extends Lr2PgCommand{
    private static final Logger logger = LogManager.getLogger();
    @Parameter(names="--tsv",description = "Use TSV instead of HTML output")
    private boolean outputTSV=false;
    @Parameter(names = {"-p","--phenopacket"}, description = "path to phenopacket file", required = true)
    private String phenopacketPath;
    /** Directory where various files are downloaded/created. */
    @Parameter(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    private String datadir="data";
    /** The threshold for showing a differential diagnosis in the main section (posterior probability of 1%).*/
    @Parameter(names= {"-t","--threshold"}, description = "threshold for showing diagnosis in HTML output")
    private double LR_THRESHOLD=0.01;
    @Parameter(names={"-m","--mvstore"}, description = "path to MV Store Exomiser database file", required = true)
    private String mvpath;
    @Parameter(names={"-j","--jannovar"}, description = "path to Jannovar transcript information file", required = true)
    private String jannovarPath;

    ///// TODO ADD THIS TO RESOURCES!!!!!!
    /** Default name of the background frequency file. */
    private final String BACKGROUND_FREQUENCY_FILE="background-freq.txt";

    /** Various metadata that will be used for the HTML org.monarchinitiative.lr2pg.output. */
    private Map<String,String> metadata;


    private boolean hasVcf;

    List<TermId> hpoIdList;
    List<TermId> negatedHpoIdList;
    String genomeAssembly;
    String vcfPath;


    public PhenopacketCommand(){
        // read the Phenopacket
        try {
            PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath);
            this.vcfPath = importer.getVcfPath();
            if (vcfPath!=null) hasVcf=true;
            this.genomeAssembly = importer.getGenomeAssembly();
            this.hpoIdList = importer.getHpoTerms();
            this.negatedHpoIdList = importer.getNegatedHpoTerms();
        } catch (ParseException | IOException e) {
            logger.fatal("Could not read phenopacket");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        String hpoOboPath = String.format("%s%s%s",datadir,File.separator,"hp.obo" );
        String phenotypeHpoaPath = String.format("%s%s%s",datadir,File.separator,"phenotype.hpoa" );



        if (hasVcf) {
            try {
                Map<TermId, Gene2Genotype> genotypemap = getVcf2GenotypeMap();
                GenotypeLikelihoodRatio genoLr = getGenotypeLR();




                Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder()
                        .hp_obo(hpoOboPath)
                        .phenotypeAnnotation(phenotypeHpoaPath)
                        .mvStore(this.mvpath);
               // List<TermId> observedHpoTerms = factory.observedHpoTerms();
               // HpoOntology ontology = factory.hpoOntology();
                //Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
            } catch (Lr2pgException e) {
                e.printStackTrace();
            }
        }
    }



    private GenotypeLikelihoodRatio getGenotypeLR() throws Lr2pgException {
        String backgroundFile = String.format("%s%s%s",datadir, File.separator,BACKGROUND_FREQUENCY_FILE);
        File f = new File(backgroundFile);
        if (!f.exists()) {
            throw new Lr2pgException(String.format("Could not find %s",BACKGROUND_FREQUENCY_FILE));
        }
        GenotypeDataIngestor ingestor = new GenotypeDataIngestor(backgroundFile);
        Map<TermId,Double> gene2back = ingestor.parse();
        return new GenotypeLikelihoodRatio(gene2back);
    }



    /** @return MVStore object with Exomiser data on variant pathogenicity and frequency. */
    public MVStore mvStore(String mvStoreAbsolutePath) {
        MVStore mvstore = new MVStore.Builder()
                    .fileName(mvStoreAbsolutePath)
                    .readOnly()
                    .open();
        return mvstore;
    }


    /** @return the object created by deserilizing a Jannovar file. */
    public JannovarData jannovarData(String jannovarTranscriptFile) throws Lr2pgException {
        if (jannovarTranscriptFile == null) {
            throw new Lr2pgException("Path to jannovar transcript file not found");
        }
        try {
            return new JannovarDataSerializer(jannovarTranscriptFile).load();

        } catch (SerializationException e) {
            throw new Lr2pgException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarTranscriptFile, e.getMessage()));
        }
    }

    private GenomeAssembly getGenomeAssembly(String ga) {
        switch (ga) {
            case "HG19":
            case "hg19":
            case "GRC37":
            case "hg37":
                return GenomeAssembly.HG19;
            case "HG38":
            case "hg38":
            case "GRC38":
                return GenomeAssembly.HG38;
            default:
                return GenomeAssembly.HG19;
        }
    }


    /**
     * Identify the variants and genotypes from the VCF file.
     * @return a map with key: An NCBI Gene Id, and value: corresponding {@link Gene2Genotype} object.
     * @throws Lr2pgException upon error parsing the VCF file or creating the Jannovar object
     */
    private Map<TermId, Gene2Genotype> getVcf2GenotypeMap() throws Lr2pgException {
        MVStore mvstore = mvStore(mvpath);
        JannovarData jannovarData = jannovarData(jannovarPath);
        GenomeAssembly assembly = getGenomeAssembly(this.genomeAssembly);
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcfPath, jannovarData, mvstore, assembly);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.metadata = vcf2geno.getVcfMetaData();
        return genotypeMap;
    }






}
