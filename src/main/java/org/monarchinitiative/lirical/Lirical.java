package org.monarchinitiative.lirical;



import org.monarchinitiative.lirical.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 1.3.1 (2020-08-13)
 */

@CommandLine.Command(name = "java -jar LIRICAL", mixinStandardHelpOptions = true,
        version = "1.3.4",
        description = "LIkelihood Ratio Interpretation of Clinical AbnormaLities")
public class Lirical implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(Lirical.class);
    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }
        long startTime = System.currentTimeMillis();
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
        long stopTime = System.currentTimeMillis();
        int elapsedTime = (int)((stopTime - startTime)*(1.0)/1000);
        if (elapsedTime > 3599) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            int elapsedHours = elapsedTime/3600;
            logger.info(String.format("Elapsed time %d:%2d%2d",elapsedHours,elapsedMinutes,elapsedSeconds));
        }
        else if (elapsedTime>59) {
            int elapsedSeconds = elapsedTime % 60;
            int elapsedMinutes = (elapsedTime/60) % 60;
            logger.info(String.format("Elapsed time %d min, %d sec",elapsedMinutes,elapsedSeconds));
        } else {
            logger.info("Elapsed time " + (stopTime - startTime) * (1.0) / 1000 + " seconds.");
        }
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }



}
