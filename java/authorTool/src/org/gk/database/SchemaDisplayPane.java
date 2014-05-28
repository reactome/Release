/*
 * Created on Sep 22, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;

/**
 * This customized JPanel is used to display the schema of GK.
 * @author wgm
 */
public class SchemaDisplayPane extends JPanel {
	private JTree classTree;
	private SearchPane searchPane;
	private GKSchema schema = null;
	private Map counterMap = new HashMap();
	private Map isDirtyMap = new HashMap();
	private JLabel classLabel;
	
	public SchemaDisplayPane() {
		init();
	}

	public SchemaDisplayPane(Schema schema) {
		this();
		setSchema(schema);
	}
	
	public void setTitle(String title) {
		classLabel.setText(title);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		classLabel = new JLabel("Classes");
		classLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		classLabel.setHorizontalAlignment(JLabel.LEFT);
		add(classLabel, BorderLayout.NORTH);
		// Add a tree for display schema
		classTree = new JTree();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		DefaultTreeModel model = new DefaultTreeModel(root);
		classTree.setModel(model);
		classTree.setCellRenderer(new SchemaCellRenderer());
		classTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		add(new JScrollPane(classTree), BorderLayout.CENTER);
		JPanel searchContentPane = new JPanel();
		searchContentPane.setLayout(new BorderLayout());
		searchContentPane.setBorder(BorderFactory.createLoweredBevelBorder());
		JLabel searchLabel = new JLabel("Search Instances");
		searchLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		searchLabel.setHorizontalAlignment(JLabel.LEFT);
		searchContentPane.add(searchLabel, BorderLayout.NORTH);
		searchPane = new SearchPane();
		Border border1 = BorderFactory.createEmptyBorder(4, 4, 4, 4);
		Border border2 = BorderFactory.createEtchedBorder();
		searchPane.setBorder(BorderFactory.createCompoundBorder(border2, border1));
		searchContentPane.add(searchPane, BorderLayout.CENTER);
		add(searchContentPane, BorderLayout.SOUTH);
		classTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath path = classTree.getSelectionPath();
				if (path == null)
					return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				GKSchemaClass schemaClass = (GKSchemaClass)node.getUserObject();
				searchPane.setSelectedClass(schemaClass);
			}
		});
	}
	
	public void setSchema(Schema schema1) {
		if (schema1 instanceof GKSchema) {
			this.schema = (GKSchema)schema1;
			GKSchemaClass rootClass = (GKSchemaClass) schema.getRootClass();
			DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootClass);
			model.setRoot(root);
			displaySchema(root, rootClass);
			// Expand all
			for (int i = 0; i < classTree.getRowCount(); i++)
				classTree.expandRow(i);
			// Popup the search pane
			searchPane.setSchema(schema);
		}
	}
	
	public void setTopLevelSchemaClasses(Collection list) {
		DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
		// Have to use a fake root
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		model.setRoot(root);
		for (Iterator it = list.iterator(); it.hasNext();) {
			GKSchemaClass cls = (GKSchemaClass) it.next();
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(cls);
			root.add(treeNode);
			displaySchema(treeNode, cls);
		}
		// Expand all
		for (int i = 0; i < classTree.getRowCount(); i++)
			classTree.expandRow(i);
		classTree.setRootVisible(false);
		searchPane.setTopLevelSchemaClasses(list);
	}
	
	/**
	 * Set the class counter map so that the counter can be displayed.
	 * @param map key: SchemaClass; value: Long
	 */
	public void setClassCounts(Map map) {
		counterMap.clear();
		if (map != null)
			counterMap.putAll(map);
	}
	
	public void setIsDirtyMap(Map map) {
		isDirtyMap.clear();
		if (map != null)
			isDirtyMap.putAll(map);
	}
	
	public void addClassCounts(Map map) {
		if (map == null)
			return;
		for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Long c1 = (Long) map.get(key);
			Long c2 = (Long) counterMap.get(key);
			if (c2 == null)
				counterMap.put(key, c1);
			else {
				counterMap.put(key, new Long(c1.longValue() + c2.longValue()));
			}
		}
	}
	
	/**
	 * A recursive method to display a Schema in a Tree.
	 * @param treeNode
	 * @param schemaClass
	 */
	private void displaySchema(DefaultMutableTreeNode treeNode, GKSchemaClass schemaClass) {
		Collection subClasses = schemaClass.getSubClasses();
		if (subClasses == null || subClasses.size() == 0)
			return;
		for (Iterator it = subClasses.iterator(); it.hasNext();) {
			GKSchemaClass subClass = (GKSchemaClass) it.next();
			DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(subClass);
			insertNodeAlphabetically(treeNode, subNode);
			displaySchema(subNode, subClass);
		}
	}
	
	private void insertNodeAlphabetically(DefaultMutableTreeNode parentNode, 
	                                      DefaultMutableTreeNode childNode) {
		GKSchemaClass childClass = (GKSchemaClass) childNode.getUserObject();
		String childName = childClass.getName();
		// Find an index for childNode
		int index = -1;
		DefaultMutableTreeNode dmt = null;
		GKSchemaClass gsc = null;
		for (int i = 0; i < parentNode.getChildCount(); i++) {
			dmt = (DefaultMutableTreeNode) parentNode.getChildAt(i);
			gsc = (GKSchemaClass) dmt.getUserObject();
			if (childName.compareTo(gsc.getName()) < 0) {
				index = i;
				break;
			}
		}
		if (index == -1)
			index = parentNode.getChildCount();
		parentNode.insert(childNode, index);
	}
	
	public Schema getSchema() {
		return this.schema;
	}
	
	public GKSchemaClass getSelectedClass() {
		TreePath path = classTree.getSelectionPath();
		if (path == null)
			return null;
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		return (GKSchemaClass) treeNode.getUserObject();
	}
	
	public void setSelectedClass(SchemaClass schemaClass) {
		DefaultMutableTreeNode treeNode = TreeUtilities.searchNode(schemaClass, classTree);
		if (treeNode != null) {
			DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
			TreePath path = new TreePath(model.getPathToRoot(treeNode));
			classTree.setSelectionPath(path);
		}
	}
	
	public void addSelectionListener(TreeSelectionListener listener) {
		classTree.getSelectionModel().addTreeSelectionListener(listener);
	}
	
	public void removeSelectionListener(TreeSelectionListener listener) {
		classTree.getSelectionModel().removeTreeSelectionListener(listener);
	}
	
	public SearchPane getSearchPane() {
		return this.searchPane;
	}
	
	public void setSearchPaneVisible(boolean isVisible) {
		JPanel pane = (JPanel) searchPane.getParent();
		pane.setVisible(isVisible);
	}
	
	public JTree getClassTree() {
		return classTree;
	}
	
	public List<SchemaClass> getDisplayedClasses() {
	    Set<SchemaClass> classes = new HashSet<SchemaClass>();
	    Set<Object> userObjects = TreeUtilities.grepAllUserObjects(classTree);
	    for (Object obj : userObjects) {
	        if (obj instanceof SchemaClass) {
	            classes.add((SchemaClass)obj);
	        }
	    }
	    List<SchemaClass> list = new ArrayList<SchemaClass>(classes);
	    InstanceUtilities.sortSchemaClasses(list);
	    return list;
	}
	
	/**
	 * Update the tree node that displays SchemaClass for adding a new Instance.
	 * @param schemaClass
	 */
	public void addInstance(Instance instance) {
		SchemaClass schemaClass = instance.getSchemClass();
		updateCounter((GKSchemaClass)schemaClass, true);
		// Check for dirty instance
		if (isDirtyMap != null && ((GKInstance)instance).isDirty()) {
			markAsDirty((GKSchemaClass)instance.getSchemClass());
		}
		updateTreeNode(schemaClass);
	}
	
	private void markAsDirty(GKSchemaClass cls) {
	    if (isDirtyMap == null)
	        return;
	    isDirtyMap.put(cls, Boolean.TRUE);
	    for (Iterator it = cls.getOrderedAncestors().iterator(); it.hasNext();)
	        isDirtyMap.put(it.next(), Boolean.TRUE);
	}
	
	private void updateCounter(GKSchemaClass schemaClass, boolean isAdd) {
		if (counterMap == null)
		    return;
		Set clsList = new HashSet(schemaClass.getOrderedAncestors());
		clsList.add(schemaClass);
		if (isAdd) {
            for (Iterator it = clsList.iterator(); it.hasNext();) {
                GKSchemaClass tmp = (GKSchemaClass) it.next();
                Long oldValue = (Long) counterMap.get(tmp);
                if (oldValue == null)
                    counterMap.put(tmp, new Long(1));
                else
                    counterMap.put(tmp, new Long(oldValue.intValue() + 1));
            }
        }
        else {
            // delete
            for (Iterator it = clsList.iterator(); it.hasNext();) {
                GKSchemaClass tmp = (GKSchemaClass) it.next();
                Long oldValue = (Long) counterMap.get(tmp);
                if (oldValue == null || oldValue.intValue() == 0)
                    continue;
                counterMap.put(tmp, new Long(oldValue.longValue() - 1));
            }
        }
	}
	
	public void deleteInstance(Instance instance) {
		GKSchemaClass schemaClass = (GKSchemaClass)instance.getSchemClass();
		deleteInstance(schemaClass);
	}
	
	/**
	 * Delete an GKInstance in a specified GKSchemaClass.
	 * @param cls
	 */
	public void deleteInstance(GKSchemaClass schemaClass) {
		updateCounter(schemaClass, false);
		updateDirtyFlag(schemaClass); // It might be removed if a new instance is deleted
		updateTreeNode(schemaClass);
	}
	
	public void markAsDirty(GKInstance instance) {
		if (isDirtyMap == null)
			return;
		GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
		Boolean oldValue = (Boolean) isDirtyMap.get(cls);
		if (oldValue != null && oldValue.booleanValue())
			return;
		markAsDirty(cls);
		updateTreeNode(cls);
	}
	
	public void removeDirtyFlag(GKInstance instance) {
	    if (isDirtyMap == null) 
	        return;
	    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
	    try {
            boolean isDirty = fileAdaptor.isDirty(cls);
            if (!isDirty) { // Need to update it from true to false
                // Update for the ancestors
                Collection c = cls.getOrderedAncestors();
                for (Iterator it = c.iterator(); it.hasNext();) {
                    GKSchemaClass cls1 = (GKSchemaClass) it.next();
                    boolean isDirty1 = fileAdaptor.isDirty(cls1);
                    if (!isDirty1)
                        isDirtyMap.put(cls1, Boolean.FALSE);
                }
                isDirtyMap.put(instance.getSchemClass(), Boolean.FALSE);
            }
            updateTreeNode(cls);
        }
        catch (Exception e) {
            System.err.println("SchemaDisplayPane.removeDirtyFlag(): " + e);
            e.printStackTrace();
        }
	}
	
	public void clearDeleteRecord(Collection cls) {
	    if (isDirtyMap == null)
	        return ;
	    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    GKSchemaClass schemaCls = null;
	    for (Iterator it = cls.iterator(); it.hasNext();) {
	        schemaCls = (GKSchemaClass) it.next();
	        updateDirtyFlag(schemaCls);
	        updateTreeNode(schemaCls);
	    }
	}
	
	private void updateDirtyFlag(GKSchemaClass cls) {
	    if (isDirtyMap == null)
	        return;
	    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    try {
            Set clsSet = new HashSet(cls.getOrderedAncestors());
            clsSet.add(cls);
            GKSchemaClass tmpCls = null;
            boolean isDirty;
            for (Iterator it = clsSet.iterator(); it.hasNext();) {
                tmpCls = (GKSchemaClass) it.next();
                isDirty = fileAdaptor.isDirty(tmpCls);
                if (isDirty)
                    isDirtyMap.put(tmpCls, Boolean.TRUE);
                else
                    isDirtyMap.put(tmpCls, Boolean.FALSE);
            }
        }
        catch (Exception e) {
            System.err.println("SchemaDisplayPane.removeDirtyFlag(): " + e);
            e.printStackTrace();
        }
	}
	
	private void updateTreeNode(SchemaClass cls) {
	    Collection c = new HashSet(((GKSchemaClass)cls).getOrderedAncestors());
	    c.add(cls);
	    for (Iterator it = c.iterator(); it.hasNext();) {
            GKSchemaClass tmp = (GKSchemaClass) it.next();
            List treeNodes = TreeUtilities.searchNodes(tmp, classTree);
            if (treeNodes != null && treeNodes.size() > 0) {
                DefaultTreeModel model = (DefaultTreeModel) classTree.getModel();
                for (Iterator it1 = treeNodes.iterator(); it1.hasNext();) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it1.next();
                    model.nodeChanged(treeNode);
                }
            }
        }
	}
	
	class SchemaCellRenderer extends DefaultTreeCellRenderer {
		private Icon icon;
		
		public SchemaCellRenderer() {
			super();
			icon = GKApplicationUtilities.createImageIcon(getClass(),"Class.gif");
		}
		
		public Component getTreeCellRendererComponent(
			JTree tree,
			Object value,
			boolean sel,
			boolean expanded,
			boolean leaf,
			int row,
			boolean hasFocus) {
			Component comp = super.getTreeCellRendererComponent(tree, value, sel, 
			                                 expanded, leaf, row, hasFocus);
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
			GKSchemaClass gkSchema = (GKSchemaClass) treeNode.getUserObject();
			if (gkSchema != null) {
				Long value1 = (Long)counterMap.get(gkSchema);
				Boolean isDirty = (Boolean) isDirtyMap.get(gkSchema);
				String label = null;
				if (isDirty != null && isDirty.booleanValue())
					label = ">" + gkSchema.getName();
				else
					label = gkSchema.getName();
				if (value1 != null) {
					setText(label + " [" + value1 + "]");
				}
				else
					setText(label);
				setIcon(icon);
			}
			else {
				setText("");
				setIcon(null);
			}
			return comp;
		}
	}
}
