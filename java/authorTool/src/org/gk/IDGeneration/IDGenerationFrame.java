/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.border.BevelBorder;

import org.gk.database.AttributeEditConfig;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.AboutGKPane;
import org.gk.util.GKApplicationUtilities;

/** 
 *  This is the main class for the IDGeneration program.  It provides menu
 *  bar, tool bar and the IDGenerationPane, which allows detailed user interaction.
 * @author croft
 */
public class IDGenerationFrame  extends JFrame {
	public static final String ID_GENERATOR_NAME = "Reactome ID Generator";
	public static final String VERSION = "1.0";
	public static final int BUILD_NUMBER = 1;
	private IDGenerationController controller;
	private IDGenerationPane idGenerationPane;
	private Properties systemProperties;
	
	public IDGenerationFrame() {
		init();
	}
	
	private void init() {
		initFileAdaptor();
		
		systemProperties = SystemProperties.retrieveSystemProperties();
		String lfName = systemProperties.getProperty("lookAndFeel");
		if (lfName == null) {
			lfName = GKApplicationUtilities.getDefaultLF();
			systemProperties.setProperty("lookAndFeel", lfName);
		}
		GKApplicationUtilities.setLookAndFeel(lfName);
		
		// Display a launch window
		AboutGKPane aboutPane = new AboutGKPane(true, true);
		aboutPane.setStatus("Initializing...");
		aboutPane.setApplicationTitle(ID_GENERATOR_NAME);
		aboutPane.setVersion(VERSION);
		aboutPane.setBuildNumber(BUILD_NUMBER);
		aboutPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.lightGray, Color.black));
		JWindow window = new JWindow();
		window.getContentPane().add(aboutPane, BorderLayout.CENTER);
		window.setSize(378, 320);
		GKApplicationUtilities.center(window);
		window.setVisible(true);

		controller = new IDGenerationController(this);
		
		initMenuBar();
		
		JToolBar toolbar = createToolBar();
		getContentPane().add(toolbar, BorderLayout.NORTH);

		idGenerationPane = new IDGenerationPane(this);

		getContentPane().add(idGenerationPane, BorderLayout.CENTER);

		installListeners();
		
		// Make the frame visible
		setTitle(ID_GENERATOR_NAME);
		setSize(1000, 800);
		GKApplicationUtilities.center(this);
		setVisible(true);

		// Dispose of launch window
		window.setVisible(false);
		window.dispose();
	}
	
    private void initFileAdaptor() {
        try {
			XMLFileAdaptor adaptor = new XMLFileAdaptor();
			PersistenceManager.getManager().setActiveFileAdaptor(adaptor);
		} catch (Exception e) {
			System.err.println("initFileAdaptor: problems creating new file adaptor");
			e.printStackTrace();
		}
    }
    
	private void initMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
        JMenuItem quitItem = fileMenu.add(controller.getQuitAction());
		menuBar.add(fileMenu);
		
		JMenu toolsMenu = new JMenu("Tools");
        JMenuItem testItem = toolsMenu.add(controller.getTestAction());
        JMenuItem runItem = toolsMenu.add(controller.getRunAction());
        JMenuItem rollbackItem = toolsMenu.add(controller.getRollbackAction());
		menuBar.add(toolsMenu);
		
		JMenu databaseMenu = new JMenu("Database");
        databaseMenu.add(controller.getIdentifierDatabaseAction());
        databaseMenu.add(controller.getgk_centralAction());
		menuBar.add(databaseMenu);
		
		setJMenuBar(menuBar);
	}
	
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		
		if (controller!=null) {
			JButton testBtn = toolbar.add(controller.getTestAction());
			toolbar.addSeparator();
			JButton runBtn = toolbar.add(controller.getRunAction());
			toolbar.addSeparator();
			JButton rollbackBtn = toolbar.add(controller.getRollbackAction());
			toolbar.addSeparator();
			JButton checkVersionNumsBtn = toolbar.add(controller.getCheckVersionNumsAction());

			Insets insets = new Insets(0, 0, 0, 0);
			testBtn.setMargin(insets);
			runBtn.setMargin(insets);
			rollbackBtn.setMargin(insets);
		}
		
		return toolbar;
	}
	
	private void installListeners() {
	}
	
	static public void main(String[] args) {
		new IDGenerationFrame();
	}
	
	public void exit() {
		if (close())
			System.exit(0);
	}
	
	/**
	 * This method gets called when the user wants to quit.  It does
	 * any final cleanup first, then closes the frame.
	 * 
	 * @return
	 */
	public boolean close() {
	    if (!checkSave())
	        return false;

	    // Save any outstanding changes from the options dialog.
		AttributeEditConfig.getConfig().commit(systemProperties);
		
		// Save the window bounds
		Rectangle r = getBounds();
		String boundsStr = r.x + " " + r.y + " " + r.width + " " + r.height;
		systemProperties.setProperty("windowBounds", boundsStr);
		// Save the properties
		SystemProperties.storeSystemProperties();
		dispose();
		return true;
	}
	
	/**
	 * Returns true if nothing needs to be saved.
	 * 
	 * @return
	 */
	private boolean checkSave() {
		return true;
	}

	public IDGenerationController getController() {
		return controller;
	}

	public IDGenerationPane getIdGenerationPane() {
		return idGenerationPane;
	}
}