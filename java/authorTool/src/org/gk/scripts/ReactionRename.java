/*
 * Created on Oct 23, 2012
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This script is used to rename reactions.
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class ReactionRename {
    
    public ReactionRename() {
        
    }
    
    private MySQLAdaptor getDBA() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_central_102312",
                                            "root",
                                            "macmysql01");
        return dba;
    }
    
    @Test
    public void renameReactions() throws Exception {
        MySQLAdaptor dba = getDBA();
        // Want to check human reactions only
        GKInstance homosapiens = dba.fetchInstance(48887L);
        Collection<GKInstance> reactions = dba.fetchInstanceByAttribute(ReactomeJavaConstants.ReactionlikeEvent,
                                                                        ReactomeJavaConstants.species,
                                                                        "=",
                                                                        homosapiens);
        String[] attributes = new String[] {
                ReactomeJavaConstants.input,
                ReactomeJavaConstants.output,
                ReactomeJavaConstants.catalystActivity
        };
        dba.loadInstanceAttributeValues(reactions, attributes);
        String fileName = "tmp/ReactionRenaming_102612.txt";
        FileUtilities fu = new FileUtilities();
        fu.setOutput(fileName);
        fu.printLine("DB_ID\tClass\tDisplayName\tReaction_Type\tRename");
        for (GKInstance reaction : reactions) {
            ReactionType type = guessReactionType(reaction);
            String name = generateReactionName(reaction, type);
//            if (type == ReactionType.transform)
//                System.out.println(reaction + "\t" + type + "\t" + name);
            fu.printLine(reaction.getDBID() + "\t" + 
                         reaction.getSchemClass().getName() + "\t" + 
                         reaction.getDisplayName() + "\t" + 
                         type + "\t" + 
                         name);
        }
        fu.close();
    }
    
    private String generateReactionName(GKInstance reaction,
                                        ReactionType type) throws Exception {
        switch (type) {
            case activate:
                return getActivationName(reaction);
            case bind:
                return getBindingName(reaction);
            case dissociate:
                return getDissociationName(reaction);
            case depolymerize:
                return getDeploymerizationName(reaction);
            case polymerize:
                return getPolymerizationName(reaction);
            case transport:
                return getTransportationName(reaction);
            case transform:
                return getTransformName(reaction);
            default:
                return reaction.getDisplayName();
        }
    }
    
    private String getTransformName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        String inputEntityName = concatenateInstances(inputs, false);
        String outputEntityName = concatenateInstances(outputs, false);
        if (inputs == null || outputs == null || inputs.size() == 0 || outputs.size() == 0) {
            if (inputs != null && inputs.size() > 0) {
                return inputEntityName + (inputs.size() == 1 ? " is " : " are ") + "consumed";
            }
            if (outputs != null && outputs.size() > 0) {
                return outputEntityName + (outputs.size() == 1 ? " is " : " are ") + "produced";
            }
            return null;
        }
        String forceName = getPositiveForceName(reaction, false);
        StringBuilder builder = new StringBuilder();
        if (forceName != null) {
            builder.append(forceName).append(" transforms ");
            builder.append(inputEntityName);
            builder.append(" to ");
            builder.append(outputEntityName);
        }
        else {
            builder.append(inputEntityName);
            if (inputs == null || inputs.size() <= 1)
                builder.append(" is");
            else
                builder.append(" are");
            builder.append(" transformed to ");
            builder.append(outputEntityName);
        }
        return builder.toString();
    }
    
    /**
     * The implementation of this method is rather complicated: first it has to find
     * which entity or entities have been transported, then find transported 
     * compartments, and also needs to check cell types in case the compartment is
     * the same.
     * @param reaction
     * @return
     * @throws Exception
     */
    private String getTransportationName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        // Try to find which input(s) have been transported by check compartments
        Set<GKInstance> inputCompartmentSet = new HashSet<GKInstance>();
        Set<GKInstance> outputCompartmentSet = new HashSet<GKInstance>();
        Set<GKInstance> inputCellTypes = new HashSet<GKInstance>();
        Set<GKInstance> outputCellTypes = new HashSet<GKInstance>();
        Set<GKInstance> transported = grepTransprotedEntities(inputs, 
                                                              outputs, 
                                                              inputCompartmentSet, 
                                                              outputCompartmentSet, 
                                                              inputCellTypes, 
                                                              outputCellTypes);
        if (transported.size() > 0) {
            String entityNames = concatenateInstances(transported, false);
            String inputCompartmentNames = concatenateInstances(inputCompartmentSet, true);
            String outputCompartmentNames = concatenateInstances(outputCompartmentSet, true);
            StringBuilder builder = new StringBuilder();
            builder.append(entityNames);
            if (transported.size() == 1)
                builder.append(" is translocated from [");
            else
                builder.append(" are translocated from [");
            builder.append(inputCompartmentNames);
            if (inputCellTypes.size() == 0)
                builder.append("]");
            else
                builder.append(" of ").append(concatenateInstances(inputCellTypes, true)).append("]");
            builder.append(" to [").append(outputCompartmentNames);
            if (outputCellTypes.size() == 0)
                builder.append("]");
            else
                builder.append(" of ").append(concatenateInstances(outputCellTypes, true)).append("]");
            // Need to check if there is any CAS or PositiveRegulation related to it
            String forceName = getPositiveForceName(reaction, true);
            if (forceName != null)
                builder.append(" by ").append(forceName);
            return builder.toString();
        }
//        return null;
        throw new IllegalArgumentException("Cannot find a transproted entity for " + reaction);
    }
    
    private Set<GKInstance> grepTransprotedEntities(List<GKInstance> inputs,
                                                    List<GKInstance> outputs,
                                                    Set<GKInstance> inputCompartmentSet,
                                                    Set<GKInstance> outputCompartmentSet,
                                                    Set<GKInstance> inputCellTypes,
                                                    Set<GKInstance> outputCellTypes) throws Exception {
        Set<GKInstance> transported = new HashSet<GKInstance>();
        for (GKInstance input : inputs) {
            List<GKInstance> compartments = input.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (compartments == null || compartments.size() == 0)
                continue;
            Set<GKInstance> refEntities = InstanceUtilities.grepReferenceEntitiesForPE(input);
            if (refEntities == null || refEntities.size() == 0)
                continue; // Cannot determine if input and output are the same
            for (GKInstance output : outputs) {
                List<GKInstance> outputCompartments = output.getAttributeValuesList(ReactomeJavaConstants.compartment);
                if (outputCompartments == null || outputCompartments.size() == 0)
                    continue;
                Set<GKInstance> outputRefentities = InstanceUtilities.grepReferenceEntitiesForPE(output);
                if (refEntities.equals(outputRefentities)) {
                    List<GKInstance> copy = new ArrayList<GKInstance>(outputCompartments);
                    copy.removeAll(compartments);
                    if (copy.size() > 0) { // Means matched input and output have different compartment
                        transported.add(input);
                        if (inputCompartmentSet != null)
                            inputCompartmentSet.addAll(compartments);
                        if (outputCompartmentSet != null)
                            outputCompartmentSet.addAll(outputCompartments);
                    }
                    else { // Check if cell type is used
                        List<GKInstance> cellTypes1 = input.getAttributeValuesList(ReactomeJavaConstants.cellType);
                        List<GKInstance> cellTypes2 = output.getAttributeValuesList(ReactomeJavaConstants.cellType);
                        if (cellTypes1 != null && cellTypes2 != null && cellTypes1.size() > 0 && cellTypes2.size() > 0) {
                            copy = new ArrayList<GKInstance>(cellTypes1);
                            copy.removeAll(cellTypes2);
                            if (copy.size() > 0) { // Means cell types have been changed
                                transported.add(input);
                                if (inputCellTypes != null)
                                    inputCellTypes.addAll(cellTypes1);
                                if (outputCellTypes != null)
                                    outputCellTypes.addAll(cellTypes2);
                                if (inputCompartmentSet != null)
                                    inputCompartmentSet.addAll(compartments);
                                if (outputCompartmentSet != null)
                                    outputCompartmentSet.addAll(outputCompartments);
                            }
                        }
                    }
                    break;
                }
            }
        }
        return transported;
    }
    
    private String getDeploymerizationName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        String inputName = concatenateInstances(inputs, false);
        String outputName = concatenateInstances(outputs, false);
        if (inputs.size() == 1)
            return inputName + " depolymerizes to " + outputName;
        else
            return inputName + " depolymerize to " + outputName;
    }
    
    private String getPolymerizationName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        String inputName = concatenateInstances(inputs, false);
        String outputName = concatenateInstances(outputs, false);
        if (inputs.size() == 1)
            return inputName + " polymerizes to " + outputName;
        else
            return inputName + " polymerize to " + outputName;
    }
    
    private String getDissociationName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        String inputName = concatenateInstances(inputs, false);
        String outputName = concatenateInstances(outputs, false);
        if (inputs.size() == 1)
            return inputName + " dissociates to " + outputName;
        return inputName + " dissociate to " + outputName;
    }
    
    private String getBindingName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        String inputName = concatenateInstances(inputs, false);
        String outputName = concatenateInstances(outputs, false);
        return inputName + " bind to form " + outputName;
    }
    
    private String getActivationName(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        String entityNames = concatenateInstances(inputs, false);
        // Check if there is a catalystActivity
        String forceName = getPositiveForceName(reaction, false);
        if (forceName == null)
            return "Activate " + entityNames;
        else
            return forceName + " activates " + entityNames;
    }
    
    private String getPositiveForceName(GKInstance reaction,
                                        boolean needPositiveRegulation) throws Exception {
        GKInstance cas = (GKInstance) reaction.getAttributeValue(ReactomeJavaConstants.catalystActivity);
        if (cas != null) {
            String casName = getCatalystActivityName(cas);
            return casName;
        }
        // Check if there is a positive regulation for it
        Collection<GKInstance> regulations = reaction.getReferers(ReactomeJavaConstants.regulatedEntity);
        if (regulations != null) {
            Set<GKInstance> regulators = new HashSet<GKInstance>();
            for (GKInstance regulation : regulations) {
                if (!regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation))
                    continue;
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator != null)
                    regulators.add(regulator);
            }
            if (regulators.size() > 0) {
                String regualtorNames = concatenateInstances(regulators, false);
                return regualtorNames;
            }
        }
        return null;
    }
    
    private String getCatalystActivityName(GKInstance cas) throws Exception {
        GKInstance goMf = (GKInstance) cas.getAttributeValue(ReactomeJavaConstants.activity);
        GKInstance catalyst = (GKInstance) cas.getAttributeValue(ReactomeJavaConstants.physicalEntity);
        StringBuilder builder = new StringBuilder();
        if (goMf != null) {
            builder.append("The ").append(goMf.getDisplayName());
        }
        if (catalyst != null) {
            if (builder.length() > 0)
                builder.append(" of ");
            String catalystName = catalyst.getDisplayName();
            int index = catalystName.indexOf("[");
            if (index > 0)
                catalystName = catalystName.substring(0, index).trim();
            builder.append(catalystName);
        }
        if (builder.length() == 0)
            return cas.getDisplayName();
        return builder.toString();
    }
    
    private String concatenateInstances(Collection<GKInstance> instances,
                                        boolean withCompartment) {
        // Clean up the collection a little bit to avoid duplication (e.g. 2 or more
        // instances in inputs or outputs)
        // Avoid using set in order to keep the order of the original list passed.
        List<GKInstance> list = new ArrayList<GKInstance>();
        for (GKInstance inst : instances) {
            if (list.contains(inst))
                continue;
            list.add(inst);
        }
        StringBuilder builder = new StringBuilder();
        int c = 0;
        for (GKInstance instance : list) {
            String name = instance.getDisplayName();
            if (!withCompartment) {
                int index = name.lastIndexOf("[");
                if (index > 0)
                    name = name.substring(0, index).trim();
            }
            builder.append(name);
            if (c < list.size() - 2)
                builder.append(", ");
            else if (c == list.size() - 2) {
                if (list.size() == 2)
                    builder.append(" and ");
                else
                    builder.append(", and ");
            }
            c ++;
        }
        return builder.toString();
    }
    
    private ReactionType guessReactionType(GKInstance reaction) throws Exception {
        if (reaction.getSchemClass().isa(ReactomeJavaConstants.Polymerisation))
            return ReactionType.polymerize;
        if (reaction.getSchemClass().isa(ReactomeJavaConstants.Depolymerisation))
            return ReactionType.depolymerize;
        if (isBinding(reaction))
            return ReactionType.bind;
        if (isDissociation(reaction))
            return ReactionType.dissociate;
        if (isTransportation(reaction))
            return ReactionType.transport;
        if (isActivation(reaction))
            return ReactionType.activate;
        // Default should be transform
        return ReactionType.transform;
    }
    
    private boolean isActivation(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (inputs == null || outputs == null || inputs.size() == 0 || outputs.size() == 0)
            return false;
        Set<GKInstance> inputSet = new HashSet<GKInstance>(inputs);
        Set<GKInstance> outputSet = new HashSet<GKInstance>(outputs);
        if (inputSet.equals(outputSet))
            return true;
        return false;
    }
    
    private boolean isTransportation(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        if (inputs == null || outputs == null ||
            inputs.size() == 0 || outputs.size() == 0)
            return false;
        // Make sure the compartments between inputs and ouputs are not the same
        Set<GKInstance> inputComparts = new HashSet<GKInstance>();
        for (GKInstance input : inputs) {
            List<GKInstance> comparts = input.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (comparts == null)
                continue;
            inputComparts.addAll(comparts);
        }
        Set<GKInstance> outputComparts = new HashSet<GKInstance>();
        for (GKInstance output : outputs) {
            List<GKInstance> comparts = output.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (comparts == null)
                continue;
            outputComparts.addAll(comparts);
        }
        if (inputComparts.size() == 0 || outputComparts.size() == 0)
            return false;
        if (inputComparts.equals(outputComparts)) {
            // Need to check if inputs and outputs have different cell types
            Set<GKInstance> inputCellTypes = new HashSet<GKInstance>();
            for (GKInstance input : inputs) {
                List<GKInstance> cellTypes = input.getAttributeValuesList(ReactomeJavaConstants.cellType);
                if (cellTypes != null)
                    inputCellTypes.addAll(cellTypes);
            }
            Set<GKInstance> outputCellTypes = new HashSet<GKInstance>();
            for (GKInstance output : outputs) {
                List<GKInstance> cellTypes = output.getAttributeValuesList(ReactomeJavaConstants.cellType);
                if (cellTypes != null)
                    outputCellTypes.addAll(cellTypes);
            }
            if (inputCellTypes.size() > 0 && outputCellTypes.size() > 0 && 
                !inputCellTypes.equals(outputCellTypes))
                return true; // This is a specific transportation
            return false;
        }
        Set<GKInstance> transported = grepTransprotedEntities(inputs, outputs, null, null, null, null);
        return transported.size() > 0;
    }
    
    /**
     * This check is exactly opposite to bind
     * @param reaction
     * @return
     * @throws Exception
     */
    private boolean isDissociation(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        // Make sure there are more outputs than inputs
        if (outputs != null && inputs != null && // Make sure there is something there
            outputs.size() > 0 && inputs.size() > 0 &&
            outputs.size() > inputs.size()) {
            // Have to make sure there is at least one complex
            // However, the check that at least one complex whose components should be in the
            // input list is not correct since some input may change its compartment
            List<GKInstance> complexes = new ArrayList<GKInstance>();
            for (GKInstance input : inputs) {
                if (isComplexType(input))
                    complexes.add(input);
            }
            // At least one complex is required
            if (complexes.size() > 0)
                return true;
            return false;
        }
        return false;
    }
    
    /**
     * Ploymerization type is not considered here. It should be taken care already.
     * @param reaction
     * @return
     * @throws Exception
     */
    private boolean isBinding(GKInstance reaction) throws Exception {
        List<GKInstance> inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List<GKInstance> outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        // Make sure there are more outputs than inputs
        if (outputs != null && inputs != null && // Make sure there is something there
            outputs.size() > 0 && inputs.size() > 0 &&
            outputs.size() < inputs.size()) {
            // Have to make sure there is at least one complex
            // However, the check that at least one complex whose components should be in the
            // input list is not correct since some input may change its compartment
            List<GKInstance> complexes = new ArrayList<GKInstance>();
            for (GKInstance output : outputs) {
                if (isComplexType(output))
                    complexes.add(output);
            }
            // At least one complex is required
            if (complexes.size() > 0)
                return true;
            return false;
        }
        return false;
    }
    
    private boolean isComplexType(GKInstance instance) throws Exception {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex))
            return true;
        // This may be a complex entity
        if (instance.getSchemClass().isa(ReactomeJavaConstants.EntitySet)) {
            Set<GKInstance> members = InstanceUtilities.getContainedInstances(instance,
                                                                              ReactomeJavaConstants.hasMember,
                                                                              ReactomeJavaConstants.hasCandidate);
            for (GKInstance member : members) {
                if (member.getSchemClass().isa(ReactomeJavaConstants.Complex))
                    return true;
            }
        }
        return false;
    }
    
    private enum ReactionType {
        transform,
        bind,
        dissociate,
        transport,
        polymerize,
        depolymerize,
        activate
    }
    
}
