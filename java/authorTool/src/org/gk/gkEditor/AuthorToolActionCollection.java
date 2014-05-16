/*
 * Created on Oct 10, 2006
 *
 */
package org.gk.gkEditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.gk.graphEditor.ComplexGraphEditor;
import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.GraphEditorTransferHandler;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ReactionNodeGraphEditor;
import org.gk.graphEditor.UndoableDeleteEdit;
import org.gk.graphEditor.UndoableEdgeLayoutEdit;
import org.gk.graphEditor.UndoableLayoutEdit;
import org.gk.persistence.Project;
import org.gk.property.SearchDatabasePane;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;
import org.gk.render.RenderableRegistry;
import org.gk.util.AboutGKPane;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.ProgressPane;
import org.gk.util.TextSearchPane;

public class AuthorToolActionCollection {
    private Action deleteAction;
    private Action saveProjectAction;
    private Action aboutAction;
    private Action renameAction;
    private Action layoutAction;
    private Action layoutEdgesAction;
    private Action cutAction;
    private Action copyAction;
    private Action pasteAsAliasAction;
    private Action selectAllAction;
    private Action cloneAction;
    private Action searchTreeAction;
    private Action helpAction;
    private Action searchDBAction;
    private Action undoAction;
    private Action redoAction;
    // The parent frame
    AuthorToolPanel toolPane;
    private Action newProjectAction;
    private Action openProjectAction;
    private Action saveAsAction;
    private Action optionAction;
    // To control is a private text note should be hidden
    private Action privateNoteAction;
    // For view switch
    private Action switchViewAction;
    // For diagram exporting
    private Action exportDiagramAction;
    // To control if isChanged should be shown
    private Action flagChangedAction;
    // Used to control pathways should be displayed
    private Action toggleShowPathwayAction;
    // Used to dump coodiates into the backend database
    private Action saveCoordinatesToDbAction;
    // Action used to remove compartments from displayName
    private Action removeComptFromNameAction;
    // This method is used to right the bounds for nodes
    private Action tightBoundsAction;
    // Make sure all text are wrapped in node bounds
    private Action wrapTextIntoNodesAction;
    // Action used to append compartments to display names
    private Action showCompartmentInNameAction;
    // To control if events with _doNotRelease should be shown
    private Action doNotReleaseAction;
    // The parent frame
    GKEditorFrame editorFrame;
    // Refactored class to defer actions related to popup
    private PopupMenuManager popupManager;
    // Use some system properties
    protected Properties systemProperties;
    protected PathwayEditor pathwayEditor;
    
    /**
     * A protected constructor is used for subclassing.
     */
    protected AuthorToolActionCollection() {
        
    }
    
    public AuthorToolActionCollection(AuthorToolPanel toolPane) {
        this.toolPane = toolPane;
        popupManager = new PopupMenuManager(this);
    }
    
    public void setEditorFrame(GKEditorFrame editorFrame) {
        this.editorFrame = editorFrame;
        this.systemProperties = editorFrame.getProperties();
    }
    
    public void doPathwayEditorPopup(PathwayEditor editor,
                                     Point point) {
        popupManager.doPathwayEditorPopup(editor, point);
    }
    
    public Action getPrivateNoteAction() {
        if (privateNoteAction == null) {
            privateNoteAction = new AbstractAction("Hide Private Notes") {
                
                public void actionPerformed(ActionEvent e) {
                    togglePrivateNoteAction();
                }
            };
        }
        if (pathwayEditor.getHidePrivateNote())
            privateNoteAction.putValue(Action.NAME, "Show Private Notes");
        else
            privateNoteAction.putValue(Action.NAME, "Hide Private Notes");
        return privateNoteAction;
    }
    
    public Action getDoNotReleaseAction() {
        if (doNotReleaseAction == null) {
            doNotReleaseAction = new AbstractAction("Hide doNotRelease Events") {
                public void actionPerformed(ActionEvent e) {
                    toggleDoNotReleaseEvent();
                }
            };
            doNotReleaseAction.putValue(Action.NAME, "Hide doNotRelease Events");
        }
        return doNotReleaseAction;
    }
    
    protected void toggleDoNotReleaseEvent() {
        // Check the label
        String label = (String) doNotReleaseAction.getValue(Action.NAME);
        if (label.startsWith("Hide")) {
            setDoNotReleaseEventVisible(false);
            doNotReleaseAction.putValue(Action.NAME, "Show doNotRelease Events");
        }
        else if (label.startsWith("Show")) {
            setDoNotReleaseEventVisible(true);
            doNotReleaseAction.putValue(Action.NAME, "Hide doNotRelease Events");
        }
    }
    
    /**
     * Used as a template. Nothing is done for the author tool.
     * @param visible
     */
    protected void setDoNotReleaseEventVisible(boolean visible) {
        
    }
    
    private void togglePrivateNoteAction() {
        boolean hidePrivateNote = pathwayEditor.getHidePrivateNote();
        pathwayEditor.setHidePrivateNote(!hidePrivateNote);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
    }
    
    public Action getCreateLinkAction() {
        return null;
    }
    
    public Action getCreateSetAndMemberLinkAction() {
        return null;
    }
    
    public Action getCreateSetAndSetLinkAction() {
        return null;
    }
    
    public Action getToggleShowPathwayAction() {
        if (toggleShowPathwayAction == null) {
            toggleShowPathwayAction = new TogglePathwayAction();
        }
        return toggleShowPathwayAction;
    }
    
    public Action getExpandDiagramAction() {
        return null;
    }
    
    public Action getRemoveCompartFromNameAction() {
        if (removeComptFromNameAction == null) {
            removeComptFromNameAction = new AbstractAction("Remove Compartment from Name") {
                public void actionPerformed(ActionEvent e) {
                    removeCompartmentFromName();
                }
            };
        }
        return removeComptFromNameAction;
    }
    
    public Action getTightBoundsAction() {
        if (tightBoundsAction == null) {
            tightBoundsAction = new AbstractAction("Tight Node Bounds") {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.tightNodes();
                }
            };
        }
        return tightBoundsAction;
    }
    
    public Action getWrapTextIntoNodesAction() {
        if (wrapTextIntoNodesAction == null) {
            wrapTextIntoNodesAction = new AbstractAction("Wrap Text into Nodes") {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.tightNodes(true);
                }
            };
            wrapTextIntoNodesAction.putValue(Action.SHORT_DESCRIPTION,
                                             "Change the node sizes to wrap text into nodes fully");
        }
        return wrapTextIntoNodesAction;
    }
    
    private void recordCompartmentInfo(List objects) {
        Renderable r = null;
        String localization = null;
        for (Iterator it = objects.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            localization = (String) r.getAttributeValue(RenderablePropertyNames.LOCALIZATION);
            if (localization != null)
                r.setLocalization(localization);
        }
    }
    
    private void removeCompartmentFromName() {
        GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
        List objects = graphPane.getDisplayedObjects();
        boolean needUpdate = removeCompartmentFromName(objects);
        if (!needUpdate)
            return;
        // Need to repaint
        graphPane.repaint(graphPane.getVisibleRect());
        // A simple way to make property trees correct
        Project project = editorFrame.getEditorManager().getOpenedProject();
        toolPane.getPropertyView().openProject(project);
        toolPane.getGraphicView().getComponentTree().open(project.getProcess());
        editorFrame.enableSaveAction(true);
    }

    protected Action getEditPropertiesAction() {
        Action editPropertiesAction = new AbstractAction("Edit Properties") {
            public void actionPerformed(ActionEvent e) {
                if (toolPane != null)
                    toolPane.selectPropertyView();
            }
        };
        return editPropertiesAction;
    }
    
    /**
     * A helper method to remove compartment from name.
     * @param objects
     * @return
     */
    public boolean removeCompartmentFromName(List objects) {
        // Make sure this action will work only on Nodes.
        // Names of reactions will not be displayed so it is not
        // necessary to remove them. Another problem is that the following
        // implementation cannot work for some reactions that use inputs names
        // for reaction display names since [] are used.
        // Make a copy
        objects = new ArrayList(objects);
        for (Iterator it = objects.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableReaction)
                it.remove();
        }
        Renderable r = null;
        int index;
        String oldName = null;
        // The first iteration is to keep the compartment name so that 
        // they can be recovered if needed
        recordCompartmentInfo(objects);
        RenderableRegistry registry = RenderableRegistry.getRegistry();
        boolean needUpdate = false;
        for (Iterator it = objects.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            oldName = r.getDisplayName();
            if (oldName.endsWith("]")) {
                index = oldName.indexOf("[");
                if (index < 0)
                    continue; // No compartment
                String newName = oldName.substring(0, index).trim();
                // Need to check if this newName has been registered already
                if(registry.contains(newName)) {
                    // Need to link as shortcut
                    Renderable target = registry.getSingleObject(newName);
                    mergeRenderables(target, r);
                }
                else {
                    r.setDisplayName(newName);
                    RenderableRegistry.getRegistry().changeName(r, oldName);
                }
                needUpdate = true;
            }
        }
        return needUpdate;
    }
    
    /**
     * Merge two sets of Renderable objects together.
     * @param target the Renderable object should be kept
     * @param source the object will be merged away.
     */
    private void mergeRenderables(Renderable target,
                                  Renderable source) {
        String oldName = source.getDisplayName();
        // Check target if there is any shortcuts there
        List<Renderable> targetShortcuts = target.getShortcuts();
        if (targetShortcuts == null) {
            targetShortcuts = new ArrayList<Renderable>();
            target.setShortcuts(targetShortcuts);
            targetShortcuts.add(target);
        }
        List<Renderable> sourceShortcuts = source.getShortcuts();
        if (sourceShortcuts != null && sourceShortcuts.size() > 0)
            targetShortcuts.addAll(sourceShortcuts);
        else 
            targetShortcuts.add(source);
        source.setShortcuts(targetShortcuts);
        // Handle attributes
        Map attributes = target.getAttributes();
        // Don't need to reset the name since it should be fetched from attributes.
        // However, to make name display correct, this method is called
        source.setDisplayName(target.getDisplayName());
        source.setAttributes(attributes);
        // Remove register
        RenderableRegistry.getRegistry().unregister(oldName);
    }
    
    public Action getSaveCoordinatesToDbAction() {
        if (saveCoordinatesToDbAction == null) {
            saveCoordinatesToDbAction = new AbstractAction("Store Coordinates to Db...") {
                public void actionPerformed(ActionEvent e) {
                    storeCoordinatesToDb();
                }
            };
        }
        return saveCoordinatesToDbAction;
    }
    
    private void storeCoordinatesToDb() {
        Thread t = new Thread() {
            public void run() {
                CoordinateSerializer serializer = new CoordinateSerializer();
                Project project = editorFrame.getEditorManager().getOpenedProject();
                ProgressPane progressPane = new ProgressPane();
                progressPane.setIndeterminate(true);
                editorFrame.setGlassPane(progressPane);
                progressPane.setText("Storing coordinates to database...");
                editorFrame.getGlassPane().setVisible(true);
                try {
                    // Get the preferred size
                    GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
                    Dimension size = graphPane.getPreferredSize();
                    int width = (int)(size.width / graphPane.getScaleX());
                    int height = (int)(size.height / graphPane.getScaleY());
                    serializer.storeCoordinates(project,
                                                width,
                                                height,
                                                editorFrame);
                    JOptionPane.showMessageDialog(editorFrame,
                                                  "Coordinates have been stored into the databse successfully.",
                                                  "Dumping Coordinates",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
                catch(Exception e1) {
                    System.err.println("AuthorToolActionCollection.getSaveCoordinatesToDbAction(): " +
                                       e1.getMessage());
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(editorFrame,
                                                  "Error in dumping coordinates: \n" + e1.getMessage(),
                                                  "Error in Coordinate Dumping",
                                                  JOptionPane.ERROR_MESSAGE);
                }
                editorFrame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    public Action getFlagChangedAction() {
        if (flagChangedAction == null) {
            flagChangedAction = new AbstractAction("Flag Changed Objects") {
                public void actionPerformed(ActionEvent e) {
                    String text = (String) flagChangedAction.getValue(Action.NAME);
                    if (text.startsWith("Flag")) {
                        toolPane.showIsChangedInTrees(true);
                        flagChangedAction.putValue(Action.NAME,
                                                   "Unflag Changed Objects");
                    }
                    else if (text.startsWith("Unflag")) {
                        toolPane.showIsChangedInTrees(false);
                        flagChangedAction.putValue(Action.NAME,
                                                  "Flag Changed Objects");
                    }
                }
            };
        }
        return flagChangedAction;
    }
    
    public Action getSelectAllAction() {
        if (selectAllAction == null) {
            selectAllAction = new AbstractAction("Select All") {
                public void actionPerformed(ActionEvent e) {
                    GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
                    graphPane.selectAll();
                }
            };
        }
        return selectAllAction;
    }
    
    public Action getExportDiagramAction() {
        if (exportDiagramAction == null) {
            exportDiagramAction = new AbstractAction("Export Diagram") {
                public void actionPerformed(ActionEvent e) {
                    exportPathwayDiagram();
                }
            };
        }
        return exportDiagramAction;
    }
    
    private void exportPathwayDiagram() {
        // Get the file name
        try {
            pathwayEditor.exportDiagram();
        }
        catch(IOException e) {
            System.err.println("AuthorToolActionCollection.exportPathwayImage(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(pathwayEditor,
                                          "Pathway diagram cannot be exported: " + e,
                                          "Error in Diagram Export",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public Action getSwitchViewAction() {
        if (switchViewAction == null) {
            switchViewAction = new AbstractAction("Switch View") {
                public void actionPerformed(ActionEvent e) {
                    toolPane.switchView();
                }
            };
        }
        return switchViewAction;
    }
    
    public Action getOptionAction() {
        if (optionAction == null) {
            optionAction = new AbstractAction("Options") {
                public void actionPerformed(ActionEvent e) {
                    OptionDialog dialog = new OptionDialog(editorFrame);
                    dialog.setSize(360, 305);
                    dialog.setLocationRelativeTo(toolPane);
                    dialog.setTitle("Options");
                    dialog.setModal(true);
                    dialog.setVisible(true);
                }
            };
        }
        return optionAction;
    }
    
    public Action getAboutAction() {
        if (aboutAction == null) {
            aboutAction = new AbstractAction("About",
                                             AuthorToolAppletUtilities.createImageIcon("About16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    AboutGKPane gkPane = new AboutGKPane();
                    gkPane.setApplicationTitle(GKEditorFrame.GK_EDITOR_NAME);
                    gkPane.setBuildNumber(GKEditorFrame.BUILD_NUMBER);
                    gkPane.setVersion(GKEditorFrame.VERSION);
                    gkPane.displayInDialog((Frame)SwingUtilities.getRoot(toolPane));
                }
            };
            aboutAction.putValue(Action.SHORT_DESCRIPTION, "About Reactome");
        }
        return aboutAction;
    }
    
    public Action getSearchDBAction() {
        if (searchDBAction == null) {
            searchDBAction = new AbstractAction("Search DB",
                                                AuthorToolAppletUtilities.createImageIcon("Database.gif")) {
                public void actionPerformed(ActionEvent e) {
                    searchDB();
                }
            };
            searchDBAction.putValue(Action.SHORT_DESCRIPTION, "Search Reactome Database");
        }
        return searchDBAction;
    }
    
    private void searchDB() {
        final SearchDatabasePane searchPane = new SearchDatabasePane();
        final JDialog dialog = GKApplicationUtilities.createDialog(toolPane, 
                                                                   "Search Reactome Database");
        dialog.getContentPane().add(searchPane, BorderLayout.CENTER);
        searchPane.addCancelAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        searchPane.addOKAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                List selected = searchPane.getSelectedObjects();
                addSelectedDBInstances(selected);
                dialog.dispose();
            }
        });
        if (!searchPane.initDatabaseConnection()) {
            JOptionPane.showMessageDialog(toolPane,
                                          "Cannot initialize a database connection.",
                                          "Error in Search Database",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Focus the name text field when the dialog is opened
        dialog.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                searchPane.focusTextField();
            }
        });
        dialog.pack();
        dialog.setModal(true);
        GKApplicationUtilities.center(dialog);
        dialog.setVisible(true);
    }
    
    private void addSelectedDBInstances(List instances) {
        if (instances == null || instances.size() == 0)
            return; // Don't do anything
        // Want to take advantage of GraphEditorTransferHandler
        ArrayList list = new ArrayList();
        list.add("dbBrowser");
        list.addAll(instances);
        try {
            GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
            GraphEditorTransferHandler handler = (GraphEditorTransferHandler) graphPane.getTransferHandler();
            handler.importListOfRenderables(list, graphPane);
        }
        catch(Exception e) {
            System.err.println("AuthorToolActionCollection.addSelectedDBInstances(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(toolPane,
                                          "Cannot add instances from the database: " + e.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public Action getSaveAsAction() {
        if (saveAsAction == null) {
            saveAsAction = new AbstractAction("Save Project As...",
                                              AuthorToolAppletUtilities.createImageIcon("SaveAs16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    editorFrame.getEditorManager().saveAs();
                }
            };
            saveAsAction.putValue(Action.SHORT_DESCRIPTION, "Save Project As...");
        }
        return saveAsAction;
    }
    
    public Action getSaveProjectAction() {
        if (saveProjectAction == null) {
            saveProjectAction = new AbstractAction("Save Project",
                                                   AuthorToolAppletUtilities.createImageIcon("Save16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    editorFrame.getEditorManager().save();
                }                                      
            };  
            saveProjectAction.putValue(Action.SHORT_DESCRIPTION, "Save Project");
        }
        return saveProjectAction;
    }
    
    public Action getOpenProjectAction() {
        if (openProjectAction == null) {
            openProjectAction = new AbstractAction("Open Project",
                                                   AuthorToolAppletUtilities.createImageIcon("Open16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    editorFrame.getEditorManager().open();
                }
            };
            openProjectAction.putValue(Action.SHORT_DESCRIPTION, "Open Project");
        }
        return openProjectAction;
    }
    
    public Action getNewProjectAction() {
        if (newProjectAction == null) {
            newProjectAction = new AbstractAction("New Project",
                                                  AuthorToolAppletUtilities.createImageIcon("New16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    editorFrame.getEditorManager().createNewProject();
                }
            };
            newProjectAction.putValue(Action.SHORT_DESCRIPTION, "New Project");
        }
        return newProjectAction;
    }
    
    public Action getHelpAction() {
        if (helpAction == null) {
            helpAction = new AbstractAction("Help",
                                            AuthorToolAppletUtilities.createImageIcon("Help16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    if (AuthorToolAppletUtilities.isInApplet) {
                        URL baseUrl = AuthorToolAppletUtilities.getCodeBase();
                        try {
                            String fileName = "docs" + File.separator + "AuthorToolHelp.html";
                            URL helpUrl = new URL(baseUrl, fileName);
                            AuthorToolAppletUtilities.displayURL(helpUrl.toExternalForm(), "docs");
                        }
                        catch(MalformedURLException e1) {
                            System.err.println("AuthorToolActionCollection.getHelpAction(): " + e1);
                            e1.printStackTrace();
                        }
                    }
                    else {
                        try {
                            String fileName = "AuthorToolHelp.html";
                            AuthorToolAppletUtilities.displayHelp(fileName, 
                                                                  toolPane);
                        }
                        catch(Exception e1) {
                            System.err.println("GKActionCollection.getHelpAction(): " + e1);
                            e1.printStackTrace();
                        }
                    }
                }
            };
        }
        return helpAction;
    }

    public Action getSearchTreeAction() {
        if (searchTreeAction == null) {
            searchTreeAction = new AbstractAction("Find",
                    AuthorToolAppletUtilities.createImageIcon("Search16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    searchTree();
                }
            };
            searchTreeAction.putValue(Action.SHORT_DESCRIPTION, "Find object");
        }
        return searchTreeAction;
    }
    
    private void searchTree() {
        TextSearchPane searchPane = new TextSearchPane();
        if (searchPane.showSearchDialog(toolPane, 
                                        "Search Object")) {
            String searchKey = searchPane.getSearchKey();
            boolean isWholeNameMatch = searchPane.isWholeNameOnly();
            boolean isCaseSensitive = searchPane.isCaseSensitive();
            java.util.List list = toolPane.search(searchKey, isWholeNameMatch, isCaseSensitive);
            if (list == null || list.size() == 0) {
                JOptionPane.showMessageDialog(toolPane,
                                              "No instance with the specified name.",
                                              "Empty Search Result",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
    
    public Action getCutAction() {
        if (cutAction == null) {
            cutAction = new AbstractAction("Cut", 
                                 AuthorToolAppletUtilities.createImageIcon("Cut16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    cut();
                }
            };
            cutAction.putValue(Action.SHORT_DESCRIPTION, "Cut"); 
        }
        return cutAction;   
    }
    
    protected void cut() {
        pathwayEditor.cut();
        updateSelectRelatedAction();
        enableSave();
    }
    
    public Action getCopyAction() {
        if (copyAction == null) {
            copyAction = new AbstractAction("Copy",
                                 AuthorToolAppletUtilities.createImageIcon("Copy16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.copy();
                    pasteAsAliasAction.setEnabled(true);
                    //pasteAsNewInstanceAction.setEnabled(true);
                }
            };
            copyAction.putValue(Action.SHORT_DESCRIPTION, "Copy");
        }
        return copyAction;
    }
    

    protected void cloneSelection() {
        pathwayEditor.cloneInstances();
        enableSave();
    }
    
    public Action getCloneAction() {
        if (cloneAction == null) {
            cloneAction = new AbstractAction("Clone",
                                           AuthorToolAppletUtilities.createImageIcon("PasteAsNewInstance.gif")) {
                public void actionPerformed(ActionEvent e) {
                    cloneSelection();
                }

            };
            cloneAction.putValue(Action.SHORT_DESCRIPTION, "Clone selected objects");
        }
        return cloneAction;
    }
    
    public Action getPasteAsAliasAction() {
        if (pasteAsAliasAction == null) {
            pasteAsAliasAction = new AbstractAction("Paste", //"Paste as Alias",
                                AuthorToolAppletUtilities.createImageIcon("Paste16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.pasteAsAliase();
                    enableSave();
                }
            };
            pasteAsAliasAction.putValue(Action.SHORT_DESCRIPTION, "Paste as shortcuts");
        }
        return pasteAsAliasAction;
    }
    
    protected void enableSave() {
        if (toolPane != null)
            toolPane.enableSaveAction(true);
    }
    
    public Action getLayoutEdgesAction() {
        if (layoutEdgesAction == null) {
            layoutEdgesAction = new AbstractAction("Layout Edges",
                                                   AuthorToolAppletUtilities.createImageIcon("EdgeLayout.gif")) {
                public void actionPerformed(ActionEvent e) {
                    layoutEdges();
                }
            };
            layoutEdgesAction.putValue(Action.SHORT_DESCRIPTION, "Layout edges");
        }
        return layoutEdgesAction;
    }
    
    private void layoutEdges() {
        // For undo/redo
        List selection = pathwayEditor.getSelection();
        List<HyperEdge> edges = new ArrayList<HyperEdge>();
        // layout all edges
        if (selection == null || selection.size() == 0) {
            for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
                Object obj = it.next();
                if (obj instanceof HyperEdge)
                    edges.add((HyperEdge)obj);
            }
        }
        else {
            for (Object obj : selection) {
                if (obj instanceof HyperEdge)
                    edges.add((HyperEdge)obj);
            }
        }
        if (edges.size() == 0)
            return;
        UndoableEdgeLayoutEdit edit = new UndoableEdgeLayoutEdit(pathwayEditor, edges);
        pathwayEditor.layoutEdges();
        pathwayEditor.addUndoableEdit(edit);
        if (toolPane != null) {
            if (toolPane.getDisplayedView() instanceof PropertyEditorView)
                ((PropertyEditorView)toolPane.getDisplayedView()).repaintOverview();
        }
        enableSave();
    }

    public Action getLayoutAction() {
        if (layoutAction == null) {
            layoutAction = new AbstractAction("Automatic Layout",
                                              AuthorToolAppletUtilities.createImageIcon("LayeredLayout.gif")) {
                public void actionPerformed(ActionEvent e) {
                    layout();
                }
            };       
            layoutAction.putValue(Action.SHORT_DESCRIPTION, "Hierarchically layout the pathway");                         
        }
        return layoutAction;
    }
    
    private void layout() {
        // Check if a warning is needed: need a warning if there is a compartment
        boolean needWarning = false;
        for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
            if (it.next() instanceof RenderableCompartment) {
                needWarning = true;
                break;
            }
        }
        if (needWarning) {
            int reply = JOptionPane.showConfirmDialog(pathwayEditor,
                                                      "The current auto-layout implementation will not honor compartment setting.\n" + 
                                                      "Do you still want to do auto-layout?", 
                                                      "Auto-Layout?", 
                                                      JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
        }
        UndoableLayoutEdit edit = new UndoableLayoutEdit(pathwayEditor);
        pathwayEditor.layoutRenderable();
        pathwayEditor.addUndoableEdit(edit);
        if (toolPane != null &&
            toolPane.getDisplayedView() instanceof PropertyEditorView) {
            PropertyEditorView view = (PropertyEditorView) toolPane.getDisplayedView();
            view.repaintOverview();
        }
        enableSave();
    }
    
    private Node getSingleSelectedNode() {
        GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
        if (graphPane == null)
            return null;
        java.util.List selection = graphPane.getSelection();
        if (selection.size() > 1)
            return null;
        Renderable r = (Renderable) selection.get(0);
        if (r instanceof Node)
            return (Node)r;
        return null;
    }
    
    private HyperEdge getSingleSelectedEdge() {
        GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
        if (graphPane == null)
            return null;
        java.util.List selection = graphPane.getSelection();
        if (selection.size() > 1)
            return null;
        Renderable r = (Renderable) selection.get(0);
        if (r instanceof HyperEdge)
            return (HyperEdge)r;
        return null;
    }
    
    public Action getRenameAction() {
        if (renameAction == null) {
            renameAction = new AbstractAction("Rename") {
                public void actionPerformed(ActionEvent e) {
                    java.util.List selection = pathwayEditor.getSelection();
                    if (selection.size() == 1) {
                        Renderable renderable = (Renderable) selection.get(0);
                        // Only these types can be used for on-line editing
                        if (isEditable(renderable)) {
                            pathwayEditor.setIsEditing(true);
                            pathwayEditor.setEditingNode((Node)renderable);
                            // bounds of renderable might be not good in a scaled GraphEditorPane
                            //graphPane.repaint(renderable.getBounds());
                            pathwayEditor.repaint(pathwayEditor.getVisibleRect());
                        }
                        else
                            rename(renderable, pathwayEditor);
                    }
                }
            };
        }
        return renameAction;
    }
    
    private void rename(Renderable r,
                        GraphEditorPane graphPane) {
        // Use a simple dialog to rename a Renderable dialog
        String name = r.getDisplayName();
        String message = "Please enter a new name:";
        graphPane.rename(r, 
                         message, 
                         name, 
                         name);
    }
    
    private boolean isEditable(Renderable r) {
        if (r instanceof Node) {
            if (r instanceof RenderablePathway)
                return false;
            // As of June 16, 2008, all RenderableComplex should be
            // editable.
//            if (r instanceof RenderableComplex) {
//                if (((RenderableComplex)r).isComponentsHidden() ||
//                    r.getComponents() == null ||
//                    r.getComponents().size() == 0)
//                    return true;
//                else
//                    return false;
//            }
            return true;
        }
        return false;
    }
    
    public Action getDeleteAction() {
        if (deleteAction == null) {
            deleteAction = new AbstractAction("Delete",
                                AuthorToolAppletUtilities.createImageIcon("Delete16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    delete();
                }

            };
            deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete");
        }
        return deleteAction;
    }
    
    protected void delete() {
        java.util.List selection = pathwayEditor.getSelection();
        if (selection == null || selection.size() == 0)
            return;
        String msg =
            "Are you sure you want to delete the selected object"
            + ((selection.size() == 1) ? "?" : "s?");
        int reply =
            JOptionPane.showConfirmDialog(pathwayEditor, // Use toolPane to make the focus correctly. Otherwise, the focus might be shifted to panel
                                          msg,
                                          "Delete Confirmation",
                                          JOptionPane.YES_NO_CANCEL_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return;
        UndoableDeleteEdit edit = new UndoableDeleteEdit(pathwayEditor, new ArrayList<Renderable>(selection));
        pathwayEditor.deleteSelection();
        pathwayEditor.addUndoableEdit(edit);
        updateSelectRelatedAction();
    }
    
    private JMenuItem generateSetStoiItemForComplex(final RenderableComplex complex,
                                                    final GraphEditorPane editor) {
        JMenuItem item = null;
        List selection = editor.getSelection();
        if (selection != null && selection.size() == 1) {
            final Renderable node = (Renderable) selection.get(0);
            item = new JMenuItem("Set Stoichiometry");
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int oldStoi = complex.getStoichiometry(node);
                    String stoiStr = JOptionPane.showInputDialog(editor,
                            "Please input a positive integer for stoichiometry:",
                            new Integer(oldStoi));
                    if (stoiStr == null || stoiStr.trim().length() == 0)
                        return; // Do Nothing
                    try {
                        int stoi = Integer.parseInt(stoiStr);
                        if (stoi == oldStoi)
                            return;
                        complex.setStoichiometry(node, stoi);
                        if (!complex.isChanged()) {
                            complex.setIsChanged(true);
                            //applet.getEditorManager().updateIsChanged(complex);
                        }
                        toolPane.enableSaveAction(true);
                        editor.repaint(editor.getVisibleRect());
                    }
                    catch(NumberFormatException e1) {
                        JOptionPane.showMessageDialog(editor,
                                "Use a positive integer as stoichiometry. Please try again.",
                                "Error in Setting Stoichiometry",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }
        return item;
    }
    
    private JMenuItem generateSetStoiItemForReaction(final RenderableReaction reaction,
                                                     final GraphEditorPane editor) {
        JMenuItem item = null;
        // Check if a Node is selected
        List selection = editor.getSelection();
        if (selection != null && selection.size() == 1) {
            if (selection.get(0) instanceof Node) {
                final Renderable node = (Renderable) selection.get(0);
                final List inputs = reaction.getInputNodes();
                final List outputs = reaction.getOutputNodes();
                if (inputs.contains(node) || outputs.contains(node)) {
                    item = new JMenuItem("Set Stoichiometry");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            int oldStoi = reaction.getStoichiometry(node);
                            String stoiStr = JOptionPane.showInputDialog(editor,
                                    "Please input a positive integer for stoichiometry:",
                                    oldStoi + "");
                            if (stoiStr == null || stoiStr.trim().length() == 0)
                                return; // Do Nothing
                            try {
                                int stoi = Integer.parseInt(stoiStr);
                                if (oldStoi == stoi)
                                    return; // Nothing new
                                if (inputs.contains(node))
                                    reaction.setInputStoichiometry(node, stoi);
                                else
                                    reaction.setOutputStoichiometry(node, stoi);
                                toolPane.enableSaveAction(true);
                                editor.repaint(editor.getVisibleRect());
                            }
                            catch(NumberFormatException e1) {
                                JOptionPane.showMessageDialog(editor,
                                        "Use a positive integer as stoichiometry. Please try again.",
                                        "Error in Setting Stoichiometry",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    });
                }
            }
        }
        return item;
    }
    
    public void updateActions() {
        Project project = editorFrame.getEditorManager().getOpenedProject();
        if (project == null) {
            saveProjectAction.setEnabled(false);
            saveAsAction.setEnabled(false);
            searchTreeAction.setEnabled(false);
        }
        else {
            saveAsAction.setEnabled(true);
            searchTreeAction.setEnabled(true);
        }
        GraphEditorPane graphPane = toolPane.getDisplayedGraphPane();
        if (graphPane == null ||
            toolPane.getDisplayedView() instanceof PropertyEditorView) {
            cutAction.setEnabled(false);
            copyAction.setEnabled(false);
            pasteAsAliasAction.setEnabled(false);
            cloneAction.setEnabled(false);
            deleteAction.setEnabled(false);
        }
        if (graphPane != null) {
            layoutAction.setEnabled(true);
            if (graphPane instanceof PathwayEditor) {
                layoutAction.putValue(Action.SHORT_DESCRIPTION, "Hierarchically layout the pathway");
            }
            else if (graphPane instanceof ReactionNodeGraphEditor) {
                layoutAction.putValue(Action.SHORT_DESCRIPTION, "Layout reaction automatically");
            }
            else if (graphPane instanceof ComplexGraphEditor) {
                layoutAction.putValue(Action.SHORT_DESCRIPTION, "Layout complex automatically");
            }
        }
        updateUndoRedoActions();
        updateSelectRelatedAction();
    }
    
    public void updateUndoRedoActions() {
        GraphEditorPane graphPane = pathwayEditor;
        if (graphPane == null ||
            (toolPane != null && toolPane.getDisplayedView() instanceof PropertyEditorView)) {
            undoAction.setEnabled(false);
            redoAction.setEnabled(false);
        }
        if (graphPane != null) {
            undoAction.setEnabled(graphPane.canUndo());
            redoAction.setEnabled(graphPane.canRedo());
        }
    }
    
    protected void updateSelectRelatedAction() {
        // Check GraphEditorPane first
        if (pathwayEditor != null &&
            (toolPane == null || toolPane.getDisplayedView() instanceof GraphicEditorView)) { // Validate these actions only under graphic view
            java.util.List selection = pathwayEditor.getSelection();
            int size = selection.size();
            //validateDisplayFormatAction(selection);
            if (size > 0) {
                boolean isPastable = isTransferrable(selection);
                if (isPastable) {
                    cutAction.setEnabled(true);
                    copyAction.setEnabled(true);
                    cloneAction.setEnabled(true);
                }
                else {
                    cutAction.setEnabled(false);
                    copyAction.setEnabled(false);
                    cloneAction.setEnabled(false);
                }
                pasteAsAliasAction.setEnabled(false);
                deleteAction.setEnabled(true);
            }
            else {
                deleteAction.setEnabled(false);
                cloneAction.setEnabled(false);
                cutAction.setEnabled(false);
                copyAction.setEnabled(false);
                Clipboard clipboard = getClipboard(pathwayEditor);
                if (clipboard != null) {
                    Transferable transferable = clipboard.getContents(this);
                    TransferHandler transferHandler = pathwayEditor.getTransferHandler();
                    if (transferHandler.canImport(pathwayEditor, 
                                                  transferable.getTransferDataFlavors())) {
                        pasteAsAliasAction.setEnabled(true);
                    }
                    else {
                        pasteAsAliasAction.setEnabled(false);
                    }
                }
                else {
                    // Enable anywhere since no where to know the data
                    //TODO:  This can be implemented by using a new local clipboard
                    pasteAsAliasAction.setEnabled(true);
                }
            }
        }
    }
    
    boolean isTransferrable(List selection) {
        // Make sure all can be transferred
        Renderable r = null;
        for (Iterator it = selection.iterator(); it.hasNext();) {
            r = (Renderable) it.next();
            if (!r.isTransferrable())
                return false;
        }
        return true;
    }

    /**
     * Returns the clipboard to use for cut/copy/paste. This method is copied
     * from javax.swing.TansferHandler.
     */
    private Clipboard getClipboard(JComponent c) {
        Clipboard clipboard = null;
        try {
            clipboard = c.getToolkit().getSystemClipboard();
        }
        catch(Exception e) {
            // Just a try: it will fail in the applet.
            //e.printStackTrace();
        }
        return clipboard;
    }
    
    private class TogglePathwayAction extends AbstractAction {
        private boolean isVisible = false;
        private Icon hidePathwayIcon = AuthorToolAppletUtilities.createImageIcon("HidePathway.png");
        private Icon showPathwayIcon = AuthorToolAppletUtilities.createImageIcon("ShowPathway.png");
        
        public TogglePathwayAction() {
            super("Draw Pathway");
            putValue(Action.SHORT_DESCRIPTION, 
                     "Toggle pathway drawing");
            // Need to figure out the initialize value
            String drawPathway = systemProperties.getProperty("drawPathway");
            if (drawPathway == null || drawPathway.equals("false")) {
                isVisible = false;
            }
            else
                isVisible = true;
            toggleSelection();
        }
        
        public void actionPerformed(ActionEvent e) {
            isVisible = !isVisible;
            toggleSelection();
        }
        
        private void toggleSelection() {
            if (isVisible)
                putValue(Action.SMALL_ICON, showPathwayIcon);
            else
                putValue(Action.SMALL_ICON, hidePathwayIcon);
            pathwayEditor.setPathwayVisible(isVisible);
            systemProperties.setProperty("drawPathway", isVisible + "");
        }
        
        void setPathwayVisible(boolean isVisible) {
            if (this.isVisible != isVisible) {
                this.isVisible = isVisible;
                toggleSelection();
            }
        }
    }
    
    public Action getUndoAction() {
        if (undoAction == null) {
            undoAction = new AbstractAction("Undo",
                                            AuthorToolAppletUtilities.createImageIcon("Undo16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.undo();
                }
            };
            undoAction.putValue(Action.SHORT_DESCRIPTION,
                                "undo");
        }
        return undoAction;
    }
    
    public Action getRedoAction() {
        if (redoAction == null) {
            redoAction = new AbstractAction("Redo",
                                            AuthorToolAppletUtilities.createImageIcon("Redo16.gif")) {
                public void actionPerformed(ActionEvent e) {
                    pathwayEditor.redo();
                }
            };
            redoAction.putValue(Action.SHORT_DESCRIPTION,
                                "redo");
        }
        return redoAction;
    }

    protected boolean isShowComponentActionNeeded(RenderableComplex complex) {
        return complex.getComponents() != null &&
               complex.getComponents().size() > 0;
    }

    protected void hideComponents(boolean isHidden) {
        List selection = pathwayEditor.getSelection();
        for (Iterator it = selection.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof RenderableComplex) {
                RenderableComplex complex = (RenderableComplex) obj;
                complex.hideComponents(isHidden);
                complex.invalidateBounds();
                complex.invalidateConnectWidgets();
            }
        }
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        enableSave();
    }

    public PathwayEditor getPathwayEditor() {
        return this.pathwayEditor;
    }
}