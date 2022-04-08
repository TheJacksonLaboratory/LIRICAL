package org.monarchinitiative.lirical.cli.cmd;


import org.monarchinitiative.lirical.bootstrap.LiricalFactory;
import org.monarchinitiative.lirical.core.output.LiricalRanking;
import org.monarchinitiative.lirical.cli.simulation.PhenoGenoCaseSimulator;
import org.monarchinitiative.lirical.cli.simulation.PhenoOnlyCaseSimulator;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Path;
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


@CommandLine.Command(name = "simulate",
        aliases = {"S"},
        mixinStandardHelpOptions = true,
        description = "Simulate analysis from phenopacket (with or without VCF)",
        hidden = true)
public class SimulatePhenopacketWithVcfCommand extends PhenopacketCommand {
    private static final Logger logger = LoggerFactory.getLogger(SimulatePhenopacketWithVcfCommand.class);
    @CommandLine.Option(names = {"-v", "--template-vcf"}, required = true, description = "path to template VCF file")
    private String templateVcfPath;
    @CommandLine.Option(names = {"--phenopacket-dir"}, description = "path to directory with multiple phenopackets")
    private String phenopacketDir;
    @CommandLine.Option(names = {"-outputfile"}, description = "name of the output file with simulation results")
    private String simulationOutFile = null;
    @CommandLine.Option(names = {"--phenotype-only"}, description = "run simulations with phenotypes only?")
    private boolean phenotypeOnly=false;
    @CommandLine.Option(names={"--output-vcf"}, description = "output a VCF file or files with results of the simulation")
    private boolean outputVCF = false;
    /** If true, the program will output an HTML file.*/
    @CommandLine.Option(names="--output-html", arity = "0..1", description = "Provide HTML output (default: ${DEFAULT-VALUE})")
    protected boolean outputHTML=true;
    /** If true, the program will output a Tab Separated Values file.*/
    @CommandLine.Option(names="--output-tsv", arity = "0..1", description = "output a TSV file or files with results of the simulation (default: ${DEFAULT-VALUE})")
    protected boolean outputTSV=false;
    @CommandLine.Option(names={"--random"},description = "randomize the HPO terms from the phenopacket")
    private boolean randomize = false;

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
    public SimulatePhenopacketWithVcfCommand() {
    }

    /**
     * This method coordinates the analysis for VCF files.
     * @param phenopacketPath File with the Phenopacket we are currently analyzing
     */
    private void runOneVcfAnalysis(Path phenopacketPath) {
        PhenoGenoCaseSimulator simulator = new PhenoGenoCaseSimulator(phenopacketPath, this.templateVcfPath, this.factory, this.randomize);
        simulator.run();
        int diseaseRank = simulator.getRank_of_disease();
        int geneRank    = simulator.getRank_of_gene();
        detailedResultLineList.add(simulator.getDetails());
        System.out.println(simulator.getDetails());
        rank2countMap.putIfAbsent(diseaseRank,0);
        rank2countMap.merge(diseaseRank,1, Integer::sum); // increment count
        geneRank2CountMap.putIfAbsent(geneRank,0);
        geneRank2CountMap.merge(geneRank,1, Integer::sum); // increment count
        if (outputTSV) {
//            simulator.outputTsv(output.outfilePrefix, factory.getLrThreshold(), factory.getMinDifferentials(), output.outdir);
        }
        if (outputHTML) {
//            simulator.outputHtml(output.outfilePrefix, factory.getLrThreshold(), factory.getMinDifferentials(), output.outdir);
        }
    }



    private void runOnePhenotypeOnlyAnalysis(Path phenopacketPath) {
        PhenoOnlyCaseSimulator simulator = new PhenoOnlyCaseSimulator(phenopacketPath,this.factory);
        simulator.run();
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
     * {@link #templateVcfPath}. The function {@link #runOneVcfAnalysis(Path)} will determine the
     * rank of the correct diagnosis as represented in the Phenopacket.
     * @param metadata
     */
    private void runWithVcf(Map<String, String> metadata) {
        this.factory = LiricalFactory.builder()
//                .datadir(this.datadir) // TODO - fix
//                .genomeAssembly(runConfiguration.genomeAssembly)
                .exomiser(this.dataSection.exomiserDataDirectory)
                .transcriptdatabase(runConfiguration.transcriptDb)
                .backgroundFrequency(this.dataSection.backgroundFrequencyFile)
                .global(runConfiguration.globalAnalysisMode)
                .lrThreshold(runConfiguration.lrThreshold)
                .minDiff(runConfiguration.minDifferentialsToShow)
                .build();
        // TODO - fix
//        factory.qcHumanPhenotypeOntologyFiles();
//        factory.qcExternalFilesInDataDir();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();


        if (phenopacketPath != null) {
            logger.info("Running single file Phenopacket/VCF simulation at {}", phenopacketPath);
            runOneVcfAnalysis(phenopacketPath);
        } else if (this.phenopacketDir != null) {
            outputTSV=true; // needed so that we can capture the results of the simulations across all cases
            logger.info("Running Phenopacket/VCF simulations at {}", phenopacketDir);
            final File folder = new File(phenopacketDir);
            if (! folder.isDirectory()) {
                throw new PhenolRuntimeException("Could not open Phenopackets directory at "+phenopacketDir);
            }
            int counter=0;
            File[] files = folder.listFiles();
            if (files == null) {
                throw new LiricalRuntimeException("Could not files in phenopackets directory");
            }
            for (File fileEntry : files) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".json")) {
                    logger.info("\tPhenopacket: \"{}\"", fileEntry.getAbsolutePath());
                    System.out.println(++counter + ") "+ fileEntry.getName());
                    runOneVcfAnalysis(fileEntry.toPath());
                }
            }
        } else {
            System.err.println("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
            throw new LiricalRuntimeException("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
        }
        metadata.put("hpoVersion", factory.getHpoVersion());
    }

    private void runPhenotypeOnly(Map<String, String> metadata) {
        this.factory = LiricalFactory.builder()
                // TODO - fix
//                .datadir(this.datadir)
                .minDiff(runConfiguration.minDifferentialsToShow)
                .lrThreshold(runConfiguration.lrThreshold)
                .global(runConfiguration.globalAnalysisMode)
                .build();
        // TODO - fix
//        factory.qcHumanPhenotypeOntologyFiles();
//        factory.qcExternalFilesInDataDir();


        if (phenopacketPath != null) {
            logger.info("Running single file Phenopacket/VCF simulation at {}", phenopacketPath);
            runOnePhenotypeOnlyAnalysis(phenopacketPath);
        } else if (this.phenopacketDir != null) {
            outputTSV=true; // needed so that we can capture the results of the simulations across all cases
            logger.info("Running Phenopacket/VCF simulations at {}", phenopacketDir);
            final File folder = new File(phenopacketDir);
            if (! folder.isDirectory()) {
                throw new PhenolRuntimeException("Could not open Phenopackets directory at "+phenopacketDir);
            }
            int counter=0;
            File[] files = folder.listFiles();
            if (files == null) {
                throw new PhenolRuntimeException("Could not find phenopacket files in " + phenopacketDir);
            }
            for (final File fileEntry : files) {
                if (fileEntry.isFile() && fileEntry.getAbsolutePath().endsWith(".json")) {
                    logger.info("\tPhenopacket: \"{}\"", fileEntry.getAbsolutePath());
                    System.out.println(++counter + ") "+ fileEntry.getName());
                    runOnePhenotypeOnlyAnalysis(fileEntry.toPath());
                }
               // if (counter>4)break;
            }
            System.out.println("[INFO] Processed " + counter + " phenopackets");
        } else {
            System.err.println("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
            throw new LiricalRuntimeException("[ERROR] Either the --phenopacket or the --phenopacket-dir option is required");
        }
    }



    private String getSettingsString() {
        List<String> settings= new ArrayList<>();
        settings.add("phenopacket-dir: " + phenopacketDir);
        settings.add("phenopackets: n=" + this.rankingsList.size());
        settings.add("keep: " + (runConfiguration.globalAnalysisMode ? "true" : "false"));
        settings.add("random: " + (randomize ?  "true" : "false"));
        settings.add("phenotypeOnly: "+ (phenotypeOnly? "true":"false"));
        settings.add("transcriptDb: " + runConfiguration.transcriptDb);

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
    public Integer call() {
        rankingsList = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();
       // disease2rankMap = new HashMap<>();
        detailedResultLineList = new ArrayList<>();
        rank2countMap=new HashMap<>();
        geneRank2CountMap = new HashMap<>();
        checkInput();
        if (phenotypeOnly) {
            runPhenotypeOnly(metadata);
        } else {
            runWithVcf(metadata);
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
        return 0;
    }

}
