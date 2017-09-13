package org.monarchinitiative.lr2pg.likelihoodratio;

import java.util.List;

public class LRTest {

    private double pretestProbability;
    //private double sensitivity;
//    private double specificity;

    private List<TestResult> testResults;

    private char S;


    public LRTest(List<TestResult> results, double pretestprob, char TestSign) {
        this.pretestProbability = pretestprob;
        this.testResults = results;
        this.S=TestSign;
//        this.sensitivity=result.getSensitivity();
//        this.specificity=result.getSpecificity();
    }


    /**
     * TODO what if pretest prob is 100% ?
     *
     * @return
     */
    public double getPretestOdds() {
        return pretestProbability / (1 - pretestProbability);
    }


    public double getCompositeLikelihoodRatio() {
        if (S == 'P') {
            double Poslr = 1.0;
            for (TestResult tres : testResults) {
                Poslr *= tres.PositivelikelihoodRatio();
            }
            return Poslr;
        }
        else if (S == 'N') {
            double Neglr = 1.0;
            for (TestResult tres : testResults) {
                Neglr *= tres.NegativelikelihoodRatio();
            }
            return Neglr;
        }
        else {
            System.out.println("Wrong sign is entered. S is either 'N' or 'P'! ");
            return 0;
        }
    }




    public double getPosttestOdds() {
        return getCompositeLikelihoodRatio() * getPretestOdds();
    }

    public double getPosttestProbability() {
        double po=getPosttestOdds();
        return po/(1+po);

    }


}
