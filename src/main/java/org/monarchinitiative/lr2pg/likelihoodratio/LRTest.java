package org.monarchinitiative.lr2pg.likelihoodratio;

import java.util.List;

public class LRTest {

    private double pretestProbability;
    //private double sensitivity;
//    private double specificity;

    private List<TestResult> testResults;


    public LRTest(List<TestResult> results, double pretestprob) {
        this.pretestProbability=pretestprob;
        this.testResults=results;
//        this.sensitivity=result.getSensitivity();
//        this.specificity=result.getSpecificity();
    }


    /**
     * TODO what if pretest prob is 100% ?
     * @return
     */
    public double getPretestOdds() {
        return pretestProbability/(1-pretestProbability);
    }


    public double getCompositeLikelihoodRatio() {
        double lr=1.0;
        for (TestResult tres:testResults) {
            lr *= tres.likelihoodRatio();
        }
        return lr;
    }


    public double getPosttestOdds() {
        return getCompositeLikelihoodRatio() * getPretestOdds();
    }

    public double getPosttestProbability() {
        double po=getPosttestOdds();
        return po/(1+po);

    }


}
