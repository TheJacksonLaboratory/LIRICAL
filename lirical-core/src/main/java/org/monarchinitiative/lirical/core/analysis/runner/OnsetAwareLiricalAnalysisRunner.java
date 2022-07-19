package org.monarchinitiative.lirical.core.analysis.runner;


import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.onset.DiseaseOnsetProbability;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An implementation of {@link org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner} that integrates
 * onset of a disease into post-test probability calculation. The onset LR component is calculated
 * using provided {@link DiseaseOnsetProbability}.
 */
public class OnsetAwareLiricalAnalysisRunner extends BaseLiricalAnalysisRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnsetAwareLiricalAnalysisRunner.class);

    private final DiseaseOnsetProbability onsetProbability;

    protected OnsetAwareLiricalAnalysisRunner(PhenotypeService phenotypeService,
                                              PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                              GenotypeLikelihoodRatio genotypeLikelihoodRatio,
                                              DiseaseOnsetProbability onsetProbability) {
        super(phenotypeService, phenotypeLrEvaluator, genotypeLikelihoodRatio);
        this.onsetProbability = Objects.requireNonNull(onsetProbability);
    }

    @Override
    protected List<String> validateAnalysisParameters(AnalysisData data, AnalysisOptions options, List<String> errors) {
        errors = super.validateAnalysisParameters(data, options, errors);

        if (data.age().isEmpty())
            errors.add("Age not provided");

        return errors;
    }

    @Override
    protected Optional<OnsetAwareTestResult> calculateTestResult(HpoDisease disease,
                                                                 AnalysisData analysisData,
                                                                 AnalysisOptions options,
                                                                 Map<TermId, List<Gene2Genotype>> diseaseToGenotype) {
        GenotypeLikelihoodRatioResult genotypeLikelihoodRatioResult = calculateGenotypeLikelihoodRatio(disease, analysisData, options, diseaseToGenotype);
        if (genotypeLikelihoodRatioResult.discardDiffDg())
            return Optional.empty();

        if (analysisData.age().isEmpty())
            // Presence of age should have been checked in `validateAnalysisParameters` method.
            throw new IllegalArgumentException("Age should have been set!");

        Optional<Double> pretestOptional = options.pretestDiseaseProbability().pretestProbability(disease.id());
        if (pretestOptional.isEmpty()) {
            LOGGER.warn("Missing pretest probability for {} ({})", disease.diseaseName(), disease.id());
            return Optional.empty();
        }

        double pretestProbability = pretestOptional.get();

        PhenotypeLikelihoodRatios phenotypeRatios = calculatePhenotypeLikelihoodRatios(disease, analysisData.presentPhenotypeTerms(), analysisData.negatedPhenotypeTerms());

        double onsetLr = calculateOnsetLR(disease.id(), analysisData.age().get());

        return Optional.of(
                new OnsetAwareTestResult(
                        disease.id(),
                        pretestProbability,
                        phenotypeRatios.observed(),
                        phenotypeRatios.excluded(),
                        genotypeLikelihoodRatioResult.bestGenotypeLr(),
                        onsetLr)
        );
    }

    private double calculateOnsetLR(TermId diseaseId, Age age) {
        double observableGivenAge = onsetProbability.diseaseObservableGivenAge(diseaseId, age);
        double notObservableGivenAge = onsetProbability.diseaseNotObservableGivenAge(diseaseId, age);
        return observableGivenAge / notObservableGivenAge;
    }
}
