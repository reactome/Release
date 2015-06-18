package org.reactome.web.fireworks.util;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.reactome.web.fireworks.model.Node;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Tooltip extends SimplePanel {

    private static Tooltip tooltip;
    private static FireworksStyleFactory.FireworksStyle style = FireworksStyleFactory.getFireworksStyle();

    private boolean preventShowing = false;

    public static Tooltip getTooltip(){
        if(tooltip==null){
            tooltip = new Tooltip();
        }
        return tooltip;
    }

    public void hide(){
        setVisible(false);
    }

    @Override
    public void add(Widget w) {
        this.clear();
        super.add(w);
        this.setStyleName(style.bubble());
    }

    public void show(final TooltipContainer container, final Node node) {
        if(preventShowing) return; //If the node is not visible, preventShowing has to be set to false previously

        this.add(new PathwayInfoPanel(node));
        container.add(this, -1000, -1000); //Adding it where is not visible
        container.getElement().appendChild(this.getElement());
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                setPositionAndShow(container, (int) node.getX(), (int) node.getY(), node.getSize());
            }
        });
    }

    public void setPositionAndShow(TooltipContainer container, int offsetX, int offsetY, double nodeSize) {
        this.setVisible(true);
        int left; int top; int size = (int) Math.ceil(nodeSize) + 4;
        if(offsetX < container.getWidth()/2) {
            left = offsetX - 12;
            if((offsetY - size) < 50){
                top = offsetY + size;
                this.addStyleName(style.bubbleTopLeft());
            }else{
                top = offsetY - getOffsetHeight() - size;
                this.addStyleName(style.bubbleBottomLeft());
            }
        }else{
            left = offsetX - getOffsetWidth() + 12;
            if((offsetY - size) < 50){
                top = offsetY + size;
                this.addStyleName(style.bubbleTopRight());
            }else{
                top = offsetY - getOffsetHeight() - size;
                this.addStyleName(style.bubbleBottomRight());
            }
        }
        this.setPosition(left, top);
    }

    public void setPreventShowing(boolean preventShowing) {
        this.preventShowing = preventShowing;
        if(preventShowing && isVisible()) this.hide();
    }

    private void setPosition(int left, int top){
        Element elem = getElement();
        elem.getStyle().setPropertyPx("left", left);
        elem.getStyle().setPropertyPx("top", top);
    }
}