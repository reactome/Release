package org.reactome.server.analysis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@ResponseStatus(value = HttpStatus.REQUEST_ENTITY_TOO_LARGE)
public class RequestEntityTooLargeException extends RuntimeException {

    public RequestEntityTooLargeException() {
        super();
    }
}
