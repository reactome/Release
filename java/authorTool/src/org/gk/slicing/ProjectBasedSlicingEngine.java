/*
 * Created on May 7, 2008
 *
 */
package org.gk.slicing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;
import org.junit.Test;

/**
 * This class is used to do a project-based slicing enginge. This class is based on
 * the original Reactome-centric slicing class, SlicingEngine. The slicing is based on 
 * the following rules:
 * 1. The utmost rule: only events with _doRelease true should be in the slice! 
 * 2. The released pathway list should be provided as before. The pathways in this list
 * will be listed under a FrontPageItem. This list should be project based. Actually this 
 * list has already determined the project. The project setting will not be checked actually, 
 * but can be used for debugging or QA purpose. (List1)
 * 3. For each pathway in this list, all contained Events and Reactions should be in the slice. 
 * The contained Events and Reactions will be pulled out based on Pathway.hasEvent and ReactionLikeEvent.hasMember 
 * value list. (List2)
 * 4. Events referred by events in List1 and List2 should be in the slice. This rule is used to handle
 * precedingEvent or other event-type values (List3)
 * 5. Events that refer to events in List2 and List3 should be in the slice. This rule is used to handle 
 * followingEvent (List4)
 * 6. Pathways containing events List3 and List4 should be in the slice (List5)
 * 7. All other non-event instances will be sliced as before.
 * @author wgm
 */
@SuppressWarnings("unchecked")
public class ProjectBasedSlicingEngine extends SlicingEngine {
    private String project = null;
    private boolean useForSpecies = false;
    
    public ProjectBasedSlicingEngine() {
    }
    
    public void setProject(String project) {
        this.project = project;
    }
    
    public String getProject() {
        return project;
    }
    
    public void setUseForSpecies(boolean useForSpecies) {
        this.useForSpecies = useForSpecies; 
    }
    
    /**
     * This method is used to check the difference between two slicing 
     * implementation.
     * @throws Exception
     */
    @Test
    public void compareTwoSlicings() throws Exception {
        MySQLAdaptor oldDba = new MySQLAdaptor("localhost",
                                               "test_slicing_ver25",
                                               "root",
                                               "macmysql01",
                                               3306);
        MySQLAdaptor newDba = new MySQLAdaptor("localhost",
                                               "test_slicing_ver25_new",
                                               "root",
                                               "macmysql01",
                                               3306);
        Collection oldPathways = oldDba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        Collection newPathways = newDba.fetchInstancesByClass(ReactomeJavaConstants.Pathway);
        List<String> oldNames = new ArrayList<String>();
        for (Iterator it = oldPathways.iterator(); it.hasNext();)
            oldNames.add(it.next().toString());
        List<String> newNames = new ArrayList<String>();
        for (Iterator it = newPathways.iterator(); it.hasNext();) {
            newNames.add(it.next().toString());
        }
        System.out.println("Old pathways: " + oldNames.size());
        System.out.println("New pathays: " + newNames.size());
        List<String> oldCopy = new ArrayList<String>(oldNames);
        List<String> newCopy = new ArrayList<String>(newNames);
        oldCopy.removeAll(newNames);
        System.out.println("More in old: " + oldCopy.size());
        for (String name : oldCopy)
            System.out.println("\t" + name);
        newCopy.removeAll(oldNames);
        System.out.println("More in new: " + newCopy.size());
        for (String name : newCopy)
            System.out.println("\t" + name);
    }
    
    /**
     * This method is used to test extractEvents
     * @throws Exception
     */
    @Test
    public void testExtractEvents() throws Exception {
        sourceDBA = new MySQLAdaptor("localhost",
                                     "gk_central_051208",
                                     "root",
                                     "macmysql01",
                                     3306);
        processFileName = "ver25_topics.txt";
        topLevelIDs = getReleasedProcesses();
        Map eventMap = extractEvents();
        System.out.println("Total events: " + eventMap.size());
        // A class-based analysis
        Map<String, List<String>> cls2Instances = new HashMap<String, List<String>>();
        for (Iterator it = eventMap.keySet().iterator(); it.hasNext();) {
            Long dbId = (Long) it.next();
            GKInstance instance = (GKInstance) eventMap.get(dbId);
            SchemaClass cls = instance.getSchemClass();
            List<String> list = cls2Instances.get(cls.getName());
            if (list == null) {
                list = new ArrayList<String>();
                cls2Instances.put(cls.getName(),
                                  list);
            }
            list.add(instance.toString());
        }
        for (Iterator<String> it = cls2Instances.keySet().iterator(); it.hasNext();) {
            String clsName = it.next();
            List<String> list = cls2Instances.get(clsName);
            System.out.println(clsName + ": " + list.size());
        }
        // Want to check what the differences between these two methods
        MySQLAdaptor oldSlice = new MySQLAdaptor("localhost",
                                                 "test_slicing_ver25",
                                                 "root",
                                                 "macmysql01",
                                                 3306);
        Collection events = oldSlice.fetchInstancesByClass(ReactomeJavaConstants.Event);
        Map<String, List<String>> oldSliceMap = new HashMap<String, List<String>>();
        for (Iterator it = events.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            SchemaClass cls = instance.getSchemClass();
            List<String> list = oldSliceMap.get(cls.getName());
            if (list == null) {
                list = new ArrayList<String>();
                oldSliceMap.put(cls.getName(), list);
            }
            list.add(instance.toString());
        }
        // Compare these two maps
        System.out.println("Difference:");
        for (Iterator<String> it = oldSliceMap.keySet().iterator(); it.hasNext();) {
            String clsName = it.next();
            List<String> oldList = oldSliceMap.get(clsName);
            List<String> oldCopy = new ArrayList<String>(oldList);
            List<String> newList = cls2Instances.get(clsName);
            List<String> newCopy = new ArrayList<String>(newList);
            oldCopy.removeAll(newList);
            System.out.println(clsName + " more in old: " + oldCopy.size());
            for (String inst : oldCopy)
                System.out.println("\t" + inst);
            newCopy.removeAll(oldList);
            System.out.println(clsName + " more in new: " + newCopy.size());
            for (String inst: newCopy)
                System.out.println("\t" + inst);
        }
    }
    
    public Map extractEvents() throws Exception {
        // To speed up the performance, get all events and their "hasEvent"
        // and "hasInstance" values first
        Collection events = sourceDBA.fetchInstancesByClass(ReactomeJavaConstants.Event);
        SchemaClass cls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Event);
        sourceDBA.loadInstanceAttributeValues(events, 
                                              cls.getAttribute(ReactomeJavaConstants.precedingEvent));
        // _doNotRelease has been moved to Event since May, 2005.
        // _doNotRelease has been changed to _doRelease as of March 8, 2007
        sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doRelease));
        cls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
        sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants.hasEvent));
        Collection regulations = sourceDBA.fetchInstancesByClass(ReactomeJavaConstants.Regulation);
        cls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Regulation);
        sourceDBA.loadInstanceAttributeValues(regulations, 
                                              cls.getAttribute(ReactomeJavaConstants.regulatedEntity));
        sourceDBA.loadInstanceAttributeValues(regulations,
                                              cls.getAttribute(ReactomeJavaConstants.regulator));
        // We don't want to do a batch loading for regulatedEvent since the software will do a reverse
        // query. No need to load those attribute values first.
        // Load hasMember attributes in reaction
        cls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent);
        Collection reactions = sourceDBA.fetchInstancesByClass(ReactomeJavaConstants.ReactionlikeEvent);
        if (cls.isValidAttribute(ReactomeJavaConstants.hasMember))
            sourceDBA.loadInstanceAttributeValues(reactions,
                                                  cls.getAttribute(ReactomeJavaConstants.hasMember));
        Map<Long, GKInstance> eventMap = new HashMap<Long, GKInstance>();
        // Need to go through the event reference graph to get all events first
        for (Long dbId : topLevelIDs) {
            extractEvents(dbId, eventMap);
        }
        if (useForSpecies)
            extractEventsForSpecies(events, regulations, eventMap);
        else
            extractEvents(events, regulations, eventMap);
        return eventMap;      
    }

    /**
     * A method refactored from extractEvents().
     * @param events
     * @param regulations
     * @param eventMap
     * @throws Exception
     */
    private void extractEvents(Collection events, 
                               Collection regulations,
                               Map<Long, GKInstance> eventMap) throws Exception {
        // Wrap in a loop to get all reference graph
        int currentSize = eventMap.size();
        int preSize = 0;
        //int round = 1;
        do {
            preSize = eventMap.size();
            extractEventTypeReferences(eventMap);
            extractFollowingEvents(events, eventMap);
            extractContainers(events, eventMap);
            extractRegulatedEvents(events, regulations, eventMap);
            //System.out.println(round + ": " + eventMap.size());
            //round ++;
        }
        while (preSize < eventMap.size()); // More events have been added
    }
    
    private void extractEventsForSpecies(Collection events,
                                         Collection regulations,
                                         Map<Long, GKInstance> eventMap) throws Exception {
        extractFollowingEvents(events, eventMap);
        extractContainers(events, eventMap);
        extractRegulatedEvents(events, regulations, eventMap);
        extractEventTypeReferences(eventMap);
    }
    
    private void extractRegulatedEvents(Collection events,
                                        Collection regulations,
                                        Map eventMap) throws Exception {
        int preSize = 0;
        do {
            preSize = eventMap.size();
            for (Iterator it = regulations.iterator(); it.hasNext();) {
                GKInstance regulation = (GKInstance) it.next();
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                GKInstance regulated = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulatedEntity);
                if (regulated == null || regulator == null)
                    continue;
                if (eventMap.containsKey(regulated.getDBID())) {
                    if (regulator.getSchemClass().isa(ReactomeJavaConstants.Event) &&
                        !eventMap.containsKey(regulator.getDBID()) &&
                        shouldEventInSlice(regulator))
                        eventMap.put(regulator.getDBID(),
                                     regulator);
                }
                else if (eventMap.containsKey(regulator.getDBID())) {
                    if (regulated.getSchemClass().isa(ReactomeJavaConstants.Event) &&
                        !eventMap.containsKey(regulated.getDBID()) &&
                        shouldEventInSlice(regulated))
                        eventMap.put(regulated.getDBID(),
                                     regulated);
                }
            }
        }
        while (preSize < eventMap.size());
    }
                                        
    
    private void extractContainers(Collection events,
                                   Map eventMap) throws Exception {
        extractEventInReverse(ReactomeJavaConstants.hasEvent, 
                              events, 
                              eventMap);
    }
    
    private void extractFollowingEvents(Collection events,
                                        Map eventMap) throws Exception {
        extractEventInReverse(ReactomeJavaConstants.precedingEvent,
                              events, 
                              eventMap);
    }
    
    private void extractEventInReverse(String attribute,
                                       Collection events,
                                       Map eventMap) throws Exception {
        Set current = new HashSet(events);
        int prevSize = 0;
        // Do once no matter what
        do {
            prevSize = current.size();
            for (Iterator it = current.iterator(); it.hasNext();) {
                GKInstance event = (GKInstance) it.next();
                if (!event.getSchemClass().isValidAttribute(attribute))
                    continue;
                if (eventMap.containsKey(event.getDBID())) {
                    it.remove();
                    continue;
                }
                if (!shouldEventInSlice(event)) {
                    it.remove();
                    continue;
                }
                List values = event.getAttributeValuesList(attribute);
                if (values == null ||
                    values.size() == 0)
                    continue;
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    GKInstance precedingEvent = (GKInstance) it1.next();
                    if (eventMap.containsKey(precedingEvent.getDBID())) {
                        eventMap.put(event.getDBID(),
                                     event);
                        it.remove();
                        break;
                    }
                }
            }
        }
        while (prevSize > current.size());
    }
    
    private void extractEventTypeReferences(Map eventMap) throws Exception {
        Set current = new HashSet(eventMap.values());
        Set next = new HashSet();
        SchemaClass eventCls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Event);
        Set<SchemaClass> allEventClses = getAllEventClasses(eventCls);
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                Collection attributes = instance.getSchemClass().getAttributes();
                for (Iterator it1 = attributes.iterator(); it1.hasNext();) {
                    GKSchemaAttribute att = (GKSchemaAttribute) it1.next();
                    if (isEventAttribute(att, allEventClses)) {
                        List values = instance.getAttributeValuesList(att);
                        for (Iterator it2 = values.iterator(); it2.hasNext();) {
                            GKInstance value = (GKInstance) it2.next();
                            if (value.getSchemClass().isa(ReactomeJavaConstants.Event) &&
                                shouldEventInSlice(value) &&
                                !eventMap.containsKey(value.getDBID())) {
                                eventMap.put(value.getDBID(),
                                             value);
                                next.add(value);
                            }
                        }
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    private Set<SchemaClass> getAllEventClasses(SchemaClass eventCls) {
        Set<SchemaClass> clses = new HashSet<SchemaClass>();
        Set<SchemaClass> current = new HashSet<SchemaClass>();
        current.add(eventCls);
        Set<SchemaClass> next = new HashSet<SchemaClass>();
        while (current.size() > 0) {
            for (SchemaClass cls : current) {
                clses.add(cls);
                Collection subclasses = ((GKSchemaClass)cls).getSubClasses();
                next.addAll(subclasses);
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return clses;
    }
    
    /**
     * Use to check if an attribute can have event classes.
     * @param attribute
     * @param allEventClses
     * @return
     */
    private boolean isEventAttribute(GKSchemaAttribute attribute,
                                     Set<SchemaClass> allEventClses) {
        for (SchemaClass cls : allEventClses) {
            if (attribute.isValidClass(cls))
                return true;
        }
        return false;
    }
    
    private void extractEvents(Long dbId, Map eventMap) throws Exception {
        GKInstance event = sourceDBA.fetchInstance(dbId);
        if (event == null)
            throw new IllegalStateException("Event cannot be found for a DB_ID in the release list: " + dbId);
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(event);
        GKInstance instance = null;
        List values = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                // Check _DNR
                if (!shouldEventInSlice(instance))
                    continue;
                if (eventMap.containsKey(instance.getDBID()))
                    continue; // This event has been checked
                eventMap.put(instance.getDBID(), instance);
                if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                    values = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                    if (values != null && values.size() > 0) {
                        next.addAll(values);
                    }
                }
                if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                    values = instance.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (values != null && values.size() > 0) {
                        next.addAll(values);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    
    public void testTopics() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "test_gk_central",
                                            "authortool",
                                            "***REMOVED***",
                                            3306);
        String fileName = "ver28_topics.txt";
        
                                            
    }
}
