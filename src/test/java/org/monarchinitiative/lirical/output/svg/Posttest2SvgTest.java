package org.monarchinitiative.lirical.output.svg;



import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.TestResources;
import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.likelihoodratio.LrMatchType;
import org.monarchinitiative.lirical.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.lirical.likelihoodratio.LrWithExplanationFactory;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class Posttest2SvgTest {

    private static LrWithExplanationFactory FACTORY;

    private static AnalysisResults RESULTS;

    @BeforeAll
    public static void setup() throws NullPointerException {
        double PRETEST_PROB = 0.001;
        FACTORY = new LrWithExplanationFactory(TestResources.hpo());
        List<TestResult> results = new ArrayList<>();
        //result 1
        TermId some = TermId.of("HP:0000006");
        List<LrWithExplanation> reslist = createTestList(some, 1d, 10d, 100d);
        List<LrWithExplanation> excluded = List.of();
        List<HpoAnnotation> annotations = List.of();
        List<TermId> terms = List.of();
        HpoDisease d1 = HpoDisease.of(TermId.of("MONDO:1"), "DISEASE 1",annotations,terms,terms,terms,terms);
        TestResult result1 = TestResult.of(reslist,excluded,d1,PRETEST_PROB, null);
        List<LrWithExplanation> reslist2 = createTestList(some, 10d,100d,1000d);
        HpoDisease d2 = HpoDisease.of(TermId.of("MONDO:2"), "DISEASE 2", annotations,terms,terms,terms,terms);
        TestResult result2 = TestResult.of(reslist2,excluded,d2,PRETEST_PROB, null);
        List<LrWithExplanation> reslist3 = createTestList(some, 1d,0.1d,0.01d);
        HpoDisease d3 = HpoDisease.of(TermId.of("MONDO:3"), "DISEASE3", annotations,terms,terms,terms,terms);
        TestResult result3 = TestResult.of(reslist3,excluded,d3,PRETEST_PROB, null);
        results.add(result1);
        results.add(result2);
        results.add(result3);
        RESULTS = AnalysisResults.of(results);
    }



    @Test
    public void testConstructor() {
        double threshold = 0.02;
        int numtoshow = 3;
        Posttest2Svg p2svg = new Posttest2Svg(RESULTS, threshold,numtoshow);
        assertNotNull(p2svg);
    }

    @Test
    public void testNumberOfDiffsToShow() {
        double threshold = 0.02;
        int numtoshow = 3;
        Posttest2Svg p2svg = new Posttest2Svg(RESULTS, threshold,numtoshow);
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
