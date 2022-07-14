package org.monarchinitiative.lirical.core.likelihoodratio.poisson;

public class NumberIsTooSmallException extends Exception {
    public NumberIsTooSmallException(double mc, double threshold) {
        super(String.format("%f exceeded %f",mc,threshold));
    }
}
