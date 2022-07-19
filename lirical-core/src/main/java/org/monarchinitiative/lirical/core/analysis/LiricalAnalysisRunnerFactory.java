package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.onset.DiseaseOnsetProbability;
import org.monarchinitiative.lirical.core.analysis.onset.proba.SimpleDiseaseOnsetProbability;
import org.monarchinitiative.lirical.core.analysis.runner.DefaultLiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.runner.OnsetAwareLiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiricalAnalysisRunnerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalAnalysisRunnerFactory.class);

    private final PhenotypeService phenotypeService;
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    private final GenotypeLikelihoodRatio genotypeLikelihoodRatio;

    public LiricalAnalysisRunnerFactory(PhenotypeService phenotypeService,
                                        PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                        GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        this.phenotypeService = phenotypeService;
        this.phenotypeLrEvaluator = phenotypeLrEvaluator;
        this.genotypeLikelihoodRatio = genotypeLikelihoodRatio;
    }

    public LiricalAnalysisRunner getRunner(AnalysisRunnerOptions options) {
        return switch (options.runnerType()) {
            case DEFAULT -> {
                LOGGER.info("Using default LIRICAL analysis runner");
                yield DefaultLiricalAnalysisRunner.of(phenotypeService, phenotypeLrEvaluator, genotypeLikelihoodRatio);
            }
            case ONSET -> {
                DiseaseOnsetProbability onsetProbability = new SimpleDiseaseOnsetProbability(phenotypeService.diseases(), options.onsetAwareRunnerOptions().strict());
                LOGGER.info("Using onset aware LIRICAL analysis runner parametrized by {}", options.onsetAwareRunnerOptions());
                yield new OnsetAwareLiricalAnalysisRunner(phenotypeService,
                        phenotypeLrEvaluator,
                        genotypeLikelihoodRatio,
                        onsetProbability);
            }
        };
    }

}
