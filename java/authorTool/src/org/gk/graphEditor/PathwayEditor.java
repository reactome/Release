/*
 * PathwayEditor.java
 *
 * Created on June 16, 2003, 1:02 PM
 */

package org.gk.graphEditor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.render.*;
import org.gk.util.GraphLayoutEngine;
/**
 * This customized JPanel is used to draw a Pathway.
 * @author  wgm
 */
public class PathwayEditor extends GraphEditorPane {
    // Used as a background image
    private BufferedImage backgroundImage;
    private Point bgImagePos;
    // For link widgets
    private LinkWidgetHandler linkWidgetHandler;
    // For connection popup
    private ConnectionPopupManager connectionPopupManager;
    // To track if pathways should be drawn
    private boolean drawPathway;
    // A flag to control is complex component editing should be enabled on the fly based on moving
    private boolean isComplexComponentEditDisabled;
    // Used to control if private note should be drawn
    private boolean hidePrivateNote;

    /** Creates a new instance of PathwayEditor */
    public PathwayEditor() {
        init();
        setRenderable(new RenderablePathway("Untitled"));
    }
    
    public PathwayEditor(RenderablePathway pathway) {
        init();
        setRenderable(pathway);
    }
    
    public void disableComplexComponentEdit(boolean disable) {
        this.isComplexComponentEditDisabled = disable;
    }
    
    public void setBackgroundImage(BufferedImage img) {
        this.backgroundImage = img;
    }
    
    public void setHidePrivateNote(boolean hide) {
        this.hidePrivateNote = hide;
    }
    
    public boolean getHidePrivateNote() {
        return this.hidePrivateNote;
    }
    
    public void setBackgroudImagePosition(int x, int y) {
        if (bgImagePos == null)
            bgImagePos = new Point();
        bgImagePos.x = x;
        bgImagePos.y = y;
    }
    
    private void init() {
        connectionPopupManager = new ConnectionPopupManager();
        
        // For linkWidgets
        linkWidgetAction = new LinkWidgetAction(this);
        linkWidgetHandler = new LinkWidgetHandler(this);
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        // Clear the editor
        Graphics2D g2 = (Graphics2D) g;
        if (backgroundImage != null && bgImagePos != null)
            g2.drawImage(backgroundImage,
                         bgImagePos.x,
                         bgImagePos.y,
                         this);
        List comps = displayedObject.getComponents();
        // Draw nodes first
        if (comps == null || comps.size() == 0)
            return;
        // For special treatment
        Renderable selected = null;
        List selection = getSelection();
        if (!isEditing && selection.size() == 1) {
            selected = (Renderable) selection.get(0);
        }
        java.util.List edges = new ArrayList();
        Rectangle clip = g.getClipBounds();
        // Draw Compartments first
        drawCompartments(g, 
                         comps, 
                         selected,
                         clip);
        // Drawing pathways after blocks in case they are used as container
        for (Iterator it = comps.iterator(); it.hasNext();) {
            Renderable obj = (Renderable) it.next();
            if (obj instanceof RenderablePathway) {
                RenderablePathway pathway = (RenderablePathway) obj;
                pathway.validateBounds(g);
                if (clip.intersects(pathway.getBounds()))
                    pathway.render(g);
            }
        }
        // Need to draw editing compartment first before complexes
        if (isEditing && editingNode instanceof RenderableCompartment) {
            if (editor instanceof DefaultNodeEditor)
                ((DefaultNodeEditor)editor).setScaleFactors(scaleX, scaleY);
            editor.render(g);
        }
        // Draw complexes now
        drawComplexes(comps, clip, g);
        // Drawing editing node
        if (isEditing && !(editingNode instanceof RenderableCompartment)) {
            if (editor instanceof DefaultNodeEditor)
                ((DefaultNodeEditor)editor).setScaleFactors(scaleX, scaleY);
            editor.render(g); // Always draw editing node. It will not impact the performance,
                              // since there is only one node in editing.
            if (editingNode instanceof RenderableComplex) {
                drawContainerComponents((RenderableComplex)editingNode,
                                        g,
                                        clip);
            }
        }
		for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
			Renderable obj = (Renderable)it.next();
            if (isEditing && (obj == editingNode)) {
                continue;
			}
            if (obj instanceof RenderableCompartment ||
                obj instanceof RenderableComplex ||
                obj instanceof RenderablePathway)
                continue; // Escape it. It should be drawn earlier.
            if (obj instanceof Node) {
                Node node = (Node) obj;
                if (hidePrivateNote && (node instanceof Note) && ((Note)node).isPrivate())
                    continue;
            	node.validateBounds(g);
            	if (clip.intersects(node.getBounds()))
            		node.render(g);
            }
            else if (obj instanceof HyperEdge) {
                edges.add(obj);
            }
        }
        // Want to draw this first so that the dragged edge will not
        // be hidden by the popup.
        connectionPopupManager.drawConnectionPopup(g2);
        // Draw HyperEdges
        for (Iterator it = edges.iterator(); it.hasNext();) {
            HyperEdge reaction = (HyperEdge) it.next();
            reaction.validateConnectInfo();
            if (clip.intersects(reaction.getBounds()))
            	reaction.render(g);
        }
        // Do another draw for selected instances
        if (selected != null &&
            !(selected instanceof RenderableCompartment) &&
            clip.intersects(selected.getBounds())) {
            selected.render(g);
            if (selected instanceof RenderableComplex)
                drawContainerComponents((ContainerNode)selected,
                                        g, 
                                        clip);
        }
        // Draw the link widgets for the selected nodes
        drawLinkWidgets(g2);
        // Draw the dragging rectangle
       	drawDragRect(g2); 
    }

    protected List<RenderableCompartment> drawCompartments(Graphics g, 
                                                           List comps,
                                                           Renderable selected,
                                                           Rectangle clip) {
        List<RenderableCompartment> compartments = getCompartmentsInHierarchy();
        int index = compartments.indexOf(selected);
        // Draw no membrane first
        for (RenderableCompartment compartment : compartments) {
            if (compartment == selected)
                continue;
            if (compartment.getDisplayName() != null &&
                compartment.getDisplayName().endsWith("membrane"))
                continue;
            if (clip.intersects(compartment.getBounds()))
                compartment.render(g);
        }
        // Draw membranes now
        for (RenderableCompartment compartment : compartments) {
            if (compartment == selected)
                continue;
            if (compartment.getDisplayName() != null &&
                !compartment.getDisplayName().endsWith("membrane"))
                continue;
            if (clip.intersects(compartment.getBounds()))
                compartment.render(g);
        }
        // Last draw selected so that it can be displayed
        if (selected instanceof RenderableCompartment &&
            clip.intersects(selected.getBounds())) {
            selected.render(g);
            // All contained compartments
            drawContainerComponents((ContainerNode)selected,
                                    g, 
                                    clip);
        }
        return compartments;
    }
    
    private void drawContainerComponents(ContainerNode container,
                                         Graphics g,
                                         Rectangle clip) {
        List<Renderable> components = RenderUtility.getComponentsInHierarchy(container);
        for (int i = components.size() - 1; i >= 0; i--) {
            Renderable r = components.get(i);
            if (clip.intersects(r.getBounds()))
                r.render(g);
        }
    }
    
    protected void drawComplexes(List comps,
                                 Rectangle clip,
                                 Graphics g) {
        // Complexes should be drawn in a hierarchy way so that
        // contained complexes can be draw later to avoid overlapping
        // Get all complexes
        List<RenderableComplex> complexes = new ArrayList<RenderableComplex>();
        for (Iterator it = comps.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableComplex)
                complexes.add((RenderableComplex)obj);
        }
        while (complexes.size() > 0) {
            for (Iterator<RenderableComplex> it = complexes.iterator(); it.hasNext();) {
                RenderableComplex complex = it.next();
                // If the container of a complex has been drawn, this complex
                // can be drawn.
                if (!complexes.contains(complex.getContainer())) {
                    complex.validateBounds(g);
                    if (clip.intersects(complex.getBounds()))
                        complex.render(g);
                    it.remove();
                }
            }
        }
    }

    public ConnectionPopupManager getConnectionPopupManager() {
        return connectionPopupManager;
    }
    
    public void setConnectionPopupManager(ConnectionPopupManager manager) {
        this.connectionPopupManager = manager;
    }
    
	public void setShouldDrawLinkWidgets(boolean shouldDraw) {
		linkWidgetHandler.setShouldDrawLinkWidgets(shouldDraw);
	}
    
	private void drawLinkWidgets(Graphics2D g2) {
        linkWidgetHandler.drawLinkWidgets(g2);
	}
    
    public void insertCompartment(RenderableCompartment compartment) {
        // Check if the position is set
        if (compartment.getPosition() == null) {
            Point p = new Point(defaultInsertPos);
            compartment.setPosition(p);
            updateDefaultInsertPos();
        }
        // Make sure bounds is in the visible area
        Rectangle bounds = compartment.getBounds();
        int dx = 0; 
        int dy = 0;
        if (bounds.x <= 0)
            dx = 10 - bounds.x;
        if (bounds.y <= 0)
            dy = 10 - bounds.y;
        // Use move
        compartment.move(dx, dy);
        displayedObject.addComponent(compartment);
        compartment.setContainer(displayedObject);
        repaint(getVisibleRect());
        validateCompartmentSetting(compartment);
        firePropertyChange("insert", displayedObject, compartment);
        fireGraphEditorActionEvent(GraphEditorActionEvent.INSERT);
    }
    
    public void insertNode(Node node) {
        super.insertNode(node);
        // Check if the position is set
        if (node.getPosition() == null) {
            Point p = new Point(defaultInsertPos);
            node.setPosition(p);
            updateDefaultInsertPos();
        }
        ((RenderablePathway)displayedObject).addComponent(node);
        if (node.getContainer() != displayedObject)
        	node.setContainer(displayedObject);
        revalidate(); // Make sure if any scrollbar is needed
        repaint(getVisibleRect()); // To figure out bounds for newly added node.
        validateCompartmentSetting(node);
        if (node instanceof RenderablePathway)
            ((RenderablePathway)node).setIsVisible(drawPathway);
        firePropertyChange("insert", displayedObject, node);
        fireGraphEditorActionEvent(GraphEditorActionEvent.INSERT);
    }
    
    /**
     * 
     * Override the super class method to control if pathways should be displayed.
     */
    @Override
    public void setRenderable(Renderable displayedObject) {
        List components = displayedObject.getComponents();
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof RenderablePathway) {
                    RenderablePathway pathway = (RenderablePathway) obj;
                    pathway.setIsVisible(drawPathway);
                }
            }
        }
        super.setRenderable(displayedObject);
    }
    
    /**
     * Call this method to find a suitable RenderableComplex that can have the passed
     * Node object as its direct (first level) component.
     * @param node
     * @return
     */
    public RenderableComplex pickUpComplex(Node node) {
        if (isComplexComponentEditDisabled) 
            return null;
        for (Iterator it = getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            // Make sure it is visible. It should not be used as a conainer
            // if it is hidden.
            if (r == node || // Avoid itself
                !r.isVisible() ||
                !(r instanceof RenderableComplex))
                continue;
            // Make sure it is the first level complex
            if (r.getContainer() instanceof RenderableComplex)
                continue;
            // If the complex is hiding components, don't let it be container
            RenderableComplex complex = (RenderableComplex) r;
            if (complex.isComponentsHidden())
                continue;
            RenderableComplex picked = complex.pickUpContainer(node);
            if (picked != null)
                return picked;
        }
        return null;
    }
    
    /**
     * Get the appropriate compartment for the passed Renderable object.
     * @param r
     * @return
     */
    public RenderableCompartment pickUpCompartment(Renderable r) {
        List<RenderableCompartment> compartments = getCompartmentsInHierarchy();
        if (compartments == null || compartments.size() == 0)
            return null;
        for (int i = compartments.size() - 1; i >= 0; i--) {
            RenderableCompartment compart = compartments.get(i);
            if (compart.isAssignable(r))
                return compart;
        }
        return null;
    }
    
    /**
     * Get a list of Renderable objects that can be contained by the passed
     * RenderableCompartment. This method has not considered the compartment
     * hierarchy.
     * @param compartment
     * @return
     */
    public List<Renderable> pickUpCompartmentComponents(RenderableCompartment compartment) {
        List<Renderable> list = new ArrayList<Renderable>();
        for (Iterator it = getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (compartment.isAssignable(r))
                list.add(r);
        }
        return list;
    }
    
    /**
     * Gather a list of Nodes that can be components of the passed complex.
     * @param complex
     * @return
     */
    public List<Node> pickUpComplexComponents(RenderableComplex complex) {
        List<Node> nodes = new ArrayList<Node>();
        if (isComplexComponentEditDisabled) {
            List components = RenderUtility.getAllDescendents(complex);
            if (components != null) {
                for (Iterator it = components.iterator(); it.hasNext();) {
                    Node node = (Node) it.next();
                    nodes.add(node);
                }
            }
        }
        else {
            for (Iterator it = getDisplayedObjects().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (complex.isAssignable(r)) {
                    nodes.add((Node)r);
                }
            }
        }
        return nodes;
    }
    
    private void changeComplexComponents(Renderable container) {
        if (container instanceof RenderableComplex)
            validateComplexShortcuts((RenderableComplex)container);
        GraphEditorActionEvent event = new GraphEditorActionEvent(container);
        event.setID(GraphEditorActionEvent.COMPLEX_COMPONENT_CHANGED);
        fireGraphEditorActionEvent(event);
    }
    
    /**
     * Called when a complex's component has been changed. But originally this complex
     * has other shortcuts. Now a new complex should be created.
     * @param complex
     */
    private void validateComplexShortcuts(RenderableComplex complex) {
        if (complex.getShortcuts() != null && complex.getShortcuts().size() > 0) {
            List<Renderable> shortcuts = complex.getShortcuts();
            // Track the shortcut
            Renderable shortcut = null;
            for (Renderable r : shortcuts) {
                if (r != complex) {
                    shortcut = r;
                    break;
                }
            }
            String oldName = complex.getDisplayName();
            String newName = RenderableRegistry.getRegistry().generateUniqueName(complex);
            JOptionPane.showMessageDialog(this,
                                          "There are other complexes having same names as this complex to be changed. \n" +
                                          "The identity of this complex will be changed to: " + newName,
                                          "Complex Checking",
                                          JOptionPane.INFORMATION_MESSAGE);
            complex.delinkToShortcuts();
            // Register target and shortcut
            RenderableRegistry registry = RenderableRegistry.getRegistry();
            registry.remove(complex, false);
            // Need to cut the links to other shortcuts
            complex.setDisplayName(newName);
            registry.add(complex);
            // Just in case
            registry.add(shortcut);
            firePropertyChange("delinkShortcuts",
                               shortcut,
                               complex);
            fireGraphEditorActionEvent(GraphEditorActionEvent.DE_LINKSHORTCUTS);
        }
    }
    
    @Override
    protected void validateComplexSetting(Renderable r) {
        if (isComplexComponentEditDisabled)
            return;
        if (!(r instanceof Node))
            return;
        Node node = (Node) r;
        RenderableComplex complex = pickUpComplex(node);
        if (node.getContainer() == complex)
            return; // It has been assigned already
        ContainerNode container = (ContainerNode) node.getContainer();
        // Don't remove from the topmost component
        if (container != null && container != displayedObject) {
            container.removeComponent(node);
            // Need to assign displayedObject for the time being
            // in case complex is null. For compartment setting, the client
            // should call validateCompartmentSetting() after this method call.
            node.setContainer(displayedObject);
            changeComplexComponents(container);
        }
        if (complex != null) {
            complex.addComponent(node);
            node.setContainer(complex);
            changeComplexComponents(complex);
        }
        if (r instanceof RenderableComplex) {
            complex = (RenderableComplex) r;
            // Need to empty original components first
            List<Renderable> originalComps = complex.getComponents();
            List<Renderable> copyOriginal = new ArrayList<Renderable>();
            if (originalComps != null)
                copyOriginal.addAll(originalComps);
            List<Node> compNodes = pickUpComplexComponents(complex);
            copyOriginal.removeAll(compNodes); // Should be deleted
            boolean isChanged = false;
            for (Renderable tmp : copyOriginal) {
                complex.removeComponent(tmp);
                // This should be correct since only resizing can
                // be used to remove a complex component.
                tmp.setContainer(complex.getContainer());
                isChanged = true;
            }
            for (Node comp : compNodes) {
                container = (ContainerNode) comp.getContainer();
                if (container == complex)
                    continue;
                if (compNodes.contains(container))
                    continue; // container will be handled directly
                // Do a relink here
                if (container instanceof RenderableComplex) {
                    comp.getContainer().removeComponent(comp);
                }
                complex.addComponent(comp);
                comp.setContainer(complex);
                isChanged = true;
            }
            if (isChanged) {
                changeComplexComponents(complex);
            }
        }
    }

    /**
     * Check if the passed Renderable object's compartment setting is correct.
     * This method should be called when the passed Renderable object is moved.
     * @param r
     */
    protected void validateCompartmentSetting(Renderable r) {
        List components = getDisplayedObjects();
        // Do a whole sale compartment setting
        if (!(r instanceof RenderableCompartment)) {
            // Just need a simple checking
            List<RenderableCompartment> compartments = getCompartmentsInHierarchy();
            validateCompartmentSetting(r, compartments);
        }
        else { // Move a compartment will change a lot (esp. resizing)
            // Old compartment hierarchy
            List<RenderableCompartment> compartments = getCompartmentsInHierarchy();
            RenderableCompartment self = (RenderableCompartment) r;
            compartments.remove(self);
            // Looking for its container
            validateCompartmentSetting(self, compartments);
            // Looking to see if it can contain others
            // Only its siblings need to check
            if (self.getContainer() instanceof RenderableCompartment) {
                // Make a copy to avoid ConcurrentModificationException
                List tmpList = new ArrayList(self.getContainer().getComponents());
                for (Iterator it = tmpList.iterator(); it.hasNext();) {
                    Renderable tmp = (Renderable) it.next();
                    if (tmp == self)
                        continue;
                    self.validateContainerSetting(tmp);
                }
                // All contained components should be checked
                if (self.getComponents() != null && self.getComponents().size() > 0) {
                    // Make a copy to avoid java.util.ConcurrentModificationException.
                    List copy = new ArrayList(self.getComponents());
                    RenderableCompartment parent = (RenderableCompartment) self.getContainer();
                    for (Iterator it = copy.iterator(); it.hasNext();) {
                        Renderable tmp = (Renderable) it.next();
                        self.validateContainerSetting(tmp);
                        if (self.contains(tmp))
                            continue;
                        tmp.setContainer(parent);
                        parent.addComponent(tmp);
                    }
                }
            }
            else {
                // It is the topmost compartment, and is not contained by others.
                // Check if it can contain any top-level Renderable objects. If not,
                // it cannot contain other compartments.
                for (Iterator it = components.iterator(); it.hasNext();) {
                    Renderable tmp = (Renderable) it.next();
                    if (tmp == self)
                        continue;
                    // Reset components contained by self.
                    if (isTopLevelObject(tmp) || tmp.getContainer() == self)
                        self.validateContainerSetting(tmp);
                }
            }
        }
        firePropertyChange("compartment", null, r);
    }
    
    /**
     * Check if a Renderable is a top level object. If a Renderable object's container
     * is a RenderablePathway, this Renderable object can be treated as the toplevel object.
     * This method should be used for checking compartments and complexes hierarchy only.
     * @param r
     * @return
     */
    private boolean isTopLevelObject(Renderable r) {
        Renderable container = r.getContainer();
        if (container == null ||
            container instanceof RenderablePathway)
            return true;
        return false;
    }

    /**
     * A helper method to check if a Renderable can be contained by a list of
     * RenderableCompartment in a hierarchical order.
     * @param r
     * @param compartments
     */
    private void validateCompartmentSetting(Renderable r,
                                            List<RenderableCompartment> compartments) {
        // Start from the back
        for (int i = compartments.size() - 1; i >= 0; i--) {
            RenderableCompartment compartment = compartments.get(i);
            compartment.validateContainerSetting(r);
            if (compartment.contains(r)) {
                break;
            }
        }
    }
    
    private List<RenderableCompartment> getCompartmentsInHierarchy() {
        List components = getDisplayedObjects();
        List<RenderableCompartment> compartments = new ArrayList<RenderableCompartment>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableCompartment) {
                RenderableCompartment compartment = (RenderableCompartment) r;
                if (isTopLevelObject(compartment)) {
                    // Should be the highest level
                    getCompartmentsInHierarchy(compartment, 
                                               compartments);
                }
            }
        }
        return compartments;
    }
    
    private void getCompartmentsInHierarchy(RenderableCompartment compartment,
                                            List<RenderableCompartment> compartments) {
        compartments.add(compartment);
        List comps = compartment.getComponents();
        if (comps == null || comps.size() == 0)
            return;
        for (Iterator it = comps.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableCompartment) {
                RenderableCompartment childComp = (RenderableCompartment) obj;
                getCompartmentsInHierarchy(childComp, compartments);
            }
        }
    }
    
    public void insertEdge(HyperEdge edge, boolean useDefaultInsertPos) {
        if (useDefaultInsertPos) {
            Point position = new Point(defaultInsertPos);
            edge.initPosition(position);
            updateDefaultInsertPos();
        }
        ((RenderablePathway)displayedObject).addComponent(edge);
        if (edge.getContainer() != displayedObject)
        	edge.setContainer(displayedObject);
        // Need to figure out the exact bounds for draw
        Rectangle bounds = edge.getBounds();
        Rectangle tmp = new Rectangle(bounds);
        // Make it a little big
        tmp.x -= 10;
        tmp.y -= 10;
        tmp.width += 20;
        tmp.height += 20;
        repaint(tmp);
        validateCompartmentSetting(edge);
        if (edge instanceof RenderableReaction) {
        	firePropertyChange("insert", displayedObject, edge);
        	fireGraphEditorActionEvent(GraphEditorActionEvent.INSERT);
        }
    }
    
    public void setEditingNode(Node node) {
    	super.setEditingNode(node);
        editor = RendererFactory.getFactory().getEditor(node);
        if (editor == null) // This might be null if node is null
            return;
    	editor.setRenderable(node);
    	editor.setCaretPosition(0);
    	if (node != null) {
            if (editor instanceof DefaultNodeEditor)
                ((DefaultNodeEditor)editor).setComponent(this);
    		editor.setSelectionStart(0);
    		editor.setSelectionEnd(node.getDisplayName().length());
    	}
    }
    
    public void layoutRenderable() {
    	RenderablePathway pathway = (RenderablePathway) displayedObject;
    	// Using hierarchical layout as default.
    	pathway.layout(GraphLayoutEngine.HIERARCHICAL_LAYOUT);
        //pathway.layout(GraphLayoutEngine.FORCE_DIRECTED_LAYOUT);
    	// As of Jan 12, 2007, centering is turing off since all Renderable objects
        // cannot stay at the same position for some reason after several layout actions
        // Center the pathway
        //centerRenderable();
        // Call this method to try making bounds correctly
        validateNodeBounds();
    	revalidate();
		repaint(getVisibleRect());
		fireGraphEditorActionEvent(GraphEditorActionEvent.LAYOUT);
    }
    
    /**
     * Layout displayed FlowLines. If there are selected edges, the selected edges will be
     * layouted, otherwise, all edges will be layouted.
     */
    public void layoutEdges() {
        if (displayedObject.getComponents() == null ||
                displayedObject.getComponents().size() == 0)
            return;
        java.util.List selection = getSelection();
        // Construct a list of edges to be laid out
        List<HyperEdge> edges = new ArrayList<HyperEdge>();
        if (selection.size() == 0) { // Do all edge layout
            for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof HyperEdge) {
                    edges.add((HyperEdge)r);
                }
            }
        }
        else {
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r instanceof HyperEdge)
                    edges.add((HyperEdge)r);
            }
        }
        if (edges.size() == 0)
            return;
        for (HyperEdge edge : edges)
            edge.layout();
        repaint(getVisibleRect());
        firePropertyChange("layout", null, edges);
    }
    
    /**
     * Check if the specified Point is contained in a link widget.
     * @param p
     * @return one value of: LINK_WIDGET_NONE, LINK_WIDGET_EAST, LINK_WIDGET_SOUTH,
     * LINK_WIDGET_NORTH, LINK_WIDGET_WEST.
     */
    public int isLinkWidgetPicked(int x, int y) {
    	return linkWidgetHandler.isLinkWidgetPicked(x, y);
    }
    
	public void validateNodeBounds() {
		Graphics g = getGraphics();
		if (g == null || displayedObject.getComponents() == null)
			return;
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
			Object obj = it.next();
			if (obj instanceof Node) {
				Node node = (Node) obj;
				node.validateBounds(g);
			}
		}
	}
	
	public void setLinkWidgetAction(LinkWidgetAction action) {
		linkWidgetAction = action;
	}
	
	/**
	 * Pack a list of selected Renderable objects into a specified RenderablePathway object.
	 * @param packEvents a list of events to be packed.
	 * @param container the pathway container for the packing.
	 */
	public void packEvents(java.util.List packEvents, 
	                       RenderablePathway container) {
	    // Get the intital position from one of the selected node
		Point p = null;
		for (Iterator it = packEvents.iterator(); it.hasNext();) {
		    Object obj = it.next();
		    if (obj instanceof Node) {
		        p = ((Renderable)obj).getPosition();
		        break;
		    }
		}
		if (p != null)
			container.setPosition(new Point(p));
		Renderable r = null;
		// have to filter out those FlowLine objects whose two nodes are outside the container
		FlowLine flowLine = null;
		Renderable input;
		Renderable output;
		for (Iterator it = packEvents.iterator(); it.hasNext();) {
		    r = (Renderable) it.next();
		    if (r instanceof FlowLine) {
		        flowLine = (FlowLine) r;
		        input = flowLine.getInputNode(0);
		        output = flowLine.getOutputNode(0);
		        if (packEvents.contains(input) ||
		            packEvents.contains(output))
		            continue;
		        it.remove();
		    }
		}
		// Start packing
		// Add all selected objects to container
		for (Iterator it = packEvents.iterator(); it.hasNext();) {
		    r = (Renderable) it.next();
		    displayedObject.removeComponent(r);
		    container.addComponent(r);
		    r.setContainer(container);
		}
		insertNode(container);
		validateLinks(packEvents, container);
		container.setIsSelected(true);
        java.util.List selection1 = new ArrayList();
        selection1.add(container);
        setSelection(selection1);
        repaint(getVisibleRect());
        firePropertyChange("pack", container, packEvents);
	}
	
	/**
	 * A helper method to validate the nodes attached to the FlowArrow
	 * instances correct. A split FlowArrow is defined as one node is packed
	 * and another node is not. The split FlowArrow should be deleted. The linking
	 * information should be saved into preceding property.
	 * @param packEvents
	 */
	private void validateLinks(List packEvents, Renderable container) {
	    FlowLine flowLine = null;
	    Node input;
	    Node output;
	    // Check FlowLine objects in the packEvents list
	    for (Iterator it = packEvents.iterator(); it.hasNext();) {
	        Object obj = it.next();
	        if (obj instanceof FlowLine) {
                flowLine = (FlowLine) obj;
                input = flowLine.getInputNode(0);
                output = flowLine.getOutputNode(0);
                boolean isInputContained = packEvents.contains(input);
                boolean isOutputContained = packEvents.contains(output);
                if (isInputContained && isOutputContained)
                    continue;
                if ((isInputContained && output != null) || (isOutputContained && input != null)) {
                    addPrecedingEventProperty(input, output);
                    flowLine.remove(input);
                    flowLine.remove(output);
                    container.removeComponent(flowLine);
                }
	        }
	    }
	    // Check FlowLine objects in the displayed object
	    for (Iterator it = displayedObject.getComponents().iterator(); it.hasNext();) {
	        Object obj = it.next();
	        if (obj instanceof FlowLine) {
	            flowLine = (FlowLine) obj;
	            input = flowLine.getInputNode(0);
	            output = flowLine.getOutputNode(0);
	            boolean isInputContained = packEvents.contains(input);
	            boolean isOutputContained = packEvents.contains(output);
	            if (isInputContained && isOutputContained) {
	                flowLine.remove(input);
	                flowLine.remove(output);
	                it.remove();
	                // Add a new FlowLine in the container
	                FlowLine flowLine1 = createFlowLine(input, output);
	                container.addComponent(flowLine1);
	            }
	            else if (isInputContained || isOutputContained) {
	                if (input != null)
	                    flowLine.remove(input);
	                if (output != null)
	                    flowLine.remove(output);
	                if (input != null && output != null)
	                    addPrecedingEventProperty(input, output);
	                it.remove();
	            }
	        }
	    }
	}
	
	private void addPrecedingEventProperty(Renderable input, Renderable output) {
        List list = (List) output.getAttributeValue(RenderablePropertyNames.PRECEDING_EVENT);
        if (list == null) {
            list = new ArrayList();
            output.setAttributeValue(RenderablePropertyNames.PRECEDING_EVENT, list);
        }
        list.add(input);
    }
	
	private FlowLine createFlowLine(Node input, Node output) {
	    FlowLine flowLine = new FlowLine();
	    flowLine.addInput(input);
	    flowLine.addOutput(output);
	    Point p = new Point();
	    Point p1 = input.getPosition();
	    Point p2 = output.getPosition();
	    p.x = (p1.x + p2.x) / 2;
	    p.y = (p1.y + p2.y) / 2;
	    flowLine.initPosition(p);
	    return flowLine;
	}
	
    private Node searchNodeByName(List nodes, String displayName) {
        Node node = null;
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            node = (Node) it.next();
            if (node.getDisplayName().equals(displayName))
                return node;
        }
        return null;
    }

    @Override
    public void setPathwayVisible(boolean isVisible) {
        drawPathway = isVisible;
        List components = getDisplayedObjects();
        if (components == null)
            return;
        boolean repaint = false;
        for (Iterator it = components.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderablePathway) {
                RenderablePathway pathway = (RenderablePathway) obj;
                pathway.setIsVisible(isVisible);
                repaint = true;
            }
        }
        if (repaint)
            repaint(getVisibleRect());
    }

    @Override
    public List<Renderable> deleteSelection() {
        // Check if there is any instance in the component
        if (isComplexComponentEditDisabled) {
            // Check if any Renderable is a complex component
            List selection = getSelection();
            List<Renderable> toBeDeleted = new ArrayList<Renderable>();
            for (Iterator it = selection.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.getContainer() instanceof RenderableComplex) {
                    continue;
                }
                toBeDeleted.add(r);
            }
            if (toBeDeleted.size() < selection.size()) {
                // Show an information
                JOptionPane.showMessageDialog(this, 
                                              "Complex subunits cannot be deleted in this view. Please use attribute editing.", 
                                              "Deleteion", 
                                              JOptionPane.INFORMATION_MESSAGE);
            }
            return deleteSelection(toBeDeleted);
        }
        else
            return super.deleteSelection();
    }
    
    /**
     * Tight nodes so that text can be fitted into the bounds of a node. 
     * @param applyToOverflowNodesOnly true the action should be applied to nodes having text overflowed.
     */
    public void tightNodes(boolean applyToOverflowNodesOnly) {
        // Want to right the bounds
        final double oldWidthRatio = Node.getWidthRatioOfBoundsToText();
        final double oldHeightRatio = Node.getHeightRatioOfBoundsToText();
        final int oldWidth = Node.getNodeWidth();
        List objects = getDisplayedObjects();
        Renderable r = null;
        for (Iterator it = objects.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            // Escape compartment
            if (r instanceof RenderableCompartment ||
                r instanceof Note)
                continue; 
            if (r instanceof Node) {
                Node node = (Node) r;
                // Apply tight nodes for overflow text only
                if (applyToOverflowNodesOnly) {
                    if (node.isTextOverflowed()) {
                        // Use minimum width to keep the layout as much as possible
                        Rectangle bounds = node.getBounds();
                        if (bounds != null)
                            node.setMinWidth(bounds.width);
                        node.setBounds(null);
                        node.invalidateTextBounds();
                        node.invalidateConnectWidgets();
                        node.invalidateNodeAttachments();
                    }
                }
                else {
                    node.setBounds(null);
                    node.invalidateTextBounds();
                    node.invalidateConnectWidgets();
                    node.invalidateNodeAttachments();
                }
            }
        }
        Node.setWidthRatioOfBoundsToText(1.0d);
        Node.setHeightRatioOfBoundsToText(1.0d);
        // Need to repaint
        Rectangle visibleRect = getVisibleRect();
        repaint(visibleRect);
        // Use layout as the property name to force any state change.
        fireGraphEditorActionEvent(ActionType.LAYOUT);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Need to revert Node settings back
                Node.setWidthRatioOfBoundsToText(oldWidthRatio);
                Node.setHeightRatioOfBoundsToText(oldHeightRatio);
                Node.setNodeWidth(oldWidth);
            }
        });
    }
    
    /**
     * This method is used to make the nodes tight vs text encapsulating the node. Sometimes, the text
     * is too small or big regarding the rectangle used by the node. This call will make it tight.
     */
    public void tightNodes() {
        tightNodes(false);
    }
    
}
