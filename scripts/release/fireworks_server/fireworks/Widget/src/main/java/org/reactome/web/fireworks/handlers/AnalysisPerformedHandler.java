package org.reactome.web.fireworks.handlers;

import com.google.gwt.event.shared.EventHandler;
import org.reactome.web.fireworks.events.AnalysisPerformedEvent;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public interface AnalysisPerformedHandler extends EventHandler {

    void onAnalysisPerformed(AnalysisPerformedEvent e);

}
