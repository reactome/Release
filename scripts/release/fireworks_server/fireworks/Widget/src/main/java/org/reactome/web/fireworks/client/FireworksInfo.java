package org.reactome.web.fireworks.client;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.*;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.*;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class FireworksInfo extends AbsolutePanel implements FireworksZoomHandler,
        NodeHoverHandler, NodeHoverResetHandler, NodeSelectedHandler, NodeSelectedResetHandler {
    private EventBus eventBus;

    private InlineLabel nodes;
    private InlineLabel hovered;
    private InlineLabel selected;
    private InlineLabel factor;
    private InlineLabel pValue;

    public FireworksInfo(EventBus eventBus) {
        this.eventBus = eventBus;
        this.setStyle(300, 100);

        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(getInfoPanel("Visible pathways", this.nodes = new InlineLabel("loading...")));
        verticalPanel.add(getInfoPanel("Hovered", this.hovered = new InlineLabel("none")));
        verticalPanel.add(getInfoPanel("Selected", this.selected = new InlineLabel("none")));
        verticalPanel.add(getInfoPanel("Factor", this.factor = new InlineLabel("1")));
        verticalPanel.add(getInfoPanel("pValue", this.pValue = new InlineLabel("none")));

        add(verticalPanel);

        initHandlers();
    }

    public void setNodes(int nodes){
        this.nodes.setText("" + nodes);
    }

    private void initHandlers(){
        this.eventBus.addHandler(FireworksZoomEvent.TYPE, this);
        this.eventBus.addHandler(NodeHoverEvent.TYPE, this);
        this.eventBus.addHandler(NodeHoverResetEvent.TYPE, this);
        this.eventBus.addHandler(NodeSelectedEvent.TYPE, this);
        this.eventBus.addHandler(NodeSelectedResetEvent.TYPE, this);
    }

    private Widget getInfoPanel(String title, Widget holder){
        FlowPanel fp = new FlowPanel();
        fp.add(new InlineLabel(title + ": "));
        fp.add(holder);
        return fp;
    }

    private void setStyle(double w, double h){
        this.setWidth(w + "px"); this.setHeight(h + "px");
        Style style = this.getElement().getStyle();
        style.setBackgroundColor("white");
        style.setBorderStyle(Style.BorderStyle.SOLID);
        style.setBorderWidth(1, Style.Unit.PX);
        style.setBorderColor("grey");
        style.setPosition(Style.Position.ABSOLUTE);
        style.setBottom(0, Style.Unit.PX);
        style.setRight(0, Style.Unit.PX);

        style.setOverflow(Style.Overflow.SCROLL);
    }

    @Override
    public void onFireworksZoomChanged(FireworksZoomEvent event) {
        this.factor.setText("" + event.getStatus().getFactor());
    }

    @Override
    public void onNodeHover(NodeHoverEvent event) {
        this.hovered.setText(event.getNode().getStId());
        Node node = event.getNode();
        if(node.getStatistics()!=null){
            this.pValue.setText(NumberFormat.getFormat("#.##E0").format(node.getStatistics().getpValue()));
        }
    }

    @Override
    public void onNodeHoverReset() {
        this.hovered.setText("");
        this.pValue.setText("");
    }

    @Override
    public void onNodeSelected(NodeSelectedEvent event) {
        this.selected.setText(event.getNode().getStId());
    }

    @Override
    public void onNodeSelectionReset() {
        this.selected.setText("");
    }
}
