package org.monarchinitiative.lr2pg.likelihoodratio;

/**
 * This class organizes information about the result of a test. For instance,  a GDx VCC test for
 * glaucoma may result in a measurement of 48, a value that is known to
 * have a 60% sensitivity and 97% specificity for glaucoma. In general, we need to have the sensitivity and the
 * specificity of a test result in order to perform a likelihood ratio test. The numerical value of the test
 * (in this case, 48) is not important.
 * @author Peter Robinson
 * @version 0.0.1
 */
public class TestResult {

    private double sensitivity;

    private double specificity;


    public double getSensitivity() {
        return sensitivity;
    }

    public double getSpecificity() {
        return specificity;
    }

    public TestResult(double sens, double spec) {
        this.sensitivity=sens;
        this.specificity=spec;
    }


    /**
     * TODO what if specificity and/or sensitivity is 100%?
     * @return
     */
    public double PositivelikelihoodRatio() {
        double LR = 0;
        //Put a threshold |sensitivity|<\epsilon
        if(sensitivity == 0)
            return 0;
        try{
            LR = sensitivity /(1-specificity);
            return LR;
        }
        catch (ArithmeticException e) {
            System.err.println(e);
            return 0;
        }
        catch (Exception e){
            System.err.println(e);
            return 0;
        }


    }

    public double NegativelikelihoodRatio() {

        double LR = 0;
        //Put a threshold, if |sensitivity-1|<\epsilon
        if(sensitivity == 1)
            return 0;
        try{
            LR = specificity /(1-sensitivity);
            return LR;
        }
        catch (ArithmeticException e) {
            System.err.println(e);
            return 0;
        }
        catch (Exception e){
            System.err.println(e);
            return 0;
        }


    }


}
