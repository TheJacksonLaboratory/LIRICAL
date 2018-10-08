package org.monarchinitiative.lr2pg.vcf;


import com.google.common.collect.Sets;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import de.charite.compbio.jannovar.annotation.Annotation;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.cmd.annotate_vcf.JannovarAnnotateVCFOptions;
import htsjdk.variant.variantcontext.VariantContext;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import org.monarchinitiative.exomiser.core.genome.VariantAnnotator;
import org.monarchinitiative.exomiser.core.genome.VariantContextSampleGenotypeConverter;
import org.monarchinitiative.exomiser.core.genome.VariantDataService;
import org.monarchinitiative.exomiser.core.genome.VcfFiles;
import org.monarchinitiative.exomiser.core.genome.dao.serialisers.MvStoreUtil;
import org.monarchinitiative.exomiser.core.model.*;
import org.monarchinitiative.exomiser.core.model.VariantAnnotation;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;
import org.monarchinitiative.exomiser.core.model.frequency.Frequency;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.VariantEffectPathogenicityScore;
import org.monarchinitiative.exomiser.core.proto.AlleleProto;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.monarchinitiative.exomiser.core.model.frequency.FrequencySource.*;

/**
 * This class calculates the relative proportions of variants assessed as having a certain degree of pathogenicity
 * by Exomiser. We examine all of the variants and place them into two pathogenicity bins: predicted benign (0-0.8)
 * and predicted pathogenic (0.8-1.0). We then calculate the sum of all predicted pathogenic variant frequencies using
 * the population background from the Exomiser database (gnomAD, TOPMed, others, but filtering out variants that are listed
 * in ClinVar).
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class PredPathCalculator {
    private static final Logger logger = LoggerFactory.getLogger(PredPathCalculator.class);
    /** We will assume a frequency of 1:100,000 if no frequency data is available. */
    private final float DEFAULT_FREQUENCY = 0.00001F;
    /** The Exomiser reports the frequencies as percentages -- thus we need to convert here, because we want the default frequency to be 1:100_000. */
    private final float DEFAULT_PERCENTAGE = 100 * DEFAULT_FREQUENCY;
    /** A set of interpretation classes from ClinVar that we will regard as pathogenic. */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);
    /** File name for the file that wiull contain the frequencies of predicted pathogenic variants in the
     * population background, i.e., from gnomAD and TOPMed from the Exomiser database.*/
    private String outputFileName="background-freq.txt";

    /** An Exomiser class that annotates an arbitrary variant with frequency and pathogenicity information. */
    private final VariantAnnotator variantAnnotator;
    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    private final VariantDataService variantDataService;
    /** Key: e.g., FBN1; value: e.g., 2200 (the Entrez Gene Id of the FBN1 gene). */
    private Map<String,String> symbol2geneIdMap=new HashMap<>();

    private JannovarAnnotateVCFOptions options;

    public PredPathCalculator(VariantAnnotator variantAnnotator, MVStore alleleStore, VariantDataService variantDataService) {
        this.variantAnnotator = variantAnnotator;
        this.alleleMap = MvStoreUtil.openAlleleMVMap(alleleStore);
        this.variantDataService = variantDataService;
        initJannovarOptions();
    }

    private void initJannovarOptions() {
        new JannovarAnnotateVCFOptions();
    }


    private final Set<String> geneSymbolSet = new HashSet<>();

    private final HashMap<String,String> symbol2idMap = new HashMap<>();



    /**
     * This function inputs the data from the MV store, bins each variant into one of four categories,
     * normalizes the frequencies, and writes the results to a file that can be used elsewhere.
     */
    void run() {
        boolean doClinvar=false;
        boolean doGnomad=true;
        boolean do1000genomes=false;
        logger.info("Running...");
       /* to do */
    }




    /**
     * This function writes all of the pathogenicity scores for any variant classified as pathogenic by ClinVar to
     * a file.
     */
//    private void calculateGenotypePathScoreOLDs(String vcfPath) {
//        try {
//            BufferedWriter cvwriter = new BufferedWriter(new FileWriter("clinvarpath.txt"));
//            for (Map.Entry<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> entry : alleleMap.entrySet()) {
//                AlleleProto.AlleleKey alleleKey = entry.getKey();
//                AlleleProto.AlleleProperties alleleProperties = entry.getValue();
//                VariantAnnotation variantAnnotation = variantAnnotator.annotate(String.valueOf(alleleKey.getChr()), alleleKey.getPosition(), alleleKey.getRef(), alleleKey.getAlt());
//                VariantEffect variantEffect = variantAnnotation.getVariantEffect();
//                if (!variantEffect.isOffExome()) {
//                    PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProperties);
//                    if (pathogenicityData.isEmpty()) {
//                        //pathDataEmpty++;
//                        continue; // should almost never happen
//                    }
//                    float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
//                    ClinVarData clinVarData = pathogenicityData.getClinVarData();
//                    // ClinVar have three 'pathogenic' significance values - pathogenic, pathogenic_or_likely_pathogenic and likely_pathogenic
//                    // they also have a review status which will tell you how much confidence you might want to assign a given interpretation.
//                    // see https://www.ncbi.nlm.nih.gov/clinvar/docs/clinsig/
//                    if (PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS.contains(clinVarData.getPrimaryInterpretation())) {
//                        cvwriter.write(pathogenicity + "\n");
//                    }
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    int otherAll=0;

    private int getAltAlleleCount(Collection<SampleGenotype> sampleGenotypes) {
        int c=0;
        for (SampleGenotype sampleGenotype : sampleGenotypes) {
            for  (AlleleCall alleleCall : sampleGenotype.getCalls()) {
                // Alternatively log the het and hom count
                if (alleleCall == AlleleCall.ALT || alleleCall == AlleleCall.OTHER_ALT) {
                    c++;
                }
                if (alleleCall.equals(AlleleCall.OTHER_ALT)) {
                    otherAll++;
                    if (otherAll%1000==0) {
                        System.err.println("OTHER AL="+otherAll);
                    }
                }
            }
        }
        return c;
    }

    private static final String SO_SPLICE_REGION_VARIANT="SO:0001630";
    private static final String SO_SPLICE_SITE_VARIANT="SO:0001630";
    private static final String SO_SPLICE_DONOR_VARIANT="SO:0001575";






    private void calculateGenotypePathScores(String vcfPath) {
        logger.info("Calculating path scores for " + vcfPath);
        AnnotatingVariantFactory annotatingVariantFactory = new AnnotatingVariantFactory(variantAnnotator, variantDataService);

        Map<String, Double> geneAlleleFrequencies = new HashMap<>();

//        String chr15="/home/robinp/data/exomiserdata/1kg/ALL.chr15.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz";
//        String pfeifer="C:/Users/hhx640/Documents/exomiser-cli-dev/examples/Pfeiffer-quartet.vcf";
        Path path = Paths.get(vcfPath);
        Stream<VariantContext> variantContextStream = VcfFiles.readVariantContexts(path);
        Stream<VariantEvaluation> variantEvaluationStream = annotatingVariantFactory.createVariantEvaluations(variantContextStream);
        variantEvaluationStream.forEach(variantEvaluation -> {
//            logger.debug("{} {}", variantEvaluation.getGeneSymbol(), variantEvaluation);
//            logger.debug("{}", variantEvaluation.getPathogenicityData());

            String geneSymbol = variantEvaluation.getGeneSymbol();
            String id = variantEvaluation.getGeneId();
            this.symbol2geneIdMap.put(geneSymbol,id);
            // The following makes sure that each gene symbol appears once in the map.
            Double result = geneAlleleFrequencies.putIfAbsent(geneSymbol,0.0);
            int alleleCount = 0;
            if (variantEvaluation.getPathogenicityScore() >= 0.9) {

                int altAlleleId = variantEvaluation.getAltAlleleId();
                VariantContext variantContext = variantEvaluation.getVariantContext();
                Map<String, SampleGenotype> sampleGenotypes = VariantContextSampleGenotypeConverter.createAlleleSampleGenotypes(variantContext, altAlleleId);
                logger.debug("Converting {} {} {} {}", geneSymbol, variantContext.getReference(), variantContext.getAlternateAllele(altAlleleId), variantContext.getGenotypes());

                alleleCount= getAltAlleleCount(sampleGenotypes.values());
                String SOvarEffect = variantEvaluation.getVariantEffect().getSequenceOID();
                if (SOvarEffect.equals(SO_SPLICE_REGION_VARIANT)) {
                    // This is ion the splice region but is not splice donor or acceptor.
                    // It should have a pathogenicity score much less than 0.9 -- this should be fixed in Exomiser
                    // Therefore, just skip this variant!!
                    // TODO refactor Exomiser
                } else {
                    // What is the denominator?
                    int totalSampleCount=sampleGenotypes.size();
                    double freq = (double) alleleCount/totalSampleCount;
                    double current = geneAlleleFrequencies.get(geneSymbol);
                    geneAlleleFrequencies.put(geneSymbol,current + freq);
                }
                if (geneSymbol.equals("FBN1")) {
                    List<TranscriptAnnotation> tr=variantEvaluation.getTranscriptAnnotations();
                    String ta = tr.size()>0?tr.get(0).toString() : "?";
                    System.out.println(String.format("%s: %s %s>%s n=%d", ta,geneSymbol, variantContext.getReference(), variantContext.getAlternateAllele(altAlleleId),alleleCount));
                }
            }

            logger.debug("{} {}", geneSymbol, geneAlleleFrequencies.get(geneSymbol));
        });

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("thousandGenomeAlleleCounts.txt"))) {
            writer.write("#GeneSymbol\tGeneID\tSumAlleleFrequencies\n");
            for (Map.Entry<String, Double> entry : geneAlleleFrequencies.entrySet()){
                String genesymbol=entry.getKey();
                String geneid = this.symbol2geneIdMap.get(genesymbol);
                writer.write(entry.getKey() + "\t" + geneid + "\t"+ entry.getValue() + "\n");
                writer.flush();
            }
        } catch (Exception e) {
            logger.error("[ERROR] unable to write gene-allele frequencies: {}", e);
        }

    }


    /**
     * This method goes through all of the Exomiser's variants and records the frequencies of variants in the
     * predicted pathogenic bin (i.e., Exomiser score of 0.9 to 1.0).
     * @throws IOException thrown if there is an issue writing to file.
     */
//    private void binPathogenicityData() {
//        logger.trace("Binning pathogenicity data...");
//        for (Map.Entry<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> entry : alleleMap.entrySet()) {
//            AlleleProto.AlleleKey alleleKey = entry.getKey();
//            AlleleProto.AlleleProperties alleleProperties = entry.getValue();
//            VariantAnnotation variantAnnotation = variantAnnotator.annotate(String.valueOf(alleleKey.getChr()), alleleKey.getPosition(), alleleKey.getRef(), alleleKey.getAlt());
//            VariantEffect variantEffect = variantAnnotation.getVariantEffect();
//            if (!variantEffect.isOffExome()) {
//                //package org.monarchinitiative.exomiser.core.model.frequency;
//                // Note that frequency data are expressed as percentages
//                FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProperties);
//                PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProperties);
//                if (pathogenicityData.isEmpty()) {
//                   // pathDataEmpty++;
//                }
//                String SOvarEffect = variantEffect.getSequenceOID();
//                if (SOvarEffect.equals(SO_SPLICE_REGION_VARIANT)) {
//                    continue; // skip the +3..+6 splice region vars etc
//                }
//                // The following is the population (background) frequency, expressed as a percentage
//                // If we have not information  about the frequency, assume the variant is rare and
//                // use the default value
//                if (! frequencyData.hasKnownFrequency())
//                    continue; // skip unknown's
//
//
//                // ClinVar annotations are often assigned to variants without any pathogenicity data from dbNSFP, in these cases they would receive a score of zero
//                // in these cases we'll give them a default score based on their variant effect - these are often frameshift types.
//                float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
//                String genesymbol = variantAnnotation.getGeneSymbol();
//                String id = variantAnnotation.getGeneId();
////                ClinVarData clinVarData = pathogenicityData.getClinVarData();
////                // ClinVar have three 'pathogenic' significance values - pathogenic, pathogenic_or_likely_pathogenic and likely_pathogenic
////                // they also have a review status which will tell you how much confidence you might want to assign a given interpretation.
////                // see https://www.ncbi.nlm.nih.gov/clinvar/docs/clinsig/
////                if (PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS.contains(clinVarData.getPrimaryInterpretation())) {
////                  // We skip any pathogenic variants in this dataset
////                }
//
//                Frequency afr = frequencyData.getFrequencyForSource(GNOMAD_E_AFR);
//                if (afr==null) {
//                    afr = frequencyData.getFrequencyForSource(GNOMAD_G_AFR);
//                }
//
//                Frequency eas = frequencyData.getFrequencyForSource(GNOMAD_E_EAS);
//                if (eas==null) {
//                    eas=frequencyData.getFrequencyForSource(GNOMAD_G_EAS);
//                }
//                if (eas!=null) {
//                    float frequencyAsPercentage = eas.getFrequency();
//                    //addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_EAS);
//                }
//
//            }
//
//        }
//    }

    /**
     * Calculate a pathogenicity score for the current variant in the same way that the Exomiser does.
     * @param variantEffect class of variant such as Missense, Nonsense, Synonymous, etc.
     * @param pathogenicityData Object representing the predicted pathogenicity of the data.
     * @return the predicted pathogenicity score.
     */
    private float calculatePathogenicity(VariantEffect variantEffect, PathogenicityData pathogenicityData) {
        float predictedScore = pathogenicityData.getScore();
        float variantEffectScore = VariantEffectPathogenicityScore.getPathogenicityScoreOf(variantEffect);
        switch (variantEffect) {
            case MISSENSE_VARIANT:
                return pathogenicityData.hasPredictedScore() ? predictedScore : variantEffectScore;
            case SYNONYMOUS_VARIANT:
                // there are cases where synonymous variants have been assigned a high MutationTaser score.
                // These looked to have been wrongly mapped and are therefore probably wrong. So we'll use the default score for these.
                return variantEffectScore;
            default:
                return Math.max(predictedScore, variantEffectScore);
        }
    }

    private FrequencySource[] orderedSources = {GNOMAD_E_AFR,GNOMAD_E_AMR,GNOMAD_E_ASJ,GNOMAD_E_EAS,GNOMAD_E_FIN,GNOMAD_E_NFE,GNOMAD_E_SAS};

    private String[] headerFields = {"AFR","AMR","ASJ","EAS","FIN","NFE","SAS"};









}
