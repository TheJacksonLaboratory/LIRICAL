package org.monarchinitiative.lr2pg.io;

import java.io.PrintWriter;
import org.apache.commons.cli.*;

public class CommandParser {

    private String hpoPath=null;
    private String annotationPath=null;

    public String getHpoPath() {
        return hpoPath;
    }

    public String getAnnotationPath() {
        return annotationPath;
    }

    public CommandParser(String args[]) {
        final CommandLineParser cmdLineGnuParser = new DefaultParser();

        final Options gnuOptions = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;

        try
        {
            commandLine = cmdLineGnuParser.parse(gnuOptions, args);
            if (commandLine.hasOption("o")) {
                hpoPath=commandLine.getOptionValue("o");
            } else {
                System.err.println("[ERROR] hp.obo file (-og) required.");
                printUsage();
                System.exit(1);
            }
            if (commandLine.hasOption("a")) {
                annotationPath=commandLine.getOptionValue("a");
            } else {
                System.err.println("[ERROR] phenotype_annotation.tab file (-h) required.");
                printUsage();
                System.exit(1);
            }
        }
        catch (ParseException parseException)  // checked exception
        {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage() );
        }

    }




    /**
     * Construct and provide GNU-compatible Options.
     *
     * @return Options expected from command-line of GNU form.
     */
    public static Options constructOptions()
    {
        final Options gnuOptions = new Options();
        gnuOptions.addOption("o", "hpo", true, "HPO OBO file path")
                .addOption("a", "annotations", true, "Annotation file path");
        return gnuOptions;
    }



    /**
     * Print usage information to provided OutputStream.
     */
    public static void printUsage()
    {
        final PrintWriter writer = new PrintWriter(System.out);
        final HelpFormatter usageFormatter = new HelpFormatter();
        final String applicationName="LR2PG";
        final Options options= constructOptions();
        usageFormatter.printUsage(writer, 80, applicationName, options);
        writer.print("\t TODO.\n");
        writer.close();
        System.exit(0);
    }

}

