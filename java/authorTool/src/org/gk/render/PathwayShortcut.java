/*
 * Created on Sep 8, 2003
 */
package org.gk.render;

import java.util.List;

/**
 * A Shortcut to a RenderablePathway.
 * @author wgm
 */
public class PathwayShortcut extends RenderablePathway implements Shortcut {
	
	private Renderable target;
	
	public PathwayShortcut(Renderable pathway) {
		this.target = pathway;
	}
	
	public Renderable getTarget() {
		return this.target;
	}
	
	public String getDisplayName() {
		return target.getDisplayName();
	}
	
	public void setDisplayName(String name) {
		target.setDisplayName(name);
	}
	
	public void addComponent(Renderable comp) {
		target.addComponent(comp);
	}
	
	public List getComponents() {
		return target.getComponents();
	}
	public void setAttributeValue(String attributeName, Object value) {
		target.setAttributeValue(attributeName, value);
	}
    
    public boolean isChanged() {
        return target.isChanged();
    }

	public Object getAttributeValue(String attributeName) {
		return target.getAttributeValue(attributeName);
	}
}
