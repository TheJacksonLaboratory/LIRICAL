package org.monarchinitiative.lirical.likelihoodratio;

import com.google.common.collect.ImmutableList;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.poisson.PoissonDistribution;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation.noVariantsDetectedAutosomalRecessive;
import static org.monarchinitiative.phenol.formats.hpo.HpoModeOfInheritanceTermIds.*;

/**
 * This class is responsible for calculating the genotype-based likelihood ratio.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GenotypeLikelihoodRatio {
    private static final Logger logger = LoggerFactory.getLogger(GenotypeLikelihoodRatio.class);
    /* Default frequency of called-pathogenic variants in the general population (gnomAD). In the vast majority of
     * cases, we can derive this information from gnomAD. This constant is used if for whatever reason,
     * data was not available.
     */
    private static final double DEFAULT_LAMBDA_BACKGROUND = 0.1;
    /** A heuristic to downweight an autosomal recessive disease by a factor of 1/10 if we only find one pathogenic allele. */
    final double HEURISTIC_ONE_ALLELE_FOR_AR_DISEASE = 0.10;
    /** A heuristic to downweight an  disease by a factor of 1/10 if the number of predicted pathogenic alleles in
     * the VCF file is above lambda_d. */
    final double HEURISTIC_PATH_ALLELE_COUNT_ABOVE_LAMBDA_D = 0.10;
    /** A small-ish number to avoid dividing by zero. */
    private static final double EPSILON = 1e-5;
    /** Use strict penalties if the genotype does not match the disease model in terms of number of called
     * pathogenic alleles.*/
    private final boolean strict;

    /**
     * Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency (ie.,
     * lambda-background), the sum of pathogenic bin variants in the population (gnomAD).
     */
    private final Map<TermId, Double> gene2backgroundFrequency;
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

    /**
     * @param g2background background frequencies of called pathogenic variants in genes.
     */
    public GenotypeLikelihoodRatio(Map<TermId, Double> g2background) {
        this.gene2backgroundFrequency = g2background;
        this.recessivePoissonDistribution = new PoissonDistribution(2.0);
        this.dominantPoissonDistribution = new PoissonDistribution((1.0));
        this.strict=false;
    }

    /**
     * @param g2background background frequencies of called pathogenic variants in genes.
     * @param str strictness of genotype likelihood ratio (see {@link #strict}).
     */
    public GenotypeLikelihoodRatio(Map<TermId, Double> g2background, boolean str) {
        this.gene2backgroundFrequency = g2background;
        this.recessivePoissonDistribution = new PoissonDistribution(2.0);
        this.dominantPoissonDistribution = new PoissonDistribution((1.0));
        this.strict=str;
    }

    /**
     * If no pathogenic variant at all was identified in the gene of interest, we use a heuristic score that
     * intends to represent the probability of missing the variant for technical reasons. We will estimate
     * this probability to be 5%. For autosomal recessive diseases, we will estimate the probability at
     * 5% * 5%.
     *
     * @param inheritancemodes List of all inheritance modes associated with this disease (usually a single one)
     * @return genotype likelihood ratio for situation where no variant at all was found in a gene
     */
    private GenotypeLrWithExplanation getLRifNoVariantAtAllWasIdentified(List<TermId> inheritancemodes, Gene2Genotype g2g) {
        final TermId autosomalRecessiveInheritance = TermId.of("HP:0000007");
        final double ESTIMATED_PROB = 0.05d;
        for (TermId tid : inheritancemodes) {
            if (tid.equals(autosomalRecessiveInheritance)) {
                return GenotypeLrWithExplanation.noVariantsDetectedAutosomalRecessive(ESTIMATED_PROB * ESTIMATED_PROB, g2g.getSymbol());
            }
        }
        return GenotypeLrWithExplanation.noVariantsDetectedAutosomalDominant(ESTIMATED_PROB, g2g.getSymbol());
    }

    /**
     * Check if the optional has a value already. If not, set it to val. Otherwise, set it to the maximum
     * @param val New value
     * @param opt Optional that may or may not already have a value
     * @return an optional with val or with the max of val and opt.get() if opt has a value
     */
    private Optional<Double> updateMax(double val, Optional<Double> opt) {
        if (!opt.isPresent()) {
            return Optional.of(val);
        } else if (val > opt.get()){
            return Optional.of(val);
        } else {
            return opt;
        }
    }


    /**
     * Calculate the genotype likelihood ratio using lambda_disease=1 for autosomal dominant and lambda_disease=2
     * for autosomal recessive.
     *
     * @param g2g              {@link Gene2Genotype} object with list of variants in current gene. Can be null if no variants were found in the gene
     * @param inheritancemodes list of modes of inheritance associated with disease being investigated (usually with just one entry).
     * @param geneId           EntrezGene id of the gene we are investigating.
     * @return likelihood ratio of the genotype given the disease/geniId combination
     */
    GenotypeLrWithExplanation evaluateGenotype(Gene2Genotype g2g, List<TermId> inheritancemodes, TermId geneId) {
        // special case 1: No variant found in this gene
        if (g2g.equals(Gene2Genotype.NO_IDENTIFIED_VARIANT)) {
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes, g2g);
        }
        // special case 2: Clinvar-pathogenic variant(s) found in this gene.
        // The likelihood ratio is defined as 1000**count, where 1 for autosomal dominant and
        // 2 for autosomal recessive. (If the count of pathogenic alleles does not match
        // the expected count, return 1000.

        if (g2g.hasPathogenicClinvarVar()) {
            int count = g2g.pathogenicClinVarCount();
            if (inheritancemodes.contains(AUTOSOMAL_RECESSIVE)) {
                if (count == 2) {
                    return GenotypeLrWithExplanation.twoPathClinVarAllelesRecessive(Math.pow(1000d, 2), g2g.getSymbol());
                }
            } else { // for all other MoI, including AD, assume that only one ClinVar allele is pathogenic
                return GenotypeLrWithExplanation.pathClinVar(Math.pow(1000d, 1d), g2g.getSymbol());
            }
        }
        double observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
        if (!g2g.hasPredictedPathogenicVar() || observedWeightedPathogenicVariantCount < EPSILON) {
            // no identified variant or the pathogenicity score of identified variant is close to zero
            // essentially sam as no identified variant, this should happen rarely if ever.
            return getLRifNoVariantAtAllWasIdentified(inheritancemodes, g2g);
        }

        // if we get here then
        // 1. g2g was not null
        // 2. There was at least one observed variant
        // 3. There was no pathogenic variant listed in ClinVar.
        // Therefore, we apply the main algorithm for calculating the LR genotype score.

        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        if (inheritancemodes == null || inheritancemodes.isEmpty()) {
            // This is probably because the HPO annotation file is incomplete
            logger.warn("No inheritance mode annotation found for geneId {}, reverting to default", geneId.getValue());
            // Add a default dominant mode to avoid not ranking this gene at all
            inheritancemodes = ImmutableList.of(AUTOSOMAL_DOMINANT);
        }
        // The following is a heuristic to avoid giving genes with a high background count
        // a better score for pathogenic than background -- the best explanation for
        // a gene with high background is that a variant is background (unless variant is ClinVar-path, see above).
        if (lambda_background > 1.0) {
            lambda_background = Math.min(lambda_background, g2g.pathogenicAlleleCount());
        }
        // Use the following four vars to keep track of which option was the max.
        Optional<Double> max = Optional.empty();
        TermId maxInheritanceMode = INHERITANCE_ROOT; // MoI associated with the maximum pathogenicity
        boolean heuristicOneAlleleAR = false;
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


            if (strict && inheritanceId.equals(AUTOSOMAL_RECESSIVE) && g2g.pathogenicAlleleCount() < 2) {
                max = updateMax(HEURISTIC_ONE_ALLELE_FOR_AR_DISEASE, max);
                maxInheritanceMode = inheritanceId;
                heuristicOneAlleleAR = true;
                heuristicPathCountAboveLambda = false;
            } else if (strict && g2g.pathogenicAlleleCount() > (lambda_disease + EPSILON)) {
                double HEURISTIC = HEURISTIC_PATH_ALLELE_COUNT_ABOVE_LAMBDA_D * (g2g.pathogenicAlleleCount() - lambda_disease);
                max = updateMax(HEURISTIC, max);
                maxInheritanceMode = inheritanceId;
                heuristicOneAlleleAR = false;
                heuristicPathCountAboveLambda = true;
            } else { // the following is the general case, where either the variant count
                // matches or we are not using the strict option.
                D = pdDisease.probability(observedWeightedPathogenicVariantCount);
                PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
                B = pdBackground.probability(observedWeightedPathogenicVariantCount);
                if (B > 0 && D > 0) {
                    heuristicOneAlleleAR = false;
                    heuristicPathCountAboveLambda = false;
                    double ratio = D / B;
                    if (max.isPresent() && ratio > max.get()) {
                        max = Optional.of(ratio);
                        maxInheritanceMode = inheritanceId;
                    } else if (!max.isPresent()) {
                        max = Optional.of(ratio);
                        maxInheritanceMode = inheritanceId;
                    }
                }
            }
        }
        // We should always have some value for max once we get here but
        // there is a default value of 0.05 to avoid null errors so that
        // we do not crash if something unexpected occurs. (Should actually never be used)
        final double DEFAULTVAL = 0.05;
        double returnvalue = max.orElse(DEFAULTVAL);
        if (heuristicOneAlleleAR) {
            return GenotypeLrWithExplanation.explainOneAlleleRecessive(returnvalue, observedWeightedPathogenicVariantCount, lambda_background, g2g.getSymbol());
        } else if (heuristicPathCountAboveLambda) {
            return GenotypeLrWithExplanation.explainPathCountAboveLambdaB(returnvalue, g2g, maxInheritanceMode, lambda_background);
        } else {
            return GenotypeLrWithExplanation.explanation(returnvalue, g2g, maxInheritanceMode,lambda_background, B, D);
        }
    }


    /**
     * This method is intended to explain the score that is produced by {@link #evaluateGenotype}, and
     * produces a shoprt summary that can be displayed in the org.monarchinitiative.lirical.output file. It is intended to be used for the
     * best candidates, i.e., those that will be displayed on the org.monarchinitiative.lirical.output page.
     *
     * @param g2g {@link Gene2Genotype} object with variants/genotypes in the current gene.
     * @param inheritancemodes           List of all inheritance modes associated with this disease (usually has one element,rarely multiple)
     * @param geneId                     EntrezGene id of the current gene.
     * @return short summary of the genotype likelihood ratio score.
     */
    String explainGenotypeScore(Gene2Genotype g2g, List<TermId> inheritancemodes, TermId geneId, double logGenotypeLR) {
        double observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();

        StringBuilder sb = new StringBuilder();
        sb.append(g2g.getSymbol()).append(": ");
        double lambda_disease = 1.0;
        if (inheritancemodes != null && inheritancemodes.size() > 0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease = 2.0;
            }
            if (tid.equals(AUTOSOMAL_DOMINANT)) {
                sb.append(" Mode of inheritance: autosomal dominant. ");
            } else if (tid.equals(AUTOSOMAL_RECESSIVE)) {
                sb.append(" Mode of inheritance: autosomal recessive. ");
            } else if (tid.equals(X_LINKED_RECESSIVE)) {
                sb.append(" Mode of inheritance: X-chromosomal recessive. ");
            } else if (tid.equals(X_LINKED_DOMINANT)) {
                sb.append(" Mode of inheritance: X-chromosomal recessive. ");
            }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        sb.append(String.format("Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f. ",
               observedWeightedPathogenicVariantCount, (int) lambda_disease, lambda_background));
        if (g2g.hasPathogenicClinvarVar()) {
            int count = g2g.pathogenicClinVarCount();
            if (inheritancemodes.contains(AUTOSOMAL_RECESSIVE)) {
                if (count == 2) {
                    sb.append(" Genotype score set to LR=10<sup>6</sup> with two ClinGen pathogenic alleles and autosomal recessive mode of inheritance.");
                   return sb.toString();
                }
            } else { // for all other MoI, including AD, assume that only one ClinVar allele is pathogenic
                sb.append(" Genotype score set to LR=10<sup>3</sup> with one ClinGen pathogenic alle.");
                return sb.toString();
            }
        }


        double D;
        if (observedWeightedPathogenicVariantCount < EPSILON) {
            D = 0.05; // heuristic--chance of zero variants given this is disease is 5%
        } else {
            PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
            D = pdDisease.probability(observedWeightedPathogenicVariantCount);
        }
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
        double B = pdBackground.probability(observedWeightedPathogenicVariantCount);
        sb.append(String.format("P(G|D)=%.4f. P(G|&#172;D)=%.4f", D, B));
        if (B > 0 && D > 0) {
            double r = Math.log10(D / B);
            sb.append(String.format(". log<sub>10</sub>(LR): %.2f.", r));
        }
        return sb.toString();
    }


}
