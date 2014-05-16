/*
 * Created on Sep 23, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.Border;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * Use this class to manage all JFrames that are used in this package. This is
 * a singleton.
 * @author wgm
 */
public class FrameManager implements AttributeEditListener, PropertyChangeListener {
	private static FrameManager instance;
	// A map to register all opened JFrames
	private Map frames;
	// A WindowListener to un-register the JFrame.
	private WindowListener closeFrameListener;
	private Image iconImage;
	// There should be only one browser in the application.
	private GKDatabaseBrowser browser;
	private PropertyChangeSupport propertySupport;
	// Event View
	private JFrame eventViewFrame;
	private EventCentricViewPane eventViewPane;
    // A list of actions for EventView
    private List additionalActionsForEventView;
    // These two actions are used to launch DB schema view and event view
    private Action dbSchemaViewAction;
    private Action dbEventViewAction;
	
	private FrameManager() {
		frames = new HashMap();
		closeFrameListener = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				JFrame sourceFrame = (JFrame)e.getSource();
				AttributePane attributePane = extractAttributePane(sourceFrame);
				if (attributePane != null) {
					frames.remove(attributePane.getInstance());
					int size = frames.size();
					propertySupport.firePropertyChange("remove", size + 1, size);
				}
			}	
		};
		propertySupport = new PropertyChangeSupport(this);
	}
	
	public static FrameManager getManager() {
		if (instance == null)
			instance = new FrameManager();
		return instance;
	}
    
    public void setAdditionalActionsForEventView(List actions) {
        this.additionalActionsForEventView = actions;
    }
	
	public void setIconImage(Image image) {
		this.iconImage = image;
	}
	
	public void setDBSchemaViewAction(Action action) {
	    this.dbSchemaViewAction = action;
	}
	
	public void setDBEventViewAction(Action action) {
	    this.dbEventViewAction = action;
	}
	
	public void showEventView(final MySQLAdaptor adaptor, 
	                          final boolean useShortMenu, 
	                          final int popupType) {
	    if (eventViewFrame != null) {
	        eventViewFrame.setState(JFrame.NORMAL);
	        eventViewFrame.toFront();
	        return ;
	    }
	    String title = adaptor.getDBName() + "@" + adaptor.getDBHost() + " - Event Hierarchical View";
	    eventViewFrame = new JFrame(title);
	    eventViewPane = new EventCentricViewPane();
	    if (additionalActionsForEventView != null) {
            for (Iterator it = additionalActionsForEventView.iterator();
                 it.hasNext();) {
                Action action = (Action) it.next();
                eventViewPane.getEventPane().addAdditionalPopupAction(action);
            }
        }
	    eventViewFrame.getContentPane().add(eventViewPane, BorderLayout.CENTER);
	    eventViewFrame.setSize(900, 800);
	    eventViewPane.setIsForDB(true);
		if (popupType == GKDBBrowserPopupManager.AUTHOR_TOOL_TYPE)
			eventViewPane.getEventPane().setPopupType(EventPanePopupManager.DB_AUTHOR_TOOL_TYPE);
		else if (popupType == GKDBBrowserPopupManager.CURATOR_TOOL_TYPE)
			eventViewPane.getEventPane().setPopupType(EventPanePopupManager.DB_CURATOR_TOOL_TYPE);
		// Place this call after GUI set up to display the progress pane
		eventViewPane.setMySQLAdaptor(adaptor);
		if (iconImage != null)
			eventViewFrame.setIconImage(iconImage);
		eventViewFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
			    String boundsStr = GKApplicationUtilities.generateWindowBoundsString(eventViewFrame);
			    Properties prop = GKApplicationUtilities.getApplicationProperties();
			    prop.setProperty("dbEventBrowserBounds", boundsStr);
				eventViewFrame = null;
			}
		});
		// Add a simple menu
		JMenuBar menubar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
//		JMenuItem schemaViewItem = new JMenuItem("Schema View");
//		schemaViewItem.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				openBrowser(adaptor, useShortMenu, popupType);
//			}
//		});
		JMenuItem refreshItem = new JMenuItem("Refresh");
		refreshItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// A way to refresh the dispalyed events.
				eventViewPane.setMySQLAdaptor(adaptor);
			}
		});
		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventViewFrame.dispose();
				eventViewFrame = null;
			}
		});
		if (dbSchemaViewAction != null) {
		    fileMenu.add(dbSchemaViewAction);
		    fileMenu.addSeparator();
		}
		fileMenu.add(refreshItem);
		fileMenu.addSeparator();
		fileMenu.add(closeItem);
		menubar.add(fileMenu);
		eventViewFrame.setJMenuBar(menubar);
		GKApplicationUtilities.center(eventViewFrame);
		Properties prop = GKApplicationUtilities.getApplicationProperties();
		if (prop != null) {
		    String boundsStr = prop.getProperty("dbEventBrowserBounds");
		    if (boundsStr != null)
		        GKApplicationUtilities.setWindowBoundsFromString(eventViewFrame, boundsStr);
		}
		eventViewFrame.setVisible(true);
	}
	
	public GKDatabaseBrowser openBrowser(MySQLAdaptor adaptor, int popupType) {
		return openBrowser(adaptor, false, popupType);
	}
	
	public GKDatabaseBrowser openBrowser(MySQLAdaptor adaptor, 
	                                     final boolean useShortMenu,
	                                     int popupType) {
		if (browser != null) {
			browser.setState(JFrame.NORMAL);
			browser.toFront();
		}
		else {
			GKDatabaseBrowser browser1 = new GKDatabaseBrowser(adaptor, 
			                                                   useShortMenu,
			                                                   dbEventViewAction);
			browser1.setPopupType(popupType);
			if (browser1.isDisplayable()) {
				browser = browser1;
				if (iconImage != null)
					browser.setIconImage(iconImage);
				browser.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
                        // Keep track the bounds
                        String boundsStr = GKApplicationUtilities.generateWindowBoundsString(browser);
                        Properties prop = GKApplicationUtilities.getApplicationProperties();
                        if (prop != null)
                            prop.setProperty("dbSchemaBrowserBounds", boundsStr);
					    if (useShortMenu) {
							closeSchemaBrowser();
						}
						else
							closeBrowser();
					}
				});
				// Set the bounds of this browser
				Properties prop = GKApplicationUtilities.getApplicationProperties();
				String boundsStr = prop.getProperty("dbSchemaBrowserBounds");
				if (boundsStr != null)
				    GKApplicationUtilities.setWindowBoundsFromString(browser, boundsStr);;
				browser.setVisible(true);
			}
		}
		return browser;		
	}
	
	public GKDatabaseBrowser getBrowser() {
		return this.browser;
	}
    
    public JFrame getEventViewFrame() {
        return this.eventViewFrame;
    }
    
    public EventCentricViewPane getEventViewPane() {
        return this.eventViewPane;
    }
    
	/**
	 * Close the opened GKDatabaseBrowser.
	 */
	public void closeBrowser() {
		closeAll();
		if (browser != null) {
			browser.dispose();
			browser = null;
		}
		if (eventViewFrame != null) {
			eventViewFrame.dispose();
			eventViewFrame = null;
		}
	}
	
	public void closeSchemaBrowser() {
		if (browser != null) {
			browser.setVisible(false);
			browser.dispose();
			browser = null;
		}
	}
	
	/**
	 * Close all opened Frames for Instances
	 */
	public void closeAll() {
		if (frames.size() == 0)
			return;
		int size = frames.size();
		JFrame frame = null;
		for (Iterator it = frames.values().iterator(); it.hasNext();) {
			frame = (JFrame) it.next();
			frame.setVisible(false);
			frame.dispose();
		}
		frames.clear();
		propertySupport.firePropertyChange("remove", size, 0);
	}
	
	/**
	 * Close all JFrames that are opened for local GKInstances.
	 */
	public void closeAllForLocal() {
	    if (frames.size() == 0)
	        return;
	    int counter = 0;
	    GKInstance instance = null;
	    JFrame frame = null;
	    for (Iterator it = frames.keySet().iterator(); it.hasNext();) {
	        instance = (GKInstance) it.next();
	        if (instance.getDbAdaptor() instanceof XMLFileAdaptor) {
	            frame = (JFrame) frames.get(instance);
	            frame.setVisible(false);
	            frame.dispose();
	            it.remove();
	            counter ++;
	        }
	    }
	    if (counter == 0)
	        return;
	    propertySupport.firePropertyChange("remove", frames.size() + counter, frames.size());
	}
	
	/**
	 * Close the opened attribute frame for a specified GKInstance.
	 * @param instance
	 */
	public void close(GKInstance instance) {
		JFrame frame = (JFrame) frames.get(instance);
		if (frame != null) {
			frame.dispose();
			frames.remove(instance);
			propertySupport.firePropertyChange("remove", frames.size() + 1, frames.size() - 1);
		}
	}
	
	/**
	 * A simple method to arrange all opened instance Frames.
	 */
	public void arrangeAll() {
		int size = frames.size();
		if (size == 0)
			return;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = 0;
		int y = 0;
		java.util.List frameList = new ArrayList(frames.values());
		int col = 3;
		int tmp = (int)Math.sqrt(size);
		if (tmp > 0 && (size / tmp == tmp)) // Make it a square
			col = tmp;
		// A very naive arrange method. The colum is 3 at most.
		if (size <= col) {
			int w = screenSize.width / size;
			for (int i = 0; i < size; i++) {
				JFrame frame = (JFrame) frameList.get(i);
				frame.setLocation(x, y);
				frame.setSize(w, screenSize.height);
				frame.validate();
				frame.setState(JFrame.NORMAL);
				frame.toFront();
				x += w;
			}
		}
		else {
			int row;
			if (size % col == 0) 
				row = size / col;
			else
				row = size / col + 1;
			int index = 0;
			int h = screenSize.height / row;
			int w = screenSize.width / col;
			while (index < size) {
				for (int i = 0; i < col; i++) {
					if (index > size - 1)
						break;
					JFrame frame = (JFrame)frameList.get(index);
					frame.setLocation(x, y);
					frame.setSize(w, h);
					frame.validate();
					frame.setState(JFrame.NORMAL);
					frame.toFront();
					index ++;
					x += w;
				}
				x = 0;
				y += h;
			}
		}
	}
	
	private void showFrame(String title) {
		for (Iterator it = frames.keySet().iterator(); it.hasNext();) {
			Instance instance = (Instance) it.next();
			if (instance.toString().equals(title)) {
				JFrame frame = (JFrame) frames.get(instance);
				frame.setState(JFrame.NORMAL);
				frame.toFront();
				break;
			}
		}
	}
	
	public void showShellInstanceInDB(GKInstance instance, JComponent comp) {
	    showShellInstanceInDB(instance, comp, null);
	}
	
	public void showShellInstanceInDB(GKInstance instance, JComponent comp, JDialog parentDialog) {
		// Get the database copy
		try {
			MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(comp);
            if (adaptor == null) {
				JOptionPane.showMessageDialog(comp,
				                              "Cannot connect to the database.",
				                              "Error in DB Connecting",
				                              JOptionPane.ERROR_MESSAGE);
				return ;
			}
			Collection c = adaptor.fetchInstanceByAttribute(instance.getSchemClass().getName(),
															"DB_ID",
															"=",
															instance.getDBID());
			if (c.size() == 0) {
				JOptionPane.showMessageDialog(comp,
											  "Cannot find the original instance object in the database.\n" + 
											  "It might be deleted.",
											  "Cannot Find Instance",
											  JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			GKInstance dbCopy = (GKInstance) c.iterator().next();
			// dbCopy might be a shell if there is a shell copy just checked out since it is cached in the
			// memory and isShell flag is not reset.
			dbCopy.setIsShell(false);
			if (parentDialog != null)
			    showInstance(dbCopy, parentDialog, false, true);
			else
			    showInstance(dbCopy, false, true);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(comp,
										  "Cannot fetch objects from the database: \n" + e,
										  "Error in Database Connection",
										  JOptionPane.ERROR_MESSAGE);
			System.err.println("FrameManager.showShellInstance(): " + e);
			e.printStackTrace();
		}		    
	}
	
	/**
	 * Show a SHELL instance.
	 * @param instance
	 */
	public void showShellInstance(GKInstance instance, JComponent comp) {
		showShellInstance(instance, comp, null);
	}
	
	public void showShellInstance(GKInstance instance, JComponent comp, JDialog parentDialog) {
		// Always display this dialog: If a GKInstance has already been displayed from DB, the user should be asked if
		// he/she wants to view it in the db. Otherwise, it will be confused.
		int reply = JOptionPane.showConfirmDialog(comp,
									  			 "This is a shell instance. Do you want to view it in the database.",
									             "View Shell Instance",
									             JOptionPane.YES_NO_CANCEL_OPTION);
		if (reply == JOptionPane.NO_OPTION) {
			if (parentDialog != null)
			    showInstance(instance, parentDialog);
			else
			    showInstance(instance);
		}
		else if (reply == JOptionPane.YES_OPTION){
			showShellInstanceInDB(instance, comp, parentDialog);	    
		}
	}
	
	private JLabel createLocationLabel(GKInstance instance) {
		JLabel locationLabel = null;
		if (instance.getDbAdaptor() instanceof XMLFileAdaptor) {
			locationLabel = new JLabel("@Local Repository");
		}
		else if (instance.getDbAdaptor() instanceof MySQLAdaptor) {
			locationLabel = new JLabel("@Database Repository");
		}
		Border border1 = BorderFactory.createLoweredBevelBorder();
		Border border2 = BorderFactory.createEmptyBorder(4, 4, 4, 4);
		Border border3 = BorderFactory.createCompoundBorder(border1, border2);
		locationLabel.setBorder(border3);
		return locationLabel;
	}
	
	/**
	 * Show an GKInstance. 
	 * @param instance the Instance to be displayed.
	 * @param isEditable true for displaying an GKInstnace that is editable.
	 */
	public void showInstance(GKInstance instance, boolean isEditable) {
		showInstance(instance, isEditable, false);	
	}
	
	private void showInstance(GKInstance instance, boolean isEditable, boolean needCheckoutBtn) {
		String title = instance.toString();
		if (frames.containsKey(instance)) {
			JFrame frame = (JFrame) frames.get(instance);
			frame.setState(JFrame.NORMAL);
			frame.toFront();
			return;
		}
		JFrame frame = new JFrame(title);
		if (iconImage != null)
			frame.setIconImage(iconImage);
		AttributePane propPane = new AttributePane(instance);
		propPane.setEditable(isEditable);
		if (needCheckoutBtn)
		    propPane.showCheckoutButton();
//		if (isEditable) {
		    // Display attributes only
		frame.getContentPane().add(propPane, BorderLayout.CENTER);
		// Don't delete the following code: it is too slow this the following code
		// is applied to an IE that is referred by multiple instances. For the time
		// being, this feature will not be enabled.
//		}
//		else { // Used for database display: no need update for the referrers pane.
//		    JTabbedPane tabbedPane = new JTabbedPane();
//		    tabbedPane.add("Attributes", propPane);
//		    // Display referres
//		    ReverseAttributePane referrersPane = new ReverseAttributePane();
//		    referrersPane.hideClosePane();
//		    referrersPane.setGKInstance(instance);
//		    referrersPane.setParentComponent(frame);
//		    tabbedPane.add("Referrers", referrersPane);
//		    frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
//		}
		// Add a title to display the localization
		frame.getContentPane().add(createLocationLabel(instance), BorderLayout.SOUTH);
		frame.setSize(600, 700);
		GKApplicationUtilities.center(frame);
		frames.put(instance, frame);
		frame.addWindowListener(closeFrameListener);
		frame.setVisible(true);
		frame.requestFocus();
		int size = frames.size();
		propertySupport.firePropertyChange("add", size - 1, size);			    
	}
	
	/**
	 * Display a GKInstnace that is not editable.
	 * @param instance
	 */
	public void showInstance(GKInstance instance) {
		showInstance(instance, false);
	}
	
	/**
	 * An overloaded method to show an GKInstance in a modal JDialog.
	 * @param instance
	 * @param parentDialog
	 */
	public void showInstance(GKInstance instance, JDialog parentDialog) {
		showInstance(instance, parentDialog, false, false);
	}
	
	public void showInstance(GKInstance instance, JDialog parentDialog, boolean isEditable) {
		showInstance(instance, parentDialog, isEditable, false);		
	}
	
	private void showInstance(GKInstance instance, 
	                          JDialog parentDialog, 
	                          boolean isEditable, 
	                          boolean needCheckoutBtn) {
		JDialog dialog = new JDialog(parentDialog, "Properties: " + instance.toString());
		AttributePane propPane = new AttributePane(instance);
		propPane.setEditable(isEditable);
		if (needCheckoutBtn)
		    propPane.showCheckoutButton();
		dialog.getContentPane().add(propPane, BorderLayout.CENTER);
		dialog.getContentPane().add(createLocationLabel(instance), BorderLayout.SOUTH);
		dialog.setSize(600, 800);
		GKApplicationUtilities.center(dialog);
		dialog.setModal(true);
		dialog.setVisible(true);			    
	}
	
	public void updateUI() {
		Collection allFrames = frames.values();
		JFrame frame = null;
		for (Iterator it = allFrames.iterator(); it.hasNext();) {
			frame = (JFrame) it.next();
			SwingUtilities.updateComponentTreeUI(frame);
		}
		if (browser != null)
			SwingUtilities.updateComponentTreeUI(browser);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		propertySupport.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l) {
		propertySupport.removePropertyChangeListener(l);
	}
	
	public Map getFrames() {
		return frames;
	}
	
	private AttributePane extractAttributePane(JFrame frame) {
		AttributePane attributePane = null;
		for (int i = 0; i < frame.getContentPane().getComponentCount(); i++) {
			Component comp = frame.getContentPane().getComponent(i);
			if (comp instanceof AttributePane) {
				attributePane = (AttributePane) comp;
				break;
			}
		}
		return attributePane;		
	}
	
	public void attributeEdit(AttributeEditEvent e) {
		Instance instance = e.getEditingInstance();
		if (!frames.containsKey(instance))
			return;
		JFrame frame = (JFrame) frames.get(instance);
		// Find the AttributePane object
		AttributePane attributePane = extractAttributePane(frame);
		// Refresh the table.
		attributePane.refresh();
		frame.setTitle("Properties: " + instance.toString());
		// This is a fake call to update the menu.
		propertySupport.firePropertyChange("add", frames.size(), frames.size());
		// Update the title
	}
	
	public JMenu generateWindowMenu() {
		final JMenu windowMenu = new JMenu("Window");
		final JMenuItem closeAllItem = new JMenuItem("Close All");
		closeAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FrameManager.getManager().closeAll();
			}
		});
		closeAllItem.setMnemonic('C');
		windowMenu.add(closeAllItem);
		final JMenuItem arrangeAllItem = new JMenuItem("Arrange All");
		arrangeAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FrameManager.getManager().arrangeAll();
			}
		});
		arrangeAllItem.setMnemonic('A');
		windowMenu.add(arrangeAllItem);
		// Disable by default
		arrangeAllItem.setEnabled(false);
		closeAllItem.setEnabled(false);
		final ActionListener listAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JMenuItem item = (JMenuItem) e.getSource();
				String title = item.getText();
				int index = title.indexOf(":");
				int lastIndex = title.lastIndexOf("@") - 1; // There is an extra space
				title = title.substring(index + 2, lastIndex);
				FrameManager.getManager().showFrame(title);
			}
		};
		PropertyChangeListener pL = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				// clear all list first
				int c = windowMenu.getMenuComponentCount();
				for (int i = c - 1; i > 1; i--) 
					windowMenu.remove(i);
				Map frames = FrameManager.getManager().getFrames();
				if (frames.size()> 0) {
					windowMenu.addSeparator();
					java.util.List titleList = new ArrayList(frames.size());
					for (Iterator it = frames.keySet().iterator(); it.hasNext();) {
						GKInstance instance = (GKInstance) it.next();
						if (instance.getDbAdaptor() instanceof MySQLAdaptor)
							titleList.add(instance.toString() + " @database");
						else if (instance.getDbAdaptor() instanceof XMLFileAdaptor)
							titleList.add(instance.toString() + " @localhost");
						else
							titleList.add(instance.toString() + " @unknow");
					}
					Collections.sort(titleList);
					int index = 0;
					String title = null;
					for (Iterator it = titleList.iterator(); it.hasNext();) {
						title = (String) it.next();
						JMenuItem item = new JMenuItem(index + ": " + title);
						item.addActionListener(listAction);
						windowMenu.add(item);
						index ++;
					}
					closeAllItem.setEnabled(true);
					arrangeAllItem.setEnabled(true);
				}
				else {
					closeAllItem.setEnabled(false);
					arrangeAllItem.setEnabled(false);
				}
			}
		};
		addPropertyChangeListener(pL);
		return windowMenu;
	}
	
	
	/**
	 * Waits for property change events from AttributeEditConfig
	 * and passes them on to frames that are interested.
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
	    String propName = evt.getPropertyName();
	    if (propName.equals("AllowComboBoxEditor")) {
	        boolean newValue = ((Boolean)evt.getNewValue()).booleanValue();
	        Collection allFrames = frames.values();
	        JFrame frame;
	        AttributePane attributePane;
	        for (Iterator it = allFrames.iterator(); it.hasNext();) {
	            frame = (JFrame) it.next();
	            attributePane = extractAttributePane(frame);
	            if (attributePane != null)
	                attributePane.setAllowComboBoxEditor(newValue);
	        }
	    }
	    else {
	        boolean newValue = ((Boolean)evt.getNewValue()).booleanValue();
	        Collection allFrames = frames.values();
	        JFrame frame;
	        AttributePane attributePane;
	        for (Iterator it = allFrames.iterator(); it.hasNext();) {
	            frame = (JFrame) it.next();
	            attributePane = extractAttributePane(frame);
	            if (attributePane == null)
	                continue;
	            attributePane.setGroupAttributesByCategory(newValue);
	        }
            if (eventViewFrame != null) {
                // find EventCentricViewPane and set the value
                for (int i = 0; i < eventViewFrame.getContentPane().getComponentCount(); i++) {
                    Component comp = eventViewFrame.getContentPane().getComponent(i);
                    if (comp instanceof EventCentricViewPane) {
                        EventCentricViewPane eventView = (EventCentricViewPane) comp;
                        eventView.getAttributePane().setGroupAttributesByCategory(newValue);
                    }
                }
            }
            if (browser != null) {
                browser.getSchemaView().getAttributePane().setGroupAttributesByCategory(newValue);
            }
	    }
	}
}
