/*
 * Created on Oct 10, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.GraphEditorUndoManager;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.SelectionMediator;
import org.gk.persistence.Project;
import org.gk.render.Renderable;
import org.gk.util.GKApplicationUtilities;

public class AuthorToolPanel extends JPanel {
    // Fixed name
    public static final String GK_EDITOR_NAME = "Reactome Author Tool";
    public static final String VERSION = "2.2";
    public static final int BUILD_NUMBER = 26;    
    // GUIs
    private GraphicEditorView graphicView;
    private PropertyEditorView propertyView;
    private JTabbedPane tabbedPane;
    AuthorToolActionCollection actionCollection;
    // Current opened project
    private Project project;
    // Two toolbars
    private JToolBar mainBar;
    private JToolBar insertBar;
    
    public AuthorToolPanel(GKEditorFrame editorFrame) {
        actionCollection = new AuthorToolActionCollection(this);
        actionCollection.setEditorFrame(editorFrame);
        initGUI();
    }
    
    public void setTextInToolbarVisible(boolean isVisible) {
        List<Component> compList = new ArrayList<Component>();
        for (Component comp : mainBar.getComponents())
            compList.add(comp);
        for (Component comp : insertBar.getComponents())
            compList.add(comp);
        if (isVisible) {
            for (Component comp : compList) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    Action action = btn.getAction();
                    String text = (String) action.getValue(Action.NAME);
                    btn.setText(text);
                }
            }
        }
        else {
            for (Component comp : compList) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    btn.setText("");
                }
            }
        }
    }
    
    public void initGUI() {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        // Mac can place the titles of tabs at the center-bottom, which is nice
        if (GKApplicationUtilities.isMac())
            tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        // Otherwise use the default position: top-left corner.
        add(tabbedPane, BorderLayout.CENTER);
        graphicView = new GraphicEditorView();
        graphicView.setActionCollection(actionCollection);
        // Add PropertyEditorView
        propertyView  = new PropertyEditorView();
        // For toolbar
        mainBar = createToolBar();
        PathwayEditorInsertActions insertActions = new PathwayEditorInsertActions();
        insertActions.setPathwayEditor(graphicView.getPathwayEditor());
        insertBar = insertActions.createToolBar();
        insertBar.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        insertBar.setFloatable(true);
        mainBar.add(insertBar);
        add(mainBar, BorderLayout.NORTH);
        tabbedPane.addTab("Graphic Editor", graphicView);
        tabbedPane.add("Property Editor", propertyView);
        // Synchronize two views
        synchronizeTwoViews();
        // To control insertBar: insertBar should appear only for graph view
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedComponent() == graphicView) {
                    insertBar.setVisible(true);
                    actionCollection.getSearchDBAction().setEnabled(true);
                    actionCollection.updateActions();
                }
                else {
                    insertBar.setVisible(false);
                    actionCollection.getSearchDBAction().setEnabled(false);
                    actionCollection.updateActions();
                }
            }
        });
        PathwayEditor editor = getPathwayEditor();
        GraphEditorUndoManager undoManager = editor.getUndoManager();
        undoManager.addUndoableEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                actionCollection.updateUndoRedoActions();
            }
            
        });
    }
    
    public void showIsChangedInTrees(boolean shown) {
        graphicView.showIsChangedInTree(shown);
        propertyView.showIsChangedInTree(shown);
    }
    
    public void selectPropertyView() {
        // The second tab
        tabbedPane.setSelectedIndex(1);
    }
    
    public void selectGraphView() {
        tabbedPane.setSelectedIndex(0);
    }
    
    public void switchView() {
        int index = tabbedPane.getSelectedIndex();
        if (index == 0)
            tabbedPane.setSelectedIndex(1);
        else
            tabbedPane.setSelectedIndex(0);
    }
    
    public Component getDisplayedView() {
        return tabbedPane.getSelectedComponent();
    }
    
    public PathwayEditor getPathwayEditor() {
        return graphicView.getPathwayEditor();
    }
    
    public PropertyEditorView getPropertyView() {
        return this.propertyView;
    }
    
    private void synchronizeTwoViews() {
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propName = e.getPropertyName();
                if (propName.equals("insert")) {
                    Renderable inserted = (Renderable) e.getNewValue();
                    propertyView.add(inserted);
                }
                else if (propName.equals("delete")) {
                    Object deleted = e.getNewValue();
                    if (deleted instanceof List)
                        propertyView.delete((List)deleted);
                    else if (deleted instanceof Renderable)
                        propertyView.delete((Renderable)deleted);
                }
                else if (propName.equals("delinkShortcuts")) {
                    Renderable target = (Renderable) e.getNewValue();
                    Renderable shortcut = (Renderable) e.getOldValue();
                    propertyView.delinkShortcuts(target, 
                                                 shortcut);
                }
                else if (propName.equals("rename") ||
                         propName.equals("isChanged")) {
                    Renderable renamed = (Renderable) e.getNewValue();
                    propertyView.refresh(renamed);
                }
            }
        };
        graphicView.addPropertyChangeListener(listener);
        // Graph Editor Actions in the PathwayEditor pane
        GraphEditorActionListener graphListener = new GraphEditorActionListener() {
            public void graphEditorAction(GraphEditorActionEvent e) {
                if (e.getID() == GraphEditorActionEvent.COMPLEX_COMPONENT_CHANGED) {
                    // Source should be a container
                    propertyView.refreshComplex((Renderable)e.getSource());
                }
            }
        };
        graphicView.addGraphEditorActionListener(graphListener);
        propertyView.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propName = e.getPropertyName();
                if (propName.equals("rename") ||
                    propName.equals("isChanged")) {
                    graphicView.refresh((Renderable)e.getNewValue());
                    enableSaveAction(true);
                }
            }
        });
        // Synchronize selections in two views using the mediator pattern
        SelectionMediator selectionMediator = new SelectionMediator();
        propertyView.setSelectionMediator(selectionMediator);
        graphicView.setSelectionMediator(selectionMediator);
    }
    
    private void formatButton(JButton btn,
                              String text) {
        btn.setText(text);
        btn.getAction().putValue(Action.NAME, text);
        btn.setVerticalTextPosition(JButton.BOTTOM);
    }
    
    private JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.addSeparator();
        JButton btn = toolbar.add(actionCollection.getNewProjectAction());
        formatButton(btn, "New Project");
        btn = toolbar.add(actionCollection.getOpenProjectAction());
        formatButton(btn, "Open");
        btn = toolbar.add(actionCollection.getSaveProjectAction());
        formatButton(btn, "Save");
        btn = toolbar.add(actionCollection.getSearchTreeAction());
        formatButton(btn, "Find");
        btn = toolbar.add(actionCollection.getCutAction());
        formatButton(btn, "Cut");
        btn = toolbar.add(actionCollection.getCopyAction());
        formatButton(btn, "Copy");
        btn = toolbar.add(actionCollection.getPasteAsAliasAction());
        // Based on feedback from Marc, use Paste as Alias only. Consider
        // to add a new action "Clone" in the future.
        //btn.setText("Paste as Alias");
        formatButton(btn, "Paste");
        btn = toolbar.add(actionCollection.getCloneAction());
        formatButton(btn, "Clone");
        btn = toolbar.add(actionCollection.getDeleteAction());
        formatButton(btn, "Delete");
        btn = toolbar.add(actionCollection.getUndoAction());
        formatButton(btn, "Undo");
        btn = toolbar.add(actionCollection.getRedoAction());
        formatButton(btn, "Redo");
        btn = toolbar.add(actionCollection.getSearchDBAction());
        formatButton(btn, "Search DB");
        toolbar.addSeparator();
        btn = toolbar.add(actionCollection.getLayoutAction());
        formatButton(btn, "Layout");
        btn = toolbar.add(actionCollection.getLayoutEdgesAction());
        formatButton(btn, "Layout Edge");
        btn = toolbar.add(actionCollection.getToggleShowPathwayAction());
        formatButton(btn, "Draw Pathway");
        return toolbar;
    }
    
    /**
     * Open a Renderable object in a new GraphEditorPane.
     * @param renderable
     */
    public void open(Renderable renderable) {
        graphicView.open(renderable);
        propertyView.open(renderable);
    }
    
    public GraphEditorPane getDisplayedGraphPane() {
        return graphicView.getPathwayEditor();
    }
    
    public GraphicEditorView getGraphicView() {
        return this.graphicView;
    }
    
    public void enableSaveAction(boolean save) {
        actionCollection.getSaveProjectAction().setEnabled(save);
        project.setIsDirty(save);
    }
    
    public void openProject(Project project) {
        this.project = project;
        graphicView.openProject(project);
        propertyView.openProject(project);
    }
    
    public AuthorToolActionCollection getActionCollection() {
        return actionCollection;
    }
    
    void setJScrollPaneDividerPosition(int pos) {
        graphicView.jsp.setDividerLocation(pos);
    }
    
    int getJScrollPaneDividerPosition() {
        return graphicView.jsp.getDividerLocation();
    }
    
    public List search(String key,
                       boolean isWholeNameMatch,
                       boolean isCaseSensitive) {
        List list = graphicView.search(key,
                                       isWholeNameMatch,
                                       isCaseSensitive);
        return list;
    }
}
