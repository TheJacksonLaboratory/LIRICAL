package org.monarchinitiative.lr2pg.exception;

public class Lr2pgException extends Exception {


    public Lr2pgException() { super(); }
    public Lr2pgException(String message) { super(message);}
    public Lr2pgException(String message, Exception e) { super(message,e);}


}
