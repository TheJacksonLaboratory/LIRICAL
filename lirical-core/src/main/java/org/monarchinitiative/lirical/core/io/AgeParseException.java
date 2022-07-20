package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.lirical.core.exception.LiricalException;

/**
 * Thrown when {@link AgeParser} cannot parse <code>payload</code> into
 * an {@link org.monarchinitiative.phenol.annotations.base.temporal.Age}.
 */
public class AgeParseException extends LiricalException {

    public AgeParseException() {
        super();
    }

    public AgeParseException(String message) {
        super(message);
    }

    public AgeParseException(String message, Exception e) {
        super(message, e);
    }

    public AgeParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgeParseException(Throwable cause) {
        super(cause);
    }

    protected AgeParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
