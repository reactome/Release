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
public class InvalidAttributeValueException extends Exception {
	public InvalidAttributeValueException (SchemaAttribute att, Object value) {
        // value might be null. Don't use value.toString
		super("Invalid value '" + value + "' for attribute '" + att.getName() + "'.");
	}
}
