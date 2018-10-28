package org.monarchinitiative.lr2pg.io;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.monarchinitiative.lr2pg.cmd.*;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandLine {

    private Lr2PgCommand command=null;

    /** Path to YAML configuration file */
    private String yamlPath=null;
    /** Default path to downloaded data */
    private final String DEFAULT_DATA_DIRECGTORY="data";

    private String dataPath=null;

    private boolean overwriteDownload=false;



    public CommandLine(String args[]){
        final CommandLineParser cmdLineGnuParser = new DefaultParser();

        final Options gnuOptions = constructGnuOptions();
        org.apache.commons.cli.CommandLine commandLine;

        String mycommand = null;
        String clstring = "";
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
            if (commandLine.hasOption("o")) {
                overwriteDownload=true;
            } else {
                overwriteDownload=false;
            }
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
                    this.command =  new VcfCommand(factory);
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
        private static void printUsage(String message) {

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
}
