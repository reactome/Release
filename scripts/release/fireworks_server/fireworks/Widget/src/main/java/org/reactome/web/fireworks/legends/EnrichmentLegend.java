package org.reactome.web.fireworks.legends;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasGradient;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.InlineLabel;
import org.reactome.web.fireworks.analysis.EntityStatistics;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.*;
import org.reactome.web.fireworks.model.Node;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.profiles.FireworksProfile;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class EnrichmentLegend extends LegendPanel implements AnalysisPerformedHandler, AnalysisResetHandler,
        NodeHoverHandler, NodeHoverResetHandler, NodeSelectedHandler, NodeSelectedResetHandler,
        ProfileChangedHandler {

    private Canvas gradient;
    private Canvas flag;
    private Node hovered;
    private Node selected;

    public EnrichmentLegend(EventBus eventBus) {
        super(eventBus);
        this.gradient = createCanvas(30, 200);
        this.fillGradient();

        this.flag = createCanvas(50, 210);

        //Setting the legend style
        addStyleName(RESOURCES.getCSS().enrichmentLegend());

        this.add(new InlineLabel("0"), 20, 5);
        this.add(this.gradient, 10, 25);
        this.add(this.flag, 0, 20);
        this.add(new InlineLabel("0.05"), 12, 230);

        initHandlers();

        this.setVisible(false);
    }

    private Canvas createCanvas(int width, int height) {
        Canvas canvas = Canvas.createIfSupported();
        canvas.setCoordinateSpaceWidth(width);
        canvas.setCoordinateSpaceHeight(height);
        canvas.setPixelSize(width, height);
        return canvas;
    }


    @Override
    public void onNodeHover(NodeHoverEvent event) {
        if(!event.getNode().equals(this.selected)) {
            this.hovered = event.getNode();
        }
        this.draw();
    }

    @Override
    public void onAnalysisPerformed(AnalysisPerformedEvent e) {
        switch (e.getAnalysisType()){
            case OVERREPRESENTATION:
            case SPECIES_COMPARISON:
                setVisible(true);
                break;
            default:
                setVisible(false);
        }
    }

    @Override
    public void onAnalysisReset() {
        this.setVisible(false);
    }

    @Override
    public void onNodeHoverReset() {
        this.hovered = null;
        this.draw();
    }

    @Override
    public void onNodeSelected(NodeSelectedEvent event) {
        this.hovered = null;
        this.selected = event.getNode();
        this.draw();
    }

    @Override
    public void onNodeSelectionReset() {
        this.selected = null;
        this.draw();
    }

    @Override
    public void onProfileChanged(ProfileChangedEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                fillGradient();
                draw();
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.draw();
    }

    private void fillGradient(){
        FireworksProfile profile = FireworksColours.PROFILE;
        Context2d ctx = this.gradient.getContext2d();
        CanvasGradient grd = ctx.createLinearGradient(0, 0, 30, 200);
        grd.addColorStop(0, profile.getNodeEnrichmentColour(0));
        grd.addColorStop(1, profile.getNodeEnrichmentColour(0.05));

        ctx.clearRect(0, 0, this.gradient.getCoordinateSpaceWidth(), this.gradient.getCoordinateSpaceHeight());
        ctx.setFillStyle(grd);
        ctx.beginPath();
        ctx.fillRect(0, 0, 30, 200);
        ctx.closePath();
    }

    private void draw(){
        if(!this.isVisible()) return;

        Context2d ctx = this.flag.getContext2d();
        ctx.clearRect(0, 0, this.flag.getOffsetWidth(), this.flag.getOffsetHeight());

        if(this.hovered!=null){
            EntityStatistics statistics = this.hovered.getStatistics();
            if(statistics!=null){
                double pValue = statistics.getpValue();
                if(pValue<=0.05) {
                    String colour = FireworksColours.PROFILE.getNodeHighlightColour();
                    int y = (int) Math.round(200 * pValue / 0.05) + 5;
                    ctx.setFillStyle(colour);
                    ctx.setStrokeStyle(colour);
                    ctx.beginPath();
                    ctx.moveTo(5, y - 5);
                    ctx.lineTo(10, y);
                    ctx.lineTo(5, y + 5);
                    ctx.lineTo(5, y - 5);
                    ctx.fill();
                    ctx.stroke();
                    ctx.closePath();

                    ctx.beginPath();
                    ctx.moveTo(10, y);
                    ctx.lineTo(40, y);
                    ctx.setStrokeStyle("yellow");
                    ctx.stroke();
                    ctx.closePath();
                }
            }
        }

        if(this.selected!=null){
            EntityStatistics statistics = this.selected.getStatistics();
            if(statistics!=null){
                double pValue = statistics.getpValue();
                if(pValue<=0.05) {
                    String colour = FireworksColours.PROFILE.getNodeSelectionColour();
                    int y = (int) Math.round(200 * pValue / 0.05) + 5;
                    ctx.setFillStyle(colour);
                    ctx.setStrokeStyle(colour);
                    ctx.beginPath();
                    ctx.moveTo(45, y - 5);
                    ctx.lineTo(40, y);
                    ctx.lineTo(45, y + 5);
                    ctx.lineTo(45, y - 5);
                    ctx.fill();
                    ctx.stroke();
                    ctx.closePath();

                    ctx.beginPath();
                    ctx.moveTo(10, y);
                    ctx.lineTo(40, y);
                    ctx.stroke();
                    ctx.closePath();
                }
            }
        }
    }

    private void initHandlers() {
        this.eventBus.addHandler(AnalysisPerformedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
        this.eventBus.addHandler(NodeHoverEvent.TYPE, this);
        this.eventBus.addHandler(NodeHoverResetEvent.TYPE, this);
        this.eventBus.addHandler(NodeSelectedEvent.TYPE, this);
        this.eventBus.addHandler(NodeSelectedResetEvent.TYPE, this);
        this.eventBus.addHandler(ProfileChangedEvent.TYPE, this);
    }

}
