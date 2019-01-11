package org.monarchinitiative.lr2pg;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.cmd.*;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 0.9.1 (2019-01-02)
 */


public class LR2PG  {
    private static final Logger logger = LogManager.getLogger();


    @Parameter(names = {"-h", "--help"}, help = true, description = "display this help message")
    private boolean usageHelpRequested;


    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();

        LR2PG lr2pg = new LR2PG();
        DownloadCommand download = new DownloadCommand();
        SimulatePhenotypesCommand simulate = new SimulatePhenotypesCommand();
        GridSearchCommand grid = new GridSearchCommand();
        Gt2GitCommand gt2git = new Gt2GitCommand();
        VcfCommand vcf = new VcfCommand();
        PhenopacketCommand phenopacket = new PhenopacketCommand();
        JCommander jc = JCommander.newBuilder()
                .addObject(lr2pg)
                .addCommand("download", download)
                .addCommand("simulate", simulate)
                .addCommand("grid", grid)
                .addCommand("gt2git",gt2git)
                .addCommand("vcf",vcf)
                .addCommand("phenopacket",phenopacket)
                .build();
        jc.setProgramName("java -jar Lr2pg.jar");
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println("[ERROR] "+e.getMessage());
            jc.usage();
            System.exit(1);
        }

        if (jc.getParsedCommand()==null ) {
            System.err.println("[ERROR] no command passed");
            jc.usage();
           System.exit(1);
       } else {
            System.out.println("Got parsed command = " + jc.getParsedCommand());
        }

        if ( lr2pg.usageHelpRequested) {
            jc.usage();
            System.exit(1);
        }

        String command = jc.getParsedCommand();
        Lr2PgCommand lr2pgcommand=null;
        switch (command) {
            case "download":
                lr2pgcommand= download;
                break;
            case "simulate":
                lr2pgcommand = simulate;
               break;
           case "grid":
               lr2pgcommand = grid;
               break;
           case "gt2git":
               lr2pgcommand = gt2git;
               break;
           case "vcf":
               lr2pgcommand =vcf;
               break;
           case "phenopacket":
                lr2pgcommand =phenopacket;
                break;
           default:
               System.err.println(String.format("[ERROR] command \"%s\" not recognized",command));
               jc.usage();
               System.exit(1);

        }
        try {
            lr2pgcommand.run();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }



}
