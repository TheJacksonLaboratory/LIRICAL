package org.monarchinitiative.lirical.core.exception;

/**
 * An exception thrown by {@link org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner} if the analysis
 * cannot be run.
 * @deprecated will be moved into {@link org.monarchinitiative.lirical.core.analysis} package.
 */
// TODO - move to analysis package.
@Deprecated(forRemoval = true)
public class LiricalAnalysisException extends LiricalException {
    public LiricalAnalysisException() {
        super();
    }

    public LiricalAnalysisException(String message) {
        super(message);
    }

    public LiricalAnalysisException(String message, Exception e) {
        super(message, e);
    }

    public LiricalAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiricalAnalysisException(Throwable cause) {
        super(cause);
    }

    protected LiricalAnalysisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
