/*
 * Created on Jan 11, 2007
 *
 */
package org.gk.property;

import java.awt.Point;
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
import org.gk.render.*;
import org.gk.schema.SchemaClass;

/**
 * A helper class for mapping types to GKInstance, Renderable
 * @author guanming
 *
 */
public class SearchDBTypeHelper {
    // types
    private final String types[] = new String[] {
            "Protein",
            "Complex",
            "Compound",
            "Reaction",
            "Pathway",
            "Gene",
            "RNA"
    };
    
    public SearchDBTypeHelper() {
        
    }
    
    public String[] getTypes() {
        return types;
    }
    
    public List mapTypeToReactomeCls(String type) {
//      "Protein",
//      "Complex",
//      "Compound",
//      "Reaction",
//      "Pathway",
//      "Gene",
//      "RNA"
        List clsNames = new ArrayList();
        if (type.equals("Protein") ||
            type.equals("Gene") ||
            type.equals("RNA"))
            clsNames.add(ReactomeJavaConstants.GenomeEncodedEntity);
        else if (type.equals("Complex"))
            clsNames.add(ReactomeJavaConstants.Complex);
        else if (type.equals("Compound"))
            clsNames.add(ReactomeJavaConstants.SimpleEntity);
        else if (type.equals("Reaction"))
            clsNames.add(ReactomeJavaConstants.ReactionlikeEvent);
        else if (type.equals("Pathway")) {
            clsNames.add(ReactomeJavaConstants.Pathway);
            //clsNames.add(ReactomeJavaConstants.ConceptualEvent);
        }
        return clsNames;
    }
    
    public List mapGKInstanceToRenderable(String type,
                                          List instances) {
        Map id2Renderable = new HashMap();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Renderable r = (Renderable) id2Renderable.get(instance.getDBID());
            if (r == null) {
                r = convertToRenderable(type, instance);
                id2Renderable.put(instance.getDBID(), r);
            }
            // Need pull out inputs, outputs, catalysts, inhibitors and activators
            if (r instanceof RenderableReaction)
                pulloutNodesForReaction((RenderableReaction)r,
                                        instance,
                                        id2Renderable);
        }
        return new ArrayList(id2Renderable.values());
    }
    
    private void pulloutNodesForReaction(RenderableReaction reaction,
                                         GKInstance instance,
                                         Map id2Node) {
        try {
            // Stoichiometries are not supported as of Jan 11, 2007
            List inputs = instance.getAttributeValuesList(ReactomeJavaConstants.input);
            if (inputs != null && inputs.size() > 0) {
                for (Iterator it = inputs.iterator(); it.hasNext();) {
                    GKInstance input = (GKInstance) it.next();
                    Renderable r = getReactionNode(input, id2Node);
                    reaction.addInput((Node)r);
                }
            }
            List outputs = instance.getAttributeValuesList(ReactomeJavaConstants.output);
            if (outputs != null && outputs.size() > 0) {
                for (Iterator it = outputs.iterator(); it.hasNext();) {
                    GKInstance output = (GKInstance) it.next();
                    Renderable r = getReactionNode(output, id2Node);
                    reaction.addOutput((Node)r);
                }
            }
            // catalyst
            List cas = instance.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
            if (cas != null && cas.size() > 0) {
                for (Iterator it = cas.iterator(); it.hasNext();) {
                    GKInstance ca = (GKInstance) it.next();
                    GKInstance catalyst = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                    if (catalyst == null)
                        continue;
                    Renderable r = getReactionNode(catalyst, id2Node);
                    reaction.addHelper((Node)r);
                }
            }
            // Inhibitors
            Collection regulations = instance.getReferers(ReactomeJavaConstants.regulatedEntity);
            if (regulations != null && regulations.size() > 0) {
                for (Iterator it = regulations.iterator(); it.hasNext();) {
                    GKInstance regulation = (GKInstance) it.next();
                    GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                    // Want to handle only physical entity now
                    if (!(regulator.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity)))
                        continue;
                    Renderable r = getReactionNode(regulator, id2Node);
                    if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation))
                        reaction.addInhibitor((Node)r);
                    else 
                        reaction.addActivator((Node)r);
                }
            }
        }
        catch(Exception e) {
            System.err.println("SearchDBTypeHelper.pulloutNodesForReaction(): " + e);
            e.printStackTrace();
        }
    }
    
    private Renderable getReactionNode(GKInstance instance,
                                       Map id2Node) throws Exception {
        Renderable r = (Renderable) id2Node.get(instance.getDBID());
        if (r == null) {
            Class type = guessNodeType(instance);
            r = (Renderable) type.newInstance();
            convertToRenderable(instance, r);
            id2Node.put(instance.getDBID(), r);
        }
        return r;
    }
    
    private Renderable convertToRenderable(String type,
                                           GKInstance instance) {
        Renderable r = generateRenderableFromType(type);
        return convertToRenderable(instance, r);
    }

    private Renderable convertToRenderable(GKInstance instance, 
                                           Renderable r) {
        // Need to copy display name
        r.setDisplayName(instance.getDisplayName());
        // Need a preset position for node
        r.setPosition(new Point(50, 50));
        InstanceToRenderableConverter.getPropertiesForRenderable(instance, r);
        return r;
    }
    
    public Class guessNodeType(GKInstance entity) throws Exception {
        SchemaClass cls = entity.getSchemClass();
        if (cls.isa(ReactomeJavaConstants.Complex))
            return RenderableComplex.class;
        else if (cls.isa(ReactomeJavaConstants.SimpleEntity))
            return RenderableChemical.class;
        else if (cls.isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
            // Have to check reference entity
            GKInstance referenceEntity = (GKInstance) entity.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (referenceEntity != null) {
                SchemaClass refCls = referenceEntity.getSchemClass();
                if (refCls.isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
                    refCls.isa(ReactomeJavaConstants.ReferenceGeneProduct))
                    return RenderableProtein.class;
                else if (refCls.isa(ReactomeJavaConstants.ReferenceDNASequence))
                    return RenderableGene.class;
                else if (refCls.isa(ReactomeJavaConstants.ReferenceRNASequence))
                    return RenderableRNA.class;
            }
            else
                return RenderableProtein.class; // Use the protein as the default type since it should dominate
        }
        else if (cls.isa(ReactomeJavaConstants.EntitySet)) {
            // As of December, 2013, use a single class for EntitySet
            return RenderableEntitySet.class;
//            // Get the represented PE
//            Set<GKInstance> members = getRepresentedEntities(entity);
//            if (members != null && members.size() > 0) {
//                GKInstance member = members.iterator().next();
//                // Take the first one
//                return guessNodeType(member);
//            }
        }
        return RenderableEntity.class; // Very generic
    }
    
    private Set<GKInstance> getRepresentedEntities(GKInstance set) throws Exception {
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        Set<GKInstance> next = new HashSet<GKInstance>();
        Set<GKInstance> current = new HashSet<GKInstance>();
        current.add(set);
        while (current.size() > 0) {
            for (GKInstance i : current) {
                if (!(i.getSchemClass().isa(ReactomeJavaConstants.EntitySet)))
                    rtn.add(i);
                else {
                    if (i.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                        List hasMember = i.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                        if (hasMember != null)
                            next.addAll(hasMember);
                    }
                    if (i.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                        List hasCandidate = i.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                        if (hasCandidate != null)
                            next.addAll(hasCandidate);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        return rtn;
    }
    
    private Renderable generateRenderableFromType(String type) {
        Renderable rtn = null;
        if (type.equals("Protein")) {
            rtn = new RenderableProtein();
        }
        else if (type.equals("Complex"))
            rtn = new RenderableComplex();
        else if (type.equals("Compound"))
            rtn = new RenderableChemical();
        else if (type.equals("Reaction"))
            rtn = new RenderableReaction();
        else if (type.equals("Pathway"))
            rtn = new ProcessNode();
        else if (type.equals("Gene"))
            rtn = new RenderableGene();
        else if (type.equals("RNA"))
            rtn = new RenderableRNA();
        else
            rtn = new RenderableEntity();
        return rtn;
    }
}
