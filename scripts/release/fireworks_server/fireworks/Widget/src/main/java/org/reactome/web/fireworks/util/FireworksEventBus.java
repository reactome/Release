package org.reactome.web.fireworks.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import org.reactome.web.fireworks.client.FireworksFactory;

import java.util.Date;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksEventBus extends SimpleEventBus {

    private DateTimeFormat fmt = DateTimeFormat.getFormat("HH:mm:ss");

    @Override
    public void fireEvent(GwtEvent<?> event) {
//        super.fireEvent(event);
        String msg = "Please do not use fireEvent. Use fireEventFromSource instead.";
        System.err.println(msg);
//        throw new RuntimeException(msg);
    }

    @Override
    public void fireEventFromSource(GwtEvent<?> event, Object source) {
        super.fireEventFromSource(event, source);
        if(FireworksFactory.EVENT_BUS_VERBOSE && !GWT.isScript()) {
            System.out.println(
                    this.fmt.format(new Date()) + " " +
                    source.getClass().getSimpleName() + " >> " +
                    event
            );
        }
    }
}