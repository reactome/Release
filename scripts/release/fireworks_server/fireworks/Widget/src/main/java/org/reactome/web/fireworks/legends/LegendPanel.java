package org.reactome.web.fireworks.legends;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AbsolutePanel;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class LegendPanel extends AbsolutePanel {

    public static LegendResources RESOURCES;
    static {
        RESOURCES = GWT.create(LegendResources.class);
        RESOURCES.getCSS().ensureInjected();
    }

    /**
     * A ClientBundle of resources used by this widget.
     */
    public interface LegendResources extends ClientBundle {
        /**
         * The styles used in this widget.
         */
        @Source(LegendPanelCSS.CSS)
        LegendPanelCSS getCSS();

        @Source("images/close_clicked.png")
        ImageResource closeClicked();

        @Source("images/close_disabled.png")
        ImageResource closeDisabled();

        @Source("images/close_hovered.png")
        ImageResource closeHovered();

        @Source("images/close_normal.png")
        ImageResource closeNormal();

        @Source("images/forward_clicked.png")
        ImageResource forwardClicked();

        @Source("images/forward_disabled.png")
        ImageResource forwardDisabled();

        @Source("images/forward_hovered.png")
        ImageResource forwardHovered();

        @Source("images/forward_normal.png")
        ImageResource forwardNormal();

        @Source("images/play_clicked.png")
        ImageResource playClicked();

        @Source("images/play_disabled.png")
        ImageResource playDisabled();

        @Source("images/play_hovered.png")
        ImageResource playHovered();

        @Source("images/play_normal.png")
        ImageResource playNormal();

        @Source("images/pause_clicked.png")
        ImageResource pauseClicked();

        @Source("images/pause_disabled.png")
        ImageResource pauseDisabled();

        @Source("images/pause_hovered.png")
        ImageResource pauseHovered();

        @Source("images/pause_normal.png")
        ImageResource pauseNormal();

        @Source("images/rewind_clicked.png")
        ImageResource rewindClicked();

        @Source("images/rewind_disabled.png")
        ImageResource rewindDisabled();

        @Source("images/rewind_hovered.png")
        ImageResource rewindHovered();

        @Source("images/rewind_normal.png")
        ImageResource rewindNormal();

        @Source("images/speed_clicked.png")
        ImageResource speedClicked();

        @Source("images/speed_disabled.png")
        ImageResource speedDisabled();

        @Source("images/speed_hovered.png")
        ImageResource speedHovered();

        @Source("images/speed_normal.png")
        ImageResource speedNormal();
    }

    /**
     * Styles used by this widget.
     */
    @CssResource.ImportedWithPrefix("fireworks-LegendPanel")
    public interface LegendPanelCSS extends CssResource {
        /**
         * The path to the default CSS styles used by this resource.
         */
        String CSS = "org/reactome/web/fireworks/legends/LegendPanel.css";

        String legendPanel();

        String analysisControl();

        String close();

        String enrichmentLegend();

        String enrichmentControl();

        String expressionLegend();

        String expressionControl();

        String forward();

        String pause();

        String play();

        String rewind();

        String slide();

        String speed();
    }

    protected EventBus eventBus;

    public LegendPanel(EventBus eventBus) {
        this.eventBus = eventBus;
        //Setting the legend style
        getElement().getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
        setStyleName(RESOURCES.getCSS().legendPanel());
    }
}
