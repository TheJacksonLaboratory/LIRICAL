package org.monarchinitiative.lr2pg;


import org.monarchinitiative.lr2pg.command.*;
import org.monarchinitiative.lr2pg.configuration.Lr2pgConfiguration;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.io.CommandParser;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class Lr2pgApplicationRunner implements ApplicationRunner  {
    public static final Logger logger = LoggerFactory.getLogger(Lr2pgApplicationRunner.class);

    @Autowired
    private HpoOntology ontology;

    @Autowired
    private Map<TermId,HpoDisease> diseaseMap;



    private String dataDownloadDirectory;



    /**The command object.*/
    private Command command = null;

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));


        logger.info("NonOptionArgs: {}", args.getNonOptionArgs());
        logger.info("OptionNames: {}", args.getOptionNames());
        for (String name : args.getOptionNames()){
            logger.info("arg-" + name + "=" + args.getOptionValues(name));
        }

        if (args.containsOption("help") || args.containsOption("h")) {
            printUsage("See the README file for usage!");
        }

        List<String> nonoptionargs = args.getNonOptionArgs();
        if (nonoptionargs.size() != 1) {
            printUsage("[ERROR] No program command given");
        }
        // if we get here, we have one command
        String mycommand = nonoptionargs.get(0);

        // collect the options

        switch (mycommand) {
            case "download":
                boolean overwrite=false;
                logger.warn(String.format("Download command to %s", dataDownloadDirectory));
                this.command = new DownloadCommand(dataDownloadDirectory, overwrite);
                break;
            case "simulate":

                System.err.println("SIMULATE");
                runPhenoSimulation();
                System.err.println("END");
                System.exit(1);
//                this.command = new SimulateCasesCommand(this.dataDownloadDirectory,
//                        n_cases_to_simulate, n_terms_per_case, n_noise_terms, gridSearch);
                break;
            case "svg":

//                if (diseaseId ==null) {
//                    printUsage("svg command requires --disease option");
//                }
                //n_terms_per_case, n_noise_terms);
                //this.command = new HpoCase2SvgCommand(this.dataDownloadDirectory, diseaseId,svgOutFileName,n_terms_per_case,n_noise_terms);
                break;
            case "phenogeno":

//                if (termList==null) {
//                    System.err.println("[ERROR] --term-list with list of HPO ids required");
//                    phenoGenoUsage();
//                    System.exit(1);
//                }
//                if (diseaseId==null){
//                    System.err.println("[ERROR] --disease option (e.g., OMIM:600100) required");
//                    phenoGenoUsage();
//                    System.exit(1);
//                }
//                if (entrezGeneId==null){
//                    System.err.println("[ERROR] --geneid option (e.g., 2200) required");
//                    phenoGenoUsage();
//                    System.exit(1);
//                }
//                if (backgroundFreq==null) {
//                    backgroundFreq=DEFAULT_BACKGROUND_FREQ;
//                }
//                this.command = new SimulatePhenoGenoCaseCommand(this.dataDownloadDirectory,
//                        this.entrezGeneId,
//                        this.varcount,
//                        this.varpath,
//                        this.diseaseId,
//                        this.termList,
//                        this.backgroundFreq);
//                break;
            default:
                printUsage(String.format("Did not recognize command: \"%s\"", mycommand));
        }

        command.execute();
        logger.trace("done execution");
    }


    private void runGenoPhenoSimulation() {
        ApplicationContext context = new AnnotationConfigApplicationContext(Lr2pgConfiguration.class);
        HpoPhenoGenoCaseSimulator simulator = context.getBean(HpoPhenoGenoCaseSimulator.class);
    }



    private void runPhenoSimulation() {
        ApplicationContext context = new AnnotationConfigApplicationContext(Lr2pgConfiguration.class);
        PhenotypeOnlyHpoCaseSimulator simulator = context.getBean(PhenotypeOnlyHpoCaseSimulator.class);
        simulator.debugPrint();
        try {
            simulator.simulateCases();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
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



    /**
     * Print usage information
     */
    public static void printUsage(String message) {
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
