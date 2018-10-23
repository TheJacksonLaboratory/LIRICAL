package org.monarchinitiative.lr2pg;


import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.analysis.GridSearch;
import org.monarchinitiative.lr2pg.analysis.Vcf2GenotypeMap;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.configuration.Lr2pgConfiguration;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.hpo.HpoPhenoGenoCaseSimulator;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.lr2pg.io.HpoDownloader;
import org.monarchinitiative.lr2pg.io.YamlParser;
import org.monarchinitiative.lr2pg.svg.Lr2Svg;
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
public class Lr2pgApplicationRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(Lr2pgApplicationRunner.class);


    private HpoOntology ontology;

    private Map<TermId, HpoDisease> diseaseMap;


    private PhenotypeOnlyHpoCaseSimulator phenotypeOnlyHpoCaseSimulator;


    private String diseaseId;

    private String dataDownloadDirectory;


    private HpoPhenoGenoCaseSimulator hpoPhenoGenoCaseSimulatorsimulator;
    //

    private  Map<TermId, String> geneId2SymbolMap;


    private GridSearch gridSearch;

    @Autowired
    private String hpOboPath;



//    @Autowired
//    JannovarData jannovarData;


//    @Autowired @Lazy
//    private PredPathCalculator ppcalcalculator;

    @Override
    public void run(ApplicationArguments args) {

        logger.error("Application started with command-line arguments: {}", Arrays.toString(args.getSourceArgs()));
        logger.error("NonOptionArgs: {}", args.getNonOptionArgs());
        logger.error("OptionNames: {}", args.getOptionNames());


        for (String name : args.getOptionNames()) {
            logger.error("arg: " + name + "=" + args.getOptionValues(name));
        }

        if (args.containsOption("help") || args.containsOption("h")) {
            printUsage("See the README file for usage!");
        }

        List<String> nonoptionargs = args.getNonOptionArgs();
        if (nonoptionargs.size() != 1) {
            for (String s : nonoptionargs) {
                System.err.println("noa=" + s);
            }
            printUsage("[ERROR] No program analysis given-size=" + nonoptionargs.size());

            return;
        }
        ApplicationContext context = new AnnotationConfigApplicationContext(Lr2pgConfiguration.class);
        // if we get here, we have one analysis
        String mycommand = nonoptionargs.get(0);
        logger.error("Command="+mycommand);

        String yml="src/main/resources/yaml/demo3.yml";
        YamlParser yparser = new YamlParser(yml);
        Lr2PgFactory factory=null;
        try {
            Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder().
                    hp_obo(yparser.getHpOboPath()).
                    mvStore(yparser.getMvStorePath())
                    .mim2genemedgen(yparser.getMedgen())
                    .geneInfo(yparser.getGeneInfo())
                    .phenotypeAnnotation(yparser.phenotypeAnnotation())
                    .vcf(yparser.vcfPath()).
                    jannovarFile(yparser.jannovarFile());
            factory=builder.build();
        } catch (Lr2pgException e){
            e.printStackTrace();
        }


        switch (mycommand) {
            case "download":
                boolean overwrite = false;
                logger.warn(String.format("Download analysis to %s", dataDownloadDirectory));
                HpoDownloader downloader = new HpoDownloader(dataDownloadDirectory, overwrite);
                downloader.download();
                break;
            case "simulate":
                System.err.println("SIMULATE");
                phenotypeOnlyHpoCaseSimulator.debugPrint();
                try {
                    phenotypeOnlyHpoCaseSimulator.simulateCases();
                } catch (Lr2pgException e) {
                    e.printStackTrace();
                }
                break;
            case "svg":
                // do SVG with pheno only
                int cases_to_simulate = 1;
                if (this.diseaseId == null || this.diseaseId.isEmpty()) {
                    System.err.println("Error diseaseId not defined");
                    return;
                }
                TermId diseaseCurie = TermId.constructWithPrefix(diseaseId);
                // simulator = new PhenotypeOnlyHpoCaseSimulator( dataDirectory, cases_to_simulate, n_terms_per_case, n_noise_terms);
                try {
                    HpoDisease disease = phenotypeOnlyHpoCaseSimulator.name2disease(diseaseCurie);
                    phenotypeOnlyHpoCaseSimulator.simulateCase(disease);
                    HpoCase hpocase = phenotypeOnlyHpoCaseSimulator.getCurrentCase();
                    Lr2Svg l2svg = new Lr2Svg(hpocase, diseaseCurie, disease.getName(), ontology, null);
                    l2svg.writeSvg("test.svg");
                } catch (Lr2pgException e) {
                    e.printStackTrace();
                    System.err.println("Could not simulate case");
                }
                break;
            case "phenogeno":

                HpoCase hpocase = this.hpoPhenoGenoCaseSimulatorsimulator.evaluateCase();
                System.err.println(hpocase.toString());
                 diseaseCurie = TermId.constructWithPrefix("OMIM:154700");
                HpoDisease disease = diseaseMap.get(diseaseCurie);
                String diseaseName = disease.getName();
                //Map<TermId, String> geneId2SymbolMap=(Map<TermId, String>)context.getAutowireCapableBeanFactory().getBean("geneId2SymbolMap");
                this.hpoPhenoGenoCaseSimulatorsimulator.outputSvg(diseaseCurie, diseaseName, ontology, this.geneId2SymbolMap);
                System.err.println(this.hpoPhenoGenoCaseSimulatorsimulator.toString());

                break;
            case "grid":
                try {
                    gridSearch.gridsearch();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "vcf":
                try {
                    String vcf = factory.vcfPath();
                    MVStore mvstore = factory.mvStore();
                    JannovarData jannovarData = factory.jannovarData();
                    Vcf2GenotypeMap vcf2geno = new Vcf2GenotypeMap(vcf,jannovarData,mvstore,GenomeAssembly.HG19);
                    Map<TermId,Gene2Genotype> genotypeMap=vcf2geno.vcf2genotypeMap();
                } catch (Lr2pgException e) {
                    e.printStackTrace();
                }


//                System.err.println("FILE="+jannovarHg19File.getAbsolutePath());


            // vcfParser.parse(vcf);

                break;
            default:
                printUsage(String.format("Did not recognize analysis: \"%s\"", mycommand));
        }

        logger.trace("done execution");
    }


    private static String getVersion() {
        String DEFAULT = "0.4.0";// default, should be overwritten by the following.
        String version = null;
        try {
            Package p = Lr2pgApplicationRunner.class.getPackage();
            version = p.getImplementationVersion();
        } catch (Exception e) {
            // do nothing
        }
        return version != null ? version : DEFAULT;
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
