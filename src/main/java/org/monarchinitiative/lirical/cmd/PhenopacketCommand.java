package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Multimap;

import org.json.simple.parser.ParseException;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Download a number of files needed for LIRICAL analysis
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Run LIRICAL from a Phenopacket")
public class PhenopacketCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(PhenopacketCommand.class);
    @Parameter(names = {"-b", "--background"}, description = "path to non-default background frequency file")
    protected String backgroundFrequencyFile;
    @Parameter(names = {"-p", "--phenopacket"}, description = "path to phenopacket file", required = true)
    protected String phenopacketPath;
    @Parameter(names = {"-e", "--exomiser"}, description = "path to the Exomiser data directory")
    protected String exomiserDataDirectory;
    @Parameter(names={"--transcriptdb"}, description = "transcript database (UCSC, Ensembl, RefSeq)")
    protected String transcriptDb="ucsc";


    /**
     * If true, the phenopacket contains the path of a VCF file.
     */
    private boolean hasVcf;
    /**
     * List of HPO terms observed in the subject of the investigation.
     */
    private List<TermId> hpoIdList;
    /**
     * List of excluded HPO terms in the subject.
     */
    private List<TermId> negatedHpoIdList;
    /**
     * String representing the genome build (hg19 or hg38).
     */
    private String genomeAssembly;
    /**
     * Path to the VCF file (if any).
     */
    private String vcfPath = null;


    public PhenopacketCommand() {

    }

    @Override
    public void run() {
// read the Phenopacket
        logger.trace("Will analyze phenopacket at " + phenopacketPath);
        this.metadata = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", this.phenopacketPath);
        try {
            PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath);
            this.vcfPath = importer.getVcfPath();
            hasVcf = importer.hasVcf();
            if (hasVcf) {
                this.metadata.put("vcf_file", this.vcfPath);
            }
            this.genomeAssembly = importer.getGenomeAssembly();
            this.hpoIdList = importer.getHpoTerms();
            this.negatedHpoIdList = importer.getNegatedHpoTerms();
            metadata.put("sample_name", importer.getSamplename());
        } catch (ParseException pe) {
            logger.error("Could not parse phenopacket: {}", pe.getMessage());
            throw new LiricalRuntimeException("Could not parse Phenopacket at " + phenopacketPath + ": " + pe.getMessage());
        } catch (IOException e) {
            logger.error("Could not read phenopacket: {}", e.getMessage());
            throw new LiricalRuntimeException("Could not find Phenopacket at " + phenopacketPath + ": " + e.getMessage());
        }


        if (hasVcf) {
            LiricalFactory factory = new LiricalFactory.Builder()
                    .datadir(this.datadir)
                    .genomeAssembly(this.genomeAssembly)
                    .exomiser(this.exomiserDataDirectory)
                    .vcf(this.vcfPath)
                    .backgroundFrequency(this.backgroundFrequencyFile)
                    .build();
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            factory.qcExomiserFiles();
            factory.qcGenomeBuild();
            factory.qcVcfFile();

            Map<TermId, Gene2Genotype> genotypemap = factory.getGene2GenotypeMap();

            GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
            Ontology ontology = factory.hpoOntology();
            Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
            PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
            Multimap<TermId, TermId> disease2geneMultimap = factory.disease2geneMultimap();
            Map<TermId, String> geneId2symbol = factory.geneId2symbolMap();
            CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
                    .ontology(ontology)
                    .negated(this.negatedHpoIdList)
                    .diseaseMap(diseaseMap)
                    .disease2geneMultimap(disease2geneMultimap)
                    .genotypeMap(genotypemap)
                    .phenotypeLr(phenoLr)
                    .genotypeLr(genoLr);

            CaseEvaluator evaluator = caseBuilder.build();
            HpoCase hcase = evaluator.evaluate();


            if (!factory.transcriptdb().equals("n/a")) {
                this.metadata.put("transcriptDatabase", factory.transcriptdb());
            }
            int n_genes_with_var = factory.getGene2GenotypeMap().size();
            this.metadata.put("genesWithVar", String.valueOf(n_genes_with_var));
            this.metadata.put("exomiserPath", factory.getExomiserPath());
            this.metadata.put("hpoVersion", factory.getHpoVersion());
            LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,this.metadata)
                    .genotypeMap(genotypemap)
                    .geneid2symMap(this.geneId2symbol)
                    .outdirectory(this.outdir)
                    .prefix(this.outfilePrefix);
            LiricalTemplate template = outputTSV ?
                    builder.buildGenoPhenoTsvTemplate() :
                     builder.buildPhenotypeHtmlTemplate();
            template.outputFile();
        } else {
            // i.e., the Phenopacket has no VCF reference -- LIRICAL will work on just phenotypes!
            LiricalFactory factory = new LiricalFactory.Builder()
                    .datadir(this.datadir)
                    .build();
            factory.qcHumanPhenotypeOntologyFiles();
            factory.qcExternalFilesInDataDir();
            Ontology ontology = factory.hpoOntology();
            Map<TermId, HpoDisease> diseaseMap = factory.diseaseMap(ontology);
            PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
            CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
                    .ontology(ontology)
                    .negated(this.negatedHpoIdList)
                    .diseaseMap(diseaseMap)
                    .phenotypeLr(phenoLr);
            CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
            HpoCase hcase = evaluator.evaluate();
            this.metadata.put("hpoVersion", factory.getHpoVersion());
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
    }
}
