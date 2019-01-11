package org.monarchinitiative.lr2pg.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Multimap;
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
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.io.PhenopacketImporter;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.output.HtmlTemplate;
import org.monarchinitiative.lr2pg.output.Lr2pgTemplate;
import org.monarchinitiative.lr2pg.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    /** List of HPO terms observed in the subject of the investigation. */
    private List<TermId> hpoIdList;
    /** List of excluded HPO terms in the subject. */
    private List<TermId> negatedHpoIdList;
    /** String representing the genome build (hg19 or hg38). */
    private String genomeAssembly;
    /** Path to the VCF file (if any). */
    private String vcfPath=null;
    /** Representation of the Exomiser database (http://www.h2database.com/html/mvstore.html). */
    private MVStore mvstore;


    public PhenopacketCommand(){

    }

    @Override
    public void run() {
// read the Phenopacket
        logger.trace("Will analyze phenopacket at " + phenopacketPath);
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
        String hpoOboPath = String.format("%s%s%s",datadir,File.separator,"hp.obo" );
        String phenotypeHpoaPath = String.format("%s%s%s",datadir,File.separator,"phenotype.hpoa" );
        String hsapiensGeneInfoPath = String.format("%s%s%s",datadir,File.separator,"Homo_sapiens_gene_info.gz");
        String mim2geneMedgenPath = String.format("%s%s%s",datadir,File.separator,"mim2gene_medgen");


        if (hasVcf) {
            try {
                Lr2PgFactory factory = new Lr2PgFactory.Builder()
                        .hp_obo(hpoOboPath)
                        .phenotypeAnnotation(phenotypeHpoaPath)
                        .geneInfo(hsapiensGeneInfoPath)
                        .mim2genemedgen(mim2geneMedgenPath)
                        .mvStore(this.mvpath).build();

                MVStore mvstore = factory.mvStore();
                JannovarData jannovarData = jannovarData(jannovarPath);
                GenomeAssembly assembly = getGenomeAssembly(this.genomeAssembly);

                Map<TermId, Gene2Genotype> genotypemap = getVcf2GenotypeMap(jannovarData,mvstore, assembly);
                GenotypeLikelihoodRatio genoLr = getGenotypeLR();
                Ontology ontology = factory.hpoOntology();
                Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
                PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
                Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();
                Map<TermId,String> geneId2symbol = factory.geneId2symbolMap();
                CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
                        .ontology(ontology)
                        .diseaseMap(diseaseMap)
                        .disease2geneMultimap(disease2geneMultimap)
                        .genotypeMap(genotypemap)
                        .phenotypeLr(phenoLr)
                        .genotypeLr(genoLr);

                CaseEvaluator evaluator = caseBuilder.build();
                HpoCase hcase = evaluator.evaluate();
                hcase.outputTopResults(5,ontology,genotypemap);// TODO remove this outputs to the shell
                if (outputTSV) {
                    Lr2pgTemplate template = new TsvTemplate(hcase,ontology,genotypemap,geneId2symbol,this.metadata);
                    template.outputFile();
                } else {
                    HtmlTemplate caseoutput = new HtmlTemplate(hcase,ontology,genotypemap,geneId2symbol,this.metadata,this.LR_THRESHOLD);
                    caseoutput.outputFile();
                }
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
     */
    private Map<TermId, Gene2Genotype> getVcf2GenotypeMap(JannovarData jannovarData, MVStore mvstore, GenomeAssembly assembly) {

        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcfPath, jannovarData, mvstore, assembly);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.metadata = vcf2geno.getVcfMetaData();
        return genotypeMap;
    }

}
