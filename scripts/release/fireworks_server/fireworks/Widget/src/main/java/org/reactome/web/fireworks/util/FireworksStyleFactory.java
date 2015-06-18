package org.reactome.web.fireworks.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class FireworksStyleFactory {

    private static FireworksResources RESOURCES = null;

    /**
     * A ClientBundle of resources used by this module.
     */
    public interface FireworksResources extends ClientBundle {
        /**
         * The styles used in this widget.
         */
        @Source(FireworksStyle.DEFAULT_CSS)
        public FireworksStyle fireworksStyle();

    }

    /**
     * Styles used by this module.
     */
    @CssResource.ImportedWithPrefix("reactome-fireworks")
    public interface FireworksStyle extends CssResource {
        /**
         * The path to the default CSS styles used by this resource.
         */
        String DEFAULT_CSS = "org/reactome/web/fireworks/style/Fireworks.css";

        String bubble();
        String bubbleTopLeft();
        String bubbleTopRight();
        String bubbleBottomLeft();
        String bubbleBottomRight();
    }

    public static FireworksStyle getFireworksStyle(){
        if(RESOURCES==null){
            RESOURCES = GWT.create(FireworksResources.class);
            RESOURCES.fireworksStyle().ensureInjected();
        }
        return RESOURCES.fireworksStyle();
    }


}