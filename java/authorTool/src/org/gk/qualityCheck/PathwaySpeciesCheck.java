/*
 * Created on Aug 18, 2009
 *
 */
package org.gk.qualityCheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

/**
 * This class is used to do species check on Pathway instances.
 * @author wgm
 *
 */
public class PathwaySpeciesCheck extends SpeciesCheck {
    
    public PathwaySpeciesCheck() {
        checkClsName = ReactomeJavaConstants.Pathway;
        followAttributes = new String[] {
                ReactomeJavaConstants.hasEvent
        };
    }
    
    @Override
    protected ResultTableModel getResultTableModel() throws Exception {
        ResultTableModel model = new ComponentTableModel();
        String[] colNames = new String[] {
                "ContainedEvent",
                "Species"
        };
        model.setColNames(colNames);
        return model;
    }
    
    @Override
    protected Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances) {
        return filterInstancesForProject(instances, ReactomeJavaConstants.Pathway);
    }
    
    @Override
    protected void loadAttributes(Collection<GKInstance> instances)
            throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) dataSource;
        // Need to load all complexes in case some complexes are used by complexes for checking
        progressPane.setText("Load Pathway attribute...");
        loadAttributes(ReactomeJavaConstants.Pathway, 
                       ReactomeJavaConstants.hasEvent, 
                       dba);
        Set<GKInstance> toBeLoaded = new HashSet<GKInstance>();
        toBeLoaded.addAll(instances);
        for (GKInstance pathway : instances) {
            if (!pathway.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                continue;
            }
            Set<GKInstance> contained = InstanceUtilities.getContainedEvents(pathway);
            toBeLoaded.addAll(contained);
        }
        progressPane.setText("Load Event species...");
        loadAttributes(toBeLoaded,
                       ReactomeJavaConstants.Event,
                       ReactomeJavaConstants.species,
                       dba);
    }
    
}
