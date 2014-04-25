/*
 * Created on Dec 17, 2008
 *
 */
package org.gk.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.junit.Test;


/**
 * This handler class is used to handle the checking out of an Event instance from the database
 * to the local project.
 * @author wgm
 *
 */
public class EventCheckOutHandler {
    // A list of attribute that should be escaped during checking out to control
    // the checked out size
    private List<String> escapeAttributes;
    
    public EventCheckOutHandler() {
        escapeAttributes = new ArrayList<String>();
    }
    
    public void addEscapeAttribute(String name) {
        if (!escapeAttributes.contains(name))
            escapeAttributes.add(name);
    }
    
    private Map<SchemaClass, Set<GKInstance>> extractPathwayDiagram(Map<Long, GKInstance> touchedMap,
                                                                    Set<GKInstance> events) throws Exception {
        Map<SchemaClass, Set<GKInstance>> pdMap = new HashMap<SchemaClass, Set<GKInstance>>();
        for (GKInstance event : events) {
            Collection pds = event.getReferers(ReactomeJavaConstants.representedPathway);
            if (pds == null || pds.size() == 0)
                continue;
            for (Iterator it = pds.iterator(); it.hasNext();) {
                GKInstance pd = (GKInstance) it.next();
                if (touchedMap.containsKey(pd.getDBID()))
                    continue;
                touchedMap.put(pd.getDBID(), pd);
                addInstanceToMap(pd, pdMap);
            }
        }
        return pdMap;
    }
    
    /**
     * This method is used to check out the PathwayDiagram instances.
     * @param clsMap
     * @throws Exception
     * @Note: This method has been replaced by a private method extractPathwayDiagram because there
     * is a bug in this method.
     */
//    public void checkOutPathwayDiagram(Map<SchemaClass, Set<GKInstance>> clsMap) throws Exception {
//        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
//        SchemaClass pathwayCls = dba.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
//        Set<GKInstance> pathways = clsMap.get(pathwayCls);
//        if (pathways == null || pathways.size() == 0)
//            return;
//        SchemaClass pdCls = dba.getSchema().getClassByName(ReactomeJavaConstants.PathwayDiagram);
//        // Just in case an old schema is used
//        if (pdCls == null)
//            return;
//        // This should be faster than multiple run
//        Collection pdInstances = dba.fetchInstancesByClass(pdCls);
//        if (pdInstances == null || pdInstances.size() == 0)
//            return;
//        
//        // Load the representedPathway attribute
//        SchemaAttribute att = pdCls.getAttribute(ReactomeJavaConstants.representedPathway);
//        dba.loadInstanceAttributeValues(pdInstances, att);
//        Set<GKInstance> checkedOut = new HashSet<GKInstance>();
//        for (Iterator it = pdInstances.iterator(); it.hasNext();) {
//            GKInstance pd = (GKInstance) it.next();
//            // Check if pd should be included in the checkedOut set.
//            List<GKInstance> list = pd.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
//            if (list != null) {
//                for (Object obj : list) {
//                    if (pathways.contains(obj)) {
//                        checkedOut.add(pd);
//                        break;
//                    }
//                }
//            }
//        }
//        Map<SchemaClass, Set<GKInstance>> pdMap = new HashMap<SchemaClass, Set<GKInstance>>();
//        pdMap.put(pdCls, checkedOut);
//        loadAttributeValues(pdMap, dba);
//        // Get other instances
//        for (GKInstance pdInstance : checkedOut) {
//            for (Iterator it1 = pdInstance.getSchemaAttributes().iterator(); it1.hasNext();) {
//                att = (GKSchemaAttribute) it1.next();
//                String name = att.getName();
//                if (att.isInstanceTypeAttribute()) {
//                    java.util.List values = pdInstance.getAttributeValuesList(att);
//                    if (values == null || values.size() == 0)
//                        continue;
//                    for (Iterator it2 = values.iterator(); it2.hasNext();) {
//                        GKInstance value = (GKInstance) it2.next();
//                        if (value == null) // Just in case
//                            continue;
//                        addInstanceToMap(value, pdMap);
//                    }
//                }
//            }
//        }
//        // Load newly loaded instances
//        loadAttributeValues(pdMap, dba);
//        loadDisplayNamesForShells(pdMap, dba);
//        mergeMap(clsMap, pdMap);
//    }
    
    /**
     * Get all instances that should be placed into the local project.
     * @param eventNode
     * @return
     * @throws Exception
     */
    public Map<SchemaClass, Set<GKInstance>> pullInstances(Set<GKInstance> events) throws Exception {
        Map<Long, GKInstance> touchedMap = new HashMap<Long, GKInstance>();
        // Sorting according classes
        Map<SchemaClass, Set<GKInstance>> clsMap = new HashMap<SchemaClass, Set<GKInstance>>();
        for (GKInstance event : events) {
            touchedMap.put(event.getDBID(), event);
            addInstanceToMap(event,
                             clsMap);
        }
        // Fetch all attributes
        MySQLAdaptor dba = PersistenceManager.getManager().getActiveMySQLAdaptor();
        loadAttributeValues(clsMap, dba);
        GKSchemaAttribute att = null;
        // Fetch attributes values
        Map<SchemaClass, Set<GKInstance>> fringeMap = new HashMap<SchemaClass, Set<GKInstance>>();
        Map<SchemaClass, Set<GKInstance>> precedingMap = extractInstances(touchedMap, 
                                                                          ReactomeJavaConstants.Event,
                                                                          ReactomeJavaConstants.precedingEvent);
        mergeMap(fringeMap, precedingMap);
        loadAttributeValues(fringeMap, dba);
        for (Set<GKInstance> set : fringeMap.values())
            events.addAll(set);
        for (GKInstance event : events) {
            for (Iterator it1 = event.getSchemaAttributes().iterator(); it1.hasNext();) {
                att = (GKSchemaAttribute) it1.next();
                String name = att.getName();
                if (escapeAttributes.contains(name))
                    continue; // Escape this attribute
                if (att.isInstanceTypeAttribute()) {
                    java.util.List values = event.getAttributeValuesList(att);
                    if (values == null || values.size() == 0)
                        continue;
                    for (Iterator it2 = values.iterator(); it2.hasNext();) {
                        GKInstance value = (GKInstance) it2.next();
                        if (value == null) // Just in case
                            continue;
                        if (touchedMap.containsKey(value.getDBID()))
                            continue;
                        addInstanceToMap(value, fringeMap);
                        touchedMap.put(value.getDBID(), value);
                    }
                }
            }
        }
        // CAs should be in the list. It is catalyts we need!
        // For performance reason to load all attributes for the touchedMap
        Map<SchemaClass, Set<GKInstance>> touchedClsMap = new HashMap<SchemaClass, Set<GKInstance>>();
        for (GKInstance inst : touchedMap.values())
            addInstanceToMap(inst, touchedClsMap);
        loadAttributeValues(touchedClsMap, dba);
        
        Map<SchemaClass, Set<GKInstance>> caMap = extractCatalysts(touchedMap);
        mergeMap(fringeMap, caMap);
        Map<SchemaClass, Set<GKInstance>> regulationMap = extractRegulations(touchedMap,
                                                                             events);
        mergeMap(fringeMap, regulationMap);
        
        // Load pathway diagrams
        Map<SchemaClass, Set<GKInstance>> pdMap = extractPathwayDiagram(touchedMap, events);
        mergeMap(fringeMap, pdMap);
        
//        loadAttributeValues(fringeMap, dba);
        // Get ModifieredResidues
        Map<SchemaClass, Set<GKInstance>> modificationsMap = extractedModification(touchedMap,
                                                                                   fringeMap);
        mergeMap(fringeMap, modificationsMap);
        Map<SchemaClass, Set<GKInstance>> complexMap = extractInstances(touchedMap, 
                                                                        ReactomeJavaConstants.Complex,
                                                                        ReactomeJavaConstants.hasComponent);
        mergeMap(fringeMap, complexMap);
        // The above two statments should be called before the following to get
        // all EntitySets contained by complexes
        Map<SchemaClass, Set<GKInstance>> setMap = extractInstances(touchedMap,
                                                                    ReactomeJavaConstants.EntitySet, 
                                                                    ReactomeJavaConstants.hasMember,
                                                                    ReactomeJavaConstants.hasCandidate);
        mergeMap(fringeMap, setMap);
        loadAttributeValues(fringeMap, dba);
        // Shell instances map
        Map<SchemaClass, Set<GKInstance>> shellMap = new HashMap<SchemaClass, Set<GKInstance>>();
        Map<Long, GKInstance> touchedMap1 = new HashMap<Long, GKInstance>();
        for (SchemaClass cls : fringeMap.keySet()) {
            Set<GKInstance> list = fringeMap.get(cls);
            for (GKInstance instance1 : list) {
                for (Iterator it2 = instance1.getSchemaAttributes().iterator(); it2.hasNext();) {
                    att = (GKSchemaAttribute) it2.next();
                    if (!att.isInstanceTypeAttribute() ||
                        escapeAttributes.contains(att.getName()))
                        continue;
                    java.util.List values = instance1.getAttributeValuesList(att);
                    for (Iterator it3 = values.iterator(); it3.hasNext();) {
                        GKInstance value = (GKInstance) it3.next();
                        if (touchedMap.containsKey(value.getDBID()))
                            continue;
                        if (touchedMap1.containsKey(value.getDBID()))
                            continue;
                        value.setIsShell(true);
                        addInstanceToMap(value, shellMap);
                        touchedMap1.put(value.getDBID(), value);
                    }
                }
            }
        }
//        loadAttributeValues(shellMap, dba);
        // Merge two maps
        Map<SchemaClass, Set<GKInstance>> schemaMap = new HashMap<SchemaClass, Set<GKInstance>>(clsMap);
        mergeMap(schemaMap, fringeMap);
        mergeMap(schemaMap, shellMap);
        // Need to load _displayName for shell instance
        loadDisplayNamesForShells(schemaMap,
                                  dba);
        return schemaMap;
    }
    
    private void loadDisplayNamesForShells(Map<SchemaClass, Set<GKInstance>> clsToSet,
                                           MySQLAdaptor dba) throws Exception {
        Set<GKInstance> insts = new HashSet<GKInstance>();
        for (Set<GKInstance> set : clsToSet.values()) {
            for (GKInstance inst : set)
                if (inst.isShell())
                    insts.add(inst);
        }
        dba.loadInstanceAttributeValues(insts, new String[]{ReactomeJavaConstants._displayName});
    }
    
    private Map<SchemaClass, Set<GKInstance>> extractCatalysts(Map<Long, GKInstance> touchedMap) throws Exception {
        Map<SchemaClass, Set<GKInstance>> caMap = new HashMap<SchemaClass, Set<GKInstance>>();
        Set<GKInstance> current = new HashSet<GKInstance>(touchedMap.values());
        for (GKInstance inst : current) {
            if (inst.getSchemClass().isa(ReactomeJavaConstants.CatalystActivity)) {
                GKInstance pe = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (pe == null || touchedMap.containsKey(pe.getDBID()))
                    continue;
                touchedMap.put(pe.getDBID(), pe);
                addInstanceToMap(pe, caMap);
            }
        }
        return caMap;
    }

    private Map<SchemaClass, Set<GKInstance>> extractRegulations(Map<Long, GKInstance> touchedMap,
                                                                 Set<GKInstance> events) throws Exception  {
        Map<SchemaClass, Set<GKInstance>> regulationMap = new HashMap<SchemaClass, Set<GKInstance>>();
        for (GKInstance event : events) {
            Collection regulations = event.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulations == null || regulations.size() == 0)
                continue;
            for (Iterator it = regulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                if (touchedMap.containsKey(regulation.getDBID()))
                    continue;
                touchedMap.put(regulation.getDBID(), regulation);
                addInstanceToMap(regulation, regulationMap);
            }
        }
        return regulationMap;
    }
    
    private Map<SchemaClass, Set<GKInstance>> extractInstances(Map<Long, GKInstance> touchedMap,
                                                               String checkedCls,
                                                               String... attributes) throws Exception {
        Map<SchemaClass, Set<GKInstance>> map = new HashMap<SchemaClass, Set<GKInstance>>();
        Set<GKInstance> current = new HashSet<GKInstance>();
        for (GKInstance inst : touchedMap.values()) {
            if (inst.getSchemClass().isa(checkedCls))
                current.add(inst);
        }
        Set<GKInstance> next = new HashSet<GKInstance>();
        while (current.size() > 0) {
            for (GKInstance inst : current) {
                for (String att : attributes) {
                    if (!inst.getSchemClass().isValidAttribute(att)) 
                        continue;
                    List<?> values = inst.getAttributeValuesList(att);
                    if (values == null || values.size() == 0)
                        continue;
                    for (Iterator<?> it = values.iterator(); it.hasNext();)
                        next.add((GKInstance)it.next());
                }
            }
            for (Iterator<GKInstance> it = next.iterator(); it.hasNext();) {
                GKInstance inst = it.next();
                if (touchedMap.containsKey(inst.getDBID())) {
                    it.remove();
                    continue;
                }
                touchedMap.put(inst.getDBID(), inst);
                addInstanceToMap(inst, map);
                if (!inst.getSchemClass().isa(checkedCls))
                    it.remove();
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return map;
    }
                                                               
    private Map<SchemaClass, Set<GKInstance>> extractedModification(Map<Long, GKInstance> touchedMap,
                                                                    Map<SchemaClass, Set<GKInstance>> fringeMap)
            throws InvalidAttributeException, Exception {
        Map<SchemaClass, Set<GKInstance>> modificationsMap = new HashMap<SchemaClass, Set<GKInstance>>();
        for (SchemaClass cls : fringeMap.keySet()) {
            if (cls.isValidAttribute(ReactomeJavaConstants.hasModifiedResidue)) {
                Set<GKInstance> set = fringeMap.get(cls);
                for (GKInstance inst : set) {
                    List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
                    if (list == null)
                        continue;
                    for (Iterator it = list.iterator(); it.hasNext();) {
                        GKInstance modification = (GKInstance) it.next();
                        if (touchedMap.containsKey(modification.getDBID()))
                            continue;
                        addInstanceToMap(modification, modificationsMap);
                        touchedMap.put(modification.getDBID(), modification);
                    }
                }
            }
        }
        return modificationsMap;
    }
    
    private void loadAttributeValues(Map<SchemaClass, Set<GKInstance>> clsMap,
                                     MySQLAdaptor dba) throws Exception {
        // Fetch all attributes
        GKSchemaAttribute att = null;
        List<GKSchemaAttribute> atts = new ArrayList<GKSchemaAttribute>();
        for (SchemaClass cls : clsMap.keySet()) {
            Set<GKInstance> list = clsMap.get(cls);
            atts.clear();
            for (Iterator it1 = cls.getAttributes().iterator(); it1.hasNext();) {
                att = (GKSchemaAttribute) it1.next();
                if (att.getName().equals(ReactomeJavaConstants.DB_ID)) // DB_ID should be loaded always
                    continue; // Escape them
                atts.add(att);
            }
            dba.loadInstanceAttributeValues(list, atts);
        }
    }
    
    private void addInstanceToMap(GKInstance instance, 
                                  Map<SchemaClass, Set<GKInstance>> map) {
        Set<GKInstance> list = map.get(instance.getSchemClass());
        if (list == null) {
            list = new HashSet<GKInstance>();
            map.put(instance.getSchemClass(), list);
        }
        list.add(instance);
    }
    
    private void mergeMap(Map<SchemaClass, Set<GKInstance>> targetMap, 
                          Map<SchemaClass, Set<GKInstance>> sourceMap) {
        for (SchemaClass cls : sourceMap.keySet()) {
            Set<GKInstance> list1 = sourceMap.get(cls);
            Set<GKInstance> list2 = targetMap.get(cls);
            if (list2 != null) {
                list2.addAll(list1);
            }
            else {
                targetMap.put(cls, list1);
            }
        }
    }
    
    /**
     * A helper method to check if instances in the database to be stored into the local project exist.
     * @param schemaMap
     * @param fileAdaptor
     */
    public void checkExistence(Map<SchemaClass, Set<GKInstance>> schemaMap, 
                               XMLFileAdaptor fileAdaptor, 
                               JFrame parentFrame) {
        List<GKInstance> existedInstances = new ArrayList<GKInstance>();
        for (SchemaClass dbCls : schemaMap.keySet()) {
            Set<GKInstance> instances = schemaMap.get(dbCls);
            for (Iterator<GKInstance> it1 = instances.iterator(); it1.hasNext();) {
                GKInstance dbInstance = (GKInstance) it1.next();
                GKInstance localInstance = (GKInstance) fileAdaptor.fetchInstance(dbInstance.getDBID());
                if (localInstance != null && localInstance.isDirty() && !localInstance.isShell()) {
                    it1.remove(); // remove from the list. This is defensive programming: don't want to assume
                                  // any behaviors in XMLFileAdaptor.
                    existedInstances.add(localInstance);
                }
            }
        }
        if (existedInstances.size() == 0)
            return;
        StringBuffer buffer = new StringBuffer();
        buffer.append("The following instances have been changed in the local project. Please use update in the repository view:\n");
        for (Iterator<GKInstance> it = existedInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            buffer.append(instance.getExtendedDisplayName());
            if (it.hasNext())
                buffer.append("\n");
        }
        // Show message
        JOptionPane.showMessageDialog(parentFrame, 
                                      buffer.toString(),
                                     "Existing Instances",
                                     JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Check out an dbEvent from the database and an event hierarchy rooted at this event. The first 
     * level of referred instances should be checked out too.
     * @param dbEvent
     * @param fileAdaptor
     * @throws Exception
     */
    public void checkOutEvent(GKInstance dbEvent, 
                              XMLFileAdaptor fileAdaptor) throws Exception {
        Set<GKInstance> set = new HashSet<GKInstance>();
        set.add(dbEvent);
        checkOutEvents(set, fileAdaptor);
    }
    
    /**
     * Check out a set of GKInstance events.
     * @param dbEvents instances in this set should be Events.
     * @param fileAdaptor
     * @throws Exception
     */
    public void checkOutEvents(Set<GKInstance> dbEvents,
                               XMLFileAdaptor fileAdaptor) throws Exception {
        Set<GKInstance> allEvents = new HashSet<GKInstance>();
        for (GKInstance event : dbEvents) {
            Set<GKInstance> tmp = InstanceUtilities.getContainedEvents(event);
            allEvents.addAll(tmp);
        }
        // Don't forget to add these events.
        allEvents.addAll(dbEvents);
        Map<SchemaClass, Set<GKInstance>> schemaMap = pullInstances(allEvents);
//        checkOutPathwayDiagram(schemaMap);
        fileAdaptor.store(schemaMap);
        // Clear isShell flag for GKInstnaces from the database.
        InstanceUtilities.clearShellFlags(schemaMap);
    }
    
    @Test
    public void testCheckOutEvents() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_112410",
                                            "root",
                                            "macmysql01");
//        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
//                                            "gk_central",
//                                            "authortool",
//                                            "T001test");
        PersistenceManager.getManager().setActiveMySQLAdaptor(dba);
        GKInstance event = dba.fetchInstance(109581L);
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        PersistenceManager.getManager().setActiveFileAdaptor(fileAdaptor);
        long time1 = System.currentTimeMillis();
        checkOutEvent(event, fileAdaptor);
        long time2 = System.currentTimeMillis();
        System.out.println("Time for checking out: " + (time2 - time1));
        fileAdaptor.save("tmp/test.rtpj");
    }
    
}
