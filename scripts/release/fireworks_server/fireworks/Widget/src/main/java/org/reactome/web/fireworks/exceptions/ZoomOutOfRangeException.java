package org.reactome.web.fireworks.exceptions;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
@Deprecated
public class ZoomOutOfRangeException extends RuntimeException {

    public ZoomOutOfRangeException() {
        super("Zoom out of range");
    }

}
