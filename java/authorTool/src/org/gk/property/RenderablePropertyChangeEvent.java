/*
 * Created on May 25, 2004
 */
package org.gk.property;

import java.util.EventObject;

import org.gk.render.Renderable;

/**
 * An EventObject to describe the change of the property of a Renderable object.
 * @author wugm
 */
public class RenderablePropertyChangeEvent extends EventObject {

	private Renderable r;
	private String propName;
	private Object oldValue;
	private Object newValue;

	public RenderablePropertyChangeEvent(Renderable r,
	                                     String propName,
	                                     Object oldValue,
	                                     Object newValue) {
		super(r);
		this.r = r;
		this.propName = propName;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}
	
	public Renderable getRenderable() {
		return this.r;
	}
	
	public Object getNewValue() {
		return newValue;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public String getPropName() {
		return propName;
	}

	public void setNewValue(Object object) {
		newValue = object;
	}

	public void setOldValue(Object object) {
		oldValue = object;
	}

	public void setPropName(String string) {
		propName = string;
	}

	public void setRenderable(Renderable renderable) {
		r = renderable;
	}

}
