package org.monarchinitiative.lr2pg.io;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.*;

import java.io.File;

/**
 * Command line parser designed to generate and initialize {@link Command} objects.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CommandParser {
    private static final Logger logger = LogManager.getLogger();
    /**
     * Path to directory where we will download the needed files.
     */
    private String dataDownloadDirectory = null;
    /** This is where we download the files to by default (otherwise, specify {@code -f <arg>}).*/
    private static final String DEFAULT_DATA_DOWNLOAD_DIRECTORY = "data";
    /** The default number of "random" HPO cases to simulate.*/
    private static final int DEFAULT_N_CASES_TO_SIMULATE = 1000;
    /** The default number of terms to simulate per case.*/
    private static final int DEFAULT_N_TERMS_PER_CASE = 5;
    /** The default number of ranomd (noise) terms to add per simulated case*/
    private static final int DEFAULT_N_NOISE_TERMS_PER_CASE = 1;
    /** The number of HPO Cases to simulate.*/
    private int n_cases_to_simulate;
    /** The number of random HPO terms to simulate in each simulated case.*/
    private int n_terms_per_case;
    /** The number of random noise terms to add to each simulated HPO case.*/
    private int n_noise_terms;
    /** CURIE of disease (e.g., OMIM:600100) for the analysis. */
    private String diseaseId =null;
    /** If true, we do a grid search over the parameters for LR2PG clinical. */
    private boolean gridSearch=false;
    /** Default name of the SVG file with the results of analysis. */
    private static final String DEFAULT_SVG_OUTFILE_NAME="test.svg";
    /** Name of the SVG file with the results of analysis. */
    private String svgOutFileName=null;
    /** If true, overwrite previously downloaded files. */
    private boolean overwrite=false;
    /** Gene id (e.g., 2200 for FBN1) for disease to be simulated. */
    private String entrezGeneId =null;
    /** Mean pathogenicity of variants in pathogenic bin. */
    private double varpath=1.0;
    /** Count of variants in the pathogenic bin */
    private int varcount=1;
    /** Comma separated list of HPO ids */
    private String termList=null;
    /** Path to the file produced by G2GIT - with frequencies for background pathogenic mutations per gene */
    private String backgroundFreq=null;

    private static final String DEFAULT_BACKGROUND_FREQ=String.format("%s%s%s",
            DEFAULT_DATA_DOWNLOAD_DIRECTORY, File.separator,"background-freq.txt");

    /**The command object.*/
    private Command command = null;



    public CommandParser(String args[]) {
        final CommandLineParser cmdLineGnuParser = new DefaultParser();
        String mycommand="";
        final Options gnuOptions = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;



    }


    public Command getCommand() {
        return command;
    }

    /**
     * Construct and provide GNU-compatible Options.
     *
     * @return Options expected from command-line of GNU form.
     */
    private static Options constructOptions() {
        final Options gnuOptions = new Options();
        gnuOptions.
                addOption("a", "annotations", true, "Annotation file path")
                .addOption("b", "background", true, "path to background-freq.txt file")
                .addOption("d", "download", true, "path of directory to download files")
                .addOption("n", "noise", true, "number of noise terms per simulate case (default: 1")
                .addOption(null,"geneid", true, "EntrezGene id of affected gene")
                .addOption("o", "hpo", true, "HPO OBO file path")
                .addOption(null,"disease", true, "disease to simulate and create SVG for (e.g., OMIM:600100)")
                .addOption(null,"grid", false, "perform a grid search over parameters")
                .addOption(null,"overwrite", false, "if true, overwrite previously downloaded files")
                .addOption(null,"svg", true, "name of output SVG file")
                .addOption("s", "simulated_cases", true, "number of cases to simulate per run")
                .addOption("t", "terms", true, "number of HPO terms per simulated case (default: 5)")
                .addOption(null, "term-list", true, "comma-separate list of HPO ids")
                .addOption(null,"varcount", true, "number of variants in pathogenic bin")
                .addOption(null,"varpath", true, "mean pathogenicity of variants in pathogenic bin");
        return gnuOptions;
    }




}

