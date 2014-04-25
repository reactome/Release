/*
 * Created on Nov 3, 2008
 *
 */
package org.gk.gkEditor;

import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;

import org.gk.persistence.Project;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.util.AccordionPane;

/**
 * This panel is used to list objects in an appropriate way.
 * @author wgm
 *
 */
@SuppressWarnings("unchecked")
public class PropertyObjectListPane extends AccordionPane {
    // Tree to display objects
    protected EventPropertyEditorTree eventTree;
    protected ComplexPropertyEditorTree complexTree;
    protected EntityPropertyEditorTree entityTree;
    
    public PropertyObjectListPane() {
        init();
    }
    
    private void init() {
        // Event hierarchy
        eventTree = new EventPropertyEditorTree();
        JScrollPane eventJsp = new JScrollPane(eventTree);
        addTab("Event Hierarchy", eventJsp);
        
        // Complex hierarchy
        complexTree = new ComplexPropertyEditorTree();
        JScrollPane complexJsp = new JScrollPane(complexTree);
        addTab("Complex Hierarchy", complexJsp);
        
        // Entity List
        entityTree = new EntityPropertyEditorTree();
        JScrollPane entityJsp = new JScrollPane(entityTree);
        addTab("Entity List", entityJsp);
        
        complexTree.setEntityModel((DefaultTreeModel) entityTree.getModel());
    }
    
    private boolean isEvent(Renderable r) {
        if (r instanceof RenderablePathway ||
            r instanceof HyperEdge)
            return true;
        return false;
    }
    
    // The following six methods delegate editing actions to PropertyEditorTree.
    public void add(Renderable r) {
        if (isEvent(r))
            eventTree.insert(r);
        else if (r instanceof RenderableComplex) {
            complexTree.insert(r);
        }
        else {
            entityTree.insert(r);
        }
    }
    
    public void delete(Renderable r) {
        if (isEvent(r))
            eventTree.delete(r);
        else if (r instanceof RenderableComplex)
            eventTree.delete(r);
        else {
            // Not sure if this r is in entityTree only
            entityTree.delete(r);
            complexTree.delete(r);
        }
    }
    
    public void delete(List list) {
        eventTree.delete(list);
        complexTree.delete(list);
        entityTree.delete(list);
    }
    
    private RenderableListTree getVisibleTree() {
        JScrollPane jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, eventTree);
        if (jsp.isVisible())
            return eventTree;
        jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, complexTree);
        if (jsp.isVisible())
            return complexTree;
        jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, entityTree);
        if (jsp.isVisible())
            return entityTree;
        return null;
    }
    
    public void openProject(Project project) {
        eventTree.open(project.getProcess());
        complexTree.open(project.getProcess());
        entityTree.open(project.getProcess());
    }
    
    /**
     * Refresh the display of a Node. The structure of the tree will not change.
     * @param r
     */
    public void refresh(Renderable r) {
        if (isEvent(r))
            eventTree.refresh(r);
        else {
            complexTree.refresh(r);
            entityTree.refresh(r);
        }
    }
    
    public void refreshComplex(Renderable complex) {
        complexTree.refreshNode(complex);
    }
    
    public void clearSelection() {
        RenderableListTree tree = getVisibleTree();
        if (eventTree != tree)
            eventTree.clearSelection();
        if (complexTree != tree)
            complexTree.clearSelection();
        if (entityTree != tree)
            entityTree.clearSelection();
    }
    
    public void addTreeSelectionListener(TreeSelectionListener treeSelectionListener) {
        eventTree.addTreeSelectionListener(treeSelectionListener);
        complexTree.addTreeSelectionListener(treeSelectionListener);
        entityTree.addTreeSelectionListener(treeSelectionListener);
    }
    
    public void showIsChangedInTree(boolean shown) {
        eventTree.showIsChanged(shown);
        complexTree.showIsChanged(shown);
        entityTree.showIsChanged(shown);
    }
    
    public void delinkShortcuts(Renderable target,
                                Renderable shortcut) {
        // Currently support complex only
        if (target instanceof RenderableComplex)
            complexTree.delinkShortcuts(target, shortcut);
    }

    public void open(RenderablePathway pathway) {
        eventTree.open(pathway);
        complexTree.open(pathway);
        entityTree.open(pathway);
    }
    
    public List getSelection() {
        // Check which tree is visible
        RenderableListTree tree = getVisibleTree();
        if (tree == null)
            return null;
        return tree.getSelection();
    }
    
    public void setSelection(List selection,
                             TreeSelectionListener treeSelectionListener) {
        eventTree.removeTreeSelectionListener(treeSelectionListener);
        complexTree.removeTreeSelectionListener(treeSelectionListener);
        entityTree.removeTreeSelectionListener(treeSelectionListener);
        // Find which tree should be visible
        eventTree.setSelection(selection);
        complexTree.setSelection(selection);
        entityTree.setSelection(selection);
        eventTree.addTreeSelectionListener(treeSelectionListener);
        complexTree.addTreeSelectionListener(treeSelectionListener);
        entityTree.addTreeSelectionListener(treeSelectionListener);
        if (selection == null || selection.size() == 0)
            return;
        // Priority: Event, Complex and Entity
        if (eventTree.getSelection().size() > 0) {
            setIsClosed("Event Hierarchy", false);
            setIsClosed("Complex Hierarchy", true);
            setIsClosed("Entity List", true);
        }
        else if (entityTree.getSelection().size() > 0) { // Want to do entity first since most likely entity, not subunit is selected
            setIsClosed("Event Hierarchy", true);
            setIsClosed("Complex Hierarchy", true);
            setIsClosed("Entity List", false);
        }
        else {
            setIsClosed("Event Hierarchy", true);
            setIsClosed("Complex Hierarchy", false);
            setIsClosed("Entity List", true);
        }
    }
}
