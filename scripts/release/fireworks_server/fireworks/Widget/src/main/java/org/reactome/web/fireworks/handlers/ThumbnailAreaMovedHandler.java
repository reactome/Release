package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.NodeHoverEvent;
import org.reactome.web.fireworks.events.ThumbnailAreaMovedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface ThumbnailAreaMovedHandler extends EventHandler {

    void onThumbnailAreaMoved(ThumbnailAreaMovedEvent event);

}
