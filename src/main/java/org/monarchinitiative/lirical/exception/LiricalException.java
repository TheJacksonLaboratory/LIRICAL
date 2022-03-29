package org.monarchinitiative.lirical.exception;

public class LiricalException extends Exception {

    public LiricalException() { super(); }
    public LiricalException(String message) { super(message);}
    public LiricalException(String message, Exception e) { super(message,e);}
    public LiricalException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiricalException(Throwable cause) {
        super(cause);
    }

    protected LiricalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
