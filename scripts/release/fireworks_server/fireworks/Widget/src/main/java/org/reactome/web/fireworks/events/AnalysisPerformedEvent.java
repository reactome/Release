package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.analysis.AnalysisType;
import org.reactome.web.fireworks.analysis.ExpressionSummary;
import org.reactome.web.fireworks.analysis.PathwayBase;
import org.reactome.web.fireworks.analysis.SpeciesFilteredResult;
import org.reactome.web.fireworks.handlers.AnalysisPerformedHandler;

import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class AnalysisPerformedEvent extends GwtEvent<AnalysisPerformedHandler> {
    public static Type<AnalysisPerformedHandler> TYPE = new Type<AnalysisPerformedHandler>();

    private SpeciesFilteredResult result;

    public AnalysisPerformedEvent(SpeciesFilteredResult result) {
        this.result = result;
    }

    @Override
    public Type<AnalysisPerformedHandler> getAssociatedType() {
        return TYPE;
    }

    public AnalysisType getAnalysisType() {
        return AnalysisType.getType(this.result.getType());
    }

    public List<PathwayBase> getPathways() {
        return this.result.getPathways();
    }

    public ExpressionSummary getExpressionSummary(){
        return this.result.getExpressionSummary();
    }

    @Override
    protected void dispatch(AnalysisPerformedHandler handler) {
        handler.onAnalysisPerformed(this);
    }

    @Override
    public String toString() {
        return "AnalysisPerformedEvent{" +
                "analysisType='" + getAnalysisType() + '\'' +
                ", pathwaysHit=" + getPathways().size() +
                '}';
    }
}
