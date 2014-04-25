/*
 * Created on Dec 3, 2003
 */
package org.gk.database;

import java.awt.Component;
import java.util.EventObject;
import java.util.List;

import org.gk.model.GKInstance;

/**
 * An Event for editing attribute value.
 * @author wugm
 */
public class AttributeEditEvent extends EventObject {
	// Change types
	public static final int ADDING = 0;
	public static final int REMOVING = 1;
	public static final int UPDATING = 2;
	public static final int UNDEFINED = 3;
	public static final int REORDER = 4;
	private int editingType = UNDEFINED; // default
	private Component editingComponent;
	private String attributeName;
	private GKInstance editingInstance;
	private List<GKInstance> addedInstances;
	private List<GKInstance> removedInstances;
	
	public AttributeEditEvent(Object source) {
		super(source);
	}
	
	public AttributeEditEvent(Object source, GKInstance instance) {
		this(source);
		this.editingInstance = instance;
	}
	
	public AttributeEditEvent(Object source, GKInstance instance, String attributeName) {
		this(source);
		this.editingInstance = instance;
		this.attributeName = attributeName;
	}
	
	public AttributeEditEvent(Object source,
	                          GKInstance editingInstance,
	                          String attributeName,
	                          int editingType) {
		this(source);
		this.editingInstance = editingInstance;
		this.attributeName = attributeName;
		this.editingType = editingType;
	}
	
	public void setEditingInstance(GKInstance instance) {
		this.editingInstance = instance;
	}
	
	public GKInstance getEditingInstance() {
		return editingInstance;
	}
	
	/**
	 * @return the changed attribute name. null means all attributes might be changed.
	 */
	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String name) {
		this.attributeName = name;
	}
	
	public void setEditingComponent(Component comp) {
		this.editingComponent = comp;
	}
	
	public Component getEditingComponent() {
		return this.editingComponent;
	}
	
	public void setEditingType(int type) {
		this.editingType = type;
	}
	
	public int getEditingType() {
		return editingType;
	}
	
	public void setAddedInstances(List<GKInstance> addedInstances) {
		this.addedInstances = addedInstances;
	}
	
	public List<GKInstance> getAddedInstances() {
		return this.addedInstances;
	}
	
	public void setRemovedInstances(List<GKInstance> instances) {
		this.removedInstances = instances;
	}
	
	public List<GKInstance> getRemovedInstances() {
		return this.removedInstances;
	}
}
