package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.ExpressionColumnChangedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface ExpressionColumnChangedHandler extends EventHandler {

    void onExpressionColumnChanged(ExpressionColumnChangedEvent e);

}
