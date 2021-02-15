package org.monarchinitiative.lirical.cmd;

import com.google.common.collect.Multimap;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.YamlParser;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms. This
 * analysis is driven by a YAML file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(name = "yaml",
        aliases = {"Y"},
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from YAML file")
public class YamlCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(YamlCommand.class);
    @CommandLine.Option(names = {"-y","--yaml"}, description = "path to yaml configuration file", required = true)
    private String yamlPath;
    /** Reference to the HPO. */
    private Ontology ontology;

    private  Map<TermId, HpoDisease> diseaseMap;

    private boolean phenotypeOnly;

    private PhenotypeLikelihoodRatio phenoLr;

    /** An object that contains parameters from the YAML file for configuration. */
    protected LiricalFactory factory;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected Map<TermId,String> geneId2symbol;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    protected Map<String,String> metadata;
    /** If true, the program will output an HTML file.*/
    @CommandLine.Option(names="--html", arity = "0..1", description = "Provide HTML output (default: ${DEFAULT-VALUE})")
    protected boolean outputHTML=true;
    /** If true, the program will output a Tab Separated Values file.*/
    @CommandLine.Option(names="--tsv", arity = "0..1", description = "Provide TSV output (default: ${DEFAULT-VALUE})")
    protected boolean outputTSV=false;
    /**
     * There are gene symbols returned by Jannovar for which we cannot find a geneId. This issues seems to be related
     * to the input files used by Jannovar from UCSC ( knownToLocusLink.txt.gz has links between ucsc ids, e.g.,
     * uc003fts.3, and NCBIGene ids (earlier known as locus link), e.g., 1370).
     */
    private Set<String> symbolsWithoutGeneIds;

    /**
     * Command pattern to coordinate analysis of a VCF file with LIRICAL.
     */
    public YamlCommand() {
    }



    private void runPhenotypeOnly() throws LiricalException {
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(factory.observedHpoTerms())
                .negated(factory.negatedHpoTerms())
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .phenotypeLr(phenoLr);
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        HpoCase hcase = evaluator.evaluate();
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .prefix(this.factory.getOutfilePrefix())
                .outdirectory(this.factory.getOutdir())
                .threshold(this.factory.getLrThreshold())
                .mindiff(this.factory.getMinDifferentials());
        writeOutput(builder);
    }

    private void writeOutput(LiricalTemplate.Builder builder) {
        writeOutput(builder.buildGenoPhenoHtmlTemplate(), outputHTML);
        writeOutput(builder.buildGenoPhenoTsvTemplate(), outputTSV);
    }

    private void writeOutput(LiricalTemplate template, boolean doWrite) {
        if (doWrite) {
            template.outputFile();
            logger.error("Done analysis of " + template.getOutPath());
        }
    }

    private void runVcf() throws LiricalException {
        this.geneId2symbol = factory.geneId2symbolMap();
        Map<TermId, Gene2Genotype> genotypeMap = factory.getGene2GenotypeMap();
        this.symbolsWithoutGeneIds = factory.getSymbolsWithoutGeneIds();
        this.metadata.put("vcf_file", factory.getVcfPath());
        this.metadata.put("n_filtered_variants", String.valueOf(factory.getN_filtered_variants()));
        this.metadata.put("n_good_quality_variants",String.valueOf(factory.getN_good_quality_variants()));
        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(factory.observedHpoTerms())
                .negated(factory.negatedHpoTerms())
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypeMap)
                .phenotypeLr(phenoLr)
                .global(factory.global())
                .gene2idMap(geneId2symbol)
                .genotypeLr(genoLr);
        this.metadata.put("transcriptDatabase", factory.transcriptdb());
        int n_genes_with_var = genotypeMap.size();
        this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath",factory.getExomiserPath());
        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .prefix(this.factory.getOutfilePrefix())
                .genotypeMap(genotypeMap)
                .outdirectory(this.factory.getOutdir())
                .geneid2symMap(geneId2symbol)
                .threshold(this.factory.getLrThreshold())
                .errors(evaluator.getErrors())
                .symbolsWithOutIds(symbolsWithoutGeneIds)
                .mindiff(this.factory.getMinDifferentials());
        writeOutput(builder);
    }




    @Override
    public Integer call() throws LiricalException {
        this.factory = deYamylate(this.yamlPath);
        this.ontology =  factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        this.metadata=new HashMap<>();
        this.metadata.put("sample_name", factory.getSampleName());
        this.metadata.put("analysis_date", factory.getTodaysDate());
        this.metadata.put("yaml", this.yamlPath);

        Map<String,String> ontologyMetainfo = factory.hpoOntology().getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.metadata.put("hpoVersion",ontologyMetainfo.get("data-version"));
        }

        if (this.phenotypeOnly) {
            runPhenotypeOnly();
        } else {
            if (factory.global()) { // global mode only makes sense for genomic analysis
                this.metadata.put("global_mode", "true");
            } else {
                this.metadata.put("global_mode", "false");
            }
            this.factory.qcExomiserFiles();
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            factory.qcExomiserFiles();
            factory.qcGenomeBuild();
            factory.qcVcfFile();
            runVcf();
        }
        return 0;
    }

    /**
     * Parse the YAML file and put the results into an {@link LiricalFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link LiricalFactory} object with various settings.
     */
    private LiricalFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        this.outputTSV = yparser.doTsv();
        this.outputHTML = yparser.doHtml();
        String hpoPath = yparser.getHpoPath();
        if (hpoPath == null || !(new File(hpoPath).exists())) {
            throw new PhenolRuntimeException("Could not find hp.obo file. Consider running download command first");
        }
        Ontology ontology = OntologyLoader.loadOntology(new File(hpoPath));
        if (yparser.phenotypeOnlyMode()) {
            phenotypeOnly=true;
            LiricalFactory.Builder builder = new LiricalFactory.Builder(ontology).
                    yaml(yparser,phenotypeOnly);
            return builder.buildForPhenotypeOnlyDiagnostics();
        } else {
            phenotypeOnly=false;
            LiricalFactory.Builder builder = new LiricalFactory.Builder(ontology).
                    yaml(yparser);
            return builder.buildForGenomicDiagnostics();
        }
    }
}
