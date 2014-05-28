/*
 * Created on May 3, 2004
 */
package launcher;

import java.awt.Component;

import javax.swing.Action;

/**
 * An interface for launching an application.
 * @author wugm
 */
public interface Launchable {

	/**
	 * Launch the application.
	 */
    public void launch();
	
	/**
	 * Get the application name.
	 * @return the application name.
	 */
    public String getApplicationName();
	
    /**
     * Display a hue indicating an update is available.
     *
     */
	public void showUpdateAvailable(Action updateAction);
	
	/**
	 * Close the appliation.
	 *
	 */
	public boolean close();
	
	/**
	 * Add an action so that it can be launched in the application.
	 * @param action
	 */
	public void addCheckUpdateAction(Action action);
	
	public Component getUserFrame();
    
    /**
     * Add an action to update the schema.
     */
    public void addUpdateSchemaAction(Action action);
    
    /**
     * Use this method to update schema. However, the application
     * should be started to take this effect of new schema usually.
     * Restarting is not required in this method.
     * @return true if updating is successful.
     */
    public boolean updateSchema();

}
