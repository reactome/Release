package org.reactome.web.fireworks.client;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.AbsolutePanel;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.AnalysisPerformedHandler;
import org.reactome.web.fireworks.handlers.AnalysisResetHandler;
import org.reactome.web.fireworks.handlers.ExpressionColumnChangedHandler;
import org.reactome.web.fireworks.handlers.ProfileChangedHandler;
import org.reactome.web.fireworks.model.AnalysisInfo;
import org.reactome.web.fireworks.model.Edge;
import org.reactome.web.fireworks.model.Graph;
import org.reactome.web.fireworks.model.Node;
import org.reactome.web.fireworks.profiles.FireworksColours;
import org.reactome.web.fireworks.util.Coordinate;

import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class FireworksThumbnail extends AbsolutePanel implements HasHandlers, MouseDownHandler, MouseMoveHandler, MouseUpHandler, MouseOutHandler,
        AnalysisPerformedHandler, AnalysisResetHandler, ExpressionColumnChangedHandler, ProfileChangedHandler {

    private static final int HEIGHT = 75;


    private EventBus eventBus;
    private Graph graph;
    private final AnalysisInfo analysisInfo = new AnalysisInfo();

    private double factor;

    private Canvas thumbnail;
    private Canvas highlight;
    private Canvas selection;
    private Canvas frame;

    private Coordinate from;
    private Coordinate to;

    private Coordinate mouseDown = null;
    private Coordinate delta = null;

    FireworksThumbnail(EventBus eventBus, Graph graph) {
        this.eventBus = eventBus;
        this.graph = graph;
        this.factor = HEIGHT / graph.getMaxY();
        int width = (int) Math.ceil((graph.getMaxX() + 25) * factor);

        this.thumbnail = this.createCanvas(width, HEIGHT);
        this.highlight = this.createCanvas(width, HEIGHT);
        this.selection = this.createCanvas(width, HEIGHT);
        this.frame = this.createCanvas(width, HEIGHT);

        this.setStyle(width, HEIGHT);
        this.addHandlers();
        this.drawThumbnail();
    }

    public void clearHighlights(){
        this.cleanCanvas(this.highlight);
    }

    public void clearSelection(){
        this.cleanCanvas(this.selection);
    }

    @Override
    public void onAnalysisPerformed(AnalysisPerformedEvent e) {
        this.analysisInfo.setInfo(e.getAnalysisType(), e.getExpressionSummary());
        this.drawThumbnail();
    }

    @Override
    public void onAnalysisReset() {
        this.analysisInfo.reset();
        this.drawThumbnail();
    }

    @Override
    public void onExpressionColumnChanged(ExpressionColumnChangedEvent e) {
        this.analysisInfo.setColumn(e.getColumn());
        this.drawThumbnail();
    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
        event.stopPropagation(); event.preventDefault();
        Element elem = event.getRelativeElement();
        Coordinate c = new Coordinate(event.getRelativeX(elem), event.getRelativeY(elem));
        if(isMouseInVisibleArea(c)){
            this.mouseDown = c;
            this.delta = mouseDown.minus(from);
        }
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        event.stopPropagation(); event.preventDefault();
        Element elem = event.getRelativeElement();
        Coordinate mouse = new Coordinate(event.getRelativeX(elem), event.getRelativeY(elem));
        if(mouseDown!=null){
            if(from!=null && to!=null) {
                this.mouseDown = mouse;
                Coordinate translation = from.minus(mouse.minus(delta));
                Coordinate aux = from.minus(translation).divide(factor);
                this.eventBus.fireEventFromSource(new ThumbnailAreaMovedEvent(aux), this);
            }
        }else{
            if(isMouseInVisibleArea(mouse)){
                getElement().getStyle().setCursor(Style.Cursor.MOVE);
            }else{
                getElement().getStyle().setCursor(Style.Cursor.DEFAULT);
            }
        }
    }

    @Override
    public void onMouseUp(MouseUpEvent event) {
        event.stopPropagation(); event.preventDefault();
        this.mouseDown = null;
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        event.stopPropagation(); event.preventDefault();
        this.mouseDown = null;
    }

    @Override
    public void onProfileChanged(ProfileChangedEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                drawThumbnail();
            }
        });
    }

    void highlightEdges(Node node, Set<Edge> edges){
        cleanCanvas(this.highlight);
        Context2d ctx = this.highlight.getContext2d();
        String color = FireworksColours.PROFILE.getThumbnailHighlightColour();
        ctx.setStrokeStyle(color);
        for (Edge edge : edges) {
            edge.drawThumbnail(ctx, this.factor);
        }
        if(edges.isEmpty()) {
            ctx.setFillStyle(color);
            node.drawThumbnail(ctx, this.factor);
        }
    }

    void selectNode(Node node, Set<Edge> edges){
        cleanCanvas(this.selection);
        Context2d ctx = this.selection.getContext2d();
        String color = FireworksColours.PROFILE.getThumbnailSelectionColour();
        ctx.setStrokeStyle(color);
        for (Edge edge : edges) {
            edge.drawThumbnail(ctx, this.factor);
        }
        if(edges.isEmpty()) {
            ctx.setFillStyle(color); //"#000099" --> Here we had a slightly more pale blue before
            node.drawThumbnail(ctx, this.factor);
        }
    }

    void setVisibleArea(Coordinate from, double w, double h){
        this.from = from.multiply(factor);
        this.to = (new Coordinate(from.getX() + w, from.getY() + h)).multiply(factor);
        drawVisibleArea();
    }

    private void addHandlers(){
        this.frame.addMouseDownHandler(this);
        this.frame.addMouseMoveHandler(this);
        this.frame.addMouseUpHandler(this);
        this.frame.addMouseOutHandler(this);
        this.eventBus.addHandler(AnalysisPerformedEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
        this.eventBus.addHandler(ExpressionColumnChangedEvent.TYPE, this);
        this.eventBus.addHandler(ProfileChangedEvent.TYPE, this);
    }

    private void drawThumbnail(){
        cleanCanvas(this.thumbnail);

        int column = this.analysisInfo.getColumn();
        Context2d ctx = this.thumbnail.getContext2d();
        String color = FireworksColours.PROFILE.getThumbnailInitialColour();
        ctx.setStrokeStyle(color);
        for (Edge edge : this.graph.getEdges()) {
            switch (this.analysisInfo.getType()) {
                case SPECIES_COMPARISON:
                case OVERREPRESENTATION:
                    ctx.setStrokeStyle(edge.getColour());
                    edge.drawThumbnail(ctx, this.factor);
                    break;
                case EXPRESSION:
                    ctx.setStrokeStyle(edge.getExpressionColor(column));
                    edge.drawThumbnail(ctx, this.factor);
                    break;
                case NONE:
                default:
                    edge.drawThumbnail(ctx, this.factor);
            }
        }
    }

    private void drawVisibleArea(){
        Coordinate delta = to.minus(from);
        cleanCanvas(this.frame);
        Context2d ctx = this.frame.getContext2d();
        ctx.setFillStyle("rgba(211, 211, 211, 0.4)");
        ctx.setStrokeStyle("#555555");  //"#AAAAAA"
        ctx.fillRect(0, 0, this.frame.getOffsetWidth(), this.frame.getOffsetHeight());
        ctx.strokeRect(this.from.getX(), this.from.getY(), delta.getX(), delta.getY());
        ctx.clearRect(this.from.getX(), this.from.getY(), delta.getX(), delta.getY());
    }

    private void cleanCanvas(Canvas canvas){
        canvas.getContext2d().clearRect(0, 0, canvas.getOffsetWidth(), canvas.getOffsetHeight());
    }

    private Canvas createCanvas(int width, int height) {
        Canvas canvas = Canvas.createIfSupported();
        canvas.setCoordinateSpaceWidth(width);
        canvas.setCoordinateSpaceHeight(height);
        canvas.setPixelSize(width, height);
        this.add(canvas, 0, 0);
        return canvas;
    }

    private boolean isMouseInVisibleArea(Coordinate mouse){
        return  mouse.getX() >= from.getX()
                && mouse.getY() >= from.getY()
                && mouse.getX() <= to.getX()
                && mouse.getY() <= to.getY();
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
    }
}