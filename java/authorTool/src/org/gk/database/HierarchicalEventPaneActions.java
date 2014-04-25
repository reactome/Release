/*
 * Created on Dec 4, 2003
 */
package org.gk.database;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;

/**
 * A list of actions for a HierarchicalEventPane.
 * @author wugm
 */
public class HierarchicalEventPaneActions {
	// The owner pane.
	private HierarchicalEventPane eventPane;
	private JTree wrappedTree;
	// A list of actions
	private Action expandNodeAction;
	private Action collapseNodeAction;
	private Action deleteAction;
	private Action addComponentAction;
	private Action addInstanceAction;
	private Action addMemberAction;
	private Action addSpecizlizedFormAction;
	private Action viewAction;
	private Action viewReferrersAction;
	private MouseListener dnrCheckMouseAction;
	
	public HierarchicalEventPaneActions() {
	}
	
	public HierarchicalEventPaneActions(HierarchicalEventPane pane) {
		this.eventPane = pane;
		this.wrappedTree = pane.getTree();
	}
	
	/**
	 * Set the wrapped tree for this object. The wrapped tree is used
	 * to display GKInstance in a hierarchical way.
	 * @param tree
	 */
	public void setWrappedTree(JTree tree) {
	    this.wrappedTree = tree;
	}
	
	public MouseListener getDNRCheckMouseAction() {
	    if (dnrCheckMouseAction == null) {
	        dnrCheckMouseAction = new MouseAdapter() {
	            public void mouseClicked(MouseEvent e) {
	                int x = e.getX();
	                int y = e.getY();
	                TreePath path = wrappedTree.getPathForLocation(x, y);
	                if (path == null)
	                    return ; // Nothing selected
	                // Need to check if the selected event is shell
	                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    GKInstance event = (GKInstance) treeNode.getUserObject();
                    if (event.isShell())
                        return; // A shell instance is not editable
	                Rectangle rect = wrappedTree.getPathBounds(path);
	                if (x < (rect.getX() + 16)) {
	                    try {
	                        DefaultTreeModel model = (DefaultTreeModel) wrappedTree.getModel();
	                        toggleDR(treeNode, model);
	                    }
	                    catch(Exception e1) {
	                        System.err.println("HierarchicalEventPaneActions.getDNRCheckMouseAction(): " + e1);
	                        e1.printStackTrace();
	                    }
	                }
	            }
	        };
	    }
	    return dnrCheckMouseAction;
	}
	
	private void toggleDR(DefaultMutableTreeNode treeNode, 
	                       DefaultTreeModel treeModel) throws Exception {
	    GKInstance event = (GKInstance) treeNode.getUserObject();
	    Boolean dr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doRelease);
	    Boolean newValue = null;
        if (dr == null || !dr.booleanValue())
            newValue = Boolean.TRUE;
        else
            newValue = Boolean.FALSE;
        setDR(event, newValue, treeNode, treeModel);
	    GKSchemaClass cls = (GKSchemaClass) event.getSchemClass();
	    boolean needPropagate = AttributeEditConfig.getConfig().isAutoPropagatable(cls,
	                                                                               ReactomeJavaConstants._doRelease);
	    if (!needPropagate)
	        return; 
	    //Make all contained events have the same _doNotRelease values
	    for (int i = 0; i < treeNode.getChildCount(); i++) {
	        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
	        toggleDR(childNode, treeModel, newValue);
	    }
	}
	
	private void setDR(GKInstance event, 
	                    Boolean newValue,
	                    DefaultMutableTreeNode treeNode, 
	                    DefaultTreeModel treeModel) throws Exception {
	    event.setAttributeValue(ReactomeJavaConstants._doRelease,
	            	            newValue);
	    AttributeEditManager.getManager().attributeEdit(event, 
	                                                    ReactomeJavaConstants._doRelease);
	    treeModel.nodeChanged(treeNode);	    
	}
	
	private void toggleDR(DefaultMutableTreeNode treeNode, 
                          DefaultTreeModel model, 
                          Boolean newValue) throws Exception {
	    GKInstance event = (GKInstance) treeNode.getUserObject();
	    if (event.isShell())
	        return; // A shell instance is not editable.
	    Boolean dr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doRelease);
	    if (dr == null && !newValue.booleanValue())
	        return;
	    if (newValue.equals(dr))
	        return; 
	    setDR(event, newValue, treeNode, model);
	    for (int i = 0; i < treeNode.getChildCount(); i++) {
	        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
	        toggleDR(childNode, model, newValue);
	    }
	}
	
	public Action getViewAction() {
		if (viewAction == null) {
			viewAction = new AbstractAction("View Instance",
			                                GKApplicationUtilities.createImageIcon(getClass(),"ViewInstance.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (wrappedTree.getSelectionCount() != 1)
						return;
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) wrappedTree.getLastSelectedPathComponent();
					GKInstance instance = (GKInstance) treeNode.getUserObject();
					if (instance.isShell())
						FrameManager.getManager().showShellInstance(instance, eventPane);
					else {
						if (eventPane != null)
						    FrameManager.getManager().showInstance(instance, eventPane.isEditable());
						else
						    FrameManager.getManager().showInstance(instance, true);
					}
				}
			};
		}
		return viewAction;
	}
	
	public Action getExpandNodeAction() {
		if (expandNodeAction == null) {
			expandNodeAction = new AbstractAction("Expand Node",
			                       GKApplicationUtilities.createImageIcon(getClass(),"ExpandAll.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (wrappedTree.getSelectionCount() != 1)
						return;
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) wrappedTree.getLastSelectedPathComponent();
					TreeUtilities.expandAllNodes(treeNode, wrappedTree);
				}
			};
		}
		return expandNodeAction;
	}
	
	public Action getCollapseNodeAction() {
		if (collapseNodeAction == null) {
			collapseNodeAction = new AbstractAction("Collpase Node",
			                         GKApplicationUtilities.createImageIcon(getClass(),"CollapseAll.gif")) {
				public void actionPerformed(ActionEvent e) {
					if (wrappedTree.getSelectionCount() != 1)
						return;
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) wrappedTree.getLastSelectedPathComponent();
					TreeUtilities.collapseAllNodes(treeNode, wrappedTree);
				}
			};
		}
		return collapseNodeAction;
	}
	
	public Action getDeleteAction() {
		if (deleteAction == null) {
			deleteAction = new AbstractAction("Delete",
			                                  GKApplicationUtilities.createImageIcon(getClass(),"Delete16.gif")) {
				public void actionPerformed(ActionEvent e) {
					// Want to handle one selection only
					if (wrappedTree.getSelectionCount() != 1)
						return;
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) wrappedTree.getLastSelectedPathComponent();
                    GKInstance instance = (GKInstance) treeNode.getUserObject();
                    InstanceDeletion deletion = new InstanceDeletion();
                    JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, wrappedTree);
                    deletion.delete(instance, parentFrame);
				}
			};
		}
		return deleteAction;
	}
	
	public Action getAddComponentAction() {
		if (addComponentAction == null) {
			addComponentAction = new AbstractAction("Add Component") {
				public void actionPerformed(ActionEvent e) {
					add(ReactomeJavaConstants.hasEvent);
				}
			};
		}
		return addComponentAction;
	}
	
	public Action getAddMemberAction() {
	    if (addMemberAction == null) {
	        addMemberAction = new AbstractAction("Add Member") {
	            public void actionPerformed(ActionEvent e) {
	                add(ReactomeJavaConstants.hasMember);
	            }
	        };
	    }
	    return addMemberAction;
	}
	
	public Action getAddSpecialisedFormAction() {
	    if (addSpecizlizedFormAction == null) {
	        addSpecizlizedFormAction = new AbstractAction("Add SpecialisedForm") {
	            public void actionPerformed(ActionEvent e) {
	                add(ReactomeJavaConstants.hasSpecialisedForm);
	            }
	        };
	    }
	    return addSpecizlizedFormAction;
	}
	
	private void add(String attName) {
		GKInstance instance = eventPane.getSelectedEvent();
		if (instance == null)
			return;
		if (instance.getSchemClass().isValidAttribute(attName)) {
			JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, 
																			eventPane);
			final InstanceSelectDialog dialog = new InstanceSelectDialog(parentFrame,
																     "Select Values for Attribute \"" + attName + "\"");
			SchemaAttribute att = null;
			try {
				att = instance.getSchemClass().getAttribute(attName);
			}
			catch(InvalidAttributeException e) { // Should never occur
				System.err.println("HierarchicalEventPaneActions.add(): " + e);
				e.printStackTrace();
			}
			dialog.setTopLevelSchemaClasses(att.getAllowedClasses());
			dialog.setIsMultipleValue(att.isMultiple());
			dialog.setModal(true);
			dialog.setSize(1000, 800);
			GKApplicationUtilities.center(dialog);
			dialog.setVisible(true);  
			if (dialog.isOKClicked()) {
				java.util.List instances = dialog.getSelectedInstances();
				if (instances != null && instances.size() > 0) {
					for (Iterator it = instances.iterator(); it.hasNext();) {
						GKInstance component = (GKInstance)it.next();
						instance.addAttributeValueNoCheck(att, component);
					}
					AttributeEditEvent event = new AttributeEditEvent(eventPane);
					event.setEditingInstance(instance);
					event.setAddedInstances(instances);
					event.setAttributeName(attName);
					AttributeEditManager.getManager().attributeEdit(event);
				}
			}
		}		
	}
	
	public Action getAddInstanceAction() {
		if (addInstanceAction == null) {
			addInstanceAction = new AbstractAction("Add Instance") {
				public void actionPerformed(ActionEvent e) {
					add("hasInstance");
				}
			};
		}
		return addInstanceAction;
	}

	public Action getViewReferrersAction() {
		if (viewReferrersAction == null) {
			viewReferrersAction = new AbstractAction("Display Referrers",
			                                         GKApplicationUtilities.createImageIcon(getClass(),"DisplayReferrers.gif")) {
				public void actionPerformed(ActionEvent e) {
				    if (wrappedTree.getSelectionCount() != 1)
                        return;
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) wrappedTree.getLastSelectedPathComponent();
                    GKInstance instance = (GKInstance) treeNode.getUserObject();
                    displayReferrers(instance);
				}
			};
			viewReferrersAction.putValue(Action.SHORT_DESCRIPTION, "Display instances referring to the selected one");
		}
		return viewReferrersAction;
	}
	
	private void displayReferrers(GKInstance instance) {
	    ReverseAttributePane referrersPane = new ReverseAttributePane();
	    referrersPane.displayReferrersWithCallback(instance, wrappedTree);
	}
}	
