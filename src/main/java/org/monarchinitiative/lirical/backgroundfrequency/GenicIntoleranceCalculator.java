package org.monarchinitiative.lirical.backgroundfrequency;


import com.google.common.collect.Sets;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import org.monarchinitiative.exomiser.core.genome.VariantAnnotator;
import org.monarchinitiative.exomiser.core.genome.dao.serialisers.MvStoreUtil;
import org.monarchinitiative.exomiser.core.model.*;
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
import java.util.*;

import static org.monarchinitiative.exomiser.core.model.frequency.FrequencySource.*;

/**
 * This class calculates the relative proportions of variants assessed as having a certain degree of pathogenicity
 * by Exomiser. We examine all of the variants and place them into two pathogenicity bins: predicted benign (0-0.8)
 * and predicted pathogenic (0.8-1.0). We then calculate the sum of all predicted pathogenic variant frequencies using
 * the population background from the Exomiser database (gnomAD, TOPMed, others, but filtering out variants that are listed
 * in ClinVar).
 *
 * @author <a href="mailto:j.jacobsen@qmul.ac.uk>Jules Jacobsen</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GenicIntoleranceCalculator {
    private static final Logger logger = LoggerFactory.getLogger(GenicIntoleranceCalculator.class);
    /** We will assume a frequency of 1:100,000 if no frequency data is available. */
    private final float DEFAULT_FREQUENCY = 0.00001F;
    /** The Exomiser reports the frequencies as percentages -- thus we need to convert here, because we want the default frequency to be 1:100_000. */
    private final float DEFAULT_PERCENTAGE = 100 * DEFAULT_FREQUENCY;
    /** A set of interpretation classes from ClinVar that we will regard as pathogenic. */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);
    private static final Set<ClinVarData.ClinSig> BENIGN_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.BENIGN,
                    ClinVarData.ClinSig.LIKELY_BENIGN,
                    ClinVarData.ClinSig.BENIGN_OR_LIKELY_BENIGN);
    /** Ordered list of the populations included in the calculations. */
    private FrequencySource[] orderedSources = {GNOMAD_E_AFR,GNOMAD_E_AMR,GNOMAD_E_ASJ,GNOMAD_E_EAS,GNOMAD_E_FIN,GNOMAD_E_NFE,GNOMAD_E_SAS};
    /** The header of the org.monarchinitiative.lirical.output file that shows the populations included in the calculation. */
    private String[] headerFields = {"AFR","AMR","ASJ","EAS","FIN","NFE","SAS"};
    /** File name for the file that will contain the frequencies of predicted pathogenic variants in the
     * population background, i.e., from gnomAD  from the Exomiser database.*/
    private final String outputFileName;
    /** An Exomiser class that annotates an arbitrary variant with frequency and pathogenicity information. */
    private final VariantAnnotator variantAnnotator;
    /** Exomiser data store. */
    private final MVMap<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> alleleMap;
    /** If true, calculate the distribution of ClinVar pathogenicity scores. */
    private final boolean doClinvar;

    /**
     * @param variantAnnotator Object to annotate an arbitrary variant
     * @param alleleStore Exomiser data resource
     * @param fname name of org.monarchinitiative.lirical.output file (background-hg38.txt or background-hg19.tsv).
     * @param doClinvar flag that if true will cause the analysis to calculate the distribution of Clinvar pathogenicity scores
     */
    public GenicIntoleranceCalculator(VariantAnnotator variantAnnotator, MVStore alleleStore, String fname, boolean doClinvar) {
        this.variantAnnotator = variantAnnotator;
        this.alleleMap = MvStoreUtil.openAlleleMVMap(alleleStore);
        this.outputFileName=fname;
        this.doClinvar=doClinvar;
    }
    /** Key: a {@link FrequencySource}, representing a population; value: corresponding {@link Background} with background frequency for genes. */
    private final Map<FrequencySource,Background> backgroundMap = new HashMap<>();
    /** Set of all gene symbols used in our calculations. */
    private final Set<String> geneSymbolSet = new HashSet<>();
    /** Key: a gene symbol value: corresponding EntrezGene id */
    private final HashMap<String,String> symbol2idMap = new HashMap<>();




    /**
     * This function inputs the data from the MV store, bins each variant into one of four categories,
     * normalizes the frequencies, and writes the results to a file that can be used elsewhere.
     */
    public void run() {
        logger.info("Running...");
        if (doClinvar) {
            getClinvarPathScores();
        } else  { // do everything in GNOMAD
            initBins();
            binPathogenicityData();
            outputBinData();
        }
    }

    /** We initialize bins for the major GNOMAD populations, Gnomad exome AFR, AMR, ASJ,EAS,FIN,NFE, and SAS.
     * We will calculate overall frequencies separately for each population and also take the average.
     */
    private void initBins() {
        Background afr = new Background(GNOMAD_E_AFR);
        backgroundMap.put(GNOMAD_E_AFR,afr);
        Background amr = new Background(GNOMAD_E_AMR);
        backgroundMap.put(GNOMAD_E_AMR,amr);
        Background asj = new Background(GNOMAD_E_ASJ);
        backgroundMap.put(GNOMAD_E_ASJ,asj);
        Background eas = new Background(GNOMAD_E_EAS);
        backgroundMap.put(GNOMAD_E_EAS,eas);
        Background fin = new Background(GNOMAD_E_FIN);
        backgroundMap.put(GNOMAD_E_FIN,fin);
        Background nfe = new Background(GNOMAD_E_NFE);
        backgroundMap.put(GNOMAD_E_NFE,nfe);
        Background sas = new Background(GNOMAD_E_SAS);
        backgroundMap.put(GNOMAD_E_SAS,sas);
    }

    /**
     * Add a single variant's frequency/pathogenicity values to the appropriate bin
     * @param genesymbol The symbol of the gene that harbors the variant
     * @param geneId The Entrez Gene id of the gene that harbors the variant
     * @param frequency The frequency of the variant in the cohort represented by g2bmap
     * @param pathogenicity The pathogenicity of the variant as predicted by Exomiser
     */
    private void addToBin(String genesymbol, String geneId, double frequency, double pathogenicity, FrequencySource fsource) {
        Map<String, Gene2Bin> background2binMap = backgroundMap.get(fsource).getBackground2binMap();
        geneSymbolSet.add(genesymbol);
        symbol2idMap.put(genesymbol,geneId);
        if (!background2binMap.containsKey(genesymbol)) {
            Gene2Bin g2b = new Gene2Bin(genesymbol, geneId);
            background2binMap.put(genesymbol, g2b);
            if (background2binMap.size() % 100 == 0) {
                logger.info(String.format("Analyzing gene %d.\r", background2binMap.size()));
            }
        }
        Gene2Bin g2b = background2binMap.get(genesymbol);
        g2b.addVar(frequency, pathogenicity);
    }


    /**
     * This function writes all of the pathogenicity scores for any variant classified as pathogenic by ClinVar to
     * a file.
     */
    private void getClinvarPathScores() {
        try {
            BufferedWriter cvwriter = new BufferedWriter(new FileWriter("clinvarpath.txt"));
            int i=0;
            System.out.println("Analyzing pathogenic and benign ClinVar variants...");
            for (Map.Entry<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> entry : alleleMap.entrySet()) {
                AlleleProto.AlleleKey alleleKey = entry.getKey();
                AlleleProto.AlleleProperties alleleProperties = entry.getValue();
                VariantAnnotation variantAnnotation = variantAnnotator.annotate(String.valueOf(alleleKey.getChr()), alleleKey.getPosition(), alleleKey.getRef(), alleleKey.getAlt());
                VariantEffect variantEffect = variantAnnotation.getVariantEffect();
                if (!variantEffect.isOffExome()) {
                    PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProperties);
                    if (pathogenicityData.isEmpty()) {
                        continue; // should almost never happen
                    }
                    float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
                    ClinVarData clinVarData = pathogenicityData.getClinVarData();
                    // ClinVar have three 'pathogenic' significance values - pathogenic, pathogenic_or_likely_pathogenic and likely_pathogenic
                    // they also have a review status which will tell you how much confidence you might want to assign a given interpretation.
                    // see https://www.ncbi.nlm.nih.gov/clinvar/docs/clinsig/
                    // there are also three categories that we will regard as "benign".
                    // We org.monarchinitiative.lirical.output the pathogenicity scores and the interpretation with the goal of visualizing
                    // the distributions of benign and pathogenic variant pathogenicity scores.
                    if (PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS.contains(clinVarData.getPrimaryInterpretation()) ||
                            BENIGN_CLINVAR_PRIMARY_INTERPRETATIONS.contains(clinVarData.getPrimaryInterpretation())) {
                        cvwriter.write(pathogenicity + "\t"+clinVarData.getPrimaryInterpretation()+"\n");
                        i++;
                        if (i%10==0) {
                            System.out.print("\rAdding clinvar variant "+i);
                        }
                    }
                }
            }
            System.out.println("\nAdded a total of " + i + " clinvar variants");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method goes through all of the Exomiser's variants and records the frequencies of variants in the
     * predicted pathogenic bin (i.e., Exomiser score of 0.8 to 1.0). Note that we disregard off-exome
     * variants. We first try to find the frequency in GNOMAD_E (exome), under the assumption that this data
     * source will be the most accurate for exonic variants. Failing that, we take the corresponding
     * GNOMAD_G (genome) data.
     */
    private void binPathogenicityData() {
        logger.trace("Binning pathogenicity data...");
        int c=0;
        for (Map.Entry<AlleleProto.AlleleKey, AlleleProto.AlleleProperties> entry : alleleMap.entrySet()) {
            AlleleProto.AlleleKey alleleKey = entry.getKey();
            AlleleProto.AlleleProperties alleleProperties = entry.getValue();
            VariantAnnotation variantAnnotation = variantAnnotator.annotate(String.valueOf(alleleKey.getChr()), alleleKey.getPosition(), alleleKey.getRef(), alleleKey.getAlt());
            VariantEffect variantEffect = variantAnnotation.getVariantEffect();
            if (!variantEffect.isOffExome()) {
                // Note that frequency data are expressed as percentages
                FrequencyData frequencyData = AlleleProtoAdaptor.toFrequencyData(alleleProperties);
                PathogenicityData pathogenicityData = AlleleProtoAdaptor.toPathogenicityData(alleleProperties);
                if (variantEffect == VariantEffect.SPLICE_REGION_VARIANT) {
                    continue; // skip the +3..+6 splice region vars etc
                }
                // The following is the population (background) frequency, expressed as a percentage
                // If we have not information  about the frequency, we will assume the variant is
                // possibly an artefact and skip it.
                if (! frequencyData.hasKnownFrequency())
                    continue; // skip unknown frequency variants

                float pathogenicity = calculatePathogenicity(variantEffect, pathogenicityData);
                String genesymbol = variantAnnotation.getGeneSymbol();
                String id = variantAnnotation.getGeneId();

                Frequency afr = frequencyData.getFrequencyForSource(GNOMAD_E_AFR);
                if (afr==null) {
                    afr = frequencyData.getFrequencyForSource(GNOMAD_G_AFR);
                }
                if (afr!=null) {
                    float frequencyAsPercentage = afr.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_AFR);
                }
                Frequency amr = frequencyData.getFrequencyForSource(GNOMAD_E_AMR);
                if (amr==null) {
                    amr=frequencyData.getFrequencyForSource(GNOMAD_G_AMR);
                }
                if (amr!=null) {
                    float frequencyAsPercentage = amr.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_AMR);
                }
                Frequency asj = frequencyData.getFrequencyForSource(GNOMAD_E_ASJ);
                if (asj==null) {
                    asj=frequencyData.getFrequencyForSource(GNOMAD_G_ASJ);
                }
                if (asj!=null) {
                    float frequencyAsPercentage = asj.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_ASJ);
                }
                Frequency eas = frequencyData.getFrequencyForSource(GNOMAD_E_EAS);
                if (eas==null) {
                    eas=frequencyData.getFrequencyForSource(GNOMAD_G_EAS);
                }
                if (eas!=null) {
                    float frequencyAsPercentage = eas.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_EAS);
                }
                Frequency fin = frequencyData.getFrequencyForSource(GNOMAD_E_FIN);
                if (fin==null) {
                    fin= frequencyData.getFrequencyForSource(GNOMAD_G_FIN);
                }
                if (fin!=null) {
                    float frequencyAsPercentage = fin.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_FIN);
                }
                Frequency nfe = frequencyData.getFrequencyForSource(GNOMAD_E_NFE);
                if (nfe==null) {
                    nfe = frequencyData.getFrequencyForSource(GNOMAD_G_NFE);
                }
                if (nfe!=null) {
                    float frequencyAsPercentage = nfe.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_NFE);
                }
                Frequency sas = frequencyData.getFrequencyForSource(GNOMAD_E_SAS);
                if (sas==null) {
                    sas= frequencyData.getFrequencyForSource(GNOMAD_G_SAS);
                }
                if (sas!=null) {
                    float frequencyAsPercentage = sas.getFrequency();
                    addToBin(genesymbol, id, frequencyAsPercentage, pathogenicity, GNOMAD_E_SAS);
                }
                if (c++%100_000==0) {
                    System.out.println("Processed variant "+c);
                }
            }
        }
    }

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


    /**
     * Output one line of the background pathogenicity data file for one gene. We write the mean value
     * across the populations listed in {@link #orderedSources}.
     * @param writer A file handle
     * @param genesymbol The symbol of the gene for which we will write the frequency data in this line
     * @throws IOException if there is a problem writing to file
     */
    private void outputLine(BufferedWriter writer, String genesymbol) throws IOException {
        String geneid = this.symbol2idMap.get(genesymbol);
        if (geneid==null || geneid.length()==0) return; // skip genes without valid GeneId
        List<String> values = new ArrayList<>();
        double sum=0;
        for (FrequencySource fs : orderedSources) {
            Optional<Gene2Bin> g2bOpt = this.backgroundMap.get(fs).getGene2Bin(genesymbol);
            if (g2bOpt.isPresent()) {
                double freq = g2bOpt.get().getPathogenicBinFrequency();
                sum += freq;
                values.add(String.valueOf(freq));
            } else {
                values.add("0");
            }
        }
        double mean = sum/values.size();
        String line = String.format("%s\t%s\t%s\t%f\n",
                genesymbol,
                geneid,
                String.join("\t",values),
                mean);
        writer.write(line);
    }


    /**
     * Write the results of our calculations to file
     */
    private void outputBinData() {
        // First arrange all gene symbols in order
        List<String> symbolList = new ArrayList<>(geneSymbolSet);
        Collections.sort(symbolList);
        String header = String.join("\t",headerFields);
        header = String.format("Gene\tEntrezId\t%s\tMean\n",header );
        logger.trace("Outputting background freqeuncy file to " + this.outputFileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.outputFileName))) {
            writer.write(header);
            for (String gsymbol : symbolList) {
                outputLine(writer,gsymbol);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
