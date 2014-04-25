/*
 * Created on Nov 7, 2008
 *
 */
package org.gk.elv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;

/**
 * This customized JPanel is used to display entity GKInstance objects in a tree.
 * @author wgm
 *
 */
public class EntityInstanceListPane extends InstanceTreePane {
    
    public EntityInstanceListPane() {
    }
    
    /**
     * Set the list of SchemaClasses to be displayed in this EntityInstanceListPane.
     * @param classes
     * @throws Exception
     */
    public void setSchemaClasses(List<GKSchemaClass> classes,
                                 XMLFileAdaptor fileAdaptor) throws Exception {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        for (GKSchemaClass cls : classes) {
            Collection instances = fileAdaptor.fetchInstancesByClass(cls.getName());
            // Don't add an empty class node to save space
            if (instances == null || instances.size() == 0)
                continue;
            DefaultMutableTreeNode clsNode = new DefaultMutableTreeNode(cls);
            root.add(clsNode);
            buildTree(instances, clsNode);
        }
        model.nodeStructureChanged(root);
    }
    
    private void buildTree(Collection instances, 
                           DefaultMutableTreeNode parentNode) throws Exception {
        if (instances == null || instances.size() == 0)
            return;
        List list = new ArrayList(instances);
        InstanceUtilities.sortInstances(list);
        for (Iterator it = list.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(inst);
            parentNode.add(treeNode);
        }
    }
    
    /**
     * A helper method to search for a class node.
     * @param cls
     * @return
     */
    private DefaultMutableTreeNode searchClassNode(SchemaClass cls) {
        SchemaClass displayedCls = getDisplayedPESubclass(cls);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root.getChildCount() == 0) {
            return null;
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            if (child.getUserObject() == displayedCls) 
                return child;
        }
        return null;
    }
    
    /**
     * A helper method to create a TreeNode and insert it under the root for the specified
     * SchemaClass object.
     * @param cls
     * @return
     */
    private DefaultMutableTreeNode createClassNode(SchemaClass cls) {
        SchemaClass displayedCls = getDisplayedPESubclass(cls);
        DefaultMutableTreeNode clsNode = new DefaultMutableTreeNode(displayedCls);
        // Need to insert to the root
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // Find where it should be inserted
        int index = 0;
        if (root.getChildCount() > 0) {
            for (int i = 0; i < root.getChildCount(); i++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
                SchemaClass nodeCls = (SchemaClass) node.getUserObject();
                if (nodeCls.getName().compareTo(displayedCls.getName()) > 0) {
                    index = i;
                    break;
                }
            }
        }
        model.insertNodeInto(clsNode, root, index);
        return clsNode;
    }

    private SchemaClass getDisplayedPESubclass(SchemaClass cls) {
        SchemaClass displayedCls = cls;
        // Only the first level of PE is displayed. Need to map it to one of
        // these subclasses
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        GKSchemaClass pe = (GKSchemaClass) fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.PhysicalEntity);
        for (Iterator it = pe.getSubClasses().iterator(); it.hasNext();) {
            GKSchemaClass sub = (GKSchemaClass) it.next();
            if (cls.isa(sub)) {
                displayedCls = sub;
                break;
            }
        }
        return displayedCls;
    }

    @Override
    public void addInstance(GKInstance instance) {
        SchemaClass cls = instance.getSchemClass();
        if (!cls.isa(ReactomeJavaConstants.PhysicalEntity) ||
            cls.isa(ReactomeJavaConstants.Complex))
            return; // Only display non-complex PE in this class.
        DefaultMutableTreeNode clsNode = getClassNode(cls);
        DefaultMutableTreeNode instanceNode = new DefaultMutableTreeNode(instance);
        insertInstanceNodeAlphabetically(clsNode, instanceNode);
    }

    private DefaultMutableTreeNode getClassNode(SchemaClass cls) {
        // Find the class node first
        DefaultMutableTreeNode clsNode = searchClassNode(cls);
        if (clsNode == null)
            clsNode = createClassNode(cls);
        return clsNode;
    }

    @Override
    public boolean isDisplayable(GKInstance instance) {
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity) ||
             instance.getSchemClass().isa(ReactomeJavaConstants.Complex))
            return false;
        return true;
    }
    
    @Override
    protected void reinsert(DefaultMutableTreeNode treeNode,
                            DefaultTreeModel model) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
        model.removeNodeFromParent(treeNode);
        // Check if the old parent can still be used in case type has been changed
        GKSchemaClass parentCls = (GKSchemaClass) parent.getUserObject();
        GKInstance editingInstance = (GKInstance) treeNode.getUserObject();
        if (editingInstance.getSchemClass().isa(parentCls))
            insertInstanceNodeAlphabetically(parent, treeNode);
        else {
            // Need to insert into another node
            DefaultMutableTreeNode newParent = getClassNode(editingInstance.getSchemClass());
            insertInstanceNodeAlphabetically(newParent, treeNode);
            // Check if the old parent node should be removed
            if (parent.getChildCount() == 0)
                model.removeNodeFromParent(parent);
        }
    }
    
}
