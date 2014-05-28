/*
 * Created on Apr 21, 2005
 */
package org.gk.model;

import org.gk.schema.GKSchemaAttribute;

/**
 * An instance is referred in another instance's attribute. However,
 * the class of the first instance is not allowed for the second instance's
 * attribute. This exception describes this scenario.
 * @author wgm
 */
public class AttributeClassNotAllowedException extends Exception {
	// The original instance
	private GKInstance target;
	// Instance referred by the target
	private GKInstance reference;
	// Attribute in that reference is in
	private GKSchemaAttribute att;
	
	public AttributeClassNotAllowedException() {
		
	}
	
	public AttributeClassNotAllowedException(String message) {
		
	}
	
	/**
	 * An overloaded constructor. 
	 * @param instance the instance that use reference
	 * @param reference the reference whose class type is not allowed
	 * @param att where reference is used in instance
	 */
	public AttributeClassNotAllowedException(GKInstance instance,
			                                 GKInstance reference,
											 GKSchemaAttribute att) {
		super(reference.getDisplayName() + "'s class " + "\"" + reference.getSchemClass().getName() +
			  "\" is not allowed in attribute \"" + att.getName() + "\" for " + instance.getDisplayName());
		this.target = instance;
		this.reference = reference;
		this.att = att;
	}
	
	/**
	 * Get the GKInstance object that refers to a wrong instance.
	 * @return
	 */
	public GKInstance getInstance() {
		return target;
	}
	
	/**
	 * Get the reference.
	 * @return
	 */
	public GKInstance getReference() {
		return this.reference;
	}
	
	/**
	 * Get the GKSchemaAttribute object where reference is wrong.
	 * @return
	 */
	public GKSchemaAttribute getSchemaAttribute() {
		return this.att;
	}
	
}
