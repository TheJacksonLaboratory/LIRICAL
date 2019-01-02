package org.monarchinitiative.lr2pg.io;

import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.cmd.*;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for working with command-line arguments are starting one of the commands of LR2PG
 * Note -- replacing CLI with picocli. This class will be deleted after the dust has settled.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Deprecated
public class Lr2pgCommandLine {
    private static final Logger logger = LogManager.getLogger();
    /** Command object that will run the analysis. */
    private Lr2PgCommand command = null;
    /** String coding the command to be used. */
    private String mycommand=null;
    /** Records the original command--can be useful for error messages.*/
    private String clstring;
    /** Path to YAML configuration file*/
    private String yamlPath = null;

    private boolean doClinvar=false;
    /**
     * Default path to downloaded data
     */
    private final String DEFAULT_DATA_DIRECGTORY = "data";

    private String dataPath = null;

    private boolean overwriteDownload = false;

    private String disease = null;
    /**
     * Path to the Exomiser MVStore data file (e.g., 1802_hg19/1802_hg19_variants.mv.db)
     */
    private String mvStorePath = null;
    /**
     * Path to the Jannovar transcript file (e.g., 1802_hg19_transcripts_refseq.ser).
     */
    private String jannovarTranscriptFile = null;
    /** String representing the genome build.*/
    private String genomeAssembly=null;

    private final String DEFAULT_DISEASE = "OMIM:617132";
    /** For running in simulate mode, the number of cases to be simulated (phenotype only). */
    private Integer cases_to_simulate=null;
    /** For running in simulate mode, the number of HPO terms per case. */
    private Integer terms_per_case=null;
    /** For running in simulate mode, the number of "noise" HPO terms per case. */
    private Integer noise_terms=null;
    /** For running in simulate mode, if true, use imprecision (move terms up the inheritance tree) */
    private boolean imprecise;

    private boolean outputTSV=false;

    private String varcount;

    private String varpath;

    private String entrezgeneid;

    private String diseaseId;
    /** Comma-separated list of HPO terms, used for one case. */
    private String termlist;

    private final Options options;


    public Lr2pgCommandLine(String args[]) {
        final CommandLineParser commandLineParser = new DefaultParser();
        this.options = constructOptions();
        org.apache.commons.cli.CommandLine commandLine;

        this.clstring = "";
        if (args != null && args.length > 0) {
            clstring = Arrays.stream(args).collect(Collectors.joining(" "));
        }
        try {
            commandLine = commandLineParser.parse(options, args);
            String category[] = commandLine.getArgs();
            if (commandLine.hasOption("h")) {
                printUsageIntro();
            }
            if (category.length < 1) {
                printUsage("command missing");
            } else {
                mycommand = category[0];
            }
            if (commandLine.getArgs().length < 1) {
                printUsageIntro();
                return;
            }

            if (commandLine.hasOption("clinvar")) {
                doClinvar = true;
            }
            if (commandLine.hasOption("d")) {
                dataPath = commandLine.getOptionValue("d");
            } else {
                dataPath = DEFAULT_DATA_DIRECGTORY;
            }

            if (commandLine.hasOption("g")) {
                genomeAssembly = commandLine.getOptionValue("g");
            }
            if (commandLine.hasOption("i")) {
                disease = commandLine.getOptionValue("i");
            } else {
                disease = DEFAULT_DISEASE;
            }
            if (commandLine.hasOption("j")) {
                jannovarTranscriptFile = commandLine.getOptionValue("j");
            }
            if (commandLine.hasOption("m")) {
                this.mvStorePath = commandLine.getOptionValue("m");
            }
            if (commandLine.hasOption("tsv")) {
                this.outputTSV=true;
            }
            overwriteDownload = commandLine.hasOption("o");
            if (commandLine.hasOption("y")) {
                yamlPath = commandLine.getOptionValue("y");
            }


            switch (mycommand) {
                case "vcf":
                case "VCF":
                    if (this.yamlPath == null) {
                        printUsage("YAML file not found but required for VCF command");
                        return;
                    }
                    Lr2PgFactory factory = deYamylate(this.yamlPath);
                    this.command = new VcfCommand(factory, dataPath,outputTSV);
                    break;

               case "download":
                    //this.command = new DownloadCommand(dataPath, overwriteDownload);
                    break;
                case "simulate":
                    this.command = new SimulatePhenotypesCommand(dataPath);
                    break;
                case "grid":
                    this.command = new GridSearchCommand(dataPath);
                    break;
                case "gt2git":
                    if (mvStorePath==null) {
                        printUsage("Need to specify the MVStore file: -m <mvstore> to run gt2git command!");
                    }
                    if (jannovarTranscriptFile==null) {
                        printUsage("Need to specify the Jannovar transcript file: -j <jannovar> to run gt2git command!");
                    }
                    if (genomeAssembly==null) {
                        printUsage("Need to specify the genome build: -g <genome> to run gt2git command!");
                    }
                    this.command = new Gt2GitCommand(dataPath, mvStorePath,jannovarTranscriptFile,genomeAssembly,doClinvar);
                    break;
                default:
                    printUsage("Could not find command option");


            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }


    /**
     * Parse the YAML file and put the results into an {@link Lr2PgFactory} object.
     *
     * @param yamlPath Path to the YAML file for the VCF analysis
     * @return An {@link Lr2PgFactory} object with various settings.
     */
    private Lr2PgFactory deYamylate(String yamlPath) {

        Lr2PgFactory factory = null;
        try {
            YamlParser yparser = new YamlParser(yamlPath);
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
    private Options constructOptions() {
        final Options options = new Options();
        options.addOption("d", "data", true, "directory to download data (default \"data\")")
                .addOption(null, "clinvar", false, "determine distribution of ClinVar pathogenicity scores")
                .addOption("g", "genome", true, "string representing the genome assembly (hg19,hg38)")
                .addOption("h", "help", false, "show help")
                .addOption("j", "jannovar", true, "path to Jannovar transcript file")
                .addOption("m", "mvstore", true, "path to Exomiser MVStore file")
                .addOption("o", "overwrite", false, "overwrite downloaded files")
                .addOption(null, "tsv", false, "output TSV")
                .addOption("y", "yaml", true, "path to yaml file");
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
            TermId tid = TermId.of(id);
            builder.add(tid);
        }
        return builder.build();
    }

    private String getVersion() {
        String version = "0.0.0";// default, should be overwritten by the following.
        try {
            Package p = Lr2pgCommandLine.class.getPackage();
            version = p.getImplementationVersion();
        } catch (Exception e) {
            // do nothing
        }
        return version;
    }


    private void printUsageIntro() {
        String version = getVersion();
        System.out.println();
        System.out.println("Program: LR2PG (v. " + version + ")");
        System.out.println();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar Lr2pg.jar command [options]", options);
        System.out.println();
        System.out.println("command is one of download, gt2git, vcf, simulate");
        System.exit(0);
    }

    private void phenoGenoUsage() {
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

    private void simulateUsage() {
        System.out.println("simulate:");
        System.out.println("\tjava -jar Lr2pg.jar simulate [-d <directory>] [-s <int>] [-t <int>] [-n <int>] [--grid]");
        System.out.println("\t-d <directory>: name of directory with HPO data (default:\"data\")");
        System.out.println();
    }

    private void svgUsage() {
        System.out.println("svg:");
        System.out.println("\tjava -jar Lr2pg.jar svg --disease <name> [-- svg <file>] [-d <directory>] [-t <int>] [-n <int>]");
        System.out.println("\t--disease <string>: name of disease to simulate (e.g., OMIM:600321)");
    }

    private void downloadUsage() {
        System.out.println("download:");
        System.out.println("\tjava -jar Lr2pg.jar download [-d <directory>] [--overwrite]");
        System.out.println("\t-d <directory>: name of directory to which HPO data will be downloaded (default:\"data\")");
        System.out.println("\t--overwrite: do not skip even if file already downloaded");
        System.out.println();
    }

    private void vcfUsage() {
        System.out.println("vcf:");
        System.out.println("\tjava -jar Lr2pg.jar vcf -y <yaml> [--tsv]");
        System.out.println("\t-y <yaml>: path to YAML configuration file (required)");
        System.out.println("\t--tsv: output TSV instead of default HTML file");
        System.out.println();
    }

    private void gt2gitUsage() {
        System.out.println("gt2git:");
        System.out.println("\tjava -jar Lr2pg.jar gt2git -m <mvstore> -j <jannovar> -d <data> -g <genome>");
        System.out.println("\t-d <data>: path to LR2PG data directory");
        System.out.println("\t-g <genome>: genome build (hg19 or hg38)");
        System.out.println("\t-h: show this help");
        System.out.println("\t-j <jannovar>: path to Jannovar transcript file");
        System.out.println("\t-m <mvstore>: path to Exomiser MVStore data file");
        System.out.println();
    }


    /**
     * Print usage information
     */
    private void printUsage(String message) {
        System.out.println();

        System.out.println(message);
        if (mycommand==null) {
            printUsageIntro();
        }
        System.out.println("arguments: " +clstring);
        switch (mycommand) {
            case "download":
                downloadUsage();
                break;
            case "gt2git":
                gt2gitUsage();
                break;
            case "vcf":
                vcfUsage();
                break;
            case "simulate":
                simulateUsage();
                break;
            default:
                phenoGenoUsage();
                svgUsage();
        }

        System.exit(0);
    }
}
