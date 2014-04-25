/*
 * Created on Sep 23, 2006
 *
 */
package org.gk.gkCurator.authorTool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;
import org.gk.schema.SchemaClass;

public class PathwayHandler extends RenderableHandler {
    
    protected GKInstance convertChanged(Renderable r) throws Exception {
        Long dbId = (Long) getDbId(r);
        if (dbId == null)
            return null; // Just in case. Should not occur.
        // Fetch the db instance
        GKInstance dbInstance = dbAdaptor.fetchInstance(dbId);
        // These Types are convertable
        SchemaClass cls = dbInstance.getSchemClass();
        String localClsName = null;
        if (cls.isa(ReactomeJavaConstants.Pathway) ||
            cls.isa(ReactomeJavaConstants.EquivalentEventSet) ||
            cls.isa(ReactomeJavaConstants.ConceptualEvent)) {
            // Use the class from the database
            localClsName = cls.getName();
        }
        else
            localClsName = ReactomeJavaConstants.Pathway;
        GKInstance local = createNewWithID(localClsName, dbId);
        return local;
    }
    
    protected void extractComponents(GKInstance iPathway,
                                     Renderable rPathway,
                                     Map rToIMap) throws Exception {
        // hasComponents
        java.util.List components = rPathway.getComponents();
        if (components != null && components.size() > 0) {
            java.util.List hasComponents = new ArrayList(components.size());
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                // Only reaction and pathway can be added
                if (r instanceof RenderablePathway ||
                    r instanceof RenderableReaction ||
                    r instanceof ReactionNode) {
                    if (r instanceof Shortcut)
                        r = ((Shortcut)r).getTarget();
                    GKInstance instance1 = (GKInstance) rToIMap.get(r);
                    if (instance1 != null) {// Should always true
                        hasComponents.add(instance1);
                    }
                }
            }
            if (iPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                iPathway.setAttributeValueNoCheck(ReactomeJavaConstants.hasEvent,
                                                  hasComponents);
            else if (iPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                iPathway.setAttributeValueNoCheck(ReactomeJavaConstants.hasComponent,
                                                  hasComponents);
            else if (iPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasSpecialisedForm))
                iPathway.setAttributeValueNoCheck(ReactomeJavaConstants.hasSpecialisedForm,
                                                  hasComponents);
            else if (iPathway.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember))
                iPathway.setAttributeValueNoCheck(ReactomeJavaConstants.hasMember, 
                                                  hasComponents);
        }
    }

    protected void convertPropertiesForNew(GKInstance iPathway, 
                                           Renderable rPathway, 
                                           Map rToIMap) throws Exception {
        extractComponents(iPathway, rPathway, rToIMap);
        extractNames(rPathway, iPathway);
        extractAttachments(rPathway, iPathway);
        extractTaxon(rPathway, iPathway);
        extractLocalization(rPathway, iPathway);
        // Call this method before extractSummation() since literatureReference
        // will be used by summation.
        extractReference(rPathway, iPathway);
        extractSummation(rPathway, iPathway);
        extractPrecedingEvent(rPathway, iPathway, rToIMap);
    }
    
    protected void extractPrecedingEvent(Renderable rPathway,
                                         GKInstance iPathway,
                                         Map rToIMap) throws Exception {
        java.util.List precedingEvents = (java.util.List) rPathway.getAttributeValue(RenderablePropertyNames.PRECEDING_EVENT);
        if (precedingEvents == null || precedingEvents.size() == 0) {
            iPathway.setAttributeValue(ReactomeJavaConstants.precedingEvent, 
                                       null);
            return;
        }
        Renderable tmpNode;
        GKInstance tmpInstance;
        List list = new ArrayList(precedingEvents.size());
        for (Iterator it = precedingEvents.iterator(); it.hasNext();) {
            tmpNode = (Renderable) it.next();
            tmpInstance = (GKInstance) rToIMap.get(tmpNode);
            list.add(tmpInstance);
        }
        iPathway.setAttributeValue(ReactomeJavaConstants.precedingEvent,
                                   list);
    }

    public GKInstance createNew(Renderable r) throws Exception {
        GKInstance pathway = fileAdaptor.createNewInstance(ReactomeJavaConstants.Pathway);
        return pathway;
    }
}
