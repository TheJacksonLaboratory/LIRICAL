package org.monarchinitiative.lirical.poisson;

public class MaxCountExceededException extends Exception {

    public MaxCountExceededException(int mc) {
        super(String.valueOf(mc));
    }
}
