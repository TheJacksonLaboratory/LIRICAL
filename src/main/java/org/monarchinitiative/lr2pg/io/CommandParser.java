package org.monarchinitiative.lr2pg.io;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.*;

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

    /**The command object.*/
    private Command command = null;



    public CommandParser(String args[]) {
        final CommandLineParser cmdLineGnuParser = new DefaultParser();
        String mycommand="";
        final Options gnuOptions = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;

        try {
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

            if (commandLine.hasOption("d")) {
                this.dataDownloadDirectory = commandLine.getOptionValue("d");
            }

            if (commandLine.hasOption("grid")) {
                this.gridSearch = true;
            }
            if (commandLine.hasOption("term-list")) {
                this.termList = commandLine.getOptionValue("term-list");
            }
            if (commandLine.hasOption("varcount")) {
                try {
                    varcount=Integer.parseInt(commandLine.getOptionValue("varcount"));
                } catch (NumberFormatException e) {
                    System.err.println("Count not parse varcount");
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            if (commandLine.hasOption("varpath")) {
                try {
                    varpath=Double.parseDouble(commandLine.getOptionValue("varpath"));
                } catch (NumberFormatException e) {
                    System.err.println("Count not parse variant pathogenicity");
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            if (commandLine.hasOption("geneid")) {
                this.entrezGeneId =commandLine.getOptionValue("geneid");
            }

            if (commandLine.hasOption("overwrite")) {
                this.overwrite=true;
            }

            if (commandLine.hasOption("disease")) {
                diseaseId =commandLine.getOptionValue("disease");
            }
            if (commandLine.hasOption("svg")) {
                svgOutFileName=commandLine.getOptionValue("svg");
            } else {
                svgOutFileName=DEFAULT_SVG_OUTFILE_NAME;
            }
            if (commandLine.hasOption("t")) {
                String term = commandLine.getOptionValue("t");
                try {
                    n_terms_per_case = Integer.parseInt(term);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -t option (must be integer)");
                }
            } else {
                n_terms_per_case = DEFAULT_N_TERMS_PER_CASE;
            }
            if (commandLine.hasOption("n")) {
                String noise = commandLine.getOptionValue("n");
                try {
                    n_noise_terms = Integer.parseInt(noise);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -n option (must be integer)");
                }
            } else {
                n_noise_terms = DEFAULT_N_NOISE_TERMS_PER_CASE;
            }
            if (commandLine.hasOption("s")) {
                String simul = commandLine.getOptionValue("s");
                try {
                    n_cases_to_simulate = Integer.parseInt(simul);
                } catch (NumberFormatException nfe) {
                    printUsage("[ERROR] Malformed argument for -s option (must be integer)");
                }
            } else {
                n_cases_to_simulate = DEFAULT_N_CASES_TO_SIMULATE;
            }
            // Commands
            switch (mycommand) {
                case "download":
                    if (this.dataDownloadDirectory == null) {
                        this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                    }
                    logger.warn(String.format("Download command to %s", dataDownloadDirectory));
                    this.command = new DownloadCommand(dataDownloadDirectory,overwrite);
                    break;
                case "simulate":
                    if (this.dataDownloadDirectory == null) {
                        this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                    }
                    this.command = new SimulateCasesCommand(this.dataDownloadDirectory,
                            n_cases_to_simulate, n_terms_per_case, n_noise_terms, gridSearch);
                    break;
                case "svg":
                    if (this.dataDownloadDirectory == null) {
                        this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                    }
                    if (diseaseId ==null) {
                        printUsage("svg command requires --disease option");
                    }
                    //n_terms_per_case, n_noise_terms);
                    this.command = new HpoCase2SvgCommand(this.dataDownloadDirectory, diseaseId,svgOutFileName,n_terms_per_case,n_noise_terms);
                    break;
                case "phenogeno":
                    if (this.dataDownloadDirectory == null) {
                        this.dataDownloadDirectory = DEFAULT_DATA_DOWNLOAD_DIRECTORY;
                    }
                    if (termList==null) {
                        System.err.println("[ERROR] --term-list with list of HPO ids required");
                        phenoGenoUsage();
                        System.exit(1);
                    }
                    if (diseaseId==null){
                        System.err.println("[ERROR] --disease option (e.g., OMIM:600100) required");
                        phenoGenoUsage();
                        System.exit(1);
                    }
                    if (entrezGeneId==null){
                        System.err.println("[ERROR] --geneid option (e.g., 2200) required");
                        phenoGenoUsage();
                        System.exit(1);
                    }
                    this.command = new SimulatePhenoGeneCaseCommand(this.dataDownloadDirectory,
                            this.entrezGeneId,
                            this.varcount,
                            this.varpath,
                            this.diseaseId,
                            this.termList);
                    break;
                default:
                    printUsage(String.format("Did not recognize command: \"%s\"", mycommand));
            }
        } catch (ParseException parseException) {
            System.err.println(
                    "Encountered exception while parsing using GnuParser:\n"
                            + parseException.getMessage());
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

    private static String getVersion() {
        String DEFAULT="0.4.0";// default, should be overwritten by the following.
        String version=null;
        try {
            Package p = CommandParser.class.getPackage();
            version = p.getImplementationVersion();
        } catch (Exception e) {
            // do nothing
        }
        return version!=null?version:DEFAULT;
    }

    private static void printUsageIntro() {
        String version = getVersion();
        System.out.println();
        System.out.println("Program: LR2PG (v. "+version +")");
        System.out.println();
        System.out.println("Usage: java -jar Lr2pg.jar <command> [options]");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println();
    }

    private static void phenoGenoUsage() {
        System.out.println("phenogeno:");
        System.out.println("\tjava -jar Lr2pg.jar phenogeno --disease <id> --geneid <string> \\\n" +
                "\t\t--term-list <string> [-d <directory>] [--varcount <int>]\\\n" +
                "\t\t-b <file> [--varpath <double>]");
        System.out.println("\t--disease <id>: id of disease to simulate (e.g., OMIM:600321)");
        System.out.println("\t-d <directory>: name of directory with HPO data (default:\"data\")");
        System.out.println("\t-b <file>: path to background-freq.txt file");
        System.out.println("\t--geneid <string>: symbol of affected gene");
        System.out.println("\t--term-list <string>: comma-separated list of HPO ids");
        System.out.println("\t--varcount <int>: number of variants in pathogenic bin (default: 1)");
        System.out.println("\t--varpath <double>: mean pathogenicity score of variants in pathogenic bin (default: 1.0)");
    }

    private static void simulateUsage() {
        System.out.println("simulate:");
        System.out.println("\tjava -jar Lr2pg.jar simulate [-d <directory>] [-s <int>] [-t <int>] [-n <int>] [--grid]");
        System.out.println("\t-d <directory>: name of directory with HPO data (default:\"data\")");
        System.out.println(String.format("\t-s <int>: number of cases to simulate (default: %d)", DEFAULT_N_CASES_TO_SIMULATE));
        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
        System.out.println("\t--grid: Indicates a grid search over noise and imprecision should be performed");
        System.out.println();
    }

    private static void svgUsage() {
        System.out.println("svg:");
        System.out.println("\tjava -jar Lr2pg.jar svg --disease <name> [-- svg <file>] [-d <directory>] [-t <int>] [-n <int>]");
        System.out.println("\t--disease <string>: name of disease to simulate (e.g., OMIM:600321)");
        System.out.println(String.format("\t--svg <file>: name of output SVG file (default: %s)", DEFAULT_SVG_OUTFILE_NAME));
        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
    }

    private static void downloadUsage() {
        System.out.println("download:");
        System.out.println("\tjava -jar Lr2pg.jar download [-d <directory>]");
        System.out.println("\t-d <directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
        System.out.println();
    }



    /**
     * Print usage information
     */
    private static void printUsage(String message) {
        printUsageIntro();
        System.out.println();
        System.out.println(message);
        downloadUsage();
        simulateUsage();
        phenoGenoUsage();
        svgUsage();
        System.exit(0);
    }


}

