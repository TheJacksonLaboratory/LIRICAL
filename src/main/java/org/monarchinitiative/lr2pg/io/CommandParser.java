package org.monarchinitiative.lr2pg.io;

import java.io.PrintWriter;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.Command;
import org.monarchinitiative.lr2pg.command.DownloadCommand;
import org.monarchinitiative.lr2pg.command.SimulateCasesCommand;
/**
 * Command line parser designed to generate and initialize {@link Command} objects.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CommandParser {
    private static final Logger logger = LogManager.getLogger();
    private String hpoPath=null;
    private String annotationPath=null;
    /** Path to a file with a list of HPO terms that a "patient" has. */
    private String patientAnnotations=null;
    /** Path to directory where we will download the needed files. */
    private String dataDownloadDirectory=null;
    /** This is where we download the files to by default (otherwise, specify {@code -f <arg>}). */
    private static final String DEFAULT_DATA_DOWNLOAD_DIRECTORY="data";
    /** The default number of "random" HPO cases to simulate. */
    private static final int DEFAULT_N_CASES_TO_SIMULATE=1000;
    /** The default number of terms to simulate per case. */
    private static final int DEFAULT_N_TERMS_PER_CASE=5;
    /** The default number of ranomd (noise) terms to add per simulated case */
    private static final int DEFAULT_N_NOISE_TERMS_PER_CASE=1;
    /** The number of HPO Cases to simulate. */
    private int n_cases_to_simulate;
    /** The number of random HPO terms to simulate in each simulated case. */
    private int n_terms_per_case;
    /** The number of random noise terms to add to each simulated HPO case. */
    private int n_noise_terms;
    /** The type of analysis to run. */
    private String mycommand=null;
    /** The command object. */
    private Command command=null;



    public CommandParser(String args[]) {
        final CommandLineParser cmdLineGnuParser = new DefaultParser();

        final Options gnuOptions = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;

        try
        {
            commandLine = cmdLineGnuParser.parse(gnuOptions, args);
            String category[] = commandLine.getArgs();
            if (category.length != 1) {
                printUsage("command missing");
            } else {
                mycommand = category[0];
            }
            if (commandLine.getArgs().length < 1) {
                printUsage("no arguments passed");
                return;
            }


            if (commandLine.hasOption("o")) {
                hpoPath=commandLine.getOptionValue("o");
            }
            if (commandLine.hasOption("a")) {
                annotationPath=commandLine.getOptionValue("a");
            }
            if (commandLine.hasOption("i")) {
                patientAnnotations=commandLine.getOptionValue("i");
            }
            if (commandLine.hasOption("d")) {
                this.dataDownloadDirectory=commandLine.getOptionValue("d");
            }
            if (commandLine.hasOption("t")) {
                String term=commandLine.getOptionValue("t");
                try {
                    n_terms_per_case=Integer.parseInt(term);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -t option (must be integer)");
                }
            } else {
                n_terms_per_case=DEFAULT_N_TERMS_PER_CASE;
            }
            if (commandLine.hasOption("n")) {
                String noise = commandLine.getOptionValue("n");
                try {
                    n_noise_terms=Integer.parseInt(noise);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -n option (must be integer)");
                }
            } else {
                n_noise_terms=DEFAULT_N_NOISE_TERMS_PER_CASE;
            }
            if (commandLine.hasOption("s")) {
                String simul=commandLine.getOptionValue("s");
                try{
                    n_cases_to_simulate=Integer.parseInt(simul);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -s option (must be integer)");
                }
            } else {
                n_cases_to_simulate=DEFAULT_N_CASES_TO_SIMULATE;
            }
            // Commands
            if (mycommand.equals("download")) {
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory=DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                logger.warn(String.format("Download command to %s",dataDownloadDirectory));
                this.command=new DownloadCommand(dataDownloadDirectory);
            } else if (mycommand.equals("simulate")) {
                if (this.dataDownloadDirectory == null) {
                    this.dataDownloadDirectory=DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                }
                this.command=new SimulateCasesCommand(this.dataDownloadDirectory,n_cases_to_simulate,n_terms_per_case,n_noise_terms);
            } else {
                printUsage(String.format("Did not recognize command: %s", mycommand));
            }
        }
        catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage() );
        }

    }


    public Command getCommand() {
        return command;
    }

    /**
     * Construct and provide GNU-compatible Options.
     *
     * @return Options expected from command-line of GNU form.
     */
    private static Options constructOptions()
    {
        final Options gnuOptions = new Options();
        gnuOptions.
                addOption("a", "annotations", true, "Annotation file path")
                .addOption("d", "download", true, "path of directory to download files")
                .addOption("i","patient-hpo-terms",true, "list of HPO terms for the patient")
                .addOption("n","noise",true,"number of noise terms per simulate case (default: 1")
                .addOption("o", "hpo", true, "HPO OBO file path")
                .addOption("s","simulated_cases",true,"number of cases to simulate per run")
                .addOption("t","terms",true,"number of HPO terms per simulated case (default: 5)");
        return gnuOptions;
    }



    /**
     * Print usage information to provided OutputStream.
     */
    private static void printUsage(String message) {
        final PrintWriter writer = new PrintWriter(System.out);
        final HelpFormatter usageFormatter = new HelpFormatter();
        final String applicationName = "java -jar dimorph.jar command";
        final Options options = constructOptions();
        usageFormatter.printUsage(writer, 120, applicationName, options);
        writer.println("\twhere command is one of download,....");
        writer.println("\t- download [-d directory]: Download needed files to directory at (-d).");
        writer.close();
        System.exit(0);
    }


}

