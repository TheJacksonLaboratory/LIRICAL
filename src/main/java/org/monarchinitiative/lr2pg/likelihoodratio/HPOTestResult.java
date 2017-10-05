package org.monarchinitiative.lr2pg.likelihoodratio;

/**
 * Created by ravanv on 9/27/17.
 */
public class HPOTestResult {


        private double FreqHPOTermDisease;

        private double FreqHPOTermAllDiseases;


        public double getLRNominator() {
            return FreqHPOTermDisease;
        }

        public double getLRDenominator() {
            return FreqHPOTermAllDiseases;
        }

        public HPOTestResult(double LRNominator, double LRDominator) {
            this.FreqHPOTermDisease=LRNominator;
            this.FreqHPOTermAllDiseases=LRDominator;
        }


        /**
         * TODO what if specificity and/or sensitivity is 100%?
         * @return
         */
        public double PositivelikelihoodRatio() {

            double LR = 0;
            if(FreqHPOTermDisease == 0)
                return 0;
            try{
                LR = FreqHPOTermDisease /FreqHPOTermAllDiseases;
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


        public double NegativelikelihoodRatio() {return (1-FreqHPOTermAllDiseases) /( 1- FreqHPOTermDisease);}

}
