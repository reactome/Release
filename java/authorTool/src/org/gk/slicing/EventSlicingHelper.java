/*
 * Created on Feb 23, 2005
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
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This is a helper class for extracting events for the slice.
 * @author wgm
 *
 */
public class EventSlicingHelper {
    private Map referrersMap;

    public EventSlicingHelper() {
        referrersMap = new HashMap();
    }
    
    public Map getReferrersMap() {
        return this.referrersMap;
    }
 
    public Map extractEvents(List pathwayIDs, MySQLAdaptor sourceDBA) throws Exception {
        Map eventMap = new HashMap();
        // To speed up the performance, get all events and their "hasComponent"
        // and "hasInstance" values first
        Collection events = sourceDBA.fetchInstancesByClass(ReactomeJavaConstants.Event);
        SchemaClass cls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Event);
        // _doNotRelease has been moved to Event since May, 2005.
        if (cls.isValidAttribute(ReactomeJavaConstants._doRelease))
            sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doRelease));
        else if (cls.isValidAttribute(ReactomeJavaConstants._doNotRelease))
            sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doNotRelease));
        cls = sourceDBA.getSchema().getClassByName("Pathway");
        sourceDBA.loadInstanceAttributeValues(events, 
                                              cls.getAttribute(ReactomeJavaConstants.hasEvent));
        // Convert the list of DB_IDs to GKInstances
        List processes = new ArrayList(pathwayIDs.size());
        GKInstance process = null;
        Long dbID = null;
        for (Iterator it = pathwayIDs.iterator(); it.hasNext();) {
            dbID = (Long) it.next();
            process = sourceDBA.fetchInstance(dbID);
            if (process == null) 
                throw new IllegalStateException("SlicingEngine.extractEvents(): " +
                		"process to be released cannot be found in the source " +
                		"database (" + dbID + ").");
            processes.add(process);
        }
        // Pick up the trees rooted at process
        for (Iterator it = processes.iterator(); it.hasNext();) {
            process = (GKInstance) it.next();
            extractEvents(process, eventMap);
        }
        return eventMap;
    }
    
    /**
     * Extract all events in the specified event instance into the specified map.
     * @param event the top-level event instance.
     * @param eventMap the container. All descendants contained by event will be in
     * this map (key DB_ID, value GKInstance).
     * @throws Exception
     */
    public void extractEvents(GKInstance event, Map eventMap) throws Exception {
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
    
    protected boolean shouldEventInSlice(GKInstance event) throws Exception {
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants._doRelease)) {
            Boolean dr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doRelease);
            if (dr != null && dr.booleanValue())
                return true;
        }
        else if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants._doNotRelease)) {
            Boolean dnr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doNotRelease);
            if (dnr != null && !dnr.booleanValue())
                return true;
        }
        return false;
    }
    
    /**
     * Get the list of floating reactions to be used in preceding/following checking. There are three
     * lists of reactions used in this method. The first one has all reactions that are not a component
     * of any pathways. The second one is for reactions that are not a component of any pathways AND not
     * an instance of any other reactions. In the tree, the reactions in the second list are the roots and
     * pure floating reactions. However, if some reactions in the first list are attached to the reactions
     * in the second list, these reactions should also be listed as floating reactions. The third list of 
     * reactions are from floating pathways whose DNR are false. 
     * @throws Exception
     */
    public Map extractFloatingEvents(List topLevelIDs, MySQLAdaptor sourceDBA) throws Exception {
        Map floatingEventMap = new HashMap();
        Set events = new HashSet();
		extractFloatingReactions(events, sourceDBA);
		//System.out.println("floating events after reactions: " + events.size());
		extractFloatingPathwaysAndReactions(events, sourceDBA, topLevelIDs);
		//System.out.println("floating events after pathways: " + events.size());
		// Load precedingEvent attributes that will be used in following events
        // checking
        SchemaClass event = sourceDBA.getSchema().getClassByName("Event");
        sourceDBA.loadInstanceAttributeValues(events, event.getAttribute("precedingEvent"));
        GKInstance reaction = null;
        for (Iterator it = events.iterator(); it.hasNext();) {
            reaction = (GKInstance) it.next();
            floatingEventMap.put(reaction.getDBID(), reaction);
        }
        return floatingEventMap;
    }
    
    /**
     * Extract reactions that are contained by floating pathways whose DNRs are false. 
     * @param events
     */
    private void extractFloatingPathwaysAndReactions(Set events, MySQLAdaptor sourceDBA, List topLevelIDs) throws Exception {
		ArrayList qr = new ArrayList(3);
		SchemaClass pathwayCls = sourceDBA.getSchema().getClassByName("Pathway");
		SchemaAttribute hasCompAtt = pathwayCls.getAttribute(ReactomeJavaConstants.hasEvent);
		qr.add(sourceDBA.createReverseAttributeQueryRequest(pathwayCls, hasCompAtt, "IS NULL", null));
		SchemaClass genericEventCls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.ReactionlikeEvent);
		SchemaAttribute hasInstanceAtt = genericEventCls.getAttribute(ReactomeJavaConstants.hasMember);
		qr.add(sourceDBA.createReverseAttributeQueryRequest(pathwayCls, hasInstanceAtt, "IS NULL", null));
		// It seems that the following is not correct. It is more like a bug in the database. In pre_ver12,
		// the value will be empty if it is set to false explicitly.
		//SchemaAttribute dnrAtt = pathwayCls.getAttribute("_doNotRelease");
		//qr.add(sourceDBA.createAttributeQueryRequest(dnrAtt, "=", "false"));
		Collection pathways = sourceDBA.fetchInstance(qr);
		if (pathways == null || pathways.size() == 0)
		    return;
		GKInstance pathway = null;
		for (Iterator it = pathways.iterator(); it.hasNext();) {
		    pathway = (GKInstance) it.next();
		    if (topLevelIDs.contains(pathway.getDBID()))
		        continue; // Don't need check it
		    extractFloatingPathwaysAndReactions(pathway, events);
		}
    }
    
    private void extractFloatingPathwaysAndReactions(GKInstance pathway, Set reactions) throws Exception {
        // Check DNR
        if (!shouldEventInSlice(pathway))
            return;
        reactions.add(pathway);
        List values = null;
        SchemaAttribute att = null;
        Set children = new HashSet();
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            att = pathway.getSchemClass().getAttribute(ReactomeJavaConstants.hasEvent);
            values = pathway.getAttributeValuesList(att);
            if (values != null)
                children.addAll(values);
        }
        if (pathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
            values = pathway.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (values != null)
                children.addAll(values);
        }
        if (children.size() > 0) {
            for (Iterator it = children.iterator(); it.hasNext();) {
                GKInstance child = (GKInstance) it.next();
                if (child.getSchemClass().isa("Reaction")) {
                    reactions.add(child);
                    child.addAttributeValueNoCheck(SlicingEngine.REFERRER_ATTRIBUTE_KEY, pathway);
                    referrersMap.put(child.getDBID(), child);
                }
                else if (child.getSchemClass().isa("Pathway")) {
                    child.addAttributeValueNoCheck(SlicingEngine.REFERRER_ATTRIBUTE_KEY, pathway);
                    extractFloatingPathwaysAndReactions(child, reactions);
                    referrersMap.put(child.getDBID(), child);
                }
            }
        }
    }
    
    private void extractFloatingReactions(Set reactions, MySQLAdaptor sourceDBA) throws Exception {
		ArrayList qr = new ArrayList(1);
		SchemaClass reactionCls = sourceDBA.getSchema().getClassByName("Reaction");
		SchemaClass pathwayCls = sourceDBA.getSchema().getClassByName("Pathway");
		SchemaAttribute hasCompAtt = pathwayCls.getAttribute(ReactomeJavaConstants.hasEvent);
		qr.add(sourceDBA.createReverseAttributeQueryRequest(reactionCls, hasCompAtt, "IS NULL", null));
		// This is the first list: Reaction is not a component for any Pathway
		Collection reactions1 = sourceDBA.fetchInstance(qr);
		// Get the second list: Reaction is not an instance for any GenericEvent and
		// reaction is not a component for any Pathway.
		SchemaClass genericEventCls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.hasMember);
		SchemaAttribute hasInstanceAtt = genericEventCls.getAttribute(ReactomeJavaConstants.hasMember);
		qr.add(sourceDBA.createReverseAttributeQueryRequest(reactionCls, hasInstanceAtt, "IS NULL", null));
		Collection reactions2 = sourceDBA.fetchInstance(qr);
		if (reactions2 == null || reactions2.size() == 0)
		    return ; 
		// To speed up
		sourceDBA.loadInstanceAttributeValues(reactions2, hasInstanceAtt);
		// Reactions in this set will be pushed into floatingRxtMap
		reactions.addAll(reactions2);
		if (reactions1 != null && reactions1.size() > 0) {
		    // A reaction that is an instanceof a GenericReaction but
		    // is not a component of any Pathway should be regarded
		    // as a Floating Event.
            Set tmpSet = new HashSet(reactions1);
            tmpSet.removeAll(reactions);
            if (tmpSet.size() > 0) {
                // Try to get more reactions
                GKInstance reaction = null;
                List instances = null;
                GKInstance instance = null;
                Set toBeAdded = new HashSet();
                for (Iterator it = reactions.iterator(); it.hasNext();) {
                    reaction = (GKInstance) it.next();
                    if (!reaction.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
                        continue;
                    instances = reaction.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (instances == null || instances.size() == 0)
                        continue;
                    for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                        instance = (GKInstance) it1.next();
                        if (tmpSet.contains(instance))
                            toBeAdded.add(instance);
                    }
                }
                reactions.addAll(toBeAdded);
            }
        }
    }
    
    /**
     * Check out all events specified by the top level event ids in the list. This map
     * also includes events from floating list.
     * @param topLevelEvents a list of top level event IDs, which should be Longs.
     * @return map key: Longs values: GKInstances
     * @throws Exception
     */
    public Map extractAllEvents(List topLevelEvents, MySQLAdaptor sourceDBA) throws Exception {
        Map eventMap = extractEvents(topLevelEvents, sourceDBA);
        Map floatingEventMap = extractFloatingEvents(topLevelEvents, sourceDBA);
        Map allEventsMap = new HashMap();
        allEventsMap.putAll(eventMap);
        checkPrecedingEvents(eventMap, floatingEventMap, allEventsMap);
        checkFollowingEventsInFloating(floatingEventMap, allEventsMap);
        return allEventsMap;
    }
    
    /**
     * Get any preceding events that are floating.
     * @param eventMap
     * @param releaseEventMap
     * @throws Exception
     */
    public void checkPrecedingEvents(Map eventMap, 
                                     Map floatingEventMap, 
                                     Map releaseEventMap) throws Exception {
        Long dbId = null;
        GKInstance event = null;
        List precedingEvents = null;
        GKInstance precedingEvent = null;
        for (Iterator it = eventMap.keySet().iterator(); it.hasNext();) {
            dbId = (Long) it.next();
            event = (GKInstance) eventMap.get(dbId);
            precedingEvents = event.getAttributeValuesList("precedingEvent");
            if (precedingEvents == null || precedingEvents.size() == 0)
                continue;
            for (Iterator it1 = precedingEvents.iterator(); it1.hasNext();) {
                precedingEvent = (GKInstance) it1.next();
                if (releaseEventMap.containsKey(precedingEvent.getDBID()))
                    continue;
                if (floatingEventMap.containsKey(precedingEvent.getDBID())) {
                    extractEvents(precedingEvent, releaseEventMap);
                }
            }
        }
    }
    
    /**
     * Check if any following events are in the sky.
     * @param floatingEventMap
     * @param releaseEventMap
     * @throws Exception
     */
    public void checkFollowingEventsInFloating(Map floatingEventMap, 
                                               Map releaseEventMap) throws Exception {
        if (floatingEventMap == null || floatingEventMap.size() == 0)
            return;
        GKInstance reaction = null;
        Long dbID = null;
        List values = null;
        GKInstance ref = null;
        List dbIDs = new ArrayList(floatingEventMap.keySet());
        int preCount = dbIDs.size();
        while (true) {
            for (Iterator it = dbIDs.iterator(); it.hasNext();) {
                dbID = (Long) it.next();
                if (releaseEventMap.containsKey(dbID)) {
                    it.remove();
                    continue;
                }
                reaction = (GKInstance) floatingEventMap.get(dbID);
                values = reaction.getAttributeValuesList("precedingEvent");
                if (values == null || values.size() == 0)
                    continue;
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    ref = (GKInstance) it1.next();
                    if (releaseEventMap.containsKey(ref.getDBID())) {
                        // This floating event should be in the sky
                        extractEvents(reaction, releaseEventMap);
                        it.remove();
                        break;
                    }
                }
            }
            // No more changes of the floatingEventMap size. The loop should be broken.
            if (dbIDs.size() == preCount)
                break;
            preCount = dbIDs.size();
        }
    }
}
