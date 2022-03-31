package org.monarchinitiative.lirical.likelihoodratio;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.TestResources;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Some of this test class is based on the data and cases presented in
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2683447/ (Likelihood ratio calculations)
 * Note -- the authors of that paper rounded results and this class does not!
 */
public class TestResultTest {

    private static final double EPSILON = 0.00001;
    private static double ratio(double sensitivity, double specificity) {
        return sensitivity/(1.0 - specificity);
    }


    private static final GeneIdentifier MADE_UP_GENE = GeneIdentifier.of(TermId.of("FAKE:123"), "FAKE_GENE");
    private static LrWithExplanationFactory FACTORY;
    private HpoDisease glaucoma;

    private TestResult tresultNoGenotype;
    private TestResult tresultWithGenotype;

    @BeforeAll
    public static void beforeAll() {
        FACTORY = new LrWithExplanationFactory(TestResources.hpo());
    }

    @BeforeEach
    public void init() {
        TermId glaucomaId = TermId.of("MONDO:123");
        List<TermId> emptyList = List.of();
        List<HpoAnnotation> emptyAnnot = List.of();
        glaucoma = HpoDisease.of(glaucomaId, "Glaucoma",emptyAnnot,emptyList,emptyList,emptyList,emptyList);

        HpoDisease d1 = HpoDisease.of(TermId.of("MONDO:1"), "d1",emptyAnnot,emptyList,emptyList,emptyList,emptyList);

        TermId some = TermId.of("HP:0000006");
        List<LrWithExplanation> list1 = createTestList(some, 2.0, 3.0, 4.0);
        List<LrWithExplanation> excluded = List.of();
        double prevalence = 0.025;
        GenotypeLrWithExplanation genotypeLr = GenotypeLrWithExplanation.of(MADE_UP_GENE, 2.0, "Explanation");
        tresultWithGenotype = TestResult.of(list1,excluded,d1, prevalence, genotypeLr);
        tresultNoGenotype = TestResult.of(list1,excluded,d1,prevalence, null);
    }

    @Test
    public void testGlaucomaLR1() {
        TestResult tresult;

        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // test #1
        // we obtain a test result with 60% sensitivity and 97% specifity
        TermId some = TermId.of("HP:0000006");
        LrWithExplanation LR1 = FACTORY.create(some, some, LrMatchType.EXACT_MATCH, ratio(0.60, 0.97));
        tresult = TestResult.of(List.of(LR1), List.of(), glaucoma,prevalence, null);
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
    public void testGlaucomaLR2() {
        TestResult tresult;

        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // We now do two tests. The first test is the same as above
        TermId some = TermId.of("HP:0000006");
        LrWithExplanation LR1 = FACTORY.create(some, some, LrMatchType.EXACT_MATCH, ratio(0.60, 0.97));
        // The other test is intraocular pressure (IOP)
        // IOP: (50% sensitivity and 92% specificity[9])
        LrWithExplanation LR2 = FACTORY.create(some, some, LrMatchType.EXACT_MATCH, ratio(0.50, 0.92));
        tresult = TestResult.of(List.of(LR1, LR2), List.of(), glaucoma,prevalence, null);
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
    public void testCompositepositiveLR() {
        TestResult tresult;
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        //IOP test
        double LR1 = ratio(0.5,0.92);
        //Dic
        double LR2 = ratio(0.60, 0.97);
        //GDx
        double LR3 = ratio(0.60, 0.97);

        List<LrWithExplanation> lr1 = createTestList(TermId.of("HP:0000006"), LR1, LR2, LR3);
        tresult = TestResult.of(lr1, List.of(),glaucoma,prevalence, null);
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
    public void testTestResultSorting() {

        TermId testId1 = TermId.of("MONDO:1");
        TermId testId2 = TermId.of("MONDO:2");
        TermId testId3 = TermId.of("MONDO:3");
        List<TermId> emptyList = List.of();
        List<HpoAnnotation> emptyAnnot = List.of();
        HpoDisease d2 = HpoDisease.of(testId2,"d2",emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d1 = HpoDisease.of(testId1,"d1",emptyAnnot,emptyList,emptyList,emptyList,emptyList);
        HpoDisease d3 = HpoDisease.of(testId3,"d3",emptyAnnot,emptyList,emptyList,emptyList,emptyList);

        TermId some = TermId.of("HP:0000006");
        List<LrWithExplanation> list1 = createTestList(some, 2.0, 3.0, 4.0);
        List<LrWithExplanation> list2 = createTestList(some, 20.0, 3.0, 4.0);
        List<LrWithExplanation> list3 = createTestList(some, 20.0, 30.0, 4.0);;
        List<LrWithExplanation> excluded = List.of();
        double prevalence = 0.025;
        TestResult result1 = TestResult.of(list1,excluded,d1,prevalence, null);
        TestResult result2 = TestResult.of(list2,excluded,d2,prevalence, null);
        TestResult result3 = TestResult.of(list3,excluded,d3,prevalence, null);
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
        GenotypeLrWithExplanation genotypeLr = GenotypeLrWithExplanation.of(MADE_UP_GENE, 2.0, "Explanation");
        TestResult result4= TestResult.of(list3,excluded,d3, prevalence, genotypeLr);
        lst.add(result4);
        assertEquals(lst.get(3),result4);
        lst.sort(Comparator.reverseOrder());
        assertEquals(lst.get(0),result4);
    }

    private List<LrWithExplanation> createTestList(TermId termId, double lr1, double lr2, double lr3) {
        return List.of(
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr1),
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr2),
                FACTORY.create(termId, termId, LrMatchType.EXACT_MATCH, lr3)
        );
    }


    @Test
    public void testHasGenotype() {
        assertTrue(tresultWithGenotype.genotypeLr().isPresent());
        assertFalse(tresultNoGenotype.genotypeLr().isPresent());
    }


    @Test
    public void testGetGenotypeLR() {
        double expected=2.0;
        Optional<GenotypeLrWithExplanation> genotypeLr = tresultWithGenotype.genotypeLr();
        assertThat(genotypeLr.isPresent(), equalTo(true));
        assertEquals(expected,genotypeLr.get().lr(),EPSILON);
    }

    @Test
    public void testGetDiseaseCurie() {
        TermId diseaseCurie = TermId.of("MONDO:1"); // we used this in the init function to create tresultNoGenotype
        assertEquals(diseaseCurie,tresultNoGenotype.diseaseId());
    }

    @Test
    public void testGetDiseaseName() {
        String name="d1"; // set in init() function
        assertEquals(name,tresultNoGenotype.getDiseaseName());
    }

    @Test
    public void testObservedPhenotypeRatio() {
        // we used  List<Double> list1 = ImmutableList.of(2.0,3.0,4.0); in the init function
        assertEquals(2.0,tresultNoGenotype.getObservedPhenotypeRatio(0),EPSILON);
        assertEquals(3.0,tresultNoGenotype.getObservedPhenotypeRatio(1),EPSILON);
        assertEquals(4.0,tresultNoGenotype.getObservedPhenotypeRatio(2),EPSILON);
    }

    @Test
    public void testGetPretestProb() {
        double expected = 0.025;  // in init() we have  double prevalence = 0.025;
        assertEquals(expected,tresultNoGenotype.pretestProbability(),EPSILON);
    }

    @Test
    public void testGetEntrezGeneId() {
        Optional<GenotypeLrWithExplanation> genotypeLr = tresultWithGenotype.genotypeLr();
        assertThat(genotypeLr.isPresent(), equalTo(true));
        assertEquals(MADE_UP_GENE,genotypeLr.get().geneId());
    }


}
