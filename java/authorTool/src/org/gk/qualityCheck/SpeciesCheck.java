/*
 * Created on Aug 17, 2009
 *
 */
package org.gk.qualityCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * This class is used to check species setting. If more than one species are assigned to the checked instances
 * and its contained instances, it will be flagged as wrong.
 * @author wgm
 *
 */
public abstract class SpeciesCheck extends SingleAttributeClassBasedCheck {
    
    public SpeciesCheck() {
        checkAttribute = "species";
    }
   
    protected boolean checkSpecies(GKInstance container) throws Exception {
        Set<GKInstance> contained = getAllContainedEntities(container);
        // Skip checking for shell instances
        if (containShellInstances(contained))
            return true;
        // Nothing to check if contained is empty: probably 
        // the instance is just starting to annotation or
        // container is used as a place holder
        if (contained.size() == 0)
            return true;
        // Check only for valid instances
        Set<GKInstance> tmp = new HashSet<GKInstance>(contained);
        contained.clear();
        for (GKInstance in : tmp) {
            if (in.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
                contained.add(in);
        }
        if (contained.size() == 0)
            return true;
        // Get the species setting: species should be a single value attribute
        // Species are multiple for EntitySet and Complex.
        Set<GKInstance> containedSpecies = new HashSet<GKInstance>();
        for (GKInstance comp : contained) {
            // All should be valid: no need to check if it is valid attribute for species
            List species = comp.getAttributeValuesList(ReactomeJavaConstants.species);
            // Add species to set even it is null
            if (species != null)
                containedSpecies.addAll(species);
        }
        // As with compartment, need to compare the container species setting and contained species setting
        List<GKInstance> containerSpecies = container.getAttributeValuesList(ReactomeJavaConstants.species);
        return compareSpecies(containerSpecies,
                              new ArrayList<GKInstance>(containedSpecies));
    }
    
    private boolean compareSpecies(List<GKInstance> containerValues,
                                   List<GKInstance> containedValues) {
        InstanceUtilities.sortInstances(containedValues);
        InstanceUtilities.sortInstances(containerValues);
        return containerValues.equals(containedValues);
    }
    
    @Override
    protected boolean checkInstance(GKInstance instance) throws Exception {
        return checkSpecies(instance);
    }
    
    protected void loadSpeciesAttributeVAlues(Collection<GKInstance> instances,
                                              MySQLAdaptor dba) throws Exception {
        // Have to check all kinds of PEs. Otherwise, null will be assigned
        // to the species attribute because of a bug in MySQLAdaptor.
        loadAttributes(instances,
                       ReactomeJavaConstants.GenomeEncodedEntity, 
                       ReactomeJavaConstants.species, 
                       dba);
        loadAttributes(instances,
                       ReactomeJavaConstants.Complex,
                       ReactomeJavaConstants.species,
                       dba);
        loadAttributes(instances,
                       ReactomeJavaConstants.EntitySet,
                       ReactomeJavaConstants.species,
                       dba);
        loadAttributes(instances, 
                       ReactomeJavaConstants.Polymer,
                       ReactomeJavaConstants.species,
                       dba);
        loadAttributes(instances,
                       ReactomeJavaConstants.SimpleEntity,
                       ReactomeJavaConstants.species,
                       dba);
    }

}
