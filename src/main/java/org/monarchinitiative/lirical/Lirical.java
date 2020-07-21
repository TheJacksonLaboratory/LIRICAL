package org.monarchinitiative.lirical;



import org.monarchinitiative.lirical.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 0.9.1 (2019-01-02)
 */

@CommandLine.Command(name = "LIRICAL", mixinStandardHelpOptions = true, version = "1.3.0",
        description = "LIkelihood Ratio Interpretation of Clinical AbnormaLities")
public class Lirical implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(Lirical.class);


//    @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display this help message")
//    protected boolean usageHelpRequested;

//    private static final ImmutableSet<String> commandnames=ImmutableSet.of("download","yaml","phenopacket","simulate","grid","background","simulate-vcf","not");
//

    static public void main2(String [] args) {
        long startTime = System.currentTimeMillis();

        Lirical lirical = new Lirical();
        //LiricalCommand download = new DownloadCommand();
       // LiricalCommand simulate = new SimulatePhenotypeOnlyCommand();
      //  LiricalCommand grid = new GridSearchCommand();
       // LiricalCommand background = new BackgroundFrequencyCommand();
        //LiricalCommand yaml = new YamlCommand();
       // LiricalCommand phenopacket = new PhenopacketCommand();
       // LiricalCommand simvcf = new SimulatePhenopacketCommand();
//        JCommander jc = JCommander.newBuilder()
//                .addObject(lirical)
//              //  .addCommand("download", download)

             //   .addCommand("yaml",yaml)
                //.addCommand("background",background)
              //  .addCommand("simulate", simulate)
                //.addCommand("grid", grid)
                //.addCommand("simulate-vcf",simvcf)
                //.build();
//        jc.setProgramName("java -jar LIRICAL.jar");
//        try {
//            jc.parse(args);
//        } catch (ParameterException e) {
//            String commandstring = String.join(" ",args);
//
//            for (String a:args) {
//                if (a.equals("h") || a.equals("-h") || a.equals("--h")) {
//                    jc.usage();
//                    System.exit(1);
//                }
//            }
//            System.err.println("[ERROR] "+e.getMessage());
//            System.err.println("[ERROR] your command: "+commandstring);
//            System.err.println("[ERROR] enter java -jar LIRICAL.jar -h for more information.");
//            System.exit(1);
//        }
//        String parsedCommand = jc.getParsedCommand();
//        if (parsedCommand==null || parsedCommand.isEmpty()) {
//            jc.usage(); // user ran program with no arguments, probably help is want is wanted.
//            System.exit(0);
//        }
//        if (! commandnames.contains(parsedCommand)) {
//            System.err.println("[ERROR] did not recognize command \"" + parsedCommand +"\"");
//            System.err.println("[ERROR] available commands are " + String.join(", ",commandnames));
//            System.err.println("[ERROR] enter java -jar LIRICAL.jar -h for more information.");
//            System.exit(1);
//        }
//
//        if ( lirical.usageHelpRequested) {
//            jc.usage();
//            System.exit(1);
//        }
//
//        if (jc.getParsedCommand()==null ) {
//            System.err.println("[ERROR] no command passed");
//            jc.usage();
//           System.exit(1);
//       }
//
//        if ( lirical.usageHelpRequested) {
//
//            jc.usage();
//            System.exit(1);
//        }
//
//        String command = jc.getParsedCommand();
//      //  LiricalCommand liricalCommand=null;
//        switch (command) {
////
//
////
//
////           case "simulate-vcf":
////               liricalCommand=simvcf;
////               break;
////           case "phenopacket":
////                liricalCommand =phenopacket;
////                break;
//           default:
//               System.err.println(String.format("[ERROR] command \"%s\" not recognized",command));
//               jc.usage();
//               System.exit(1);
//        }
//        try {
//          //  liricalCommand.run();
//        } catch (LiricalException e) {
//            e.printStackTrace();
//        }
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


    public static void main(String[] args) {
        CommandLine cline = new CommandLine(new Lirical())
                .addSubcommand("background", new BackgroundFrequencyCommand())
               .addSubcommand("download", new DownloadCommand())
                .addSubcommand("grid", new GridSearchCommand())
                .addSubcommand("phenopacket", new PhenopacketCommand())
                .addSubcommand("simulate", new SimulatePhenotypeOnlyCommand())
                .addSubcommand("simulate-vcf", new SimulatePhenopacketWithVcfCommand())
                .addSubcommand("yaml", new YamlCommand());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }



}
