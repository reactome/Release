/*
 * Created on Oct 31, 2008
 *
 */
package org.gk.elv;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.gk.database.AttributeEditEvent;
import org.gk.database.AttributePane;
import org.gk.database.MultipleAttributeSearchPane;
import org.gk.database.SearchPane;
import org.gk.gkEditor.PathwayOverviewPane;
import org.gk.graphEditor.GraphEditorActionListener;
import org.gk.graphEditor.GraphEditorUndoManager;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;

/**
 * This entity level is based on the Author Tool version 4.0, a version based on SBGN. Basically,
 * it merged the pathway editor view onto the PropertyEditor and with only one left panel as the
 * index view. The overview has been replaced by the one in the graphic editor. Property editor 
 * has been redesigned to meet the requirement in the CT data model. The original Entity list has 
 * been made in a hierarchical way: ReferenceEntities have been listed as the top.
 * @author wgm
 *
 */
public class EntityLevelView extends JPanel {
    // GUIs
    private PathwayOverviewPane overviewPane;
    private SearchPane searchPane;
    private MultipleAttributeSearchPane multSearchPane;
    private InstanceTreeListPane objectListPane;
    private InstanceZoomablePathwayEditor zoomableEditor;
    // Used to display attributes
    private AttributePane attributePane;
    // Used to synchronize selection
    private SelectionMediator selectionMediator;
    // For actions
    private ElvActionCollection actionCollection;
    private ElvDiagramHandler diagramHandler;
    // For layout
    private JSplitPane leftJsp;
    private JSplitPane centerJsp;
    
    public EntityLevelView() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        objectListPane = new InstanceTreeListPane();
        //JPanel overviewPane = createOverView();
        JTabbedPane overviewAndSearchPane = createOverViewAndSearchPane();
        leftJsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                            objectListPane,
                                            overviewAndSearchPane);
        JTabbedPane rightPane = createRightPane();
        centerJsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                              leftJsp,
                                              rightPane);
        add(centerJsp, BorderLayout.CENTER);
        centerJsp.setDividerLocation(0.3d);
        centerJsp.setDividerLocation(300);
        installListeners();
        addToolbarToPathwayEditor();
        SimpleInstanceTransferHandler transferHandler = new SimpleInstanceTransferHandler();
        zoomableEditor.getPathwayEditor().setTransferHandler(transferHandler);
        objectListPane.setTransferHandler(transferHandler);
        diagramHandler = new ElvDiagramHandler();
        GraphEditorUndoManager undoManager = zoomableEditor.getPathwayEditor().getUndoManager();
        undoManager.addUndoableEditListener(new UndoableEditListener() {

            public void undoableEditHappened(UndoableEditEvent e) {
                actionCollection.updateUndoRedoActions();
            }
            
        });
    }
    
    public void storeSystemProperties(Properties properties) {
        properties.setProperty("elv.centerJsp.location", 
                               centerJsp.getDividerLocation() + "");
        properties.setProperty("elv.leftJsp.location",
                               leftJsp.getDividerLocation() + "");
        // Selected species
        String selectedSpecies = objectListPane.getEventPane().getSelectedSpecies();
        properties.setProperty("elv.selectedSpecies",
                               selectedSpecies);
    }
    
    public InstanceZoomablePathwayEditor getZoomablePathwayEditor() {
        return this.zoomableEditor;
    }
    
    public InstanceTreeListPane getTreePane() {
        return this.objectListPane;
    }
    
    private void addToolbarToPathwayEditor() {
        actionCollection = new ElvActionCollection(this);
        JToolBar toolbar = actionCollection.createToolbar();
        zoomableEditor.installToolbar(toolbar);
        Action openDiagramAction = actionCollection.getOpenDiagramAction();
        objectListPane.getEventPane().addAdditionalPopupAction(openDiagramAction);
        objectListPane.getEventPane().addAdditionalPopupAction(actionCollection.getEncapsulateDiagramAction());
    }
    
    public void setUseAsDrawingTool(boolean value) {
        zoomableEditor.setUsedAsDrawingTool(value);
        actionCollection.setUsedAsDrawingTool(value);
    }
    
    public boolean isUsedAsDrawingTool() {
        return this.zoomableEditor.isUsedAsDrawingTool();
    }
    
    public void setSystemProperties(Properties properties) {
        actionCollection.setSystemProperties(properties);
        String value = properties.getProperty("elv.centerJsp.location");
        if (value != null) {
            centerJsp.setDividerLocation(Integer.parseInt(value));
        }
        value = properties.getProperty("elv.leftJsp.location");
        if (value != null)
            leftJsp.setDividerLocation(Integer.parseInt(value));
        value = properties.getProperty("elv.selectedSpecies");
        if (value != null)
            objectListPane.getEventPane().setSelectedSpecies(value);
    }
    
    public void addGraphEditorActionListener(GraphEditorActionListener l) {
        zoomableEditor.getPathwayEditor().addGraphEditorActionListener(l);
    }
    
    /**
     * This method is used to set up all dynamic things.
     */
    private void installListeners() {
        TreeSelectionListener eventListener = new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                try {
                    GKInstance event = objectListPane.getSelectedEvent();
                    selectEvent(event);
                }
                catch(Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
        objectListPane.addEventSelectionListener(eventListener);
        overviewPane.syncrhonizeScroll(zoomableEditor);
        overviewPane.setParentEditor(zoomableEditor.getPathwayEditor());
        // Handle selection
        selectionMediator = new SelectionMediator();
        objectListPane.setSelectionMediator(selectionMediator);
        // Display property
        Selectable selectable = new Selectable() {
            public List getSelection() {
                return null;
            }

            public void setSelection(List selection) {
                displayProperty(selection);
            }
        };
        selectionMediator.addSelectable(selectable);
        zoomableEditor.setSelectionMediator(selectionMediator);
    }
    
    public void initFileAdptor(XMLFileAdaptor fileAdaptor) {
     // For refresh diagram
        fileAdaptor.addInstanceListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if (name.equals("updateDiagram")) {
                    GKInstance pdInstance = (GKInstance) evt.getOldValue();
                    refreshEvent(pdInstance);
                }
            }
        });
        resetSearchPane(); // the initial state
    }
    
    /**
     * Display the property for the first GKInstance in the provided list.
     * @param list
     */
    private void displayProperty(List list) {
        GKInstance instance = null;
        for (Iterator it = list.iterator(); it.hasNext();) {
            Object obj = it.next();
            if (obj instanceof GKInstance) {
                instance = (GKInstance) obj;
                break;
            }
        }
        if (instance != null) {
            attributePane.setInstance(instance);
        }
    }
    
    private void selectEvent(GKInstance event) throws Exception {
        if (event == null)
            return;
        if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            List<GKInstance> containers = zoomableEditor.getDisplayedPathways();
            if (containers == null || containers.size() == 0) {
                actionCollection.getEncapsulateDiagramAction().setEnabled(false);
                actionCollection.getOpenDiagramAction().setEnabled(true);
            }
            else if (containers.contains(event)) { 
                actionCollection.getEncapsulateDiagramAction().setEnabled(false);
                actionCollection.getOpenDiagramAction().setEnabled(false);
            }
            else {
                Set<GKInstance> contained = InstanceUtilities.getContainedEvents(containers);
                actionCollection.getEncapsulateDiagramAction().setEnabled(contained.contains(event));
                actionCollection.getOpenDiagramAction().setEnabled(true);
            }
        }
        else { // Default: these two actions should be disabled.
            actionCollection.getOpenDiagramAction().setEnabled(false);
            actionCollection.getEncapsulateDiagramAction().setEnabled(false);
        }
    }
    
    /**
     * This method is used to display a selected event. If the passed event is a Reaction,
     * it will be selected in a pathway diagram.
     * @param event
     */
    public void displayEvent(GKInstance event) throws Exception {
        displayEvent(event, false);
    }
    
    private void displayEvent(GKInstance event, boolean needRefresh) throws Exception {
        if (event == null)
            return;
        // Get the displayed GKInstance
        if (zoomableEditor.getDisplayedPathways().contains(event) && !needRefresh)
            return;
        if (event.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
            RenderablePathway diagram = diagramHandler.setDiagramForDisplay(event, 
                                                                            zoomableEditor);
            if (diagram == null)
                return; // May have been cancelled.
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            List<GKInstance> pathways = fileAdaptor.getRepresentedPathwaysInDiagram(diagram);
            overviewPane.setRenderable(diagram);
            objectListPane.displayEvents(pathways, diagram);
        }
    }
    
    private void refreshEvent(GKInstance pdInstance) {
        if (zoomableEditor.getDisplayedPathwayDiagram() != pdInstance)
            return;
        try {
            // Get whatever event contained
            List<?> pathways = pdInstance.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
            if (pathways == null || pathways.size() == 0)
                return;
            GKInstance pathway = (GKInstance) pathways.get(0);
            displayEvent(pathway, true);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public ElvDiagramHandler getDiagramHandler() {
        return diagramHandler;
    }
    
    private JTabbedPane createOverViewAndSearchPane() {
        overviewPane = new PathwayOverviewPane();
        overviewPane.setBorder(BorderFactory.createEtchedBorder());
        searchPane = new SearchPane();
        searchPane.addSearchMoreAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchInstances();
            }
        });
        multSearchPane = new MultipleAttributeSearchPane();
        multSearchPane.addSearchActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doMultAttSearch();
            }
        });
        objectListPane.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("sectionChanged")) {
                    resetSearchPane();
                }
            }
        });
        searchPane.addSearchActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Overview", overviewPane);
        tabbedPane.add("Search Instances", searchPane);
        return tabbedPane;
    }
    
    public void searchInstances() {
        multSearchPane.setSelectedClass(searchPane.getSchemaClass());
        JFrame parent = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
        multSearchPane.showSearch(parent);
    }
    
    private void doMultAttSearch() {
        searchPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        PersistenceAdaptor persistenceAdaptor = zoomableEditor.getXMLFileAdaptor();
        try {           
            List<GKInstance> found = multSearchPane.search(persistenceAdaptor);        
            if (found == null || found.size() == 0) {
                JOptionPane.showMessageDialog(EntityLevelView.this,
                                              "No " + searchPane.getSchemaClass().getName() + " found.",
                                              "Search Result",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
            else {
                objectListPane.setSelection(found);
                selectionMediator.fireSelectionEvent(objectListPane);
            }
        }
        catch (Exception e) {
            System.err.println("EntityLevelView.doMultAttSearch(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Error in search: " + e, 
                                          "Error in Search", 
                                          JOptionPane.ERROR_MESSAGE);
        }
        searchPane.setCursor(Cursor.getDefaultCursor());
    }
    
    private void search() {
        searchPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Collection c = null;
        PersistenceAdaptor persistenceAdaptor = zoomableEditor.getXMLFileAdaptor();
        try {           
            c = searchPane.search(persistenceAdaptor);
        }
        catch (Exception e) {
            System.err.println("EntityLevelView.search(): " + e);
            e.printStackTrace();
        }
        if (c == null || c.size() == 0) {
            JOptionPane.showMessageDialog(EntityLevelView.this,
                                          "No " + searchPane.getSchemaClass().getName() + " found.",
                                          "Search Result",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            objectListPane.setSelection(new ArrayList(c));
            selectionMediator.fireSelectionEvent(objectListPane);
        }
        searchPane.setCursor(Cursor.getDefaultCursor());
    }
    
    private void resetSearchPane() {
        String selectedTitle = objectListPane.getOpenedPane().getTitle();
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        Schema schema = fileAdaptor.getSchema();
        List<SchemaClass> clsList = new ArrayList<SchemaClass>();
        if (selectedTitle.startsWith("Event")) {
            SchemaClass eventCls = schema.getClassByName(ReactomeJavaConstants.Event);
            clsList.add(eventCls);
        }
        else if (selectedTitle.startsWith("Complex")) {
            SchemaClass complexCls = schema.getClassByName(ReactomeJavaConstants.Complex);
            clsList.add(complexCls);
        }
        else if (selectedTitle.startsWith("Entity")) {
            SchemaClass entityCls = schema.getClassByName(ReactomeJavaConstants.PhysicalEntity);
            Collection subClasses = ((GKSchemaClass)entityCls).getSubClasses();
            for (Iterator it = subClasses.iterator(); it.hasNext();) {
                SchemaClass cls = (SchemaClass) it.next();
                if (cls.getName().equals(ReactomeJavaConstants.Complex))
                    continue;
                clsList.add(cls);
            }
            // Do a sorting
            Collections.sort(clsList, new Comparator<SchemaClass>() {
                public int compare(SchemaClass cls1, SchemaClass cls2) {
                    return cls1.getName().compareTo(cls2.getName());
                }
            });
        }
        searchPane.setSelectableClasses(clsList);
        multSearchPane.setSelectableClasses(clsList);
    }
    
    private JTabbedPane createRightPane() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        zoomableEditor = new InstanceZoomablePathwayEditor();
        tabbedPane.add("Graphic Editor",
                       zoomableEditor);
        attributePane = new AttributePane();
        tabbedPane.add("Property Editor",
                       attributePane);
        return tabbedPane;
    }
    
    public void setEditable(boolean isEditable) {
        attributePane.setEditable(isEditable);
        objectListPane.setEditable(isEditable);
    }
    
    // The following five methods are used to synchronize changes in the local project.
    
    /**
     * Add a list of new GKInstances.
     */
    public void addInstances(List<GKInstance> newInstances) {
        objectListPane.addInstances(newInstances);
    }
    
    /**
     * Delete a instance from the view.
     * @param instance
     */
    public void deleteInstance(GKInstance instance) {
        zoomableEditor.deleteInstance(instance);
        objectListPane.deleteInstance(instance);
    }
    
    /**
     * Switch the type of a GKInstance.
     * @param cls
     * @param instance
     */
    public void switchedType(GKSchemaClass oldCls,
                             GKInstance instance) {
        objectListPane.switchedType(oldCls,
                                    instance);
        zoomableEditor.switchedType(oldCls, instance);
        markAsDirty(instance);
    }
    
    /**
     * Mark a GKInstance as dirty because of changes.
     * @param instance
     */
    public void markAsDirty(GKInstance instance) {
        objectListPane.markAsDirty(instance);
    }
    
    /**
     * Remove a GKInstance.
     * @param instance
     */
    public void removeDirtyFlag(GKInstance instance) {
        objectListPane.removeDirtyFlag(instance);
    }
    
    /**
     * Set up the view for the passed local project.
     * @param fileAdaptor
     */
    public void setUpLocalView(XMLFileAdaptor fileAdaptor) {
        if (fileAdaptor == null)
            return;
        try {
            objectListPane.showLocalView(fileAdaptor);
            zoomableEditor.setXMLFileAdaptor(fileAdaptor);
            overviewPane.setRenderable(new RenderablePathway());
            attributePane.setInstance(null);
        }
        catch (Exception e) {
            System.err.println("EntityLevelView.setUpLocalView(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Call to update the display of an edited event
     * @param editEvent
     */
    public void updateInstance(AttributeEditEvent editEvent) {
        objectListPane.updateInstance(editEvent);
        zoomableEditor.updateInstance(editEvent);
    }
    
}
