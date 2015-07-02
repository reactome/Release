package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.CanvasNotSupportedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface CanvasNotSupportedHandler extends EventHandler {

    void onCanvasNotSupported(CanvasNotSupportedEvent event);

}
