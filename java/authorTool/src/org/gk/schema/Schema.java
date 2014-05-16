/*
 * Created on Jun 27, 2003
 */
package org.gk.schema;

import java.io.Serializable;

/**
 * A generic schema definition.
 * @author wgm
 */
public interface Schema extends Serializable {
	
	/**
	 * Some constants.
	 */
	public static final String DB_ID_NAME = "DB_ID";
	
	/**
	 * Get the whole list of all classes in the schema.
	 * @return a list of SchemaClasses.
	 */
	public java.util.Collection getClasses();
	
	/**
	 * Get the whole list of all class names.
	 * @return a list of name Strings.
	 */
	public java.util.Collection getClassNames();
	
	public SchemaClass getClassByName(String name);
	
	/**
	 * Check is a class with the specified name is a valid (i.e. defined)
	 * schema class.
	 * @param name the class name.
	 * @return 
	 */
	public boolean isValidClass(String name);
	
	public boolean isValidClass(SchemaClass class1);
	
	public boolean isValidClassOrThrow(String name) throws InvalidClassException;
	
	public String getTimestamp();
}
