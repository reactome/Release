package uk.ac.ebi.reactome.core.exception;

/**
 * Error Object to be returned when an error occurs in the restFul Service
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class ErrorInfo {

    private final String header;
    private final StringBuffer url;
    private final String ex;
    private final StackTraceElement[] stacktrace;

    public ErrorInfo(String header, StringBuffer url, Exception ex) {
        this.header = header;
        this.url = url;
        this.ex = ex.getLocalizedMessage();
        this.stacktrace = ex.getStackTrace();
    }

    public String getHeader() {
        return header;
    }

    public StringBuffer getUrl() {
        return url;
    }

    public String getEx() {
        return ex;
    }

    public StackTraceElement[] getStacktrace() {
        return stacktrace;
    }
}
