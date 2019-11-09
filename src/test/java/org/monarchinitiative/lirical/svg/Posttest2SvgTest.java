package org.monarchinitiative.lirical.svg;



import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class Posttest2SvgTest {


    private static List<TestResult> results;

    @BeforeAll
    static void setup() throws NullPointerException {
        final double PRETEST_PROB = 0.001;
        results = new ArrayList<>();
        //result 1
        List<Double> reslist = ImmutableList.of(1d, 10d, 100d);
        List<Double> excluded = ImmutableList.of();
        List<HpoAnnotation> annotations = ImmutableList.of();
        List<TermId> terms = ImmutableList.of();
        HpoDisease d1 = new HpoDisease("DISEASE 1", TermId.of("MONDO:1"),annotations,terms,terms,terms,terms);
        TestResult result1 = new TestResult(reslist,excluded,d1,PRETEST_PROB);
        List<Double> reslist2 = ImmutableList.of(10d,100d,1000d);
        HpoDisease d2 = new HpoDisease("DISEASE 2", TermId.of("MONDO:1"),annotations,terms,terms,terms,terms);
        TestResult result2 = new TestResult(reslist2,excluded,d2,PRETEST_PROB);
        List<Double> reslist3 = ImmutableList.of(1d,0.1d,0.01d);
        HpoDisease d3 = new HpoDisease("DISEASE3", TermId.of("MONDO:3"),annotations,terms,terms,terms,terms);
        TestResult result3 = new TestResult(reslist3,excluded,d3,PRETEST_PROB);
        results.add(result1);
        results.add(result2);
        results.add(result3);
    }



    @Test
    void testConstructor() {
        double threshold = 0.02;
        int numtoshow = 3;
        Posttest2Svg p2svg = new Posttest2Svg(results, threshold,numtoshow);
        assertNotNull(p2svg);
    }

    @Test
    void testNumberOfDiffsToShow() {
        double threshold = 0.02;
        int numtoshow = 3;
        Posttest2Svg p2svg = new Posttest2Svg(results, threshold,numtoshow);
        assertEquals(3, p2svg.getNumDifferentialsToShowSVG());
       //  String svg = p2svg.getSvgString();
        //System.out.println(svg);
    }
}
