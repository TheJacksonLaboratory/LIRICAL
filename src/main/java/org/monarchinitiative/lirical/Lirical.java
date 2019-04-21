package org.monarchinitiative.lirical;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.collect.ImmutableSet;
import org.monarchinitiative.lirical.cmd.*;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 0.9.1 (2019-01-02)
 */


public class Lirical {
    private static final Logger logger = LoggerFactory.getLogger(Lirical.class);


    @Parameter(names = {"-h", "--help"}, help = true, arity = 0,description = "display this help message")
    private boolean usageHelpRequested;

    private static final ImmutableSet<String> commandnames=ImmutableSet.of("download","simulate","grid","gt2git","vcf","simulate-vcf","phenopacket");


    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();

        Lirical lr2pg = new Lirical();
        DownloadCommand download = new DownloadCommand();
        SimulatePhenotypesCommand simulate = new SimulatePhenotypesCommand();
        GridSearchCommand grid = new GridSearchCommand();
        Gt2GitCommand gt2git = new Gt2GitCommand();
        VcfCommand vcf = new VcfCommand();
        PhenopacketCommand phenopacket = new PhenopacketCommand();
        SimulateVcfCommand simvcf = new SimulateVcfCommand();
        JCommander jc = JCommander.newBuilder()
                .addObject(lr2pg)
                .addCommand("download", download)
                .addCommand("simulate", simulate)
                .addCommand("grid", grid)
                .addCommand("gt2git",gt2git)
                .addCommand("vcf",vcf)
                .addCommand("simulate-vcf",simvcf)
                .addCommand("phenopacket",phenopacket)
                .build();
        jc.setProgramName("java -jar Lr2pg.jar");
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            // Note that by default, JCommand is OK with -h download but
            // not with download -h
            // The following hack makes things work with either option.
            String mycommand=null;
            String commandstring = String.join(" ",args);
            for (String a:args) {
                if (commandnames.contains(a)) {
                    mycommand=a;
                }
                if (a.equals("h")) {
                    if (mycommand!=null) {
                        jc.usage(mycommand);
                    } else {
                        jc.usage();
                    }
                    System.exit(1);
                }
            }
            if (commandstring==null) { // user ran without any command
                jc.usage();
                System.exit(0);
            }
            System.err.println("[ERROR] "+e.getMessage());
            System.err.println("[ERROR] your command: "+commandstring);
            System.err.println("[ERROR] enter java -jar Lr2pg -h for more information.");
            System.exit(1);
        }
        String parsedCommand = jc.getParsedCommand();
        if (parsedCommand==null) {
            jc.usage(); // user ran program with no arguments, probably help is want is wanted.
            System.exit(0);
        }
        if (! commandnames.contains(parsedCommand)) {
            System.err.println("[ERROR] did not recognize command \"" + parsedCommand +"\"");
            System.err.println("[ERROR] available commands are " + String.join(", ",commandnames));
            System.err.println("[ERROR] enter java -jar Lr2pg -h for more information.");
            System.exit(1);
        }

        if ( lr2pg.usageHelpRequested) {
            if (parsedCommand==null) {
                jc.usage();
            } else {
                jc.usage(parsedCommand);
            }
            System.exit(1);
        }

        if (jc.getParsedCommand()==null ) {
            System.err.println("[ERROR] no command passed");
            jc.usage();
           System.exit(1);
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
           case "simulate-vcf":
               lr2pgcommand=simvcf;
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
        } catch (LiricalException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }



}
