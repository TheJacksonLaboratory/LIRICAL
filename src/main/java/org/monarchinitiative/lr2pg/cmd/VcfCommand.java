package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Multimap;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.lr2pg.io.YamlParser;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.output.HtmlTemplate;
import org.monarchinitiative.lr2pg.output.Lr2pgTemplate;
import org.monarchinitiative.lr2pg.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Phenotype-driven analysis of VCF (Exome/Genome) data")
public class VcfCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(VcfCommand.class);
    /** An object that contains parameters from the YAML file for configuration. */
    private Lr2PgFactory factory;
    /** Default name of the background frequency file. */
    private final String BACKGROUND_FREQUENCY_FILE="background-freq.txt";
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    private Map<TermId,String> geneId2symbol;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lr2pg.output. */
    private Map<String,String> metadata;



    @Parameter(names="--tsv",description = "Use TSV instead of HTML output")
    private boolean outputTSV=false;
    @Parameter(names = {"-y","--yaml"}, description = "path to yaml configuration file")
    private String yamlPath;
    /** Directory where various files are downloaded/created. */
    @Parameter(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    private String datadir="data";
    /** The threshold for showing a differential diagnosis in the main section (posterior probability of 1%).*/
    @Parameter(names= {"-t","--threshold"}, description = "threshold for showing diagnosis in HTML output")
    private double LR_THRESHOLD=0.01;


    /**
     * Command pattern to coordinate analysis of a VCF file with LR2PG.
     */
    public VcfCommand() {
    }



    /**
     * Identify the variants and genotypes from the VCF file.
     * @return a map with key: An NCBI Gene Id, and value: corresponding {@link Gene2Genotype} object.
     * @throws Lr2pgException upon error parsing the VCF file or creating the Jannovar object
     */
    private Map<TermId, Gene2Genotype> getVcf2GenotypeMap() throws Lr2pgException {
        String vcf = factory.vcfPath();
        MVStore mvstore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcf, jannovarData, mvstore, GenomeAssembly.HG19);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.metadata = vcf2geno.getVcfMetaData();
        return genotypeMap;
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


    private void showParams() {
        System.out.println("LR2PG: vcf parameters");
        System.out.println("\tvcf file:" + factory.vcfPath());
        System.out.println("\tYAML config file: "+yamlPath);
        System.out.println("\tMVStore file:" + factory.mvStore());
        System.out.println("\tUse TSV?: "+outputTSV);
        System.out.println("\tdata directory: "+datadir);
        System.out.println("\tthreshold: "+LR_THRESHOLD);
       // System.out.println("\tJannovar file:" + factory.jannovarData().);


    }




    public void run() throws Lr2pgException {
        this.factory  = deYamylate(this.yamlPath);
        showParams();



        Map<TermId, Gene2Genotype> genotypeMap = getVcf2GenotypeMap();
        //debugPrintGenotypeMap(genotypeMap);
        GenotypeLikelihoodRatio genoLr = getGenotypeLR();
        List<TermId> observedHpoTerms = factory.observedHpoTerms();
        Ontology ontology = factory.hpoOntology();
        Map<TermId,HpoDisease> diseaseMap = factory.diseaseMap(ontology);

        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();
        this.geneId2symbol = factory.geneId2symbolMap();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(observedHpoTerms)
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypeMap)
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();
        hcase.outputTopResults(5,ontology,genotypeMap);// TODO remove this outputs to the shell
        if (outputTSV) {
            outputTSV(hcase,ontology,genotypeMap);
        } else {
            outputHTML(hcase, ontology, genotypeMap);
        }
    }


    private void outputHTML(HpoCase hcase,Ontology ontology,Map<TermId, Gene2Genotype> genotypeMap) {
        HtmlTemplate caseoutput = new HtmlTemplate(hcase,ontology,genotypeMap,this.geneId2symbol,this.metadata,this.LR_THRESHOLD);
        caseoutput.outputFile();
    }

    /** Output a tab-separated values file with one line per differential diagnosis. */
    private void outputTSV(HpoCase hcase,Ontology ontology,Map<TermId, Gene2Genotype> genotypeMap) {
        Lr2pgTemplate template = new TsvTemplate(hcase,ontology,genotypeMap,this.geneId2symbol,this.metadata);
        template.outputFile();
    }










    private void debugPrintGenotypeMap(Map<TermId, Gene2Genotype> genotypeMap) {
        logger.error("debug print");
        int i=0;
        int N=genotypeMap.size();
        for (TermId geneId : genotypeMap.keySet()) {
            Gene2Genotype g2g = genotypeMap.get(geneId);
            double path = g2g.getSumOfPathBinScores();
            String symbol = g2g.getSymbol();
            String s = String.format("%s [%s] path: %.3f",symbol,geneId.getValue(),path);
            if (g2g.hasPredictedPathogenicVar()) {
                System.out.println(++i +"/"+N+") "+s);
            }
        }
    }

    /**
     * Parse the YAML file and put the results into an {@link Lr2PgFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link Lr2PgFactory} object with various settings.
     */
    private Lr2PgFactory deYamylate(String yamlPath) {

        Lr2PgFactory factory = null;
        try {
            YamlParser yparser = new YamlParser(yamlPath);
            Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder().
                    hp_obo(yparser.getHpOboPath()).
                    mvStore(yparser.getMvStorePath())
                    .mim2genemedgen(yparser.getMedgen())
                    .geneInfo(yparser.getGeneInfo())
                    .phenotypeAnnotation(yparser.phenotypeAnnotation())
                    .observedHpoTerms(yparser.getHpoTermList())
                    .vcf(yparser.vcfPath()).
                            jannovarFile(yparser.jannovarFile());
            factory = builder.build();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
        return factory;
    }
}
