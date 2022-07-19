package org.monarchinitiative.lirical.core.analysis.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.monarchinitiative.lirical.core.TestResources;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.onset.DiseaseOnsetProbability;
import org.monarchinitiative.lirical.core.analysis.onset.proba.SimpleDiseaseOnsetProbability;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

public class OnsetAwareLiricalAnalysisRunnerTest {

    private static final HpoDiseases DISEASES = TestResources.hpoDiseases();
    private static final Ontology HPO = TestResources.hpo();
    private static final HpoAssociationData ASSOCIATION_DATA = TestResources.hpoAssociationData();

    private static final TermId BOBOPHOBIA_A = TermId.of("OMIM:100000");
    private static final TermId BOBOPHOBIA_B = TermId.of("OMIM:200000");
    private static final double ERROR = 5E-8;

    private final PhenotypeService phenotypeService = PhenotypeService.of(HPO, DISEASES, ASSOCIATION_DATA);
    private final PhenotypeLikelihoodRatio phenotypeLikelihoodRatio = new PhenotypeLikelihoodRatio(HPO, DISEASES);
    private final GenotypeLikelihoodRatio genotypeLikelihoodRatio = Mockito.mock(GenotypeLikelihoodRatio.class);
    private final DiseaseOnsetProbability onsetProbability = new SimpleDiseaseOnsetProbability(DISEASES, false);

    private OnsetAwareLiricalAnalysisRunner instance;

    @BeforeEach
    public void setUp() {
        instance = new OnsetAwareLiricalAnalysisRunner(phenotypeService, phenotypeLikelihoodRatio, genotypeLikelihoodRatio, onsetProbability);
    }

    @ParameterizedTest
    @CsvSource({
            "P10D,    1.25E-8,  6.00961557E-9",
            "P1Y,     1.0,      0.32467533",
    })
    public void calculateTestResult_bobophobiaA(Period age, double onsetLr, double posttestProbability) {
        HpoDisease disease = DISEASES.diseaseById(BOBOPHOBIA_A).orElseThrow();
        List<TermId> presentPhenotypeTerms = prepareTermIds("HP:0000510", "HP:0000486");
        List<TermId> negatedPhenotypeTerms = List.of();
        AnalysisData analysisData = prepareAnalysisData(Age.parse(age), presentPhenotypeTerms, negatedPhenotypeTerms);
        AnalysisOptions analysisOptions = prepareAnalysisOptions();
        Map<TermId, List<Gene2Genotype>> gtMap = Map.of();

        Optional<OnsetAwareTestResult> ro = instance.calculateTestResult(disease, analysisData, analysisOptions, gtMap);

        assertThat(ro.isPresent(), equalTo(true));
        OnsetAwareTestResult result = ro.get();

        assertThat(result.onsetLr(), closeTo(onsetLr, ERROR));
        assertThat(result.posttestProbability(), closeTo(posttestProbability, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "P28D,    1.25,   0.80025608",
            "P29D,    1.00,   0.76219512",
    })
    public void calculateTestResult_bobophobiaB(Period age, double onsetLr, double posttestProbability) {
        HpoDisease disease = DISEASES.diseaseById(BOBOPHOBIA_B).orElseThrow();
        List<TermId> presentPhenotypeTerms = prepareTermIds("HP:0000510", "HP:0000486");
        List<TermId> negatedPhenotypeTerms = List.of();
        AnalysisData analysisData = prepareAnalysisData(Age.parse(age), presentPhenotypeTerms, negatedPhenotypeTerms);
        AnalysisOptions analysisOptions = prepareAnalysisOptions();
        Map<TermId, List<Gene2Genotype>> gtMap = Map.of();

        Optional<OnsetAwareTestResult> ro = instance.calculateTestResult(disease, analysisData, analysisOptions, gtMap);

        assertThat(ro.isPresent(), equalTo(true));
        OnsetAwareTestResult result = ro.get();

        assertThat(result.onsetLr(), closeTo(onsetLr, ERROR));
        assertThat(result.posttestProbability(), closeTo(posttestProbability, ERROR));
    }

    private static List<TermId> prepareTermIds(String... termIds) {
        return Arrays.stream(termIds)
                .map(TermId::of)
                .collect(Collectors.toCollection(() -> new ArrayList<>(termIds.length)));
    }

    private static AnalysisData prepareAnalysisData(Age age, List<TermId> presentPhenotypeTerms, List<TermId> negatedPhenotypeTerms) {
        return AnalysisData.of("Sample", age, Sex.UNKNOWN, presentPhenotypeTerms, negatedPhenotypeTerms, GenesAndGenotypes.empty());
    }

    private static AnalysisOptions prepareAnalysisOptions() {
        // Variants with greater deleteriousness than .8 are considered deleterious.
        float pathogenicityThreshold = .8f;
        return AnalysisOptions.of(true, PretestDiseaseProbabilities.uniform(DISEASES), true, pathogenicityThreshold);
    }
}