package org.monarchinitiative.lirical.io.output.svg;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.LrMatchType;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanationFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


public class Posttest2SvgTest {

    private static LrWithExplanationFactory FACTORY;

    private static List<HpoDisease> DISEASES;
    private static AnalysisResults RESULTS;

    public static MinimalOntology HPO;

    @BeforeAll
    public static void setup() throws NullPointerException {
        TermId some = TermId.of("HP:0000006");

        HPO = Mockito.mock(MinimalOntology.class);
        when(HPO.termForTermId(some)).thenReturn(Optional.of(Term.of(some, "Some term")));

        double PRETEST_PROB = 0.001;
        FACTORY = new LrWithExplanationFactory(HPO);
        List<TestResult> results = new ArrayList<>();
        //result 1

        List<LrWithExplanation> reslist = createTestList(some, 1d, 10d, 100d);
        List<LrWithExplanation> excluded = List.of();
        List<HpoDiseaseAnnotation> annotations = List.of();
        List<TermId> modesOfInheritance = List.of();
        HpoDisease d1 = HpoDisease.of(TermId.of("MONDO:1"), "DISEASE 1",null, annotations, modesOfInheritance);
        TestResult result1 = TestResult.of(d1.id(), PRETEST_PROB, reslist,excluded, null);
        List<LrWithExplanation> reslist2 = createTestList(some, 10d,100d,1000d);
        HpoDisease d2 = HpoDisease.of(TermId.of("MONDO:2"), "DISEASE 2", null, annotations,modesOfInheritance);
        TestResult result2 = TestResult.of(d2.id(), PRETEST_PROB, reslist2,excluded, null);
        List<LrWithExplanation> reslist3 = createTestList(some, 1d,0.1d,0.01d);
        HpoDisease d3 = HpoDisease.of(TermId.of("MONDO:3"), "DISEASE3", null, annotations,modesOfInheritance);
        TestResult result3 = TestResult.of(d3.id(), PRETEST_PROB, reslist3,excluded, null);
        results.add(result1);
        results.add(result2);
        results.add(result3);
        DISEASES = List.of(d1, d2, d3);
        RESULTS = AnalysisResults.of(results);
    }



    @Test
    public void testConstructor() {
        double threshold = 0.02;
        int numtoshow = 3;
        HpoDiseases diseases = HpoDiseases.of(List.of());
        Posttest2Svg p2svg = new Posttest2Svg(RESULTS, diseases, threshold,numtoshow);
        assertNotNull(p2svg);
    }

    @Test
    public void testNumberOfDiffsToShow() {
        double threshold = 0.02;
        int numtoshow = 3;
        HpoDiseases diseases = HpoDiseases.of(DISEASES);
        Posttest2Svg p2svg = new Posttest2Svg(RESULTS, diseases, threshold,numtoshow);
        assertEquals(3, p2svg.getNumDifferentialsToShowSVG());
       String svg = p2svg.getSvgString();
       assertFalse(svg.isEmpty());
    }

    private static List<LrWithExplanation> createTestList(TermId termId, double lr1, double lr2, double lr3) {
        return List.of(
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr1),
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr2),
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr3)
        );
    }
}
