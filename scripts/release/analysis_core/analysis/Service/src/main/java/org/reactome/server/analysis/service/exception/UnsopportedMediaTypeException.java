package org.reactome.server.analysis.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class UnsopportedMediaTypeException extends RuntimeException {

    public UnsopportedMediaTypeException() {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
    }

}
