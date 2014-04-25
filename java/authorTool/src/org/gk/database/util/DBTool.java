/*
 * Created on Oct 20, 2003
 */
package org.gk.database.util;

/**
 * An interface for add a tool to the database browser.
 * @author wgm
 */
public interface DBTool {

	/**
	 * A title that can be displayed under the menu.
	 * @return
	 */
	public String getTitle();
	
	/**
	 * Actual method to do something for the database.
	 */
	public void doAction();

}
