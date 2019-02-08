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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Phenotype-driven analysis of VCF (Exome/Genome) data")
public class VcfCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(VcfCommand.class);
    @Parameter(names="--tsv",description = "Use TSV instead of HTML output")
    private boolean outputTSV=false;
    @Parameter(names = {"-y","--yaml"}, description = "path to yaml configuration file", required = true)
    private String yamlPath;
    /** The threshold for showing a differential diagnosis in the main section (posterior probability of 1%).*/
    @Parameter(names= {"-t","--threshold"}, description = "threshold for showing diagnosis in HTML output")
    private double LR_THRESHOLD=0.01;
    /** Prefix for the output files (defailt {@code lr2pg}). Can be set via the YAML file. */
    private String outfilePrefix="lr2pg";
    @Parameter(names={"-m","--mindiff"}, description = "minimal number of differential diagnoses to show")
    private int minDifferentialsToShow=5;
    /** An object that contains parameters from the YAML file for configuration. */
    private Lr2PgFactory factory;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    private Map<TermId,String> geneId2symbol;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lr2pg.output. */
    private Map<String,String> metadata;

    /**
     * Command pattern to coordinate analysis of a VCF file with LR2PG.
     */
    public VcfCommand() {
    }



    @Override
    public void run() throws Lr2pgException {
        this.factory = deYamylate(this.yamlPath);
        factory.qcYaml();
        this.metadata=new HashMap<>();
        String vcfFilePath = factory.vcfPath();
        MVStore mvstore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        GenomeAssembly assembly = factory.getAssembly();
        Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcfFilePath, jannovarData, mvstore, assembly);
        Map<TermId, Gene2Genotype> genotypeMap = vcf2geno.vcf2genotypeMap();
        this.metadata.put("sample_name", vcf2geno.getSamplename());
        this.metadata.put("vcf_file", vcfFilePath);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        vcf2geno=null;// no longer needed, GC if necessary
        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
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
        HtmlTemplate caseoutput = new HtmlTemplate(hcase,
                ontology,
                genotypeMap,
                this.geneId2symbol,
                this.metadata,
                this.LR_THRESHOLD,
                minDifferentialsToShow);
        caseoutput.outputFile(this.outfilePrefix);
    }

    /** Output a tab-separated values file with one line per differential diagnosis. */
    private void outputTSV(HpoCase hcase,Ontology ontology,Map<TermId, Gene2Genotype> genotypeMap) {
        Lr2pgTemplate template = new TsvTemplate(hcase,ontology,genotypeMap,this.geneId2symbol,this.metadata);
        template.outputFile(this.outfilePrefix);
    }


    /**
     * Parse the YAML file and put the results into an {@link Lr2PgFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link Lr2PgFactory} object with various settings.
     */
    private Lr2PgFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder().yaml(yparser);
        Lr2PgFactory  factory = builder.buildForGenomicDiagnostics();
        return factory;
    }
}
