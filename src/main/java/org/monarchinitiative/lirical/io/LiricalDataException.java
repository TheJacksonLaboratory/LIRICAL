package org.monarchinitiative.lirical.io;

import org.monarchinitiative.lirical.exception.LiricalException;

/**
 * An exception thrown when data error (missing resource, invalid resource file, etc.) is detected.
 */
public class LiricalDataException extends LiricalException {

    public LiricalDataException() {
        super();
    }

    public LiricalDataException(String message) {
        super(message);
    }

    public LiricalDataException(String message, Exception e) {
        super(message, e);
    }

    public LiricalDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiricalDataException(Throwable cause) {
        super(cause);
    }

    protected LiricalDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
