package org.reactome.core.controller;

/**
 * When a WS client passes some query that cannot be handled by the server,
 * a QueryNotSupportedException will be thrown.
 *
 * @@author guanming
 */
public class QueryNotSupportedException extends ReactomeRemoteException {

    public QueryNotSupportedException() {
    }

    public QueryNotSupportedException(String message) {
        super(message);
    }

}
