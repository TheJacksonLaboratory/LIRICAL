package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;

public class ExomiserResourceRuntimeException extends LiricalRuntimeException {
    public ExomiserResourceRuntimeException() {
        super();
    }

    public ExomiserResourceRuntimeException(String msg) {
        super(msg);
    }

    public ExomiserResourceRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExomiserResourceRuntimeException(Throwable cause) {
        super(cause);
    }
}
