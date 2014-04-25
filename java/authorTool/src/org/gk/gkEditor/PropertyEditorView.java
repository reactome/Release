/*
 * Created on Dec 14, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.persistence.Project;
import org.gk.property.RenderablePropertyChangeEvent;
import org.gk.property.RenderablePropertyChangeListener;
import org.gk.render.Renderable;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.Shortcut;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPanel is used to edit properties for objects displayed. 
 * @author guanming
 *
 */
@SuppressWarnings("unchecked")
public class PropertyEditorView extends JPanel {
    // GUIs
    private PropertyObjectListPane objectListPane;
    private TreeSelectionCoordinator selectionCoordinator = new TreeSelectionCoordinator();
    private TreeSelectionListener treeSelectionListener;
    // Used to hold property pane
    private PropertyEditorPane propertyPane;
    private JSplitPane jsp;
    // An overall view for the editing pathway
    private JLabel overviewTitle;
    private PathwayEditor overview;
    // To synchronize selection
    private SelectionMediator selectionMediator;
    
    public PropertyEditorView() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        objectListPane = new PropertyObjectListPane();
        propertyPane = createPropertyPane();
        JPanel overviewPane = createOverviewPane();
        JSplitPane leftJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            objectListPane,
                                            overviewPane);
        leftJsp.setResizeWeight(0.5);
        // Minimum size for the tree
        leftJsp.setDividerLocation(400);
        jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                             leftJsp,
                             propertyPane);
        jsp.setResizeWeight(0.5);
        // Default
        jsp.setDividerLocation(400);
        add(jsp, BorderLayout.CENTER);
        installListeners();
    }
    
    public void setSelectionMediator(SelectionMediator selectionMediator) {
        this.selectionMediator = selectionMediator;
        selectionMediator.addSelectable(selectionCoordinator);
        selectionMediator.addSelectable(overview);
    }
    
    public void repaintOverview() {
        overview.repaint();
    }
    
    private JPanel createOverviewPane() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setBorder(BorderFactory.createEtchedBorder());
        // Want to resize automatically
        overview = new PathwayOverviewPane();
        overview.setBorder(BorderFactory.createEtchedBorder());
        pane.add(overview, BorderLayout.CENTER);
        overviewTitle = GKApplicationUtilities.createTitleLabel("Overview");
        overviewTitle.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        pane.add(overviewTitle, BorderLayout.NORTH);
        return pane;
    }
    
    public PathwayEditor getPathwayEditor() {
        return this.overview;
    }
    
    private void showProperty() {
        List list = selectionCoordinator.getSelection();
        // Need to consolidate based on name since
        // no shortcuts have been used as of July 16, 2008.
        // Have to figure out shortcuts
        Map<String, Renderable> nameToRenderable = new HashMap<String, Renderable>();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (!nameToRenderable.containsKey(r.getDisplayName())) {
                nameToRenderable.put(r.getDisplayName(),
                                     r);
            }
        }
        if (nameToRenderable.size() == 1) {
            Renderable r = nameToRenderable.values().iterator().next();
            open(r);
        }
        else if (nameToRenderable.size() == 0) {
            open(null);
        }
    }
        
    private void installListeners() {
        treeSelectionListener = new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                objectListPane.clearSelection();
                showProperty();
                selectionMediator.fireSelectionEvent(selectionCoordinator);
            }
        };
        objectListPane.addTreeSelectionListener(treeSelectionListener);
        GraphEditorActionListener selectionListener = new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.SELECTION) {
                    selectionMediator.fireSelectionEvent(overview);
                }
            }
        };
        overview.getSelectionModel().addGraphEditorActionListener(selectionListener);
    }
    
    public void showIsChangedInTree(boolean shown) {
        objectListPane.showIsChangedInTree(shown);
    }
    
    public void delinkShortcuts(Renderable target,
                                Renderable shortcut) {
        objectListPane.delinkShortcuts(target, shortcut);
    }
    
    /**
     * Display properties for the passed renderable object.
     * @param renderable
     */
    public void open(Renderable renderable) {
        if (renderable instanceof Shortcut)
            renderable = ((Shortcut)renderable).getTarget();
        display(renderable);
    }    
    
    private void display(Renderable r) {
        propertyPane.display(r);
    }
    
    private PropertyEditorPane createPropertyPane() {
        PropertyEditorPane propertyPane = new PropertyEditorPane();
        RenderablePropertyChangeListener l = new RenderablePropertyChangeListener() {
            public void propertyChange(RenderablePropertyChangeEvent e) {
                Renderable r = e.getRenderable();
                objectListPane.refresh(r);
                String propName = e.getPropName();
                if (propName.equals(RenderablePropertyNames.DISPLAY_NAME)) {
                    overview.repaint();
                    firePropertyChange("rename", null, r);
                }
                else // A common name should be fine
                    firePropertyChange("isChanged", null, e.getRenderable());
            }
        };
        propertyPane.setPropertyChangeListener(l);
        return propertyPane;
    }
    
    // The following six methods delegate editing actions to PropertyEditorTree.
    public void add(Renderable r) {
        objectListPane.add(r);
    }
    
    public void delete(Renderable r) {
        objectListPane.delete(r);
    }
    
    public void delete(List list) {
        objectListPane.delete(list);
    }
    
    public void openProject(Project project) {
        objectListPane.open(project.getProcess());
        overview.setRenderable(project.getProcess());
        // When a project is opened, display properties for the top-level pathway
        List list = new ArrayList();
        list.add(project.getProcess());
        selectionCoordinator.setSelection(list);
    }
    
    /**
     * Refresh the display of a Node. The structure of the tree will not change.
     * @param r
     */
    public void refresh(Renderable r) {
        objectListPane.refresh(r);
        // Refresh property panel
        propertyPane.refresh(r);
    }
    
    public void refreshComplex(Renderable complex) {
        objectListPane.refreshComplex(complex);
    }
    
    /**
     * This inner class is used to coordinate selections in the three trees.
     * @author guanming
     *
     */
    private class TreeSelectionCoordinator implements Selectable {
        public TreeSelectionCoordinator() {
        }
        
        public List getSelection() {
            return objectListPane.getSelection();
        }

        public void setSelection(List selection) {
            objectListPane.setSelection(selection, 
                                        treeSelectionListener);
            showProperty();
        }
        
    }
}
