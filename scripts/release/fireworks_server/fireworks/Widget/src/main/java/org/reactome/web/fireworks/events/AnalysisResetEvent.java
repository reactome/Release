package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.AnalysisResetHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisResetEvent extends GwtEvent<AnalysisResetHandler> {
    public static Type<AnalysisResetHandler> TYPE = new Type<AnalysisResetHandler>();

    @Override
    public Type<AnalysisResetHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(AnalysisResetHandler handler) {
        handler.onAnalysisReset();
    }

    @Override
    public String toString(){
        return "AnalysisResetEvent{}";
    }
}
