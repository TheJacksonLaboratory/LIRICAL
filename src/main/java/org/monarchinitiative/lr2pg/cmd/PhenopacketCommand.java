package org.monarchinitiative.lr2pg.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.protobuf.util.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2PgRuntimeException;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.io.PhenopacketImporter;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Download a number of files needed for the analysis
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Run LR2PG from a Phenopacket")
public class PhenopacketCommand extends PrioritizeCommand {
    private static final Logger logger = LogManager.getLogger();
    @Parameter(names={"-b","--background"}, description = "path to non-default background frequency file")
    private String backgroundFrequencyFile;
    @Parameter(names = {"-p","--phenopacket"}, description = "path to phenopacket file", required = true)
    private String phenopacketPath;
    @Parameter(names={"-e","--exomiser"}, description = "path to the Exomiser data directory")
    private String exomiserDataDirectory;
    @Parameter(names = {"-v", "--template-vcf"}, description = "path to template VCF file", required = true)
    private String templateVcfPath;

    /** If true, the phenopacket contains the path of a VCF file. */
    private boolean hasVcf;
    /** List of HPO terms observed in the subject of the investigation. */
    private List<TermId> hpoIdList;
    /** List of excluded HPO terms in the subject. */
    private List<TermId> negatedHpoIdList;
    /** String representing the genome build (hg19 or hg38). */
    private String genomeAssembly;
    /** Path to the VCF file (if any). */
    private String vcfPath=null;


    public PhenopacketCommand(){

    }

    @Override
    public void run() {
// read the Phenopacket
        logger.trace("Will analyze phenopacket at " + phenopacketPath);
        this.metadata=new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", this.phenopacketPath);

        // read phenopacket
        Phenopacket pp  = readPhenopacket(phenopacketPath);

        // simulate VCF file
        // this file is deleted upon JVM exit
        String genomeAssembly = "GRCh37";
        SingleVcfSimulator vcfSimulator = new SingleVcfSimulator(Paths.get(templateVcfPath));
        try {
            HtsFile htsFile = vcfSimulator.simulateVcf(pp.getSubject().getId(), pp.getVariantsList(), genomeAssembly);
            pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new Lr2PgRuntimeException("Could not simulate VCF for phenopacket");
        }

        /*
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
        }   catch (ParseException pe) {
            logger.fatal("Could not parse phenopacket: {}", pe.getMessage());
            throw new Lr2PgRuntimeException("Could not parse Phenopacket at " + phenopacketPath +": "+pe.getMessage());
        } catch (IOException e) {
            logger.fatal("Could not read phenopacket: {}", e.getMessage());
            throw new Lr2PgRuntimeException("Could not find Phenopacket at " + phenopacketPath +": "+e.getMessage());
        }
        */

        // This code replaces the functionality from above
        // at least one HtsFile has VCF format
        hasVcf = pp.getHtsFilesList().stream().anyMatch(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF));
        if (hasVcf) {
            HtsFile htsFile = pp.getHtsFilesList().stream()
                    .filter(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                    .findFirst()
                    .orElseThrow(() -> new Lr2PgRuntimeException("Phenopacket has and has not VCF file in the same time... \uD83D\uDE15"));
            this.vcfPath = htsFile.getFile().getPath();
            this.metadata.put("vcf_file", this.vcfPath);
        }

        this.genomeAssembly = genomeAssembly; // hardcoded for now, TODO - revise
        this.hpoIdList = pp.getPhenotypesList() // copied from PhenopacketImporter
                .stream()
                .distinct()
                .filter(((Predicate<Phenotype>) Phenotype::getNegated).negate()) // i.e., just take non-negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());
        this.negatedHpoIdList = pp.getPhenotypesList() // copied from PhenopacketImporter
                .stream()
                .filter(Phenotype::getNegated) // i.e., just take negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());

        metadata.put("sample_name", pp.getSubject().getId());
        if (hasVcf) {

            try {
                Lr2PgFactory factory = new Lr2PgFactory.Builder()
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


                if (! factory.transcriptdb().equals("n/a")) {
                    this.metadata.put("transcriptDatabase", factory.transcriptdb());
                }
                int n_genes_with_var=factory.getGene2GenotypeMap().size();
                this.metadata.put("genesWithVar",String.valueOf(n_genes_with_var));
                this.metadata.put("exomiserPath",factory.getExomiserPath());
                this.metadata.put("hpoVersion",factory.getHpoVersion());

                if (outputTSV) {
                    outputTSV(hcase,ontology,genotypemap);
                } else {
                    outputHTML(hcase,ontology,genotypemap);
                }
            } catch (Lr2pgException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // i.e., the Phenopacket has no VCF reference -- LR2PG will work on just phenotypes!
                Lr2PgFactory factory = new Lr2PgFactory.Builder()
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
                this.metadata.put("hpoVersion",factory.getHpoVersion());
                if (outputTSV) {
                    outputTSV(hcase,ontology);
                } else {
                    outputHTML(hcase,ontology);
                }
            } catch (Lr2pgException e) {
                e.printStackTrace();
            }
        }
    }

    private static Phenopacket readPhenopacket(String phenopacketPath) {
        Path ppPath = Paths.get(phenopacketPath);
        Phenopacket.Builder ppBuilder = Phenopacket.newBuilder();
        try (BufferedReader reader = Files.newBufferedReader(ppPath)) {
            JsonFormat.parser().merge(reader, ppBuilder);
        } catch (IOException e) {
            logger.warn("Unable to read/decode file '{}'", ppPath);
            throw new Lr2PgRuntimeException(String.format("Unable to read/decode file '%s'", ppPath));
        }
        return ppBuilder.build();
    }
}
