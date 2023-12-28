package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.exception.LiricalException;

/**
 * An exception thrown when user-provided input is invalid.
 */
// TODO - move to CLI after removing AnalysisDataParser.
public class LiricalParseException extends LiricalException {

    public LiricalParseException() {
        super();
    }

    public LiricalParseException(String message) {
        super(message);
    }

    public LiricalParseException(String message, Exception e) {
        super(message, e);
    }

    public LiricalParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiricalParseException(Throwable cause) {
        super(cause);
    }

    protected LiricalParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
