package org.monarchinitiative.lr2pg.likelihoodratio;

import org.junit.Assert;
import org.junit.Test;

/**
 * Some of this test class is based on the data and cases presented in
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2683447/
 * Note -- the authors of that paper rounded results and this class does not!
 */
public class TestLRTest {

    private static final double EPSILON=0.00001;



    @Test
    public void testGlaucomaLR1() {
        // The prevalence of glaucoma is 2.5%
        double prevalence=0.025;
        // we obtain a test result with 60% sensitivity and 97% specifity
        TestResult result = new TestResult(0.60,0.97);
        // There should be a LR of 20
        LRTest lrtest = new LRTest(result,prevalence);
        double expectedLikelihoodRatio=20;
        Assert.assertEquals(expectedLikelihoodRatio,lrtest.getLikelihoodRatio(),EPSILON);
        //pretest odds = pretest probability / (1-pretest probability)
        // pretest odds are 0.025/0.975=0.02564103
        double expectedPretestOdds=0.02564103;
        Assert.assertEquals(expectedPretestOdds,lrtest.getPretestOdds(),EPSILON);
        //Posttest odds = pretest odds * LR
        //20*0.02564103 =0.5128206
        double expectgedPosttestOdds=0.5128206;
        Assert.assertEquals(expectgedPosttestOdds,lrtest.getPosttestOdds(),EPSILON);
        //Posttest probability = posttest odds / (posttest odds+1)
        //0.3389831
        double expectedPosttestOdds=0.3389831;
        Assert.assertEquals(expectedPosttestOdds,lrtest.getPosttestProbability(),EPSILON);
    }

    @Test
    public void testGlaucomaLR2() {
        // We now do two tests. The first test is the same as above
        double prevalence=0.025;
        TestResult result = new TestResult(0.60,0.97);
        LRTest lrtest = new LRTest(result,prevalence);
        // The other test is intraocular pressure (IOP)
        // IOP: (50% sensitivity and 92% specificity[9])
        TestResult iopResult = new TestResult(0.50,0.92);
        LRTest iopTest = new LRTest(iopResult,prevalence);
        // the pretest odds are the same as with the first test because they are based only on
        // the population prevalence.
        double expectedPretestOdds=0.02564103;
        Assert.assertEquals(expectedPretestOdds,lrtest.getPretestOdds(),EPSILON);
        //Positive LR of IOP: = sensitivity / 1- specificity =
        // //0.5/ 100 − 92 = 0.5 /.08 = 6.25
        double expected=6.25;
        Assert.assertEquals(expected,iopTest.getLikelihoodRatio(),EPSILON);
        // Posttest odds  = pretest odds * LR for IOP
        //posttest odds = 0.02564103 * 6.25 = 0.1602564
        expected=0.1602564;
        Assert.assertEquals(expected,iopTest.getPosttestOdds(),EPSILON);
        //Posttest probability = posttest odds / (posttest odds+1)
       //posttest probability = 0.1602564 /(1+0.1602564) = 0.1381215
        expected=0.1381215;
        Assert.assertEquals(expected,iopTest.getPosttestProbability(),EPSILON);
        //NOW LET US DO A SECOND TEST -- same test as above (GDx)
        // we obtain a test result with 60% sensitivity and 97% specifity
        // THe pretest probability is now the posttest probability after the IOP test!
        TestResult gdxResult = new TestResult(0.60,0.97);
        LRTest gdxTest = new LRTest(gdxResult,iopTest.getPosttestProbability());
        //Pretest odds: 0.1381215 / (1-0.1381215) = 0.1602563
        expected =0.1602563;
        Assert.assertEquals(expected,gdxTest.getPretestOdds(),EPSILON);
        // The likelihood ratio of the GDX test is still the same (20)
        Assert.assertEquals(20.0,lrtest.getLikelihoodRatio(),EPSILON);
        // Posttest odds = 0.1602563 * 20 = 3.205126
        expected=3.205126;
        Assert.assertEquals(expected,gdxTest.getPosttestOdds(),EPSILON);
        //Posttest probability = 3.205126 / (1+3.205126) = 0.762195
        expected=0.762195;
        Assert.assertEquals(expected,gdxTest.getPosttestProbability(),EPSILON);



        // We now do a second test. The pretest probability of the second test
        // is now equal to the posttest probability of the first test!



    }



}
