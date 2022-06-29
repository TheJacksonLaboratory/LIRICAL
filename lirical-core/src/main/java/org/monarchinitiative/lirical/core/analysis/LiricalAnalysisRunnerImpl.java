package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.likelihoodratio.*;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LiricalAnalysisRunnerImpl implements LiricalAnalysisRunner {

    // An equivalent of CaseEvaluator

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalAnalysisRunnerImpl.class);

    private final PhenotypeService phenotypeService;
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    private final GenotypeLikelihoodRatio genotypeLikelihoodRatio;

    public static LiricalAnalysisRunnerImpl of(PhenotypeService phenotypeService,
                                               PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                               GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        return new LiricalAnalysisRunnerImpl(phenotypeService, phenotypeLrEvaluator, genotypeLikelihoodRatio);
    }

    private LiricalAnalysisRunnerImpl(PhenotypeService phenotypeService,
                                      PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                      GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.phenotypeLrEvaluator = Objects.requireNonNull(phenotypeLrEvaluator);
        this.genotypeLikelihoodRatio = Objects.requireNonNull(genotypeLikelihoodRatio);
    }

    @Override
    public AnalysisResults run(AnalysisData data, AnalysisOptions options) {
        Map<TermId, List<Gene2Genotype>> diseaseToGenotype = groupDiseasesByGene(data.genes());

        ProgressReporter progressReporter = new ProgressReporter();
        List<TestResult> results = phenotypeService.diseases().hpoDiseases()
                .map(disease -> analyzeDisease(disease, data, options, diseaseToGenotype))
                .flatMap(Optional::stream)
                .peek(d -> progressReporter.log())
                .toList();
        progressReporter.summarize();

        return AnalysisResults.of(results);
    }

    private Map<TermId, List<Gene2Genotype>> groupDiseasesByGene(GenesAndGenotypes genes) {
        Map<TermId, Collection<TermId>> geneToDisease = phenotypeService.associationData().associations().geneIdToDiseaseIds();
        Map<TermId, List<Gene2Genotype>> diseaseToGenotype = new HashMap<>(genes.size());

        for (Gene2Genotype gene : genes) {
            Collection<TermId> diseaseIds = geneToDisease.getOrDefault(gene.geneId().id(), List.of());
            for (TermId diseaseId : diseaseIds) {
                diseaseToGenotype.computeIfAbsent(diseaseId, k -> new LinkedList<>())
                        .add(gene);
            }
        }

        return diseaseToGenotype;
    }

    private Optional<TestResult> analyzeDisease(HpoDisease disease,
                                                AnalysisData analysisData,
                                                AnalysisOptions options,
                                                Map<TermId, List<Gene2Genotype>> diseaseToGenotype) {
        Optional<Double> pretestOptional = options.pretestDiseaseProbability().pretestProbability(disease.id());
        if (pretestOptional.isEmpty()) {
            LOGGER.warn("Missing pretest probability for {} ({})", disease.diseaseName(), disease.id());
            return Optional.empty();
        }
        double pretestProbability = pretestOptional.get();

        List<Gene2Genotype> genotypes = diseaseToGenotype.getOrDefault(disease.id(), List.of());

        InducedDiseaseGraph idg = InducedDiseaseGraph.create(disease, phenotypeService.hpo());
        List<LrWithExplanation> observed = observedPhenotypesLikelihoodRatios(analysisData.presentPhenotypeTerms(), idg);
        List<LrWithExplanation> excluded = excludedPhenotypesLikelihoodRatios(analysisData.negatedPhenotypeTerms(), idg);

        GenotypeLrWithExplanation bestGenotypeLr;
        if (diseaseToGenotype.isEmpty()) {
            // phenotype only
            bestGenotypeLr = null;
        } else { // We're using the genotype data
            Optional<GenotypeLrWithExplanation> bestGenotype = genotypes.stream()
                    .map(g2g -> genotypeLikelihoodRatio.evaluateGenotype(analysisData.sampleId(), g2g, disease.modesOfInheritance()))
                    .max(Comparator.comparingDouble(GenotypeLrWithExplanation::lr));
            if (bestGenotype.isEmpty()) {
                // This is a disease with no known disease gene.
                if (options.useGlobal()) {
                    // If useGlobal is true then the user wants to keep differentials with no associated gene.
                    // We create the TestResult based solely on the Phenotype data below.
                    bestGenotypeLr = null;
                } else {
                    // If useGlobal is false, we skip this differential because there is no associated gene
                    return Optional.empty();
                }
            } else {
                bestGenotypeLr = bestGenotype.get();
            }
        }

        double onsetProbability = 1.;

        return Optional.of(TestResult.of(disease.id(), pretestProbability, observed, excluded, bestGenotypeLr));
    }

    /**
     * p(observed-age-of-investigation given disease)
     * @param disease The candidate disease diagnosis
     * @param age
     * @return
     */
    double onsetProb(HpoDisease disease, Age age) {
        double EPSILON = 1e-8;
        Optional<TemporalInterval> opt = disease.diseaseOnset();
        if (opt.isEmpty()) {
            return 1 - EPSILON;
        }
        TemporalInterval interval = opt.get();
        if (interval.overlapsWith(age)) {
            return 1 - EPSILON;
        } else {
            return EPSILON;
        }
    }

    double notOnsetProba(HpoDiseases diseases, Age age) {
        int M = diseases.size();
        double p = 0d;
        for (HpoDisease disease : diseases) {
            p += onsetProb(disease, age);
        }
        return p / M;
    }

    private List<LrWithExplanation> observedPhenotypesLikelihoodRatios(List<TermId> phenotypes, InducedDiseaseGraph idg) {
        return phenotypes.stream()
                .map(phenotype -> phenotypeLrEvaluator.lrForObservedTerm(phenotype, idg))
                .toList();
    }

    private List<LrWithExplanation> excludedPhenotypesLikelihoodRatios(List<TermId> phenotypes, InducedDiseaseGraph idg) {
        return phenotypes.stream()
                .map(phenotype -> phenotypeLrEvaluator.lrForExcludedTerm(phenotype, idg))
                .toList();
    }
}
