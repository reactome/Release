/*
 * Created on Jul 2, 2003
 */
package org.gk.graphEditor;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import org.gk.render.ConnectWidget;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.ReactionNode;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;
import org.gk.render.RenderableReaction;

/**
 * This subclass of GraphEditorPane is used for drawing RenderableReaction.
 * There is only one reaction can be drawn in a ReactionEditor. No RenderablePathway 
 * can be added.
 * @author wgm
 */
public class ReactionNodeGraphEditor extends GraphEditorPane {

	public ReactionNodeGraphEditor() {
		init();
	}
	
	private void init() {
		// No need for connecting action
		connectAction.setEnabled(false);
	}
	
	public ReactionNodeGraphEditor(Renderable reaction) {
		this();
		setRenderable(reaction);
	}
	
	public void paint(Graphics g) {
		super.paint(g);
        //Clear the editor
		Graphics2D g2 = (Graphics2D)g;
		if (displayedObject == null)
			return;
		// Draw node first
		RenderableReaction reaction = getReaction();
		java.util.List components = displayedObject.getComponents();
		if (components != null) {
			Rectangle clip = g2.getClipBounds();
			for (Iterator it = components.iterator(); it.hasNext();) {
				Object obj = it.next();
                 if (isEditing && editingNode == obj) {
					editor.render(g);
					int stoi = reaction.getStoichiometry(editingNode);
					drawStoichiometry(stoi, editingNode, g2);
					continue;
				}
				if (obj instanceof RenderableComplex) {
					RenderableComplex complex = (RenderableComplex)obj;
                    if (complex.getBounds() == null)
                        complex.render(g);
                    else if (complex.getBounds().intersects(clip))
						complex.render(g);
					int stoi = reaction.getStoichiometry(complex);
					drawStoichiometry(stoi, complex, g2);
				}
                else if (obj instanceof Node) {
                    Node entity = (Node)obj;
                    entity.validateBounds(g);
                    if (entity.getBounds().intersects(clip))
                        entity.render(g);
                    int stoi = reaction.getStoichiometry(entity);
                    drawStoichiometry(stoi, entity, g2);
                }
			}
		}
		// Draw reaction
		reaction.validateConnectInfo();
		reaction.render(g);
		// Draw dragrect
		drawDragRect(g2);
	}
	
	public RenderableReaction getReaction() {
		ReactionNode reactionNode = (ReactionNode) displayedObject;
		return reactionNode.getReaction();
	}
	
	/**
	 * Insert a node into the rendered reaction.
	 * @param node either RenderableEntity or RenderableComplex. Otherwise, an IllegalArgumentException
	 * will be thrown.
	 */
	public void insertInput(Node node) {
		if ((node instanceof RenderableEntity) ||
		    (node instanceof RenderableComplex)) {
			ReactionNode reactionNode = (ReactionNode) displayedObject;
			RenderableReaction reaction = reactionNode.getReaction();
			java.util.List inputs = reaction.getInputNodes();
			Renderable existingNode = (Renderable) RenderUtility.searchNode(inputs, node.getDisplayName());
			if (existingNode != null) {
				int stoi = reaction.getInputStoichiometry(existingNode);
				reaction.setInputStoichiometry(existingNode, stoi + 1);
				existingNode.invalidateConnectWidgets();
				firePropertyChange("stoichiometry", existingNode, new Integer(stoi + 1));
			}
			else {
				reactionNode.addComponent(node);
				reaction.addInput(node);
				node.setContainer(reactionNode);
				firePropertyChange("insert", displayedObject, node);
			}
            displayedObject.setIsChanged(true);
		}
		else
			throw new IllegalArgumentException("ReactionEditor.insertInput(): node must be either RenderableEntity or RenderableComplex.");
	}
	
	public void insertOutput(Node node) {
		if ((node instanceof RenderableEntity) ||
			(node instanceof RenderableComplex)) {
			ReactionNode reactionNode = (ReactionNode) displayedObject;
			RenderableReaction reaction = reactionNode.getReaction();
			java.util.List outputs = reaction.getOutputNodes();
			Renderable existingNode = (Renderable) RenderUtility.searchNode(outputs, node.getDisplayName());
			if (existingNode != null) {
				int stoi = reaction.getOutputStoichiometry(existingNode);
				reaction.setOutputStoichiometry(existingNode, stoi + 1);
				existingNode.invalidateConnectWidgets();
				firePropertyChange("stoichiometry", existingNode, new Integer(stoi + 1));
			}
			else {
				reactionNode.addComponent(node);
				reaction.addOutput(node);
				node.setContainer(reactionNode);
				firePropertyChange("insert", displayedObject, node);
			}
            displayedObject.setIsChanged(true);
		}
		else
			throw new IllegalArgumentException("ReactionEditor.insertInput(): node must be either RenderableEntity or RenderableComplex.");
	}
	
	public void insertHelper(Node node) {
		if ((node instanceof RenderableEntity) ||
			(node instanceof RenderableComplex)) {
			ReactionNode reactionNode = (ReactionNode) displayedObject;
			RenderableReaction reaction = reactionNode.getReaction();
			reactionNode.addComponent(node);
			reaction.addHelper(node);
			node.setContainer(reactionNode);
            displayedObject.setIsChanged(true);
			firePropertyChange("insert", displayedObject, node);
		}
		else
			throw new IllegalArgumentException("ReactionEditor.insertInput(): node must be either RenderableEntity or RenderableComplex.");
	}
	
	public void insertInhibitor(Node node) {
		if ((node instanceof RenderableEntity) ||
			(node instanceof RenderableComplex)) {
			ReactionNode reactionNode = (ReactionNode) displayedObject;
			RenderableReaction reaction = reactionNode.getReaction();
			reactionNode.addComponent(node);
			reaction.addInhibitor(node);
			node.setContainer(reactionNode);
            displayedObject.setIsChanged(true);
			firePropertyChange("insert", displayedObject, node);
		}
		else
			throw new IllegalArgumentException("ReactionEditor.insertInhibitor(): node must be either RenderableEntity or RenderableComplex.");		
	}
	
	public void insertActivator(Node node) {
		if ((node instanceof RenderableEntity) ||
			(node instanceof RenderableComplex)) {
			ReactionNode reactionNode = (ReactionNode) displayedObject;
			RenderableReaction reaction = reactionNode.getReaction();
			reactionNode.addComponent(node);
			reaction.addActivator(node);
			node.setContainer(reactionNode);
            displayedObject.setIsChanged(true);
			firePropertyChange("insert", displayedObject, node);
		}
		else
			throw new IllegalArgumentException("ReactionEditor.insertActivator(): node must be either RenderableEntity or RenderableComplex.");				
	}
	
	/**
	 * Override the super class' method to automatically remove branch if it is not used.
	 */
	public void delete(Renderable r) {
		if (r instanceof RenderableReaction)
			return;
		//Keep track the Widgets for removing branches
		java.util.List widgets = new ArrayList(r.getConnectInfo().getConnectWidgets());
		// Remove branches automatically. Have to call this before call super.delete()
		// Since the index of widgets will be updated for removing branches.
		ConnectWidget widget = null;
		int role;
		int index;
		RenderableReaction reaction = getReaction();
		for (Iterator it = widgets.iterator(); it.hasNext();) {
			widget = (ConnectWidget) it.next();
			role = widget.getRole();
			index = widget.getIndex();
			switch (role) {
				case HyperEdge.INPUT :
					if (reaction.getInputPoints() != null &&
						reaction.getInputPoints().size() > 1)
						reaction.removeInputBranch(index);
					break;
				case HyperEdge.OUTPUT :
					if (reaction.getOutputPoints() != null &&
						reaction.getOutputPoints().size() > 1)
						reaction.removeOutputBranch(index);
					break;
				case HyperEdge.CATALYST :
					reaction.removeHelperBranch(index);
					break;
				case HyperEdge.INHIBITOR :
					reaction.removeInhibitorBranch(index);
					break;
				case HyperEdge.ACTIVATOR :
					reaction.removeActivatorBranch(index);
					break;
			}
		}
		super.delete(r);
	}
	
	/**
	 * Override the super class method to prevent to delete the reaction.
	 */
	public java.util.List deleteSelection() {
		// Have to exclude the reaction
		java.util.List selection = getSelection();
		if (selection == null || selection.size() == 0)
			return new ArrayList();
		boolean isReactionSelected = false;
		RenderableReaction reaction = null;
		java.util.List deleted = new ArrayList();
		//Keep track the Widgets for removing branches
		java.util.List widgets = new ArrayList();
		for (Iterator it = selection.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			if (r instanceof RenderableReaction) {
				isReactionSelected = true;
				reaction = (RenderableReaction)r;
				it.remove();
			}
			else {
				deleted.add(r);
                 if (r.getConnectInfo().getConnectWidgets() != null)
                     widgets.addAll(r.getConnectInfo().getConnectWidgets());
			}
		}
		// Remove branches automatically. Have to call this before call super.deleteSelection()
		// Since the index of widgets will be updated for removing branches.
		ConnectWidget widget = null;
		int role;
		int index;
		if (reaction == null)
			reaction = ((ReactionNode)displayedObject).getReaction();
		for (Iterator it = widgets.iterator(); it.hasNext();) {
			widget = (ConnectWidget) it.next();
			role = widget.getRole();
			index = widget.getIndex();
			switch (role) {
				case HyperEdge.INPUT :
					if (reaction.getInputPoints() != null &&
						reaction.getInputPoints().size() > 1)
						reaction.removeInputBranch(index);
					break;
				case HyperEdge.OUTPUT :
					if (reaction.getOutputPoints() != null &&
						reaction.getOutputPoints().size() > 1)
						reaction.removeOutputBranch(index);
					break;
				case HyperEdge.CATALYST :
					reaction.removeHelperBranch(index);
					break;
				case HyperEdge.INHIBITOR :
					reaction.removeInhibitorBranch(index);
					break;
				case HyperEdge.ACTIVATOR :
					reaction.removeActivatorBranch(index);
					break;
			}
		}
		super.deleteSelection();
		if (isReactionSelected) {
			addSelected(reaction);
			repaint(getVisibleRect());
		}
		return deleted;
	}
	
	public void layoutRenderable() {
		RenderableReaction reaction = getReaction();
		// Set the editor size as default
		setSize(getVisibleRect().getSize());
		// Set the position at the center of the editor
		int x = getWidth() / 2;
		int y = getHeight() / 2;
		reaction.layout(new Point(x, y));
		revalidate();
		repaint(getVisibleRect());
		GraphEditorActionEvent e = new GraphEditorActionEvent(this, 
		                                                      GraphEditorActionEvent.MOVING);
		fireGraphEditorActionEvent(e);
	}
		
	/**
	 * Refresh the rendered RenderableReaction if it is a ReactionShortcut and
	 * its target might be changed.
	 */
	public void refresh() {
	}

	/**
	 * Remove the shortcuts from the target's shortcut list. This method
	 * should be called for a ReactionEditor that displays a ReactionShortcut.
	 */
	public void cleanUp() {
	}	
	
	public java.util.List getDisplayedObjects() {
		java.util.List list = null;
		if (displayedObject == null)
			return list;
		if (displayedObject.getComponents() != null)
			list = new ArrayList(displayedObject.getComponents());
		else
			list = new ArrayList();
		list.add(getReaction());
		return list;
	}
}
