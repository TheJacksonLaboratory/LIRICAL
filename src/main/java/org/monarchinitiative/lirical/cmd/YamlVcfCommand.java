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
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms. This
 * analysis is driven by a YAML file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Phenotype-driven analysis of VCF (Exome/Genome) data")
public class YamlVcfCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(YamlVcfCommand.class);
    @Parameter(names = {"-y","--yaml"}, description = "path to yaml configuration file", required = true)
    private String yamlPath;

    /**
     * Command pattern to coordinate analysis of a VCF file with LIRICAL.
     */
    public YamlVcfCommand() {
    }



    @Override
    public void run() throws LiricalException {
        this.factory = deYamylate(this.yamlPath);
        factory.qcYaml();
        this.metadata=new HashMap<>();
        Map<TermId, Gene2Genotype> genotypeMap = factory.getGene2GenotypeMap();
        this.metadata.put("sample_name", factory.getSampleName());
        this.metadata.put("vcf_file", factory.vcfPath());
        this.metadata.put("n_filtered_variants", String.valueOf(factory.getN_filtered_variants()));
        this.metadata.put("n_good_quality_variants",String.valueOf(factory.getN_good_quality_variants()));
        this.metadata.put("analysis_date", factory.getTodaysDate());
        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
        Ontology ontology = factory.hpoOntology();
        Map<TermId,HpoDisease> diseaseMap = factory.diseaseMap(ontology);

        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Multimap<TermId,TermId> disease2geneMultimap = factory.disease2geneMultimap();
        this.geneId2symbol = factory.geneId2symbolMap();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(factory.observedHpoTerms())
                .negated(factory.negatedHpoTerms())
                .ontology(ontology)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypeMap)
                .phenotypeLr(phenoLr)
                .keepCandidates(keepIfNoCandidateVariant)
                .gene2idMap(geneId2symbol)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();


        Map<String,String> ontologyMetainfo=ontology.getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.metadata.put("hpoVersion",ontologyMetainfo.get("data-version"));
        }
        this.metadata.put("transcriptDatabase", factory.transcriptdb());
        this.metadata.put("yaml", this.yamlPath);
        int n_genes_with_var=factory.getGene2GenotypeMap().size();
        this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath",factory.getExomiserPath());
        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                .prefix(this.outfilePrefix)
                .outdirectory(this.outdir)
                .threshold(this.LR_THRESHOLD)
                .mindiff(this.minDifferentialsToShow);
        LiricalTemplate template = outputTSV ?
                builder.buildPhenotypeTsvTemplate() :
                builder.buildPhenotypeHtmlTemplate();
        template.outputFile();
    }

    /**
     * Parse the YAML file and put the results into an {@link LiricalFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link LiricalFactory} object with various settings.
     */
    private LiricalFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        LiricalFactory.Builder builder = new LiricalFactory.Builder().
                yaml(yparser);
        this.outfilePrefix = yparser.getPrefix();
        return builder.buildForGenomicDiagnostics();
    }
}
