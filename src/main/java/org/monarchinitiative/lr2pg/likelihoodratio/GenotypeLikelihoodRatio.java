package org.monarchinitiative.lr2pg.likelihoodratio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.poisson.PoissonDistribution;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class is responsible for calculate the genotype-based likelihood ratio.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GenotypeLikelihoodRatio {
    private static final Logger logger = LogManager.getLogger();



    /** {@link TermId} for "X-linked recessive inheritance. */
    private static final TermId X_LINKED_RECESSIVE = TermId.constructWithPrefix("HP:0001419");
    /** {@link TermId} for "autosomal recessive inheritance. */
    private static final TermId AUTOSOMAL_RECESSIVE = TermId.constructWithPrefix("HP:0000007");
    /** {@link TermId} for "autosomal dominant inheritance. */
    private static final TermId AUTOSOMAL_DOMINANT = TermId.constructWithPrefix("HP:0000006");
    /** Default value for background for genes for which we have no information. */
    private static final double DEFAULT_LAMBDA_BACKGROUND=0.1;

    private static final double EPSILON=1e-5;

    /** Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency sum of pathogenic bin variants. */
    private final Map<TermId,Double> gene2backgroundFrequency;


    public GenotypeLikelihoodRatio(Map <TermId,Double> g2background) {
        this.gene2backgroundFrequency=g2background;
    }


    /**
     * Calculate the genotypoe likelihood ratio using lambda_disease=1 for autosomal dominant and lambda_disease=2
     * for autosomal recessive. TODO figure out other MOIs
     * @param observedPathogenicVarCount weighted number of observed pathogenic variants in this gene
     * @param inheritancemodes list of modes of inheritance associated with disease being investigated (usually with just one entry).
     * @param geneId EntrezGene id of the gene we are investigating.
     * @return likelihood ratio of the genotype given the disease/geniId combination
     */
    Optional<Double> evaluateGenotype(double observedPathogenicVarCount, List<TermId> inheritancemodes, TermId geneId) {
        double lambda_disease=1.0;
        if (inheritancemodes!=null && inheritancemodes.size()>0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
            }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        if (! this.gene2backgroundFrequency.containsKey(geneId)) {
            return Optional.empty();
        }
        Double D;
        if (observedPathogenicVarCount<EPSILON) {
            D=0.05; // heuristic--chance of zero variants given this is disease is 5%
        } else {
            PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
            D = pdDisease.probability(observedPathogenicVarCount);
        }
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
        double B = pdBackground.probability(observedPathogenicVarCount);
        if (B>0 && D>0) {
            return Optional.of(D/B);
        } else {
            return Optional.empty();
        }
    }


}
