/*
 * Created on Dec 14, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.SelectionMediator;
import org.gk.persistence.Project;
import org.gk.render.FlowLine;
import org.gk.render.Renderable;
import org.gk.render.Shortcut;
import org.gk.util.GKApplicationUtilities;

/**
 * This view is used to edit a RenderablePathway graphically. This is an entity-level view.
 * @author guanming
 *
 */
public class GraphicEditorView extends JPanel {
    // GUIs
    private PathwayEditor pathwayEditor;
    private ZoomablePathwayEditor zoomableEditor;
    JSplitPane jsp;
    // for tree
    private JLabel treeTitle;
    private PathwayComponentTree pathwayTree;
    // For overview
    private JLabel overviewTitle;
    private PathwayOverviewPane overviewPane;
    private JSplitPane leftJsp;
    private Project project;
    // Actions
    AuthorToolActionCollection actionCollection;
    // To synchronize selection
    private SelectionMediator selectionMediator;
    
    public GraphicEditorView() {
        initGUI();
    }
    
    public void initGUI() {
        setLayout(new BorderLayout());
        leftJsp = createTreePane();
        zoomableEditor = createPathwayEditor();
        jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                             leftJsp,
                             zoomableEditor);
        jsp.setResizeWeight(0.5);
        // Default
        jsp.setDividerLocation(400);
        add(jsp, BorderLayout.CENTER);
        installListeners();
    }
    
    private JSplitPane createTreePane() {
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.setBorder(BorderFactory.createEtchedBorder());
        
        pathwayTree = new PathwayComponentTree();
        pane.add(new JScrollPane(pathwayTree), BorderLayout.CENTER);
        
        treeTitle = GKApplicationUtilities.createTitleLabel("Object List");
        treeTitle.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        pane.add(treeTitle, BorderLayout.NORTH);
        
        // For pathway overview
        JPanel pane1 = createOverView();
        
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        pane,
                                        pane1);
        jsp.setResizeWeight(0.70);
        return jsp;
    }

    private JPanel createOverView() {
        overviewPane = new PathwayOverviewPane();
        overviewPane.setBorder(BorderFactory.createEtchedBorder());
        overviewTitle = GKApplicationUtilities.createTitleLabel("Overview");
        overviewTitle.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        JPanel pane1 = new JPanel();
        pane1.setLayout(new BorderLayout());
        pane1.setBorder(BorderFactory.createEtchedBorder());
        pane1.add(overviewTitle, BorderLayout.NORTH);
        pane1.add(overviewPane, BorderLayout.CENTER);
        return pane1;
    }
    
    public void setSelectionMediator(SelectionMediator mediator) {
        this.selectionMediator = mediator;
        mediator.addSelectable(pathwayEditor);
        mediator.addSelectable(pathwayTree);
    }
    
    public void setActionCollection(AuthorToolActionCollection actionCollection) {
        this.actionCollection = actionCollection;
        this.actionCollection.pathwayEditor = pathwayEditor;
    }
    
    public void setLeftScrollPaneDividerPos(int pos) {
        this.leftJsp.setDividerLocation(pos);
    }
    
    public int getLeftScrollPaneDividerPos() {
        return this.leftJsp.getDividerLocation();
    }
    
    private ZoomablePathwayEditor createPathwayEditor() {
        ZoomablePathwayEditor editor = new ZoomablePathwayEditor();
        pathwayEditor = editor.getPathwayEditor();
        return editor;
    }
    
    private void installListeners() {
        // Synchronize selection between the tree and the editor pane
        pathwayTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selectionMediator.fireSelectionEvent(pathwayTree);
            }
        });
        // Use JavaBeans property propagation mechanism for updating.
        // It is convenient but not straightforward. Consider to create
        // a special class for this purpose.
        PropertyChangeListener graphPropListener = createGraphPropListener();
        pathwayEditor.addPropertyChangeListener(graphPropListener);
        // Enable Popup
        pathwayEditor.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {    
                if (e.isPopupTrigger()) {
                    doGraphEditorPopup(e);
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doGraphEditorPopup(e);
                }
            }
        });
        // For GraphEditorActionEvents
        GraphEditorActionListener actionListener = new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {   
                if (e.getID() == GraphEditorActionEvent.REACTION_ATTACH) {
                    Object source = e.getSource();
                    if (source instanceof FlowLine) {
                        FlowLine fl = (FlowLine) source;
                        pathwayTree.insert(fl);
                        firePropertyChange("insert", 
                                           null, 
                                           fl);
                    }
                    enableSaveAction(true);
                }
                else if (e.getID() == GraphEditorActionEvent.REACTION_DETACH) {
                    enableSaveAction(true);
                }
                else if (e.getID() == GraphEditorActionEvent.MOVING) {
                    enableSaveAction(true);
                }
                else if (e.getID() == GraphEditorActionEvent.NAME_EDITING) {
                    pathwayTree.refresh((Renderable)e.getSource());
                    firePropertyChange("rename", null, e.getSource());
                    enableSaveAction(true);
                }
                else if (e.getID() == GraphEditorActionEvent.PROP_CHANGING) {
                    Renderable r = (Renderable) e.getSource();
                    pathwayTree.refresh(r);
                    firePropertyChange("isChanged", null, r);
                    enableSaveAction(true);
                }
                else if (e.getID() == GraphEditorActionEvent.ACTION_DOUBLE_CLICKED) {
                    // Call this action event to make it easy
                    ActionEvent e1 = new ActionEvent(pathwayEditor,
                                                    ActionEvent.ACTION_FIRST,
                                                    "rename");
                    actionCollection.getRenameAction().actionPerformed(e1);
                    // Change cursor
                    List selection = pathwayEditor.getSelection();
                    // Something is editing. The cursor should be in the bounds
                    if (pathwayEditor.getEditingNode() != null)
                        pathwayEditor.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                }
            }
        };
        GraphEditorActionListener selectionListener = new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.SELECTION) {
                    actionCollection.updateSelectRelatedAction();
                    selectionMediator.fireSelectionEvent(pathwayEditor);
                }
            }
        };
        pathwayEditor.addGraphEditorActionListener(actionListener);
        pathwayEditor.getSelectionModel().addGraphEditorActionListener(selectionListener);
        pathwayTree.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String name = e.getPropertyName();
                if (name.equals("rename")) {
                    pathwayEditor.repaint(pathwayEditor.getVisibleRect());
                    firePropertyChange(name,
                                       e.getOldValue(),
                                       e.getNewValue());
                }
            }
        });
        overviewPane.syncrhonizeScroll(zoomableEditor);
        overviewPane.setParentEditor(pathwayEditor);
    }
    
    public void addGraphEditorActionListener(GraphEditorActionListener l) {
        pathwayEditor.addGraphEditorActionListener(l);
    }
    
    public void showIsChangedInTree(boolean shown) {
        pathwayTree.showIsChanged(shown);
    }
    
    /**
     * Open a Renderable object in a new GraphEditorPane.
     * @param renderable
     */
    public void open(Renderable renderable) {
        if (renderable instanceof Shortcut)
            renderable = ((Shortcut)renderable).getTarget();
        display(renderable);
        pathwayEditor.killUndo();
    }
    
    /**
     * A helper method to display a Renderable object in a GraphEditorPane.
     * @param renderable the Renderable object to be displayed
     * @return true if a new display is generated. false for no new diplay generated.
     */
    private void display(Renderable renderable) {
        pathwayEditor.setRenderable(renderable);
        overviewPane.setRenderable(renderable);
    }
    
    public void enableSaveAction(boolean save) {
        actionCollection.getSaveProjectAction().setEnabled(save);
        project.setIsDirty(save);
    }
    
    private PropertyChangeListener createGraphPropListener() {
        PropertyChangeListener graphPropListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                if (propertyName.equals("insert")) {
                    if (e.getOldValue() instanceof Renderable && e.getNewValue() instanceof Renderable) {
                        Renderable parent = (Renderable)e.getOldValue();
                        Renderable child = (Renderable)e.getNewValue();
                        pathwayTree.insert(child);
                        if (!parent.isChanged()) {
                            parent.setIsChanged(true);
                            pathwayTree.refresh(parent);
                        }
                        enableSaveAction(true);
                    }
                }
                else if (propertyName.equals("delete")) {
                    Renderable container = (Renderable) e.getOldValue();
                    List deleted = (List) e.getNewValue();
                    // Need to filter for pathwayTree
                    pathwayTree.delete(deleted);
                    if (!container.isChanged()) {
                        container.setIsChanged(true);
                        pathwayTree.refresh(container);
                    }
                    enableSaveAction(true);
                }
                else if (propertyName.equals("delinkShortcuts")) {
                    Renderable renderable = (Renderable) e.getNewValue();
                    Renderable  shortcut = (Renderable) e.getOldValue();
                    pathwayTree.delinkShortcuts(renderable,
                                               shortcut);
                }
                // propagate this action
                firePropertyChange(e.getPropertyName(), 
                                   e.getOldValue(), 
                                   e.getNewValue());
            }
        };
        return graphPropListener;
    }
    
    private void doGraphEditorPopup(MouseEvent e) {
        Object source = e.getSource();
        if (source instanceof PathwayEditor)
            actionCollection.doPathwayEditorPopup((PathwayEditor)source, 
                                                  e.getPoint());
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }
    
    public PathwayComponentTree getComponentTree() {
        return pathwayTree;
    }
    
    public void openProject(Project project) {
        pathwayTree.open(project.getProcess());
        open(project.getProcess());
        this.project = project;
    }
    
    public void refresh(Renderable r) {
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        pathwayTree.refresh(r);
    }
    
    public List search(String key,
                       boolean isWholeNameMatch,
                       boolean isCaseSensitive) {
        return pathwayTree.search(key, isWholeNameMatch,
                                  isCaseSensitive);
    }
}
