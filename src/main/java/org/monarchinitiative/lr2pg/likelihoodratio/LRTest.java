package org.monarchinitiative.lr2pg.likelihoodratio;

public class LRTest {

    private double pretestProbability;
    private double sensitivity;
    private double specificity;


    public LRTest(TestResult result, double pretestprob) {
        this.pretestProbability=pretestprob;
        this.sensitivity=result.getSensitivity();
        this.specificity=result.getSpecificity();
    }

    /**
     * TODO what is specificity is 100%?
     * @return
     */
    public double getLikelihoodRatio() {
        return sensitivity /( 1- specificity);
    }

    /**
     * TODO what if pretest prob is 100% ?
     * @return
     */
    public double getPretestOdds() {
        return pretestProbability/(1-pretestProbability);
    }

    public double getPosttestOdds() {
        return getLikelihoodRatio() * getPretestOdds();
    }

    public double getPosttestProbability() {
        double po=getPosttestOdds();
        return po/(1+po);

    }


}
