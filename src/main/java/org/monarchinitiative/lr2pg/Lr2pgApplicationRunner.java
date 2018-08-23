package org.monarchinitiative.lr2pg;


import org.monarchinitiative.lr2pg.command.*;
import org.monarchinitiative.lr2pg.configuration.Lr2pgConfiguration;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class Lr2pgApplicationRunner implements ApplicationRunner  {
    private static final Logger logger = LoggerFactory.getLogger(Lr2pgApplicationRunner.class);

    @Autowired
    private HpoOntology ontology;

    @Autowired
    private Map<TermId,HpoDisease> diseaseMap;



    private String dataDownloadDirectory;

    @Autowired
    private
    HpoPhenoGenoCaseSimulator hpoPhenoGenoCaseSimulatorsimulator;
    //
    @Autowired
    private
    Map<TermId, String> geneId2SymbolMap;

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
            printUsage("[ERROR] No program command given-size="+nonoptionargs.size());
            for (String s:nonoptionargs) {
                System.err.println("noa="+s);
            }
        }
        ApplicationContext context = new AnnotationConfigApplicationContext(Lr2pgConfiguration.class);
        // if we get here, we have one command
        String mycommand = nonoptionargs.get(0);

        // collect the options

        switch (mycommand) {
            case "download":
                boolean overwrite=false;
                logger.warn(String.format("Download command to %s", dataDownloadDirectory));
               // TODO -- implement download command here
                break;
            case "simulate":
                System.err.println("SIMULATE");
                PhenotypeOnlyHpoCaseSimulator simulator = context.getBean(PhenotypeOnlyHpoCaseSimulator.class);
                simulator.debugPrint();
                try {
                    simulator.simulateCases();
                } catch (Lr2pgException e) {
                    e.printStackTrace();
                }
                break;
            case "svg":

//                if (diseaseId ==null) {
//                    printUsage("svg command requires --disease option");
//                }
                //n_terms_per_case, n_noise_terms);
                //this.command = new HpoCase2SvgCommand(this.dataDownloadDirectory, diseaseId,svgOutFileName,n_terms_per_case,n_noise_terms);
                break;
            case "phenogeno":

                HpoCase hpocase = this.hpoPhenoGenoCaseSimulatorsimulator.evaluateCase();
                System.err.println(hpocase.toString());
                TermId diseaseCurie=TermId.constructWithPrefix("OMIM:154700");
                HpoDisease disease = diseaseMap.get(diseaseCurie);
                String diseaseName = disease.getName();
                //Map<TermId, String> geneId2SymbolMap=(Map<TermId, String>)context.getAutowireCapableBeanFactory().getBean("geneId2SymbolMap");
                this.hpoPhenoGenoCaseSimulatorsimulator.outputSvg(diseaseCurie,diseaseName,ontology, this.geneId2SymbolMap);
                System.err.println(this.hpoPhenoGenoCaseSimulatorsimulator.toString());

                break;
            default:
                printUsage(String.format("Did not recognize command: \"%s\"", mycommand));
        }

        logger.trace("done execution");
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
