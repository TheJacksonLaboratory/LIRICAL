package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Some of this test class is based on the data and cases presented in
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2683447/
 * Note -- the authors of that paper rounded results and this class does not!
 */
public class TestResultTest {

    private static final double EPSILON = 0.00001;
    private static double ratio(double sensitivity, double specificity) {
        return sensitivity/(1.0 - specificity);
    }

    @Test
    public void testGlaucomaLR1() {
        TestResult tresult;
        String diseasename = "glaucoma";
        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // test #1
        // we obtain a test result with 60% sensitivity and 97% specifity
        double LR1 = ratio(0.60, 0.97);
        builder.add(LR1);
        tresult = new TestResult(builder.build(), diseasename,prevalence);
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
        String diseasename = "glaucoma";
        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
        // The prevalence of glaucoma is 2.5%
        double prevalence = 0.025;
        // We now do two tests. The first test is the same as above
        double LR1 = ratio(0.60, 0.97);
        builder.add(LR1);
        // The other test is intraocular pressure (IOP)
        // IOP: (50% sensitivity and 92% specificity[9])
        double LR2 = ratio(0.50, 0.92);
        builder.add(LR2);
        tresult = new TestResult(builder.build(), diseasename,prevalence);
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
        String diseasename = "glaucoma";
        ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
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

        tresult = new TestResult(builder.build(),diseasename,prevalence);
         //PretestOdds = pretest prob / (1-pretest prob) = 0.95 / 0.05 = 19.0
        double expectedPretestOdds = 0.0256410;
        Assert.assertEquals(expectedPretestOdds, tresult.pretestodds(), EPSILON);

        //PosttestOdds = PrestestOdds * Compositelikelihoodratio = 0.03 * 6.25 * 20 * 20
        double expected = 64.102564;
        Assert.assertEquals(expected, tresult.posttestodds(), EPSILON);

        //PosttestProb = PosttestOdds / (1+ PosttestOdds)
        double ptodds=expected;
        expected = 0.9846396;
        Assert.assertEquals(expected, (ptodds/(ptodds+1)), EPSILON);
    }


}
