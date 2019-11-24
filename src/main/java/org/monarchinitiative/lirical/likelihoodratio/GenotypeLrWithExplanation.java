package org.monarchinitiative.lirical.likelihoodratio;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.poisson.PoissonDistribution;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.monarchinitiative.phenol.formats.hpo.HpoModeOfInheritanceTermIds.*;

public class GenotypeLrWithExplanation  {
    /** The likelihood ratio of the genotype. */
    private final double LR;
    private final String explanation;



    public GenotypeLrWithExplanation(double lratio, String explain){
        this.LR = lratio;
        this.explanation = explain;
    }






    static GenotypeLrWithExplanation noVariantsDetectedAutosomalRecessive(double ratio, String geneSymbol) {
        final String expl = String.format("%s: No variants detected with autosomal recessive disease. log<sub>10</sub>(LR)=%.3f.", geneSymbol,  Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, expl);
    }

    static GenotypeLrWithExplanation noVariantsDetectedAutosomalDominant(double ratio, String geneSymbol) {
        final String expl = String.format("%s: No variants detected. log<sub>10</sub>(LR)=%.3f.", geneSymbol,  Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, expl);
    }

    static GenotypeLrWithExplanation twoPathClinVarAllelesRecessive(double ratio, String geneSymbol) {
        final String expl = String.format("%s: Two pathogenic ClinVar variants detected with autosomal recessive disease. log<sub>10</sub>(LR)=%.3f.", geneSymbol,  Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, expl);
    }

    static GenotypeLrWithExplanation pathClinVar(double ratio, String geneSymbol) {
        final String expl = String.format("%s: Pathogenic ClinVar variant detected. log<sub>10</sub>(LR)=%.3f.", geneSymbol,  Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, expl);
    }

     static   GenotypeLrWithExplanation explainOneAlleleRecessive(double ratio, double observedWeightedPathogenicVariantCount, double lambda_background, String geneSymbol) {
        final int lambda_disease = 2;
        final String expl = String.format("%s: One pathogenic allele detected with autosomal recessive disease. " +
             "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f. log<sub>10</sub>(LR)=%.3f.",
                geneSymbol, observedWeightedPathogenicVariantCount, lambda_disease, lambda_background,  Math.log10(ratio));
         return new GenotypeLrWithExplanation(ratio, expl);
    }


    static GenotypeLrWithExplanation explainPathCountAboveLambdaB(double ratio, Gene2Genotype g2g, TermId MoI,  double lambda_background) {
        double observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
        String geneSymbol = g2g.getSymbol();
        int lambda_disease = 1;
        if (MoI.equals(AUTOSOMAL_RECESSIVE) || MoI.equals(X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String expl = String.format("%s: %s. Heuristic for high number of observed predicted pathogenic variants. "
                        + "Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f. log<sub>10</sub>(LR)=%.3f.",
                geneSymbol, getMoIString(MoI), observedWeightedPathogenicVariantCount, (int) lambda_disease, lambda_background, Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, expl);
    }

    static GenotypeLrWithExplanation explanation(double ratio, Gene2Genotype g2g, TermId modeOfInh, double lambda_b, double D, double B) {
        double observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
        String symbol = g2g.getSymbol();
        int lambda_disease = 1;
        if (modeOfInh.equals(AUTOSOMAL_RECESSIVE) || modeOfInh.equals(X_LINKED_RECESSIVE)) {
            lambda_disease = 2;
        }
        String msg = String.format("%s: P(G|D)=%.4f. P(G|&#172;D)=%.4f", symbol, D, B);
        msg = String.format("%s. %s. Observed weighted pathogenic variant count: %.2f. &lambda;<sub>disease</sub>=%d. &lambda;<sub>background</sub>=%.4f. log<sub>10</sub>(LR)=%.3f",
               msg, getMoIString(modeOfInh), observedWeightedPathogenicVariantCount,  lambda_disease, lambda_b, Math.log10(ratio));
        return new GenotypeLrWithExplanation(ratio, msg);
    }

    private static String getMoIString(TermId MoI) {
        if (MoI.equals(AUTOSOMAL_DOMINANT)) {
            return " Mode of inheritance: autosomal dominant";
        } else if (MoI.equals(AUTOSOMAL_RECESSIVE)) {
            return " Mode of inheritance: autosomal recessive";
        } else if (MoI.equals(X_LINKED_RECESSIVE)) {
            return " Mode of inheritance: X-chromosomal recessive";
        } else if (MoI.equals(X_LINKED_DOMINANT)) {
            return " Mode of inheritance: X-chromosomal recessive";
        }
        return " Mode of inheritance: not available"; // should never happen
    }


    public double getLR() {
        return LR;
    }

    public String getExplanation() {
        return explanation;
    }
}
