package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.likelihoodratio.*;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
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

        // The GT LR stays `null` if no genotype data is available.
        GenotypeLrWithExplanation bestGenotypeLr = null;
        if (!diseaseToGenotype.isEmpty()) {
            // The variant/genotype data is available for the individual
            boolean noPredictedDeleteriousVariantsWereFound = true;
            for (Gene2Genotype g2g : genotypes) { // Find the gene with the best LR match
                GenotypeLrWithExplanation candidate = genotypeLikelihoodRatio.evaluateGenotype(analysisData.sampleId(), g2g, disease.modesOfInheritance());
                bestGenotypeLr = takeNonNullOrGreaterLr(bestGenotypeLr, candidate);

                if (options.disregardDiseaseWithNoDeleteriousVariants()) {
                    // has at least one pathogenic clinvar variant or predicted pathogenic variant?
                    if (g2g.pathogenicClinVarCount(analysisData.sampleId()) > 0
                            || g2g.pathogenicAlleleCount(analysisData.sampleId(), .8f) > 0) {
                        noPredictedDeleteriousVariantsWereFound = false;
                    }
                }
            }

            if (options.disregardDiseaseWithNoDeleteriousVariants() && noPredictedDeleteriousVariantsWereFound)
                return Optional.empty();

            /*
             At this point, the `bestGenotypeLr` is null iff no gene is associated with a disease.
             If the global mode is on, we keep the differentials with no associated gene. In this case,
             `bestGenotypeLr` stays null, and it's used downstream.

             However, if the global mode is off, we skip the differential diagnosis as there is no known gene associated
             with the disease, and we return an empty optional.
            */
            if (bestGenotypeLr == null && !options.useGlobal())
                return Optional.empty();
        }

        return Optional.of(TestResult.of(disease.id(), pretestProbability, observed, excluded, bestGenotypeLr));
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

    /**
     * Use <code>candidate</code> if <code>base==null</code> or choose the {@link GenotypeLrWithExplanation}
     * with greater {@link GenotypeLrWithExplanation#lr()} value.
     */
    private static GenotypeLrWithExplanation takeNonNullOrGreaterLr(GenotypeLrWithExplanation base,
                                                                    GenotypeLrWithExplanation candidate) {
        return base == null
                ? candidate
                : base.lr() > candidate.lr()
                ? base
                : candidate;
    }
}
