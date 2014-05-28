/*
 * Created on Apr 12, 2004
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ProcessTree;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DiagramGKBReader;
import org.gk.persistence.GKBWriter;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.Project;
import org.gk.render.*;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.GKFileFilter;
import org.gk.util.StringUtils;

/**
 * This class is used to generate a Popup for the GKDataBrowser. The GKDataBrowser
 * can be launched in three difference cases: standalone, in the curator tool
 * and in the author tool.
 * @author wugm
 */
public class GKDBBrowserPopupManager {
	// Type definitions
	public static final int STANDALONE_TYPE = 0;
	public static final int CURATOR_TOOL_TYPE = 1;
	public static final int AUTHOR_TOOL_TYPE = 2; 
	// The current type
	private int type = STANDALONE_TYPE; // Default
	// A singleton
	private static GKDBBrowserPopupManager manager;
	// Actions
	private Action deleteAction;
	private Action checkOutAction;
	private Action viewReferersAction;
	private Action checkOutAsPrjAction;
	private Action checkIntoPrjAction;
	private Action checkOutEntityAction;
	private Action addToReactionAction;
	private Action viewClassDefinitionAction;
	private Action showDiagramAction;
	// The acting browser
	private GKDatabaseBrowser browser;
	// To keep the current dir
	private File currentDir;
	// Project to be check into
	private Project openedProject;
	// To popup property change
	private PropertyChangeSupport propertySupport = null;

	/**
	 * Discourage to directly call this constructor.
	 */
	private GKDBBrowserPopupManager() {
		propertySupport = new PropertyChangeSupport(this);
	}
	
	public static GKDBBrowserPopupManager getManager() {
		if (manager == null)
			manager = new GKDBBrowserPopupManager();
		return manager;
	}
	
	public static JPopupMenu getSchemaPanePopup(GKDatabaseBrowser browser) {
		if (manager == null)
			manager = new GKDBBrowserPopupManager();
		manager.setDatabaseBrowser(browser);
		GKSchemaClass cls = browser.getSchemaView().getSchemaPane().getSelectedClass();
		if (cls == null)
			return null;
		JPopupMenu popup = new JPopupMenu();
		popup.add(manager.getViewClassDefinitionAction());
		return popup;
	}

	public static JPopupMenu getInstanceListPanePopupMenu(int type, GKDatabaseBrowser browser) {
		if (manager == null)
			manager = new GKDBBrowserPopupManager();
		manager.setDatabaseBrowser(browser);
		JPopupMenu popup = null;
		java.util.List selection = browser.getSchemaView().getSelection();
		manager.type = type;
		if (type == STANDALONE_TYPE) {
			popup = new JPopupMenu();
			popup.add(manager.getViewReferersAction());
			//popup.addSeparator();
			//popup.add(manager.getDeleteAction());
		}
		else if (type == CURATOR_TOOL_TYPE) {
			popup = new JPopupMenu();
			popup.add(manager.getCheckOutAction());
			popup.addSeparator();
			popup.add(manager.getViewReferersAction());
			//popup.addSeparator();
			//popup.add(manager.getDeleteAction());
			if (selection.size() == 1) {
			    GKInstance instance = (GKInstance) selection.get(0);
			    if (instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
			        popup.add(manager.getShowDiagramAction());
			    }
			}
		}
		else if (type == AUTHOR_TOOL_TYPE) {
			if (selection == null || selection.size() == 0)
				return null;
			popup = new JPopupMenu();
			SchemaClass cls = browser.getSchemaView().getInstancePane().getSchemaClass();
			// cls should not be null
			if (cls.isa("Event")) {
				popup.add(manager.getCheckOutAsPrjAction());
				popup.add(manager.getCheckIntoPrjAction());
				popup.addSeparator();
				popup.add(manager.getViewReferersAction());
				if (selection.size() == 1)
					manager.getCheckOutAsPrjAction().setEnabled(true);
				else
					manager.getCheckOutAsPrjAction().setEnabled(false);
			}
			else if (cls.isa("PhysicalEntity")) {
				popup.add(manager.getCheckEntityAction());
				popup.add(manager.getAddToReactionAction());
				popup.addSeparator();
				popup.add(manager.getViewReferersAction());
			}
			else {
				popup.add(manager.getViewReferersAction());
			}
		}
		// validate actions
		if (selection.size() == 1)
			manager.getViewReferersAction().setEnabled(true);
		else
			manager.getViewReferersAction().setEnabled(false);
		return popup;
	}
	
	public void setOpenedProject(Project prj) {
		openedProject = prj;
	}
	
	public void setDatabaseBrowser(GKDatabaseBrowser browser) {
		this.browser = browser;
	}
	
	public Action getAddToReactionAction() {
		if (addToReactionAction == null) {
			addToReactionAction = new AbstractAction("Add to Reaction") {
				public void actionPerformed(ActionEvent e) {
					addToReaction();
				}
			};
		}
		return addToReactionAction;
	}
	
	private void addToReaction() {
		if (openedProject == null) {
			JOptionPane.showMessageDialog(browser,
										 "You have to open a project containing reactions first to do \"Add to Reaction\" action.",
										 "Error",
										 JOptionPane.ERROR_MESSAGE);
			return;                             
		}
		java.util.List selected = browser.getSchemaView().getSelection();
		if (selected == null || selected.size() == 0)
			return;
		JFrame parentFrame = (JFrame) SwingUtilities.getRoot(browser);
		AddToReactionDialog dialog = new AddToReactionDialog(openedProject, parentFrame);
		dialog.setSize(400, 400);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setModal(true);
		dialog.setVisible(true);
		if (dialog.isOKClicked) {
			Renderable r = dialog.getSelectedReaction();
			if (r != null) {
				// Convert to Renderable objects.
				try {
					java.util.List importNodes = new ArrayList(selected.size());
					for (Iterator it = selected.iterator(); it.hasNext();) {
						GKInstance entity = (GKInstance) it.next();
						Renderable node = RenderUtility.convertToNode(entity, true);
						importNodes.add(node);
					}
					propertySupport.firePropertyChange("addToReaction", r, importNodes);
				}
				catch (Exception e) {
					System.err.println("GKDBBrowserPopupMenuManager.addToReaction(): " + e);
					e.printStackTrace();
				}
			}
		}
	}
	
	public Action getViewClassDefinitionAction() {
		if (viewClassDefinitionAction == null) {
			viewClassDefinitionAction = new AbstractAction("View Defintion",
			                GKApplicationUtilities.createImageIcon(getClass(), "ViewSchema.gif")) {
				public void actionPerformed(ActionEvent e) {
					viewClassDefinition();
				}
			};
		}
		return viewClassDefinitionAction;
	}
	
	private void viewClassDefinition() {
		GKSchemaClass cls = browser.getSchemaView().getSchemaPane().getSelectedClass();
		if (cls == null)
			return;
		JLabel titleLabel = new JLabel("Slots:");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 4));
		SchemaClassDefinitionTable table = new SchemaClassDefinitionTable(cls);
		final JDialog dialog = new JDialog(browser, "Defintion for " + cls.getName());
		dialog.getContentPane().add(titleLabel, BorderLayout.NORTH);
		dialog.getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton closeBtn = new JButton("Close");
		closeBtn.setMnemonic('C');
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		controlPane.add(closeBtn);
		dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
		dialog.setSize(600, 400);
		dialog.setLocationRelativeTo(browser);
		dialog.setModal(true);
		dialog.setVisible(true);
	}
	
	public Action getDeleteAction() {
		if (deleteAction == null) {
			deleteAction = new AbstractAction("Delete") {
				public void actionPerformed(ActionEvent e) {
					delete();
				}
			};
		}
		return deleteAction;
	}
	
	private void delete() {
		java.util.List selection = browser.getSchemaView().getSelection();
		if (selection == null || selection.size() == 0)
			return;
		String instanceStr = selection.size() == 1 ? "instance" : "instances";
		int reply = JOptionPane.showConfirmDialog(browser,
									  "Are you sure you want to delete the selected " + instanceStr + "? " +
									  "The selected\n" + instanceStr + " will also be deleted in the database.",
									  "Delete Confirmation",
									  JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.NO_OPTION)
			return;       
		for (Iterator it = selection.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance) it.next();
			try {
				MySQLAdaptor dba = browser.getMySQLAdaptor();
				if (dba.supportsTransactions())
					dba.txDeleteInstance(instance);
				else
					dba.deleteInstance(instance);
				browser.getSchemaView().deleteInstance(instance);
			}
			catch(Exception e1) {
				System.err.println("GKDatabaseBrowser.deletePopup(): " + e1);
				e1.printStackTrace();
			}
			// Have to null it
			FrameManager.getManager().close(instance);
		}		
	}
	
	public Action getCheckOutAction() {
		if (checkOutAction == null) {
			checkOutAction = new AbstractAction("Check Out") {
				public void actionPerformed(ActionEvent e) {
					java.util.List selection = browser.getSchemaView().getSelection();
					checkOut(selection, browser);
				}
			};
		}
		return checkOutAction;
	}

	private Action getViewReferersAction() {
		if (viewReferersAction == null) {
			viewReferersAction = new AbstractAction("Display Referrers") {
				public void actionPerformed(ActionEvent e) {
					java.util.List selection = browser.getSchemaView().getSelection();
					if (selection.size() == 1) {
						displayReferers((GKInstance) selection.get(0));
					}
				}
			};
			viewReferersAction.putValue(Action.SHORT_DESCRIPTION, "Display instances referring to the selected one");
		}
		return viewReferersAction;
	}
	
	private Action getShowDiagramAction() {
	    if (showDiagramAction == null) {
	        showDiagramAction = new AbstractAction("Show Diagram") {
	            public void actionPerformed(ActionEvent e) {
	                showPathwayDiagram();
	            }
	        };
	        showDiagramAction.putValue(Action.SHORT_DESCRIPTION,
	        "Show pathway diagram for the selected PathwayDiagram instance");
	    }
	    return showDiagramAction;
	}
	
	private void showPathwayDiagram() {
	    List selection = browser.getSchemaView().getSelection();
	    if (selection.size() != 1)
	        return;
	    GKInstance inst = (GKInstance) selection.get(0);
	    if (!inst.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram))
	        return;
	    try {
	        DiagramGKBReader reader = new DiagramGKBReader();
	        RenderablePathway diagram = reader.openDiagram(inst);
	        ZoomablePathwayEditor pathwayEditor = new ZoomablePathwayEditor();
	        pathwayEditor.getPathwayEditor().addMouseListener(new MouseAdapter() {
	            public void mousePressed(MouseEvent e) {
	                if (e.isPopupTrigger()) {
	                    doPathwayEditorPopup(e);
	                }
	            }
	            public void mouseReleased(MouseEvent e) {
	                if (e.isPopupTrigger())
	                    doPathwayEditorPopup(e);
	            }
	            public void mouseClicked(MouseEvent e) {
	                if (e.getClickCount() == 2)
	                    doPathwayEditorDoubleClick(e);
	            }
	        });
	        pathwayEditor.getPathwayEditor().setRenderable(diagram);
	        // Check if there is any objects have been deleted
            Set<Long> dbIds = new HashSet<Long>();
            List<Renderable> components = diagram.getComponents();
            if (components != null) {
                for (Renderable r : components) {
                    if (r.getReactomeId() == null)
                        continue;
                    dbIds.add(r.getReactomeId());
                }
                // Need to check these instances in the database
                MySQLAdaptor dba = (MySQLAdaptor) inst.getDbAdaptor();
                Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseObject,
                                                               ReactomeJavaConstants.DB_ID,
                                                               "=", 
                                                               dbIds);
                for (Iterator<?> it = c.iterator(); it.hasNext();) {
                    GKInstance tmp = (GKInstance) it.next();
                    dbIds.remove(tmp.getDBID());
                }
                if (dbIds.size() > 0) {
                    // Try to highlight instances that have been deleted
                    for (Renderable r : components) {
                        if (r.getReactomeId() == null)
                            continue;
                        if (dbIds.contains(r.getReactomeId())) {
                            r.setForegroundColor(Color.RED);
                            r.setLineColor(Color.RED);
                        }
                    }
                }
            }          
            pathwayEditor.setTitle("<html><u>Diagram View: " + inst.getDisplayName() + "</u></html>");
	        pathwayEditor.getPathwayEditor().setEditable(false);
	        JFrame frame = new JFrame("Pathway Diagram View");
	        frame.getContentPane().add(pathwayEditor, BorderLayout.CENTER);
	        frame.setLocationRelativeTo(browser);
	        frame.setSize(800, 600);
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.setVisible(true);
	        if (dbIds.size() > 0) {
	            String message = null;
	            if (dbIds.size() == 1)
	                message = "An object displayed in the diagram has been deleted in the database.\n" +
	                          "This object is highlighted in red and with DB_ID " + dbIds.iterator().next() + ".";
	            else
	                message = "Some objects displayed in the diagram have been deleted in the database.\n" +
                              "These objects are highlighted in red and with DB_IDs as following:\n" +
                              StringUtils.join(", ", new ArrayList<Long>(dbIds));
	            JOptionPane.showMessageDialog(frame,
	                                          message,
	                                          "Deleted Objects in Diagram",
	                                          JOptionPane.WARNING_MESSAGE);
	        }
	    }
	    catch (Exception e) {
	        JOptionPane.showMessageDialog(browser, 
	                                      "Cannot show diagram: " + e.getMessage(), 
	                                      "Error in Show Diagram",
	                                      JOptionPane.ERROR_MESSAGE);
	        System.err.println("GKDBBrowserPopupManager.showPathwayDiagram(): " + e);
	        e.printStackTrace();
	    }
	}
	
	private void doPathwayEditorDoubleClick(MouseEvent e) {
	    PathwayEditor editor = (PathwayEditor) e.getSource();
	    List<?> selection = editor.getSelection();
	    if (selection == null || selection.size() != 1)
	        return;
	    Renderable r = (Renderable) selection.get(0);
	    if (r.getReactomeId() == null)
	        return;
	    MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
	    try {
	        GKInstance inst = dba.fetchInstance(r.getReactomeId());
	        if (inst == null)
	            return;
	        FrameManager.getManager().showInstance(inst, false);
	    }
	    catch(Exception e1) {
	        // Don't popup any dialog. Just silently printout exception
	        System.err.println("GKDBBrowserPopupManager.doPathwayEditorDoubleClick(): " + e1);
	        e1.printStackTrace();
	    }
	}
	
	private void doPathwayEditorPopup(MouseEvent e) {
	    final PathwayEditor editor = (PathwayEditor) e.getSource();
	    JPopupMenu popup = new JPopupMenu();
	    // Used to view the selected entity
	    List selection = editor.getSelection();
	    if (selection != null && selection.size() == 1) {
	        final Renderable r = (Renderable) selection.get(0);
	        if (r.getReactomeId() != null) {
	            Action action = new AbstractAction("View Instance") {
	                public void actionPerformed(ActionEvent e) {
	                    try {
	                        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
	                        GKInstance inst = dba.fetchInstance(r.getReactomeId());
	                        if (inst == null) {
	                            JOptionPane.showMessageDialog(editor,
	                                                          "Cannot find instance for " + r.getReactomeId() + ". It may have been deleted.",
	                                                          "Error in Fetching Instance",
	                                                          JOptionPane.ERROR_MESSAGE);
	                            return;
	                        }
	                        FrameManager.getManager().showInstance(inst, false);
	                    }
	                    catch(Exception e1) {
	                        e1.printStackTrace();
	                    }
	                }
	            };
	            popup.add(action);
	            popup.addSeparator();
	        }
	    }
	    Action exportDiagramAction = getExportDiagramAction(editor);
	    popup.add(exportDiagramAction);
	    Action tightNodeAction = new AbstractAction("Tight Nodes") {
	        public void actionPerformed(ActionEvent e) {
	            editor.tightNodes();
	        }
	    };
	    popup.add(tightNodeAction);
	    Action wrapTextIntoNodesAction = new AbstractAction("Wrap Text into Nodes") {
	        public void actionPerformed(ActionEvent e) {
                editor.tightNodes(true);
            }
	    };
	    popup.add(wrapTextIntoNodesAction);
	    popup.show(editor, e.getX(), e.getY());
	}
	
	/**
	 * This following action is copied from action in AuthorToolActionCollection. The code
	 * should not be copied like this. However, since AuthorToolActionCollection is encapsulated
	 * in a class in package org.gk.gkEditor, which is higher than the database package, this method
	 * cannot be used directly here. Probably a necessary refactor should be done in the future.
	 * @param pathwayEditor
	 * @return
	 */
	private Action getExportDiagramAction(final PathwayEditor pathwayEditor) {
	    Action action = new AbstractAction("Export Diagram") {
	        public void actionPerformed(ActionEvent e) {
	            try {
	                pathwayEditor.exportDiagram();
	            }
	            catch(IOException e1) {
	                System.err.println("GKDBBrowserPopupManager.getExportDiagramActio(): " + e1);
	                e1.printStackTrace();
	                JOptionPane.showMessageDialog(pathwayEditor,
	                                              "Pathway diagram cannot be exported: " + e1,
	                                              "Error in Diagram Export",
	                                              JOptionPane.ERROR_MESSAGE);
	            }
	        }
	    };
	    return action;
	}
	
	/**
	 * All Instances in the specified list instances should be from the same
	 * SchemaClass.
	 * @param instances
	 */
	private void checkOut(java.util.List instances,
	                      Component parentComp) {
	    if (instances == null || instances.size() == 0) {
	        JOptionPane.showMessageDialog(parentComp,
	                                      "No instance has been selected. Please select one or more instances first for checking out.",
	                                      "No Instance",
	                                      JOptionPane.INFORMATION_MESSAGE);
	        return;
	    }
	    try {
            SynchronizationManager.getManager().checkOut(instances, browser);
        }
        catch (Exception e) {
            System.err.println("GKDBBrowserPopupManager.checkOut(): " + e);
            e.printStackTrace();
        }
	}

	private void displayReferers(GKInstance instance) {
        final ReverseAttributePane referrersPane = new ReverseAttributePane();
        referrersPane.setGKInstance(instance);
        referrersPane.setParentComponent(browser);
        if (type == CURATOR_TOOL_TYPE) {
            // Add a check out popup menu
            Action checkOutAction = new AbstractAction("Check out") {
                public void actionPerformed(ActionEvent e) {
                    List selection = referrersPane.getSelectedInstances();
                    checkOut(selection, referrersPane);
                }
            };
            // Add a referrer action
            Action displayReferersAction = new AbstractAction("Display Referrers") {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    List selection = referrersPane.getSelectedInstances();
                    GKInstance selected = (GKInstance) selection.get(0);
                    displayReferers(selected);
                }
            };
            referrersPane.addPopupAction(checkOutAction);
            referrersPane.addPopupAction(displayReferersAction);
        }
        referrersPane.showInDialog();
	}
	
	public Action getCheckOutAsPrjAction() {
		if (checkOutAsPrjAction == null) {
			checkOutAsPrjAction = new AbstractAction("Check out as Project") {
				public void actionPerformed(ActionEvent e) {
					checkOutAsProject();
				}
			};
		}
		return checkOutAsPrjAction;
	}
	
	/**
	 * This helper method works only for a single selection.
	 */
	private void checkOutAsProject() {
		java.util.List selection = browser.getSchemaView().getSelection();
		// Make sure all selected are Pathways
		boolean isValid = true;
		for (Iterator it = selection.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance) it.next();
			if (instance.getSchemClass().isa("Reaction")) {
				isValid = false;
				break;
			}
		}
		if (!isValid) {
			JOptionPane.showMessageDialog(browser,
			                              "Only pathways can be checked out as projects. \n" + 
			                              "Please make sure you select pathways only.",
			                              "Error",
			                              JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Choose a directory
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Please choose a file name for saving the project...");
		FileFilter filter = new GKFileFilter();
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		if (currentDir != null)
			fileChooser.setCurrentDirectory(currentDir);
		File prjFile = GKApplicationUtilities.chooseSaveFile(fileChooser, 
															 browser);
		if (prjFile == null)
			return;
		currentDir = prjFile.getParentFile();
		// Write out the projects
		try {
			GKInstance pathway = (GKInstance)selection.get(0);
			RenderablePathway pathwayNode = (RenderablePathway)RenderUtility.convertToNode(pathway, true);
			// Need to assign unique IDs
			java.util.List list = RenderUtility.getAllDescendents(pathwayNode);
			int id = 0;
			pathwayNode.setID(id++);
			for (Iterator it1 = list.iterator(); it1.hasNext();) {
				Renderable r = (Renderable)it1.next();
				r.setID(id++);
			}
			Project project = new Project(pathwayNode);
            GKBWriter writer = new GKBWriter();
			writer.save(project, prjFile.getAbsolutePath());
		}
		catch (Exception e) {
			System.err.println("GKDBBrowserPopupManager.checkOutAsProject(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(browser,
			                              "Cannot check out the event as a project: " + e.getMessage(),
			                              "Error in Checking out",
			                              JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public Action getCheckIntoPrjAction() {
		if (checkIntoPrjAction == null) {
			checkIntoPrjAction = new AbstractAction("Check into Project") {
				public void actionPerformed(ActionEvent e) {
					checkIntoProject();
				}
			};
		}
		return checkIntoPrjAction;
	}
	
	private void checkIntoProject() {
		if (openedProject == null) {
			JOptionPane.showMessageDialog(browser,
			                             "You have to open a project first to do check into project action.",
			                             "Error",
			                             JOptionPane.ERROR_MESSAGE);
			return;                             
		}
		JFrame parentFrame = (JFrame) SwingUtilities.getRoot(browser);
		CheckIntoDialog dialog = new CheckIntoDialog(openedProject, parentFrame);
		dialog.setSize(400, 400);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setModal(true);
		dialog.setVisible(true);
		if (dialog.isOKClicked) {
			Renderable pathway = dialog.getSelectedPathway();
			java.util.List instances = browser.getSchemaView().getSelection();
			// Convert to Renderable objects.
			try {
				java.util.List importNodes = new ArrayList(instances.size());
				if (instances.size() == 1) {
					GKInstance instance = (GKInstance) instances.get(0);
					Renderable  r = RenderUtility.convertToNode(instance, true);
					importNodes.add(r);
				}
				else {
					GKInstance instance = (GKInstance) instances.get(0);
					GKInstance dumb = new GKInstance();
					dumb.setDbAdaptor(instance.getDbAdaptor());
					dumb.setSchemaClass(instance.getDbAdaptor().getSchema().getClassByName("Pathway"));
					java.util.List components = new ArrayList(instances);
					dumb.setAttributeValue(ReactomeJavaConstants.hasEvent, 
                                           components);
					Renderable dumbNode = RenderUtility.convertToNode(dumb, true);
					importNodes.addAll(dumbNode.getComponents());
				}
				// Make sure no duplicate appear and each inserted Renderable objects have
				// unique IDs
				if(!validateImportNodes(importNodes))
					return;
				RenderUtility.registerNodes(importNodes, openedProject.getProcess());
				// Insert the nodes
				for (Iterator it = importNodes.iterator(); it.hasNext();) {
					Renderable r = (Renderable) it.next();
					pathway.addComponent(r);
					r.setContainer(pathway);
				}
				propertySupport.firePropertyChange("checkIntoProject", pathway, importNodes);
			}
			catch (Exception e) {
				System.err.println("GKDBBrowserPopupManager.checkIntoProject(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	private boolean validateImportNodes(java.util.List importNodes) {
		// Get all Renderable objects only onces
		java.util.List all = new ArrayList();
		all.addAll(importNodes);
		for (Iterator it = importNodes.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			java.util.List list = RenderUtility.getAllDescendents(r);
			all.removeAll(list); // To remove duplication
			all.addAll(list);
		}
		// Make use project name is not used by any instances to be checked into
		String prjName = openedProject.getName();
		for (Iterator it = all.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			if (prjName.equals(r.getDisplayName())) {
				StringBuffer msg = new StringBuffer();
				msg.append("You cannot check the selected ");
				if (importNodes.size() == 1)
					msg.append("event ");
				else
					msg.append("events ");
				msg.append("into the opened project, because the project name is used \nby an instance in the ");
				if (importNodes.size() == 1)
					msg.append("selected event or its containing instances.");
				else
					msg.append("selected events or their containing instances.");
				JOptionPane.showMessageDialog(browser,
				                              msg.toString(),
				                              "Error in Check Into",
				                              JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		// Check if there are duplicates appear
		RenderableRegistry registry = RenderableRegistry.getRegistry();
		boolean contains = false;
		for (Iterator it = all.iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			if(registry.contains(r.getDisplayName())) {
				contains = true;
				break;
			}
		}
		if (contains) {
			Object[] choices = new String[]{"Replace Properties", "Keep Properties"};
			String reply = (String) JOptionPane.showInputDialog(browser, "One or more objects already exist in the editing project." +
										" Do you want to keep the \nproperties for these objects or use the properties from the database?", "Object Duplication",
										JOptionPane.QUESTION_MESSAGE, null,
										choices, "Replace Properties");
			if (reply == null)
				return false;
			// To fire propertyChange event
			Set propChangedList = new HashSet();
			if (reply.startsWith("Replace")) {
				// Do replacing
				// Use for update GUIs
				for (Iterator it = all.iterator(); it.hasNext();) {
					Renderable r = (Renderable) it.next();
					String name = r.getDisplayName();
					if (registry.contains(name)) {
						Renderable target = registry.getSingleObject(name);
						RenderUtility.copyProperties(r, target);
						propChangedList.add(target);                     
					}
				}
			}
			// Create shortcuts for those objects that exist in the editing process
			// But the creation should stop at the shortcut.
			java.util.List list1 = new ArrayList(importNodes);
			java.util.List list2 = new ArrayList();
			int c = 0;
			while (list1.size() > 0) {
				for (Iterator it = list1.iterator(); it.hasNext();) {
					Renderable r = (Renderable)it.next();
					String name = r.getDisplayName();
					if (registry.contains(name)) {
						Renderable target = registry.getSingleObject(name);
						String oldTaxon = (String) target.getAttributeValue("taxon");
						if (oldTaxon == null)
							oldTaxon = "";
						Renderable shortcut = (Renderable)target.generateShortcut();
						if (shortcut != null) {
							// Have to make sure importNode is correct
							// For first iterative
							if (c == 0) {
								int index = importNodes.indexOf(r);
								if (index >= 0)
									importNodes.set(index, shortcut);
							}
							RenderUtility.switchRenderInfo(r, shortcut);
							Renderable container = r.getContainer();
							if (container != null) {
								container.removeComponent(r);
								container.addComponent(shortcut);
								shortcut.setContainer(container);
							}
							r.setContainer(null);
							// Check if taxon is changed
							String newTaxon = (String) target.getAttributeValue("taxon");
							if (newTaxon == null)
								newTaxon = "";
							if (!newTaxon.equals(oldTaxon))
								propChangedList.add(target);
						}
					}
					else if (!(r instanceof Shortcut) &&
					         r.getComponents() != null && 
					         r.getComponents().size() > 0)
						list2.addAll(r.getComponents());
				}
				list1.clear();
				list1.addAll(list2);
				list2.clear();
				c++;
			}
			propertySupport.firePropertyChange("replaceProperties", null, propChangedList);      
		}
		return true;
	}
	
	public Action getCheckEntityAction() {
		if (checkOutEntityAction == null) {
			checkOutEntityAction = new AbstractAction("Check out Entity") {
				public void actionPerformed(ActionEvent e) {
					checkOutEntity();
				}
			};
		}
		return checkOutEntityAction;
	}
	
	private void checkOutEntity() {
		if (openedProject == null) {
			JOptionPane.showMessageDialog(browser,
			                              "You have to open a project first to do \"Check out Entity\" action.",
			                              "Error",
			                              JOptionPane.ERROR_MESSAGE);
			return;                              
		}
		java.util.List selection = browser.getSchemaView().getSelection();
		if (selection == null || selection.size() == 0)
			return;
		java.util.List renderables = new ArrayList(selection.size());
		try {
			for (Iterator it = selection.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance)it.next();
				Renderable r = RenderUtility.convertToNode(instance, true);
				if (instance.getSchemClass().isa("SequenceEntity"))
					r.setAttributeValue("type", "protein");
				else if (instance.getSchemClass().isa("Complex"))
					r.setAttributeValue("type", "complex");
				else if (instance.getSchemClass().isa("PhysicalEntity"))
					r.setAttributeValue("type", "smallMolecule");
				renderables.add(r);
			}
		}
		catch (Exception e) {
			System.err.println("GKDBBrowserPopupManager.checkOutEntity(): " + e);
			e.printStackTrace();
		}
		propertySupport.firePropertyChange("checkOutEntity", null, renderables);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		propertySupport.addPropertyChangeListener(l);
	}
	
	public void removePropertychangeListener(PropertyChangeListener l) {
		propertySupport.removePropertyChangeListener(l);
	}
	
	class CheckIntoDialog extends JDialog {
		boolean isOKClicked = false;
		protected ProcessTree processTree;
		protected JList list;
		protected JLabel titleLabel;
		private JScrollPane jsp;
		protected JButton okBtn;
		
		protected Project prj;
		// To control view
		private JRadioButton treeViewBtn;
		private JRadioButton listViewBtn;
		
		CheckIntoDialog(Project prj, JFrame parentFrame) {
			super(parentFrame);
			init();
			if (prj != null) {
				processTree.open(prj.getProcess());
				this.prj = prj;
			}
		}
		
		protected ProcessTree createProcessTree() {
			final ProcessTree processTree = new ProcessTree();
			processTree.setPathwayOnly(true);
			processTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			processTree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					if (processTree.getSelectionCount() == 0)
						okBtn.setEnabled(false);
					else
						okBtn.setEnabled(true);
				}
			});
			return processTree;
		}
		
		private void init() {
			setTitle("Pathway Selection Dialog");
			// Caption
			JPanel contentPane = new JPanel();
			contentPane.setLayout(new BorderLayout());
			contentPane.setBorder(BorderFactory.createRaisedBevelBorder());
			titleLabel = new JLabel("Please choose a pathway from the following process tree:");
			titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
			titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
			contentPane.add(titleLabel, BorderLayout.NORTH);
			// The process tree
			processTree = createProcessTree();
			// For the list view
			list = new JList();
			list.setCellRenderer(new RenderableListCellRenderer());
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					if (list.getSelectedValue() == null)
						okBtn.setEnabled(false);
					else
						okBtn.setEnabled(true);
				}
			});
			
			jsp = new JScrollPane(processTree); // Default with process tree.
			contentPane.add(jsp, BorderLayout.CENTER);
			// The radio btns
			treeViewBtn = new JRadioButton("Tree View");
			treeViewBtn.setSelected(true); // Default
			listViewBtn = new JRadioButton("List View");
			ButtonGroup bg = new ButtonGroup();
			bg.add(treeViewBtn);
			bg.add(listViewBtn);
			ItemListener viewAction = createViewAction();
			treeViewBtn.addItemListener(viewAction);
			listViewBtn.addItemListener(viewAction);
			JPanel viewControlPane = new JPanel();
			viewControlPane.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
			viewControlPane.add(treeViewBtn);
			viewControlPane.add(listViewBtn);
			contentPane.add(viewControlPane, BorderLayout.SOUTH);
			
			getContentPane().add(contentPane, BorderLayout.CENTER);
			// Control pane
			JPanel controlPane = new JPanel();
			controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));
			okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.setMnemonic('C');
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					dispose();
				}
			});
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			controlPane.add(okBtn);
			controlPane.add(cancelBtn);
			okBtn.setEnabled(false);
			getContentPane().add(controlPane, BorderLayout.SOUTH);
		}
		
		private ItemListener createViewAction() {
			ItemListener l = new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						boolean isTree = treeViewBtn.isSelected();
						if (isTree) {
							if (processTree.getRowCount() == 0) {
								processTree.open(prj.getProcess());
							}
							jsp.getViewport().setView(processTree);
							// Have to keep the selection
							if (list.getSelectedValue() == null)
								processTree.setSelected(null);
							else
								processTree.setSelected((Renderable)list.getSelectedValue());
						}
						else {
							if (list.getModel().getSize() == 0) {
								loadIntoList();
							}
							jsp.getViewport().setView(list);
							java.util.List selected = processTree.getSelected();
							if (selected == null || selected.size() == 0) {
								list.setSelectedValue(null, false);
							}
							else {
								Renderable r = (Renderable) selected.get(0);
								if (r instanceof Shortcut)
									r = ((Shortcut)r).getTarget();
								list.setSelectedValue(r, true);
							}
						}
					}
				}
			};
			return l;
		}
		
		protected void loadIntoList() {
			java.util.List pathways = new ArrayList();
			getAllPathways(prj.getProcess(), pathways);
			RenderUtility.sort(pathways);
			DefaultListModel model = new DefaultListModel();
			for (Iterator it = pathways.iterator(); it.hasNext();) {
				model.addElement(it.next());
			}
			list.setModel(model);
		}
		
		private void getAllPathways(Renderable r, java.util.List pathways) {
			if (r instanceof Shortcut)
				return;
			if (r instanceof RenderablePathway) {
				pathways.add(r);
				if (r.getComponents() != null && r.getComponents().size() > 0) {
					for (Iterator it = r.getComponents().iterator(); it.hasNext();) {
						Renderable r1 = (Renderable) it.next();
						getAllPathways(r1, pathways);
					}
				}
			}
		}
		
		public Renderable getSelectedPathway() {
			if (jsp.getViewport().getView() == processTree) {
				java.util.List selected = processTree.getSelected();
				if (selected == null || selected.size() == 0)
					return null;
				return (Renderable) selected.get(0);
			}
			return (Renderable) list.getSelectedValue();
		}
	}
	
	class AddToReactionDialog extends CheckIntoDialog {
		
		AddToReactionDialog(Project prj, JFrame parentFrame) {
			super(prj, parentFrame);
			titleLabel.setText("Please choose a pathway from the following process tree:");
			setTitle("Reaction Selection Dialog");
			
		}
		
		protected ProcessTree createProcessTree() {
			final ProcessTree processTree = new ReactionProcessTree();
			processTree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					if (processTree.getSelectionCount() == 0)
						okBtn.setEnabled(false);
					else {
						DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) processTree.getSelectionPath().getLastPathComponent();
						if (treeNode.getUserObject() instanceof RenderablePathway) {
							okBtn.setEnabled(false);
							JOptionPane.showMessageDialog(AddToReactionDialog.this,
							                              "Please select a reaction.",
							                              "Error",
							                              JOptionPane.ERROR_MESSAGE);
							return;
						}
						else
							okBtn.setEnabled(true);
					}
				}
			});
			return processTree;
		}
		
		public Renderable getSelectedReaction() {
			Renderable r = super.getSelectedPathway();
			if (r instanceof ReactionNode)
				return r;
			JOptionPane.showMessageDialog(this,
			                              "Please choose a reaction.",
			                              "Error",
			                              JOptionPane.ERROR_MESSAGE);
			return null;
		}
		
		protected void loadIntoList() {
			java.util.List pathways = new ArrayList();
			getAllReactions(prj.getProcess(), pathways);
			RenderUtility.sort(pathways);
			DefaultListModel model = new DefaultListModel();
			for (Iterator it = pathways.iterator(); it.hasNext();) {
				model.addElement(it.next());
			}
			list.setModel(model);
		}
		
		private void getAllReactions(Renderable r, java.util.List reactions) {
			if (r instanceof Shortcut)
				return;
			if (r instanceof ReactionNode)
				reactions.add(r);
			if (r instanceof RenderablePathway) {
				if (r.getComponents() != null && r.getComponents().size() > 0) {
					for (Iterator it = r.getComponents().iterator(); it.hasNext();) {
						Renderable r1 = (Renderable) it.next();
						getAllReactions(r1, reactions);
					}
				}
			}
		}
	}
	
	class RenderableListCellRenderer extends DefaultListCellRenderer {
		
		RenderableListCellRenderer() {
			super();
		}
		
		
		public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus) {

			super.getListCellRendererComponent(
							list,
							value,
							index,
							isSelected,
							cellHasFocus);
			if (value instanceof Renderable) {
				setIcon(RenderableFactory.getFactory().getIcon((Renderable)value));
			}
			return this;
		}
	}
	
	/**
	 * This subclass of ProcessTree is used for select a reaction in a
	 * process.
	 * 
	 * @author wgm
	 */
	class ReactionProcessTree extends ProcessTree {
		
		public ReactionProcessTree() {
			super();
		}
		
		protected void insertChildren(DefaultMutableTreeNode parentNode, Renderable parent) {
			if (parent.getComponents() != null) {
				Map added = new HashMap();
				DefaultTreeModel model = (DefaultTreeModel) treeModel;
				int[] index = new int[1];
				for (Iterator it = parent.getComponents().iterator(); it.hasNext();) {
					Renderable r = (Renderable) it.next();
					if (r instanceof FlowLine)
						continue; // Don't add flow lines
					if (added.containsKey(r.getDisplayName()))
						continue;
					// Don't add RenderableReaction to ReactionNode
					if (r instanceof RenderableReaction && parent instanceof ReactionNode)
						continue;
					DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(r);
					insertToModelAlphabetically(childNode, parentNode);
					added.put(r.getDisplayName(), r);
					if (!(r instanceof ReactionNode)) // Don't list RenderableEntities and RenderableComplexes.
						insertChildren(childNode, r);
				}
				if (parentNode.getChildCount() > 0) {
					TreePath path = new TreePath(model.getPathToRoot(parentNode));
					expandPath(path);
				}
			}
		}
		
		protected void open(Renderable container, DefaultMutableTreeNode parentNode) {
			java.util.List components = container.getComponents();
			if (components != null) {
				for (Iterator it = components.iterator(); it.hasNext();) {
					Renderable renderable = (Renderable) it.next();
					// Escape flow line
					if (renderable instanceof FlowLine)
						continue;
					// Escape a RenderableReaction under ReactionNode
					if (renderable instanceof RenderableReaction &&
						container instanceof ReactionNode)
						continue;
					// Escape shortcut temporarily
//					if (renderable instanceof Shortcut) {
//						shortcuts.add(renderable);
//						continue;
//					}
					DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(renderable);
					insertToNodeAlphabetically(treeNode, parentNode);
					if (renderable instanceof RenderableEntity ||
						renderable instanceof RenderableReaction)
						continue;
					if (!(renderable instanceof ReactionNode)) // Don't list RenderableEntities and RenderbaleXomplexes.
						open(renderable, treeNode);		
				}
			}
		}
	
	}
}
