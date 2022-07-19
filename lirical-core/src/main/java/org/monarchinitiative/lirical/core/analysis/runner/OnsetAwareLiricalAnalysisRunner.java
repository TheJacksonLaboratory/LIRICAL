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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    protected Optional<OnsetAwareTestResult> calculateTestResult(HpoDisease disease,
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

        double onsetLr = calculateOnsetLR(disease.id(), analysisData.age());

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

    private double calculateOnsetLR(TermId diseaseId, org.monarchinitiative.lirical.core.model.Age age) {
        Age phenolAge = Age.postnatal(age.getYears(), age.getMonths(), age.getDays());
        double presentProba = onsetProbability.diseaseObservableGivenAge(diseaseId, phenolAge);
        double notPresentProba = onsetProbability.diseaseNotObservableGivenAge(diseaseId, phenolAge);
        return presentProba / notPresentProba;
    }
}
