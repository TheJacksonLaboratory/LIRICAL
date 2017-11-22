package org.monarchinitiative.lr2pg.likelihoodratio;

//import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LikelihoodRatio {
    private static final Logger logger = LogManager.getLogger();
    //static Logger logger = Logger.getLogger(LikelihoodRatio.class.getName());
  //  private int x,y;

    /**
     *
     //* @param pretestprob
     //* @param b
     */
   /* LikelihoodRatio(int pretestprob, int b) {
        this.x=pretestprob;
        this.y=b;
        logger.trace(String.format("x=%d,y=%d",x,y ));
    }


    public int sum() { return x+y; }

    public double odds() {
        return -1d;
    }*/
    private double x,y,z;
    LikelihoodRatio(double pretestprob, double sensit, double specif) {
        this.x = pretestprob;
        this.y = sensit;
        this.z = specif;
       // logger.trace(String.format("x=%f,y=%f,z=%f",x,y,z ));
    }

    public double ratio() {return y/(1-z);}

    public double pretestodds() {return x/(1-x);}

    //public double posttestodds(){return (x/(1-x))*(y/(1-z));}

    //public double posttestprob(){return ((x/(1-x))*(y/(1-z)))/(1+(x/(1-x))*(y/(1-z)));}

    /**
     * We will interpret the frequency of an HPO feature in a disease as the sensitivity of the test.
     * The specificity of the test is interpreted to be the probability that a patient WITHOUT the
     * disease in question does NOT have the feature. THis is calculated as 1-background, where
     * background is the mean frequency of the Hpo feature across all diseases in the database. Note that
     * since the denominator of the LR test is 1-specificity=1-(1-background)=background, the LR can
     * best given as diseaseFreqeucny/background.
     * TODO what if diseaseFreqeuncy==0 ? Need a default.
     * @param diseaseFrequency
     * @param backgroundFreqeuncy
     * @return
     */
    public static double HpoFrequencies2LR(double diseaseFrequency, double backgroundFreqeuncy) {
        final double EPSILON=0.0001;
        final double ONEMINUSEPSILON=1.0-EPSILON;
        diseaseFrequency=Math.max(EPSILON,diseaseFrequency);
        backgroundFreqeuncy=Math.min(ONEMINUSEPSILON,backgroundFreqeuncy);
        return diseaseFrequency/backgroundFreqeuncy;
    }


    public static double ratio(double sensitivity, double specificity) {
        return sensitivity/(1.0 - specificity);
    }



}
