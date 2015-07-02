package uk.ac.ebi.reactome.solr.core.exception;

/**
 * TODO IMPLEMENT
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class SolrSearcherException extends Exception {

    public SolrSearcherException(String message, Throwable cause) {
        super(message, cause);
    }

    public SolrSearcherException(String message) { super(message);
    }
}
