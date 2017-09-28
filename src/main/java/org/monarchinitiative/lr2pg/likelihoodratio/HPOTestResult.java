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
        public double PositivelikelihoodRatio() {return FreqHPOTermDisease /FreqHPOTermAllDiseases;}

        public double NegativelikelihoodRatio() {return (1-FreqHPOTermAllDiseases) /( 1- FreqHPOTermDisease);}

}
