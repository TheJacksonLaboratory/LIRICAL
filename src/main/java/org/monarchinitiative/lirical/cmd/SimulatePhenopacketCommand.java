package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Comparators;
import com.google.common.collect.Multimap;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.analysis.VcfSimulator;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.output.LiricalRanking;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Disease;
import org.phenopackets.schema.v1.core.HtsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;


/**
 * Simulate one or multiple VCFs from a Phenopacket that has HPO terms and a gene mutation. The mutation will
 * be "injected" into a template VCF file (this should be a "normal" VCF file), and LIRICAL will be run, and
 * the rank of the original diagnosis from the Phenopacket will be recorded. To run a single case,
 * <pre>
 *     java -jar LIRICAL.jar -p sample-phenopacket.json -e path/to/exomiser-datadir -v template.vcf
 * </pre>
 * Use the -m 25 option to show at least 25 differential diagnosis (by default, only diseases with posterior
 * probability above 1% are displayed).
 * <p></p>
 * In order to perform simulation on an entire directory of phenopackets, replace the -p option with
 * the --phenopacket-dir option.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@Parameters(commandDescription = "Simulate analysis from phenopacket (with or without VCF)", hidden = false)
public class SimulatePhenopacketCommand extends PhenopacketCommand {
    private static final Logger logger = LoggerFactory.getLogger(SimulatePhenopacketCommand.class);
    //TODO -- Get this from the VCF file or from the Phenopacket or from command line
    private String genomeAssembly = "GRCh37";
    @Parameter(names = {"-v", "--template-vcf"}, description = "path to template VCF file", required = true)
    private String templateVcfPath;
    @Parameter(names = {"--phenopacket-dir"}, description = "path to directory with multiple phenopackets")
    private String phenopacketDir;
    @Parameter(names = {"-outputfile"}, description = "name of the output file with simulation results")
    private String simulationOutFile="vcf_simulation_results.tsv";
    @Parameter(names = {"--phenotype-only"}, description = "run simulations with phenotypes only?")
    private boolean phenotypeOnly=false;
    @Parameter(names={"--output-vcf"}, description = "output a VCF file or files with results of the simulation")
    private boolean outputVCF = false;
    @Parameter(names={"--output-tsv"}, description = "output a TSV file or files with results of the simulation")
    private boolean outputTSV = false;


    private BufferedWriter simulationOutBuffer;
    private String simulatedDisease = null;
    private List<LiricalRanking> rankingsList;
    private GenotypeLikelihoodRatio genoLr;
    private PhenotypeLikelihoodRatio phenoLr;
    private Ontology ontology;
    private Map<TermId, HpoDisease> diseaseMap;
    private Multimap<TermId, TermId> disease2geneMultimap;
    private Map<TermId, String> geneId2symbol;
    private Map<TermId, Gene2Genotype> genotypemap;
    /** Ranks of diseases */
    private Map<String,Integer> disease2rankMap;
    /** Diseases for which we could not get a rank, they were filtered out e.g., because of the strict option. */
    private List<String> unrankedDiseases;
    /** The number of counts a certain rank was assigned */
    private Map<Integer,Integer> rank2countMap;

    private LiricalFactory factory;

    /**
     * No-op constructor meant to demo the phenotype LIRICAL algorithm by simulating some case based on
     * a phenopacket and a "normal" VCF file.
     */
    public SimulatePhenopacketCommand() {
    }

    /**
     * This method coordinates
     * @param phenopacketFile File with the Phenopacket we are currently analyzing
     */
    private void runOneVcfAnalysis(File phenopacketFile) throws IOException, ParseException {
        String phenopacketAbsolutePath = phenopacketFile.getAbsolutePath();
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketAbsolutePath,this.factory.hpoOntology());
        VcfSimulator vcfSimulator = new VcfSimulator(Paths.get(this.templateVcfPath));
        HtsFile simulatedVcf;
        try {
            simulatedVcf = vcfSimulator.simulateVcf(importer.getSamplename(), importer.getVariantList(), genomeAssembly);
            //pp = pp.toBuilder().clearHtsFiles().addHtsFiles(htsFile).build();
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not simulate VCF for phenopacket");
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketAbsolutePath);
        metadata.put("sample_name", importer.getSamplename());
        if (simulatedVcf == null) {
            System.err.println("[ERROR] Could not simulate VCF for "+phenopacketFile.getName()); // should never happen
            return; // skip to next Phenopacket
        }
        if (!importer.qcPhenopacket() ){
            System.err.println("[ERROR] Could not simulate VCF for "+phenopacketFile.getName());
            return;
        }


        Disease diagnosis = importer.getDiagnosis();
        simulatedDisease = diagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        TermId correctDiagnosisTermId = TermId.of(simulatedDisease);
        this.metadata.put("phenopacket.diagnosisId", simulatedDisease);
        this.metadata.put("phenopacket.diagnosisLabel", diagnosis.getTerm().getLabel());

       // HtsFile htsFile = importer.getVcfFile();
        String vcfPath = simulatedVcf.getFile().getPath();
        this.genotypemap = factory.getGene2GenotypeMap(vcfPath);
        this.genoLr = factory.getGenotypeLR();
        // this.metadata.put("vcf_file", this.getOptionalVcfPath);

        logger.trace("Running simulation from phenopacket {} with template VCF {}",
                phenopacketAbsolutePath,
                vcfPath);
        List<TermId> hpoIdList = importer.getHpoTerms();
        // List of excluded HPO terms in the subject.
        List<TermId> negatedHpoIdList = importer.getNegatedHpoTerms();
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

        Optional<Integer> optRank = hcase.getRank(correctDiagnosisTermId);
        int rank = optRank.isPresent() ? optRank.get() : hcase.getRankOfUnrankedDisease();
        disease2rankMap.put(diagnosis.getTerm().getLabel(),rank);
        rank2countMap.putIfAbsent(rank,0); // create key if needed.
        rank2countMap.merge(rank,1,Integer::sum); // increment count
        System.out.println(diagnosis.getTerm().getLabel() + ": " + rank);





        /*
        String outdir = ".";
        int n_genes_with_var = this.genotypemap.size();
        this.metadata.put("genesWithVar", String.valueOf(n_genes_with_var));
        this.metadata.put("exomiserPath", factory.getExomiserPath());
        this.metadata.put("hpoVersion", factory.getHpoVersion());


        if (outputTSV) {
            String outname=String.format("%s.tsv","temp");
            LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,metadata)
                    .genotypeMap(genotypemap)
                    .geneid2symMap(geneId2symbol)
                    .threshold(this.LR_THRESHOLD)
                    .mindiff(minDifferentialsToShow)
                    .outdirectory(outdir)
                    .prefix(outfilePrefix);
            TsvTemplate tsvtemplate = builder.buildGenoPhenoTsvTemplate();
            tsvtemplate.outputFile(outname);
            extractRank(outname,phenopacketFile.getName());
        } else {
            LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,metadata)
                    .genotypeMap(genotypemap)
                    .geneid2symMap(geneId2symbol)
                    .threshold(this.LR_THRESHOLD)
                    .mindiff(minDifferentialsToShow)
                    .outdirectory(outdir)
                    .prefix(outfilePrefix);
            HtmlTemplate htemplate = builder.buildGenoPhenoHtmlTemplate();
            htemplate.outputFile();

        }
    */
    }



    private void runOnePhenotypeOnlyAnalysis(File phenopacketFile) throws IOException, ParseException {
        String phenopacketAbsolutePath = phenopacketFile.getAbsolutePath();
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketAbsolutePath, this.factory.hpoOntology());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        this.metadata.put("analysis_date", dateFormat.format(date));
        this.metadata.put("phenopacket_file", phenopacketAbsolutePath);
        metadata.put("sample_name", importer.getSamplename());
        Disease diagnosis = importer.getDiagnosis();
        simulatedDisease = diagnosis.getTerm().getId(); // should be an ID such as OMIM:600102
        this.metadata.put("phenopacket.diagnosisId", simulatedDisease);
        this.metadata.put("phenopacket.diagnosisLabel", diagnosis.getTerm().getLabel());

        logger.trace("Running simulation from phenopacket {} (Phenotype only)", phenopacketAbsolutePath);
        List<TermId> hpoIdList = importer.getHpoTerms();
        // List of excluded HPO terms in the subject.
        List<TermId> negatedHpoIdList = importer.getNegatedHpoTerms();
        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(hpoIdList)
                .ontology(ontology)
                .negated(negatedHpoIdList)
                .diseaseMap(diseaseMap)
                .phenotypeLr(phenoLr);

        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        HpoCase hcase = evaluator.evaluate();
        TermId correctDiagnosisTermId = TermId.of(simulatedDisease);

        Optional<Integer> optRank = hcase.getRank(correctDiagnosisTermId);
        if (optRank.isPresent()) {
            int rank = optRank.get();
            disease2rankMap.put(diagnosis.getTerm().getLabel(), rank);
            rank2countMap.putIfAbsent(rank, 0); // create key if needed.
            rank2countMap.merge(rank, 1, Integer::sum); // increment count
            System.out.println(diagnosis.getTerm().getLabel() + ": " + rank);
        } else {
            unrankedDiseases.add(diagnosis.getTerm().getLabel());
        }
    }



    private void runWithVcf() {

        this.factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .genomeAssembly(genomeAssembly)
                .exomiser(this.exomiserDataDirectory)
                .transcriptdatabase(this.transcriptDb)
                .backgroundFrequency(this.backgroundFrequencyFile)
                .strict(this.strict)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();



        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        this.disease2geneMultimap = factory.disease2geneMultimap();
        this.geneId2symbol = factory.geneId2symbolMap();
        if (this.phenopacketPath != null) {
            logger.info("Running single file Phenopacket/VCF simulation at {}", phenopacketPath);
            try {
                runOneVcfAnalysis(new File(this.phenopacketPath));
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } else if (this.phenopacketDir != null) {
            outputTSV=true; // needed so that we can capture the results of the simulations across all cases
            logger.info("Running Phenopacket/VCF simulations at {}", phenopacketDir);
            final File folder = new File(phenopacketDir);
            if (! folder.isDirectory()) {
                throw new PhenolRuntimeException("Could not open Phenopackets directory at "+phenopacketDir);
            }
            int counter=0;
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".json")) {
                    logger.info("\tPhenopacket: \"{}\"", fileEntry.getAbsolutePath());
                    System.out.println(++counter + ") "+ fileEntry.getName());
                    try {
                        runOneVcfAnalysis(fileEntry);
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                //if (counter>4)break;
            }
        } else {
            System.err.println("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
            throw new LiricalRuntimeException("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
        }
        this.metadata.put("hpoVersion", factory.getHpoVersion());
    }

    private void runPhenotypeOnly() {

        this.factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .strict(this.strict)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();


        this.ontology = factory.hpoOntology();
        this.diseaseMap = factory.diseaseMap(ontology);
        this.phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        if (this.phenopacketPath != null) {
            logger.info("Running single file Phenopacket/VCF simulation at {}", phenopacketPath);
            try {
                runOnePhenotypeOnlyAnalysis(new File(this.phenopacketPath));
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        } else if (this.phenopacketDir != null) {
            outputTSV=true; // needed so that we can capture the results of the simulations across all cases
            logger.info("Running Phenopacket/VCF simulations at {}", phenopacketDir);
            final File folder = new File(phenopacketDir);
            if (! folder.isDirectory()) {
                throw new PhenolRuntimeException("Could not open Phenopackets directory at "+phenopacketDir);
            }
            int counter=0;
            for (final File fileEntry : folder.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".json")) {
                    logger.info("\tPhenopacket: \"{}\"", fileEntry.getAbsolutePath());
                    System.out.println(++counter + ") "+ fileEntry.getName());
                    try {
                        runOnePhenotypeOnlyAnalysis(fileEntry);
                    } catch (IOException | ParseException e) {
                        e.printStackTrace();
                    }
                }
                //if (counter>4)break;
            }
        } else {
            System.err.println("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
            throw new LiricalRuntimeException("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
        }
    }



    private void outputRankings() {
        // sort the map by values first
        Map<Integer, Integer> sorted = this.rank2countMap
                .entrySet()
                .stream()
                //.sorted(comparingByValue())
                .sorted(comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                                LinkedHashMap::new));
        int total = this.rank2countMap.values().stream().mapToInt(i->i).sum();
        for (Map.Entry<Integer, Integer> e : sorted.entrySet()) {
            System.out.println(String.format("%s: %d (%.1f%%)", e.getKey(), e.getValue(),(100.0*e.getValue()/total)));
        }
        // output two files.
        // 1. rank2count.txt

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("rank2count.txt"))){
            for (Map.Entry<Integer, Integer> e : sorted.entrySet()) {
                writer.write(e.getKey() + ": " + e.getValue() +"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2. simulation-results.txt (show one line with the rank of each simulated disease).
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter("rankedSimulations.txt"));
            for (String diseaseLabel : disease2rankMap.keySet()) {
                br.write(diseaseLabel + ": " + disease2rankMap.get(diseaseLabel) +"\n");
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        disease2rankMap = new HashMap<>();
        rank2countMap=new HashMap<>();
        unrankedDiseases=new ArrayList<>();
        try {
            simulationOutBuffer = new BufferedWriter(new FileWriter(this.simulationOutFile));
            simulationOutBuffer.write(LiricalRanking.header()+"\n");
        } catch (IOException e) {
            throw new LiricalRuntimeException("Could not open " + simulationOutFile + " for writing");
        }
        if (phenotypeOnly) {
            runPhenotypeOnly();
        } else {
            runWithVcf();
        }
        int total_rank = 0;
        int N = 0;
        for (LiricalRanking lrank : this.rankingsList) {
            total_rank += lrank.getRank();
            N++;

        }
        double avgrank = (double) total_rank / N;
        logger.info("Average rank from " + N + " simulations was " + avgrank);
        try {
            this.simulationOutBuffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputRankings();
    }

}
