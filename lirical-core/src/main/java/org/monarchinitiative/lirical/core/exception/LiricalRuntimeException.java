package org.monarchinitiative.lirical.core.exception;

public class LiricalRuntimeException extends RuntimeException {
    public LiricalRuntimeException() { super();}
    public LiricalRuntimeException(String msg) { super(msg);}
    public LiricalRuntimeException(String message, Throwable cause) { super(message, cause);}
    public LiricalRuntimeException(Throwable cause) { super(cause);}
}
