package org.monarchinitiative.lirical.core.likelihoodratio.poisson;

import org.monarchinitiative.lirical.core.exception.LiricalException;

class NumberIsTooSmallException extends LiricalException {
    public NumberIsTooSmallException(double mc, double threshold) {
        super(String.format("%f exceeded %f",mc,threshold));
    }
}
