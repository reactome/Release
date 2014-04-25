/*
 * Created on Mar 3, 2009
 *
 */
package org.gk.pathwaylayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.ClassAttributeFollowingInstruction;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

/**
 * This class is used to generate VertexSearchableTerm instances. This is ported from Imre's 
 * perl script, create_PD_VertexSerachableTerms.pl.
 * @author wgm
 *
 */
public class VertexSearchableTermGenerator {
    // Cached variables for queries
    private List<ClassAttributeFollowingInstruction> peInstructions;
    private String[] allowedPEClasses;
    private List<ClassAttributeFollowingInstruction> termInstructions;
    
    public VertexSearchableTermGenerator() {
    }
    
    /**
     * This method is used to generate VertexSearchableTerm instances for a specified PathwayDiagram instance.
     * @param pathwayDiagram
     * @return
     * @throws Exception
     */
    public Set<GKInstance> generateVertexSearchableTerms(GKInstance pathwayDiagram,
                                                         GKInstance pathway,
                                                         MySQLAdaptor dba) throws Exception {
        // Get all related vertex first
        Collection vertices = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Vertex,
                                                           ReactomeJavaConstants.pathwayDiagram,
                                                           "=",
                                                           pathwayDiagram);
        if (vertices == null || vertices.size() == 0)
            return new HashSet<GKInstance>(); // In case there is nothing in the database
        Map<String, GKInstance> termToInst = new HashMap<String, GKInstance>();
        GKInstance species = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.species);
        for (Iterator it = vertices.iterator(); it.hasNext();) {
            GKInstance vertex = (GKInstance) it.next();
            Set<GKInstance> pes = null;
            if (vertex.getSchemClass().isa(ReactomeJavaConstants.ReactionVertex))
                pes = getPEsFromReactionVertex(vertex);
            else if (vertex.getSchemClass().isa(ReactomeJavaConstants.EntityVertex))
                pes = getPEsFromEntityVertex(vertex);
            // Will not handle pathway vertex for the time being
            if (pes == null || pes.size() == 0)
                continue;
            // Get actual searchable terms
            for (GKInstance pe : pes) {
                Set<String> terms = fetchSearchableTerms(pe);
                // Do some clean-up
                for (Iterator<String> it1 = terms.iterator(); it1.hasNext();) {
                    String term = it1.next();
                    if (term.startsWith("name copied from"))
                        it.remove();
                }
                // Do some other processes
                List<String> termList = new ArrayList<String>(terms);
                for (int i = 0; i < termList.size(); i++) {
                    String term = termList.get(i);
                    if (term.matches("^\\d+$") &&
                        pe.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceDatabase) &&
                        pe.getAttributeValue(ReactomeJavaConstants.referenceDatabase) != null) {
                        GKInstance db = (GKInstance) pe.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
                        termList.set(i, db.getDisplayName() + ":" + term);
                    }
                    else if (term.length() > 255)
                        termList.set(i, term.substring(0, 255));
                }
                // Create term now
                for (String term : termList) {
                    GKInstance termInst = getTermInstance(term, 
                                                          termToInst,
                                                          species,
                                                          dba);
                    // Log these information
                    List values = termInst.getAttributeValuesList(ReactomeJavaConstants.vertex);
                    if (values == null || !values.contains(vertex)) {
                        termInst.addAttributeValue(ReactomeJavaConstants.vertex, vertex);
                    }
                    values = termInst.getAttributeValuesList(ReactomeJavaConstants.termProvider);
                    if (values == null || !values.contains(pe))
                        termInst.addAttributeValue(ReactomeJavaConstants.termProvider, pe);
                }
            }
        }
        // Last check
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        for (GKInstance termInst : termToInst.values()) {
            List values = termInst.getAttributeValuesList(ReactomeJavaConstants.vertex);
            termInst.setAttributeValue(ReactomeJavaConstants.vertexCount,
                                       values == null ? 0 : values.size());
            values = termInst.getAttributeValuesList(ReactomeJavaConstants.termProvider);
            termInst.setAttributeValue(ReactomeJavaConstants.providerCount,
                                       values == null ? 0 : values.size());
            rtn.add(termInst);
        }
        return rtn;
    }
    
    private GKInstance getTermInstance(String term,
                                       Map<String, GKInstance> termToInst,
                                       GKInstance species,
                                       MySQLAdaptor dba) throws Exception {
        GKInstance termInst = termToInst.get(term);
        if (termInst == null) {
            // Create new termInst
            termInst = new GKInstance();
            termInst.setDbAdaptor(dba);
            termInst.setSchemaClass(dba.getSchema().getClassByName(ReactomeJavaConstants.VertexSearchableTerm));
            termInst.setAttributeValue(ReactomeJavaConstants.searchableTerm, term);
            termInst.setAttributeValue(ReactomeJavaConstants._displayName, term);
            termInst.setAttributeValue(ReactomeJavaConstants.species, species);
            termToInst.put(term, termInst);
        }
        return termInst;
    }
    
    private Set<String> fetchSearchableTerms(GKInstance pe) throws Exception {
        if (termInstructions == null)
            termInstructions = createTermInstructions();
        Set<String> rtn = new HashSet<String>();
        for (ClassAttributeFollowingInstruction inst : termInstructions) {
            if (pe.getSchemClass().isa(inst.getClassName())) {
                for (Object attName : inst.getAttributes()) {
                    if (pe.getSchemClass().isValidAttribute(attName.toString())) {
                        List values = pe.getAttributeValuesList(attName.toString());
                        if (values != null) {
                            rtn.addAll(values);
                        }
                    }
                }
                // Don't terminate the loop in case pe should be touched by multiple instruction.
            }
        }
        return rtn;
    }
    
    private Set<GKInstance> getPEsFromReactionVertex(GKInstance rv) throws Exception {
        // This should be a reaction
        GKInstance rxt = (GKInstance) rv.getAttributeValue(ReactomeJavaConstants.representedInstance);
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        rtn.add(rxt);
        // Inputs
        List<GKInstance> inputs = rxt.getAttributeValuesList(ReactomeJavaConstants.input);
        Set<GKInstance> linked = new HashSet<GKInstance>();
        if (inputs != null)
            linked.addAll(inputs);
        List<GKInstance> outputs = rxt.getAttributeValuesList(ReactomeJavaConstants.output);
        if (outputs != null)
            linked.addAll(outputs);
        // Based on Imre' Perl script, other linked entities (e.g. catalysts) are not handled.
        // This should be changed in the future.
        for (GKInstance pe : linked) {
            Collection c = followPE(pe);
            if (c != null)
                rtn.addAll(c);
        }
        // Need to get the while hierarhical for reaction
        List<ClassAttributeFollowingInstruction> instructions = new ArrayList<ClassAttributeFollowingInstruction>();
        ClassAttributeFollowingInstruction instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Event,
                                                                                                null,
                                                                                                new String[] {ReactomeJavaConstants.hasEvent});
        instructions.add(instruction);
        Collection c = InstanceUtilities.followInstanceAttributes(rxt, 
                                                                  instructions,
                                                                  new String[]{ReactomeJavaConstants.Event});
        if (c != null)
            rtn.addAll(c);
        return rtn;
    }
    
    private List<ClassAttributeFollowingInstruction> createTermInstructions() {
        List<ClassAttributeFollowingInstruction> instructions = new ArrayList<ClassAttributeFollowingInstruction>();
        ClassAttributeFollowingInstruction instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.PhysicalEntity,
                                                                                                new String[] {ReactomeJavaConstants.name,
                                                                                                              ReactomeJavaConstants.shortName,
                                                                                                              ReactomeJavaConstants._displayName},
                                                                                                null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReferenceEntity,
                                                             new String[] {ReactomeJavaConstants.identifier,
                                                                           ReactomeJavaConstants.name,
                                                                           ReactomeJavaConstants.otherIdentifier},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReferenceSequence,
                                                             new String[] {ReactomeJavaConstants.secondaryIdentifier},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReferencePeptideSequence,
                                                             new String[] {ReactomeJavaConstants.variantIdentifier},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.DatabaseIdentifier,
                                                             new String[] {ReactomeJavaConstants.identifier, ReactomeJavaConstants._displayName},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.GO_MolecularFunction,
                                                             new String[] {ReactomeJavaConstants.accession,
                                                                           ReactomeJavaConstants.ecNumber,
                                                                           ReactomeJavaConstants.name},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Event,
                                                             new String[] {ReactomeJavaConstants.name},
                                                             null);
        instructions.add(instruction);
        return instructions;
    }
    
    private List<ClassAttributeFollowingInstruction> createPEInstructions() {
        List<ClassAttributeFollowingInstruction> instructions = new ArrayList<ClassAttributeFollowingInstruction>();
        ClassAttributeFollowingInstruction instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Vertex,
                                                                                                new String[] {ReactomeJavaConstants.representedInstance},
                                                                                                null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Complex,
                                                             new String[] {ReactomeJavaConstants.hasComponent},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.Polymer,
                                                             new String[] {ReactomeJavaConstants.repeatedUnit},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.EntitySet,
                                                             new String[] {ReactomeJavaConstants.hasMember},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CandidateSet,
                                                             new String[] {ReactomeJavaConstants.hasCandidate},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.SimpleEntity,
                                                             new String[] {ReactomeJavaConstants.referenceEntity},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.EntityWithAccessionedSequence,
                                                             new String[] {ReactomeJavaConstants.referenceEntity},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.ReferenceEntity,
                                                             new String[] {ReactomeJavaConstants.crossReference},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.CatalystActivity,
                                                             new String[] {ReactomeJavaConstants.activity},
                                                             null);
        instructions.add(instruction);
        instruction = new ClassAttributeFollowingInstruction(ReactomeJavaConstants.PhysicalEntity,
                                                             null,
                                                             new String[] {ReactomeJavaConstants.physicalEntity});
        instructions.add(instruction);
        return instructions;
    }
    
    private String[] getAllowedEntityClasses() {
        String[] allowedClasses = new String[] {
                ReactomeJavaConstants.PhysicalEntity,
                ReactomeJavaConstants.ReferenceEntity,
                ReactomeJavaConstants.DatabaseIdentifier,
                ReactomeJavaConstants.GO_MolecularFunction
        };
        return allowedClasses;
    }
    
    private Set<GKInstance> getPEsFromEntityVertex(GKInstance ev) throws Exception {
        Collection c = followPE(ev);
        // Convert typed set
        Set<GKInstance> rtn = new HashSet<GKInstance>();
        if (c != null && c.size() > 0) {
            for (Iterator it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                rtn.add(inst);
            }
        }
        return rtn;
    }

    private Collection followPE(GKInstance ev)
            throws InvalidAttributeException, Exception {
        if (peInstructions == null)
            peInstructions = createPEInstructions();
        if (allowedPEClasses == null)
            allowedPEClasses = getAllowedEntityClasses();
        Collection c = InstanceUtilities.followInstanceAttributes(ev,
                                                                  peInstructions,
                                                                  allowedPEClasses);
        return c;
    }
    
}
