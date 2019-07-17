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

    private static final ImmutableSet<String> commandnames=ImmutableSet.of("download","simulate","grid","gt2git","yaml","simulate-vcf","phenopacket");


    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();

        Lirical lirical = new Lirical();
        DownloadCommand download = new DownloadCommand();
        SimulatePhenotypeOnlyCommand simulate = new SimulatePhenotypeOnlyCommand();
        GridSearchCommand grid = new GridSearchCommand();
        Gt2GitCommand gt2git = new Gt2GitCommand();
        YamlCommand yaml = new YamlCommand();
        PhenopacketCommand phenopacket = new PhenopacketCommand();
        SimulatePhenopacketCommand simvcf = new SimulatePhenopacketCommand();
        JCommander jc = JCommander.newBuilder()
                .addObject(lirical)
                .addCommand("download", download)
                .addCommand("simulate", simulate)
                .addCommand("grid", grid)
                .addCommand("gt2git",gt2git)
                .addCommand("yaml",yaml)
                .addCommand("simulate-vcf",simvcf)
                .addCommand("phenopacket",phenopacket)
                .build();
        jc.setProgramName("java -jar LIRICAL.jar");
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
            System.err.println("[ERROR] "+e.getMessage());
            System.err.println("[ERROR] your command: "+commandstring);
            System.err.println("[ERROR] enter java -jar LIRICAL.jar -h for more information.");
            System.exit(1);
        }
        String parsedCommand = jc.getParsedCommand();
        if (parsedCommand==null || parsedCommand.isEmpty()) {
            jc.usage(); // user ran program with no arguments, probably help is want is wanted.
            System.exit(0);
        }
        if (! commandnames.contains(parsedCommand)) {
            System.err.println("[ERROR] did not recognize command \"" + parsedCommand +"\"");
            System.err.println("[ERROR] available commands are " + String.join(", ",commandnames));
            System.err.println("[ERROR] enter java -jar LIRICAL.jar -h for more information.");
            System.exit(1);
        }

        if ( lirical.usageHelpRequested) {
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

        if ( lirical.usageHelpRequested) {

            jc.usage();
            System.exit(1);
        }

        String command = jc.getParsedCommand();
        LiricalCommand liricalCommand=null;
        switch (command) {
            case "download":
                liricalCommand= download;
                break;
            case "simulate":
                liricalCommand = simulate;
               break;
           case "grid":
               liricalCommand = grid;
               break;
           case "gt2git":
               liricalCommand = gt2git;
               break;
            case "yaml":
                liricalCommand = yaml;
                break;
           case "simulate-vcf":
               liricalCommand=simvcf;
               break;
           case "phenopacket":
                liricalCommand =phenopacket;
                break;
           default:
               System.err.println(String.format("[ERROR] command \"%s\" not recognized",command));
               jc.usage();
               System.exit(1);

        }
        try {
            liricalCommand.run();
        } catch (LiricalException e) {
            e.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        int elapsedTime = (int)((stopTime - startTime)*(1.0)/1000);
        if (elapsedTime > 3599) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            int elapsedHours = elapsedTime/3600;
            System.out.println(String.format("LIRICAL: Elapsed time was %d:%2d%2d",elapsedHours,elapsedMinutes,elapsedSeconds));
        }
        else if (elapsedTime>59) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            System.out.println(String.format("LIRICAL: Elapsed time was %d min, %d sec",elapsedMinutes,elapsedSeconds));
        } else {
            System.out.println("LIRICAL: Elapsed time was " + (stopTime - startTime) * (1.0) / 1000 + " seconds.");
        }
    }



}
