package org.monarchinitiative.lr2pg.likelihoodratio;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

/**
 * Some of this test class is based on the data and cases presented in
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC2683447/
 * Note -- the authors of that paper rounded results and this class does not!
 */
public class TestLRTest {

    private static final double EPSILON=0.00001;
    private static char SignTest = 'P';


    /*
    @Test
    public void testGlaucomaLR1() {
        List<TestResult> results = new ArrayList<>();
        if (SignTest == 'P') {
            // The prevalence of glaucoma is 2.5%
            double prevalence = 0.025;
            // we obtain a test result with 60% sensitivity and 97% specifity
            TestResult result = new TestResult(0.60, 0.97);
            //List<TestResult> results = new ArrayList<>();
            results.add(result);
            // There should be a LR of 20
            LRTest lrtest = new LRTest(results, prevalence, SignTest);
            double expectedLikelihoodRatio = 20;
            Assert.assertEquals(expectedLikelihoodRatio, lrtest.getCompositeLikelihoodRatio(), EPSILON);
            //pretest odds = pretest probability / (1-pretest probability)
            // pretest odds are 0.025/0.975=0.02564103
            double expectedPretestOdds = 0.02564103;
            Assert.assertEquals(expectedPretestOdds, lrtest.getPretestOdds(), EPSILON);
            //Posttest odds = pretest odds * LR
            //20*0.02564103 =0.5128206
            double expectgedPosttestOdds = 0.5128206;
            Assert.assertEquals(expectgedPosttestOdds, lrtest.getPosttestOdds(), EPSILON);
            //Posttest probability = posttest odds / (posttest odds+1)
            //0.3389831
            double expectedPosttestOdds = 0.3389831;
            Assert.assertEquals(expectedPosttestOdds, lrtest.getPosttestProbability(), EPSILON);
        }

    }

    @Test
    public void testGlaucomaLR2() {
        List<TestResult> results = new ArrayList<>();
        if (SignTest == 'P') {
            // We now do two tests. The first test is the same as above
            double prevalence = 0.025;
            TestResult result = new TestResult(0.60, 0.97);
            //List<TestResult> results = new ArrayList<>();
            results.add(result);
            LRTest lrtest = new LRTest(results, prevalence, SignTest);
            // The other test is intraocular pressure (IOP)
            // IOP: (50% sensitivity and 92% specificity[9])
            TestResult iopResult = new TestResult(0.50, 0.92);
            List<TestResult> iopresults = new ArrayList<>();
            iopresults.add(iopResult);
            LRTest iopTest = new LRTest(iopresults, prevalence, SignTest);


            // the pretest odds are the same as with the first test because they are based only on
            // the population prevalence.
            double expectedPretestOdds = 0.02564103;
            Assert.assertEquals(expectedPretestOdds, lrtest.getPretestOdds(), EPSILON);
            //Positive LR of IOP: = sensitivity / 1- specificity =
            // //0.5/ 100 − 92 = 0.5 /.08 = 6.25
            double expected = 6.25;
            Assert.assertEquals(expected, iopTest.getCompositeLikelihoodRatio(), EPSILON);
            // Posttest odds  = pretest odds * LR for IOP
            //posttest odds = 0.02564103 * 6.25 = 0.1602564
            expected = 0.1602564;
            Assert.assertEquals(expected, iopTest.getPosttestOdds(), EPSILON);
            //Posttest probability = posttest odds / (posttest odds+1)
            //posttest probability = 0.1602564 /(1+0.1602564) = 0.1381215
            expected = 0.1381215;
            Assert.assertEquals(expected, iopTest.getPosttestProbability(), EPSILON);
            //NOW LET US DO A SECOND TEST -- same test as above (GDx)
            // we obtain a test result with 60% sensitivity and 97% specifity
            // THe pretest probability is now the posttest probability after the IOP test!
            TestResult gdxResult = new TestResult(0.60, 0.97);
            List<TestResult> results2 = new ArrayList<>();
            results2.add(gdxResult);
            LRTest gdxTest = new LRTest(results2, iopTest.getPosttestProbability(), SignTest);
            //Pretest odds: 0.1381215 / (1-0.1381215) = 0.1602563
            expected = 0.1602563;
            Assert.assertEquals(expected, gdxTest.getPretestOdds(), EPSILON);
            // The likelihood ratio of the GDX test is still the same (20)
            Assert.assertEquals(20.0, lrtest.getCompositeLikelihoodRatio(), EPSILON);
            // Posttest odds = 0.1602563 * 20 = 3.205126
            expected = 3.205126;
            Assert.assertEquals(expected, gdxTest.getPosttestOdds(), EPSILON);
            //Posttest probability = 3.205126 / (1+3.205126) = 0.762195
            expected = 0.762195;
            Assert.assertEquals(expected, gdxTest.getPosttestProbability(), EPSILON);
        }

    }

    @Test
    public void testGlaucomaLR3() {
        List<TestResult> results = new ArrayList<>();
        if (SignTest == 'P') {
            // We now do a second test. The pretest probability of the second test
            // is now equal to the posttest probability of the first test!
            double prevalence = 0.1602563;
            //Test sensitivity is 60% sensitivity and test specifity is 97%
            TestResult result = new TestResult(0.60, 0.97);
           // List<TestResult> results = new ArrayList<>();
            results.add(result);
            LRTest lrtest = new LRTest(results, prevalence, SignTest);

            //pretest odds = pretest probability / (1-pretest probability)
            // pretest odds are 0.1602563/0.8397437=0.19083954
            double expectedPretestOdds = 0.19083954;
            Assert.assertEquals(expectedPretestOdds, lrtest.getPretestOdds(), EPSILON);
            //The likelihood ratio of the test is still the same (20)
            //LR= sensitivity / (1-specifity)=0.6/(1-0.7)=20
            double expectedLikelihoodRatio = 20;
            Assert.assertEquals(expectedLikelihoodRatio, lrtest.getCompositeLikelihoodRatio(), EPSILON);
            //PosttestOdds = LR * PretestOdds
            //PosttestOdds = 20 * 0.19083954 = 3.8167908
            double expected = 3.8167908;
            Assert.assertEquals(expected, lrtest.getPosttestOdds(), EPSILON);
            //PosttestProbability = PosttestOdds / (1+PosttestProbability)
            //PosttestProbability = 3.8167908 / 4.8167908
            expected = 0.792392;
            Assert.assertEquals(expected, lrtest.getPosttestProbability(), EPSILON);
        }
    }

    @Test
    public void testCompositepositiveLR(){
        List<TestResult> results = new ArrayList<>();
        if (SignTest == 'P') {
            double prevalence = 0.025;
            //IOP test
            TestResult result1 = new TestResult(0.5, 0.92);
            //List<TestResult> results = new ArrayList<>();
            results.add(result1);
            //PositiveLR = Sensitivity/ (1-Specifity) = 0.5 / 0.08 = 6.25
            double expectedLikelihoodRatio = 6.25;
            Assert.assertEquals(expectedLikelihoodRatio, result1.PositivelikelihoodRatio(), EPSILON);

            //Dic
            TestResult result2 = new TestResult(0.60, 0.97);
            //List<TestResult> results = new ArrayList<>();
            results.add(result2);

            expectedLikelihoodRatio = 20;
            Assert.assertEquals(expectedLikelihoodRatio, result2.PositivelikelihoodRatio(), EPSILON);

            //GDx
            TestResult result3 = new TestResult(0.60, 0.97);
            // List<TestResult> results = new ArrayList<>();
            results.add(result3);
            expectedLikelihoodRatio = 20;
            Assert.assertEquals(expectedLikelihoodRatio, result3.PositivelikelihoodRatio(), EPSILON);

            LRTest lrtest = new LRTest(results, prevalence, SignTest);

            //PretestOdds = pretest prob / (1-pretest prob) = 0.95 / 0.05 = 19.0
            double expectedPretestOdds = 0.0256410;
            Assert.assertEquals(expectedPretestOdds, lrtest.getPretestOdds(), EPSILON);

            //PosttestOdds = PrestestOdds * Compositelikelihoodratio = 0.03 * 6.25 * 20 * 20

            double expected = 64.102564;
            Assert.assertEquals(expected, lrtest.getPosttestOdds(), EPSILON);

            //PosttestProb = PosttestOdds / (1+ PosttestOdds)
            expected = 0.9846396;
            Assert.assertEquals(expected, lrtest.getPosttestProbability(), EPSILON);

        }
    }


    @Test
    public void testCompositeNegativeLR(){
        List<TestResult> results = new ArrayList<>();
        if (SignTest == 'N') {
            double prevalence = 0.95;
            //IOP test
            TestResult result1 = new TestResult(0.5, 0.92);
            //List<TestResult> results = new ArrayList<>();
            results.add(result1);
            //NegativeLR = Specifity/ (1-Sensitivity) = 0.92 / 0.5 = 1.84
            double expectedLikelihoodRatio = 1.84;
            Assert.assertEquals(expectedLikelihoodRatio, result1.NegativelikelihoodRatio(), EPSILON);

            //Optic disc:
            TestResult result2 = new TestResult(0.72, 0.79);
            results.add(result2);
            //NegativeLR = Specifity/ (1-Sensitivity) = 0.79 / 0.28 = 2.821428
            expectedLikelihoodRatio = 2.821428;
            Assert.assertEquals(expectedLikelihoodRatio, result2.NegativelikelihoodRatio(), EPSILON);

            //GDx VCC (for NFI score >20)
            TestResult result3 = new TestResult(0.905, 0.529);
            results.add(result3);
            //NegativeLR = Specifity/ (1-Sensitivity) = 0.529 / (1-0.905) = 5.5868421
            expectedLikelihoodRatio = 5.568421;
            Assert.assertEquals(expectedLikelihoodRatio, result3.NegativelikelihoodRatio(), EPSILON);

            LRTest lrtest = new LRTest(results, prevalence, SignTest);

            //Compositelikelihoodratio = LR1 * LR2 * LR3 = 1.84 * 2.821428 * 5.568421
            expectedLikelihoodRatio = 28.90806015;
            Assert.assertEquals(expectedLikelihoodRatio, lrtest.getCompositeLikelihoodRatio(), EPSILON);

            //PretestOdds = pretest prob / (1-pretest prob) = 0.95 / 0.05 = 19.0
            double expectedPretestOdds = 19.0;
            Assert.assertEquals(expectedPretestOdds, lrtest.getPretestOdds(), EPSILON);

            //PosttestOdds = PrestestOdds * Compositelikelihoodratio = 19.0 * 28.90806015
            double expected = 549.2531428;
            Assert.assertEquals(expected, lrtest.getPosttestOdds(), EPSILON);

            //PosttestProb = PosttestOdds / (1+ PosttestOdds)
            expected = 0.998182;
            Assert.assertEquals(expected, lrtest.getPosttestProbability(), EPSILON);

        }

    }

    */

}
