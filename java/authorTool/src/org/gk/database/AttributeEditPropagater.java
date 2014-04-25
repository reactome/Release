/*
 * Created on Mar 7, 2005
 *
 */
package org.gk.database;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This class is used to help propagate the attribute assignment from one GKInstance
 * to other GKInstance objects. Such kinds of propagations occur when a Species is assigned
 * to an ancestor event in a hierarchical tree. Or a compartment is assigned to a reaction.
 * In the first case, this species information should be propagated to the descendant events
 * in the tree. In the second case, this compartment information should be propagated to 
 * input, output PhysicalEntities.
 * Basically, this is a helper class for AttributePaneController.
 * @author wgm
 *
 */
public class AttributeEditPropagater {
	// Name of attributes
    private final String TAXON_ATT_NAME = ReactomeJavaConstants.species;
    private final String COMPARTMENT_ATT_NAME = ReactomeJavaConstants.compartment;
    private final String REFERENCE_ENTITY_ATT_NAME = ReactomeJavaConstants.referenceEntity;
    // For popup warning
    private Component parentComp;
    
    public AttributeEditPropagater(AttributePaneController controller) {  
    	parentComp = controller.getAttributePane();
    }
    
    public void propagate(GKSchemaAttribute att,
                          GKInstance instance) throws Exception {
        SchemaClass cls = instance.getSchemClass();
        String attName = att.getName();
        // This is not configurable for the time being (8/29/05). It should be changed soon. -- WGM
        // It seems that there is no need to configure this propgation (1/27/06). -- Guanming Wu
        if (attName.equals(REFERENCE_ENTITY_ATT_NAME) &&
            cls.isa(ReactomeJavaConstants.GenomeEncodedEntity)) {
            propagateSpeciesInGEE(instance);
            return;
        }
        boolean shouldProp = AttributeEditConfig.getConfig().isAutoPropagatable((GKSchemaClass)instance.getSchemClass(), att.getName());
        if (!shouldProp)
            return;
        if (attName.equals(TAXON_ATT_NAME)) {
            if (cls.isa(ReactomeJavaConstants.Pathway))
                propagateTaxonSettingInPathway(instance);
            else if (cls.isa(ReactomeJavaConstants.Reaction))
                propagateAttributeInReaction(TAXON_ATT_NAME, instance, true);
            else if (cls.isa(ReactomeJavaConstants.Complex))
                propagateTaxonSettingInComplex(instance);
                //propagateAttributeInComplex(TAXON_ATT_NAME, instance, true);
        }
        else if (attName.equals(COMPARTMENT_ATT_NAME)) {
            if (cls.isa(ReactomeJavaConstants.Reaction))
                propagateAttributeInReaction(COMPARTMENT_ATT_NAME, instance, true);
            else if (cls.isa(ReactomeJavaConstants.Complex))
                propagateAttributeInComplex(COMPARTMENT_ATT_NAME, instance, true);
        }
        else if (cls.isa(ReactomeJavaConstants.Event) && 
                (attName.equals(ReactomeJavaConstants._doRelease) ||
                 attName.equals(ReactomeJavaConstants.authored) ||
                 attName.equals(ReactomeJavaConstants.reviewed) ||
                 attName.equals(ReactomeJavaConstants._doNotRelease))) {
            propagateEventAttribute(instance, attName);
        }
    }
    
    private void propagateTaxonSettingInComplex(GKInstance complex) throws Exception {
        // Get the taxon value
        GKInstance taxon = (GKInstance) complex.getAttributeValue(TAXON_ATT_NAME);
        if (taxon == null)
            return; // Don't do anything if it is null
        int reply = JOptionPane.showConfirmDialog(parentComp,
                                                  "The species setting in a Complex instance can be propagated to its contained\n" +
                                                  "Complexes, EntitySets and GenomeEncodedEntities. Do you want to propagate this\n" +
                                                  "species setting?\n" +
                                                  "Note: Complexes or EntitySets containing only SimpleEntities and OtherEntities\n" +
                                                  "will not be assigned a species value.",
                                                  "Setting Species Automatically",
                                                  JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return;
        // Walk through the hierarchical tree based on "hasComponent" value.
        // If one event has taxon assigned already, stop there even though 
        // that value is different to this new value taxon. However, this is 
        // a choice should ask the actual user.
        List components = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components == null || components.size() == 0)
            return;
        Set current = new HashSet(components);
        Set next = new HashSet();
        GKInstance component = null;
        // Do a width first search to get all assignable PhysicalEntities
        // Need to check the contained PhysicalEntities to determine if
        // species value should be assigned.
        List descendentEntities = new ArrayList(); // Use this list as stack
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                component = (GKInstance) it.next();
                if (!descendentEntities.contains(component))
                    descendentEntities.add(component);
                // Check if this component is a Reaction
                if (component.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent)) {
                    List tmp = component.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                    if (tmp != null)
                        next.addAll(tmp);
                }
                else if (component.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)){
                    // Contained Complex
                    List tmp = component.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                    if (tmp != null)
                        next.addAll(tmp);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        // Check each PhysicalEntity to see if a species value can be assigned
        // This set is used to hold entities that cannot be assigned species.
        Set unassignableEntitiyIds = getSpeciesUnassignableEntities(descendentEntities);
        // Now the species value can be assigned based on these two Collections.
        for (Iterator it = descendentEntities.iterator(); it.hasNext();) {
            component = (GKInstance) it.next();
            if (unassignableEntitiyIds.contains(component.getDBID()))
                continue;
            component.setAttributeValue(TAXON_ATT_NAME, taxon);
            // Need to check if _displayName will be changed
            String newDisplayName = InstanceDisplayNameGenerator.generateDisplayName(component);
            if (!newDisplayName.equals(component.getDisplayName())) {
                component.setDisplayName(newDisplayName);
                fireAttributeEdit(component, "_displayName");
            }
            fireAttributeEdit(component, TAXON_ATT_NAME);
        }
    }

    /**
     * A helper method to extract a set of DB_IDs for those that should NOT be assigned a species value.
     * DB_IDs are used to improve the performance.
     * @param descendentEntities
     * @return
     * @throws InvalidAttributeException
     * @throws Exception
     */
    private Set getSpeciesUnassignableEntities(List descendentEntities) throws InvalidAttributeException, Exception {
        GKInstance component;
        Set unassignableEntityIDs = new HashSet();
        boolean shouldAssign = false;
        for (int i = descendentEntities.size() - 1; i >= 0; i--) {
            component = (GKInstance) descendentEntities.get(i);
            if (!component.getSchemClass().isValidAttribute(TAXON_ATT_NAME)) {
                unassignableEntityIDs.add(component.getDBID());
            }
            else if (component.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                // A complex containing only unassignable entities should not be assigned a species
                List tmp = component.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                shouldAssign = true; // Default should be assignable
                if (tmp != null && tmp.size() > 0) {
                    shouldAssign = false; // Components make it unassignable
                    for (Iterator it = tmp.iterator(); it.hasNext();) {
                        GKInstance tmpComp = (GKInstance) it.next();
                        if (!unassignableEntityIDs.contains(tmpComp.getDBID())) {
                            shouldAssign = true;
                            break;
                        }
                    }
                }
                if (!shouldAssign)
                    unassignableEntityIDs.add(component.getDBID());
            }
            else if (component.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
                // A EntitySet containing only unassignable entities should not be assigned a species
                List tmp = component.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                shouldAssign = true; // Defaultshould be assignable
                if (tmp != null && tmp.size() > 0) {
                    shouldAssign = false; // Components make it unassignable
                    for (Iterator it = tmp.iterator(); it.hasNext();) {
                        GKInstance tmpMember = (GKInstance) it.next();
                        if (!unassignableEntityIDs.contains(tmpMember.getDBID())) {
                            shouldAssign = true;
                            break;
                        }
                    }
                }
                if (!shouldAssign)
                    unassignableEntityIDs.add(component.getDBID());
            }
        }
        return unassignableEntityIDs;
    }
    
    /**
     * Ensure the species values are the same in both ReferenceEntity and GenomeEncodedEntity
     * or get species value from ReferenceEntity for GenomeEncodedEntity if null in GEE.
     * @param instance
     */
    private void propagateSpeciesInGEE(GKInstance instance) throws Exception {
        if (!instance.getSchemClass().isValidAttribute(REFERENCE_ENTITY_ATT_NAME))
            return ; // Not a valid attribute
        GKInstance referenceEntity = (GKInstance) instance.getAttributeValue(REFERENCE_ENTITY_ATT_NAME);
        if (referenceEntity == null || referenceEntity.isShell())
            return ; // Nothing can be done
        if (!referenceEntity.getSchemClass().isValidAttribute(TAXON_ATT_NAME))
            return ; // Not a change
        GKInstance refSpecies = (GKInstance) referenceEntity.getAttributeValue(TAXON_ATT_NAME); 
        GKInstance geeSpecies = (GKInstance) instance.getAttributeValue(TAXON_ATT_NAME);
        boolean useRefSpecies = false;
        //First case: GEE has species assigned. Have to make sure they are the same.
        if (geeSpecies != null) {
            if (geeSpecies != refSpecies) {
                String message = "The species value in referenceEntity is different from value in species attribute.\n " +
                                 "Do you want to use species in referenceEntity?";
                int reply = JOptionPane.showConfirmDialog(parentComp,
                                                          message,
                                                          "Species Conflict",
                                                          JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION)
                    return ; // Do nothing
                useRefSpecies = true;
            }
        }
        else { // Get the species value from ReferenceEntity
            String message = "Do you want to use species value in referenceEntity for species attribute?";
            int reply = JOptionPane.showConfirmDialog(parentComp,
                                                      message,
                                                      "Species Assignment",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                 return;
            useRefSpecies = true;
        }
        if (useRefSpecies) {
            instance.setAttributeValue(TAXON_ATT_NAME, refSpecies);
            // Need to fire the change event.
            fireAttributeEdit(instance, TAXON_ATT_NAME);
        }
    }
    
    /**
     * Grep all event instances contained by the specified event instance by "hasComponent"
     * and "hasInstance" roles.
     * @param event
     * @return a set of event GKInstances.
     * @throws Excetion
     */
    private Set grepAllDescendentInstances(GKInstance event) throws Exception {
        Set rtn = new HashSet();
        // Propagate this settings to all contained events
        Set current = grepEventChildren(event);
        Set next = new HashSet();
        GKInstance tmpEvent = null;
        Boolean tmpDNR = null;
        List tmpValues = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmpEvent = (GKInstance) it.next();
                rtn.add(tmpEvent);
                next.addAll(grepEventChildren(tmpEvent));
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return rtn;
    }
    
    private Set grepEventChildren(GKInstance event) throws Exception {
        Set children = new HashSet();
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List values = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (values != null)
                children.addAll(values);
        }
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
            List values = event.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (values != null)
                children.addAll(values);
        }
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm)) {
            List values = event.getAttributeValuesList(ReactomeJavaConstants.hasSpecialisedForm);
            if (values != null)
                children.addAll(values);
        }
        return children;
    }
    
    private void propagateEventAttribute(GKInstance event, String attName) throws Exception {
        if (!event.getSchemClass().isa("Event"))
            return;
        // Propagate this settings to all contained events
        Set descendents = grepAllDescendentInstances(event);
        if (descendents.size() == 0)
            return; // Nothing needs to do!
        String message = "The " + attName + " setting in event instance can be propagated to " +
                "its contained events.\n" + 
                "Do you want to propagate this setting?";
        int reply = JOptionPane.showConfirmDialog(parentComp,
                message,
                "Propagate Setting",
                JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return;
        GKInstance tmpEvent = null;
        SchemaAttribute att = event.getSchemClass().getAttribute(attName);
        if (att.isMultiple()) {
            List attValues = event.getAttributeValuesList(attName);
            List oldValues = null;
            for (Iterator it = descendents.iterator(); it.hasNext();) {
                tmpEvent = (GKInstance) it.next();
                att = tmpEvent.getSchemClass().getAttribute(attName);
                oldValues = tmpEvent.getAttributeValuesList(attName);
                if (!InstanceUtilities.compareAttValues(attValues, oldValues, att)) {
                    tmpEvent.setAttributeValueNoCheck(att, attValues);
                    fireAttributeEdit(tmpEvent, attName);
                }
            }
        }
        else {
            Object attValue = event.getAttributeValue(attName);
            Object oldValue = null;
            for (Iterator it = descendents.iterator(); it.hasNext();) {
                tmpEvent = (GKInstance) it.next();
                oldValue = tmpEvent.getAttributeValue(attName);
                if (attValue != oldValue) {
                    tmpEvent.setAttributeValue(attName, attValue);
                    fireAttributeEdit(tmpEvent, attName);
                }
            }
        }
    }
    
    /**
     * A helper to compare if two dnr has the same value
     * @param dnr1
     * @param dnr2
     * @return
     */
    private boolean dnrEqual(Boolean dnr1, 
                             Boolean dnr2,
                             String attName) {
        boolean value1, value2;
        if (attName.equals(ReactomeJavaConstants._doRelease)) {
            value1 = (dnr1 == null ? false : dnr1.booleanValue());
            value2 = (dnr2 == null ? false : dnr2.booleanValue());
        }
        else {
            value1 = (dnr1 == null ? true : dnr1.booleanValue());
            value2 = (dnr2 == null ? true : dnr2.booleanValue());
        }
        return value1 == value2;
    }
    
    private void propagateAttributeInComplex(String attName, 
    		                                    GKInstance instance,
    		                                    boolean needConfirm) throws Exception {
        if (!instance.getSchemClass().isa("Complex"))
            return;
        GKInstance attValue = (GKInstance) instance.getAttributeValue(attName);
        if (attValue == null)
            return;
        if (needConfirm) {
            String message = "The " + attName + " setting in Complex instance can be propagated to its contained entities.\n" + 
            "Do you want to propagate this setting?";
            int reply = JOptionPane.showConfirmDialog(parentComp,
                    message,
                    attName + " Setting",
                    JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
        }
        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components == null || components.size() == 0)
            return;
        GKInstance entity = null;
        GKSchemaAttribute att = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            entity = (GKInstance) it.next();
            if (!entity.getSchemClass().isValidAttribute(attName))
                continue;
            if (entity.getAttributeValue(attName) == null) { // Set for only an empty slot
                att = (GKSchemaAttribute) entity.getSchemClass().getAttribute(attName);
                if (att.isValidValue(attValue)) { // It is possible a valid value in Reaction is not
                    // valid in Reaction. For example, a Compartment might not
                    // be an EntityCompartment. Only EntityCompartment can be 
                    // used in Entity.
                    entity.setAttributeValue(att, attValue);
                    // Need to check if _displayName will be changed
                    String newDisplayName = InstanceDisplayNameGenerator.generateDisplayName(entity);
                    if (!newDisplayName.equals(entity.getDisplayName())) {
                        entity.setDisplayName(newDisplayName);
                        fireAttributeEdit(entity, "_displayName");
                    }
                    fireAttributeEdit(entity, attName);
                    if (entity.getSchemClass().isa("Complex"))
                        propagateAttributeInComplex(attName, entity, false);
                }
            }
        }
    }
    
    private void propagateAttributeInReaction(String attName,
                                              GKInstance instance,
											  boolean needConfirm) throws Exception {
        if (!instance.getSchemClass().isa("Reaction"))
            return;
        GKInstance attValue = (GKInstance) instance.getAttributeValue(attName);
        if (attValue == null)
            return;
        if (needConfirm) {
        	String message = "The " + attName + " setting in Reaction instance can be propagated to its input and output entities.\n" + 
							 "Do you want to propagate this setting?";
        	int reply = JOptionPane.showConfirmDialog(parentComp,
        					                          message,
													  attName + " Setting",
													  JOptionPane.YES_NO_OPTION);
        	if (reply != JOptionPane.YES_OPTION)
        		return;
        }
        List inputs = instance.getAttributeValuesList("input");
        List entities = new ArrayList();
        if (inputs != null)
            entities.addAll(inputs);
        List outputs = instance.getAttributeValuesList("output");
        if (outputs != null)
            entities.addAll(outputs);
        GKInstance entity = null;
        GKSchemaAttribute att = null;
        for (Iterator it = entities.iterator(); it.hasNext();) {
            entity = (GKInstance) it.next();
            if (!entity.getSchemClass().isValidAttribute(attName))
                continue;
            if (entity.getAttributeValue(attName) == null) {
                att = (GKSchemaAttribute) entity.getSchemClass().getAttribute(attName);
                if (att.isValidValue(attValue)) { // It is possible a valid value in Pathway is not
                                                  // valid in Reaction. For example, a Compartment might not
                    	                    	  // be an EntityCompartment. Only EntityCompartment can be 
                                                  // used in Entity.
                    entity.setAttributeValue(att, attValue);
                    fireAttributeEdit(entity, attName);
                }
            }
        }
    }
    
    private void propagateTaxonSettingInPathway(GKInstance instance) throws Exception {
        // Get the taxon value
        GKInstance taxon = (GKInstance) instance.getAttributeValue(TAXON_ATT_NAME);
        if (taxon == null)
            return; // Don't do anything if it is null
        int reply = JOptionPane.showConfirmDialog(parentComp,
        		                                  "The species setting in a Pathway instance can be propagated to its contained\n" +
        		                                  "pathways, reactions and inputs and outputs in reactions. Do you want to \n" +
        		                                  "propagate this species setting?",
												  "Setting Species Automatically",
												  JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
        	return;
        // Walk through the hierarchical tree based on "hasComponent" value.
        // If one event has taxon assigned already, stop there even though 
        // that value is different to this new value taxon. However, this is 
        // a choice should ask the actual user.
        List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
        if (components == null || components.size() == 0)
            return;
        Set current = new HashSet(components);
        Set next = new HashSet();
        GKInstance component = null;
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                component = (GKInstance) it.next();
                if (component.getAttributeValue(TAXON_ATT_NAME) != null)
                    continue;
                component.setAttributeValue(TAXON_ATT_NAME, taxon);
                fireAttributeEdit(component, TAXON_ATT_NAME);
                // Check if this component is a Reaction
                if (component.getSchemClass().isa("Reaction"))
                    propagateAttributeInReaction(TAXON_ATT_NAME, component, false);
                else if (component.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)){ // Should be a Pathway
                    List tmp = component.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
                    if (tmp != null)
                        next.addAll(tmp);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    private void fireAttributeEdit(GKInstance instance, String attName) {
        AttributeEditManager.getManager().attributeEdit(instance, attName);
    }
}
