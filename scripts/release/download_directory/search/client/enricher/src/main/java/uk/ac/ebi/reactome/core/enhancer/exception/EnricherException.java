package uk.ac.ebi.reactome.core.enhancer.exception;

/**
 * Created by flo on 6/30/14.
 */
public class EnricherException extends Exception{
    protected EnricherException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public EnricherException() {
        super();
    }

    public EnricherException(String message) {
        super(message);
    }

    public EnricherException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnricherException(Throwable cause) {
        super(cause);
    }
}
