/*
 * Created on Apr 29, 2004
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.qualityCheck.ImbalanceChecker;
import org.gk.qualityCheck.QualityCheck;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;

/**
 * This singleton is used to generate JPopupMenu for HierarchicalEventPane.
 * @author wugm
 */
public class EventPanePopupManager {
	// Popup type
	public static final int LOCAL_REPOSITORY_TYPE = 0;
	public static final int DB_CURATOR_TOOL_TYPE = 1;
	public static final int DB_AUTHOR_TOOL_TYPE = 2;
	// The singleton
	private static EventPanePopupManager manager;
	
	private EventPanePopupManager() {
	}
	
	public static EventPanePopupManager getManager() {
		if (manager == null)
			manager = new EventPanePopupManager();
		return manager;
	}
	
	/**
	 * Get a JPopupMenu for a specified type.
	 * @param type one of CURATOR_TOOL_TYPE, AUTHOR_TOOL_TYPE in GKDBBrowserPopupManager; 

	 * @param eventPane
	 * @return
	 */
	public JPopupMenu getPopupMenu(int type, final HierarchicalEventPane eventPane) {
		if (eventPane.getTree().getSelectionCount() != 1)
			return null;
		final JTree eventTree = eventPane.getTree();
		HierarchicalEventPaneActions actions = eventPane.getActions();
		JPopupMenu popup = new JPopupMenu();
		popup.add(actions.getViewAction());
		popup.add(actions.getViewReferrersAction());
		popup.addSeparator();
		if (type == DB_CURATOR_TOOL_TYPE) {
			JMenuItem checkOutItem = new JMenuItem("Check Out");
			checkOutItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)eventTree.getLastSelectedPathComponent();
					if (treeNode == null)
						return;
					checkOut(treeNode, eventTree);
				}
			});
			popup.add(checkOutItem);
			// This action has been grouped under the top-level menu.
//			JMenuItem imbalanceCheck = new JMenuItem("Check Reaction Imbalance");
//			imbalanceCheck.addActionListener(new ActionListener() {
//			    public void actionPerformed(ActionEvent e) {
//			        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)eventTree.getLastSelectedPathComponent();
//                    if (treeNode == null)
//                        return;
//                    GKInstance instance = (GKInstance)treeNode.getUserObject();
//                    checkImbalance(instance,
//                                   eventPane);
//			    }
//			});
//			popup.add(imbalanceCheck);
			popup.addSeparator();
		}
		else if (type == LOCAL_REPOSITORY_TYPE) {
		    DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
		    GKInstance selectedInstance = (GKInstance) selectedNode.getUserObject();
		    GKSchemaClass schemaCls = (GKSchemaClass) selectedInstance.getSchemClass();
		    if (schemaCls.isValidAttribute(ReactomeJavaConstants.hasComponent) ||
                schemaCls.isValidAttribute(ReactomeJavaConstants.hasEvent))
		        popup.add(actions.getAddComponentAction());
		    if (schemaCls.isValidAttribute("hasInstance"))
		        popup.add(actions.getAddInstanceAction());
		    if (schemaCls.isValidAttribute("hasMember"))
		        popup.add(actions.getAddMemberAction());
		    if (schemaCls.isValidAttribute("hasSpecialisedForm"))
		        popup.add(actions.getAddSpecialisedFormAction());
			popup.add(actions.getDeleteAction());
			popup.addSeparator();
		}
		popup.add(actions.getExpandNodeAction());
		popup.add(actions.getCollapseNodeAction());
		return popup;
	}
	
	private void checkImbalance(GKInstance event,
	                            java.awt.Component parentComponent) {
        QualityCheck checker = new ImbalanceChecker();
        checker.setDatasource(event.getDbAdaptor());
        checker.setParentComponent(parentComponent);
        // We want to get all contained reaction be event if this event is a 
        // pathway instance. Otherwise, just check this event.
        if (event.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
            checker.check(event);
        }
        // Get all contained Reaction by this pathway
        else if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            Set reactions = grepReaction(event);
            checker.check(new ArrayList(reactions));
        }
	}
	
	private Set grepReaction(GKInstance pathway) {
	    Set reactions = new HashSet();
	    String[] attNames = new String[] {
	            ReactomeJavaConstants.hasEvent,
	            ReactomeJavaConstants.hasMember
	    };
	    Set current = new HashSet();
	    current.add(pathway);
	    Set next = new HashSet();
	    try {
	        while (current.size() > 0) {
	            for (Iterator it = current.iterator(); it.hasNext();) {
	                GKInstance instance = (GKInstance) it.next();
	                if (instance.getSchemClass().isa(ReactomeJavaConstants.Reaction)) {
	                    reactions.add(instance);
	                    continue;
	                }
	                // Check values
	                for (int i = 0; i < attNames.length; i++) {
	                    if (instance.getSchemClass().isValidAttribute(attNames[i])) {
	                        List values = instance.getAttributeValuesList(attNames[i]);
	                        if (values != null)
	                            next.addAll(values);
	                    }
	                }
	            }
	            current.clear();
	            current.addAll(next);
	            next.clear();
	        }
	    }
	    catch(Exception e) {
	        System.err.println("EventPanePopupManager.grepReaction(): " + e);
	        e.printStackTrace();
	    }
	    return reactions;
	}

	private void checkOut(final DefaultMutableTreeNode eventNode, JTree eventTree) {
		// Have to save changes first
		final XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		// Specify the stop conditions
		final GKInstance instance = (GKInstance) eventNode.getUserObject();
		final JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, eventTree);
        // Need to re-work these two threads.
        final CheckOutProgressDialog progDialog = new CheckOutProgressDialog(parentFrame);
        Thread t = new Thread() {
            public void run() {
                try {
                    //long time1 = System.currentTimeMillis();
                    yield(); // Make sure progDialog.setVisible is called first.
                    progDialog.setText("Pulling instances out of the database...");
                    Set<GKInstance> events = getAllEvents(eventNode);
                    EventCheckOutHandler handler = new EventCheckOutHandler();
                    Map<SchemaClass, Set<GKInstance>> schemaMap = handler.pullInstances(events);
                    // Load the pathway diagram in another method
//                    if (!progDialog.isCancelClicked) {
//                        progDialog.setText("Pulling PahtwayDiagram out of the database...");
//                        handler.checkOutPathwayDiagram(schemaMap);
//                    }
                    if (!progDialog.isCancelClicked) {
                        // After this step, the user cannot cancel.
                        progDialog.cancelBtn.setEnabled(false);
                        progDialog.setText("Checking the existence of instances...");
                        // Need to generate an information message
                        handler.checkExistence(schemaMap, 
                                               fileAdaptor, 
                                               parentFrame);
                        progDialog.setText("Pushing instances into the local project...");
                        fileAdaptor.store(schemaMap);
                        // Clear isShell flag for GKInstnaces from the database.
                        InstanceUtilities.clearShellFlags(schemaMap);
                        progDialog.setIsDone();
                    }
                    //long time2 = System.currentTimeMillis();
                    //System.out.println("Time for checking out: " + (time2 - time1));
                }
                catch (Exception e) {
                    System.err.println("HierarchicalEventPane.checkOut(): " + e);
                    e.printStackTrace();
                    progDialog.setIsWrong();
                }
            }
        };
        t.start();
        progDialog.setVisible(true);
    }
    
    public Set<GKInstance> getAllEvents(DefaultMutableTreeNode eventNode) {
        // As if March 7, 2008. Only the selected Event and its contained 
        // event will be checked out. Not the whole project will be checked out.
//      // Find the root
//      DefaultMutableTreeNode topLevel = eventNode;
//      DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) topLevel.getParent();
//      while (parentNode.getUserObject() != null) {
//          topLevel = parentNode;
//          parentNode = (DefaultMutableTreeNode) topLevel.getParent();
//      }
        // Get all events under this branch
        Set<GKInstance> events = new HashSet<GKInstance>();
        //getAllEvents(events, topLevel);
        getAllEvents(events, eventNode);
        return events;
    }
    
    
    private void getAllEvents(Set<GKInstance> events, 
                              DefaultMutableTreeNode parentNode) {
        events.add((GKInstance)parentNode.getUserObject());
        int size = parentNode.getChildCount();
        for (int i = 0; i < size; i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            getAllEvents(events, childNode);
        }
    }
	
	class CheckOutProgressDialog extends JDialog {
		private boolean isCancelClicked = false;
		private JLabel label;
		private JProgressBar progressBar;
		private JButton cancelBtn;
		
		CheckOutProgressDialog(JFrame parentFrame) {
			super(parentFrame);
			init();
		}
		
		private void init() {
			JPanel centerPane = new JPanel();
			centerPane.setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(4, 4, 4, 4);
			label = new JLabel("Checking out instances...");
			centerPane.add(label, constraints);
			progressBar = new JProgressBar();
			constraints.gridy = 1;
			centerPane.add(progressBar, constraints);
			progressBar.setIndeterminate(true);
			cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					cancel();
				}
			});
			constraints.gridy = 2;
			centerPane.add(cancelBtn, constraints);
			cancelBtn.setDefaultCapable(true);
			getRootPane().setDefaultButton(cancelBtn);
			getContentPane().add(centerPane, BorderLayout.CENTER);
			
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					cancel();
				}
			});
			setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			setSize(300, 200);
			setLocationRelativeTo(getOwner());
			setTitle("Checking out");
			setModal(true);
		}
		
		public void setIsDone() {
			label.setText("Checking out is done.");
			progressBar.setIndeterminate(false);
			progressBar.setMaximum(100);
			progressBar.setValue(100);
			cancelBtn.setText("OK");
             cancelBtn.setEnabled(true); // In case it is disabled.
		}
		
		public void setIsWrong() {
			label.setText("<html>Something is wrong during checking out.<br>It is aborted.</html>");
			progressBar.setIndeterminate(false);
			progressBar.setVisible(false);
			cancelBtn.setText("OK");
		}
        
        public void setText(String text) {
            label.setText(text);
        }
		
		private void cancel() {
			String text = cancelBtn.getText();
			if (text.equals("OK")) {
				dispose();
				return;
			}
            if (!cancelBtn.isEnabled()) {
                JOptionPane.showMessageDialog(this,
                                              "Sorry! You cannot cancel checking out right now. Please wait after checking out is done.",
                                              "Cancel Checking Out",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
			// Confirm
			int reply = JOptionPane.showConfirmDialog(this,
													  "Are you sure you want to cancel the checking out action?",
													  "Cancel Confirmation",
													  JOptionPane.YES_NO_OPTION);
			if (reply == JOptionPane.YES_OPTION) {
				isCancelClicked = true;
				dispose();
			}
		}
	}

}
