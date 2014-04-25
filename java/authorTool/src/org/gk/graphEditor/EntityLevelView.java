/*
 * Created on Mar 31, 2004
 */
package org.gk.graphEditor;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.gk.render.Node;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;
import org.gk.util.GraphLayoutEngine;

/**
 * An entity level view of a pathway.
 * @author wugm
 */
public class EntityLevelView extends ReactionLevelView {
	// Displayed RenderablePathway
	private RenderablePathway pathway;

	public EntityLevelView() {
	}
	
	public EntityLevelView(RenderablePathway pathway) {
		this();
		setPathway(pathway, true);
	}
	
	public void setPathway(RenderablePathway pathway, boolean needShortcut) {
		extractRenderInfo(pathway, needShortcut);
	}
	
	protected void extractRenderInfo(RenderablePathway pathway, boolean needShortcut) {
		java.util.List comps = displayedObject.getComponents();
		if (comps != null)
			comps.clear();
		if (pathway.getComponents() == null ||
		    pathway.getComponents().size() == 0)
			return;
		// Get all RenderableReactions
		java.util.List reactions = new ArrayList();
		java.util.List current = new ArrayList();
		java.util.List next = new ArrayList();
		current.add(pathway);
		Renderable r = null;
		while (current.size() > 0) {
			for (Iterator it = current.iterator(); it.hasNext();) {
				r = (Renderable) it.next(); // r should be a RenderablePathway.
				if (r.getComponents() != null) {
					for (Iterator it1 = r.getComponents().iterator(); it1.hasNext();) {
						Renderable r1 = (Renderable) it1.next();
						if (r1 instanceof Shortcut)
							continue;
						if (r1 instanceof RenderablePathway)
							next.add(r1);
						else if (r1 instanceof ReactionNode)
							reactions.add(r1);
					}
				}
			}
			current.clear();
			current.addAll(next);
			next.clear();
		}
		// Extract all nodes contained in the reactions.
		Map nodeMap = new HashMap(); // Mape for maping displayName to Node
		ReactionNode reaction = null;
		if (needShortcut) { 
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				reaction = (ReactionNode) it.next();
				if (reaction.getComponents() == null ||
					reaction.getComponents().size() == 0)
					continue;
				for (Iterator it1 = reaction.getComponents().iterator(); it1.hasNext();) {
					r = (Renderable) it1.next();
					if (nodeMap.containsKey(r.getDisplayName()))
						continue;
					if (r instanceof Shortcut) 
						r = ((Shortcut)r).getTarget();
					// Use shortcut
					nodeMap.put(r.getDisplayName(), r.generateShortcut());
				}
			}
		}
		else { 
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				reaction = (ReactionNode)it.next();
				if (reaction.getComponents() == null || reaction.getComponents().size() == 0)
					continue;
				for (Iterator it1 = reaction.getComponents().iterator(); it1.hasNext();) {
					r = (Renderable)it1.next();
					if (nodeMap.containsKey(r.getDisplayName()))
						continue;
					if (r instanceof Shortcut)
						nodeMap.put(r.getDisplayName(), ((Shortcut)r).getTarget());
					else
						nodeMap.put(r.getDisplayName(), r);
				}
			}
		}
		// Add all nodes first
		for (Iterator it = nodeMap.values().iterator(); it.hasNext();) {
			r = (Renderable) it.next();
			// Don't call the following method. It is too heavy for this purpose.
			//insertNode((Node)r);
			displayedObject.addComponent(r);
		}
		// Add reaction edges
		RenderableReaction reactionEdge = null;
		java.util.List list = null;
		for (Iterator it = reactions.iterator(); it.hasNext();) {
			reaction = (ReactionNode) it.next();
			if (reaction.getComponents() == null || reaction.getComponents().size() == 0)	
				continue; // Don't add an empty reaction
			reactionEdge = new RenderableReaction();
			reactionEdge.setDisplayName(reaction.getDisplayName());
			// Don't call insertEdge(). It is too heavy for this purpose.
			displayedObject.addComponent(reactionEdge);
			reactionEdge.initPosition((Point)reaction.getPosition().clone());
			// Attach inputs
			list = reaction.getReaction().getInputNodes();
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					Renderable input = (Renderable) it1.next();
					Node addNode = (Node) nodeMap.get(input.getDisplayName());
					reactionEdge.addInput(addNode);
				}
			}
			list = reaction.getReaction().getOutputNodes();
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					Renderable output = (Renderable) it1.next();
					Node addNode = (Node) nodeMap.get(output.getDisplayName());
					reactionEdge.addOutput(addNode);
				}
			}
			list = reaction.getReaction().getHelperNodes();
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					Renderable helper = (Renderable) it1.next();
					Node addNode = (Node) nodeMap.get(helper.getDisplayName());
					reactionEdge.addHelper(addNode);
				}
			}
			list = reaction.getReaction().getInhibitorNodes();
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					Renderable inhibitor = (Renderable) it1.next();
					Node addNode = (Node) nodeMap.get(inhibitor.getDisplayName());
					reactionEdge.addInhibitor(addNode);
				}
			}
			list = reaction.getReaction().getActivatorNodes();
			if (list != null && list.size() > 0) {
				for (Iterator it1 = list.iterator(); it1.hasNext();) {
					Renderable activator = (Renderable) it.next();
					Node addNode = (Node) nodeMap.get(activator.getDisplayName());
					reactionEdge.addActivator(addNode);
				}
			}
		}
	}
	
	public void layoutPathway(int type) {
		GraphLayoutEngine engine = GraphLayoutEngine.getEngine();
		// Change dimension for graph
		int oldLayerDist = engine.getLayerdDist();
		int oldNodeSep = engine.getNodeSep();
		int oldEdgeLen = engine.getEdgeLen();
		// Use half value
		engine.setLayerDist(oldLayerDist / 3);
		engine.setNodeSep(oldNodeSep / 2);
		engine.setEdgeLen(oldEdgeLen / 2);
		super.layoutPathway(type);
		// Reset dimension for graph
		engine.setLayerDist(oldLayerDist);
		engine.setNodeSep(oldNodeSep);
		engine.setEdgeLen(oldEdgeLen);
	}
	
	protected void displaySelected() {
		if (getSelection().size() != 1)
			return;
		Object obj = getSelection().get(0);
		if (obj instanceof RenderableReaction) {
			displayReaction((RenderableReaction)obj);	
		}
		else if (obj instanceof RenderableComplex) {
			JFrame parentFrame = (JFrame) SwingUtilities.getRoot(this);
			JDialog dialog = new JDialog(parentFrame);
			displayComplex((RenderableComplex)obj, dialog);
		}
	}
}
