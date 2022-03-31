package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.io.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Age;
import org.phenopackets.schema.v1.core.Sex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Period;
import java.util.*;

/**
 * Run LIRICAL from a Phenopacket -- with or without accompanying VCF file.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter N Robinson</a>
 */

@CommandLine.Command(name = "phenopacket",
        aliases = {"P"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from a Phenopacket")
public class PhenopacketCommand extends AbstractPrioritizeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketCommand.class);

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            required = true,
            description = "path to phenopacket file")
    protected Path phenopacketPath;

//    /**
//     * List of HPO terms observed in the subject of the investigation.
//     */
//    private List<TermId> hpoIdList;
//    /**
//     * List of excluded HPO terms in the subject.
//     */
//    private List<TermId> negatedHpoIdList;
//    /**
//     * String representing the genome build (hg19 or hg38). We get this from the Phenopacket
//     */
//    private String genomeAssembly;
//    /**
//     * There are gene symbols returned by Jannovar for which we cannot find a geneId. This issues seems to be related
//     * to the input files used by Jannovar from UCSC ( knownToLocusLink.txt.gz has links between ucsc ids, e.g.,
//     * uc003fts.3, and NCBIGene ids (earlier known as locus link), e.g., 1370).
//     */
//    private Set<String> symbolsWithoutGeneIds;
//    /**
//     * Path to the VCF file (if any).
//     */
//    private Path vcfPath = null;

    // TODO - VariantMetadataService, VariantParser, GenotypedVariantParser

//    /**
//     * Run an analysis of a phenopacket that contains a VCF file.
//     */
//    private void runVcfAnalysis(Map<TermId, HpoDisease> diseaseMap,
//                                Map<TermId, Collection<GeneIdentifier>> diseaseToGene,
//                                Map<TermId, Gene2Genotype> geneToGenotype, Map<String, String> metadata) {
////        HpoAssociationData hpoAssociationData = readHpoAssociationData(); // TODO -
//        this.factory = LiricalFactory.builder()
//                .ontology(ontology)
//                .genomeAssembly(this.genomeAssembly)
//                .exomiser(this.exomiserDataDirectory)
//                .vcf(this.vcfPath)
//                .backgroundFrequency(this.backgroundFrequencyFile)
//                .global(this.globalAnalysisMode)
//                .orphanet(this.useOrphanet)
//                .transcriptdatabase(this.transcriptDb)
//                .lrThreshold(this.lrThreshold)
//                .minDiff(this.minDifferentialsToShow)
//                .build();
////        factory.qcHumanPhenotypeOntologyFiles(); // TODO - check
////        factory.qcExternalFilesInDataDir();
//        factory.qcExomiserFiles();
//        factory.qcGenomeBuild();
//        factory.qcVcfFile();
//
//        symbolsWithoutGeneIds = factory.getSymbolsWithoutGeneIds();
//        GenotypeLikelihoodRatio genoLr = factory.getGenotypeLR();
//        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(this.ontology, diseaseMap);
//        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
//                .ontology(this.ontology)
//                .negated(this.negatedHpoIdList)
//                .diseaseMap(diseaseMap)
//                .disease2geneMultimap(diseaseToGene)
//                .genotypeMap(geneToGenotype)
//                .phenotypeLr(phenoLr)
//                .genotypeLr(genoLr);
//
//        CaseEvaluator evaluator = caseBuilder.build();
//        HpoCase hcase = evaluator.evaluate();
//
//        if (!factory.transcriptdb().equals("n/a")) {
//            metadata.put("transcriptDatabase", factory.transcriptdb());
//        }
//        int n_genes_with_var = geneToGenotype.size();
//        metadata.put("genesWithVar", String.valueOf(n_genes_with_var));
//        metadata.put("exomiserPath", factory.getExomiserPath().map(Path::toAbsolutePath).map(Path::toString).orElse(""));
//        metadata.put("hpoVersion", factory.getHpoVersion());
//        metadata.put("sample_name", factory.getSampleName());
//        if (globalAnalysisMode) {
//            metadata.put("global_mode", "true");
//        } else {
//            metadata.put("global_mode", "false");
//        }
//        this.geneId2symbol = factory.geneId2symbolMap();
//        List<String> errors = evaluator.getErrors();
//        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology, metadata)
//                .genotypeMap(geneToGenotype)
//                .geneid2symMap(this.geneId2symbol)
//                .errors(errors)
//                .outdirectory(this.outdir)
//                .threshold(factory.getLrThreshold())
//                .symbolsWithOutIds(symbolsWithoutGeneIds)
//                .mindiff(factory.getMinDifferentials())
//                .prefix(this.outfilePrefix);
//
//        if (outputTSV) {
//            builder.buildGenoPhenoTsvTemplate().outputFile();
//        }
//        if (outputHTML) {
//            builder.buildGenoPhenoHtmlTemplate().outputFile();
//        }
//    }

//    /**
//     * Run an analysis of a phenopacket that only has Phenotype data
//     */
//    private void runPhenotypeOnlyAnalysis(Map<TermId, HpoDisease> diseaseMap,
//                                          HashMap<String, String> metadata) {
//        this.factory = LiricalFactory.builder()
//                .ontology(ontology)
//                .orphanet(this.useOrphanet)
//                .build();
//        // TODO - check
////        factory.qcHumanPhenotypeOntologyFiles();
////        factory.qcExternalFilesInDataDir();
//        PhenotypeLikelihoodRatio phenoLr = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
//        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(this.hpoIdList)
//                .ontology(this.ontology)
//                .negated(this.negatedHpoIdList)
//                .diseaseMap(diseaseMap)
//                .phenotypeLr(phenoLr);
//        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
//        HpoCase hcase = evaluator.evaluate();
//        metadata.put("hpoVersion", factory.getHpoVersion());
//        List<String> errors = evaluator.getErrors();
//        LiricalTemplate.Builder builder = new LiricalTemplate.Builder(hcase,ontology,metadata)
//                .prefix(this.outfilePrefix)
//                .outdirectory(this.outdir)
//                .threshold(this.factory.getLrThreshold())
//                .mindiff(this.factory.getMinDifferentials())
//                .errors(errors);
//        LiricalTemplate template = outputTSV ?
//                builder.buildPhenotypeTsvTemplate() :
//                builder.buildPhenotypeHtmlTemplate();
//        template.outputFile();
//    }


//    @Override
//    public Integer call() throws LiricalDataException { // TODO - remove the exceptions
//
//
//        // 2. - analyze case
//
//
////        Path vcfPath = vcfPathOpt.orElse(null);
////        boolean useGenotype = vcfPath != null;
//        boolean useGenotype = false;
////        AnalysisOptions options = new AnalysisOptions(globalAnalysisMode);
////        AnalysisResults results = analyzer.runAnalysis(analysisData, options);
//
//
////        try {
////            PhenotypeService phenotypeService = lirical.phenotypeService();
////            HpoAssociationData hpoAssociationData = phenotypeService.hpoAssociationData();
//        HashMap<String, String> metadata = new HashMap<>(); // TODO - move down, do not use in the analysis
////            if (useGenotype) {
////                runVcfAnalysis(
////                        factory.diseaseMap(phenotypeService.ontology()),
////                        hpoAssociationData.diseaseToGenes(), factory.getGene2GenotypeMap(), metadata); // try to run VCF analysis because use passed -e option.
////            } else {
//                // i.e., the Phenopacket has no VCF reference -- LIRICAL will work on just phenotypes!
////                runPhenotypeOnlyAnalysis(factory.diseaseMap(ontology), metadata);
////            }
////        } catch (IOException e) {
////            logger.error("Error: {}", e.getMessage(), e);
////            return 1;
////        }
//
//        // 3. - write results
//        if (useGenotype) {
//            metadata.put("vcf_file", vcfPath.toAbsolutePath().toString());
//        }
//        this.genomeAssembly = importer.getGenomeAssembly().orElse(null); // LiricalFactory should use hg38 if null
//
//        metadata.put("sample_name", importer.getSamplename());
//
//
//        logger.trace("Will analyze phenopacket at " + phenopacketPath);
//        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
//        Date date = new Date();
//        metadata.put("analysis_date", dateFormat.format(date));
//        metadata.put("phenopacket_file", phenopacketPath.toAbsolutePath().toString());
//
//        return 0;
//    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) {
        // Read the Phenopacket
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath);

        // Parse & sanitize HPO terms
        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> observedTerms = importer.getHpoTerms().stream()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();
        List<TermId> negatedTerms = importer.getNegatedHpoTerms().stream()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        // Parse sample attributes
        String sampleId = importer.getSampleId();
        org.monarchinitiative.lirical.hpo.Age age = importer.getAge().filter(a -> Age.getDefaultInstance().equals(a))
                .map(Age::getAge)
                .map(Period::parse)
                .map(org.monarchinitiative.lirical.hpo.Age::parse)
                .orElse(org.monarchinitiative.lirical.hpo.Age.ageNotKnown());

        org.monarchinitiative.lirical.hpo.Sex sex = importer.getSex()
                .map(this::toSex)
                .orElse(org.monarchinitiative.lirical.hpo.Sex.UNKNOWN);

        // Go through VCF file (if present)
        GenesAndGenotypes genes;
        Optional<Path> vcfPathOpt = importer.getVcfPath();
        if (vcfPathOpt.isEmpty()) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(vcfPathOpt.get(), lirical.genomeBuild(), lirical.variantMetadataService(), lirical.phenotypeService().associationData());
        }

        return AnalysisData.of(sampleId, age, sex, observedTerms, negatedTerms, genes);
    }

    private org.monarchinitiative.lirical.hpo.Sex toSex(Sex sex) {
        return switch (sex) {
            case MALE -> org.monarchinitiative.lirical.hpo.Sex.MALE;
            case FEMALE -> org.monarchinitiative.lirical.hpo.Sex.FEMALE;
            case OTHER_SEX, UNKNOWN_SEX, UNRECOGNIZED -> org.monarchinitiative.lirical.hpo.Sex.UNKNOWN;
        };
    }

}
