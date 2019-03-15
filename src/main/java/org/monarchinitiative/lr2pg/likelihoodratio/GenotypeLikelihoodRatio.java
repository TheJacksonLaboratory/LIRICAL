package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.poisson.PoissonDistribution;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.monarchinitiative.phenol.formats.hpo.HpoModeOfInheritanceTermIds.*;

/**
 * This class is responsible for calculating the genotype-based likelihood ratio.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GenotypeLikelihoodRatio {
    private static final Logger logger = LogManager.getLogger();

    private static final double DEFAULT_LAMBDA_BACKGROUND=0.1;

    private static final double EPSILON=1e-5;

    /** Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency sum of pathogenic bin variants. */
    private final Map<TermId,Double> gene2backgroundFrequency;
    /** This is a Poisson distribution object that is used to help calculate the genotype likelihood ratio for cases
     * with autosomal recessive inheritance. We can construct this object once and reuse it.*/
    private final PoissonDistribution recessivePoissonDistribution;
    /** This is a Poisson distribution object that is used to help calculate the genotype likelihood ratio for cases
     * with autosomal dominant inheritance. We can construct this object once and reuse it.*/
    private final PoissonDistribution dominantPoissonDistribution;

    /**
     * @param g2background background frequencies of called pathogenic variants in genes.
     */
    public GenotypeLikelihoodRatio(Map <TermId,Double> g2background) {
        this.gene2backgroundFrequency=g2background;
        this.recessivePoissonDistribution =  new PoissonDistribution(2.0);
        this.dominantPoissonDistribution = new PoissonDistribution((1.0));
    }

    /**
     * If no pathogenic variant at all was identified in the gene of interest, we use a heuristic score that
     * intends to represent the probability of missing the variant for technical reasons. We will estimate
     * this probability to be 5%. For autosomal recessive diseases, we will estimate the probability at
     * 5% * 5%.
     * @param inheritancemodes List of all inheritance modes associated with this disease (usually a single one)
     * @return genotype likelihood ratio for situation where no variant at all was found in a gene
     */
    private double getLRifNoVariantAtAllWasIdentified(List<TermId> inheritancemodes) {
        final TermId autosomalRecessiveInheritance = TermId.of("HP:0000007");
        final double ESTIMATED_PROB = 0.05d;
        for (TermId tid : inheritancemodes) {
            if (tid.equals(autosomalRecessiveInheritance)) {
                return ESTIMATED_PROB * ESTIMATED_PROB;
            }
        }
        return ESTIMATED_PROB;
    }


    /**
     * Calculate the genotype likelihood ratio using lambda_disease=1 for autosomal dominant and lambda_disease=2
     * for autosomal recessive.
     * @param g2g  {@link Gene2Genotype} object with list of variants in current gene. Can be null if no variants were found in the gene
     * @param inheritancemodes list of modes of inheritance associated with disease being investigated (usually with just one entry).
     * @param geneId EntrezGene id of the gene we are investigating.
     * @return likelihood ratio of the genotype given the disease/geniId combination
     */
    double evaluateGenotype(Gene2Genotype g2g, List<TermId> inheritancemodes, TermId geneId) {
        double observedWeightedPathogenicVariantCount=0;
        // special case 1: No variant found in this gene
        if (g2g.equals(Gene2Genotype.NO_IDENTIFIED_VARIANT)) {
            double d = getLRifNoVariantAtAllWasIdentified(inheritancemodes);
            return d;
        }
        // special case 2: Clinvar-pathogenic variant(2) found in this gene.
        if (g2g.hasPathogenicClinvarVar()) {
            return Math.pow(1000d, g2g.pathogenicClinVarCount());
        }
        observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
        if (observedWeightedPathogenicVariantCount<EPSILON) {
            // essentially sam as no identified variant, this should happen rarely if ever.
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes);
        }


        if (! g2g.hasPredictedPathogenicVar()) {
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes);
        }
        // if we get here then
        // 1. g2g was not null
        // 2. There was at least one observed variant
        // 3. There was no pathogenic variant listed in ClinVar.
        // Therefore, we apply the main algorithm for calculating the LR genotype score.

        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        if (inheritancemodes==null || inheritancemodes.isEmpty()) {
            // This is probably because the HPO annotation file is incomplete
            logger.warn("No inheritance mode annotation found for geneId {}, reverting to default", geneId.getValue());
            // Add a default dominant mode to avoid not ranking this gene at all
            inheritancemodes = ImmutableList.of(AUTOSOMAL_DOMINANT);
        }
        Optional<Double> max = Optional.empty();
        for (TermId inheritanceId : inheritancemodes) {
            double lambda_disease=1.0;
            PoissonDistribution pdDisease;
            if (inheritanceId.equals(AUTOSOMAL_RECESSIVE) || inheritanceId.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
                pdDisease = recessivePoissonDistribution;
            } else {
                pdDisease = dominantPoissonDistribution;
            }
            // Heuristic for the case where we have more called pathogenic variants than we should have
            // for instance, we have an autosomal recessive disease but there are 5 called pathogenic
            // variants
            if (observedWeightedPathogenicVariantCount>lambda_disease) {
                double D = pdDisease.probability(observedWeightedPathogenicVariantCount);
                PoissonDistribution pdBackground = new PoissonDistribution(observedWeightedPathogenicVariantCount);
                double B = pdBackground.probability(observedWeightedPathogenicVariantCount);
                return D/B;
            }
            double D = pdDisease.probability(observedWeightedPathogenicVariantCount);
            PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
            double B = pdBackground.probability(observedWeightedPathogenicVariantCount);
            if (B>0 && D>0) {
                double ratio=D/B;
                if (max.isPresent() && ratio > max.get()) {
                    max=Optional.of(ratio);
                } else if (!max.isPresent()) {
                    max=Optional.of(ratio);
                }
            }
        }
        // We should always have some value for max once we get here but
        // there is a default value of 0.05^2 to avoid null errors just in case
       return max.orElse(0.05*0.05);
    }





    /** This method is intended to explain the score that is produced by {@link #evaluateGenotype}, and
     * produces a shoprt summary that can be displayed in the org.monarchinitiative.lr2pg.output file. It is intended to be used for the
     * best candidates, i.e., those that will be displayed on the org.monarchinitiative.lr2pg.output page.
     * @param observedPathogenicVarCount number of variants called to ne pathogenic
     * @param inheritancemodes List of all inheritance modes associated with this disease (usually has one element,rarely multiple)
     * @param geneId EntrezGene id of the current gene.
     * @return short summary of the genotype likelihood ratio score.
     */
    String explainGenotypeScore(double observedPathogenicVarCount, List<TermId> inheritancemodes, TermId geneId) {
        StringBuilder sb = new StringBuilder();
        double lambda_disease=1.0;
        if (inheritancemodes!=null && inheritancemodes.size()>0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
            }
            if (tid.equals(AUTOSOMAL_DOMINANT)) {sb.append(" Mode of inheritance: autosomal dominant. "); }
            else if (tid.equals(AUTOSOMAL_RECESSIVE)) {sb.append(" Mode of inheritance: autosomal recessive. "); }
            else if (tid.equals(X_LINKED_RECESSIVE)) {sb.append(" Mode of inheritance: X-chromosomal recessive. "); }
            else if (tid.equals(X_LINKED_DOMINANT)) {sb.append(" Mode of inheritance: X-chromosomal recessive. "); }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        sb.append(String.format("Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f. ",
                observedPathogenicVarCount,(int)lambda_disease,lambda_background));
        double D;
        if (observedPathogenicVarCount<EPSILON) {
            D=0.05; // heuristic--chance of zero variants given this is disease is 5%
        } else {
            PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
            D = pdDisease.probability(observedPathogenicVarCount);
        }
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
        double B = pdBackground.probability(observedPathogenicVarCount);
        sb.append(String.format("P(G|D)=%.4f. P(G|&#172;D)=%.4f",D,B));
        if (B>0 && D>0) {
            double r=Math.log10(D/B);
            sb.append(String.format(". log<sub>10</sub>(LR): %.2f.",r));
        }
        return sb.toString();
    }


}
