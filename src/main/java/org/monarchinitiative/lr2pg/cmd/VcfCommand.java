package org.monarchinitiative.lr2pg.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.configuration.TranscriptDatabase;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.YamlParser;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Phenotype-driven analysis of VCF (Exome/Genome) data")
public class VcfCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(VcfCommand.class);
    @Parameter(names = {"-y","--yaml"}, description = "path to yaml configuration file", required = true)
    private String yamlPath;
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
        Map<TermId, Gene2Genotype> genotypeMap = factory.getGene2GenotypeMap();
        this.metadata.put("sample_name", factory.getSampleName());
        this.metadata.put("vcf_file", factory.vcfPath());
        this.metadata.put("n_filtered_variants", String.valueOf(factory.getN_filtered_variants()));
        this.metadata.put("n_good_quality_variants",String.valueOf(factory.getN_good_quality_variants()));
        this.metadata.put("analysis_date", factory.getTodaysDate());
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
                .keepCandidates(keepIfNoCandidateVariant)
                .gene2idMap(geneId2symbol)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();
        hcase.outputTopResults(5,ontology,genotypeMap);// TODO remove this outputs to the shell

        Map<String,String> ontologyMetainfo=ontology.getMetaInfo();
        if (ontologyMetainfo.containsKey("data-version")) {
            this.metadata.put("hpoVersion",ontologyMetainfo.get("data-version"));
        }
        this.metadata.put("transcriptDatabase", factory.transcriptdb());
        this.metadata.put("yaml", this.yamlPath);
        int n_genes_with_var=factory.getGene2GenotypeMap().size();
        this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath",factory.getExomiserPath());


        if (outputTSV) {
            outputTSV(hcase,ontology,genotypeMap);
        } else {
            outputHTML(hcase, ontology, genotypeMap);
        }
    }

    /**
     * Parse the YAML file and put the results into an {@link Lr2PgFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link Lr2PgFactory} object with various settings.
     */
    private Lr2PgFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder().
                yaml(yparser).
                filter(filterOnFILTER);
        Lr2PgFactory  factory = builder.buildForGenomicDiagnostics();
        return factory;
    }
}
