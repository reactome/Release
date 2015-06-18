package org.reactome.web.fireworks.client;

import com.google.gwt.animation.client.AnimationScheduler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import org.reactome.web.fireworks.analysis.AnalysisType;
import org.reactome.web.fireworks.analysis.SpeciesFilteredResult;
import org.reactome.web.fireworks.analysis.factory.AnalysisModelFactory;
import org.reactome.web.fireworks.events.*;
import org.reactome.web.fireworks.handlers.*;
import org.reactome.web.fireworks.model.FireworksData;
import org.reactome.web.fireworks.model.Graph;
import org.reactome.web.fireworks.model.Node;
import org.reactome.web.fireworks.model.factory.ModelException;
import org.reactome.web.fireworks.model.factory.ModelFactory;
import org.reactome.web.fireworks.util.Coordinate;
import org.reactome.web.fireworks.util.FireworksEventBus;
import org.reactome.web.fireworks.util.Tooltip;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
class FireworksViewerImpl extends ResizeComposite implements FireworksViewer,
        MouseDownHandler, MouseMoveHandler, MouseUpHandler, MouseOutHandler, MouseWheelHandler,
        FireworksVisibleAreaChangedHandler, FireworksZoomHandler, ClickHandler, /*DoubleClickHandler,*/
        AnalysisResetHandler, ExpressionColumnChangedHandler,
        ControlActionHandler, ProfileChangedHandler {

    EventBus eventBus = new FireworksEventBus();

    FireworksViewerManager manager;

    FireworksCanvas canvases;

    FireworksData data;

    boolean forceFireworksDraw = true;

    // mouse positions relative to canvas (not the model)
    // Do not assign the same value at the beginning
    Coordinate mouseCurrent = new Coordinate(-100, -100);
    Coordinate mousePrevious = new Coordinate(-200, -200);

    Coordinate mouseDown = null;
    boolean fireworksMoved = false;

    Node hovered = null;
    Node selected = null;

    public FireworksViewerImpl(String json) {
        try {
            Graph graph = ModelFactory.getGraph(json);
            this.data = new FireworksData(graph);
            this.canvases = new FireworksCanvas(eventBus, graph); //Graph needed for the thumbnail
            this.manager = new FireworksViewerManager(eventBus, graph);
            initWidget(canvases);
        } catch (FireworksCanvas.CanvasNotSupportedException e) {
            initWidget(new Label("Canvas not supported"));
            fireEvent(new CanvasNotSupportedEvent());
        } catch (ModelException e) {
            initWidget(new Label(e.getMessage()));
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public FireworksViewerImpl() {
        initialize();
    }

    @Override
    public HandlerRegistration addAnalysisResetHandler(AnalysisResetHandler handler) {
        return this.eventBus.addHandler(AnalysisResetEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addCanvasNotSupportedHandler(CanvasNotSupportedHandler handler){
        return this.eventBus.addHandler(CanvasNotSupportedEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addFireworksLoaded(FireworksLoadedHandler handler) {
        return this.eventBus.addHandler(FireworksLoadedEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addExpressionColumnChangedHandler(ExpressionColumnChangedHandler handler) {
        return this.eventBus.addHandler(ExpressionColumnChangedEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addNodeHoverHandler(NodeHoverHandler handler){
        return this.eventBus.addHandler(NodeHoverEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addNodeHoverResetHandler(NodeHoverResetHandler handler) {
        return this.eventBus.addHandler(NodeHoverResetEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addNodeOpenedHandler(NodeOpenedHandler handler) {
        return this.eventBus.addHandler(NodeOpenedEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addNodeSelectedHandler(NodeSelectedHandler handler) {
        return this.eventBus.addHandler(NodeSelectedEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addNodeSelectedResetHandler(NodeSelectedResetHandler handler) {
        return this.eventBus.addHandler(NodeSelectedResetEvent.TYPE, handler);
    }

    @Override
    public HandlerRegistration addProfileChangedHandler(ProfileChangedHandler handler) {
        return this.eventBus.addHandler(ProfileChangedEvent.TYPE, handler);
    }


    @Override
    public Node getSelected() {
        return this.selected;
    }

    @Override
    public void highlightNode(String stableIdentifier) {
        Node node = this.data.getNode(stableIdentifier); if(node==null) return;
        Tooltip.getTooltip().setPreventShowing(node.equals(this.selected) || !this.manager.isNodeVisible(node));
        this.setHoveredNode(node);
    }

    @Override
    public void highlightNode(Long dbIdentifier) {
        Node node = this.data.getNode(dbIdentifier); if(node==null) return;
        Tooltip.getTooltip().setPreventShowing(node.equals(this.selected) || !this.manager.isNodeVisible(node));
        this.setHoveredNode(node);
    }

    @Override
    public void openPathway(String stableIdentifier) {
        Node node = this.data.getNode(stableIdentifier); if(node==null) return;
        this.selectNode(node, false); this.openNode(node);
    }

    @Override
    public void openPathway(Long dbIdentifier) {
        Node node = this.data.getNode(dbIdentifier); if(node==null) return;
        this.selectNode(node, false); this.openNode(node);
    }

    @Override
    public void onResize() {
        if(!isVisible()) return;
        super.onResize();
        this.forceFireworksDraw = true;
    }

    @Override
    public void onFireworksVisibleAreaChanged(FireworksVisibleAreaChangedEvent event) {
        //TODO: We need a way to specify the redraw is because of canvas is moving and not zooming
        this.forceFireworksDraw = true;
    }

    @Override
    public void onFireworksZoomChanged(FireworksZoomEvent event) {
        this.forceFireworksDraw = true;
    }

    @Override
    public void onAnalysisReset() {
        this.data.resetPathwaysAnalysisResult();
        this.forceFireworksDraw = true;
    }

    @Override
    public void onExpressionColumnChanged(ExpressionColumnChangedEvent e) {
        this.canvases.setColumn(e.getColumn()); //First the column needs to be set in the canvases
        this.forceFireworksDraw = true;
    }

    @Override
    public void onControlAction(ControlActionEvent event) {
        switch (event.getAction()){
            case FIT_ALL:   this.manager.displayAllNodes(true); break;
            case ZOOM_IN:   this.manager.zoom(0.25);            break;
            case ZOOM_OUT:  this.manager.zoom(-0.25);           break;
            case UP:        this.manager.translate(0, 10);      break;
            case RIGHT:     this.manager.translate(-10, 0);     break;
            case DOWN:      this.manager.translate(0, -10);     break;
            case LEFT:      this.manager.translate(10, 0);      break;
            case OPEN:      this.openNode(this.selected);       break;
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        event.stopPropagation(); event.preventDefault();
        if(this.hovered!=null){
            //After a usability testing it was seen people hardly double
            //clicked the nodes, so when the already selected node is
            //clicked again, it automatically expands the node
            if(this.hovered==this.selected){
                this.manager.expandNode(this.selected);
            } else{
                this.selectNode(hovered, false);
            }
        }else{
            if(!this.fireworksMoved && this.selected!=null){
                this.selectNode(null, false);
            }
        }
        this.fireworksMoved = false;
    }

//    @Override
//    public void onDoubleClick(DoubleClickEvent event) {
//        event.stopPropagation(); event.preventDefault();
//        if(this.hovered!=null){
//            if(this.hovered!=this.selected){
//                this.selectNode(hovered, false);
//            }
//            this.manager.expandNode(this.hovered);
//        }else {
//            Element element = event.getRelativeElement();
//            Coordinate mouse = new Coordinate(event.getRelativeX(element), event.getRelativeY(element));
//            this.manager.onMouseScrolled(-10, mouse);
//        }
//    }

    @Override
    public void onMouseDown(MouseDownEvent event) {
        event.stopPropagation(); event.preventDefault();
        Tooltip.getTooltip().setPreventShowing(true);

        this.fireworksMoved = false;
        setMouseDownPosition(event.getRelativeElement(), event);
    }

    @Override
    public void onMouseMove(MouseMoveEvent event) {
        event.stopPropagation(); event.preventDefault();
        if(mouseDown!=null){
            this.fireworksMoved = true;
            translateGraphObjects(event.getRelativeElement(), event);
            setMouseDownPosition(event.getRelativeElement(), event);
            forceFireworksDraw = true;
        }else {
            setMousePosition(event.getRelativeElement(), event);
        }
    }

    @Override
    public void onMouseOut(MouseOutEvent event) {
        event.stopPropagation(); event.preventDefault();
        this.fireworksMoved = false;
        mouseDown = null;
        Tooltip.getTooltip().setPreventShowing(false);
        mouseCurrent = new Coordinate(-200, -200);
    }

    @Override
    public void onMouseUp(MouseUpEvent event) {
        event.stopPropagation(); event.preventDefault();
        mouseDown = null;
        Tooltip.getTooltip().setPreventShowing(false);
        setMousePosition(event.getRelativeElement(), event);
    }

    @Override
    public void onMouseWheel(MouseWheelEvent event) {
        event.stopPropagation(); event.preventDefault();
        Tooltip.getTooltip().hide(); //Let's the manager decide whether it is still needed ;)
        Element element = event.getRelativeElement();
        Coordinate mouse = new Coordinate(event.getRelativeX(element), event.getRelativeY(element));
        this.manager.onMouseScrolled(event.getDeltaY(), mouse);
    }

    @Override
    public void onProfileChanged(ProfileChangedEvent event) {
        data.updateColours();
        forceFireworksDraw = true;
    }

    @Override
    public void resetHighlight() {
        this.setHoveredNode(null);
    }

    @Override
    public void resetSelection() {
        this.selectNode(null, false);
    }

    @Override
    public void selectNode(String stableIdentifier) {
        this.selectNode(this.data.getNode(stableIdentifier), true);
    }

    @Override
    public void selectNode(Long dbIdentifier) {
        this.selectNode(this.data.getNode(dbIdentifier), true);
    }

    @Override
    public void setAnalysisToken(String token, final String resource){
        this.canvases.onAnalysisReset();
        this.data.resetPathwaysAnalysisResult();
        final Object _this = this;
        AnalysisModelFactory.retrievePathwayBaseList(token, this.data.getSpeciesId(), resource, new AnalysisModelFactory.AnalysisModelFactoryHandler() {
            @Override
            public void onPathwaysBaseListRetrieved(SpeciesFilteredResult result) {
                result.setAnalysisType(AnalysisType.getType(result.getType()));
                data.setPathwaysAnalysisResult(result); //Data has to be set in the first instance
                eventBus.fireEventFromSource(new AnalysisPerformedEvent(result), _this);
                forceFireworksDraw = true;
            }

            @Override
            public void onPathwaysBaseListError(Throwable e) {
                forceFireworksDraw = true;
            }
        });
    }

    @Override
    public void resetAnalysis() {
        eventBus.fireEventFromSource(new AnalysisResetEvent(), this);
    }

    private void doUpdate(){
        this.doUpdate(false);
    }

    private void doUpdate(boolean force){
        if(this.forceFireworksDraw){
            this.forceFireworksDraw = false;
            this.drawFireworks();
            return;
        }
        if(force || !mouseCurrent.equals(mousePrevious)){
            Node node = this.manager.getHoveredNode(mouseCurrent);
            if(node==null){
                if(this.hovered!=null){
                    this.hovered = null;
                    canvases.getTopCanvas().getElement().getStyle().setCursor(Style.Cursor.DEFAULT);
                    this.eventBus.fireEventFromSource(new NodeHoverResetEvent(), this);
                }
            }else{
                canvases.getTopCanvas().getElement().getStyle().setCursor(Style.Cursor.POINTER);
                if(!node.equals(this.hovered)){
                    this.hovered = node;
                    if(!node.equals(this.selected)) {
                        this.eventBus.fireEventFromSource(new NodeHoverEvent(node), this);
                    }
                }
            }
        }
        this.mousePrevious = this.mouseCurrent;
    }

    private void drawFireworks(){
        this.canvases.drawElements(this.manager.getVisibleElements());
        this.canvases.drawText(this.selected);
        this.canvases.selectNode(this.selected);
        this.canvases.highlightNode(this.hovered);
    }

    @Override
    protected void initWidget(Widget widget) {
        super.initWidget(widget);
        //We need to defer the program counter to the parents in order to finish DOM tasks
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                initialize();
            }
        });
    }

    private void initialize(){
        this.onResize(); // Adjusts size before rendering the Fireworks for the first time
        this.initHandlers();
        this.forceFireworksDraw = true; //IMPORTANT! Do NOT place it inside the scheduler class
        this.manager.displayAllNodes(false);
        this.eventBus.fireEventFromSource(new FireworksLoadedEvent(this.data.getSpeciesId()), this);
        AnimationScheduler.get().requestAnimationFrame(new AnimationScheduler.AnimationCallback() {
            @Override
            public void execute(double timestamp) {
                doUpdate();
                AnimationScheduler.get().requestAnimationFrame(this); // Call it again.
            }
        });
    }

    protected void initHandlers() {
        this.canvases.addClickHandler(this);
//        this.canvases.addDoubleClickHandler(this);
        this.canvases.addMouseDownHandler(this);
        this.canvases.addMouseMoveHandler(this);
        this.canvases.addMouseOutHandler(this);
        this.canvases.addMouseUpHandler(this);
        this.canvases.addMouseWheelHandler(this);

        this.eventBus.addHandler(ControlActionEvent.TYPE, this);
        this.eventBus.addHandler(AnalysisResetEvent.TYPE, this);
        this.eventBus.addHandler(ExpressionColumnChangedEvent.TYPE, this);
        this.eventBus.addHandler(FireworksVisibleAreaChangedEvent.TYPE, this);
        this.eventBus.addHandler(FireworksZoomEvent.TYPE, this);
        this.eventBus.addHandler(ProfileChangedEvent.TYPE, this);
    }

    protected void openNode(Node node){
        if(node!=null){
            this.manager.expandNode(node);
        }
    }

    protected void setMouseDownPosition(Element element, MouseEvent event){
        this.mouseDown = new Coordinate(event.getRelativeX(element), event.getRelativeY(element));
    }

    protected void setMousePosition(Element element, MouseEvent event) {
        this.mouseCurrent = new Coordinate(event.getRelativeX(element), event.getRelativeY(element));
    }

    protected void translateGraphObjects(Element element, MouseEvent event){
        double dX = event.getRelativeX(element) - mouseDown.getX();
        double dY = event.getRelativeY(element) - mouseDown.getY();
        this.manager.translate(dX, dY);
    }

    private void selectNode(Node toSelect, boolean displayNodeAndParents){
        Tooltip.getTooltip().hide();
        if(toSelect!=null){
            if(displayNodeAndParents) {
                this.manager.displayNodeAndParents(toSelect);
            }
            if(!toSelect.equals(this.selected)) {
                this.selected = toSelect;
                //Note: the selection happens because other classes are listening to this event
                this.eventBus.fireEventFromSource(new NodeSelectedEvent(this.selected), this);
            }
        }else{
            if(this.selected!=null) {
                this.selected = null;
                this.eventBus.fireEventFromSource(new NodeSelectedResetEvent(), this);
            }
        }
    }

    private void setHoveredNode(Node toHighlight){
        if(toHighlight!=null){
            if(!toHighlight.equals(this.hovered) && !toHighlight.equals(this.selected)){
                this.hovered = toHighlight;
                //Note: the highlighting happens because other classes are listening to this event
                this.eventBus.fireEventFromSource(new NodeHoverEvent(this.hovered), this);
            }
        }else{
            if(this.hovered!=null){
                this.hovered = null;
                this.eventBus.fireEventFromSource(new NodeHoverResetEvent(), this);
            }
        }
    }
}