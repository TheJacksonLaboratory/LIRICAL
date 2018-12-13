package org.monarchinitiative.lr2pg.likelihoodratio;

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


    public GenotypeLikelihoodRatio(Map <TermId,Double> g2background) {
        this.gene2backgroundFrequency=g2background;
    }


    /**
     * Calculate the genotype likelihood ratio using lambda_disease=1 for autosomal dominant and lambda_disease=2
     * for autosomal recessive. TODO figure out other MOIs TODO do not allow g2g to be null, refactor
     * @param g2g  {@link Gene2Genotype} object with list of variants in current gene. Can be null if no variants were found in the gene
     * @param inheritancemodes list of modes of inheritance associated with disease being investigated (usually with just one entry).
     * @param geneId EntrezGene id of the gene we are investigating.
     * @return likelihood ratio of the genotype given the disease/geniId combination
     */
    Optional<Double> evaluateGenotype(Gene2Genotype g2g, List<TermId> inheritancemodes, TermId geneId) {
        double observedWeightedPathogenicVariantCount=0;
        if (g2g!=null) {
            observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
            if (g2g.hasPathogenicClinvarVar()) {
                return Optional.of(Math.pow(1000d, g2g.pathogenicClinVarCount()));
            }
        }
        double lambda_disease=1.0;
        if (inheritancemodes!=null && inheritancemodes.size()>0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
            }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        double D;
        if (observedWeightedPathogenicVariantCount<EPSILON) {
            D=0.05; // heuristic--chance of zero variants given this is disease is 5%
        } else {
            PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
            D = pdDisease.probability(observedWeightedPathogenicVariantCount);
        }
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
        double B = pdBackground.probability(observedWeightedPathogenicVariantCount);
        if (B>0 && D>0) {
            return Optional.of(D/B);
        } else {
            return Optional.empty();
        }
    }

    /** This method is intended to explain the score that is produced by {@link #evaluateGenotype}, and
     * produces a shoprt summary that can be displayed in the output file. It is intended to be used for the
     * best candidates, i.e., those that will be displayed on the output page.
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
