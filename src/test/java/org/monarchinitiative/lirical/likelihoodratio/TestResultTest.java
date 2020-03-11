package org.monarchinitiative.lirical.likelihoodratio;

import com.google.common.collect.ImmutableList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Some of this test class is based on the data and cases presented in
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2683447/ (Likelihood ratio calculations)
 * Note -- the authors of that paper rounded results and this class does not!
 */
class TestResultTest {

    private static final double EPSILON = 0.00001;
    private static double ratio(double sensitivity, double specificity) {
        return sensitivity/(1.0 - specificity);
    }
    private HpoDisease glaucoma;

    private TestResult tresultNoGenotype;
    private TestResult tresultWithGenotype;

    @BeforeEach
    void init() {
        TermId glaucomaId = TermId.of("MONDO:123");
        List<TermId> emptyList = ImmutableList.of();
        List<HpoAnnotation> emptyAnnot = ImmutableList.of();
        glaucoma = new HpoDisease("Glaucoma",glaucomaId,emptyAnnot,emptyList,emptyList,emptyList,emptyList);

        TermId testId1 = TermId.of("MONDO:1");
        TermId testId2 = TermId.of("MONDO:2");
        TermId testId3 = TermId.of("MONDO:3");
        HpoDisease d1 = new HpoDisease("d1",testId1,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d2 = new HpoDisease("d2",testId2,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d3 = new HpoDisease("d3",testId3,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        List<Double> list1 = ImmutableList.of(2.0,3.0,4.0);
        List<Double> list2 = ImmutableList.of(20.0,3.0,4.0);
        List<Double> list3 = ImmutableList.of(20.0,30.0,4.0);
        ImmutableList<Double> excluded = ImmutableList.of();
        double prevalence = 0.025;
        TermId geneId =  TermId.of("FAKE:123");
        double genotypeLR=2.0;
        tresultWithGenotype = new TestResult(list1,excluded,d1,genotypeLR,geneId,prevalence);
        tresultNoGenotype = new TestResult(list1,excluded,d1,prevalence);
    }

    @Test
    void testGlaucomaLR1() {
        TestResult tresult;

        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
        ImmutableList<Double> excluded = ImmutableList.of();
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // test #1
        // we obtain a test result with 60% sensitivity and 97% specifity
        double LR1 = ratio(0.60, 0.97);
        builder.add(LR1);
        tresult = new TestResult(builder.build(), excluded,glaucoma,prevalence);
        // There should be a LR of 20 after just one test
        assertEquals(1, tresult.getNumberOfTests());
        // There should be a LR of 20
        assertEquals(20.0, tresult.getCompositeLR(), EPSILON);
        //pretest odds = pretest probability / (1-pretest probability)
        // pretest odds are 0.025/0.975=0.02564103
        double expectedPretestOdds = 0.02564103;
        assertEquals(expectedPretestOdds, tresult.pretestodds(), EPSILON);
        //Posttest odds = pretest odds * LR
        //20*0.02564103 =0.5128206
        double expectedPosttestOdds = 0.5128206;
        assertEquals(expectedPosttestOdds, tresult.posttestodds(), EPSILON);
        //Posttest probability = posttest odds / (posttest odds+1)
        //0.3389831
        double expectedPosttestProb = 0.3389831;
        assertEquals(expectedPosttestProb, (expectedPosttestOdds / (expectedPosttestOdds + 1)), EPSILON);
    }

    @Test
    void testGlaucomaLR2() {
        TestResult tresult;
        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
        ImmutableList<Double> excluded = ImmutableList.of();
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // We now do two tests. The first test is the same as above
        double LR1 = ratio(0.60, 0.97);
        builder.add(LR1);
        // The other test is intraocular pressure (IOP)
        // IOP: (50% sensitivity and 92% specificity[9])
        double LR2 = ratio(0.50, 0.92);
        builder.add(LR2);
        tresult = new TestResult(builder.build(),excluded, glaucoma,prevalence);
        // the pretest odds are the same as with the first test because they are based only on
        // the population prevalence.
        double expectedPretestOdds = 0.02564103;
        assertEquals(expectedPretestOdds, tresult.pretestodds(), EPSILON);
        //Positive LR of IOP: = sensitivity / 1- specificity =
        // //0.5/ 100 − 92 = 0.5 /.08 = 6.25
        double expected = 6.25 * 20;  // LR is product of LRs of individual tests
        assertEquals(expected, tresult.getCompositeLR(), EPSILON);
    }


    @Test
    void testCompositepositiveLR() {
        TestResult tresult;
        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
        ImmutableList<Double> excluded = ImmutableList.of();
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        //IOP test
        double LR1 = ratio(0.5,0.92);
        builder.add(LR1);
        //Dic
        double LR2 = ratio(0.60, 0.97);
        builder.add(LR2);
        //GDx
        double LR3 = ratio(0.60, 0.97);
        builder.add(LR3);

        tresult = new TestResult(builder.build(),excluded,glaucoma,prevalence);
         //PretestOdds = pretest prob / (1-pretest prob) = 0.95 / 0.05 = 19.0
        double expectedPretestOdds = 0.0256410;
        assertEquals(expectedPretestOdds, tresult.pretestodds(), EPSILON);

        //PosttestOdds = PrestestOdds * Compositelikelihoodratio = 0.03 * 6.25 * 20 * 20
        double expected = 64.102564;
        assertEquals(expected, tresult.posttestodds(), EPSILON);

        //PosttestProb = PosttestOdds / (1+ PosttestOdds)
        double ptodds=expected;
        expected = 0.9846396;
        assertEquals(expected, (ptodds/(ptodds+1)), EPSILON);
    }


    @Test
    void testTestResultSorting() {

        TermId testId1 = TermId.of("MONDO:1");
        TermId testId2 = TermId.of("MONDO:2");
        TermId testId3 = TermId.of("MONDO:3");
        List<TermId> emptyList = ImmutableList.of();
        List<HpoAnnotation> emptyAnnot = ImmutableList.of();
        HpoDisease d1 = new HpoDisease("d1",testId1,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d2 = new HpoDisease("d2",testId2,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d3 = new HpoDisease("d3",testId3,emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        List<Double> list1 = ImmutableList.of(2.0,3.0,4.0);
        List<Double> list2 = ImmutableList.of(20.0,3.0,4.0);
        List<Double> list3 = ImmutableList.of(20.0,30.0,4.0);
        ImmutableList<Double> excluded = ImmutableList.of();
        double prevalence = 0.025;
        TestResult result1 = new TestResult(list1,excluded,d1,prevalence);
        TestResult result2 = new TestResult(list2,excluded,d2,prevalence);
        TestResult result3 = new TestResult(list3,excluded,d3,prevalence);
        assertEquals(24.0,result1.getCompositeLR(),EPSILON);
        assertEquals(240.0,result2.getCompositeLR(),EPSILON);
        assertEquals(2400.0,result3.getCompositeLR(),EPSILON);
        List<TestResult> lst = new ArrayList<>();
        lst.add(result1);
        lst.add(result2);
        lst.add(result3);
        assertEquals(lst.get(0),result1);
        lst.sort(Comparator.reverseOrder());
        assertEquals(lst.get(0),result3);
        assertEquals(lst.get(1),result2);
        assertEquals(lst.get(2),result1);
        // equivalently  //Map<TermId,TestResult> evaluateRanks(Map<TermId,TestResult> resultMap)
        // The ranks of the objects get set in the evaluate method of HpoCase so cannot be tested here.
        // now add another test result, same as result3 but with additional genotype evidence
        // result4 should now be the top hit
        double genotypeLR=2.0;
        TermId geneId=TermId.of("NCBI:Fake");
        TestResult result4=new TestResult(list3,excluded,d3,genotypeLR,geneId,prevalence);
        lst.add(result4);
        assertEquals(lst.get(3),result4);
        lst.sort(Comparator.reverseOrder());
        assertEquals(lst.get(0),result4);
    }


    @Test
    void testHasGenotype() {
        assertTrue(tresultWithGenotype.hasGenotype());
        assertFalse(tresultNoGenotype.hasGenotype());
    }


    @Test
    void testGetGenotypeLR() {
        double expected=2.0;
        assertEquals(expected,tresultWithGenotype.getGenotypeLR(),EPSILON);
    }

    @Test
    void testNoGenotypeThrows() {
        // We should never try to access the genotype LR if no genotype was tested
        // and so the method throws an exception
        Assertions.assertThrows(NullPointerException.class, () ->
            assertEquals(1.0,tresultNoGenotype.getGenotypeLR(),EPSILON)
        );
    }

    @Test
    void testHasExplanation() {
        assertFalse(tresultNoGenotype.hasGenotypeExplanation());
        tresultNoGenotype.setGenotypeExplanation("nonsense");
        assertTrue(tresultNoGenotype.hasGenotypeExplanation());
        assertEquals("nonsense",tresultNoGenotype.getGenotypeExplanation());
    }

    @Test
    void testGetDiseaseCurie() {
        TermId diseaseCurie = TermId.of("MONDO:1"); // we used this in the init function to create tresultNoGenotype
        assertEquals(diseaseCurie,tresultNoGenotype.getDiseaseCurie());
    }

    @Test
    void testGetDiseaseName() {
        String name="d1"; // set in init() function
        assertEquals(name,tresultNoGenotype.getDiseaseName());
    }

    @Test
    void testObservedPhenotypeRatio() {
        // we used  List<Double> list1 = ImmutableList.of(2.0,3.0,4.0); in the init function
        assertEquals(2.0,tresultNoGenotype.getObservedPhenotypeRatio(0),EPSILON);
        assertEquals(3.0,tresultNoGenotype.getObservedPhenotypeRatio(1),EPSILON);
        assertEquals(4.0,tresultNoGenotype.getObservedPhenotypeRatio(2),EPSILON);
    }

    @Test
    void testGetPretestProb() {
        double expected = 0.025;  // in init() we have  double prevalence = 0.025;
        assertEquals(expected,tresultNoGenotype.getPretestProbability(),EPSILON);
    }

    @Test
    void testGetEntrezGeneId() {
        TermId expected=TermId.of("FAKE:123");
        assertEquals(expected,tresultWithGenotype.getEntrezGeneId());
    }


}
