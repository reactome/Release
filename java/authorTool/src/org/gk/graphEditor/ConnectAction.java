/*
 * ConnectAction.java
 *
 * Created on June 18, 2003, 3:43 PM
 */

package org.gk.graphEditor;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEdit;

import org.gk.render.*;
import org.gk.render.RenderableFeature.FeatureType;

/**
 * This GraphEditorAction implementation is used to connect a reaction to a entity.
 * @author  wgm
 */
public class ConnectAction implements GraphEditorAction {
    // parent pane
    private GraphEditorPane editorPane;
    // Connect widget
    private ConnectWidget connectWidget;
    // For dragging
    Point prevPoint;
    // intermediate node
    Renderable selectedNode;
    // A flag to disable connecting action
    private boolean disable = false;
    // For scrolling
    private Rectangle scrollRect = new Rectangle();
    
    /** Creates a new instance of ConnectAction */
    public ConnectAction(GraphEditorPane editor) {
        this.editorPane = editor;
    }
    
    public void setConnectWidget(ConnectWidget widget) {
        this.connectWidget = widget;
        editorPane.getConnectionPopupManager().setConnectWdiget(widget);
    }
    
    /** 
     * Search a node during mouse dragging and connect it after mouse released.
     */
	public void doAction(MouseEvent e) {
		if (disable) {
			if (connectWidget.getConnectedNode() == null) {
				// Do moving
				Point connectP = connectWidget.getPoint();
                // Should p be scaled? Have to find an example for it.
				connectP.x = (int) (e.getX() / editorPane.getScaleX());
				connectP.y = (int) (e.getY() / editorPane.getScaleY());
				editorPane.repaint(editorPane.getVisibleRect());
			}
			return;
		}
        // Do dragging and searching
		if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
			doMouseDragging(e);
        }
        // Do connection and set to default selectAction.
        else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            doMouseReleased();
        }
    }
    
    private void setIsChanged() {
        HyperEdge edge = connectWidget.getEdge();
        Renderable source = null;
        if (edge instanceof RenderableInteraction ||
            edge instanceof RenderableReaction) {
            edge.setIsChanged(true);
            source = edge;
        }
        else if (edge instanceof FlowLine) {
            // Output has its precedingEvent changed
            Node output = edge.getOutputNode(0);
            if (output != null) {
                output.setIsChanged(true);
                source = edge;
            }
        }
        if (source != null) {
            GraphEditorActionEvent event = new GraphEditorActionEvent(source);
            event.setID(GraphEditorActionEvent.PROP_CHANGING);
            editorPane.fireGraphEditorActionEvent(event);
        }
    }

    private void doMouseDragging(MouseEvent e) {
        ConnectionPopupManager popupManager = editorPane.getConnectionPopupManager();
        if (connectWidget.getConnectedNode() != null) {
            setIsChanged();
            connectWidget.disconnect();
        	// Kill undo for the time being. undo/redo may be implemented in the future.
        	editorPane.killUndo();
        	DetachActionEvent detachEvent = new DetachActionEvent(connectWidget.getEdge());
        	detachEvent.setRole(connectWidget.getRole());
        	detachEvent.setDetached(connectWidget.getConnectedNode());
        	connectWidget.setConnectedNode(null);
        	editorPane.fireGraphEditorActionEvent(detachEvent);
        }
        Point connectP = connectWidget.getPoint();
        connectP.x = (int) (e.getX() / editorPane.getScaleX());
        connectP.y = (int) (e.getY() / editorPane.getScaleY());
        // Check if any nodes under this point
        java.util.List components =
        	editorPane.getRenderable().getComponents();
        if (components != null && components.size() > 0) {
            // Check connection popup first
            if (popupManager.isConnectPopupShown()) {
                if (!popupManager.isPopupPicked(connectP)) {
                    popupManager.showPopup(false);
                }
            }
            else {
                selectedNode = null;
                int size = components.size();
                boolean isNodeOnly = false;
                if ((connectWidget.getEdge() instanceof RenderableReaction) || // Reaction can connect to nodes
                    (connectWidget.getRole() == HyperEdge.INPUT)) // A FlowLine's input cannot connect to reactions since the type
                                                                  // should have been assigned already. Such flowline is used for source
                                                                  // (e.g. inhibitor, activator).
                    isNodeOnly = true;
                else if ((connectWidget.getEdge() instanceof FlowLine) && // FlowLine used for Process can work with Node only
                         (connectWidget.getEdge().getInputNode(0) instanceof ProcessNode))
                    isNodeOnly = true;
                for (int i = size - 1; i > -1; i--) {
                    // Need to do in the reverse order in contrary to the drawing order so that
                    // the top one can be picked
                    Renderable r = (Renderable) components.get(i);
                    if (avoidConnect(r, isNodeOnly))
                        continue;
                    if (selectedNode == null &&
                        r.isPicked(connectP)) {
                        r.setIsHighlighted(true);
                        selectedNode = r;
                    }
                    else
                        r.setIsHighlighted(false);
                }
                popupManager.setAnchorObject(selectedNode);
                popupManager.setEntryPoint(connectP);
                popupManager.showPopup(selectedNode != null);
            }
        }
        connectWidget.getEdge().invalidateBounds();
        // Need to validate connectWidgets if backbonePoints size is 2
        // This is used to validate another end of the edge.           
        if (connectWidget.getEdge().getBackbonePoints().size() == 2)
        	connectWidget.getEdge().invalidateConnectWidgets();
        editorPane.revalidate();
        // Scrolling
        scrollRect.x = e.getX();
        scrollRect.y = e.getY();
        scrollRect.width = 10;
        scrollRect.height = 10;
        editorPane.scrollRectToVisible(scrollRect);
        editorPane.repaint(editorPane.getVisibleRect());
    }
    
    private boolean avoidConnect(Renderable obj, boolean isNodeOnly) {
        // Need to do in the reverse order in contrary to the drawing order so that
        // the top one can be picked
        if (obj == connectWidget.getEdge())
            return true; // Have to escape itself
        // Don't connect to block and text
        if (obj instanceof RenderableCompartment || obj instanceof Note ||
            obj instanceof RenderablePathway)
            return true;
        if ((obj instanceof ProcessNode || obj instanceof ReactionNode) &&
             connectWidget.getEdge() instanceof RenderableReaction)
            return true; // Reaction cannot connect to pathway directly. Escape it!
        if (obj instanceof FlowLine)
            return true;
        if (isNodeOnly && (obj instanceof HyperEdge))
            return true;
        if (connectWidget.getRole() == HyperEdge.INPUT) {
            if (obj instanceof HyperEdge)
                return true;
            // Only pathway can be used as preceder to another pathway
            if ((connectWidget.getEdge() instanceof FlowLine) &&
                (connectWidget.getEdge().getOutputNode(0) instanceof ProcessNode) &&
                !(obj instanceof ProcessNode))
                return true;
        }
        // Embedded in a Complex (ie. complex component)
        if (obj.getContainer() instanceof RenderableComplex)
            return true;
        return false;
    }

    private void doMouseReleased() {
        // Don't need to draw popup
        ConnectionPopupManager popupManager = editorPane.getConnectionPopupManager();
        popupManager.showPopup(false);
        // Link a node to edge
        if (selectedNode instanceof Node) {
            linkToNode(popupManager);
        }
        // Link another node to reaction
        else if (selectedNode instanceof RenderableReaction) {
            linkNodeToReaction(popupManager);
        }
        else {
            deleteUnAttachedEdge();
        }
        editorPane.currentAction = editorPane.selectAction;
        editorPane.repaint(editorPane.getVisibleRect());
    }
    
    /**
     * A Node or Reaction links to another Node.
     * @param popupManager
     */
    private void linkToNode(ConnectionPopupManager popupManager) {
        selectedNode.setIsHighlighted(false);
        String typeText = popupManager.getSelectedText();
        if (typeText.equals("Associate")) {
            createAssociation();
        }
        else if (typeText.equals("Produce"))
            createProduce();
        else if (typeText.equals("Phosphorylate")) {
            createPhosphorylation();
        }
        else if (typeText.equals("Precede")) {
            // Just a simple relationship
            connectWidget.getEdge().addOutput((Node)selectedNode);
            setIsChanged();
            AttachActionEvent graphEvent = new AttachActionEvent(connectWidget.getEdge());
            graphEvent.setAttached(selectedNode);
            graphEvent.setConnectWidget(connectWidget);
            editorPane.fireGraphEditorActionEvent(graphEvent);
            UndoableEdit edit = new UndoableInsertEdit(editorPane, connectWidget.getEdge());
            editorPane.addUndoableEdit(edit);
        }
        else {
            connectWidget.setConnectedNode((Node)selectedNode);
            connectWidget.invalidate();
            connectWidget.connect();
            // Assign type to flow line
            if (connectWidget.getEdge() instanceof FlowLine &&
                connectWidget.getRole() == HyperEdge.OUTPUT) {
                FlowLine flowLine = (FlowLine) connectWidget.getEdge();
                RenderableInteraction interaction = convertFLToInteraction(flowLine);
                InteractionType type = InteractionType.getType(popupManager.getSelectedText());
                interaction.setInteractionType(type);
                RenderUtility.setInteractionName(interaction);
                RenderableRegistry.getRegistry().add(interaction);
            }
            setIsChanged();
            //GraphEditorActionEvent graphEvent = new GraphEditorActionEvent(connectWidget.getEdge());
            //graphEvent.setID(GraphEditorActionEvent.REACTION_ATTACH);
            AttachActionEvent graphEvent = new AttachActionEvent(connectWidget.getEdge());
            graphEvent.setAttached(selectedNode);
            graphEvent.setConnectWidget(connectWidget);
            editorPane.fireGraphEditorActionEvent(graphEvent);
            UndoableEdit edit = new UndoableInsertEdit(editorPane, connectWidget.getEdge());
            editorPane.addUndoableEdit(edit);
        }
    }
    
    private RenderableInteraction convertFLToInteraction(FlowLine fl) {
        if (fl instanceof RenderableInteraction)
            return (RenderableInteraction) fl;
        RenderableInteraction interaction = (RenderableInteraction) RenderableFactory.generateRenderable(RenderableInteraction.class, 
                                                                                                         editorPane.getRenderable());
        List points = fl.getBackbonePoints();
        interaction.setBackbonePoints(points);
        interaction.setPosition(fl.getPosition());
        Node input = fl.getInputNode(0);
        Node output = fl.getOutputNode(0);
        editorPane.delete(fl);
        if (input != null)
            interaction.addInput(input);
        if (output != null)
            interaction.addOutput(output);
        interaction.invalidateBounds();
        interaction.setBackgroundColor(fl.getBackgroundColor());
        interaction.setLineWidth(fl.getLineWidth());
        editorPane.insertEdge(interaction, false);
        connectWidget.setEdge(interaction);
        return interaction;
    }
    
    private RenderableReaction generateNewReaction() {
        // Create a Reaction
        RenderableReaction reaction = new RenderableReaction();
        reaction.setDisplayName("Reaction");
        String newName = RenderableRegistry.getRegistry().generateUniqueName(reaction);
        reaction.setDisplayName(newName);
        RenderableRegistry.getRegistry().add(reaction);
        editorPane.insertEdge(reaction, true);
        return reaction;
    }
    
    private boolean blockInDrawingMode() {
        if (editorPane.usedAsDrawingTool) {
            JOptionPane.showMessageDialog((Container) SwingUtilities.getAncestorOfClass(Container.class, editorPane),
                                      "This type of action is blocked in the drawing mode.\n" +
                                      "You can turn it on in the preference dialog.",
                                      "Action Block",
                                      JOptionPane.INFORMATION_MESSAGE);
            fireAnEmptyAttachEvent();
            return true;
        }
        return false;
    }

    private void fireAnEmptyAttachEvent() {
        // Still need to fire an empty attach event
        Renderable edge = connectWidget.getEdge();
        // Attach event used in a special case here.
        AttachActionEvent event = new AttachActionEvent(edge);
        // The widget will be gone after deletion
        event.setConnectWidget(null);
        event.setRole(connectWidget.getRole());
        editorPane.fireGraphEditorActionEvent(event);
    }
    
    private void createAssociation() {
        if (blockInDrawingMode())
            return;
        // Create a Reaction
        final RenderableReaction reaction = generateNewReaction();
        reaction.setReactionType(ReactionType.ASSOCIATION);
        reaction.addInput((Node)selectedNode);
        Node originalNode = (Node) connectWidget.getEdge().getInputNode(0);
        reaction.addInput(originalNode);
        // Create a new complex
        final RenderableComplex complex = new RenderableComplex();
        Renderable process = editorPane.getRenderable();
        // Want to decompose a Complex
        Node comp1 = generateShortcut((Node)selectedNode, process);
        Node comp2 = generateShortcut((Node)originalNode, process);
        complex.addComponent(comp1);
        comp1.setContainer(complex);
        complex.addComponent(comp2);
        comp2.setContainer(complex);
        complex.setBoundsFromComponents();
        complex.setDisplayName(selectedNode.getDisplayName() + ":" + originalNode.getDisplayName());
        String newName = RenderableRegistry.getRegistry().generateUniqueName(complex);
        complex.setDisplayName(newName);
        RenderableRegistry.getRegistry().add(complex);
        reaction.addOutput(complex);
        editorPane.insertNode(complex);
        editorPane.delete(connectWidget.getEdge());
        // Have to wait all other drawing done so that
        // text layout for complex is there
        // to get a nice layout.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Move down
                complex.move(0, 200);
                complex.layout();
                reaction.layout();
                // Just want to pass the information that layout has been changed to
                // other client. This is really a hack!!!
                editorPane.firePropertyChange("layout", false, true);
                editorPane.repaint(editorPane.getVisibleRect());
            }
        });
        // Add an undoable edit
        List<Renderable> list = new ArrayList<Renderable>();
        list.add(reaction);
        addComplexToUndoList(complex, list);
        UndoableInsertEdit edit = new UndoableInsertEdit(editorPane,
                                                         list);
        editorPane.addUndoableEdit(edit);
        fireReactionAttachEvent(reaction, null);
    }
    
    private void addComplexToUndoList(RenderableComplex complex,
                                      List<Renderable> list) {
        list.add(complex);
        List<Renderable> components = RenderUtility.getComponentsInHierarchy(complex);
        for (Renderable r : components)
            list.add(r);
    }
    
    private Node generateShortcut(Node node,
                                  Renderable process) {
        if (node instanceof RenderableComplex) {
            RenderableComplex shortcut = RenderUtility.generateComplexShortcut((RenderableComplex)node);
            Set<Renderable> allComponents = RenderUtility.getAllContainedComponents(shortcut);
            for (Renderable r : allComponents) {
                process.addComponent(r);
                RenderableRegistry.getRegistry().add(r);
            }
            process.addComponent(shortcut);
            return shortcut;
        }
        else {
            Node shortcut = (Node) node.generateShortcut();
            process.addComponent(shortcut);
            RenderableRegistry.getRegistry().add(shortcut);
            return shortcut;
        }
    }
    
    private void createPhosphorylation() {
        if (blockInDrawingMode())
            return;
        RenderableReaction reaction = generateNewReaction();
        Node input = (Node) selectedNode;
        reaction.addInput(input);
        Node kinase = (Node) connectWidget.getEdge().getInputNode(0);
        reaction.addHelper(kinase);
        Node pNode = generatePhosphorylated(input);
        reaction.addOutput(pNode);
        // Generate ATP and ADP
        Renderable container = selectedNode.getContainer();
        Node atp = generateChemical("ATP", 
                                    container,
                                    input);
        if (atp != null)
            reaction.addInput(atp);
        Node adp = generateChemical("ADP", 
                                    container,
                                    pNode);
        if (adp != null)
            reaction.addOutput(adp);
        reaction.layout();
        editorPane.delete(connectWidget.getEdge());
        // For undo
        List<Renderable> list = new ArrayList<Renderable>();
        list.add(atp);
        list.add(adp);
        list.add(reaction);
        if (pNode instanceof RenderableComplex)
            addComplexToUndoList((RenderableComplex)pNode, list);
        else
            list.add(pNode);
        UndoableEdit edit = new UndoableInsertEdit(editorPane,
                                                   list);
        editorPane.addUndoableEdit(edit);
        fireReactionAttachEvent(reaction, null);
    }
    
    /**
     * A helper method to generate ATP, ADP or other compounds.
     * @param name
     * @param container used to search if a compound having same name
     * has been there.
     * @return
     */
    private Node generateChemical(String name,
                                  Renderable container,
                                  Node anchor) {
        // Search if there is any Renderable have been created.
        Node chemical = null;
        Renderable found = RenderableRegistry.getRegistry().getSingleObject(name);
        if (found == null) {
            chemical = new RenderableChemical();
            chemical.setDisplayName(name);
            RenderableRegistry.getRegistry().add(chemical);
            editorPane.insertNode(chemical);
            return chemical;
        }
        if (!(found instanceof Node))
            return null; // name has been used by others.
        List<Renderable> shortcuts = found.getShortcuts();
        if (shortcuts == null || shortcuts.size() == 0) {
            if (found.getContainer() == container)
                return (Node) found;
        }
        else {
            for (Renderable r : shortcuts) {
                if (r.getContainer() == container)
                    return (Node) r;
            }
        }
        // Need to create a new chemical
        chemical = (Node) found.generateShortcut();
        // Assign a default position
        int x = found.getPosition().x;
        x += 50 * Math.random();
        int y = found.getPosition().y;
        y += 50 * Math.random();
        chemical.setPosition(x, y);
        return chemical;
    }
    
    private void createProduce() {
        if (blockInDrawingMode())
            return;
        RenderableReaction reaction = generateNewReaction();
        Node output = (Node) selectedNode;
        reaction.addOutput(output);
        Node input = (Node) connectWidget.getEdge().getInputNode(0);
        reaction.addInput(input);
        fireReactionAttachEvent(reaction, null);
        List backbonePoints = reaction.getBackbonePoints();
        while (backbonePoints.size() > 2) {
            // Want to use two points only
            Point p = (Point) backbonePoints.get(1);
            reaction.removeBackbonePoint(p);
        }
        reaction.validatePosition();
        reaction.layout();
        editorPane.delete(connectWidget.getEdge());
        UndoableEdit edit = new UndoableInsertEdit(editorPane,
                                                   reaction);
        editorPane.addUndoableEdit(edit);
    }

    /**
     * A helper method to fire an attach event for the passed reaction object.
     * @param reaction
     */
    private void fireReactionAttachEvent(RenderableReaction reaction,
                                         Renderable attachedNode) {
        //GraphEditorActionEvent event = new GraphEditorActionEvent(reaction);
        AttachActionEvent event = new AttachActionEvent(reaction);
        event.setAttached(attachedNode);
        editorPane.fireGraphEditorActionEvent(event);
    }
    
    private Node generatePhosphorylated(Node node) {
        Node rtn = generateShortcut(node, 
                                    editorPane.getRenderable());
        // Add a phosphorylation feature
        RenderableFeature feature = new RenderableFeature();
        feature.setFeatureType(FeatureType.PHOSPHORYLATED);
        // In the middle of the north side
        feature.setRelativePosition(0.5d, 0.0d);
        rtn.addFeatureLocally(feature);
        // Set the position
        rtn.move(200, 0);
        RenderableRegistry.getRegistry().add(rtn);
        //editorPane.insertNode(rtn);
        return rtn;
    }

    private void linkNodeToReaction(ConnectionPopupManager popupManager) {
        selectedNode.setIsHighlighted(false);
        Node node = connectWidget.getEdge().getInputNode(0);
        RenderableReaction reaction = (RenderableReaction) selectedNode;
        String role = popupManager.getSelectedText();
        if (role != null) {
            if (role.equals("Input")) 
                reaction.addInput(node);
            else if (role.equals("Output")) 
                reaction.addOutput(node);
            else if (role.equals("Catalyze"))
                reaction.addHelper(node);
            else if (role.equals("Activate"))
                reaction.addActivator(node);
            else if (role.equals("Inhibit"))
                reaction.addInhibitor(node);
        }
        editorPane.delete(connectWidget.getEdge());
        // Don't want to handle right now.
        editorPane.killUndo();
        fireReactionAttachEvent(reaction, selectedNode);
    }
    
    private void deleteUnAttachedEdge() {
        Renderable edge = connectWidget.getEdge();
        if (edge instanceof FlowLine) {
            if (!editorPane.usedAsDrawingTool)
                editorPane.delete(edge); // Delete it all
        }
        else if (edge instanceof RenderableReaction) {
            RenderableReaction reaction = (RenderableReaction) edge;
            reaction.deleteUnAttachedBranch(connectWidget);
        }
        fireAnEmptyAttachEvent();
    }
    
    public void setEnabled(boolean enabled) {
    	this.disable = !enabled;
    }
}
