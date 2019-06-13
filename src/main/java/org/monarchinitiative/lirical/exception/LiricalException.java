package org.monarchinitiative.lirical.exception;

public class LiricalException extends Exception {


    public LiricalException() { super(); }
    public LiricalException(String message) { super(message);}
    public LiricalException(String message, Exception e) { super(message,e);}


}
