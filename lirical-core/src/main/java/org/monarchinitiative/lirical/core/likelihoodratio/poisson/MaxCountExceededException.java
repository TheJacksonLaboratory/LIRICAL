package org.monarchinitiative.lirical.core.likelihoodratio.poisson;

public class MaxCountExceededException extends Exception {

    public MaxCountExceededException(int mc) {
        super(String.valueOf(mc));
    }
}
