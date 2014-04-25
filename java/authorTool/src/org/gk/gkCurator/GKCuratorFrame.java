/*
 * Created on Nov 11, 2003
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import launcher.Launchable;

import org.gk.database.AttributeEditConfig;
import org.gk.database.AttributeEditEvent;
import org.gk.database.AttributeEditListener;
import org.gk.database.AttributeEditManager;
import org.gk.database.EventCentricViewPane;
import org.gk.database.EventPanePopupManager;
import org.gk.database.FrameManager;
import org.gk.database.SchemaViewPane;
import org.gk.database.SynchronizationManager;
import org.gk.graphEditor.EntityLevelView;
import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.ReactionLevelView;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.osxAdapter.OSXApplication;
import org.gk.persistence.Bookmarks;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.RenderUtility;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchemaClass;
import org.gk.util.AboutGKPane;
import org.gk.util.CloseableTabbedPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.RecentProjectHelper;
import org.w3c.dom.Document;

/**
 * This is the main class for the curator tool.
 * @author wugm
 */
public class GKCuratorFrame extends JFrame implements OSXApplication, Launchable {
	// System info
	public static final String CURATOR_TOOL_NAME = "Reactome Curator Tool";
	public static final String PROJECT_EXT_NAME = ".rtpj";
	public static final String VERSION = "3.1";
	public static final int BUILD_NUMBER = 75;
    static final String QA_MENU_TEXT = "QA Check";
	// For tab title
	private final String PROJECT_TITLE = "Event Hierarchical View";
	private final String INSTANCE_TITLE = "Schema View";
	private final String ENTITY_TITLE = "Entity Level View";
	// A list of actions
	private CuratorActionCollection actionCollection;
	// For holding tabbed panes
	private CloseableTabbedPane tabbedPane;
	// Two views
	private EventCentricViewPane eventView;
	private SchemaViewPane schemaView;
	//private EntityLevelViewPane entityView;
	private org.gk.elv.EntityLevelView entityView;
	// System properties
	private Properties prop;
	private boolean needSaveDefaultPerson = true;
	// GUIs
	JMenu toolMenu;
	private JMenuItem instanceViewItem;
	private JMenuItem eventViewItem;
	private JMenuItem entityViewItem;
	private JMenuItem bookmarkViewItem;
	private JComponent focusedComponent;
	protected boolean isMac = false;
    // To help handling recent project menus
	private RecentProjectHelper recentPrjHelper;
	private boolean entityViewVisible = true;
    // For autosaving
    private AutoSaveThread autoSaveThread;
	
	public GKCuratorFrame() {
		GKApplicationUtilities.enableMacFullScreen(this);
		init();
	}

	private void init() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("mac") > -1) {
			isMac = true;
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("apple.awt.showGrowBox", "true");
		}
		loadProp();
		String lfName = prop.getProperty("lookAndFeel");
		if (lfName == null) {
			lfName = GKApplicationUtilities.getDefaultLF();
			prop.setProperty("lookAndFeel", lfName);
		}
		GKApplicationUtilities.setLookAndFeel(lfName);
		// Display a launch window
		AboutGKPane aboutPane = new AboutGKPane(true, true);
		aboutPane.setStatus("Initializing...");
		aboutPane.setApplicationTitle(CURATOR_TOOL_NAME);
		aboutPane.setVersion(VERSION);
		aboutPane.setBuildNumber(BUILD_NUMBER);
		aboutPane.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.lightGray, Color.black));
		JWindow window = new JWindow();
		window.getContentPane().add(aboutPane, BorderLayout.CENTER);
		window.setSize(378, 320);
		GKApplicationUtilities.center(window);
		window.setVisible(true);
		// Start actual initialization
		actionCollection = new CuratorActionCollection(this);
		initMenuBar();
		JToolBar toolbar = createToolBar();
		getContentPane().add(toolbar, BorderLayout.NORTH);
		tabbedPane = new CloseableTabbedPane();
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				focusedComponent = (JComponent)tabbedPane.getSelectedComponent();
				actionCollection.validateActions();
			}
		});
		tabbedPane.setCloseAction(new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				JComponent comp = (JComponent)tabbedPane.getSelectedComponent();
				if (comp != null) {
					tabbedPane.remove(comp);
					if (comp == eventView) {
						eventViewItem.setText("Show Event View");
						// tabbedPane's stateChanged cannot be fired.
						if (tabbedPane.getTabCount() == 1) {
							focusedComponent = schemaView;
						}
					}
					else if (comp == schemaView) {
						instanceViewItem.setText("Show " + INSTANCE_TITLE);
						if (tabbedPane.getTabCount() == 1)
							focusedComponent = eventView;
					}
					else if (comp == entityView) {
						entityViewItem.setText("Show Entity View");
						if (tabbedPane.getTabCount() == 1)
							focusedComponent = entityView;
					}
					actionCollection.validateActions();
				}
			}
		});
		// add schema view
		schemaView = new SchemaViewPane();
		// To control the hideBookmarkView item
		schemaView.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String propName = e.getPropertyName();
				if (propName.equals("hideBookmarkView")) {
                     boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
                     if (newValue)
                         bookmarkViewItem.setText("Show Bookmark View");
                     prop.setProperty("hideBookmarkView", e.getNewValue() + "");
                 }
			}
		});
        schemaView.setEditable(true);
        // View settings
        String groupInstances = prop.getProperty("groupChangedInstances", "false");
        if (groupInstances.equals("true"))
            schemaView.getInstancePane().setIsInstancesShouldBeGrouped(true);
        String isSpeciesDisplayed = prop.getProperty("isSpeciesDisplayed", "false");
        if (isSpeciesDisplayed.equals("true"))
            schemaView.getInstancePane().setSpeciesDisplayed(true);
        String isSpeciesAfterName = prop.getProperty("isSpeciesAfterName", "false");
        if (isSpeciesAfterName.equals("true"))
            schemaView.getInstancePane().setSpeciesAfterName(true);
        tabbedPane.addTab(INSTANCE_TITLE, schemaView);
		// add event view
		eventView = new EventCentricViewPane();
		eventView.setEditable(true);
		eventView.getEventPane().setPopupType(EventPanePopupManager.LOCAL_REPOSITORY_TYPE);
		tabbedPane.addTab(PROJECT_TITLE, eventView);
		// add entity view
		entityView = new org.gk.elv.EntityLevelView();
		entityView.setEditable(true);
		entityView.setSystemProperties(prop);
		// Default make it use as a drawing tool only
		String isUsedAsDrawingTool = prop.getProperty("useELVasDrawingTool", "true");
		if (isUsedAsDrawingTool.equals("false"))
		    entityView.setUseAsDrawingTool(false);
		else
		    entityView.setUseAsDrawingTool(true);
		//entityView.setEditable(true);
		if (entityViewVisible)
			tabbedPane.addTab(ENTITY_TITLE, entityView);

		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		initMainFrame();
		FrameManager.getManager().setIconImage(getIconImage());
		// Install listeners
		installListeners();
		loadMetaProperties();
		init1(window); // Continue initialization
	}

	/**
	 * A refactored method. Continue the initialization after the property "defaultProj"
	 * is specified.
	 */
	void init1(JWindow window) {
		String selectedSpecies = prop.getProperty("projectSpecies");
		if (selectedSpecies != null)
			eventView.setSelectedSpecies(selectedSpecies);
		// Set the database connecting info
		PersistenceManager.getManager().setDBConnectInfo(prop);
		initFileAdaptor();
		// Have to call after file adaptor is set up
		// since XMLFileAdaptor should be cached in QA actions.
		initQAActions();
		// Default Person
		String defaultPerson = prop.getProperty("defaultPerson");
		if (defaultPerson != null)
			SynchronizationManager.getManager().setDefaultPerson(new Long(defaultPerson));
	    // This is a harsh way to provide a parent component that can be used in
		// SynchronizationManager object. This parent component might not be appropriate
		// For example, if the parent dialog is actual a synchronization dialog.
		SynchronizationManager.getManager().setParentComponent(this);
		String lastProject = prop.getProperty("lastProject");
	    boolean isLastProjectOpened = false;
	    if (lastProject != null) {
	        // Check if the last project file is still there
	        File file = new File(lastProject);
	        if (file.exists()) {
	            isLastProjectOpened = open(lastProject);
	        }
	    }
	    // No new project is opened, create a new project
        window.setVisible(false);
        window.dispose();
        if (!isLastProjectOpened) {
        	createNewProject();
        }
		String selectedView = prop.getProperty("selectedView");
		if (selectedView != null) {
			for (int i = 0; i < tabbedPane.getTabCount(); i++) {
				String tabTitle = tabbedPane.getTitleAt(i);
				if (tabTitle.equals(selectedView)) {
					tabbedPane.setSelectedIndex(i);
					break;
				}
			}
		}
		// Initialize the actions' status
		actionCollection.initializeActionStatus();
		setVisible(true);
		loadBookmarks();
        // For autosaving
        autoSaveThread = new AutoSaveThread();
        autoSaveThread.setPriority(autoSaveThread.getPriority() - 2);
        autoSaveChanged();
	}
	
	private void initFileAdaptor() {
		try {
			XMLFileAdaptor adaptor = new XMLFileAdaptor();
	        adaptor.addInstanceListener(new PropertyChangeListener() {
	            public void propertyChange(PropertyChangeEvent e) {
	                if (e.getPropertyName().equals("addNewInstance")) {
	                    java.util.List newValues = (java.util.List)e.getNewValue();
	                    boolean needRebuildTree = false;
	                    boolean needRefresh = false;
	                    GKSchemaClass selectedCls = schemaView.getSchemaPane().getSelectedClass();
	                    for (Iterator it = newValues.iterator(); it.hasNext();) {
	                        GKInstance instance = (GKInstance)it.next();
	                        schemaView.addInstance(instance);
	                        if (instance.getSchemClass().isa("Event"))
	                            needRebuildTree = true;
	                        if (!needRefresh && instance.getSchemClass().isa(selectedCls))
	                            needRefresh = true;
	                    }
	                    if (needRefresh)
	                        schemaView.getInstancePane().refresh();
	                    entityView.addInstances(newValues);
	                    if (needRebuildTree) {
	                        eventView.rebuildTree();
	                    }
	                    actionCollection.enableSaveAction(true);
	                }
	                else if (e.getPropertyName().equals("deleteInstance")) {
	                    GKInstance instance = (GKInstance)e.getNewValue();
	                    schemaView.deleteInstance(instance);
	                    eventView.deleteInstance(instance);
//	                    entityView.deleteInstance(instance);
	                    actionCollection.enableSaveAction(true);
	                    FrameManager.getManager().close(instance);          
	                    // Make sure the bookmark view correct
	                    schemaView.getBookmarkView().deleteBookmark(instance);  
	                    entityView.deleteInstance(instance);
	                }
	                else if (e.getPropertyName().equals("switchType")) {
	                    GKInstance instance = (GKInstance) e.getNewValue();
	                    GKSchemaClass oldCls = (GKSchemaClass) e.getOldValue();
	                    schemaView.switchedType(oldCls, instance);
	                    eventView.switchedType(oldCls, instance);
	                    entityView.switchedType(oldCls, instance);
	                    entityView.switchedType(oldCls, instance);
	                    actionCollection.enableSaveAction(true);
	                }
	                else if (e.getPropertyName().equals("markAsDirty")) {
	                    actionCollection.enableSaveAction(true);
	                    if (e.getNewValue() instanceof GKInstance) {
	                        GKInstance dirtyInstance = (GKInstance) e.getNewValue();
	                        schemaView.markAsDirty(dirtyInstance);
                            eventView.markAsDirty(dirtyInstance);
                            entityView.markAsDirty(dirtyInstance);
	                    }
	                }
	                else if (e.getPropertyName().equals("removeDirty")) {
	                    actionCollection.enableSaveAction(true);
	                    if (e.getNewValue() instanceof GKInstance) {
	                        GKInstance clearedInstance = (GKInstance) e.getNewValue();
	                        schemaView.removeDirtyFlag(clearedInstance);
	                        entityView.removeDirtyFlag(clearedInstance);
	                    }
	                }
	                else if (e.getPropertyName().equals("clearDeleteRecord")) {
	                    if (e.getNewValue() instanceof Collection)
	                        schemaView.clearDeleteRecord((Collection)e.getNewValue());
	                }
	                else if (e.getPropertyName().equals("fileIsDirty"))
	                    actionCollection.enableSaveAction(true);
	                else if (e.getPropertyName().equals("save"))
	                    actionCollection.enableSaveAction(false);
	            }
	        });
			PersistenceManager.getManager().setActiveFileAdaptor(adaptor);
			entityView.initFileAdptor(adaptor);
		}
		catch(Exception e) {
			System.err.println("GKCuratorFrame.initFileAdaptor(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					                      "Cannot initialize a file adaptor for the local repository. The application will exit.",
					                      "Initialization Error",
					                      JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	private void installListeners() {
		// Let the WindowListener handle this close action.
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		// To save properties
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		JTree tree = schemaView.getSchemaPane().getClassTree();
		tree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doClassTreePopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doClassTreePopup(e);
			}
		});
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				actionCollection.validateActions();
			}
		});
		// Add a Popup for InstanceListPane
		schemaView.getInstancePane().getInstanceList().addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doInstanceListPopup(e);				
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doInstanceListPopup(e);
			}
		});
		schemaView.getInstancePane().getInstanceList().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				actionCollection.validateActions();
			}
		});
		eventView.getEventPane().addSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				actionCollection.validateActions();
			}
		});
//		entityView.getEventPane().addSelectionListener(new TreeSelectionListener() {
//			public void valueChanged(TreeSelectionEvent e) {
//				actionCollection.validateActions();
//			}
//		});
		// Make sure the views on the tabs are consistent to the data
		AttributeEditListener editListener = new AttributeEditListener() {
			public void attributeEdit(AttributeEditEvent e) {
				GKInstance instance = e.getEditingInstance();
				// Check if the eventTree needs to be updated
				String attName = e.getAttributeName();
				if (attName == null || 
				    attName.equals("hasInstance") || 
                    attName.equals(ReactomeJavaConstants.hasEvent) ||
				    attName.equals("hasComponent") ||
				    attName.equals("hasMember") ||
				    attName.equals("hasSpecialisedForm") ||
                    attName.equals("_displayName") ||
                    attName.equals(ReactomeJavaConstants._doRelease) ||
				    attName.equals(ReactomeJavaConstants._doNotRelease)) {
					eventView.updateInstance(e);
				}
				// To synchronize the view
				entityView.updateInstance(e);
				Component attributePane = e.getEditingComponent();
				// Need to update
				if (schemaView.getAttributePane().getInstance() == instance
					&& attributePane != schemaView.getAttributePane()) { // No need to update itself
					schemaView.getAttributePane().refresh();
				}
				if (eventView.getAttributePane().getInstance() == instance
					&& attributePane != eventView.getAttributePane()) { // No need to update itself
					eventView.getAttributePane().refresh();
				}
//				if (entityView.getAttributePane().getInstance() == instance
//						&& attributePane != entityView.getAttributePane()) { // No need to update itself
//					entityView.getAttributePane().refresh();
//				}
			}
		};
		AttributeEditManager.getManager().addAttributeEditListener(editListener);
		AttributeEditManager.getManager().addAttributeEditListener(FrameManager.getManager());
		// Add actions for converting to the authoring tool.
		eventView.getEventPane().addAdditionalPopupAction(actionCollection.getOpenInAuthoringToolAction());
		eventView.getEventPane().addAdditionalPopupAction(actionCollection.getExportToAuthoringToolAction());
		// Handle some property setttings
        AttributeEditConfig.getConfig().addPropertyChangeListener(new PropertyChangeListener() {
           public void propertyChange(PropertyChangeEvent e) { 
               String propName = e.getPropertyName();
               if (propName.equals("AllowComboBoxEditor")) {
                   boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
                   schemaView.getAttributePane().setAllowComboBoxEditor(newValue);
                   eventView.getAttributePane().setAllowComboBoxEditor(newValue);
               }
               else if (propName.equals("GroupAttributesByCategories")) {
                   boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
                   schemaView.getAttributePane().setGroupAttributesByCategory(newValue);
                   eventView.getAttributePane().setGroupAttributesByCategory(newValue);
               }
           }
        });
        // Link to changes in the entity level view
        if (entityView != null) {
            entityView.addGraphEditorActionListener(new GraphEditorActionListener() {
                public void graphEditorAction(GraphEditorActionEvent e) {
                    if (e.isSavableEvent()) {
                        actionCollection.enableSaveAction(true);
                        PersistenceManager.getManager().getActiveFileAdaptor().markAsDirty();
                    }
                }
            });
        }
    }

	private void doClassTreePopup(MouseEvent e) {
		JTree tree = (JTree)e.getSource();
		if (tree.getSelectionCount() == 1) {
			JPopupMenu popup = new JPopupMenu();
			popup.add(actionCollection.getViewSchemaAction());
			popup.add(actionCollection.getCreateInstanceAction());
			popup.addSeparator();
			popup.add(actionCollection.getSynchronizeDBAction());
			popup.show(tree, e.getX(), e.getY());
		}
	}
	
	private void doInstanceListPopup(MouseEvent e) {
		final JList list = (JList) e.getSource();
		if (list.getSelectedValue() != null) {
		    JPopupMenu popup = new JPopupMenu();
		    popup.add(actionCollection.getViewInstanceAction());
		    popup.add(actionCollection.getBatchEditAction());
		    popup.add(actionCollection.getViewReferersAction());
		    final GKInstance instance = (GKInstance) list.getSelectedValue();
		    // Do display it in the event pane
		    if (instance.getSchemClass().isa("Event")) {
		        JMenuItem item = new JMenuItem("View in Tree",
		                                       GKApplicationUtilities.createImageIcon(getClass(), "ViewEventInTree.gif"));
		        item.addActionListener(new ActionListener() {
		            public void actionPerformed(ActionEvent e) {
		                // Check event view is displayed
		                if (tabbedPane.getTabCount() == 1) {
		                    tabbedPane.addTab(PROJECT_TITLE, eventView);
		                    if (entityViewVisible)
		                        tabbedPane.addTab(ENTITY_TITLE, entityView);
		                }
		                tabbedPane.setSelectedComponent(eventView);
		                Collection c = Arrays.asList(list.getSelectedValues());
		                eventView.getEventPane().select(c);
		                //						entityView.getEventPane().select(c);
		            }
		        });
		        popup.add(item);
		    }
		    // Display all reactions in a pathway
		    if (instance.getSchemClass().isa("Pathway") &&
		            list.getSelectedValues().length == 1) {
		        JMenuItem item = new JMenuItem("Show in Reaction-Level View",
		                                       GKApplicationUtilities.createImageIcon(getClass(), "ReactionLevelView.gif"));
		        item.addActionListener(new ActionListener() {
		            public void actionPerformed(ActionEvent e) {
		                if (instance.isShell()) {
		                    JOptionPane.showMessageDialog(GKCuratorFrame.this, "The selected pathway is a shell instance, " +
		                            "cannot be displayed in a reaction-level view.", "Error", JOptionPane.ERROR_MESSAGE);
		                    return;
		                }
		                final ReactionLevelView view = new ReactionLevelView();
		                try {
		                    RenderablePathway pathway = (RenderablePathway) RenderUtility.convertToNode(instance, false);
		                    view.setPathway(pathway, false);
		                    final JFrame frame = new JFrame("Reaction-Level View: " + pathway.getDisplayName());
		                    frame.getContentPane().add(new JScrollPane(view), BorderLayout.CENTER);
		                    frame.setSize(800, 600);
		                    frame.setIconImage(getIconImage());
		                    GKApplicationUtilities.center(frame);
		                    frame.addWindowListener(new WindowAdapter() {
		                        public void windowOpened(WindowEvent e) {
		                            // Don't want to wait and need to valide bounds ASAP
		                            view.paintImmediately(view.getBounds());
		                            view.layoutRenderable();
		                        }
		                    });
		                    frame.setVisible(true);
		                }
		                catch(Exception e1) {
		                    System.err.println("GKCuratorFrame.doInstanceListPopup(): " + e1);
		                    e1.printStackTrace();
		                }
		            }
		        });
		        popup.add(item);
		        if (!GKApplicationUtilities.isDeployed) {
		            item = new JMenuItem("Show in Entity-Level View");
		            item.addActionListener(new ActionListener() {
		                public void actionPerformed(ActionEvent e) {
		                    if (instance.isShell()) {
		                        JOptionPane.showMessageDialog(GKCuratorFrame.this, "The selected pathway is a shell instance, " +
		                                "cannot be displayed in an entity-level view.", "Error", JOptionPane.ERROR_MESSAGE);
		                        return;
		                    }
		                    final EntityLevelView view = new EntityLevelView();
		                    try {
		                        RenderablePathway pathway = (RenderablePathway) RenderUtility.convertToNode(instance, false);
		                        view.setPathway(pathway, false);
		                        final JFrame frame = new JFrame("Entity-Level View: " + pathway.getDisplayName());
		                        frame.getContentPane().add(new JScrollPane(view), BorderLayout.CENTER);
		                        frame.setSize(800, 600);
		                        frame.setIconImage(getIconImage());
		                        GKApplicationUtilities.center(frame);
		                        frame.addWindowListener(new WindowAdapter() {
		                            public void windowOpened(WindowEvent e) {
		                                // Don't want to wait and need to valide bounds ASAP
		                                view.paintImmediately(view.getBounds());
		                                view.layoutRenderable();
		                            }
		                        });
		                        frame.setVisible(true);
		                    }
		                    catch(Exception e1) {
		                        System.err.println("GKCuratorFrame.doInstanceListPopup(): " + e1);
		                        e1.printStackTrace();
		                    }
		                }
		            });
		            popup.add(item);
		        }
		    }
		    popup.add(actionCollection.getAddBookmarkAction());
		    popup.addSeparator();
		    popup.add(actionCollection.getCloneInstanceAction());
		    if (list.getSelectedValues().length == 1 && !instance.isShell()) { // Block these two actions for a shell instance
		        if (instance.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
		            popup.add(actionCollection.getAddHasModifiedResidueAction());
		        else if (instance.getSchemClass().isa(ReactomeJavaConstants.DefinedSet))
		            popup.add(actionCollection.getDeepCloneDefinedSet());
		    }
		    popup.add(actionCollection.getSwitchTypeAction());
		    // Action to create EWAS from RefPepSeq
		    if (instance.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
		            instance.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct)) {
		        popup.add(actionCollection.getCreateEwasFromRefPepSeqAction());
		    }
		    popup.add(actionCollection.getDeleteInstanceAction());
		    popup.addSeparator();
		    popup.add(actionCollection.getCompareInstancesAction());
		    popup.add(actionCollection.getMergeInstanceAction());
		    popup.addSeparator();
		    popup.add(actionCollection.getMatchInstancesAction());
		    popup.add(actionCollection.getCompareDBInstanceAction());
		    popup.add(actionCollection.getUpdateFromDBAction());
		    popup.add(actionCollection.getCheckInAction());
		    popup.show(list, e.getX(), e.getY());
		}
	}

	private void loadProp() {
	    prop = new Properties();
	    //File file = new File("resources" + File.separator + "curator.prop");
	    File file = null;
	    try {
	        file = GKApplicationUtilities.getPropertyFile("curator.prop");
	    }
	    catch(Exception e) {
	        System.err.println("GKCuratorFrame.loadProp(): " + e);
	        e.printStackTrace();
	    }
	    if (file != null && file.exists()) {
	        try {
	            FileInputStream fis = new FileInputStream(file);
	            prop.load(fis);
	            fis.close();
	        }
	        catch (IOException e) {
	            System.err.println("GKCuratorFrame.loadProp(): " + e);
	            e.printStackTrace();
	        }
	    }
	    // File may be null. However, we should still keep a non-null system-wide Properties object.
	    GKApplicationUtilities.setApplicationProperties(prop);
	    // Set for transaction
	    SynchronizationManager.getManager().setProperties(prop);
	    // Check auto setting list, and other properties
	    AttributeEditConfig.getConfig().loadProperties(prop);
	}
	
	private void loadMetaProperties() {
	    try {
	        InputStream metaConfig = GKApplicationUtilities.getConfig("curator.xml");
	        if (metaConfig == null)
	            return;
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = dbf.newDocumentBuilder();
	        Document document = builder.parse(metaConfig);
	        AttributeEditConfig.getConfig().loadConfig(document);
	    }
	    catch(Exception e) {
	        System.err.println("GKCuratorFrame.loadMetaProperties(): " + e);
	        e.printStackTrace();
	    }
	}
	
	private void initQAActions() {
	    QAMenuHelper qaHelper = new QAMenuHelper();
	    JMenu qaMenu = qaHelper.createQAMenu(schemaView, 
	                                         PersistenceManager.getManager().getActiveFileAdaptor());
	    toolMenu.add(qaMenu, 0);
	    // Need a separator
	    toolMenu.add(new JPopupMenu.Separator(), 1);
	}
	
	/**
	 * A helper method to check if save is needed if the file is dirty.
	 * @return true for saving is done or not necessary while false for cancelling
	 * saving action.
	 */
	private boolean checkSave() {
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		if (adaptor != null) {
			// Check if there are any changes
			if (adaptor.isDirty()) {
				int reply = JOptionPane.showConfirmDialog(this,
						                                  "There are changes that are not saved.\n" + "Do you want to save changes?",
						                                  "Save Changes?",
						                                  JOptionPane.YES_NO_CANCEL_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
					actionCollection.save();
				}
				else if (reply == JOptionPane.CANCEL_OPTION)
					return false;
			}
		}
		return true;
	}

	public boolean close() {
	    if (!checkSave())
	        return false;
	    saveProperties();
	    dispose();
	    return true;
	}

	protected void saveProperties() {
	    XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    if (adaptor != null) {
	        // Have to save the last project
	        if (adaptor.getSourceName() == null)
	            prop.remove("lastProject");
	        else
	            prop.setProperty("lastProject", adaptor.getSourceName());
	    }
	    
	    // Save any outstanding changes.
	    AttributeEditConfig.getConfig().commit(prop);
	    
	    // Save for the recent projects
	    recentPrjHelper.storeProjects(prop);
	    entityView.storeSystemProperties(prop);
	    prop.setProperty("projectSpecies", eventView.getSelectedSpecies());
	    int index = tabbedPane.getSelectedIndex();
	    String tabTitle = tabbedPane.getTitleAt(index);
	    prop.setProperty("selectedView", tabTitle);
	    // Remove the dbPwd
	    prop.remove("dbPwd");
	    // Remove these information
//	    prop.remove("wsUser");
//	    prop.remove("wsKey");
	    // Save the defaultPerson for default InstanceEdit
	    if (needSaveDefaultPerson) {
	        Long personId = SynchronizationManager.getManager().getDefaultPerson();
	        if (personId != null) {
	            prop.setProperty("defaultPerson", personId + "");
	        }
	    }
	    else {
	        prop.remove("defaultPerson");
	        // make it default
	        needSaveDefaultPerson = true;
	    }
	    // Save the window bounds
	    String boundsStr = GKApplicationUtilities.generateWindowBoundsString(this);
	    prop.setProperty("windowBounds", boundsStr);
	    // Save the divider locations for the JSplitPanes in the schemaview
	    int[] locations = schemaView.getJSPDividerLocations();
	    for (int i = 0; i < locations.length; i++) {
	        prop.setProperty("schemaViewJSP" + i, locations[i] + "");
	    }
	    // Save if all changed instances should be grouped together
	    boolean isGrouped = schemaView.getInstancePane().isInstancesShouldBeGrouped();
	    prop.setProperty("groupChangedInstances", isGrouped + "");
	    boolean isSpeciesDisplayed = schemaView.getInstancePane().isSpeciesDisplayed();
	    prop.setProperty("isSpeciesDisplayed", isSpeciesDisplayed + "");
	    boolean isSpeciesAfterName = schemaView.getInstancePane().isSpeciesAfterName();
	    prop.setProperty("isSpeciesAfterName", isSpeciesAfterName + "");
	    boolean isUsedAsDrawingTool = entityView.isUsedAsDrawingTool();
	    prop.setProperty("useELVasDrawingTool", isUsedAsDrawingTool + "");
	    // Save the properties
	    //File file = new File("resources" + File.separator + "curator.prop");
	    try {
	        File file = GKApplicationUtilities.getPropertyFile("curator.prop");
	        FileOutputStream fos = new FileOutputStream(file);
	        prop.store(fos, CURATOR_TOOL_NAME);
	        fos.close();
	    }
	    catch(IOException e) {
	        System.err.println("GKCuratorFrame.saveProperties(): " + e);
	        e.printStackTrace();
	    }
	}
	
	private void loadBookmarks() {
	    XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    if (adaptor == null)
	        return;
		Bookmarks bookmarks = adaptor.getBookmarks();
		if (bookmarks != null)
			schemaView.getBookmarkView().setBookmarks(bookmarks);
	}
	
	public void setSaveDefaultPerson(boolean needSave) {
		needSaveDefaultPerson = needSave;
	}

	public JComponent getFocusedComponent() {
		return this.focusedComponent;
	}
	
	protected void prepareForNewProject(XMLFileAdaptor fileAdaptor) {
		eventView.setUpLocalView();
		if (entityViewVisible)
			entityView.setUpLocalView(fileAdaptor);
		schemaView.setPersistenceAdaptor(fileAdaptor);
		schemaView.getBookmarkView().refresh();
		SynchronizationManager.getManager().refresh();
		// All opened instances should be closed.
		FrameManager.getManager().closeAllForLocal();
		refreshTitle();
	}

	public boolean open(String fileName) {
	    if(!checkSave()) // Need to save changes
	        return false;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        try {
			fileAdaptor.setSource(fileName);
			prepareForNewProject(fileAdaptor);
			// Last directory is logged when a file is selected regardless of 
			// this file cannot be opened.
//			// Keep track the last directory
//			File file = new File(fileName);
//			prop.setProperty("currentDir", file.getParent());
			recentPrjHelper.addRecentProject(fileName);
            SynchronizationManager.getManager().setDefaultPerson(fileAdaptor.getDefaultPersonId());
            // Save current properties to avoid status loss in a crash
            saveProperties();
            createBackUpProject(fileAdaptor);
			return true;
        }
        catch (Exception e) {
        	System.err.println("GKCuratorFrame.open(): " + e);
        	e.printStackTrace();
        	JOptionPane.showMessageDialog(this,
        			                      "Error in Open a Project",
										  "Cannot open the specified project. Please make sure the selected file has the correct format.",
										  JOptionPane.ERROR_MESSAGE);
        	return false;
        }
    }

	// Create a back-up copy in case anything is wrong and a edited project cannot be saved.
    // Make sure these statements are at the end of this block in case a temp file
    // cannot be created so that a file can be opened at least!
	private void createBackUpProject(final XMLFileAdaptor fileAdaptor) {
	    Thread t = new Thread() {
	        public void run() {
	            try {
	                File file = GKApplicationUtilities.createTempFile("AutoSaved.rtpj");
	                fileAdaptor.saveAsTemp(file.getAbsolutePath());
	            }
	            catch(Exception e) {
	                System.err.println("GKCuratorFrame.createBackUpProject(): " + e);
	                e.printStackTrace();
	            }
	        }
	    };
	    t.setPriority(Thread.currentThread().getPriority() - 2);
	    t.start();
	}
    
    public boolean open(File file) {
    	return open(file.getAbsolutePath());
    }
    
    public void autoSaveChanged() {
        String isEnabled = prop.getProperty("autoSave", "true");
        if (isEnabled.equals("false"))
            autoSaveThread.setIsStop(true);
        else {
            autoSaveThread.setIsStop(false);
            String min = prop.getProperty("autoSaveMin", "10");
            autoSaveThread.setSavePeriod(Integer.parseInt(min));
            // Interrupt is not stable enough. Have to wait for finish.
            // So don't use it!
            if (!autoSaveThread.isAlive())
                autoSaveThread.start();
            // In this code, a previous sleep has to be done before the new time
            // setting can take action.
        }
    }
    
    /**
     * Create a new project.
     * @return true if a new project is created successfully.
     */
    public boolean createNewProject() {
        return createNewProject(true);
    }
    
    public boolean createNewProject(boolean needDefaultPerson) {
        if (!checkSave())
            return false;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        fileAdaptor.reset();
        prepareForNewProject(fileAdaptor);
        if (needDefaultPerson)
            preparePersonForNewProject();
        return true;
    }
    
    private void preparePersonForNewProject() {
        PersonChooseDialog personDialog = new PersonChooseDialog(this);   
        Long defaultPersonId = getDefaultPersonId();
        if (defaultPersonId != null)
            personDialog.setUsedPersonId(defaultPersonId);
        if (this.isVisible())
            personDialog.setLocationRelativeTo(this);
        else
            GKApplicationUtilities.center(personDialog);
        personDialog.setModal(true);
        personDialog.setVisible(true);
        Long dbId = personDialog.getPersonId();
        SynchronizationManager.getManager().setDefaultPerson(dbId);
        PersistenceManager.getManager().getActiveFileAdaptor().setDefaultPersonId(dbId);
    }
    
    private Long getDefaultPersonId() {
        Long dbId = SynchronizationManager.getManager().getDefaultPerson();
        if (dbId != null)
            return dbId;
        String defaultPerson = prop.getProperty("defaultPerson");
        if (defaultPerson != null) 
            return new Long(defaultPerson);
        return null;
    }

	public java.util.List getTopLevelEvents() {
		return eventView.getTopLevelEvents();
	}

	public EventCentricViewPane getEventView() {
		return eventView;
	}

	public SchemaViewPane getSchemaView() {
		return this.schemaView;
	}
	
	public org.gk.elv.EntityLevelView getEntityLevelView() {
	    return this.entityView;
	}

	private void initMainFrame() {
		setTitle(CURATOR_TOOL_NAME);
		ImageIcon icon = GKApplicationUtilities.createImageIcon(getClass(), "R-small.png");
		setIconImage(icon.getImage());
		String boundStr = prop.getProperty("windowBounds");
		// For setting the divider locations for the JSplitPanes.
		String dividerStr1 = prop.getProperty("schemaViewJSP0");
		String dividerStr2 = prop.getProperty("schemaViewJSP1");
		String dividerStr3 = prop.getProperty("schemaViewJSP2");
		if (dividerStr1 != null && dividerStr2 != null && dividerStr3 != null) {
			int location1 = Integer.parseInt(dividerStr1);
			int location2 = Integer.parseInt(dividerStr2);
			int location3 = Integer.parseInt(dividerStr3);
			schemaView.setJSPDividerLocations(location1, location2, location3);
		}
		if (boundStr == null) {
			setSize(1000, 800);
			GKApplicationUtilities.center(this);
		}
		else {
		    GKApplicationUtilities.setWindowBoundsFromString(this, boundStr);
		}
        // Check Bookmark view: default is display the bookmark view
        String hideBookmarkView = prop.getProperty("hideBookmarkView");
        if (hideBookmarkView != null && hideBookmarkView.equals("true"))
            schemaView.hideBookmarkView();
        // Set the sorting mode for the attributes
        boolean isGrouped = AttributeEditConfig.getConfig().isGroupAttributesByCategories();
        schemaView.getAttributePane().setGroupAttributesByCategory(isGrouped);
        eventView.getAttributePane().setGroupAttributesByCategory(isGrouped);
        schemaView.getInstancePane().showViewSettings();
	}

	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		JButton newProjectBtn = toolbar.add(actionCollection.getNewProjectAction());
		JButton openProjectBtn = toolbar.add(actionCollection.getOpenAction());
		JButton saveProjectBtn = toolbar.add(actionCollection.getSaveProjectAction());
		JButton saveAsBtn = toolbar.add(actionCollection.getSaveAsAction());
		toolbar.addSeparator();
		JButton searchBtn = toolbar.add(actionCollection.getSearchInstanceAction());
		JButton createInstanceBtn = toolbar.add(actionCollection.getCreateInstanceAction());
		JButton cloneInstanceBtn = toolbar.add(actionCollection.getCloneInstanceAction());
         JButton switchTypeBtn = toolbar.add(actionCollection.getSwitchTypeAction());
		JButton deleteInstanceBtn = toolbar.add(actionCollection.getDeleteInstanceAction());
		JButton compareInstanceBtn = toolbar.add(actionCollection.getCompareInstancesAction());
		JButton mergeInstanceBtn = toolbar.add(actionCollection.getMergeInstanceAction());
		JButton viewInstanceBtn = toolbar.add(actionCollection.getViewInstanceAction());
		JButton viewReferrersBtn = toolbar.add(actionCollection.getViewReferersAction());
		toolbar.addSeparator();
		JButton matchInstanceBtn = toolbar.add(actionCollection.getMatchInstancesAction());
		JButton compareDBInstanceBtn = toolbar.add(actionCollection.getCompareDBInstanceAction());
		JButton updateFromDBBtn = toolbar.add(actionCollection.getUpdateFromDBAction());
		JButton checkInBtn = toolbar.add(actionCollection.getCheckInAction());
		toolbar.addSeparator();
		JButton helpBtn = toolbar.add(actionCollection.getHelpAction());
		JButton reportBugBtn = toolbar.add(actionCollection.getReportBugAction());
		if (!isMac) {
			Insets insets = new Insets(0, 0, 0, 0);
			newProjectBtn.setMargin(insets);
			openProjectBtn.setMargin(insets);
			saveProjectBtn.setMargin(insets);
			saveAsBtn.setMargin(insets);
			searchBtn.setMargin(insets);
			createInstanceBtn.setMargin(insets);
			cloneInstanceBtn.setMargin(insets);
            switchTypeBtn.setMargin(insets);
			deleteInstanceBtn.setMargin(insets);
			compareInstanceBtn.setMargin(insets);
			viewInstanceBtn.setMargin(insets);
			viewReferrersBtn.setMargin(insets);
			matchInstanceBtn.setMargin(insets);
			compareDBInstanceBtn.setMargin(insets);
			mergeInstanceBtn.setMargin(insets);
			updateFromDBBtn.setMargin(insets);
			checkInBtn.setMargin(insets);
			helpBtn.setMargin(insets);
			reportBugBtn.setMargin(insets);
		}
		return toolbar;
	}
	
	private void initMenuBar() {
		if (isMac)
			GKApplicationUtilities.macOSXRegistration(this);
		JMenuBar menuBar = new JMenuBar();
		int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        // Set up the file menu
		JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = fileMenu.add(actionCollection.getNewProjectAction());
        JMenuItem openItem = fileMenu.add(actionCollection.getOpenAction());
		openItem.setAccelerator(KeyStroke.getKeyStroke('O', shortcutMask));
        fileMenu.addSeparator();
        JMenuItem savePrjItem = fileMenu.add(actionCollection.getSaveProjectAction());
		savePrjItem.setAccelerator(KeyStroke.getKeyStroke('S', shortcutMask));
		JMenuItem saveAsItem = fileMenu.add(actionCollection.getSaveAsAction());
		fileMenu.addSeparator();
        JMenuItem projectInfoItem = fileMenu.add(actionCollection.getProjectInfoAction());
        fileMenu.addSeparator();
		JMenu importMenu = new JMenu("Import From...");
		fileMenu.add(importMenu);
		importMenu.add(actionCollection.getImportFromAuthoringToolAction());
        importMenu.add(actionCollection.getImportFromAuthorTool2Action());
		//importMenu.add(actionCollection.getImportFromVer1PrjAction());
		importMenu.add(actionCollection.getImportExternalPathwayAction());
        importMenu.add(actionCollection.getImportFromMODReactomeAction());
        // Export to Protege
		JMenu exportMenu = new JMenu("Export As...");
		fileMenu.add(exportMenu);
		exportMenu.add(actionCollection.getExportAsProtegePinAction());
		exportMenu.add(actionCollection.getExportAsProtegePrgAction());
		JMenuItem anchorForProject = fileMenu.add(actionCollection.getRebuildProjectAction());
		recentPrjHelper = new RecentProjectHelper();
		recentPrjHelper.setTopAnchorItem(anchorForProject);
		recentPrjHelper.setFileMenu(fileMenu);
		loadRecentProjects(fileMenu);
		if (!isMac) {
			fileMenu.addSeparator();
			JMenuItem exitItem = new JMenuItem("Exit");
			exitItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exit();
				}
			});
			fileMenu.add(exitItem);
			recentPrjHelper.setBottomAnchorItem(exitItem);
		}
		menuBar.add(fileMenu);
		// Set up the edit menu
		JMenu editMenu = new JMenu("Edit");
		instanceViewItem = new JMenuItem("Hide " + INSTANCE_TITLE);
		instanceViewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = tabbedPane.indexOfTab(INSTANCE_TITLE);
				if (index >= 0) {
					tabbedPane.removeTabAt(index);
					instanceViewItem.setText("Show " + INSTANCE_TITLE);
				}
				else {
					tabbedPane.addTab(INSTANCE_TITLE, schemaView);
					tabbedPane.setSelectedComponent(schemaView);
					instanceViewItem.setText("Hide " + INSTANCE_TITLE);
				}
			}
		});
		eventViewItem = new JMenuItem("Hide Event View");
		eventViewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = tabbedPane.indexOfTab(PROJECT_TITLE);
				if (index >= 0) {
					tabbedPane.removeTabAt(index);
					eventViewItem.setText("Show Event View");
				}
				else {
					tabbedPane.addTab(PROJECT_TITLE, eventView);
					tabbedPane.setSelectedComponent(eventView);
					eventViewItem.setText("Hide Event View");
				}
			}
		});
		entityViewItem = new JMenuItem("Hide Entity View");
		entityViewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = tabbedPane.indexOfTab(ENTITY_TITLE);
				if (index >= 0) {
					tabbedPane.removeTabAt(index);
					entityViewItem.setText("Show Entity View");
				}
				else {
					tabbedPane.addTab(ENTITY_TITLE, entityView);
					tabbedPane.setSelectedComponent(entityView);
					entityViewItem.setText("Hide Entity View");
				}
			}
		});
		bookmarkViewItem = new JMenuItem("Hide Bookmark View");
		bookmarkViewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = bookmarkViewItem.getText();
				if (text.startsWith("Hide")) {
					schemaView.hideBookmarkView();
					bookmarkViewItem.setText("Show Bookmark View");
				}
				else {
					schemaView.showBookmarkView();
					bookmarkViewItem.setText("Hide Bookmark View");
				}
			}
		});
		editMenu.add(eventViewItem);
		if (entityViewVisible)
			editMenu.add(entityViewItem);
		editMenu.add(instanceViewItem);
		editMenu.add(bookmarkViewItem);
		editMenu.addSeparator();
		editMenu.add(actionCollection.getSearchInstanceAction());
		editMenu.addSeparator();
		JMenuItem newInstanceItem = editMenu.add(actionCollection.getCreateInstanceAction());
		newInstanceItem.setAccelerator(KeyStroke.getKeyStroke('N', shortcutMask));
		JMenuItem cloneItem = editMenu.add(actionCollection.getCloneInstanceAction());
		cloneItem.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.SHIFT_MASK | shortcutMask));
         JMenuItem switchTypeItem = editMenu.add(actionCollection.getSwitchTypeAction());
        JMenuItem deleteItem = editMenu.add(actionCollection.getDeleteInstanceAction());
		if (!isMac)
			deleteItem.setAccelerator(KeyStroke.getKeyStroke("DELETE"));
		editMenu.addSeparator();
		editMenu.add(actionCollection.getCompareInstancesAction());
		editMenu.add(actionCollection.getMergeInstanceAction());
		editMenu.addSeparator();
		JMenuItem viewInstanceItem = editMenu.add(actionCollection.getViewInstanceAction());
		JMenuItem batchEditItem = editMenu.add(actionCollection.getBatchEditAction());
		JMenuItem viewReferersItem = editMenu.add(actionCollection.getViewReferersAction());
		JMenuItem addBookmarkItem = editMenu.add(actionCollection.getAddBookmarkAction());
		menuBar.add(editMenu);
		// Set up the database menu
		JMenu dbMenu = new JMenu("Database");
		JMenu browserMenu = new JMenu("Database Browser");
		dbMenu.add(browserMenu);
		JMenuItem schemaViewItem = browserMenu.add(actionCollection.getDBSchemaViewAction());
		schemaViewItem.setAccelerator(KeyStroke.getKeyStroke("F5"));
		JMenuItem dbEventViewItem = browserMenu.add(actionCollection.getDBEventViewAction());
		dbEventViewItem.setAccelerator(KeyStroke.getKeyStroke("F7"));
		dbMenu.addSeparator();
		JMenuItem matchItem = dbMenu.add(actionCollection.getMatchInstancesAction());
		JMenuItem compareItem = dbMenu.add(actionCollection.getCompareDBInstanceAction());
		JMenuItem updateItem = dbMenu.add(actionCollection.getUpdateFromDBAction());
		dbMenu.addSeparator();
		JMenuItem checkInItem = dbMenu.add(actionCollection.getCheckInAction());
		dbMenu.addSeparator();
		JMenuItem syncItem = dbMenu.add(actionCollection.getSynchronizeDBAction());
		syncItem.setAccelerator(KeyStroke.getKeyStroke("F2"));
		menuBar.add(dbMenu);
		// Set up the tool menu
		toolMenu = new JMenu("Tools");
		//JMenuItem updateMetaItem = toolMenu.add(actionCollection.getUpdateMetaAction());
		//updateMetaItem.setAccelerator(KeyStroke.getKeyStroke('U', shortcutMask));
		JMenuItem deployPDItem = toolMenu.add(actionCollection.getDeployPathwayDiagramAction());
		JMenuItem visualizationItem = toolMenu.add(actionCollection.getVisualizationAction());
		//JMenuItem eventReleaseItem = toolMenu.add(actionCollection.getEventReleaseAction());
		if (!isMac) {
			toolMenu.addSeparator();
			toolMenu.add(actionCollection.getOptionAction());
		}
        // Add actions for GO related request
        toolMenu.addSeparator();
        JMenu requestGOTermMenu = new JMenu("Request GO Term");
        toolMenu.add(requestGOTermMenu);
        requestGOTermMenu.add(actionCollection.getRequestNewGOTermAction());
        requestGOTermMenu.add(actionCollection.getTrackGORequestAction());
        // To launch PSI-MOD go browser
        toolMenu.add(actionCollection.getLaunchPsiModBrowserAction());
        toolMenu.add(actionCollection.getLaunchDiseaseBrowserAction());
		menuBar.add(toolMenu);
		JMenu windowMenu = FrameManager.getManager().generateWindowMenu();
		menuBar.add(windowMenu);
		// Set up the help menu
		JMenu helpMenu = new JMenu("Help");
		JMenuItem helpItem = helpMenu.add(actionCollection.getHelpAction());
		helpItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
		helpMenu.add(actionCollection.getReportBugAction());
		if (!isMac) {
			helpMenu.addSeparator();
			JMenuItem aboutItem = helpMenu.add(actionCollection.getAboutAction());
		}
		menuBar.add(helpMenu);
		if (!isMac) {
			fileMenu.setMnemonic('F');
			editMenu.setMnemonic('E');
			dbMenu.setMnemonic('D');
			toolMenu.setMnemonic('T');
			windowMenu.setMnemonic('W');
			helpMenu.setMnemonic('H');
		}
		setJMenuBar(menuBar);
	}
	
    protected void addRecentProject(String fileName) {
        recentPrjHelper.addRecentProject(fileName);
    }
	
	private void loadRecentProjects(JMenu fileMenu) {
    	// Load the recent opened process files
        int totalProjectNumber = 4;
        recentPrjHelper.setTotalProjectNumber(totalProjectNumber);
        recentPrjHelper.loadProjects(prop);
        ActionListener recentPrjAction = new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		JMenuItem item = (JMenuItem) e.getSource();
        		String text = item.getText();
        		int index = text.indexOf(" ");
        		int i = Integer.parseInt(text.substring(0, index));
        		String projectSourceName = recentPrjHelper.getRecentProject(i - 1);
        		File file = new File(projectSourceName);
        		if (file.exists() && open(projectSourceName)) 
        		    recentPrjHelper.switchToTop(i - 1);
        		else
        		    recentPrjHelper.removeProject(i - 1);
        	}
        };
        recentPrjHelper.setProjectActionListener(recentPrjAction);
        List prjMenus = recentPrjHelper.getProjectMenus();
        if (prjMenus.size() > 0) {
        	fileMenu.addSeparator();
        	for (int i = 0; i < prjMenus.size(); i++) {
        		JMenuItem item = (JMenuItem) prjMenus.get(i);
        	    fileMenu.add(item);
        	}
        }
	}
	
	public void exit() {
		if (close())
			System.exit(0);
	}
	
	// The following three methods are used to hook to MacOS X's application menu.
	public void about() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "exit");
		actionCollection.getAboutAction().actionPerformed(e);
	}
    
	public void preferences() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "options");
		actionCollection.getOptionAction().actionPerformed(e);
	}
    
	public void quit() {
		exit();
	}

	public Properties getSystemProperties() {
		return prop;
	}
	
	public void launch() {
		// Everything is handled by the default constructor.
	}
	
	public String getApplicationName() {
		return CURATOR_TOOL_NAME;
	}
	
    /**
     * Display a hue indicating an update is available.
     */
	public void showUpdateAvailable(Action updateAction) {
	    // Get the JToolBar 
	    JToolBar toolbar = null;
	    int count = getContentPane().getComponentCount();
	    for (int i = 0; i < count; i++) {
	        Component c = getContentPane().getComponent(i);
	        if (c instanceof JToolBar) {
	            toolbar = (JToolBar) c;
	            break;
	        }
	    }
	    if (toolbar == null)
	        throw new IllegalStateException("GKCuratorFrame.showUpdateAvailable(): No toolbar defined.");
	    toolbar.addSeparator();
	    toolbar.add(updateAction);
	    toolbar.invalidate();
	    toolbar.validate();
	}
	
	public void refreshTitle() {
	    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    String sourceName = null;
	    if (fileAdaptor != null)
	        sourceName = fileAdaptor.getSourceName();
	    if (sourceName == null)
	        sourceName = "Untitled";
	    setTitle(sourceName + " - " + CURATOR_TOOL_NAME);
	}
	
	/**
	 * Add an action so that it can be launched in the application.
	 * @param checkUpdateAction
	 */
	public void addCheckUpdateAction(Action checkUpdateAction) {
	    // Add this action under tools menu
	    // Get the tool menu
	    JMenuBar menubar = getJMenuBar();
	    int c = menubar.getMenuCount();
	    JMenu menu = null;
	    JMenu toolMenu = null;
	    for (int i = 0; i < c; i++) {
	        menu = menubar.getMenu(i);
	        if (menu.getText().equals("Tools")) {
	            toolMenu = menu;
	            break;
	        }
	    }
	    if (toolMenu == null)
	        throw new IllegalStateException("GKCuratorFrame.addCheckUpdateAction(): No Tool Menu");
	    //toolMenu.addSeparator();
	    toolMenu.add(checkUpdateAction);
	}
    
    /**
     * Required by Launchable interface to update schema.
     */
    public void addUpdateSchemaAction(Action action) {
        // Add this action under tools menu
        JMenuBar menubar = getJMenuBar();
        int c = menubar.getMenuCount();
        JMenu menu = null;
        JMenu toolMenu = null;
        for (int i = 0; i < c; i++) {
            menu = menubar.getMenu(i);
            if (menu.getText().equals("Tools")) {
                toolMenu = menu;
                break;
            }
        }
        if (toolMenu == null)
            throw new IllegalStateException("GKCuratorFrame.addUpdateScehmaAction(): No Tool Menu");
        toolMenu.addSeparator();
        toolMenu.add(action);
    }
    
    /**
     * Required by Launchable interface to update schema. However,
     * the tool should be restarted to use the new schema. Restarting
     * is not required in this method.
     */
    public boolean updateSchema() {
        // Warning
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "The local project might not be able to open since the new schema\n" +
                                                  "might not be compatible to the current one.\n" +
                                                  "Are you sure you want to continue?",
                                                  "Confirm Updating Schema?",
                                                  JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return false;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        if (fileAdaptor == null) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot find local file adaptor to update the schema.",
                                          "Error in Updating Schema",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            PersistenceManager.getManager().updateLocalSchema(this);
            // If no exception is thrown in the above statement, it should be successful.
            return true;
        }
        catch(IOException e) {
            JOptionPane.showMessageDialog(this,
                                          "Cannot update schema: " + e.getMessage(),
                                          "Error in Updating Schema",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
	
	public Component getUserFrame() {
	    return this;
	}

	public static void main(String[] args) {
		new GKCuratorFrame();
	}
}
