/*
 * Created on May 28, 2004
 */
package org.gk.render;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This utility class is used to switch between ReactionNode and RenderablePathway.
 * This class is implemented as a sinlgeton.
 * @author wugm
 */
public class RenderableSwitchHelper {
	private static RenderableSwitchHelper helper;
	
	public static RenderableSwitchHelper getHelper() {
		if (helper == null)
			helper = new RenderableSwitchHelper();
		return helper;
	}
	
	private RenderableSwitchHelper() {
	}

	/**
	 * Switch a RenderablePathway to a ReactionNode. An empty pathway can be switched to a 
	 * reaction with the same name as the pathway bearing all properties from pathway. However
	 * a non-empty pathway (containing other events) cannot be switched, an IllegalArgumentException will be thrown.
	 * @param pathway
	 * @return
	 */
	public ReactionNode switchPathwayToReaction(RenderablePathway pathway) throws IllegalArgumentException {
		// Check to make sure pathway can be switched
		java.util.List components = pathway.getComponents();
		if (components != null && components.size() > 0) {
			for (Iterator it = components.iterator(); it.hasNext();) {
				Object obj = it.next();
				if (obj instanceof RenderablePathway ||
					obj instanceof ReactionNode) {
					throw new IllegalArgumentException("RenderableSwitchHelper.switchPathwayToReaction(): " +						"Cannot switch a pathway containing events to a reaction.");
				}
			}
		}
		ReactionNode reactionNode = new ReactionNode();
		RenderableReaction reaction = new RenderableReaction();
		reactionNode.setReaction(reaction);
		// Copy properties: All properites in RenderablePathway can be used in RenderableReaction.
		copyProperties(pathway, reactionNode);
		copyDispayProperties(pathway, reactionNode);
		reaction.initPosition(new Point(100, 100)); // Assign a default point
		// Have to figure out all necessary links and shortcuts.
		copyLinks(pathway, reactionNode);
		validateShortcuts(pathway, reactionNode);
		Renderable container = pathway.getContainer();
		container.removeComponent(pathway);
		container.addComponent(reactionNode);
		reactionNode.setContainer(container);
		return reactionNode;
	}
	
	private void copyProperties(Renderable source,
	                            Renderable target) {
		Object value = source.getAttributeValue(RenderablePropertyNames.ALIAS);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.ALIAS, value);
		value = source.getAttributeValue(RenderablePropertyNames.PRECEDING_EVENT);
		if (value != null) {
			// In case it contains self
			java.util.List list = (java.util.List) value;
			int index = list.indexOf(source);
			if (index >= 0)
				list.set(index, target);
			target.setAttributeValue(RenderablePropertyNames.PRECEDING_EVENT, value);
		}
		value = source.getAttributeValue(RenderablePropertyNames.SUMMATION);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.SUMMATION, value);
		value = source.getAttributeValue(RenderablePropertyNames.REFERENCE);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.REFERENCE, value);
		value = source.getAttributeValue(RenderablePropertyNames.ATTACHMENT);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.ATTACHMENT, value);
		value = source.getAttributeValue(RenderablePropertyNames.TAXON);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.TAXON, value);
		value = source.getAttributeValue(RenderablePropertyNames.LOCALIZATION);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.LOCALIZATION, value);
		value = source.getAttributeValue(RenderablePropertyNames.CREATED);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.CREATED, value);
		value = source.getAttributeValue(RenderablePropertyNames.MODIFIED);
		if (value != null)
			target.setAttributeValue(RenderablePropertyNames.MODIFIED, value);
		target.setID(source.getID());
		target.setDisplayName(source.getDisplayName());
		target.setReactomeId(source.getReactomeId());
	}
	
	private void copyDispayProperties(Renderable source, Renderable target) {
		// Copy display properties
		target.setBackgroundColor(source.getBackgroundColor());
		target.setForegroundColor(source.getForegroundColor());
		// Copy positions
		target.setPosition(new Point(source.getPosition()));
	}
	
	/**
	 * Switch a ReactionNode to a RenderablePathway. An empty reaction can be switched to pathway
	 * with the same display name and properties. However, a non-empty reaction cannot switch to
	 * pathway, a IllegaleArgument exception will be thrown.
	 * @param node
	 * @return
	 */
	public RenderablePathway switchReactionToPathway(ReactionNode node) {
		// Check to make sure pathway can be switched
		java.util.List components = node.getComponents();
		if (components != null && components.size() > 0) {
			for (Iterator it = components.iterator(); it.hasNext();) {
				Object obj = it.next();
				if (obj instanceof Node) {
					throw new IllegalArgumentException("RenderableSwitchHelper.switchReactonToPathway(): " +
						"Cannot switch a reaction containing molecules to a pathway.");
				}
			}
		}
		RenderablePathway pathway = new RenderablePathway();
		// Copy properties: All properites in RenderablePathway can be used in RenderableReaction.
		copyProperties(node, pathway);
		copyDispayProperties(node, pathway);
		// Have to figure out all necessary links and shortcuts.
		copyLinks(node, pathway);
		validateShortcuts(node, pathway);
		Renderable container = node.getContainer();
		container.removeComponent(node);
		container.addComponent(pathway);
		pathway.setContainer(container);
		return pathway;
	}
	
	private void copyLinks(Renderable source, Renderable target) {
		ConnectInfo connectInfo = source.getConnectInfo();
		target.setConnectInfo(connectInfo);
		// Switch links to target
		if (connectInfo.getConnectWidgets() != null) {
			ConnectWidget widget = null;
			for (Iterator it = connectInfo.getConnectWidgets().iterator(); it.hasNext();) {
				widget = (ConnectWidget) it.next();
				Renderable edge = widget.getEdge();
				if (edge == null)
					continue;
				if (edge.getConnectInfo().getConnectWidgets() != null) {
					for (Iterator it1 = edge.getConnectInfo().getConnectWidgets().iterator(); it1.hasNext();) {
						ConnectWidget widget1 = (ConnectWidget) it1.next();
						if (widget1.getConnectedNode() == source) {
							widget1.setConnectedNode((Node)target);
							break;
						}
					}
				}
			}
		}
	}
	
	private void validateShortcuts(Renderable source, Renderable target) {
		java.util.List needAddNodes = new ArrayList();
		if (source instanceof Shortcut) {
			needAddNodes.add(((Shortcut)source).getTarget());
			java.util.List shortcuts = ((Shortcut)source).getTarget().getShortcuts();
			if (shortcuts != null) {
				needAddNodes.addAll(shortcuts);
				needAddNodes.remove(source);
			}
		}
		else {
			java.util.List shortcuts = source.getShortcuts();
			if (shortcuts != null) {
				needAddNodes.addAll(shortcuts);
			}
		}
		if (needAddNodes.size() == 0)
			return;
		for (Iterator it = needAddNodes.iterator(); it.hasNext();) {
			Renderable node = (Renderable) it.next();
			Renderable targetShortcut = (Renderable) target.generateShortcut();
			copyDispayProperties(node, targetShortcut);
			copyLinks(node, targetShortcut);
			Renderable container = node.getContainer();
			container.removeComponent(node);
			container.addComponent(targetShortcut);
			targetShortcut.setContainer(container);
		}
	}
}
