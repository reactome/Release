package org.reactome.web.fireworks.legends;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasGradient;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.InlineLabel;
import org.reactome.web.fireworks.analysis.EntityStatistics;
import org.reactome.web.fireworks.analysis.ExpressionSummary;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.*;
import org.reactome.web.fireworks.model.Node;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.profiles.FireworksProfile;
import org.reactome.web.fireworks.util.gradient.ThreeColorGradient;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ExpressionLegend extends LegendPanel implements AnalysisPerformedHandler, AnalysisResetHandler,
        NodeHoverHandler, NodeHoverResetHandler, NodeSelectedHandler, NodeSelectedResetHandler,
        ExpressionColumnChangedHandler, ProfileChangedHandler {

    private Canvas gradient;
    private Canvas flag;
    private Node hovered;
    private Node selected;

    private double min;
    private double max;
    private int column = 0;

    private InlineLabel topLabel;
    private InlineLabel bottomLabel;

    public ExpressionLegend(EventBus eventBus) {
        super(eventBus);
        this.gradient = createCanvas(30, 200);
        this.flag = createCanvas(50, 210);

        //Setting the legend style
        addStyleName(RESOURCES.getCSS().expressionLegend());

        fillGradient();

        this.topLabel = new InlineLabel("");
        this.add(this.topLabel, 5, 5);

        this.add(this.gradient, 10, 25);
        this.add(this.flag, 0, 20);

        this.bottomLabel = new InlineLabel("");
        this.add(this.bottomLabel, 5, 230);

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
            case EXPRESSION:
                ExpressionSummary es = e.getExpressionSummary();
                if(es!=null){
                    this.min = es.getMin();
                    this.max = es.getMax();
                    this.topLabel.setText( NumberFormat.getFormat("#.##E0").format(max) );
                    this.bottomLabel.setText( NumberFormat.getFormat("#.##E0").format(min) );
                }
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
    public void onExpressionColumnChanged(ExpressionColumnChangedEvent e) {
        this.column = e.getColumn();
        draw();
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

        grd.addColorStop(0, profile.getNodeExpressionColour(0));
        grd.addColorStop(0.5,profile.getNodeExpressionColour(0.5));
        grd.addColorStop(1, profile.getNodeExpressionColour(1));

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
            if(statistics!=null && statistics.getpValue()<0.05){
                if(statistics.getExp()!=null) {
                    String colour = FireworksColours.PROFILE.getNodeHighlightColour();
                    double expression = statistics.getExp().get(this.column);
                    double p = ThreeColorGradient.getPercentage(expression, this.min, this.max);
                    int y = (int) Math.round(200 * p) + 5;
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
                    ctx.stroke();
                    ctx.closePath();
                }
            }
        }

        if(this.selected!=null){
            EntityStatistics statistics = this.selected.getStatistics();
            if(statistics!=null && statistics.getpValue()<0.05){
                if(statistics.getExp()!=null) {
                    String colour = FireworksColours.PROFILE.getNodeSelectionColour();
                    double expression = statistics.getExp().get(this.column);
                    double p = ThreeColorGradient.getPercentage(expression, this.min, this.max);
                    int y = (int) Math.round(200 * p) + 5;
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
        this.eventBus.addHandler(ExpressionColumnChangedEvent.TYPE, this);
        this.eventBus.addHandler(ProfileChangedEvent.TYPE, this);
    }

}
