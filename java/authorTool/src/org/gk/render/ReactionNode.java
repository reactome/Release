/*
 * Created on Jul 18, 2003
 */
package org.gk.render;

import java.util.ArrayList;

/**
 * This Node is used to display a RenderableReaction.
 * @author wgm
 */
public class ReactionNode extends Node {
	private RenderableReaction reaction;
	
	public ReactionNode() {
		//boundsBuffer = 8;
	}
	
	public ReactionNode(RenderableReaction reaction) {
		this();
		setReaction(reaction);
	}
	
	public void setReaction(RenderableReaction reaction) {
		this.reaction = reaction;
		setDisplayName(reaction.getDisplayName());
		reaction.setContainer(this); // Make the reaction contained in this node.
	}
	
	public RenderableReaction getReaction() {
		return this.reaction;
	}
	
	public java.util.List getComponents() {
		return this.components;
	}
	
	public void setDisplayName(String name) {
		super.setDisplayName(name);
		if (reaction != null) {
			if (reaction.getDisplayName() == null) {
				reaction.setDisplayName(name);
			}
			else if (!reaction.getDisplayName().equals(name)){
				reaction.setDisplayName(name);
			}
		}
	}
	
	public Renderable generateShortcut() {
		ReactionNode shortcut = new ReactionNode();
		generateShortcut(shortcut);
        return shortcut;
	}
	
	public void addComponent(Renderable renderable) {
		if (components == null)
			components = new ArrayList();
		if (renderable instanceof RenderableReaction)
			setReaction((RenderableReaction)renderable);
		else
			components.add(renderable);
	}
	
	public Object removeComponent(Renderable renderable) {
		if (components != null) {
			if(components.remove(renderable))
				return renderable;
		}
		return null;
	}

	public Object getAttributeValue(String attributeName) {
		if (reaction != null)
			return reaction.getAttributeValue(attributeName);
		return null;
	}

	public void setAttributeValue(String attributeName, Object value) {
		if (reaction != null)
			reaction.setAttributeValue(attributeName, value);
	}
	
	public String getType() {
		return "Reaction";
	}
}
