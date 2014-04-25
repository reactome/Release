/*
 * Created on Jun 25, 2003
 */
package org.gk.render;

import java.io.Serializable;


/**
 * This class describes a task.
 * @author wgm
 */
public class GKTask implements Serializable {
	// The object that this task should be done on. 
	private Renderable target; // read-only property
	private String propertyName; // read-only property
	private String description; // read-only property
	private boolean isUserDefined; // Mark if this task is specified by the user.
	
	public GKTask(Renderable target, String label, String desc) {
		this.target = target;
		this.description = desc;
		this.propertyName = label;
	}
	
	public GKTask(Renderable target, String label, String desc, boolean isUserDefined) {
		this(target, label, desc);
		this.isUserDefined = isUserDefined;
	}
	
	public Renderable getTarget() {
		return this.target;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public String getPropertyName() {
		return this.propertyName;
	}
	
	public String toString() {
		return getDescription();
	}
	
	public boolean isUserDefined() {
		return this.isUserDefined;
	}

}
