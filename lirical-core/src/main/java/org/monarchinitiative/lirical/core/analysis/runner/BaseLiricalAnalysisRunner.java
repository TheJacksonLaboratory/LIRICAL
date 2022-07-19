package org.monarchinitiative.lirical.core.analysis.runner;

import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.likelihoodratio.*;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

/**
 * Base class for {@link LiricalAnalysisRunner} that handles calculating {@link TestResult} for
 * all known diseases one disease at a time using
 * {@link #calculateTestResult(HpoDisease, AnalysisData, AnalysisOptions, Map)} method.
 * <p>
 * The base class defines a bunch of useful methods and records that are possibly shareable between different
 * child classes.
 */
abstract class BaseLiricalAnalysisRunner implements LiricalAnalysisRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLiricalAnalysisRunner.class);

    protected final PhenotypeService phenotypeService;
    protected final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    protected final GenotypeLikelihoodRatio genotypeLikelihoodRatio;
    private final ForkJoinPool liricalWorkerPool;

    protected BaseLiricalAnalysisRunner(PhenotypeService phenotypeService,
                                        PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                                        GenotypeLikelihoodRatio genotypeLikelihoodRatio) {
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.phenotypeLrEvaluator = Objects.requireNonNull(phenotypeLrEvaluator);
        this.genotypeLikelihoodRatio = Objects.requireNonNull(genotypeLikelihoodRatio);
        int parallelism = Runtime.getRuntime().availableProcessors();
        LOGGER.debug("Creating LIRICAL pool with {} workers.", parallelism);
        this.liricalWorkerPool = new ForkJoinPool(parallelism, LiricalWorkerThread::new, null, false);
    }

    @Override
    public AnalysisResults run(AnalysisData data, AnalysisOptions options) {
        Map<TermId, List<Gene2Genotype>> diseaseToGenotype = groupGenesByDisease(data.genes());

        ProgressReporter progressReporter = new ProgressReporter(1_000, "diseases");
        Stream<TestResult> testResultStream = phenotypeService.diseases().hpoDiseases()
                .parallel() // why not?
                .peek(d -> progressReporter.log())
                .map(disease -> calculateTestResult(disease, data, options, diseaseToGenotype))
                .flatMap(Optional::stream);

        try {
            List<TestResult> results = liricalWorkerPool.submit(testResultStream::toList).get();
            progressReporter.summarize();
            return AnalysisResults.of(results);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
            return AnalysisResults.empty();
        }
    }

    private Map<TermId, List<Gene2Genotype>> groupGenesByDisease(GenesAndGenotypes genes) {
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

    protected abstract Optional<? extends TestResult> calculateTestResult(HpoDisease disease,
                                                       AnalysisData analysisData,
                                                       AnalysisOptions options,
                                                       Map<TermId, List<Gene2Genotype>> diseaseToGenotype);

    protected PhenotypeLikelihoodRatios calculatePhenotypeLikelihoodRatios(HpoDisease disease,
                                                                           List<TermId> present,
                                                                           List<TermId> negated) {
        InducedDiseaseGraph idg = InducedDiseaseGraph.create(disease, phenotypeService.hpo());
        List<LrWithExplanation> observed = observedPhenotypesLikelihoodRatios(present, idg);
        List<LrWithExplanation> excluded = excludedPhenotypesLikelihoodRatios(negated, idg);
        return new PhenotypeLikelihoodRatios(observed, excluded);
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

    protected GenotypeLikelihoodRatioResult calculateGenotypeLikelihoodRatio(HpoDisease disease,
                                                                             AnalysisData analysisData,
                                                                             AnalysisOptions options, Map<TermId, List<Gene2Genotype>> diseaseToGenotype) {
        // The GT LR stays `null` if no genotype data is available.
        GenotypeLrWithExplanation bestGenotypeLr = null;
        if (!diseaseToGenotype.isEmpty()) {
            List<Gene2Genotype> genotypes = diseaseToGenotype.getOrDefault(disease.id(), List.of());

            // The variant/genotype data is available for the individual
            boolean noPredictedDeleteriousVariantsWereFound = true;
            for (Gene2Genotype g2g : genotypes) { // Find the gene with the best LR match
                GenotypeLrWithExplanation candidate = genotypeLikelihoodRatio.evaluateGenotype(analysisData.sampleId(), g2g, disease.modesOfInheritance());
                bestGenotypeLr = takeNonNullOrGreaterLr(bestGenotypeLr, candidate);

                if (options.disregardDiseaseWithNoDeleteriousVariants()) {
                    // has at least one pathogenic clinvar variant or predicted pathogenic variant?
                    if (g2g.pathogenicClinVarCount(analysisData.sampleId()) > 0
                            || g2g.pathogenicAlleleCount(analysisData.sampleId(), options.pathogenicityThreshold()) > 0) {
                        noPredictedDeleteriousVariantsWereFound = false;
                    }
                }
            }

            if (options.disregardDiseaseWithNoDeleteriousVariants() && noPredictedDeleteriousVariantsWereFound)
                return GenotypeLikelihoodRatioResult.NA;

            /*
             At this point, the `bestGenotypeLr` is null iff no gene is associated with a disease.
             If the global mode is on, we keep the differentials with no associated gene. In this case,
             `bestGenotypeLr` stays null, and it's used downstream.

             However, if the global mode is off, we skip the differential diagnosis as there is no known gene associated
             with the disease, and we return an empty optional.
            */
            if (bestGenotypeLr == null && !options.useGlobal())
                return GenotypeLikelihoodRatioResult.NA;
        }
        return new GenotypeLikelihoodRatioResult(false, bestGenotypeLr);
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

    protected record PhenotypeLikelihoodRatios(List<LrWithExplanation> observed, List<LrWithExplanation> excluded) {
    }

    protected record GenotypeLikelihoodRatioResult(boolean discardDiffDg, GenotypeLrWithExplanation bestGenotypeLr) {
        private static final GenotypeLikelihoodRatioResult NA = new GenotypeLikelihoodRatioResult(true, null);
    }
}
