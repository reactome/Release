/*
 * Created on Mar 31, 2008
 *
 */
package org.gk.qualityCheck;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * The logic for Complex.compartment check is implmeneted as following:
 * 0). The complex should have no-empty compartment value.
 * 1). There should be only one compartment value in Complex though the value itself is a multi-valued
 * attribute
 * 2). The value of an Complex should be one of its subunits' compartment values, which may have more than
 * one.
 * 3). There should not be more than two compartment values in all subunits. If two compartments in
 * subunits, these two compartments should be adjacent.
 * 4). includedLocation is not checked.
 * @author gwu
 *
 */
public class ComplexCompartmentCheck extends CompartmentCheck {
    protected final String NEIGHBOR_FILE_NAME = "AdjacentCompartments.txt";
    
    public ComplexCompartmentCheck() {
        checkClsName = ReactomeJavaConstants.Complex;
        followAttributes = new String[] {
                ReactomeJavaConstants.hasComponent
        };
    }
    
    protected void loadAttributes(Collection<GKInstance> instances) throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) dataSource;
        Set<GKInstance> toBeLoaded = loadComplexHasComponent(instances,
                                                             dba);
        if (toBeLoaded == null)
            return;
        // No need to load EntitySet's component information. They should be handled
        // by EntitySet compartment checking.
        progressPane.setText("Load PhysicalEntity compartment...");
        loadAttributes(toBeLoaded,
                       ReactomeJavaConstants.PhysicalEntity,
                       ReactomeJavaConstants.compartment,
                       dba);
    }
    
    @Override
    protected Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances) {
        return filterInstancesForProject(instances, ReactomeJavaConstants.Complex);
    }
    
    /**
     * For EntitySet, compartments used by members should be the same as
     * its container, EntitySet instance.
     * @param containedCompartments
     * @param containerCompartments
     * @return
     */
    protected boolean compareCompartments(Set containedCompartments,
                                          List containerCompartments) throws Exception {
        if (containedCompartments.size() > 2 ||
            containerCompartments.size() > 1)
            return false;
        // To make compare easier
        if (containerCompartments == null || containerCompartments.size() == 0)
            return false;
//            containerCompartments = EMPTY_LIST;
//        // Components and complex should have the same numbers of compartments used.
//        if (containerCompartments.size() != containedCompartments.size())
//            return false;
        // Make sure two compartments are adjacent if there are two compartments
        if (containedCompartments.size() == 2) {
            // Both of compartments should be listed 
            Iterator it = containedCompartments.iterator();
            GKInstance compartment1 = (GKInstance) it.next();
            GKInstance compartment2 = (GKInstance) it.next();
            Map neighborMap = getNeighbors();
            List neighbors = (List) neighborMap.get(compartment1.getDBID());
            if (neighbors == null ||
                !neighbors.contains(compartment2.getDBID()))
                return false; // The used two compartments are not adjacent. This should be an error.
            if (!containedCompartments.contains(containerCompartments.get(0))) {
                return false; // The sole container value should be one of subunit values.
            }
//            if (!containerCompartments.contains(compartment1) ||
//                !containerCompartments.contains(compartment2))
//                return false; // At least one of compartment used by component is not listed.
        }
        else if (containedCompartments.size() == 1) { // There should be only one compartment used by compartment
            GKInstance componentCompartment = (GKInstance) containedCompartments.iterator().next();
            GKInstance complexCompartment = (GKInstance) containerCompartments.get(0);
            if (componentCompartment != complexCompartment)
                return false;
        }
        // No compartment is setting should be returned as true in this checking.
        return true;
    }
    
    protected ResultTableModel getResultTableModel() {
        ResultTableModel tableModel = new ComponentTableModel();
        tableModel.setColNames(new String[] {"Component", "Compartment"});
        return tableModel;
    }
}
