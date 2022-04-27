package org.monarchinitiative.lirical.core.likelihoodratio;

import org.monarchinitiative.lirical.core.likelihoodratio.poisson.PoissonDistribution;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.monarchinitiative.phenol.constants.hpo.HpoModeOfInheritanceTermIds.*;


/**
 * This class is responsible for calculating the genotype-based likelihood ratio.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
// TODO - needs to become an interface
public class GenotypeLikelihoodRatio {
    private static final Logger logger = LoggerFactory.getLogger(GenotypeLikelihoodRatio.class);
    /** A heuristic to downweight an  disease by a factor of 1/10 if the number of predicted pathogenic alleles in
     * the VCF file is above lambda_d. */
    final double HEURISTIC_PATH_ALLELE_COUNT_ABOVE_LAMBDA_D = 0.10;
    private static final double DEFAULT_GLR = 0.05;
    /** A small-ish number to avoid dividing by zero. */
    private static final double EPSILON = 1e-5;

    /**
     * Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency (ie.,
     * lambda-background), the sum of pathogenic bin variants in the population (gnomAD).
     */
    private final BackgroundVariantFrequencyService backgroundVariantFrequencyService;
    /**
     * This is a Poisson distribution object that is used to help calculate the genotype likelihood ratio for cases
     * with autosomal recessive inheritance. We can construct this object once and reuse it. This is
     * lambda-disease with lambda=2
     */
    private final PoissonDistribution recessivePoissonDistribution;
    /**
     * This is a Poisson distribution object that is used to help calculate the genotype likelihood ratio for cases
     * with autosomal dominant inheritance. We can construct this object once and reuse it. This is
     * lambda-disease with lambda=1
     */
    private final PoissonDistribution dominantPoissonDistribution;

    /** Use strict penalties if the genotype does not match the disease model in terms of number of called
     * pathogenic alleles.*/
    private final boolean strict;
    /**
     * Variant with pathogenicity greater than this threshold is considered deleterious.
     */
    private final float pathogenicityThreshold;

    /**
     * @param options genotype LR options
     */
    public GenotypeLikelihoodRatio(BackgroundVariantFrequencyService backgroundVariantFrequencyService, Options options) {
        this.backgroundVariantFrequencyService = backgroundVariantFrequencyService;
        Objects.requireNonNull(options);
        this.recessivePoissonDistribution = new PoissonDistribution(2.0);
        this.dominantPoissonDistribution = new PoissonDistribution(1.0);
        this.strict = options.strict;
        this.pathogenicityThreshold = options.pathogenicityThreshold;
    }

    /**
     * If no pathogenic variant at all was identified in the gene of interest, we use a heuristic score that
     * intends to represent the probability of missing the variant for technical reasons. We will estimate
     * this probability to be 5%. For autosomal recessive diseases, we will estimate the probability at
     * 5% * 5%.
     *
     * @param inheritanceModes List of all inheritance modes associated with this disease (usually a single one)
     * @return genotype likelihood ratio for situation where no variant at all was found in a gene
     */
    private static GenotypeLrWithExplanation getLRifNoVariantAtAllWasIdentified(Collection<TermId> inheritanceModes, Gene2Genotype g2g) {
        if (inheritanceModes.stream().anyMatch(tid -> tid.equals(AUTOSOMAL_RECESSIVE)))
            // compatible with autosomal recessive inheritance
            return GenotypeLrWithExplanation.noVariantsDetectedAutosomalRecessive(g2g.geneId(), DEFAULT_GLR * DEFAULT_GLR);
        else
            return GenotypeLrWithExplanation.noVariantsDetectedAutosomalDominant(g2g.geneId(), DEFAULT_GLR);
    }

    /**
     * Check if the optional has a value already. If not, set it to val. Otherwise, set it to the maximum
     * @param left New value
     * @param right Optional that may or may not already have a value
     * @return an optional with val or with the max of val and opt.get() if opt has a value
     */
    private double updateMax(double left, Double right) {
        if (right == null) {
            return left;
        } else {
            return Math.max(left, right);
        }
    }


    /**
     * Calculate the genotype likelihood ratio using lambda_disease=1 for autosomal dominant and lambda_disease=2
     * for autosomal recessive.
     *
     * @param g2g              {@link Gene2Genotype} object with list of variants in current gene. Can be null if no variants were found in the gene
     * @param inheritancemodes list of modes of inheritance associated with disease being investigated (usually with just one entry).
     * @return likelihood ratio of the genotype given the disease/geniId combination
     */
    public GenotypeLrWithExplanation evaluateGenotype(String sampleId, Gene2Genotype g2g, List<TermId> inheritancemodes) {
        // special case 1: No variant found in this gene
        if (!g2g.hasVariants()) {
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes, g2g);
        }
        // special case 2: Clinvar-pathogenic variant(s) found in this gene.
        // The likelihood ratio is defined as 1000**count, where 1 for autosomal dominant and
        // 2 for autosomal recessive. (If the count of pathogenic alleles does not match
        // the expected count, return 1000.

        int pathogenicClinVarAlleleCount = g2g.pathogenicClinVarCount(sampleId);
        if (pathogenicClinVarAlleleCount > 0) {
            if (inheritancemodes.contains(AUTOSOMAL_RECESSIVE)) {
                if (pathogenicClinVarAlleleCount == 2) {
                    return GenotypeLrWithExplanation.twoPathClinVarAllelesRecessive(g2g.geneId(),Math.pow(1000d, 2));
                }
            } else { // for all other MoI, including AD, assume that only one ClinVar allele is pathogenic
                return GenotypeLrWithExplanation.pathClinVar(g2g.geneId(), Math.pow(1000d, 1d));
            }
        }
        int pathogenicAlleleCount = g2g.pathogenicAlleleCount(sampleId, pathogenicityThreshold);
        double observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores(sampleId, pathogenicityThreshold);
        if (pathogenicAlleleCount == 0 || observedWeightedPathogenicVariantCount < EPSILON) {
            // no identified variant or the pathogenicity score of identified variant is close to zero
            // essentially same as no identified variant, this should happen rarely if ever.
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes, g2g);
        }

        // if we get here then
        // 1. g2g was not null
        // 2. There was at least one observed variant
        // 3. There was no pathogenic variant listed in ClinVar.
        // Therefore, we apply the main algorithm for calculating the LR genotype score.

        double lambda_background = backgroundVariantFrequencyService.frequencyForGene(g2g.geneId().id())
                .orElse(backgroundVariantFrequencyService.defaultVariantFrequency());
        if (inheritancemodes == null || inheritancemodes.isEmpty()) {
            // This is probably because the HPO annotation file is incomplete
            logger.warn("No inheritance mode annotation found for geneId {}, reverting to default", g2g.geneId().id().getValue());
            // Add a default dominant mode to avoid not ranking this gene at all
            inheritancemodes = List.of(AUTOSOMAL_DOMINANT);
        }
        // The following is a heuristic to avoid giving genes with a high background count
        // a better score for pathogenic than background -- the best explanation for
        // a gene with high background is that a variant is background (unless variant is ClinVar-path, see above).
        if (lambda_background > 1.0) {
            lambda_background = Math.min(lambda_background, pathogenicAlleleCount);
        }
        // Use the following four vars to keep track of which option was the max.
        Double max = null;
        TermId maxInheritanceMode = INHERITANCE_ROOT; // MoI associated with the maximum pathogenicity
        boolean heuristicPathCountAboveLambda = false;
        // If these variables are used, they will be specifically initialized.
        // we start them off at 1.0/1.0, which would lead to a zero-effect likelihood ratio of 1
        // if for whatever reason they are not set, which should never happen if we get to the
        //last if/else
        double B = 1.0; // background
        double D = 1.0; // disease
        for (TermId inheritanceId : inheritancemodes) {
            double lambda_disease = 1.0;
            PoissonDistribution pdDisease;
            if (inheritanceId.equals(AUTOSOMAL_RECESSIVE) || inheritanceId.equals(X_LINKED_RECESSIVE)) {
                lambda_disease = 2.0;
                pdDisease = recessivePoissonDistribution;
            } else {
                pdDisease = dominantPoissonDistribution;
            }
            // Heuristic for the case where we have more called pathogenic variants than we should have
            // in a gene without a high background count -- we will model this as technical error and
            // will take the observed path weighted count to not be more than lambda_disease.
            // this will have the effect of not downweighting these genes
            // the user will have to judge whether one of the variants is truly pathogenic.
            if (strict && pathogenicAlleleCount > (lambda_disease + EPSILON)) {
                double HEURISTIC = HEURISTIC_PATH_ALLELE_COUNT_ABOVE_LAMBDA_D * (pathogenicAlleleCount - lambda_disease);
                max = updateMax(HEURISTIC, max);
                maxInheritanceMode = inheritanceId;
                heuristicPathCountAboveLambda = true;
            } else { // the following is the general case, where either the variant count
                // matches or we are not using the strict option.
                D = pdDisease.probability(observedWeightedPathogenicVariantCount);
                PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
                B = pdBackground.probability(observedWeightedPathogenicVariantCount);
                if (B > 0 && D > 0) {
                    double ratio = D / B;
                    if (max != null && ratio > max) {
                        max = ratio;
                        maxInheritanceMode = inheritanceId;
                        heuristicPathCountAboveLambda = false;
                    } else if (max == null) {
                        max = ratio;
                        maxInheritanceMode = inheritanceId;
                        heuristicPathCountAboveLambda = false;
                    }
                }
            }
        }
        // We should always have some value for max once we get here but
        // there is a default value of 0.05 to avoid null errors so that
        // we do not crash if something unexpected occurs. (Should actually never be used)
        double returnvalue = max == null ? DEFAULT_GLR : max;
        if (heuristicPathCountAboveLambda) {
            return GenotypeLrWithExplanation.explainPathCountAboveLambdaB(g2g.geneId(), returnvalue, maxInheritanceMode, lambda_background, observedWeightedPathogenicVariantCount);
        } else {
            return GenotypeLrWithExplanation.explanation(g2g.geneId(), returnvalue, maxInheritanceMode,lambda_background, B, D, observedWeightedPathogenicVariantCount);
        }
    }

    public static class Options {
        private final float pathogenicityThreshold;
        private final boolean strict;

        public Options(float pathogenicityThreshold, boolean strict) {
            this.pathogenicityThreshold = pathogenicityThreshold;
            this.strict = strict;
        }

    }
}
