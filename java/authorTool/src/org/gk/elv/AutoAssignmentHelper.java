/*
 * Created on Dec 4, 2008
 *
 */
package org.gk.elv;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.database.AttributeEditManager;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.FlowLine;
import org.gk.render.Note;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableRegistry;


/**
 * This helper class is used to assignment compartment setting based on 
 * the locations of Renderable in a diagram.
 * @author wgm
 *
 */
public class AutoAssignmentHelper {
    private PathwayEditor pathwayEditor;
    private XMLFileAdaptor fileAdpator;
    
    public AutoAssignmentHelper(InstanceZoomablePathwayEditor zoomableEditor) {
        pathwayEditor = zoomableEditor.getPathwayEditor();
        fileAdpator = zoomableEditor.getXMLFileAdaptor();
    }
    
    public void assignCompartments() {
        // Check if there is any compartment in the diagram. If none, don't do anything.
        // This assumes that the user has not started assignment compartment.
        boolean needValidation = false;
        for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (r instanceof RenderableCompartment) {
                needValidation = true;
                break;
            }
        }
        if (!needValidation)
            return;
        Map<Long, List<Renderable>> reactomeIdToObjects = generateIdToObjects();
        if (!checkCompartmentsInAliases(reactomeIdToObjects))
            return;
        // Need to check compartment for all displayed objects
        for (Long id : reactomeIdToObjects.keySet()) {
            List<Renderable> list = reactomeIdToObjects.get(id);
            if (list.size() == 1)
                assignCompartment(list.get(0));
            else
                handleShortcuts(list);
        }
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        //TODO: Consider assigning compartment to displayed pathways.
    }
    
    private Map<Long, List<Renderable>> generateIdToObjects() {
        Map<Long, List<Renderable>> map = new HashMap<Long, List<Renderable>>();
        for (Iterator it = pathwayEditor.getDisplayedObjects().iterator(); it.hasNext();) {
            Renderable r = (Renderable) it.next();
            if (shouldBlockAssignment(r))
                continue;
            List<Renderable> list = map.get(r.getReactomeId());
            if (list == null) {
                list = new ArrayList<Renderable>();
                map.put(r.getReactomeId(), list);
            }
            list.add(r);
        }
        return map;
    }
    
    private void handleShortcuts(List<Renderable> list) {
        Renderable r = list.get(0);
        GKInstance instance = fileAdpator.fetchInstance(r.getReactomeId());
        if (instance == null)
            return;
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment))
            return;
        // Check how many compartments
        Map<Renderable, RenderableCompartment> shortcutToCompart = new HashMap<Renderable, RenderableCompartment>();
        for (Renderable shortcut : list) {
            RenderableCompartment compart = getCompartment(shortcut);
            shortcutToCompart.put(shortcut, compart);
        }
        if (shortcutToCompart.values().size() == 1) {
            // Treated as a simple case
            assignCompartment(r);
        }
        else {
            handleShortcutDuplicaition(r, shortcutToCompart);
        }
    }

    private void handleShortcutDuplicaition(Renderable r,
                                            Map<Renderable, RenderableCompartment> shortcutToCompart) {
        // Need to duplicate instance
        Map<RenderableCompartment, List<Renderable>> comptToRenderables = new HashMap<RenderableCompartment, List<Renderable>>();
        for (Renderable shortcut : shortcutToCompart.keySet()) {
            RenderableCompartment compartment = shortcutToCompart.get(shortcut);
            List<Renderable> list = comptToRenderables.get(compartment);
            if (list == null) {
                list = new ArrayList<Renderable>();
                comptToRenderables.put(compartment, list);
            }
            list.add(shortcut);
        }
        try {
            Map<GKInstance, RenderableCompartment> instanceToComp = new HashMap<GKInstance, RenderableCompartment>();
            for (RenderableCompartment compt : comptToRenderables.keySet()) {
                GKInstance instance = getCompartmentInstance(compt);
                instanceToComp.put(instance, compt);
            }
            GKInstance instance = fileAdpator.fetchInstance(r.getReactomeId());
            // Check the original compartment setting
            GKInstance originalCompt = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.compartment);
            RenderableCompartment compt = instanceToComp.get(originalCompt);
            if (compt != null) {
                instanceToComp.remove(originalCompt);
                // Just in case: maybe there are more than two compartment settings
                instance.setAttributeValue(ReactomeJavaConstants.compartment, originalCompt);
            }
            else {
                // Just pick the first one as the original
                GKInstance first = instanceToComp.keySet().iterator().next();
                instance.setAttributeValue(ReactomeJavaConstants.compartment, first);
                instanceToComp.remove(first);
            }
            // Duplicate for remaining
            InstanceCloneHelper cloneHelper = new InstanceCloneHelper();
            for (GKInstance compInst : instanceToComp.keySet()) {
                compt = instanceToComp.get(compInst);
                List<Renderable> list = comptToRenderables.get(compt);
                GKInstance duplicated = cloneHelper.cloneInstance(instance, 
                                                                  fileAdpator);
                duplicated.setAttributeValue(ReactomeJavaConstants.compartment, 
                                             compInst);
                checkDisplayName(null, duplicated);
                for (Renderable tmp : list) {
                    tmp.setReactomeId(duplicated.getDBID());
                }
            }
            validateShortcuts(comptToRenderables);
        }
        catch(Exception e) {
            System.err.println("AutoAssignmentHelper.handleShortcutDuplication(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Need to break up shortcuts link among different Renderables
     * @param comptToRenderables
     */
    private void validateShortcuts(Map<RenderableCompartment, List<Renderable>> comptToRenderables) {
        RenderableRegistry registry = RenderableRegistry.getRegistry();
        for (RenderableCompartment compartment : comptToRenderables.keySet()) {
            List<Renderable> list = comptToRenderables.get(compartment);
            registry.unregister(list.get(0).getDisplayName());
            if (list.size() == 1) {
                // No need to have shortcuts
                Renderable r = list.get(0);
                r.setShortcuts(null);
                r.setAttributes(new HashMap());
                GKInstance instance = fileAdpator.fetchInstance(r.getReactomeId());
                r.setDisplayName(instance.getDisplayName());
                registry.add(r);
            }
            else {
                // Make a copy -- a safe measure!
                List<Renderable> shortcuts = new ArrayList<Renderable>(list);
                Map attributes = new HashMap();
                for (Renderable r : list) {
                    r.setShortcuts(shortcuts);
                    r.setAttributes(attributes);
                }
                Renderable r = list.get(0);
                GKInstance instance = fileAdpator.fetchInstance(r.getReactomeId());
                r.setDisplayName(instance.getDisplayName());
                registry.add(r);
            }
        }
    }
    
    /**
     * Check if two aliases are in different compartments. If they are, the user will be asked if
     * duplication is needed. Here I don't use the shortcuts since shortcuts should be deprectated.
     */
    private boolean checkCompartmentsInAliases(Map<Long, List<Renderable>> reactomeIdToObjects) {
        boolean needConfirm = false;
        Set<RenderableCompartment> compartments = new HashSet<RenderableCompartment>();
        for (Long id : reactomeIdToObjects.keySet()) {
            List<Renderable> list = reactomeIdToObjects.get(id);
            if (list.size() == 1)
                continue;
            // Don't user shortcuts. They will be deprecated soon!!!
            compartments.clear();
            for (Renderable r : list) {
                RenderableCompartment compartment = getCompartment(r);
                compartments.add(compartment);
            }
            if (compartments.size() > 1) {
                needConfirm = true;
                break;
            }
        }
        if (needConfirm) {
            int reply = JOptionPane.showConfirmDialog(pathwayEditor,
                                                      "Some entities are in different compartments but are the same.\n" +
                                                      "These entities need to be cloned. Do you want to continue?",
                                                      "Clone Entities?",
                                                      JOptionPane.OK_CANCEL_OPTION);
            return reply == JOptionPane.OK_OPTION;
        }
        return true;
    }
    
    private boolean shouldBlockAssignment(Renderable r) {
        // If r is a complex subunit, avoid to assign compartment to it.
        //TODO: There is a techinical difficult here to assign compartment
        // to complex subunit. In many cases, complex subunits have different
        // compartment values from its container. For simplicity, just ignore
        // complex compartment for the time being.
        if (r.getContainer() instanceof RenderableComplex)
            return true;
        if (r instanceof RenderableCompartment ||
                r instanceof FlowLine ||
                r instanceof Note)
            return true;
        return false;
    }
    
    private void assignCompartment(Renderable r) {
        if (shouldBlockAssignment(r))
            return;
        try {
            // Just need to check this changedValue
            GKInstance instance = fileAdpator.fetchInstance(r.getReactomeId());
            if (instance == null)
                return;
            // Just in case
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment))
                return;
            RenderableCompartment compartment = getCompartment(r);
            GKInstance gkComp = getCompartmentInstance(compartment);
            instance.setAttributeValue(ReactomeJavaConstants.compartment, gkComp);
            checkDisplayName(r, instance);
        }
        catch(Exception e) {
            System.err.println("InstanceZoomablePahtwayEditor.assignCompartment(): " + e);
            e.printStackTrace();
        }
    }

    private void checkDisplayName(Renderable r, GKInstance instance) {
        String newDisplayName = InstanceDisplayNameGenerator.generateDisplayName(instance);
        if (!newDisplayName.equals(instance.getDisplayName())) {
            instance.setDisplayName(newDisplayName);
            if (r != null)
                r.setDisplayName(newDisplayName);
            AttributeEditManager.getManager().attributeEdit(instance, 
                                                            ReactomeJavaConstants._displayName);
        }
    }
    
    private GKInstance getCompartmentInstance(RenderableCompartment compartment) throws Exception {
        GKInstance gkComp = null;
        if (compartment != null) {
            gkComp = fileAdpator.fetchInstance(compartment.getReactomeId());
        }
        else {
            // Automatically assign extracellular region
            gkComp = getExtraCellularRegionCompartment();
        }
        return gkComp;
    }
    
    /**
     * A recursive way to get the lowest compartment for a passed Renderable object.
     * @param r
     * @return
     */
    private RenderableCompartment getCompartment(Renderable r) {
        if (r instanceof RenderableCompartment)
            return null;
        Renderable container = r.getContainer();
        while (container != null && 
               !(container instanceof RenderableCompartment))
            container = container.getContainer();
        if (container instanceof RenderableCompartment)
            return (RenderableCompartment) container;
        return null;
    }
    
    /**
     * Get GKInstance for ExtraCellular region compartment.
     * @return
     * @throws Exception
     */
    private GKInstance getExtraCellularRegionCompartment() throws Exception {
        GKInstance ex = null;
        String displayName = "extracellular region";
        // Check if it is in the local project
        Collection c = fileAdpator.fetchInstanceByAttribute(ReactomeJavaConstants.EntityCompartment,
                                                            ReactomeJavaConstants._displayName,
                                                            "=",
                                                             displayName);
        if (c == null || c.size() == 0) {
            // Get from the database
            MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor(pathwayEditor);
            if (dba != null) {
                c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.EntityCompartment,
                                                 ReactomeJavaConstants._displayName,
                                                 "=",
                                                 displayName);
                ex = (GKInstance) c.iterator().next();
                // download to local 
                ex = PersistenceManager.getManager().getLocalReference(ex);
            }
        }
        else
            ex = (GKInstance) c.iterator().next();
        if (ex == null) {
            // Have to create one
            ex = fileAdpator.createNewInstance(ReactomeJavaConstants.EntityCompartment);
            ex.addAttributeValue(ReactomeJavaConstants.name, displayName);
            ex.setDisplayName(displayName);
        }
        return ex;
    }
    
}
