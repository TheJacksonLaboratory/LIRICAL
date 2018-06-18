package org.monarchinitiative.lr2pg.poisson;

public class MaxCountExceededException extends Exception {

    public MaxCountExceededException(int mc) {
        super(String.valueOf(mc));
    }
}
