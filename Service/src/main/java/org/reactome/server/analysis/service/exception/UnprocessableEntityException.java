package org.reactome.server.analysis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Unprocessable Entity")
public class UnprocessableEntityException extends RuntimeException {

    public UnprocessableEntityException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase());
    }
}
