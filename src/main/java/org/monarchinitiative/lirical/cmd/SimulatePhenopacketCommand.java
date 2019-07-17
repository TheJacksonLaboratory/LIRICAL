package org.monarchinitiative.lirical.cmd;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.json.simple.parser.ParseException;
import org.monarchinitiative.lirical.simulation.PhenoGenoCaseSimulator;
import org.monarchinitiative.lirical.simulation.PhenoOnlyCaseSimulator;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.output.LiricalRanking;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByKey;


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
    @Parameter(names = {"-a","--assembly"})
    private String genomeAssembly = "GRCh37";
    @Parameter(names = {"-v", "--template-vcf"}, description = "path to template VCF file", required = true)
    private String templateVcfPath;
    @Parameter(names = {"--phenopacket-dir"}, description = "path to directory with multiple phenopackets")
    private String phenopacketDir;
    @Parameter(names = {"-outputfile"}, description = "name of the output file with simulation results")
    private String simulationOutFile = null;
    @Parameter(names = {"--phenotype-only"}, description = "run simulations with phenotypes only?")
    private boolean phenotypeOnly=false;
    @Parameter(names={"--output-vcf"}, description = "output a VCF file or files with results of the simulation")
    private boolean outputVCF = false;
    @Parameter(names={"--output-tsv"}, description = "output a TSV file or files with results of the simulation")
    private boolean outputTSV = false;
    @Parameter(names={"--random"},description = "randomize the HPO terms from the phenopacket")
    private boolean randomize = false;
    /** If true, output HTML or TSV */
    private boolean outputFiles = false;

    private List<LiricalRanking> rankingsList;
    /** Each entry in this list represents one simulated case with various data about the simulation. */
    private List<String> detailedResultLineList;

    /** The number of counts a certain rank was assigned */
    private Map<Integer,Integer> rank2countMap;
    /** key-- rank in a simulation; value -- number of times the rank was achieved. */
    private Map<Integer,Integer> geneRank2CountMap;

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
        PhenoGenoCaseSimulator simulator = new PhenoGenoCaseSimulator(phenopacketFile, this.templateVcfPath, this.factory, this.randomize);
        simulator.run();
        int diseaseRank = simulator.getRank_of_disease();
        int geneRank    = simulator.getRank_of_gene();
        String diseaseLabel = simulator.getDiagnosisLabel();
        detailedResultLineList.add(simulator.getDetails());
        System.out.println(simulator.getDetails());
        //disease2rankMap.put(diseaseLabel,diseaseRank);
        rank2countMap.putIfAbsent(diseaseRank,0); // create key if needed.
        rank2countMap.merge(diseaseRank,1,Integer::sum); // increment count

        geneRank2CountMap.putIfAbsent(geneRank,0);
        geneRank2CountMap.merge(geneRank,1,Integer::sum); // increment count


        if (outputTSV) {
            simulator.outputTsv(outfilePrefix,LR_THRESHOLD,minDifferentialsToShow,outdir);
        } else {
            simulator.outputHtml(outfilePrefix,LR_THRESHOLD,minDifferentialsToShow,outdir);
        }

    }



    private void runOnePhenotypeOnlyAnalysis(File phenopacketFile) throws IOException, ParseException {

        PhenoOnlyCaseSimulator simulator = new PhenoOnlyCaseSimulator(phenopacketFile,this.factory);

        int rank = simulator.getRank_of_disease();
        String diseaseLabel = simulator.getDiagnosisLabel();
        detailedResultLineList.add(simulator.getDetails());
        rank2countMap.putIfAbsent(rank, 0); // create key if needed.
        rank2countMap.merge(rank, 1, Integer::sum); // increment count
        System.out.println(diseaseLabel + ": " + rank);

    }


    /**
     * Run one or multiple simulations that are driven from one or multiple phenopackets. Each simulation
     * will add pathogenic allele(s) from the phenopacket to the otherwise background VCF file at
     * {@link #templateVcfPath}. The function {@link #runOneVcfAnalysis(File)} will determine the
     * rank of the correct diagnosis as represented in the Phenopacket.
     */
    private void runWithVcf() {

        this.factory = new LiricalFactory.Builder()
                .datadir(this.datadir)
                .genomeAssembly(genomeAssembly)
                .exomiser(this.exomiserDataDirectory)
                .transcriptdatabase(this.transcriptDb)
                .backgroundFrequency(this.backgroundFrequencyFile)
                .keep(this.keepIfNoCandidateVariant)
                .strict(this.strict)
                .build();
        factory.qcHumanPhenotypeOntologyFiles();
        factory.qcExternalFilesInDataDir();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();


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



    private String getSettingsString() {
        List<String> settings= new ArrayList<>();
        settings.add("phenopacket-dir: "+phenopacketDir!=null?phenopacketDir:"n/a");
        settings.add("phenopackets: n=" + this.rankingsList.size());
        settings.add("string: " + (strict ? "true" : "false"));
        settings.add("random: " + (randomize ?  "true" : "false"));
        settings.add("phenotypeOnly: "+ (phenotypeOnly? "true":"false"));
        settings.add("transcriptDb: " + this.transcriptDb);

        return String.join(";",settings);
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

        // now gene ranks
        sorted = this.geneRank2CountMap
                .entrySet()
                .stream()
                //.sorted(comparingByValue())
                .sorted(comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2,
                        LinkedHashMap::new));
        total = this.geneRank2CountMap.values().stream().mapToInt(i->i).sum();
        for (Map.Entry<Integer, Integer> e : sorted.entrySet()) {
            System.out.println(String.format("%s: %d (%.1f%%)  [by gene]", e.getKey(), e.getValue(),(100.0*e.getValue()/total)));
        }


        // output two files.
        // 1. rank2count.txt

        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        String rank2countName = String.format("rank2count-%s.txt",timeStamp);
        if (this.simulationOutFile==null) {
            simulationOutFile = String.format("ranked_simulation_results-%s.tsv",timeStamp);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rank2countName))){
            for (Map.Entry<Integer, Integer> e : sorted.entrySet()) {
                writer.write(e.getKey() + ": " + e.getValue() +"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 2. simulation-results.txt (show one line with the rank of each simulated disease).
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.simulationOutFile));
            if (phenotypeOnly) {
                bw.write(PhenoOnlyCaseSimulator.getHeader() + "\n");
            } else {
                bw.write(PhenoGenoCaseSimulator.getHeader() + "\n");
            }
            bw.write("#" + getSettingsString() + "\n");

            for (String diseaseResult : detailedResultLineList) {
                bw.write(diseaseResult + "\n");
            }
            bw.close();
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
       // disease2rankMap = new HashMap<>();
        detailedResultLineList = new ArrayList<>();
        rank2countMap=new HashMap<>();
        geneRank2CountMap = new HashMap<>();
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
        outputRankings();
    }

}
