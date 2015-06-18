package org.reactome.web.fireworks.client;

import com.google.gwt.event.shared.EventBus;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.FireworksResizeHandler;
import org.reactome.web.fireworks.handlers.ThumbnailAreaMovedHandler;
import org.reactome.web.fireworks.model.Edge;
import org.reactome.web.fireworks.model.FireworksStatus;
import org.reactome.web.fireworks.model.Graph;
import org.reactome.web.fireworks.model.Node;
import org.reactome.web.fireworks.util.*;
import uk.ac.ebi.pwp.structures.quadtree.interfaces.QuadTreeBox;
import uk.ac.ebi.pwp.structures.quadtree.model.Box;
import uk.ac.ebi.pwp.structures.quadtree.model.QuadTree2D;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class FireworksViewerManager implements MovementAnimation.FireworksZoomAnimationHandler,
        FireworksResizeHandler, ThumbnailAreaMovedHandler, FocusAnimationHandler {
    static final int ZOOM_MIN = 1;
    static final int ZOOM_MAX = 100;
    static final double ZOOM_FACTOR = 0.25; //0.50;

    private EventBus eventBus;

    //The max number of elements for every QuadTree quadrant node
    static final int NUMBER_OF_ELEMENTS = 500;

    //Several instances could have different values
    private boolean EDGES_SELECTABLE = FireworksFactory.EDGES_SELECTABLE;

    private Graph graph;

    private QuadTree2D<QuadTreeBox> quadTree;

    private double width;
    private double height;

    private FireworksStatus currentStatus;
    private Node nodeToOpen = null;

    MovementAnimation movementAnimation;

    FireworksViewerManager(EventBus eventBus, Graph graph) {
        this.eventBus = eventBus;
        this.currentStatus = new FireworksStatus(1.0, new Coordinate(0,0));
        this.setGraph(graph);
        this.initHandlers();
    }

    @Override
    public void onFireworksResized(FireworksResizedEvent event) {
        this.width = event.getWidth();
        this.height = event.getHeight();
        this.fireFireworksVisibleAreaChangedEvent();
    }

    @Override
    public void onThumbnailAreaMoved(ThumbnailAreaMovedEvent event) {
        Coordinate delta = this.currentStatus.getDistance(event.getCoordinate());
        translate(delta.getX(), delta.getY());
    }

    public void onMouseScrolled(int delta, Coordinate coordinate) {
        double factor = this.currentStatus.getFactor() - delta * ZOOM_FACTOR;
        //Check boundaries only when the user scrolls
        if (factor < ZOOM_MIN) {
            factor = ZOOM_MIN;
        } else if (factor > ZOOM_MAX) {
            factor = ZOOM_MAX;
        }
        this.setZoom(factor, coordinate);
    }

    @Override
    public void translate(double dX, double dY){
        //IMPORTANT: trick to avoid recalculating every time drawing happens
        //           is to move the model in the normal way
        Coordinate delta = new Coordinate(dX, dY);
        for (Node node : this.graph.getNodes()) {
            node.move(delta);
        }
        for (Edge edge : this.graph.getEdges()) {
            edge.setControl();
        }

        //IMPORTANT: translation for the viewer windows goes the opposite way
        this.currentStatus.translate(delta);
        this.fireFireworksVisibleAreaChangedEvent();
    }

    @Override
    public void zoomToCoordinate(Coordinate model, Coordinate canvas, double factor){
        //DELTA is RELATIVE to the CURRENT STATUS of the model
        Coordinate delta = model.multiply(factor).minus(canvas);
        for (Node node : this.graph.getNodes()) {
            node.zoom(factor, delta);
        }
        for (Edge edge : this.graph.getEdges()) {
            edge.setControl();
        }
        //The translation is RELATIVE to the ORIGINAL model
        this.currentStatus = new FireworksStatus(factor, delta.divide(factor));

        this.eventBus.fireEventFromSource(new FireworksZoomEvent(this.currentStatus, this.width/factor, this.height/factor, canvas), this);
    }

    protected void displayNodeAndParents(Node node){
        if(this.currentStatus.getFactor() > ZOOM_MAX){
            reduceNode(node);
        }else{
            displayNode(node);
        }
    }

    protected void displayAllNodes(boolean animation){
        //1- Calculate the outer box containing the node and all its parents
        double minX = this.graph.getMinX(); double maxX = this.graph.getMaxX();
        double minY = this.graph.getMinY(); double maxY = this.graph.getMaxY();

        //2- Display action
        this.displayAction(minX, minY, maxX, maxY, animation);
    }

    private void displayNode(Node node){
        //1- Calculate the outer box containing the node and all its parents
        double minX = node.getMinX(); double maxX = node.getMaxX();
        double minY = node.getMinY(); double maxY = node.getMaxY();
        for (Node ancestor : node.getAncestors()) {
            minX = Math.min(minX, ancestor.getMinX());
            maxX = Math.max(maxX, ancestor.getMaxX());
            minY = Math.min(minY, ancestor.getMinY());
            maxY = Math.max(maxY, ancestor.getMaxY());
        }

        //2 - Display action
        this.displayAction(minX, minY, maxX, maxY, true);
    }

    private void displayAction(double minX, double minY, double maxX, double maxY, boolean animation){
        //1- Growing the box a "space" bigger as the view offset
        double space = 20;
        minX -= space; minY -= space; maxX += space; maxY += space;

        //2- Calculate the
        double width = (maxX - minX);
        double height = (maxY - minY);
        double p = this.height / this.width;

        if(Double.isNaN(p)){
            //This happens when the window where the widget is attached to has not visible area
            p = 1;
        }

        //3- Calculate the factor
        double fW = this.width / width;
        double fH = this.height / height;
        double factor = fW < fH ? fW : fH;
        if(factor > 3.0){
            factor = 3.0; //Never deeper than 3 (the view result is a little bit useless)
        }else if (factor < 0.5){
            //Getting smaller than 0.5 does not produce a nice view either way
            factor = 0.5;
        }

        //3- Calculating proportions (and corrections for positioning)
        if(width > height){
            double aux = height;
            height = width * p;
            minY -= (height - aux)/2.0; //if wider then height and minY corrected
        }else{
            double aux = width;
            width = height / p;
            minX -= (width - aux)/2.0; //if higher then width and minX corrected
        }

        //4- Current and new positions are based in the centre of the box containing the selected element
        Coordinate centre = new Coordinate(minX + width/2.0, minY + height/2.0);
        Coordinate canvasCentre = new Coordinate(this.width/2.0, this.height/2.0);

        //5- Display the area
        if(this.movementAnimation!=null) this.movementAnimation.cancel();
        Coordinate currentCentre = this.currentStatus.getModelCoordinate(canvasCentre);
        if(animation) {
            //5.1- Animates the movement
            this.movementAnimation = new MovementAnimation(this, currentCentre, canvasCentre, this.currentStatus.getFactor());
            this.movementAnimation.moveTo(centre, canvasCentre, factor);
        }else{
            //5.2- Direct to the final coordinate (No animation)
            this.zoomToCoordinate(centre, canvasCentre, factor);
        }
    }

    public void expandNode(Node node){
        this.nodeToOpen = node;
        double minX = node.getMinX(); double maxX = node.getMaxX();
        double minY = node.getMinY(); double maxY = node.getMaxY();

        //3- Calculate the
        double width = (maxX - minX);
        double height = (maxY - minY);

        //4- Calculating proportions (and corrections for positioning)
        double factor = (width > height) ? (this.width / width) : (this.height / height) ;

        //6- Current and new positions are based in the centre of the box containing the selected element
        Coordinate canvasCentre = new Coordinate(this.width/2.0, this.height/2.0);

        //7- Animates the movement
        FocusInAnimation animation = new FocusInAnimation(this, node.getCurrentPosition(), this.currentStatus.getFactor());
        animation.moveTo(canvasCentre, factor);
    }

    public void reduceNode(Node node){
        FocusOutAnimation animation = new FocusOutAnimation(this, this.nodeToOpen.getCurrentPosition(), this.currentStatus.getFactor());
        animation.zoomOut(node, ZOOM_MAX / 2);
    }

    protected Node getHoveredNode(Coordinate mouse){
        Coordinate c = this.currentStatus.getModelCoordinate(mouse);
        double f = 1 / this.currentStatus.getFactor();
        //TODO: Test if the following strategy currently works
//        for (QuadTreeBox item : quadTree.getItems(c.getX(), c.getY())) {
//            if(item instanceof Node){
//                return (Node) item;
//            }
//        }
        List<Edge> targetEges = new LinkedList<Edge>();
        for (QuadTreeBox item : quadTree.getItems(new Box(c.getX()-f, c.getY()-f, c.getX()+f, c.getY()+f))) {
            if(item instanceof Node){
                return (Node) item;
            }else if(EDGES_SELECTABLE && item instanceof Edge){
                targetEges.add((Edge) item);
            }
        }
        for (Edge edge : targetEges) {
            if(edge.isMouseInEdge(mouse)){
                return edge.getTo();
            }
        }
        return null;
    }

    protected Set<QuadTreeBox> getVisibleElements(){
        return this.quadTree.getItems(this.currentStatus.getVisibleModelArea(this.width, this.height));
    }

    protected boolean isNodeVisible(Node node){
        if(node==null) return false;
        for (QuadTreeBox item : getVisibleElements()) {
            if(item instanceof Node){
                Node target = (Node) item;
                if(target.equals(node)) return true;
            }
        }
        return false;
    }

    @Override
    public void setZoom(double factor, Coordinate point) {
        if(factor==this.currentStatus.getFactor()) return;

        Coordinate model = this.currentStatus.getModelCoordinate(point);
        this.zoomToCoordinate(model, point, factor);
    }

    public void zoom(double factor){
        factor = this.currentStatus.getFactor() + factor;
        if(factor < ZOOM_MIN){
            factor = ZOOM_MIN;
        }else if(factor > ZOOM_MAX){
            factor = ZOOM_MAX;
        }
        Coordinate point = new Coordinate(width/2, height/2);
        Coordinate model = this.currentStatus.getModelCoordinate(point);
        this.zoomToCoordinate(model, point, factor);
    }

    @Override
    public void focusFinished(Node node) {
        if(node!=null) {
            displayNode(node);
            this.nodeToOpen = null;
        }else{
            this.eventBus.fireEventFromSource(new NodeOpenedEvent(this.nodeToOpen), this);
        }
    }

    protected void setGraph(Graph graph){
        this.graph = graph;
        double minX = this.graph.getMinX(); double maxX = this.graph.getMaxX();
        double minY = this.graph.getMinY(); double maxY = this.graph.getMaxY();
        this.quadTree = new QuadTree2D<QuadTreeBox>(minX, minY, maxX, maxY, NUMBER_OF_ELEMENTS);
        for (Node node : this.graph.getNodes()) {
            this.quadTree.add(node);
        }
        for (Edge edge : this.graph.getEdges()) {
            this.quadTree.add(edge);
        }
    }

    private void fireFireworksVisibleAreaChangedEvent(){
        Coordinate offset = this.currentStatus.getOffset();
        double w = this.width / this.currentStatus.getFactor();
        double h = this.height / this.currentStatus.getFactor();
        this.eventBus.fireEventFromSource(new FireworksVisibleAreaChangedEvent(offset, w, h), this);
    }

    private void initHandlers(){
        this.eventBus.addHandler(FireworksResizedEvent.TYPE, this);
        this.eventBus.addHandler(ThumbnailAreaMovedEvent.TYPE, this);
    }
}