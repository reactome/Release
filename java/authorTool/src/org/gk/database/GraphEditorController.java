/*
 * Created on Jun 28, 2004
 */
package org.gk.database;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.gk.graphEditor.ComplexGraphEditor;
import org.gk.graphEditor.DetachActionEvent;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.LinkWidgetAction;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ReactionNodeGraphEditor;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.property.SearchDBTypeHelper;
import org.gk.render.*;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * A list of actions for GraphEditorPane used in EventCentricView.
 * @author wugm
 */
public class GraphEditorController {
	private Action insertEntityAction;
	private Action insertEventAction;
	private Action deleteAction;
	private Action layoutAction;
	private Action layoutEdgeAction;
    // For zooming
    private Action zoomToFitAction;
    private Action zoomInAction;
    private Action zoomOutAction;
    private Action unZoomAction;
	// The controlled EventCentricViewPane
	private EventCentricViewPane eventPane;
	// Three GraphEditorPane
	private GraphEditorPane pathwayPane;
	private GraphEditorPane reactionPane;
	private GraphEditorPane complexPane;
	// For firing AttributeEditEvent 
	private AttributeEditEvent editEvent;
    // Used to figure out the correct type
    private SearchDBTypeHelper typeHelper;

    public GraphEditorController() {
        this(null);
    }
    
	public GraphEditorController(EventCentricViewPane eventPane) {
		this.eventPane = eventPane;
		editEvent = new AttributeEditEvent(this);
        typeHelper = new SearchDBTypeHelper();
	}
	
	public Action getLayoutAction() {
		if (layoutAction == null) {
			layoutAction = new AbstractAction("Layout",
			                GKApplicationUtilities.createImageIcon(getClass(), "LayeredLayout.gif")) {
				public void actionPerformed(ActionEvent e) {
					GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
					if (graphPane != null) {
						graphPane.layoutRenderable();
					}
				}
			};
			layoutAction.putValue(Action.SHORT_DESCRIPTION, "Layout");
		}
		return layoutAction;
	}
	
	public Action getLayoutEdgeAction() {
		if (layoutEdgeAction == null) {
			layoutEdgeAction = new AbstractAction("Layout Edges",
			                GKApplicationUtilities.createImageIcon(getClass(), "EdgeLayout.gif")) {
				public void actionPerformed(ActionEvent e) {
					GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
					if (graphPane instanceof PathwayEditor) {
						((PathwayEditor)graphPane).layoutEdges();
					}
				}
			};
			layoutEdgeAction.putValue(Action.SHORT_DESCRIPTION, "Layout Edge");
		}
		return layoutEdgeAction;
	}
	
	public Action getDeleteAction() {
		if (deleteAction == null) {
			deleteAction = new AbstractAction("Delete", 
			                GKApplicationUtilities.createImageIcon(getClass(), "Delete16.gif")) {
				public void actionPerformed(ActionEvent e) {
					delete();
				}
			};
			deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete");
			// Default should be disabled
			deleteAction.setEnabled(false);
		}
		return deleteAction;
	}
    
    public Action getZoomInAction() {
        if (zoomInAction == null) {
            zoomInAction = new AbstractAction("Zoom In",
                 GKApplicationUtilities.createImageIcon(getClass(), "ZoomIn16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
                    if (graphPane == null)
                        return;
                    double scaleX = graphPane.getScaleX();
                    double scaleY = graphPane.getScaleY();
                    graphPane.zoom(scaleX * 2.0, scaleY * 2.0);
                }
            };
            zoomInAction.putValue(Action.SHORT_DESCRIPTION, "Zoom in");
        }
        return zoomInAction;
    }
    
    public Action getZoomToFitAction() {
        if (zoomToFitAction == null) {
            zoomToFitAction = new AbstractAction("Zoom To Fit") {
                public void actionPerformed(ActionEvent e) {
                    GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
                    if (graphPane != null)
                        graphPane.zoomToFit();
                }
            };
        }
        return zoomToFitAction;
    }
    
    public Action getUnZoomAction() {
        if (unZoomAction == null) {
            unZoomAction = new AbstractAction("Un-Zoom") {
                public void actionPerformed(ActionEvent e) {
                    GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
                    if (graphPane != null) {
                        graphPane.zoom(1.0d, 1.0d);
                    }
                }
            };
        }
        return unZoomAction;
    }
    
    public Action getZoomOutAction() {
        if (zoomOutAction == null) {
            zoomOutAction = new AbstractAction("Zoom Out",
                    GKApplicationUtilities.createImageIcon(getClass(), "ZoomOut16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
                    if (graphPane == null)
                        return;
                    double scaleX = graphPane.getScaleX();
                    double scaleY = graphPane.getScaleY();
                    graphPane.zoom(scaleX / 2.0, scaleY / 2.0);
                }
            };
            zoomOutAction.putValue(Action.SHORT_DESCRIPTION, "Zoom Out");
        }
        return zoomOutAction;
    }
	
	public void delete() {
		GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
		if (graphPane instanceof PathwayEditor)
			deleteForPathway(graphPane);
		else if (graphPane instanceof ReactionNodeGraphEditor)
			deleteForReaction(graphPane);		
	}
	
	private void deleteForReaction(GraphEditorPane graphPane) {
		java.util.List selection = graphPane.getSelection();
		if (selection == null || selection.size() == 0) 
			return;
		java.util.List deleting = new ArrayList();
		RenderableReaction reaction = ((ReactionNodeGraphEditor)graphPane).getReaction();
		// Check for input first
		java.util.List inputs = reaction.getInputNodes();
		for (Iterator it = inputs.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (selection.contains(obj))
				deleting.add(obj);
		}
		removeValues(deleting, "input");
		// Check for outputs
		deleting.clear();
		java.util.List outputs = reaction.getOutputNodes();
		for (Iterator it = outputs.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (selection.contains(obj)) {
				deleting.add(obj);
			}
		}
		removeValues(deleting, "output");
		// Have to remove catalystActivity here
		deleting.clear();
		java.util.List helpers = reaction.getHelperNodes();
		for (Iterator it = helpers.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (selection.contains(obj))
				deleting.add(obj);
		}
		if (deleting.size() > 0) {
			GKInstance instance = (GKInstance) eventPane.getAttributePane().getInstance();
			try {
				java.util.List ca = instance.getAttributeValuesList("catalystActivity");
				if (ca != null && ca.size() > 0) {
					java.util.List deletingInstances = new ArrayList(deleting.size());
					for (Iterator it = ca.iterator(); it.hasNext();) {
						GKInstance ca1 = (GKInstance) it.next();
						GKInstance caEntity = (GKInstance) ca1.getAttributeValue("physicalEntity");
						for (Iterator it1 = deleting.iterator(); it1.hasNext();) {
							Renderable node = (Renderable) it1.next();
							if (node.getDisplayName().equals(caEntity.getDisplayName())) {
								deletingInstances.add(ca1);
								break;
							}
						}
					}
					AttributePane attPane = eventPane.getAttributePane();
					for (Iterator it = deletingInstances.iterator(); it.hasNext();) {
						GKInstance i = (GKInstance) it.next();
						attPane.removeValue(i, "catalystActivity");
					}
				}
			}
			catch (Exception e) {
				System.err.println("GraphEditorController.deleteForReaction(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Delete the selected instances in the graph editor pane.
	 */
	private void deleteForPathway(GraphEditorPane graphPane) {
		java.util.List selection = graphPane.getSelection();
		if (selection == null || selection.size() == 0)
			return;
		// Need to remove edges first to keep precedingEvent slot consistent
		// Directly remove edges
		java.util.List edges = new ArrayList();
		java.util.List nodes = new ArrayList();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			if (r instanceof FlowLine) {
				edges.add(r);
			}
			else if (r instanceof Node)
				nodes.add(r);
		}
		for (Iterator it = edges.iterator(); it.hasNext();) {
			FlowLine line = (FlowLine)it.next();
			Renderable input = line.getInputNode(0);
			Renderable output = line.getOutputNode(0);
			removePrecedingValue(input, output);
			graphPane.delete(line);
		}
		// Handle Nodes first.
		// Figure out what instances should be removed
		removeValues(nodes, 
                     ReactomeJavaConstants.hasEvent);
		graphPane.getSelectionModel().removeSelection();
		graphPane.revalidate();
		graphPane.repaint(graphPane.getVisibleRect());
	}
	
	private void removeValues(java.util.List removeNodes, String attName) {
		if (removeNodes == null || removeNodes.size() == 0)
			return;
		GKInstance instance = (GKInstance) eventPane.getAttributePane().getInstance();
		java.util.List removeInstances = new ArrayList(removeNodes.size());
		try {
			java.util.List components = instance.getAttributeValuesList(attName);
			for (Iterator it = removeNodes.iterator(); it.hasNext();) {
				Renderable r = (Renderable) it.next();
				String name = r.getDisplayName();
				if (name == null)
					continue;
				for (Iterator it1 = components.iterator(); it1.hasNext();) {
					GKInstance childInstance = (GKInstance) it1.next();
                     // Name might contain DB_ID 
                    if (name.equals(childInstance.getDisplayName()) ||
                         name.equals(childInstance.getDisplayName() + "(" + childInstance.getDBID() + ")")) {
						removeInstances.add(childInstance);
						break;
					}
				}
			}
		}
		catch(Exception e) {
			System.err.println("GraphEditorController.delete(): " + e);
			e.printStackTrace();
		}
		if (removeInstances.size() > 0) {
			AttributePane attPane = eventPane.getAttributePane();
			for (Iterator it = removeInstances.iterator(); it.hasNext();) {
				GKInstance child = (GKInstance) it.next();
				attPane.removeValue(child, attName);
			}
		}		
	}
	
	public Action getInsertEntityAction() {
		if (insertEntityAction == null) {
			insertEntityAction = new AbstractAction("Insert Entity",
			                GKApplicationUtilities.createImageIcon(getClass(), "AddEntityCB.gif")) {
				public void actionPerformed(ActionEvent e) {
					insertEntity((JButton)e.getSource());
				}
			};
			insertEntityAction.putValue(Action.SHORT_DESCRIPTION, "Insert Entity");
		}
		return insertEntityAction;
	}
	
	private void insertEntity(JButton btn) {
		// Use a JPopupMenu for selecting role
		JPopupMenu popup = new JPopupMenu();
		ActionListener l = createAddEntityAction();
		JMenuItem inputItem = new JMenuItem("Input");
		inputItem.setActionCommand("input");
		inputItem.addActionListener(l);
		JMenuItem outputItem = new JMenuItem("Output");
		outputItem.addActionListener(l);
		outputItem.setActionCommand("output");
		JMenuItem catalystItem = new JMenuItem("Catalyst");
		catalystItem.setActionCommand("catalystActivity");
		catalystItem.addActionListener(l);
		popup.add(inputItem);
		popup.add(outputItem);
		popup.add(catalystItem);
		Point p = SwingUtilities.convertPoint(btn.getParent(), btn.getX(), btn.getY(), eventPane);
		p.y += (btn.getHeight() + 2); // A little buffer
		popup.show(eventPane, p.x, p.y);
	}
	
	private void insertValue(String attName) {
		GKInstance instance = (GKInstance) eventPane.getAttributePane().getInstance();
		if (instance == null)
			return;
		SchemaClass cls = instance.getSchemClass();
		try {
			SchemaAttribute att = cls.getAttribute(attName);
			Collection validClasses = att.getAllowedClasses();
			eventPane.getAttributePane().addInstanceValue(validClasses, att, (JFrame)SwingUtilities.getRoot(eventPane));
		}
		catch (InvalidAttributeException e) {
			System.err.println("GraphEditorActions.insertEntity(): " + e);
			e.printStackTrace();
		}
	}
	
	private ActionListener createAddEntityAction() {
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();
				insertValue(command);
			}
		};
		return l;
	}
	
	public Action getInsertEventAction() {
		if (insertEventAction == null) {
			insertEventAction = new AbstractAction("Insert Event",
			                GKApplicationUtilities.createImageIcon(getClass(), "AddPathway.gif")) {
				public void actionPerformed(ActionEvent e) {
					insertValue(ReactomeJavaConstants.hasEvent);
				}
			};
			insertEventAction.putValue(Action.SHORT_DESCRIPTION, "Insert Event");
		}
		return insertEventAction;
	}
	
	protected void removeInstances(java.util.List instances) {
		if (instances == null || instances.size() == 0)
			return;
		GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
		if (graphPane == null)
			return;
		// Get the list of nodes that should be deleted
		java.util.List deletingNodes = new ArrayList(instances.size());
		java.util.List renderables = graphPane.getDisplayedObjects();
		for (Iterator it = instances.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance) it.next();
			for (Iterator it1 = renderables.iterator(); it1.hasNext();) {
				Renderable r = (Renderable) it1.next();
				String name = r.getDisplayName();
				if (name == null)
					continue;
				if (name.equals(instance.getDisplayName())) {
					deletingNodes.add(r);
					break;
				}
			}
		}
		if (deletingNodes.size() > 0) {
			if (graphPane instanceof PathwayEditor) {
				for (Iterator it = deletingNodes.iterator(); it.hasNext();) {
					Renderable node = (Renderable)it.next();
					graphPane.delete(node);
				}
			}
			// Have to check stoichiometries
			else if (graphPane instanceof ReactionNodeGraphEditor) {
				RenderableReaction reaction = ((ReactionNodeGraphEditor)graphPane).getReaction();
				java.util.List inputs = reaction.getInputNodes();
				java.util.List outputs = reaction.getOutputNodes();
				java.util.List helpers = reaction.getHelperNodes();
				for (Iterator it = deletingNodes.iterator(); it.hasNext();) {
					Renderable node = (Renderable) it.next();
					// Figure out the role for node
					if (inputs.contains(node)) {
						int stoi = reaction.getInputStoichiometry(node);
						if (stoi > 1)
							reaction.setInputStoichiometry(node, stoi - 1);
						else
							graphPane.delete(node);
					}
					else if (outputs.contains(node)) {
						int stoi = reaction.getOutputStoichiometry(node);
						if (stoi > 1)
							reaction.setOutputStoichiometry(node, stoi - 1);
						else
							graphPane.delete(node);
					}
					else if (helpers.contains(node)) {
						graphPane.delete(node);
					}
				}
			}
			graphPane.repaint(graphPane.getVisibleRect());
		}
	}
	
	protected void addInstances(java.util.List instances) {
		GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
		if (graphPane == null)
			return;
		java.util.List renderables = new ArrayList(instances.size());
		try {
			GKInstance instance = null;
			Renderable r = null;
			for (Iterator it = instances.iterator(); it.hasNext();) {
				instance = (GKInstance) it.next();
				r = convertInstanceToNode(instance);
				renderables.add(r);
			}
		}
		catch(Exception e) {
			System.err.println("EventCentricViewPane.addInstances(): " + e);
			e.printStackTrace();
		}
		if (renderables.size() > 0) {
			for (Iterator it = renderables.iterator(); it.hasNext();) {
				Node r = (Node)it.next();
				graphPane.insertNode(r);
			}
			graphPane.repaint(graphPane.getVisibleRect());
		}
	}
	
	protected void addInstances(java.util.List instances, String type) {
		GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
		if (!(graphPane instanceof ReactionNodeGraphEditor))
			return;
		ReactionNodeGraphEditor reactionEditor = (ReactionNodeGraphEditor)graphPane;
		try {
			if (type.equals("input")) {
				for (Iterator it = instances.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					Node node = convertInstanceToNode(instance);
					reactionEditor.insertInput(node);
				}
			}
			else if (type.equals("output")) {
				for (Iterator it = instances.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					Node node = convertInstanceToNode(instance);
					reactionEditor.insertOutput(node);
				}				
			}
			else if (type.equals("catalystActivity")) {
				for (Iterator it = instances.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					GKInstance pe = (GKInstance) instance.getAttributeValue("physicalEntity");
					if (pe == null)
						continue;
					Node node = convertInstanceToNode(pe);
					if (node != null)
						reactionEditor.insertHelper(node);
				}
			}
			reactionEditor.repaint(reactionEditor.getVisibleRect());
		}
		catch (Exception e) {
			System.err.println("GraphEditorController.addInstance(): " + e);
			e.printStackTrace();
		}
	}
	
	protected void validateToolBar(GraphEditorPane graphPane) {
		if (!eventPane.isEditable())
			return;
		JToolBar bar = eventPane.graphToolbar;
		JButton btn;
		Action action;
		bar.setVisible(true);
		if (graphPane instanceof PathwayEditor) {
			for (int i = 0; i < bar.getComponentCount(); i++) {
				btn = (JButton)bar.getComponentAtIndex(i);
				action = btn.getAction();
				if (action == insertEntityAction)
					btn.setVisible(false);
				else
					btn.setVisible(true);
			}
		}
		else {
			for (int i = 0; i < bar.getComponentCount(); i++) {
				btn = (JButton) bar.getComponentAtIndex(i);
				action = btn.getAction();
				if (action == insertEventAction ||
				    action == layoutEdgeAction)
					btn.setVisible(false);
				else
					btn.setVisible(true);
			}
		}
		// Disable delete
		deleteAction.setEnabled(false);
	}
	
	public GraphEditorPane getGraphEditor(GKInstance instance) {
		GraphEditorPane graphPane = null;
		// Comment on 7/12/05: in new schema, only pathway and reaction can be displayed
		// in a graphic view. EquivalentEventSet and ConceptualEvent cannot be displayed
		// graphically.
		if (instance.getSchemClass().isa("Pathway")) {
			if (pathwayPane == null) {
				pathwayPane = new PathwayEditor();
				InstanceLinkWidgetAction linkWidgetAction = new InstanceLinkWidgetAction(pathwayPane);
				((PathwayEditor)pathwayPane).setLinkWidgetAction(linkWidgetAction);
				installListeners(pathwayPane);
			}
			graphPane = pathwayPane;
		}
		else if (instance.getSchemClass().isa("Reaction")) {
			if (reactionPane == null) {
				reactionPane = new ReactionNodeGraphEditor();
                reactionPane.setEditable(false);
				installListeners(reactionPane);
			}
			graphPane = reactionPane;
		}
		else if (instance.getSchemClass().isa("Complex")) {
			if (complexPane == null) {
				complexPane = new ComplexGraphEditor();
				installListeners(complexPane);
			}
			graphPane = complexPane;
		}
		if (graphPane != null) {
            // Popup menu  will be used to handle editing for reactions
            if (!(graphPane instanceof ReactionNodeGraphEditor))
                graphPane.setEditable(eventPane.isEditable());
			try {
				Renderable r = convertInstanceToNode(instance);
				graphPane.setRenderable(r);
			}
			catch(Exception e) {
				System.err.println("GraphEditorFactory.getGraphEditor(): " + e);
				e.printStackTrace();
			}
		}
		return graphPane;
	}
    
	private void installListeners(final GraphEditorPane graphPane) {
		// For popup menu
		graphPane.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				graphPane.requestFocus();
				if (e.isPopupTrigger())
					doPopup(graphPane, e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doPopup(graphPane, e);
			}
		});
		graphPane.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
                int meta = e.getModifiers();
                // Use meta key to make this platform-friendlier: command is used under
                // MacOS
                if ((meta == Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) &&
                    e.getKeyCode() == KeyEvent.VK_A) {
				//if (e.isControlDown() &&
				 //   e.getKeyCode() == KeyEvent.VK_A) {
					graphPane.selectAll();
					graphPane.repaint(graphPane.getVisibleRect());
				}
				else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
					if (eventPane.isEditable()) {
						delete();
					}
				}
				e.consume(); // To prevent propagating to the parent component.
			}
			public void keyReleased(KeyEvent e) {
				e.consume();
			}
		});
		// To catch the adding and deleting occuring in GraphEditorPane directly
		graphPane.addGraphEditorActionListener(new GraphEditorActionListener() {
			public void graphEditorAction(GraphEditorActionEvent e) {
				if (e.getID() == GraphEditorActionEvent.REACTION_ATTACH) {
					Object source = e.getSource();
					if (source instanceof FlowLine) {
						// Fetch out the input and output
						FlowLine flowLine = (FlowLine) source;
						Renderable input = flowLine.getInputNode(0);
						Renderable output = flowLine.getOutputNode(0);
						if(!addPrecedingValue(input, output)) {
							pathwayPane.delete(flowLine);
							pathwayPane.repaint(pathwayPane.getVisibleRect());
						}
					}	
				}
				else if (e.getID() == GraphEditorActionEvent.REACTION_DETACH) {
					if (e.getSource() instanceof FlowLine) {
						FlowLine flowLine = (FlowLine) e.getSource();
						if (e instanceof DetachActionEvent) {
							DetachActionEvent detachEvent = (DetachActionEvent) e;
							Renderable input = null;
							Renderable output = null;
							// Try to get input/output from this detachEvent.
							if (detachEvent.getDetached() != null) {
							    int role = detachEvent.getRole();
							    Renderable detached = detachEvent.getDetached();
							    if (role == HyperEdge.INPUT) 
							        input = detached;
							    else if (role == HyperEdge.OUTPUT)
							        output = detached;
							}
							if (input == null)
								input = flowLine.getInputNode(0);
							if (output == null)
								output = flowLine.getOutputNode(0);
							removePrecedingValue(input, output);
						}
					}					
				}
				else if (e.getID() == GraphEditorActionEvent.ACTION_DOUBLE_CLICKED) {
					handleDoubleClick(graphPane);
				}
			}
		});
		// For selection event
		graphPane.getSelectionModel().addGraphEditorActionListener(new GraphEditorActionListener() {
			public void graphEditorAction(GraphEditorActionEvent e) {
				if (e.getID() == GraphEditorActionEvent.SELECTION) {
					// Control delete action
					GraphEditorPane graphPane = eventPane.getDisplayGraphPane();
					if (graphPane != null) {
						java.util.List selection = graphPane.getSelection();
						if (selection == null || selection.size() == 0)
							deleteAction.setEnabled(false);
						else
							deleteAction.setEnabled(true);
					}
				}
			}
		});
	}
	
	private void handleDoubleClick(final GraphEditorPane graphPane) {
		java.util.List selection = graphPane.getSelection();
		if (selection.size() == 1) {
			Object obj = selection.get(0);
			if (obj instanceof Node) {
				// Find the instance
				GKInstance instance = getInstanceForNode((Renderable)obj);
				if (instance == null)
					return;
				if (instance.getSchemClass().isa("Event")) {
					eventPane.setSelectedEvent(instance);
				}
				else {
					if (instance.isShell())
						FrameManager.getManager().showShellInstance(instance, eventPane);
					else
						FrameManager.getManager().showInstance(instance, eventPane.isEditable());
				}
			}
		}
	}
	
	private GKInstance getInstanceForNode(Renderable node) {
		GKInstance container = (GKInstance) eventPane.getAttributePane().getInstance();
		if (container.getSchemClass().isa("Pathway")) {
			try {
				java.util.List comps = container.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
				for (Iterator it = comps.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
                     // Name might contain DB_ID
					if (node.getDisplayName().equals(instance.getDisplayName()) ||
                         node.getDisplayName().equals(instance.getDisplayName() + "(" + instance.getDBID() + ")"))
						return instance;
				}
			}
			catch (Exception e) {
				System.err.println("GraphEditorController.getInstanceForNode(): " + e);
				e.printStackTrace();
			}
		}
		else if (container.getSchemClass().isa("Reaction")) {
			try {
				GKInstance instance = null;
				// Check input
				java.util.List inputs = container.getAttributeValuesList("input");
				if (inputs != null && inputs.size() > 0) {
					for (Iterator it = inputs.iterator(); it.hasNext();) {
						instance = (GKInstance) it.next();
						if (node.getDisplayName().equals(instance.getDisplayName()))
							return instance;
					}
				}
				// Check output
				java.util.List outputs = container.getAttributeValuesList("output");
				if (outputs != null && outputs.size() > 0) {
					for (Iterator it = outputs.iterator(); it.hasNext();) {
						instance = (GKInstance) it.next();
						if (node.getDisplayName().equals(instance.getDisplayName()))
							return instance;						
					}
				}
				// Check catalyst
				java.util.List catalysts = container.getAttributeValuesList("catalystActivity");
				if (catalysts != null && catalysts.size() > 0) {
					for (Iterator it = catalysts.iterator(); it.hasNext();) {
						instance = (GKInstance) it.next();
						GKInstance catalyst = (GKInstance) instance.getAttributeValue("physicalEntity");
						if (catalyst != null && node.getDisplayName().equals(catalyst.getDisplayName()))
							return catalyst;
					}
				}
			}
			catch(Exception e) {
				System.err.println("GraphEditorController.getInstanceForNode(): " + e);
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private void removePrecedingValue(Renderable input, Renderable output) {
		if (input == null || output == null)
			return; // There is no links originally	
		// Find the corresponding GKInstance
		GKInstance inputInstance = null;
		GKInstance outputInstance = null;
		try {
			GKInstance container = (GKInstance)eventPane.getAttributePane().getInstance();
			java.util.List comps = container.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
			for (Iterator it = comps.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance)it.next();
				if (outputInstance == null &&
				    instance.getDisplayName().equals(output.getDisplayName())) {
				    outputInstance = instance;
					break;
                }
			}
            // Use precedingEvent list instead of "hasComponent" in case inputInstance
            // have been removed
            List list = outputInstance.getAttributeValuesList(ReactomeJavaConstants.precedingEvent);
            if (list == null)
                return;
            boolean isRemoved = false;
            for (Iterator it = list.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                if (instance.getDisplayName().equals(input.getDisplayName())) {
                    it.remove();
                    isRemoved = true;
                    break;
                }
            }
            if (isRemoved) {
                editEvent.setAttributeName("precedingEvent");
                list = new ArrayList(1);
                list.add(inputInstance);
                editEvent.setRemovedInstances(list);
                editEvent.setEditingInstance(outputInstance);
                editEvent.setEditingType(AttributeEditEvent.REMOVING);
                AttributeEditManager.getManager().attributeEdit(editEvent);
            }
		}
		catch (Exception e) {
			System.err.println("GraphEditorController.removePrecedingValue(): " + e);
			e.printStackTrace();
		}

	}
	
	private boolean addPrecedingValue(Renderable input, Renderable output) {
		if (input == null || output == null)
			return true;
		GKInstance container = (GKInstance) eventPane.getAttributePane().getInstance();
		try {
			java.util.List comps = container.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
			// Find the input and output
			GKInstance inputInstance = null;
			GKInstance outputInstance = null;
			for (Iterator it = comps.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				if (inputInstance == null &&
				    instance.getDisplayName().equals(input.getDisplayName())) {
					inputInstance = instance;
				}
				if (outputInstance == null &&
				    instance.getDisplayName().equals(output.getDisplayName())) {
					outputInstance = instance;
				}
				if (inputInstance != null &&
				    outputInstance != null)
				    break;
			}
			// Have to make sure outputInstance is not a Shell Instance
			if (outputInstance.isShell()) {
				JOptionPane.showMessageDialog(eventPane,
				                              "\"" + outputInstance.getDisplayName() + "\" is a shell instance and cannot be linked.\n" +				                              "You have to download it first and then link it.",
				                              "Error in Linkging",
				                              JOptionPane.ERROR_MESSAGE);
				return false;
			}
			outputInstance.addAttributeValue("precedingEvent", inputInstance);
			editEvent.setAttributeName("precedingEvent");
			java.util.List list = new ArrayList(1);
			list.add(inputInstance);
			editEvent.setAddedInstances(list);
			editEvent.setEditingInstance(outputInstance);
			editEvent.setEditingType(AttributeEditEvent.ADDING);
			AttributeEditManager.getManager().attributeEdit(editEvent);
		}
		catch(Exception e) {
			System.err.println("GraphEditorController.addPrecedingValue(): " + e);
			e.printStackTrace();
		}
		return true;
	}
	
	private void doPopup(GraphEditorPane graphPane, MouseEvent e) {
		JPopupMenu popup = new JPopupMenu();
		if (graphPane instanceof PathwayEditor) {
			if (eventPane.isEditable()) {
				popup.add(getInsertEventAction());
				popup.addSeparator();
				popup.add(getDeleteAction());
				popup.addSeparator();
			}
			popup.add(getLayoutAction());
			popup.add(getLayoutEdgeAction());
		}
		else {
			if (eventPane.isEditable()) {
				ActionListener l = createAddEntityAction();
				JMenuItem insertInput = new JMenuItem("Insert Input");
				insertInput.setActionCommand("input");
				insertInput.addActionListener(l);
				popup.add(insertInput);
				JMenuItem insertOutput = new JMenuItem("Insert Output");
				insertOutput.setActionCommand("output");
				insertOutput.addActionListener(l);
				popup.add(insertOutput);
				JMenuItem insertCatalyst = new JMenuItem("Insert Catalyst");
				insertCatalyst.setActionCommand("catalystActivity");
				insertCatalyst.addActionListener(l);
				popup.add(insertCatalyst);
				
				popup.addSeparator();
				popup.add(getDeleteAction());
				popup.addSeparator();
			}
			popup.add(getLayoutAction());
		}
        // Add zoom actions
        popup.addSeparator();
        popup.add(getZoomInAction());
        popup.add(getZoomOutAction());
        popup.add(getZoomToFitAction());
        popup.add(getUnZoomAction());
		popup.show(graphPane, e.getX(), e.getY());
	}
	
	public Node convertInstanceToNode(GKInstance instance) {
		Node node = null;
		SchemaClass cls = instance.getSchemClass();
		try {
			if (cls.isa(ReactomeJavaConstants.Pathway)) {
				node = convertToPathwayNode(instance);
			}
			else if (cls.isa(ReactomeJavaConstants.ReactionlikeEvent)) {
				node = convertToReactionNode(instance);
			}
			else if (cls.isa(ReactomeJavaConstants.Complex)) {
				node = convertToComplexNode(instance);
			}
			else {
				node = new RenderableEntity(instance.getDisplayName());
				node.setPosition(50, 50);
			}
		}
		catch (Exception e) {
			System.err.println("GraphController.convertInstanceToNode(): " + e);
			e.printStackTrace();
		}
		return node;
	}
	
	private Node convertToComplexNode(GKInstance instance) throws Exception {
		RenderableComplex complex = new RenderableComplex(instance.getDisplayName());
        complex.hideComponents(true); // Don't display pathway components
		java.util.List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
		if (components != null) {
             Set usedNames = new HashSet();
			for (Iterator it = components.iterator(); it.hasNext();) {
				GKInstance tmp = (GKInstance) it.next();
				Renderable node = null;
                 String name = tmp.getDisplayName();
                 if (usedNames.contains(name))
                     name = name + "(" + tmp.getDBID() + ")";
				if (tmp.getSchemClass().isa("Complex")) 
					node = new RenderableComplex(name);
				else
					node = new RenderableEntity(name);
				node.setPosition(50, 50);
				complex.addComponent(node);
			}
		}
		return complex;
	}
	
	private Node convertToReactionNode(GKInstance instance) throws Exception {
		RenderableReaction reaction = new RenderableReaction();
		reaction.setDisplayName(instance.getDisplayName());
		reaction.initPosition(new Point(50, 50)); // Randomly assign position.
		Node node = new ReactionNode(reaction);
		java.util.List inputs = instance.getAttributeValuesList("input");
		Map nodeMap = new HashMap();
		Map stoiMap = new HashMap();
		if (inputs != null) {
			for (Iterator it = inputs.iterator(); it.hasNext();) {
				GKInstance inputInstance = (GKInstance) it.next();
				String name = inputInstance.getDisplayName();
				if (nodeMap.containsKey(name)) {
					// Update stoiMap
					Integer value = (Integer) stoiMap.get(name);
					stoiMap.put(name, new Integer(value.intValue() + 1));
				}
				else {
					Node entity = createReactionNode(inputInstance, node);
					reaction.addInput(entity);
					node.addComponent(entity);
					nodeMap.put(name, entity);
					stoiMap.put(name, new Integer(1));
				}
			}
			// Set the input stoichiometries
			for (Iterator it = stoiMap.keySet().iterator(); it.hasNext();) {
				String name = (String) it.next();
				Integer value = (Integer) stoiMap.get(name);
				if (value.intValue() > 1) {
					Renderable entity = (Renderable) nodeMap.get(name);
					reaction.setInputStoichiometry(entity, value.intValue());
				}
			}
		}
		java.util.List outputs = instance.getAttributeValuesList("output");
		if (outputs != null) {
			nodeMap.clear();
			stoiMap.clear();
			for (Iterator it = outputs.iterator(); it.hasNext();) {
				GKInstance outputInstance = (GKInstance) it.next();
				String name = outputInstance.getDisplayName();
				if (nodeMap.containsKey(name)) {
					// Update stoiMap
					Integer value = (Integer) stoiMap.get(name);
					stoiMap.put(name, new Integer(value.intValue() + 1));
				}
				else {
					Node entity = createReactionNode(outputInstance,
                                                     node);
                    reaction.addOutput(entity);
					node.addComponent(entity);
					nodeMap.put(name, entity);
					stoiMap.put(name, new Integer(1));
				}
			}
			// Set output stoichiometries
			for (Iterator it = stoiMap.keySet().iterator(); it.hasNext();) {
				String name = (String) it.next();
				Integer value = (Integer) stoiMap.get(name);
				if (value.intValue() > 1) {
					Renderable entity = (Renderable) nodeMap.get(name);
					reaction.setOutputStoichiometry(entity, value.intValue());
				}
			}
		}
		java.util.List ca = instance.getAttributeValuesList("catalystActivity");
		if (ca != null && ca.size() > 0) {
			java.util.List helpers = new ArrayList(ca.size());
			for (Iterator it = ca.iterator(); it.hasNext();) {
				GKInstance activity = (GKInstance) it.next();
				java.util.List catalysts = activity.getAttributeValuesList("physicalEntity");
				if (catalysts != null && catalysts.size() > 0) {
					for (Iterator it1 = catalysts.iterator(); it1.hasNext();) {
						GKInstance catalyst = (GKInstance) it1.next();
						Renderable helper = createReactionNode(catalyst,
                                                               node);
						helpers.add(helper);
					}
				}
			}
			if (helpers.size() > 0) {
				for (Iterator it = helpers.iterator(); it.hasNext();) {
					Node helper = (Node) it.next();
					reaction.addHelper(helper);
					node.addComponent(helper);
				}
			}
		}
		Collection regulation = instance.getReferers(ReactomeJavaConstants.regulatedEntity);
		if (regulation != null && regulation.size() > 0) {
		    java.util.List inhibitors = new ArrayList();
		    java.util.List activators = new ArrayList();
		    for (Iterator it = regulation.iterator(); it.hasNext();) {
		        GKInstance instance1 = (GKInstance)it.next();
		        GKInstance regulator = (GKInstance) instance1.getAttributeValue("regulator");
		        if (regulator == null || !regulator.getSchemClass().isa("PhysicalEntity"))
		            continue;
		        Renderable modulator = createReactionNode(regulator,
                                                          node);
                if (instance1.getSchemClass().isa("NegativeRegulation")) { // Inhibitor
		            inhibitors.add(modulator);
		        }
		        else if (instance1.getSchemClass().isa("PositiveRegulation")) { // Activator
		            activators.add(modulator);
		        }
		    }
		    // Add to reaction
		    if (inhibitors.size() > 0) {
		        for (Iterator it = inhibitors.iterator(); it.hasNext();) {
		            Node inhibitor = (Node)it.next();
		            reaction.addInhibitor(inhibitor);
		            node.addComponent(inhibitor);
		        }
		    }
		    if (activators.size() > 0) {
		        for (Iterator it = activators.iterator(); it.hasNext();) {
		            Node activator = (Node)it.next();
		            reaction.addActivator(activator);
		            node.addComponent(activator);
		        }
		    }
		}
		// Don't want to keep any selection information. This is more like a bug: during adding different
		// branches, selectPoint is auto-set somehow, which breaks some code.
		reaction.getSelectionInfo().reset();
		return node;
	}
    
    private Node createReactionNode(GKInstance instance,
                                    Renderable container) throws Exception {
        Node entity = null;
        if (instance.getSchemClass().isa("Complex"))
            entity = new RenderableComplex();
        else {
            // Have to find the type for instance
            Class type = typeHelper.guessNodeType(instance);
            entity = (Node) type.newInstance();
        }
        entity.setDisplayName(instance.getDisplayName());
        entity.setReactomeId(instance.getDBID());
        //entity.setModelInstance(inputInstance);
        entity.setPosition(50, 50);
        return entity;
    }
    
    private Renderable createNodeForEntitySet(GKInstance eventSet) throws Exception {
        // Determine the type of the specified event: Pathway or Reaction 
        // Get all leaf nodes
        List instances = getInstancesFromEventSet(eventSet);
        boolean isReaction = true;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            if (instance.getSchemClass().isa("Pathway")) {
                isReaction = false;
                break;
            }
        }
        if (isReaction)
            return new ReactionNode();
        else
            return new RenderablePathway();
    }
    
    private List getInstancesFromEventSet(GKInstance eventSet) throws Exception {
        Set instances = new HashSet();
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(eventSet);
        GKInstance tmp = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                if (tmp.getSchemClass().isValidAttribute("hasSpecialisedForm")) {
                    List values = tmp.getAttributeValuesList("hasSpecialisedForm");
                    if (values != null && values.size() > 0)
                        next.addAll(values);
                    else
                        instances.add(tmp);
                }
                else if (tmp.getSchemClass().isValidAttribute("hasMember")) {
                    List values = tmp.getAttributeValuesList("hasMember");
                    if (values != null && values.size() > 0)
                        next.addAll(values);
                    else
                        instances.add(tmp);
                }
                else {
                    instances.add(tmp);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return new ArrayList(instances);
    }
    
	private Node convertToPathwayNode(GKInstance instance) throws Exception {
		Node node = new RenderablePathway();
		node.setDisplayName(instance.getDisplayName());
		node.setPosition(50, 50);
		// Get the child events
        List list = null;
        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
            list = instance.getAttributeValuesListNoCheck(ReactomeJavaConstants.hasEvent); // it has been checked already
        else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
            list = instance.getAttributeValuesListNoCheck(ReactomeJavaConstants.hasComponent);
		if (list != null && list.size() > 0) {
			Map map = new HashMap();
             Set usedNames = new HashSet();
			for (Iterator it = list.iterator(); it.hasNext();) {
				GKInstance child = (GKInstance)it.next();
				if (child == null)
					continue;
				Renderable childNode = null;
				if (child.getSchemClass().isa("Pathway")) {
					childNode = new ProcessNode();
				}
				else if (child.getSchemClass().isa("Reaction")) {
					childNode = new ReactionNode();
				}
                else
                    childNode = createNodeForEntitySet(child);
				// No more types are supported
				if (childNode != null) {
                     // two children might have the same displayNames. Use this method
                     // to force the displayNames unique in the converted RenderablePathway
                     // since it matters for autolayout for Graphviz.
                     String displayName = child.getDisplayName();
                     if (usedNames.contains(displayName))
                         displayName = displayName + "(" + child.getDBID() + ")";
                     else
                         usedNames.add(displayName);
					childNode.setDisplayName(displayName);
					childNode.setReactomeId(child.getDBID());
					//childNode.setModelInstance(child);
					childNode.setPosition(50, 50);
					map.put(child.getDBID(), childNode);
					node.addComponent(childNode);
					childNode.setContainer(node);
				}
			}
			// Figure out the relationships among sub events.
			for (Iterator it = list.iterator(); it.hasNext();) {
				GKInstance child = (GKInstance)it.next();
				if (child == null)
					continue;
				Node childNode = (Node)map.get(child.getDBID());
				java.util.List precedingEvents = child.getAttributeValuesList("precedingEvent");
				if (precedingEvents == null || precedingEvents.size() == 0)
					continue;
				for (Iterator it1 = precedingEvents.iterator(); it1.hasNext();) {
					GKInstance child1 = (GKInstance)it1.next();
					Node childNode1 = (Node)map.get(child1.getDBID());
					if (childNode1 == null)
						continue;
					FlowLine flowLine = new FlowLine();
					flowLine.initPosition(new Point(50, 50));
					flowLine.addInput(childNode1);
					flowLine.addOutput(childNode);
					node.addComponent(flowLine);
					flowLine.setContainer(node);
				}
			}
		}
		return node;
	}
	
	class InstanceLinkWidgetAction extends LinkWidgetAction {
	
		public InstanceLinkWidgetAction(GraphEditorPane graphPane) {
			super(graphPane);
		}
		
		/**
		 * Override the super class' method to provide CuratorTool specific action.
		 */
		protected void insertNode(MouseEvent e) {
			java.util.List oldComps = new ArrayList(pathwayPane.getDisplayedObjects());
			insertValue(ReactomeJavaConstants.hasEvent);
			// Try to figure out the added new nodes
			java.util.List newComps = new ArrayList(pathwayPane.getDisplayedObjects());
			newComps.removeAll(oldComps);
			if (newComps.size() == 0)
				return;
			int selectedDirection = pathwayPane.isLinkWidgetPicked(e.getX(), e.getY());		
			Node node = (Node) pathwayPane.getSelection().get(0);
			pathwayPane.getSelectionModel().removeSelected(node);
			Node newNode = null;
			// Get common properties
			boolean isInput;
			if (selectedDirection == GraphEditorPane.LINK_WIDGET_EAST ||
			    selectedDirection == GraphEditorPane.LINK_WIDGET_SOUTH)
			    isInput = false;
			else
				isInput = true;
			Point position1 = node.getPosition();
			Dimension size = node.getBounds().getSize();
			int index = 0;
			for (Iterator it = newComps.iterator(); it.hasNext();) {
				newNode = (Node) it.next();
				Point position = new Point();
				switch (selectedDirection) {
					case GraphEditorPane.LINK_WIDGET_EAST :
						position.x = position1.x + size.width / 2 + NEW_NODE_DISTANCE;
						position.y = position1.y + NEW_NODE_DISTANCE * index;
						break;
					case GraphEditorPane.LINK_WIDGET_SOUTH :
						position.x = position1.x + NEW_NODE_DISTANCE * index;
						position.y = position1.y + size.height / 2 + NEW_NODE_DISTANCE;
						break;
					case GraphEditorPane.LINK_WIDGET_WEST :
						position.x = position1.x - size.width / 2 - NEW_NODE_DISTANCE;
						if (position.x < 10)
							position.x = 10;
						position.y = position1.y + NEW_NODE_DISTANCE * index;
						break;
					case GraphEditorPane.LINK_WIDGET_NORTH :
						position.x = position1.x + NEW_NODE_DISTANCE * index;
						position.y = position1.y - size.height / 2 - NEW_NODE_DISTANCE;
						if (position.y < 10)
							position.y = 10;
						break;
				}
				newNode.setPosition(position);
				// link
				FlowLine flowLine = new FlowLine();
				// Initialize the positions and attach nodes
				if (isInput) {
					if(addPrecedingValue(newNode, node)) {
						flowLine.setInputHub(newNode.getPosition());
						flowLine.setOutputHub(node.getPosition());
						flowLine.addInput(newNode);
						flowLine.addOutput(node);
						pathwayPane.insertEdge(flowLine, false);
					}
				}
				else {
					if (addPrecedingValue(node, newNode)) {
						flowLine.setOutputHub(newNode.getPosition());
						flowLine.setInputHub(node.getPosition());
						flowLine.addOutput(newNode);
						flowLine.addInput(node);
						pathwayPane.insertEdge(flowLine, false);
					}
				}
				// Select the newNode
				pathwayPane.getSelectionModel().addSelected(newNode);
				index ++;
			}
			pathwayPane.setShouldDrawLinkWidgets(false);
			pathwayPane.repaint(); 
			final Renderable newNode1 = (Renderable) newComps.get(0);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					pathwayPane.revalidate();
					// It might be not initialized because of synchronization issue.
					if (newNode1.getBounds() != null)
						pathwayPane.scrollRectToVisible(newNode1.getBounds());
				}
			});
		}
	}
}
