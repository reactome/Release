/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.database.SchemaDisplayPane;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;

/** 
 *  Provides a tree view on the instance hierarchy.  The user can
 *  select the types of instance that should be included into stable
 *  ID generation.
 *  
 *  TODO: this class is a bit of a mixture of view and model - these
 *  should be separated sometime.
 *  
 *  To get a List of SchemaClass objects corresponding to the user-
 *  selected classes, just do:
 *  
 *  getSchemaDisplayPane().getSelectedClasses()
 *  
 * @author croft
 */
public class IncludeInstancesPane extends JPanel {
	private IncludeInstances includeInstances;
	private IDGenerationPane iDGenerationPane;
	private IncludeInstancesController controller;
	private SchemaDisplayPane schemaDisplayPane;
	// List classes that should never be displayed
	private String[] forbiddenClasses = {
			"StableIdentifier",
	};
	
	public IncludeInstancesPane(IDGenerationPane iDGenerationPane) {
		init();
		this.iDGenerationPane = iDGenerationPane;
	}
	
	private void init() {
		includeInstances = new IncludeInstances();
		controller = new IncludeInstancesController(this);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JTextField dummyTextField = new JTextField(5);
		int minimumTextHeight = dummyTextField.getMinimumSize().height;
		
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel titleLabel = new JLabel("Included instances");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		titlePanel.add(titleLabel);
		titlePanel.setMaximumSize(new Dimension(titlePanel.getMaximumSize().width, minimumTextHeight));
		add(titlePanel);
				
		// Overwrites title
		JPanel showPane = new JPanel();
		showPane.setLayout(new FlowLayout(FlowLayout.LEADING));
		JButton defaultSelectionBtn = new JButton(controller.getDefaultSelectionAction());
		showPane.add(defaultSelectionBtn);
		JButton copyBtn = new JButton(controller.getCopyAction());
		showPane.add(copyBtn);
		JButton pasteBtn = new JButton(controller.getPasteAction());
		showPane.add(pasteBtn);
		JButton deselectAllBtn = new JButton(controller.getDeselectAllAction());
		showPane.add(deselectAllBtn);
		
		showPane.setMaximumSize(new Dimension(showPane.getMaximumSize().width, minimumTextHeight));
		add(showPane);

		schemaDisplayPane = new SchemaDisplayPane();
		schemaDisplayPane.getClassTree().getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		schemaDisplayPane.setSearchPaneVisible(false);// Dont let user do searches
		
		add(schemaDisplayPane);
		
		setPreferredSize(new Dimension(200, getPreferredSize().height));
	}
	
	public IDGenerationPane getIDGenerationPane() {
		return iDGenerationPane;
	}
	
	public TreePath[] getSelectedPaths() {
		return schemaDisplayPane.getClassTree().getSelectionPaths();
	}
	
	public TreePath[] getPaths() {
		int rowCount = schemaDisplayPane.getClassTree().getRowCount();
		TreePath[] paths = new TreePath[rowCount];
		for (int i=0; i<rowCount; i++)
			paths[i] = schemaDisplayPane.getClassTree().getPathForRow(i);
		
		return paths;
	}
	
	public void setSelectedPaths(TreePath[] paths) {
		schemaDisplayPane.getClassTree().setSelectionPaths(paths);
	}
	
	public void setSelectedPaths(String[] pathNames) {
		List selectedPathList = new ArrayList();
		TreePath path;
		DefaultMutableTreeNode treeNode;
		String schemaClassName;
		List childNodeNameList;
		
		// Map all TreePaths to their corresponding path names
		TreePath[] paths = getPaths();
		Map nameToPathMap = new HashMap();
		for (int i=0; i<paths.length; i++) {
			path = paths[i];
			treeNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			if (treeNode==null)
				System.err.println("IncludedInstancesPane.setSelectedPaths: WARNING - no TreeNode for path number: " + i);
			else {
				schemaClassName = getTreeNodeName(treeNode);
				nameToPathMap.put(schemaClassName, path);
			}
		}
		
		// Use supplied default path name list to identify
		// corresponding paths.  Also, for each of these
		// paths, find the corresponding child paths too.
		String pathName;
		for (int j=0; j<pathNames.length; j++) {
			pathName = pathNames[j];
			path = (TreePath)nameToPathMap.get(pathName);
			if (path==null) {
				System.err.println("IncludedInstancesPane.setSelectedPaths: WARNING - no path corresponding to pathName=" + pathName);
				continue;
			}
			
			selectedPathList.add(path);
			treeNode = (DefaultMutableTreeNode)path.getLastPathComponent();
			if (treeNode==null) {
				System.err.println("IncludedInstancesPane.setSelectedPaths: WARNING - no TreeNode corresponding to pathName=" + pathName);
				continue;
			}
			
			childNodeNameList = getChildNodeNames(treeNode);
			for (Iterator it = childNodeNameList.iterator(); it.hasNext();) {
				pathName = (String)it.next();
				path = (TreePath)nameToPathMap.get(pathName);
				if (path==null)
					System.err.println("IncludedInstancesPane.setSelectedPaths: WARNING - no child path corresponding to pathName=" + pathName);
				else
					selectedPathList.add(path);
			}
		}
		
		// Select the paths
		if (selectedPathList.size()>0) {
			TreePath[] selectedPaths = new TreePath[selectedPathList.size()];
			int i=0;
			for (Iterator it = selectedPathList.iterator(); it.hasNext();) {
				path = (TreePath)it.next();
				selectedPaths[i++] = path;
			}
			
			setSelectedPaths(selectedPaths);
		}
	}
	
	/**
	 * Recursively get the names of the children of the given TreeNode.
	 * 
	 * @param treeNode
	 * @return
	 */
	private List getChildNodeNames(DefaultMutableTreeNode treeNode) {
		if (treeNode==null)
			return null;
		
		List childNodeNames = new ArrayList();
		int childCount = treeNode.getChildCount();
		DefaultMutableTreeNode childNode;
		String childNodeName;
		List childofChildNodeNames;
		for (int k=0; k<childCount; k++) {
			childNode = (DefaultMutableTreeNode)treeNode.getChildAt(k);
			childNodeName = getTreeNodeName(childNode);
			if (childNodeName!=null)
				childNodeNames.add(childNodeName);
			childofChildNodeNames = getChildNodeNames(childNode);
			if (childofChildNodeNames!=null)
				childNodeNames.addAll(childofChildNodeNames);
		}
		
		return childNodeNames;
	}
	
	private String getTreeNodeName(DefaultMutableTreeNode treeNode) {
		if (treeNode==null)
			return null;
		
		String schemaClassName = null;
		try {
			GKSchemaClass schemaClass = (GKSchemaClass)treeNode.getUserObject();
			schemaClassName = schemaClass.getName();
		} catch (RuntimeException e) {
			System.err.println("IncludedInstancesPane.getTreeNodeName: WARNING - problem getting DefaultMutableTreeNode name");
			e.printStackTrace();
		}
		
		return schemaClassName;
	}
	
	/**
	 * Get all selected instance classes.
	 * 
	 * @return
	 */
	public List getSelectedClasses() {
		List selectedClasses = new ArrayList();
		TreePath[] paths = getSelectedPaths();
		
		// Maybe nothing was selected
		if (paths==null)
			return selectedClasses;
		
		int i;
		for (i=0; i<paths.length; i++) {
			TreePath path = paths[i];
			if (path == null)
				return null;
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
			selectedClasses.add(treeNode.getUserObject());
		}
		
		// TODO: It would be better to remove forbidden classes
		// before they are even displayed.
		return includeInstances.removeForbiddenClasses(selectedClasses);
	}
	
	/**
	 * Get the roots for all selected classes.  If a class, and
	 * all of its subclasses, have been selected, then only return
	 * the class.
	 * 
	 * @return
	 */
	public List getSelectedRootClasses() {
		List selectedClasses = getSelectedClasses();
		List selectedRootClasses = includeInstances.extractRootClasses(selectedClasses);
		
		// TODO: It would be better to remove forbidden classes
		// before they are even displayed.
		return includeInstances.removeForbiddenClasses(selectedRootClasses);
	}
	
	public void deselectAllClasses() {
		schemaDisplayPane.getClassTree().clearSelection();
		
	}
	
	public void clear() {
		DefaultTreeModel model = (DefaultTreeModel) schemaDisplayPane.getClassTree().getModel();
		model.setRoot(null);
	}
	
	public void setSchema(Schema schema) {
		schemaDisplayPane.setSchema(schema);
	}
}