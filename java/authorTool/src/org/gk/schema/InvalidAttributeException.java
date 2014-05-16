/*
 * Created on Sep 8, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.schema;

/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class InvalidAttributeException extends Exception {
	public InvalidAttributeException(SchemaClass cls, SchemaAttribute att) {
		super("Invalid attribute '" + att.getName() + "' for class '" + cls.getName() + "'.");
	}
	
	public InvalidAttributeException(SchemaClass cls, String attName) {
		super("Invalid attribute '" + attName + "' for class '" + cls.getName() + "'.");
	}
	
}
