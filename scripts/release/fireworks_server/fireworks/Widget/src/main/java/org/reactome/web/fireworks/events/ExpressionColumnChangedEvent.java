package org.reactome.web.fireworks.events;

import com.google.gwt.event.shared.GwtEvent;
import org.reactome.web.fireworks.handlers.ExpressionColumnChangedHandler;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExpressionColumnChangedEvent extends GwtEvent<ExpressionColumnChangedHandler> {
    public static Type<ExpressionColumnChangedHandler> TYPE = new Type<ExpressionColumnChangedHandler>();

    private Integer column;

    public ExpressionColumnChangedEvent(Integer column) {
        this.column = column;
    }

    @Override
    public Type<ExpressionColumnChangedHandler> getAssociatedType() {
        return TYPE;
    }

    public Integer getColumn() {
        return column;
    }

    @Override
    protected void dispatch(ExpressionColumnChangedHandler handler) {
        handler.onExpressionColumnChanged(this);
    }

    @Override
    public String toString() {
        return "ExpressionColumnChangedEvent{" +
                "column=" + column +
                '}';
    }
}
