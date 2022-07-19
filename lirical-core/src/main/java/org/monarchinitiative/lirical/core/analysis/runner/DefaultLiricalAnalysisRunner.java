package org.monarchinitiative.lirical.core.analysis.runner;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.*;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default analysis runner implementation that integrates present and absent phenotype term LRs, and genotype LR
 * into the final post-test probability.
 */
public class DefaultLiricalAnalysisRunner extends BaseLiricalAnalysisRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLiricalAnalysisRunner.class);

    public static DefaultLiricalAnalysisRunner of(PhenotypeService phenotypeService,
                                                  PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                                  GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        return new DefaultLiricalAnalysisRunner(phenotypeService, phenotypeLrEvaluator, genotypeLikelihoodRatio);
    }

    private DefaultLiricalAnalysisRunner(PhenotypeService phenotypeService,
                                         PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                         GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        super(phenotypeService, phenotypeLrEvaluator, genotypeLikelihoodRatio);
    }

    @Override
    protected Optional<TestResult> calculateTestResult(HpoDisease disease,
                                                       AnalysisData analysisData,
                                                       AnalysisOptions options,
                                                       Map<TermId, List<Gene2Genotype>> diseaseToGenotype) {
        GenotypeLikelihoodRatioResult genotypeLikelihoodRatioResult = calculateGenotypeLikelihoodRatio(disease, analysisData, options, diseaseToGenotype);
        if (genotypeLikelihoodRatioResult.discardDiffDg())
            return Optional.empty();

        Optional<Double> pretestOptional = options.pretestDiseaseProbability().pretestProbability(disease.id());
        if (pretestOptional.isEmpty()) {
            LOGGER.warn("Missing pretest probability for {} ({})", disease.diseaseName(), disease.id());
            return Optional.empty();
        }

        double pretestProbability = pretestOptional.get();

        PhenotypeLikelihoodRatios phenotypeRatios = calculatePhenotypeLikelihoodRatios(disease, analysisData.presentPhenotypeTerms(), analysisData.negatedPhenotypeTerms());

        return Optional.of(
                new TestResultDefault(
                        disease.id(),
                        pretestProbability,
                        phenotypeRatios.observed(),
                        phenotypeRatios.excluded(),
                        genotypeLikelihoodRatioResult.bestGenotypeLr()
                )
        );
    }

}
