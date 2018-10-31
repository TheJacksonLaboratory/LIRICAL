package org.monarchinitiative.lr2pg.io;

import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.monarchinitiative.lr2pg.cmd.*;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for working with command-line arguments are starting one of the commands of LR2PG
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CommandLine {

    private Lr2PgCommand command=null;
    /** Records the original command--can be useful for error messages. */
    private String clstring;
    /** Path to YAML configuration file */
    private String yamlPath=null;
    /** Default path to downloaded data */
    private final String DEFAULT_DATA_DIRECGTORY="data";

    private String dataPath=null;

    private boolean overwriteDownload=false;

    private String disease=null;

    private final String DEFAULT_DISEASE="OMIM:617132";


    private String cases_to_simulate;

    private String terms_per_case;

    private String noise_terms;

    private String imprecise;

    private String varcount;

    private String varpath;

    private String entrezgeneid;

    private String diseaseId;

    private String termlist;




    public CommandLine(String args[]){
        final CommandLineParser cmdLineGnuParser = new DefaultParser();

        final Options gnuOptions = constructGnuOptions();
        org.apache.commons.cli.CommandLine commandLine;

        String mycommand = null;
        this.clstring = "";
        if (args != null && args.length > 0) {
            clstring = Arrays.stream(args).collect(Collectors.joining(" "));
        }
        try {
            commandLine = cmdLineGnuParser.parse(gnuOptions, args);
            String category[] = commandLine.getArgs();
            if (category.length < 1) {
                printUsage("command missing");
            } else {
                mycommand = category[0];

            }
            if (commandLine.getArgs().length < 1) {
                printUsage("no arguments passed");
                return;
            }


            if (commandLine.hasOption("d")) {
                dataPath=commandLine.getOptionValue("d");
            } else {
                dataPath=DEFAULT_DATA_DIRECGTORY;
            }
            if (commandLine.hasOption("i")) {
                disease=commandLine.getOptionValue("i");
            } else {
                disease=DEFAULT_DISEASE;
            }
            overwriteDownload = commandLine.hasOption("o");
            if (commandLine.hasOption("y")) {
                yamlPath = commandLine.getOptionValue("y");
            }


            switch (mycommand) {
                case "vcf":
                case "VCF":
                    if (this.yamlPath==null) {
                        printUsage("YAML file not found but required for VCF command");
                        return;
                    }
                    Lr2PgFactory factory = deYamylate(this.yamlPath);
                    this.command =  new VcfCommand(factory, dataPath);
                    break;

                case "download":
                    if (overwriteDownload) {
                        this.command=new DownloadCommand(dataPath,overwriteDownload);
                    } else {
                        this.command = new DownloadCommand(dataPath);
                    }
                    break;
                case "simulate":
                    this.command=new SimulatePhenotypesCommand(dataPath);
                    break;
                case "grid":
                    this.command=new GridSearchCommand(dataPath);
                    break;
                case "svg":
                    this.command=new SimulateSvgPhenoOnlyCommand(dataPath,disease);
                    break;
                case "gt2git":
                    this.command=new Gt2GitCommand(dataPath);
                    break;
                default:
                    printUsage("Could not find command option");


            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }





    private Lr2PgFactory deYamylate(String yamlPath) {
        YamlParser yparser = new YamlParser(yamlPath);
        Lr2PgFactory factory = null;
        try {
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



        public Lr2PgCommand getCommand() {
            return command;
        }

        /**
         * Construct and provide GNU-compatible Options.
         *
         * @return Options expected from command-line of GNU form.
         */
        private static Options constructGnuOptions() {
            final Options options = new Options();
            options.addOption("d", "data", true, "directory to download data (default \"data\")")
                    .addOption("o","overwrite",false,"overwrite downloaded files")
                    .addOption("y","yaml",true,"path to yaml file");
            return options;
        }

    /**
     * We expect to get an argument such as
     * --term.list=HP:0002751,HP:0001166,HP:0004933,HP:0001083,HP:0003179
     *
     * @return list of termid for this patient
     */
    List<TermId> termIdList() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (String id : this.termlist.split(",")) {
            TermId tid = TermId.constructWithPrefix(id);
            builder.add(tid);
        }
        return builder.build();
    }

        private static String getVersion() {
            String version = "0.0.0";// default, should be overwritten by the following.
            try {
                Package p = CommandLine.class.getPackage();
                version = p.getImplementationVersion();
            } catch (Exception e) {
                // do nothing
            }
            return version;
        }

        /**
         * Print usage information to provided OutputStream.
         */
        private static void printUsage2(String message) {

            String version = getVersion();
            final PrintWriter writer = new PrintWriter(System.out);
            // final HelpFormatter usageFormatter = new HelpFormatter();
            // final String applicationName="java -jar diachromatic.jar command";
            // final Options options=constructGnuOptions();
            writer.println(message);
            writer.println();
            //usageFormatter.printUsage(writer, 120, applicationName, options);
            writer.println("Program: LR2PG (Human Phenotype Ontology LR app)");
            writer.println("Version: " + version);
            writer.println();
            writer.println("Usage: java -jar Lr2pg.jar <command> [options]");
            writer.println();
            writer.println("Available commands:");
            writer.println();
            writer.println("download:");
            writer.println("\tjava -jar HPOWorkbench.jar download  [-d <directory>]");
            writer.println("\t<directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
            writer.println();
            writer.println("vcf:");
            writer.println("\tjava -jar Lr2Pg.jar vcf -y <config.yml>  \\");
            writer.println("\t<config.yml>: path to YAML configuration file (required)");
            writer.println("\t<pheno_annot.tab>: path to annotation file (default \"data/phenotype_annotation.tab\")");
            writer.println("\t<term>: HPO term id (e.g., HP:0000123)");
            writer.println();
            writer.close();
            System.exit(0);
        }

    private static void printUsageIntro() {
        String version = getVersion();
        System.out.println();
        System.out.println("Program: LR2PG (v. " + version + ")");
        System.out.println();
        System.out.println("Usage: java -jar Lr2pg.jar <analysis> [options]");
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
//        System.out.println(String.format("\t-s <int>: number of cases to simulate (default: %d)", DEFAULT_N_CASES_TO_SIMULATE));
//        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
//        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
//        System.out.println("\t--grid: Indicates a grid search over noise and imprecision should be performed");
        System.out.println();
    }

    private static void svgUsage() {
        System.out.println("svg:");
        System.out.println("\tjava -jar Lr2pg.jar svg --disease <name> [-- svg <file>] [-d <directory>] [-t <int>] [-n <int>]");
        System.out.println("\t--disease <string>: name of disease to simulate (e.g., OMIM:600321)");
//        System.out.println(String.format("\t--svg <file>: name of output SVG file (default: %s)", DEFAULT_SVG_OUTFILE_NAME));
//        System.out.println(String.format("\t-t <int>: number of HPO terms per case (default: %d)", DEFAULT_N_TERMS_PER_CASE));
//        System.out.println(String.format("\t-n <int>: number of noise terms per case (default: %d)", DEFAULT_N_NOISE_TERMS_PER_CASE));
    }

    private static void downloadUsage() {
        System.out.println("download:");
        System.out.println("\tjava -jar Lr2pg.jar download [-d <directory>] [--overwrite]");
        System.out.println("\t-d <directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
        System.out.println("\t--overwrite: do not skip even if file already downloaded");
        System.out.println();
    }

    private static void vcfUsage() {
        System.out.println("vcf:");
        System.out.println("\tjava -jar Lr2pg.jar vcf [--V <VCF FILE>] ");
        System.out.println("\t-d <directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
        System.out.println("\t--overwrite: do not skip even if file already downloaded");
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
        vcfUsage();
        System.exit(0);
    }
}
