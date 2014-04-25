/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;

/**
 * 
 * @author wgm
 */
public interface SchemaClass extends Serializable {
	/**
	 * A list of SchemaAttributes.
	 * @return
	 */
	public java.util.Collection getAttributes();
	
	public SchemaAttribute getAttribute(String attributeName) throws InvalidAttributeException;
	
	public java.util.Collection getSuperClasses();
	
	public String getName();
	
	public boolean isAbstract();
	
	/**
	 * Return a list of all ancestors with the root as the first.
	 * @return
	 */
	public java.util.List getOrderedAncestors();
	
	public java.util.Collection getReferers();

	public boolean isValidAttribute(SchemaAttribute attribute);

	public boolean isValidAttribute(String attributeName);
	
	public boolean isValidAttributeOrThrow(String attributeName) throws InvalidAttributeException;
	
	/**
	 * For testing if the current class is an instance of another class
	 * @return
	 */
	public boolean isa(SchemaClass schemaClass);
	
	public boolean isa(String className);
}
