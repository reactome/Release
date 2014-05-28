/*
 * Created on Nov 6, 2008
 *
 */
package org.gk.elv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.database.AttributeEditEvent;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.TreeUtilities;

/**
 * This customized JPanel is used to display complexes in a hierarchical way.
 * @author wgm
 *
 */
public class ComplexHierarchicalPane extends InstanceTreePane {
    
    public ComplexHierarchicalPane() {
    }
    
    /**
     * Set the top level complex instances to be displayed in this ComplexHierarchicalPane.
     * @param complexes
     * @throws Exception
     */
    public void setComplexes(List<GKInstance> complexes) throws Exception {
        InstanceUtilities.sortInstances(complexes);
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // Empty the tree first
        root.removeAllChildren();
        for (GKInstance complex : complexes) {
            buildTree(complex, root);
        }
        model.nodeStructureChanged(root);
    }
    
    /**
     * A recursive method to build a tree.
     * @param complex
     * @param parentNode
     * @throws Exception
     */
    private void buildTree(GKInstance complex,
                           DefaultMutableTreeNode parentNode) throws Exception {
        DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(complex);
        //parentNode.add(currentNode);
        insertInstanceNodeAlphabetically(parentNode, 
                                         currentNode);
        if (!(complex.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)))
            return;
        List components = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components == null || components.size() == 0)
            return;
        // Don't want to duplicate displaying
        Set<GKInstance> set = new HashSet<GKInstance>();
        for (Iterator it = components.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            set.add(inst);
        }
        for (GKInstance comp : set)
            buildTree(comp, currentNode);
    }

    @Override
    public void addInstance(GKInstance instance) {
        if (!isDisplayable(instance))
            return;
        // Check if this PE has been a component of other complex
        try {
            List<GKInstance> containers = searchForComplexContainers(instance);
            addInstance(instance, containers);
        }
        catch(Exception e) {
            System.err.println("ComplexHierarchicalPane.addInstance(): " + e);
            e.printStackTrace();
        }
    }
    
    private void addInstance(GKInstance instance,
                             List<GKInstance> containers) throws Exception {
        if (containers.size() == 0) {
            insertInstance(instance);
        }
        else {
            for (GKInstance container : containers) {
                List treeNodes = TreeUtilities.searchNodes(container, tree);
                if (treeNodes == null || treeNodes.size() == 0)
                    insertInstance(instance);
                for (Iterator it = treeNodes.iterator(); it.hasNext();) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
                    buildTree(instance,
                              treeNode);
                }
            }
        }
    }

    private void insertInstance(GKInstance instance) throws Exception {
        // Only a new Complex can be inserted
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
            // Insert as a new complex node
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
            // Need to add any contained node
            buildTree(instance, 
                      root);
        }
    }
    
    private List<GKInstance> searchForComplexContainers(GKInstance instance) throws Exception {
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
//        List referrers = fileAdaptor.getReferers(instance);
//        List<GKInstance> containers = new ArrayList<GKInstance>();
//        for (Iterator it = referrers.iterator(); it.hasNext();) {
//            GKInstance referrer = (GKInstance) it.next();
//            if (referrer.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
//                List components = referrer.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
//                if (components != null && components.contains(instance))
//                    containers.add(referrer);
//            }
//        }
        // A much faster way to run hasCompnent search
        Collection<?> complexes = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Complex);
        List<GKInstance> containers = new ArrayList<GKInstance>();
        for (Iterator<?> it = complexes.iterator(); it.hasNext();) {
            GKInstance complex = (GKInstance) it.next();
            List<?> list = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            if (list != null && list.contains(instance))
                containers.add(complex);
        }
        return containers;
    }
    
    @Override
    public void updateInstance(AttributeEditEvent editEvent) {
        super.updateInstance(editEvent);
        if (editEvent.getAttributeName() == null ||
            editEvent.getAttributeName().equals(ReactomeJavaConstants.hasComponent)) {
            // Need to reset the tree
            GKInstance instance = editEvent.getEditingInstance();
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                return;
            try {
                List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                List<DefaultMutableTreeNode> treeNodes = TreeUtilities.searchNodes(instance, tree);
                if (treeNodes != null && treeNodes.size() > 0) {
                    for (DefaultMutableTreeNode treeNode : treeNodes) {
                        treeNode.removeAllChildren();
                        if (components == null || components.size() == 0)
                            continue;
                        // To avoid duplication
                        Set<GKInstance> set = new HashSet<GKInstance>(components);
                        for (GKInstance comp : set) {
                            buildTree(comp, treeNode);
                        }
                    }
                }
            }
            catch(Exception e) {
                System.err.println("ComplexHierarchicalPane.updateInstance(): " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isDisplayable(GKInstance instance) {
        return instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity);
    }
    
}
