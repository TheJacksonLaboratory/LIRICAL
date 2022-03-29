package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.lirical.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.likelihoodratio.*;
import org.monarchinitiative.lirical.model.Gene2Genotype;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LiricalAnalysisRunnerImpl implements LiricalAnalysisRunner {

    // An equivalent of CaseEvaluator

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalAnalysisRunnerImpl.class);

    private final PhenotypeService phenotypeService;
    private final PretestDiseaseProbability pretestDiseaseProbability;
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    private final GenotypeLikelihoodRatio genotypeLikelihoodRatio;

    public static LiricalAnalysisRunnerImpl of(PhenotypeService phenotypeService,
                                               PretestDiseaseProbability pretestDiseaseProbability,
                                               PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                               GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        return new LiricalAnalysisRunnerImpl(phenotypeService, pretestDiseaseProbability, phenotypeLrEvaluator, genotypeLikelihoodRatio);
    }

    private LiricalAnalysisRunnerImpl(PhenotypeService phenotypeService,
                                      PretestDiseaseProbability pretestDiseaseProbability,
                                      PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                      GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.pretestDiseaseProbability = Objects.requireNonNull(pretestDiseaseProbability);
        this.phenotypeLrEvaluator = Objects.requireNonNull(phenotypeLrEvaluator);
        this.genotypeLikelihoodRatio = Objects.requireNonNull(genotypeLikelihoodRatio);
    }

    @Override
    public AnalysisResults run(AnalysisData data, AnalysisOptions options) {
        Map<TermId, List<Gene2Genotype>> diseaseToGenotype = groupDiseasesByGene(data.genes());

        List<TestResult> results = phenotypeService.diseases().hpoDiseases().parallel()
                .map(disease -> analyzeDisease(disease, data.sampleId(), data.presentPhenotypeTerms(), data.negatedPhenotypeTerms(), options, diseaseToGenotype))
                .flatMap(Optional::stream)
                .toList();

        return AnalysisResults.of(results);
    }

    private Map<TermId, List<Gene2Genotype>> groupDiseasesByGene(GenesAndGenotypes genes) {
        Map<TermId, Collection<TermId>> geneToDisease = phenotypeService.associationData().geneToDiseases();
        Map<TermId, List<Gene2Genotype>> diseaseToGenotype = new HashMap<>(genes.size());

        for (Gene2Genotype gene : genes) {
            Collection<TermId> diseaseIds = geneToDisease.getOrDefault(gene.id(), List.of());
            for (TermId diseaseId : diseaseIds) {
                diseaseToGenotype.computeIfAbsent(diseaseId, k -> new LinkedList<>())
                        .add(gene);
            }
        }

        return diseaseToGenotype;
    }

    private Optional<TestResult> analyzeDisease(HpoDisease disease,
                                                String sampleId,
                                                List<TermId> observedTerms,
                                                List<TermId> excludedTerms,
                                                AnalysisOptions options,
                                                Map<TermId, List<Gene2Genotype>> diseaseToGenotype) {
        Optional<Double> pretestOptional = pretestDiseaseProbability.pretestProbability(disease.id());
        if (pretestOptional.isEmpty()) {
            LOGGER.warn("Missing pretest probability for {} ({})", disease.getDiseaseName(), disease.id());
            return Optional.empty();
        }
        double pretestProbability = pretestOptional.get();

        List<Gene2Genotype> genotypes = diseaseToGenotype.getOrDefault(disease.id(), List.of());

        InducedDiseaseGraph idg = InducedDiseaseGraph.create(disease, phenotypeService.hpo());
        List<LrWithExplanation> observed = observedPhenotypesLikelihoodRatios(observedTerms, idg);
        List<LrWithExplanation> excluded = excludedPhenotypesLikelihoodRatios(excludedTerms, idg);

        GenotypeLrWithExplanation bestGenotypeLr;
        if (!diseaseToGenotype.isEmpty()) { // We're using the genotype data
            Optional<GenotypeLrWithExplanation> bestGenotype = genotypes.stream()
                    .map(g2g -> genotypeLikelihoodRatio.evaluateGenotype(sampleId, g2g, disease.getModesOfInheritance()))
                    .max(Comparator.comparingDouble(GenotypeLrWithExplanation::lr));
            if (bestGenotype.isEmpty()) {
                // This is a disease with no known disease gene.
                if (options.useGlobal()) {
                    // If useGlobal is true then the user wants to keep differentials with no associated gene.
                    // We create the TestResult based solely on the Phenotype data.
                    bestGenotypeLr = null;
                } else {
                    // If useGlobal is false, we skip this differential because there is no associated gene
                    return Optional.empty();
                }
            } else {
                bestGenotypeLr = bestGenotype.get();
            }
        } else {
            // phenotype only
            bestGenotypeLr = null;
        }

        return Optional.of(TestResult.of(observed, excluded, disease, pretestProbability, bestGenotypeLr));
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
