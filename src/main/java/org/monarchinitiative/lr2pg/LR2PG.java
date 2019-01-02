package org.monarchinitiative.lr2pg;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.monarchinitiative.lr2pg.cmd.*;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.YamlParser;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 0.9.1 (2019-01-02)
 */

@org.apache.logging.log4j.core.tools.picocli.CommandLine.Command(name = "java -jar Lr2pg.jar",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage|@:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description|@:%n%n",
        parameterListHeading = "%n@|bold,underline Parameters|@:%n",
        optionListHeading = "%n@|bold,underline Options|@:%n",
        header = "likelihood-ratio analysis of phenotypes and genotypes.",
        description = "Phenotype-driven analysis of exomes/genomes.")
public class LR2PG implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    @org.apache.logging.log4j.core.tools.picocli.CommandLine.Parameters(index = "0", description = "LR2PG command to be exectuted.")
    private String command =null;

    /** Path to YAML configuration file*/
    @CommandLine.Option(names = {"-y","--yaml"}, description = "path to yaml configuration file")
    private String yamlPath = null;

    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    private String datadir="data";

    @CommandLine.Option(names={"-m","--mvstore"}, description = "path to Exomiser MVStore file")
    private String mvStorePath;

    @CommandLine.Option(names={"-j","--jannovar"}, description = "path to Jannovar transcript file")
    private String jannovarTranscriptFile;

    @CommandLine.Option(names={"g", "genome"}, description = "string representing the genome assembly (hg19,hg38)")
    private String genomeAssembly;

    @CommandLine.Option(names="clinvar", description = "determine distribution of ClinVar pathogenicity scores")
    private boolean doClinvar;

    @CommandLine.Option(names={"-o","--overwrite"}, description = "determine distribution of ClinVar pathogenicity scores")
    private boolean overwriteDownload;

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    private boolean usageHelpRequested;

    @CommandLine.Option(names= {"-t","--threshold"}, description = "threshold for showing diagnosis in HTML output")
    private double threshold=0.01;

    @CommandLine.Option(names="--tsv",description = "Use TSV instead of HTML output")
    private boolean useTsvOutput;

    /** Used to record the command line string used. */
    private static String clstring;

    static public void main(String [] args) {

        long startTime = System.currentTimeMillis();
        clstring = "";
        if (args != null && args.length > 0) {
            clstring = Arrays.stream(args).collect(Collectors.joining(" "));
        }
        new CommandLine(new LR2PG()).parseWithHandler(new CommandLine.RunLast(), System.err, args);





//        Lr2pgCommandLine clp = new Lr2pgCommandLine(args);
//        Lr2PgCommand command = clp.getCommand();
//        try {
//            command.run();
//        } catch (Lr2pgException e) {
//            e.printStackTrace();
//        }
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }


   /* @CommandLine.Command
    void commit(@CommandLine.Option(names = {"-d", "--data"}) String datadir,
                @CommandLine.Option(names = {"-o","--overwrite"}, paramLabel = "<commit>") boolean overwrite) {
        int x=2;
        System.out.println(x);
    } */


   @Override
    public void run() {
       if (usageHelpRequested) {
           CommandLine.usage(new LR2PG(), System.out);
           System.exit(0);
       }

       System.out.println("Running");
       System.out.println("datadir="+datadir);
       System.out.println("command="+command);
       Lr2PgCommand lr2pgcommand=null;
       switch (command) {
           case "vcf":
           case "VCF":
               if (this.yamlPath == null) {
                   printUsage("YAML file not found but required for VCF command");
                   return;
               }
               Lr2PgFactory factory = deYamylate(this.yamlPath);
               if (useTsvOutput) {
                   // output TSV Instead of HTML (threshold not needed for this)
                   lr2pgcommand = new VcfCommand(factory,datadir,useTsvOutput);
               } else {
                   // output HTML file at the indicated threshold to show differentials.
                   lr2pgcommand = new VcfCommand(factory, datadir, threshold);
               }
               break;
           case "download":
               lr2pgcommand= new DownloadCommand(datadir, overwriteDownload);
               break;
           case "simulate":
               lr2pgcommand = new SimulatePhenotypesCommand(datadir);
               break;
           case "grid":
               lr2pgcommand = new GridSearchCommand(datadir);
               break;
           case "gt2git":
               if (mvStorePath==null) {
                   printUsage("Need to specify the MVStore file: -m <mvstore> to run gt2git command!");
               }
               if (jannovarTranscriptFile==null) {
                   printUsage("Need to specify the Jannovar transcript file: -j <jannovar> to run gt2git command!");
               }
               if (genomeAssembly==null) {
                   printUsage("Need to specify the genome build: -g <genome> to run gt2git command!");
               }
               lr2pgcommand = new Gt2GitCommand(datadir, mvStorePath,jannovarTranscriptFile,genomeAssembly,doClinvar);
               break;
           default:
               printUsage("Could not find command option");


       }
       try {
           lr2pgcommand.run();
       } catch (Lr2pgException e) {
           e.printStackTrace();
       }

   }
    /**
     * Print usage information
     */
    private void printUsage(String message) {
        System.out.println();
        System.out.println("arguments: " + clstring);
        System.out.println(message);
        CommandLine.usage(new LR2PG(), System.out);
        System.exit(0);
    }

    /**
     * Parse the YAML file and put the results into an {@link Lr2PgFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link Lr2PgFactory} object with various settings.
     */
    private Lr2PgFactory deYamylate(String yamlPath) {

        Lr2PgFactory factory = null;
        try {
            YamlParser yparser = new YamlParser(yamlPath);
            Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder().
                    hp_obo(yparser.getHpOboPath()).
                    mvStore(yparser.getMvStorePath())
                    .mim2genemedgen(yparser.getMedgen())
                    .geneInfo(yparser.getGeneInfo())
                    .phenotypeAnnotation(yparser.phenotypeAnnotation())
                    .observedHpoTerms(yparser.getHpoTermList())
                    .vcf(yparser.vcfPath()).
                            jannovarFile(yparser.jannovarFile());
            factory = builder.build();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
        return factory;
    }

}
