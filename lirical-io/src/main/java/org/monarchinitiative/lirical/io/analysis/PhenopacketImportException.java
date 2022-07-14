package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.LiricalParseException;

public class PhenopacketImportException extends LiricalParseException {

    public PhenopacketImportException() {
        super();
    }

    public PhenopacketImportException(String message) {
        super(message);
    }

    public PhenopacketImportException(String message, Exception e) {
        super(message, e);
    }

    public PhenopacketImportException(String message, Throwable cause) {
        super(message, cause);
    }

    public PhenopacketImportException(Throwable cause) {
        super(cause);
    }

    protected PhenopacketImportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
