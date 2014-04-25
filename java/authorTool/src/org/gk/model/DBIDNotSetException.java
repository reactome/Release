/*
 * Created on Oct 31, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gk.model;

/**
 * @author vastrik
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DBIDNotSetException extends Exception {

	/**
	 * 
	 */
	public DBIDNotSetException(Instance instance) {
		super("Instance '" + instance.toString() + "' has no DBID");
	}
	
	public DBIDNotSetException(String message) {
	    super(message);
	}

}
