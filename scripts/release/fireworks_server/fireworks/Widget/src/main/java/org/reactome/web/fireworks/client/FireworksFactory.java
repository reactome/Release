package org.reactome.web.fireworks.client;

import com.google.gwt.resources.client.TextResource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class FireworksFactory {
    public static boolean EVENT_BUS_VERBOSE = false;
    public static boolean SHOW_INFO = false;
    public static boolean EDGES_SELECTABLE = true;

    public static FireworksViewer createFireworksViewer(TextResource json){
        return new FireworksViewerImpl(json.getText());
    }

    public static FireworksViewer createFireworksViewer(String json){
        return new FireworksViewerImpl(json);
    }
}
