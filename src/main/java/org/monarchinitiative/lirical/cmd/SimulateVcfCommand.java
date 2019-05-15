package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.protobuf.util.JsonFormat;
import org.h2.mvstore.WriteBuffer;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.analysis.VcfSimulator;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.HtmlTemplate;
import org.monarchinitiative.lirical.output.LiricalRanking;
import org.monarchinitiative.lirical.output.LiricalTemplate;
import org.monarchinitiative.lirical.output.TsvTemplate;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.OntologyClass;
import org.phenopackets.schema.v1.core.Phenotype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;

@Parameters(commandDescription = "Simulate VCF analysis from phenopacket", hidden = false)
public class SimulateVcfCommand extends PrioritizeCommand {
    private static final Logger logger = LoggerFactory.getLogger(SimulateVcfCommand.class);
    //TODO -- Get this from the VCF file or from the Phenopacket or from command line
    private String genomeAssembly = "GRCh37";
    @Parameter(names = {"-p", "--phenopacket"}, description = "path to phenopacket file")
    private String phenopacketPath;
    @Parameter(names = {"-e", "--exomiser"}, description = "path to the Exomiser data directory")
    private String exomiserDataDirectory;
    @Parameter(names = {"-v", "--template-vcf"}, description = "path to template VCF file", required = true)
    private String templateVcfPath;
    @Parameter(names = {"-b", "--background"}, description = "path to non-default background frequency file")
    private String backgroundFrequencyFile;
    @Parameter(names = {"--phenopacket-dir"}, description = "path to directory with multiple phenopackets")
    private String phenopacketDir;
    @Parameter(names = {}, description = "name of the output file with simulation results")
    private String simulationOutFile="vcf_simulation_results.txt";

    private BufferedWriter simulationOutBuffer;
    /**
     * If true, the phenopacket contains the path of a VCF file.
     */
    private boolean hasVcf;
    private String simulatedDisease = null;
    private List<LiricalRanking> rankingsList;
    private GenotypeLikelihoodRatio genoLr;
    private PhenotypeLikelihoodRatio phenoLr;
    private Ontology ontology;
    private Map<TermId, HpoDisease> diseaseMap;
    private Multimap<TermId, TermId> disease2geneMultimap;
    private Map<TermId, String> geneId2symbol;
    private Map<TermId, Gene2Genotype> genotypemap;

    private LiricalFactory factory;

    /**
     * No-op constructor meant to demo the phenotype LR2PG algorithm by simulating some case based on
     * a phenopacket and a "normal" VCF file.
     */
    public SimulateVcfCommand() {
    }

    private static Phenopacket readPhenopacket(String phenopacketPath) {
        Path ppPath = Paths.get(phenopacketPath);
        Phenopacket.Builder ppBuilder = Phenopacket.newBuilder();
        try (BufferedReader reader = Files.newBufferedReader(ppPath)) {
            JsonFormat.parser().merge(reader, ppBuilder);
        } catch (IOException e) {
            logger.warn("Unable to read/decode file '{}'", ppPath);
            throw new LiricalRuntimeException(String.format("Unable to read/decode file '%s'", ppPath));
        }
        return ppBuilder.build();
    }

    /**
     * This method coordinates
     * @param phenopacketAbsolutePath
     */
    private void runOneVcfAnalysis(String phenopacketAbsolutePath) {

        Phenopacket pp = readPhenopacket(phenopacketAbsolutePath);
        VcfSimulator vcfSimulator = new VcfSimulator(Paths.get(this.templateVcfPath));
        try {
            HtsFile htsFile = vcfSimulator.simulateVcf(pp.getSubject().getId(), pp.getVariantsList(), genomeAssembly);
            pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not simulate VCF for phenopacket");
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketAbsolutePath);
        metadata.put("sample_name", pp.getSubject().getId());
        hasVcf = pp.getHtsFilesList().stream().anyMatch(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF));
        if (pp.getDiseasesCount() != 1) {
            System.err.println("[ERROR] to run this simulation a phenoopacket must have exactly one disease diagnosis");
            System.err.println("[ERROR]  " + pp.getSubject().getId() + " had " + pp.getDiseasesCount());
            return; // skip to next Phenopacket
        }
        if (!hasVcf) {
            System.err.println("[ERROR] Could not simulate VCF"); // should never happen
            return; // skip to next Phenopacket
        }



        Disease diagnosis = pp.getDiseases(0);
        simulatedDisease = diagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.metadata.put("phenopacket.diagnosisId", simulatedDisease);
        this.metadata.put("phenopacket.diagnosisLabel", diagnosis.getTerm().getLabel());

        HtsFile htsFile = pp.getHtsFilesList().stream()
                .filter(hf -> hf.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .findFirst()
                .orElseThrow(() -> new LiricalRuntimeException("Phenopacket has and has not VCF file in the same time... \uD83D\uDE15"));
        String vcfPath = htsFile.getFile().getPath();
        this.genotypemap = factory.getGene2GenotypeMap(vcfPath);
        this.genoLr = factory.getGenotypeLR();
        // this.metadata.put("vcf_file", this.getOptionalVcfPath);

        logger.trace("Running simulation from phenopacket {} with template VCF {}",
                phenopacketAbsolutePath,
                vcfPath);
        List<TermId> hpoIdList = pp.getPhenotypesList() // copied from PhenopacketImporter
                .stream()
                .distinct()
                .filter(((Predicate<Phenotype>) Phenotype::getNegated).negate()) // i.e., just take non-negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());
        // List of excluded HPO terms in the subject.
        List<TermId> negatedHpoIdList = pp.getPhenotypesList() // copied from PhenopacketImporter
                .stream()
                .filter(Phenotype::getNegated) // i.e., just take negated phenotypes
                .map(Phenotype::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(ImmutableList.toImmutableList());


        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                .ontology(ontology)
                .negated(negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .disease2geneMultimap(disease2geneMultimap)
                .genotypeMap(genotypemap)
                .phenotypeLr(phenoLr)
                .genotypeLr(genoLr);

        CaseEvaluator evaluator = caseBuilder.build();
        HpoCase hcase = evaluator.evaluate();

        String outdir = ".";
        int n_genes_with_var = this.genotypemap.size();
        this.metadata.put("genesWithVar", String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath", factory.getExomiserPath());
        this.metadata.put("hpoVersion", factory.getHpoVersion());


        if (outputTSV) {
            LiricalTemplate template = new TsvTemplate(hcase, ontology, genotypemap, this.geneId2symbol, this.metadata);
            String outname=String.format("%s.tsv","temp");
            template.outputFile(outname);
            extractRank(outname);
        } else {
            HtmlTemplate caseoutput = new HtmlTemplate(hcase,
                    ontology,
                    genotypemap,
                    this.geneId2symbol,
                    this.metadata,
                    this.LR_THRESHOLD,
                    minDifferentialsToShow);
            caseoutput.outputFile(this.outfilePrefix, outdir);
        }
    }

    /**
     * This can be run in a single phenopacket mode (in which case phenopacketPath needs to be defined) or in
     * multi-phenopacket mode (in which case phenopacketDir needs to be defined).
     */
    @Override
    public void run()  {
        rankingsList = new ArrayList<>();
        this.metadata = new HashMap<>();
        try {
            simulationOutBuffer = new BufferedWriter(new FileWriter(this.simulationOutFile));
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not open " + simulationOutFile + " for writing");
        }
        this.factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .genomeAssembly(genomeAssembly)
                .exomiser(this.exomiserDataDirectory)
                // .vcf(getOptionalVcfPath)
                .transcriptdatabase(this.transcriptDb)
                .backgroundFrequency(this.backgroundFrequencyFile)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();
      //  factory.qcVcfFile();


        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        this.disease2geneMultimap = factory.disease2geneMultimap();
        this.geneId2symbol = factory.geneId2symbolMap();
        if (this.phenopacketPath != null) {
            logger.info("Running single file Phenopacket/VCF simulation at {}", phenopacketPath);
            runOneVcfAnalysis(this.phenopacketPath);
        } else if (this.phenopacketDir != null) {
            logger.info("Running Phenopacket/VCF simulations at {}", phenopacketDir);
            final File folder = new File(phenopacketDir);
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".json")) {
                    logger.info("\tPhenopacket: \"{}\"", fileEntry.getAbsolutePath());
                    runOneVcfAnalysis(fileEntry.getAbsolutePath());
                }
            }
        } else {
            System.err.println("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
            throw new LiricalRuntimeException("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
        }

        int total_rank = 0;
        int N = 0;
        for (LiricalRanking lrank : this.rankingsList) {
            total_rank += lrank.getRank();
            N++;
            System.out.println(lrank.toString());
        }
        double avgrank = (double) total_rank / N;
        logger.info("Average rank from " + N + " simulations was " + avgrank);
    }

    private int extractRank(String path) {
        int rank = -1;
        int n_over_50=0; // number of differentials with post prob over 50%
        int n_total=0; // total number of differentials
        LiricalRanking lr=null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("!")) continue;
                if (line.startsWith("rank")) continue;
                String[] fields = line.split("\t");
                rank = Integer.parseInt(fields[0]);
                String diseaseName = fields[1];
                String diseaseCurie = fields[2];
                String posttest = fields[4].replace("%","");// remove the percent sign
                if (diseaseCurie.equals(this.simulatedDisease)) {
                    logger.info("Got rank of {} for simulated disease {}", rank, simulatedDisease);
                    lr = new LiricalRanking(rank, line);
                    rankingsList.add(lr);
                }
                Double posttestprob = Double.parseDouble(posttest);
                if (posttestprob>50.0) n_over_50++;
                n_total++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String res = String.format("%s: posttest prob > 50%%: %d/%d",this.simulatedDisease,n_over_50,n_total);
        if (lr!=null) {
            lr.addExplanation(res);
        }
        logger.info(res);
        // We should never get here. If we do, then probably the OMIM id used in the Phenopacket
        // is incorrect or outdated.
        // This command is not intended for general consumption. Therefore, it is better
        // to terminate the program and correct the error rather than just continuing.
        if (rank==-1) {
            System.err.println("[ERROR] Could not find rank of simulated disease \"" +simulatedDisease + "\"");
            System.exit(1);
        }
        return rank;
    }


}
