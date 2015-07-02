package uk.ac.ebi.reactome.solr.indexer.exception;

/**
 * Exception Class for Indexer and Converter
 * Created by flo on 4/29/14.
 */

public class IndexerException extends Exception {

    public IndexerException() {
        super();
    }

    public IndexerException(String message) {
        super(message);
    }

    public IndexerException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexerException(Throwable cause) {
        super(cause);
    }

    protected IndexerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
