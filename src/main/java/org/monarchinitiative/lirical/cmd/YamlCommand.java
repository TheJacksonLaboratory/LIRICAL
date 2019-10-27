package org.monarchinitiative.lirical.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
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
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms. This
 * analysis is driven by a YAML file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Run LIRICAL from YAML file")
public class YamlCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(YamlCommand.class);
    @Parameter(names = {"-y","--yaml"}, description = "path to yaml configuration file", required = true)
    private String yamlPath;
    /** Reference to the HPO. */
    private Ontology ontology;

    private  Map<TermId,HpoDisease> diseaseMap;

    private boolean phenotypeOnly;

    private PhenotypeLikelihoodRatio phenoLr;

    /** If true, run with VCF file, otherwise, perform phenotype-only analysis. */
    private boolean hasVcf;

    /**
     * Command pattern to coordinate analysis of a VCF file with LIRICAL.
     */
    public YamlCommand() {
    }



    private HpoCase runPhenotypeOnly() throws LiricalException {
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(factory.observedHpoTerms())
                .negated(factory.negatedHpoTerms())
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .phenotypeLr(phenoLr);
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        return evaluator.evaluate();
    }

    private HpoCase runVcf() throws LiricalException {
        Map<TermId, Gene2Genotype> genotypeMap = factory.getGene2GenotypeMap();
        this.metadata.put("vcf_file", factory.getVcfPath());
        this.metadata.put("n_filtered_variants", String.valueOf(factory.getN_filtered_variants()));
        this.metadata.put("n_good_quality_variants",String.valueOf(factory.getN_good_quality_variants()));
        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();
        this.geneId2symbol = factory.geneId2symbolMap();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(factory.observedHpoTerms())
                .negated(factory.negatedHpoTerms())
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypeMap)
                .phenotypeLr(phenoLr)
                .global(globalAnalysisMode)
                .gene2idMap(geneId2symbol)
                .genotypeLr(genoLr);
        this.metadata.put("transcriptDatabase", factory.transcriptdb());
        int n_genes_with_var=factory.getGene2GenotypeMap().size();
        this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath",factory.getExomiserPath());
        CaseEvaluator evaluator = caseBuilder.build();
        return evaluator.evaluate();
    }




    @Override
    public void run() throws LiricalException {
        this.factory = deYamylate(this.yamlPath);
        this.ontology =  factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        this.metadata=new HashMap<>();
        this.metadata.put("sample_name", factory.getSampleName());
        this.metadata.put("analysis_date", factory.getTodaysDate());
        this.metadata.put("yaml", this.yamlPath);
        Ontology ontology = factory.hpoOntology();
        Map<TermId,HpoDisease> diseaseMap = factory.diseaseMap(ontology);
        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Map<String,String> ontologyMetainfo=ontology.getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.metadata.put("hpoVersion",ontologyMetainfo.get("data-version"));
        }
        HpoCase hcase;
        if (this.phenotypeOnly) {
            hcase =runPhenotypeOnly();
        } else {
            hcase=runVcf();
        }

        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .prefix(this.outfilePrefix)
                .outdirectory(this.outdir)
                .threshold(this.LR_THRESHOLD)
                .mindiff(this.minDifferentialsToShow);
        LiricalTemplate template = outputTSV ?
                builder.buildPhenotypeTsvTemplate() :
                builder.buildPhenotypeHtmlTemplate();
        template.outputFile();
        logger.error("Done analysis of " + outfilePrefix);
    }

    /**
     * Parse the YAML file and put the results into an {@link LiricalFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link LiricalFactory} object with various settings.
     */
    private LiricalFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        Optional<Integer> mindiff = yparser.mindiff();
        mindiff.ifPresent(i -> this.minDifferentialsToShow = i);
        Optional<Double> threshold = yparser.threshold();
        threshold.ifPresent(d -> this.LR_THRESHOLD = d);
        this.outfilePrefix = yparser.getPrefix();

        String hpoPath = yparser.getHpoPath();
        if (hpoPath == null || !(new File(hpoPath).exists())) {
            throw new PhenolRuntimeException("Could not find hp.obo file. Consider running download command first");
        }
        Ontology ontology = OntologyLoader.loadOntology(new File(hpoPath));

        if (yparser.getOutDirectory().isPresent()) {
            this.outdir=yparser.getOutDirectory().get();
        }

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
