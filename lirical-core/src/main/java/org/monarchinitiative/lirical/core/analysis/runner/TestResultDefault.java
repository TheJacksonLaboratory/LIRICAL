package org.monarchinitiative.lirical.core.analysis.runner;


import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.stream.DoubleStream;

public class TestResultDefault extends TestResultBase {

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     *
     * @param diseaseId          ID of the disease being tested
     * @param pretestProbability pretest probability of the disease
     * @param observedResults    list of individual test results for observed phenotypes
     * @param excludedResults    list of individual test results for excluded phenotypes
     * @param genotypeLr         LR result for the genotype
     */
    public TestResultDefault(TermId diseaseId,
                             double pretestProbability,
                             List<LrWithExplanation> observedResults,
                             List<LrWithExplanation> excludedResults,
                             GenotypeLrWithExplanation genotypeLr) {
        super(diseaseId, pretestProbability, observedResults, excludedResults, genotypeLr);
    }

    @Override
    protected DoubleStream compositeLikelihoodRatioOperands() {
        // Nothing on top of the likelihood ratio components provided TestResultBase.
        // The composite ratio will be equal to the product of the phenotype LR's
        // multiplied by the genotype LR.
        return observedAndExcludedPhenotypesAndGenotypeLr();
    }

}
