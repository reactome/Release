/*
 * Created on Nov 4, 2008
 *
 */
package org.gk.elv;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.database.AttributeEditEvent;
import org.gk.database.EventCellRenderer;
import org.gk.database.EventTreeBuildHelper;
import org.gk.database.HierarchicalEventPane;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.gk.util.AccordionPane;
import org.gk.util.GKApplicationUtilities;


/**
 * A subclass of PropertyObjectList for display GKInstances. 
 * @author wgm
 *
 */
public class InstanceTreeListPane extends AccordionPane implements Selectable {
    private HierarchicalEventPane eventPane;
    private ComplexHierarchicalPane complexPane;
    private EntityInstanceListPane entityPane;
    // For selection control
    private SelectionMediator selectionMediator;
    // Used to control selection
    private TreeSelectionListener tsListener;
    
    public InstanceTreeListPane() {
        init();
    }
    
    public void setSelectionMediator(SelectionMediator mediator) {
        this.selectionMediator = mediator;
        this.selectionMediator.addSelectable(this);
        tsListener = new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                selectionMediator.fireSelectionEvent(InstanceTreeListPane.this);
            }
        };
        eventPane.addSelectionListener(tsListener);
        complexPane.getTree().addTreeSelectionListener(tsListener);
        entityPane.getTree().addTreeSelectionListener(tsListener);
        eventPane.getTree().setDragEnabled(true); // Want to make it DnD
    }
    
    public HierarchicalEventPane getEventPane() {
        return this.eventPane;
    }
    
    private void init() {
        eventPane = new HierarchicalEventPane();
        String prop = GKApplicationUtilities.getApplicationProperties().getProperty("elvHideReactionsAtTop", 
                                                                                    Boolean.FALSE + "");
        eventPane.setHideReactionsInTopLevel(new Boolean(prop));
        eventPane.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("hideReactionAtTop")) {
                    GKApplicationUtilities.getApplicationProperties().setProperty("elvHideReactionsAtTop",
                                                                                  evt.getNewValue() + "");
                }
            }
        });
        EventCellRenderer renderer = (EventCellRenderer) eventPane.getTree().getCellRenderer();
        renderer.enableGrayOn(true);
        eventPane.hideTitle();
        addTab("Event Hierarchy", eventPane);
        complexPane = new ComplexHierarchicalPane();
        addTab("Complex Hierarchy", complexPane);
        entityPane = new EntityInstanceListPane();
        addTab("Entity List", entityPane);
    }
    
    public void setEditable(boolean isEditable) {
        eventPane.setEditable(isEditable);
    }
    
    public InstanceTreePane getComplexPane() {
        return this.complexPane;
    }
    
    public InstanceTreePane getEntityPane() {
        return this.entityPane;
    }
    
    protected void displayEvents(List<GKInstance> pathways,
                                 RenderablePathway diagram) {
        // Reset the flag for all displayed events
        resetIsOnElvFlag();
        for (GKInstance pathway : pathways)
            pathway.setAttributeValueNoCheck("isOnElv", Boolean.TRUE);
        List components = diagram.getComponents();
        if (components == null || components.size() == 0)
            return;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        for (Iterator it = components.iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r.getReactomeId() == null)
                continue;
            GKInstance instance = fileAdaptor.fetchInstance(r.getReactomeId());
            instance.setAttributeValueNoCheck("isOnElv", Boolean.TRUE);
        }
        eventPane.getTree().repaint();
    }
    
    private void resetIsOnElvFlag() {
        List<GKInstance> projects = eventPane.getTopLevelEvents();
        Set<GKInstance> allEvents = new HashSet<GKInstance>();
        for (GKInstance project : projects) {
            allEvents.add(project);
            if (project.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                Set<GKInstance> contained = InstanceUtilities.getContainedEvents(project);
                allEvents.addAll(contained);
            }
        }
        for (GKInstance event : allEvents)
            event.removeAttributeValueNoCheck("isOnElv", Boolean.TRUE);
    }
    
    @Override
    public void setTransferHandler(TransferHandler newHandler) {
        entityPane.getTree().setTransferHandler(newHandler);
        complexPane.getTree().setTransferHandler(newHandler);
        eventPane.getTree().setTransferHandler(newHandler);
    }

    public void addEventSelectionListener(TreeSelectionListener l) {
        eventPane.addSelectionListener(l);
    }
    
    public GKInstance getSelectedEvent() {
        return eventPane.getSelectedEvent();
    }
    
    /**
     * Set up the view for the passed local project.
     * @param fileAdaptor
     * @throws Exception
     */
    public void showLocalView(XMLFileAdaptor fileAdaptor) throws Exception {
        rebuildEventTree(fileAdaptor);
        Collection complexes = fileAdaptor.fetchInstancesByClass(ReactomeJavaConstants.Complex);
        List<GKInstance> list = new ArrayList<GKInstance>();
        if (complexes != null) {
            for (Iterator it = complexes.iterator(); it.hasNext();) {
                GKInstance complex = (GKInstance) it.next();
                list.add(complex);
            }
        }
        InstanceUtilities.sortInstances(list);
        complexPane.setComplexes(list);
        // Grep a list of entity SchemaClasses for display
        List<GKSchemaClass> entityClses = new ArrayList<GKSchemaClass>();
        GKSchemaClass pe = (GKSchemaClass)fileAdaptor.getSchema().getClassByName(ReactomeJavaConstants.PhysicalEntity);
        for (Iterator it = pe.getSubClasses().iterator(); it.hasNext();) {
            // Avoid listing Complex
            GKSchemaClass cls = (GKSchemaClass) it.next();
            if (cls.isa(ReactomeJavaConstants.Complex))
                continue;
            entityClses.add(cls);
        }
        InstanceUtilities.sortSchemaClasses(entityClses);
        entityPane.setSchemaClasses(entityClses, fileAdaptor);
    }

    private void rebuildEventTree(XMLFileAdaptor fileAdaptor) throws Exception {
        EventTreeBuildHelper treeHelper = new EventTreeBuildHelper();
        // Show events
        List events = fileAdaptor.fetchAllEvents();
        List<GKInstance> topEvents = treeHelper.getTopLevelEvents(events);
        eventPane.setTopLevelEvents(topEvents);
    }
    
    /**
     * Get the selected GKInstance displayed.
     * @return
     */
    public List<GKInstance> getSelection() {
        List<GKInstance> selected = new ArrayList<GKInstance>();
        // Find which tab is selected
        if (eventPane.isVisible()) {
            List list = eventPane.getSelection();
            if (list != null && list.size() > 0) {
                for (Iterator it = list.iterator(); it.hasNext();) {
                    selected.add((GKInstance) it.next());
                }
            }
        }
        else if (complexPane.isVisible()) {
            selected = complexPane.getSelectedInstances();
        }
        else if (entityPane.isVisible()) {
            selected = entityPane.getSelectedInstances();
        }
        return selected;
    }
    
    /**
     * Set the selection by a list of GKInstances.
     */
    public void setSelection(List selection) {
        eventPane.getTree().removeTreeSelectionListener(tsListener);
        complexPane.getTree().removeTreeSelectionListener(tsListener);
        entityPane.getTree().removeTreeSelectionListener(tsListener);
        // Find which tree should be visible
        eventPane.setSelectedInstances(selection);
        complexPane.setSelectedInstances(selection);
        entityPane.setSelectedInstances(selection);
        eventPane.getTree().addTreeSelectionListener(tsListener);
        complexPane.getTree().addTreeSelectionListener(tsListener);
        entityPane.getTree().addTreeSelectionListener(tsListener);
        if (selection == null || selection.size() == 0)
            return;
        // Priority: Event, Complex and Entity
        if (eventPane.getSelection().size() > 0) {
            setIsClosed("Event Hierarchy", false);
            setIsClosed("Complex Hierarchy", true);
            setIsClosed("Entity List", true);
        }
        else {
            // Want to keep the open one still open
            if (isClosed("Complex Hierarchy")) {
                if (entityPane.getSelectedInstances().size() > 0) {
                    setIsClosed("Entity List", false);
                    setIsClosed("Event Hierarchy", true);
                }
                else if (complexPane.getSelectedInstances().size() > 0) {
                    setIsClosed("Complex Hierarchy", false);
                    setIsClosed("Entity List", true);
                    setIsClosed("Event Hierarchy", true);
                }
            }
            else {
                if (complexPane.getSelectedInstances().size() == 0 &&
                    entityPane.getSelectedInstances().size() > 0) {
                    setIsClosed("Complex Hierarchy", true);
                    setIsClosed("Entity List", false);
                    setIsClosed("Event Hierarchy", true);
                }
            }
        }
    }
    
    /**
     * Add a new GKInstance to the list view.
     * @param instance
     */
    public void addInstance(GKInstance instance) {
        entityPane.addInstance(instance);
        complexPane.addInstance(instance);
        eventPane.addInstance(instance);
    }
    
    public void addInstances(List<GKInstance> newInstances) {
        if (newInstances == null || newInstances.size() == 0)
            return;
        for (GKInstance instance : newInstances) {
            entityPane.addInstance(instance);
            complexPane.addInstance(instance);
        }
        // Check if a refresh is needed for the event hierarchical view
        boolean needRefresh = false;
        for (GKInstance inst : newInstances) {
            if (inst.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                needRefresh = true;
                break;
            }
        }
        if (needRefresh) {
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            try {
                rebuildEventTree(fileAdaptor);
            }
            catch(Exception e) {
                System.err.println("InstanceTreeListPane.addInstances(): " + e);
                e.printStackTrace();
            }
        }
        else {
            // Probably it is an reaction
            for (GKInstance inst : newInstances)
                eventPane.addInstance(inst);
        }
    }
    
    public void markAsDirty(GKInstance instance) {
        entityPane.updateInstance(instance);
        complexPane.updateInstance(instance);
        eventPane.markAsDirty(instance);
    }
    
    public void removeDirtyFlag(GKInstance instance) {
        entityPane.updateInstance(instance);
        complexPane.updateInstance(instance);
        //TODO: Most likely a new method is needed in the eventPane class.
    }
    
    public void deleteInstance(GKInstance instance) {
        entityPane.deleteInstance(instance);
        complexPane.deleteInstance(instance);
        eventPane.deleteInstance(instance);
    }
    
    /**
     * Update the view of the passed GKInstance
     * @param instance
     */
    public void updateInstance(AttributeEditEvent editEvent) {
        entityPane.updateInstance(editEvent);
        complexPane.updateInstance(editEvent);
        eventPane.updateInstance(editEvent);
    }
    
    public void switchedType(SchemaClass oldCls,
                             GKInstance instance) {
        entityPane.updateInstance(instance);
        complexPane.updateInstance(instance);
    }
}
