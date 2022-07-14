package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.monarchinitiative.lirical.core.exception.LiricalException;

public class ExomiserResourceException extends LiricalException {

    public ExomiserResourceException() {
        super();
    }

    public ExomiserResourceException(String message) {
        super(message);
    }

    public ExomiserResourceException(String message, Exception e) {
        super(message, e);
    }

    public ExomiserResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExomiserResourceException(Throwable cause) {
        super(cause);
    }

    protected ExomiserResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
