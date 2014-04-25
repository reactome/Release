/*
 * Created on Jul 14, 2004
 */
package org.gk.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * A helper class for building a hierarchical event tree. Using this method to
 * isolate schema related information into one class.
 * @author wugm
 */
public class EventTreeBuildHelper {

	private MySQLAdaptor dba;
	
	public EventTreeBuildHelper() {
	}
	
	public EventTreeBuildHelper(MySQLAdaptor dba) {
		this.dba = dba;
	}
	
	/**
	 * Get a list of attribute names that are related to tree building (e.g. hasComponent,
	 * hasInstance, etc).
	 * @return
	 */
	public static String[] getTreeAttributeNames() {
	    String[] attNames = new String[]{
                "hasEvent",
	            "hasComponent",
	            "hasInstance",
	            "hasMember",
	            "hasSpecialisedForm"
	    };
	    return attNames;
	}
	
	public static Icon[] getTreeIcons() {
	    Icon[] icons = new Icon[]{
	            GKApplicationUtilities.getIsPartOfIcon(),
	            GKApplicationUtilities.getIsAIcon(),
	            GKApplicationUtilities.getIsMemberIcon(),
	            GKApplicationUtilities.getIsSpecializedFormIcon()
	    };
	    return icons;
	}
	
	public static String[] getReverseTreeAttributeNames() {
	    String[] attNames = new String[]{
	            "componentOf",
	            "instanceOf",
	            "memberOf",
	            "specialisedFromOf"
	    };
	    return attNames;
	}
	
	public static void cacheReverseAttributes(Collection events) {
	    String[] attNames = getTreeAttributeNames();
	    String[] reverseAttNames = getReverseTreeAttributeNames();
	    GKInstance event = null;
	    java.util.List values = null;
	    try {
            for (Iterator it = events.iterator(); it.hasNext();) {
                event = (GKInstance) it.next();
                for (int i = 0; i < attNames.length; i++) {
                    if (!event.getSchemClass().isValidAttribute(attNames[i]))
                        continue;
                    values = event.getAttributeValuesList(attNames[i]);
                    if (values == null || values.size() == 0)
                        continue;
                    for (Iterator it1 = values.iterator(); it1.hasNext();) {
                        GKInstance child = (GKInstance) it1.next();
                        child.addAttributeValueNoCheck(reverseAttNames[i], event);
                    }
                }
            }
        } 
	    catch (Exception e) {
            System.err.println("EventTreeBuildHelper.cacheReverseAttributes(): " + e);
            e.printStackTrace();
        }
	}
	
	public static void buildTree(DefaultMutableTreeNode treeNode, 
	                         GKInstance event,
	                         Map node2Icon) {
		java.util.List values = null;
		try {
		    String hasInstanceAttName = null;
		    Icon instanceIcon = null;
		    // Adapted to the new Schema -- 7/12/05
		    if (event.getSchemClass().isValidAttribute("hasInstance")) {
		        hasInstanceAttName = "hasInstance";
		        instanceIcon = GKApplicationUtilities.getIsAIcon();
		    }
		    else if (event.getSchemClass().isValidAttribute("hasMember")) {
		        hasInstanceAttName = "hasMember";
		        instanceIcon = GKApplicationUtilities.getIsMemberIcon();;
		    }
		    else if (event.getSchemClass().isValidAttribute("hasSpecialisedForm")) {
		        hasInstanceAttName = "hasSpecialisedForm";
		        instanceIcon = GKApplicationUtilities.getIsSpecializedFormIcon();
		    }
            if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
                values = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                if (values != null) {
                    for (Iterator it = values.iterator(); it.hasNext();) {
                        GKInstance e = (GKInstance)it.next();
                        if (e == null)
                            continue;
                        DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
                        treeNode.add(subNode);
                        node2Icon.put(subNode, GKApplicationUtilities.getIsPartOfIcon());
                        buildTree(subNode, e, node2Icon);
                    }
                }
            }
		    if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
		        values = event.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
		        if (values != null) {
		            for (Iterator it = values.iterator(); it.hasNext();) {
		                GKInstance e = (GKInstance)it.next();
		                if (e == null)
		                    continue;
		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
		                treeNode.add(subNode);
		                node2Icon.put(subNode, GKApplicationUtilities.getIsPartOfIcon());
		                buildTree(subNode, e, node2Icon);
		            }
		        }
		    }
		    // Check hasInstance relationships
		    if (hasInstanceAttName != null) {
		        values = event.getAttributeValuesList(hasInstanceAttName);
		        if (values != null) {
		            for (Iterator it = values.iterator(); it.hasNext();) {
		                GKInstance e = (GKInstance)it.next();
		                if (e == null)
		                    continue;
		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
		                treeNode.add(subNode);
		                node2Icon.put(subNode, instanceIcon);
		                buildTree(subNode, e, node2Icon);
		            }
		        }
		    }
		}
		catch (Exception e) {
			System.err.println("HierarchicalEventPane.buildTree(): " + e);
			e.printStackTrace();
		}
	}
	
	public List<GKInstance> getTopLevelComplexes(Collection complexes) throws Exception {
	    String[] attNames = new String[] {
	            ReactomeJavaConstants.hasComponent
	    };
	    return getTopLevelInstances(complexes, attNames);
	}
	
	/**
	 * Get the top-level events from the passed list of events.
	 * @param events
	 * @return
	 * @throws Exception
	 */
	public List<GKInstance> getTopLevelEvents(List events) throws Exception {
	       // To find the top level events
        String[] attNames = new String[]{"hasEvent",
                                         "hasComponent",
                                         "hasInstance",
                                         "hasMember",
                                         "hasSpecialisedForm"};
	    return getTopLevelInstances(events, attNames);
	}

    private List<GKInstance> getTopLevelInstances(Collection instances, String[] attNames)
            throws InvalidAttributeException, Exception {
        GKInstance event = null;
	    java.util.List values = null;
	    // Need to find the top-level events
	    Map<Long, GKInstance> map = new HashMap<Long, GKInstance>(); // Use as a cache
	    for (Iterator it = instances.iterator(); it.hasNext();) {
	        event = (GKInstance) it.next();
	        map.put(event.getDBID(), event);
	    }
	    for (Iterator it = instances.iterator(); it.hasNext();) {
	        event = (GKInstance) it.next();
	        for (int i = 0; i < attNames.length; i++) {
	            if (event.getSchemClass().isValidAttribute(attNames[i])) {
	                values = event.getAttributeValuesList(attNames[i]);
	                if (values != null && values.size() > 0) {
	                    for (Iterator it1 = values.iterator(); it1.hasNext();) {
	                        GKInstance tmp = (GKInstance) it1.next();
	                        map.remove(tmp.getDBID());
	                    }
	                }
	            }
	        }
	    }
	    // Get the top-level events
	    return new ArrayList<GKInstance>(map.values());
    }
	
	public Collection getTopLevelEvents() throws Exception {
		ArrayList qr = new ArrayList();
		Schema schema = dba.getSchema();
		SchemaClass eventCls = schema.getClassByName("Event");
		SchemaClass pathwayCls = schema.getClassByName("Pathway");
		if (pathwayCls.isValidAttribute(ReactomeJavaConstants.hasEvent)) {
		    SchemaAttribute hasCompAtt = pathwayCls.getAttribute(ReactomeJavaConstants.hasEvent);
            // We need to list orphan reactions too. So use eventCls instead of pathwayCls since
            // this is reverseAttribute query
		    qr.add(dba.createReverseAttributeQueryRequest(eventCls, hasCompAtt, "IS NULL", null));
		}
        else if (pathwayCls.isValidAttribute(ReactomeJavaConstants.hasComponent)) {
            SchemaAttribute hasCompAtt = pathwayCls.getAttribute(ReactomeJavaConstants.hasComponent);
            qr.add(dba.createReverseAttributeQueryRequest(eventCls, hasCompAtt, "IS NULL", null));
        }
        SchemaClass bbeCls = schema.getClassByName(ReactomeJavaConstants.BlackBoxEvent);
        if (bbeCls != null) {
            SchemaAttribute hasEventAtt = bbeCls.getAttribute(ReactomeJavaConstants.hasEvent);
            if (hasEventAtt != null)
                qr.add(dba.createReverseAttributeQueryRequest(eventCls, hasEventAtt, "IS NULL", null));
        }
		Collection topLevelPathways = dba.fetchInstance(qr);
        // Filter out Interactions in case they are in the databases. Interactions are used
        // in the database used to store pathways converted from other databases and interactions
        // loaded for functional interaction analysis.
        for (Iterator it = topLevelPathways.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            if (event.getSchemClass().isa(ReactomeJavaConstants.Interaction))
                it.remove();
        }
		return topLevelPathways;
	}

	public Collection getAllEvents() throws Exception {
		return dba.fetchInstancesByClass("Event");
	}
	
	/**
	 * The attributes for "hasInstance", "hasComponent", "taxon" will be loaded.
	 * @param events
	 * @throws Exception
	 */
	public void loadAttribtues(Collection events) throws Exception {
        List equivalentEvents = new ArrayList();
        List conceptualEvents = new ArrayList();
        List pathways = new ArrayList();
        List blackBoxEvents = new ArrayList();
        List reactions = new ArrayList();
        for (Iterator it = events.iterator(); it.hasNext();) {
            GKInstance in = (GKInstance) it.next();
            // Exclude Interactions
            if (in.getSchemClass().isa(ReactomeJavaConstants.Interaction))
                it.remove();
            else if (in.getSchemClass().isa(ReactomeJavaConstants.Pathway))
                pathways.add(in);
            else if (in.getSchemClass().isa(ReactomeJavaConstants.BlackBoxEvent))
                blackBoxEvents.add(in);
            else if (in.getSchemClass().isa(ReactomeJavaConstants.EquivalentEventSet))
                equivalentEvents.add(in);
            else if (in.getSchemClass().isa(ReactomeJavaConstants.ConceptualEvent))
                conceptualEvents.add(in);
            if (in.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
                reactions.add(in);
        }
        // Try to make it work with new schema. No GenericEvent in the new Schema -- Guanming 7/12/05
		// These are for new schema -- Guanming 7/12/05
        SchemaClass cls = null;
        if (equivalentEvents.size() > 0) {
            cls = dba.getSchema().getClassByName("EquivalentEventSet");
            SchemaAttribute att = cls.getAttribute("hasMember");
            if (att != null)
                dba.loadInstanceAttributeValues(equivalentEvents, att);
        }
        if (conceptualEvents.size() > 0) {
            cls = dba.getSchema().getClassByName("ConceptualEvent");
            SchemaAttribute att = cls.getAttribute("hasSpecialisedForm");
            if (att != null)
                dba.loadInstanceAttributeValues(conceptualEvents, att);
        }
        if (pathways.size() > 0) {
            cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Pathway);
            if (cls.isValidAttribute(ReactomeJavaConstants.hasEvent))
                dba.loadInstanceAttributeValues(pathways, cls.getAttribute(ReactomeJavaConstants.hasEvent));
            else if (cls.isValidAttribute(ReactomeJavaConstants.hasComponent))
                dba.loadInstanceAttributeValues(pathways, cls.getAttribute(ReactomeJavaConstants.hasComponent));
        }
        if (reactions.size() > 0) {
            cls = dba.getSchema().getClassByName(ReactomeJavaConstants.Reaction);
            if (cls.isValidAttribute(ReactomeJavaConstants.hasMember))
                dba.loadInstanceAttributeValues(reactions, cls.getAttribute(ReactomeJavaConstants.hasMember));
        }
        // hasEvent is used by BlackBoxEvent too. If just call the above two statements, attribute values
        // in BlackBoxEvent cannot be loaded. So just split the events list to pathways and BlackBoxEvents
		if (blackBoxEvents.size() > 0) {
		    cls = dba.getSchema().getClassByName(ReactomeJavaConstants.BlackBoxEvent);
		    dba.loadInstanceAttributeValues(blackBoxEvents, cls.getAttribute(ReactomeJavaConstants.hasEvent));
		}
		cls = dba.getSchema().getClassByName("Event");
		if (cls.isValidAttribute("taxon"))
		    dba.loadInstanceAttributeValues(events, cls.getAttribute("taxon"));
		else if (cls.isValidAttribute("species")) // New schema uses species
		    dba.loadInstanceAttributeValues(events, cls.getAttribute("species"));
		// Need to load _doNotRelease
        if (cls.isValidAttribute(ReactomeJavaConstants._doRelease))
            dba.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doRelease));
        else if (cls.isValidAttribute(ReactomeJavaConstants._doNotRelease))
            dba.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doNotRelease));
	}
	
	/**
	 * Cache "instanceOf" and "componentOf" values into non-valid attribute slots to
	 * speed up the performace. This method should be called after a batch call for 
	 * "hasComponent" and "hasInstance". Otherwise, the performace will be bad.
	 * @param events
	 * @throws Exception
	 */
	public void cacheOfTypeValues(Collection events) throws Exception {
		// Cache componentOf values
		GKInstance event = null;
		java.util.List values = null;
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
            if (!event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
				continue;
			values = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
			if (values == null || values.size() == 0)
				continue;
			for (Iterator it1 = values.iterator(); it1.hasNext();) {
				GKInstance child = (GKInstance) it1.next();
				child.addAttributeValueNoCheck("componentOf", event);
			}
		}
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance)it.next();
			if (!event.getSchemClass().isValidAttribute("hasInstance"))
				continue;
			values = event.getAttributeValuesList("hasInstance");
			if (values == null || values.size() == 0)
				continue;
			for (Iterator it1 = values.iterator(); it1.hasNext();) {
				GKInstance referer = (GKInstance)it1.next();
				referer.addAttributeValueNoCheck("instanceOf", event);
			}
		}
	}
	
}
