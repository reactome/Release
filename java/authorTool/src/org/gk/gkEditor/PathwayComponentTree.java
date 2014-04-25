/*
 * Created on Dec 14, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableFactory;
import org.gk.render.RenderablePathway;
import org.gk.util.TreeUtilities;

/**
 * This customized JTree is used to list Renderable objects contained by a pathway.
 * Objects in pathway are listed by categories: Entity, Complex, Gene, Reaction, Process, etc.
 * @author guanming
 *
 */
public class PathwayComponentTree extends RenderableListTree {
    // cached node
    private DefaultMutableTreeNode pathwayNode;
    
    public PathwayComponentTree() {
        init();
    }
    
    private void init() {
        initTree();
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
        setCellEditor(new TypeTreeCellEditor(this,
                                             renderer));
        setEditable(true);
    }
    
    public void addEditingListener(CellEditorListener l) {
        DefaultTreeCellEditor editor = (DefaultTreeCellEditor) getCellEditor();
        editor.addCellEditorListener(l);
    }
    
    public void open(RenderablePathway process) {
        this.pathway = process;
        DefaultTreeModel model = (DefaultTreeModel) treeModel;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        // Close the old one first
        root.removeAllChildren();
        pathwayNode = new DefaultMutableTreeNode(process);
        root.add(pathwayNode);
        List components = process.getComponents();
        if (components != null) {
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                insert(r);
            }
        }
        model.nodeStructureChanged(root);
        // Want to open all nodes
        TreeUtilities.expandAllNodes(pathwayNode, this);
    }
    
    /**
     * If force is true, a type node will be created if it is not there.
     * @param r
     * @param force
     * @return
     */
    private DefaultMutableTreeNode getTypeNode(Renderable r,
                                                boolean force) {
        String type = getTypeNodeName(r);
        if (type == null)
            return null;
        int c = pathwayNode.getChildCount();
        int index = c;
        // Check if it is there
        DefaultMutableTreeNode node = null;
        for (int i = 0; i < c; i++) {
            node = (DefaultMutableTreeNode) pathwayNode.getChildAt(i);
            String name = (String) node.getUserObject();
            if (name.equals(type))
                return node;
        }
        if (!force)
            return null;
        // Have to insert a new one
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(type);
        insertStringNodeAlphabetically(type, newNode, pathwayNode);
        return newNode;
    }

    public void delete(Renderable r) {
        if (!displayable(r))
            return;
        if (r.getContainer() != null)
            return; // Still used somewhere (e.g. in Complex).
                    // converted from shortcut to target
        DefaultMutableTreeNode typeNode = getTypeNode(r, false);
        if (typeNode == null)
            return; // This should not be possible
        int c = typeNode.getChildCount();
        DefaultMutableTreeNode toBeDeleted = null;
        for (int i = 0; i < c; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) typeNode.getChildAt(i);
            if (node.getUserObject() == r) {
                toBeDeleted = node;
                break;
            }
        }
        if (toBeDeleted == null)
            return;
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.removeNodeFromParent(toBeDeleted);
        // Delete the type node too if nothing left for that type.
        c = typeNode.getChildCount();
        if (c == 0)
            model.removeNodeFromParent(typeNode);
//        if (r instanceof RenderableComplex) {
//            // Delete all components in complex recursively
//            List components = r.getComponents();
//            if (components == null || components.size() == 0)
//                return;
//            for (Iterator it = components.iterator(); it.hasNext();) {
//                Renderable comp = (Renderable) it.next();
//                comp.setContainer(null);
//                delete(comp);
//            }
//        }
    }
    
    public void insert(Renderable r) {
        if (!displayable(r))
            return;
        DefaultMutableTreeNode typeNode = getTypeNode(r, true);
        if (typeNode == null)
            return;
        insert(r, typeNode);
    }
    
    protected DefaultMutableTreeNode insert(Renderable r, 
                                            DefaultMutableTreeNode typeNode) {
        DefaultMutableTreeNode newNode = super.insert(r, typeNode);
        // Check for components only for complex. Pathways are added based on the existing
        // Renderable objects.
        if (r instanceof RenderableComplex) {
            List components = r.getComponents();
            if (components == null || components.size() == 0)
                return newNode;
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable tmp = (Renderable) it.next();
                insert(tmp);
            }
        }
        return newNode;
    }

    private String getTypeNodeName(Renderable r) {
        String type = r.getType();
        // Exclude Pathway
        if (type.endsWith("y") && !type.endsWith("ay")) {
            type = type.substring(0, type.length() - 1) + "ies";
        }
        else if (type.endsWith("x") || type.endsWith("s"))
            type = type + "es";
        else
            type = type + "s";
        return type;
    }
    
    public List search(String key,
                       boolean isWholeNameOnly,
                       boolean isCaseSensitive) {
        List rtn = new ArrayList();
        List treeNodes = new ArrayList();
        if (pathwayNode == null)
            return rtn;
        search(treeNodes, 
               pathwayNode, 
               key,
               isWholeNameOnly, 
               isCaseSensitive);
        // Select tree path
        if (treeNodes.size() > 0) {
            clearSelection();
            for (Iterator it = treeNodes.iterator(); it.hasNext();) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
                rtn.add(treeNode.getUserObject());
                TreePath treePath = new TreePath(treeNode.getPath());
                addSelectionPath(treePath);
            }
        }
        return rtn;
    }
    
    private void search(List list,
                        DefaultMutableTreeNode treeNode,
                        String key,
                        boolean isWholeNameOnly,
                        boolean isCaseSensitive) {
        Object userObj = treeNode.getUserObject();
        if (userObj instanceof Renderable) {
            Renderable r = (Renderable) userObj;
            String name = r.getDisplayName();
            if (isWholeNameOnly && isCaseSensitive) {
                if (name.equals(key))
                    list.add(treeNode);
            }
            else if (isWholeNameOnly) {
                if (name.equalsIgnoreCase(key))
                    list.add(treeNode);
            }
            else if (isCaseSensitive) {
                if (name.indexOf(key) > -1)
                    list.add(treeNode);
            }
            else {
                key = key.toLowerCase();
                name = name.toLowerCase();
                if (name.indexOf(key) > -1)
                    list.add(treeNode);
            }
        }
        // Check children
        int count = treeNode.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            search(list,
                   tmp,
                   key,
                   isWholeNameOnly,
                   isCaseSensitive);
        }
    }
    
    class TypeTreeCellEditor extends DefaultTreeCellEditor {
        
        public TypeTreeCellEditor(JTree tree,
                                  DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }
        
        /**
         * To block delete key popup to the main event queque. Delete is used to delete
         * selected objects.
         */
        protected TreeCellEditor createTreeCellEditor() {
            Border              aBorder = UIManager.getBorder("Tree.editorBorder");
            DefaultTextField tf = new DefaultTextField(aBorder);
            DefaultCellEditor   editor = new DefaultCellEditor(tf) {
                public boolean shouldSelectCell(EventObject event) {
                    boolean retValue = super.shouldSelectCell(event);
                    return retValue;
                }
            };
            tf.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    if (isEditing())
                        stopEditing();
                }
            });
            // One click to edit.
            editor.setClickCountToStart(1);
            return editor;
        }
        
        /**
         * Proved customized Icons.
         */
        protected void determineOffset(JTree tree, Object value,
                                       boolean isSelected, boolean expanded,
                                       boolean leaf, int row) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object obj = node.getUserObject();
            if (obj instanceof Renderable)
                editingIcon = RenderableFactory.getFactory().getIcon((Renderable)obj);
            else
                editingIcon = renderer.getDefaultOpenIcon();
            if(editingIcon != null)
                offset = renderer.getIconTextGap() +
                editingIcon.getIconWidth();
            else
                offset = renderer.getIconTextGap();
        }

        /**
         * Used editing text as the display name for the editing Renderable object.
         */
        public Object getCellEditorValue() {
            String newValue = (String) super.getCellEditorValue();
            TreePath path = getEditingPath();
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = treeNode.getUserObject();
            if (userObj instanceof Renderable) {
                Renderable r = (Renderable) userObj;
                String oldName = r.getDisplayName();
                if (!newValue.equals(oldName)) {
                    r.setDisplayName(newValue);
                    // Cannot fire events using celleditorlistener since editingStopped
                    // cannot give the changed information out.
                    PathwayComponentTree.this.firePropertyChange("rename", null, r);
                }
            }
            return userObj;
        }

        /**
         * Block editing on the type nodes.
         */
        public boolean isCellEditable(EventObject event) {
            if (event instanceof MouseEvent) {
                MouseEvent e = (MouseEvent) event;
                TreePath path = getPathForLocation(e.getX(), e.getY());
                if (path == null)
                    return false;
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                // block type node editing
                if (node.getUserObject() instanceof String)
                    return false;
            }
            return super.isCellEditable(event);
        }        
        
    }
}
