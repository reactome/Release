/*
 * Created on Aug 18, 2009
 *
 */
package org.gk.qualityCheck;

import java.util.Collection;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;

public class EntitySetSpeciesCheck extends SpeciesCheck {
    
    public EntitySetSpeciesCheck() {
        checkClsName = ReactomeJavaConstants.EntitySet;
        followAttributes = new String[] {
                ReactomeJavaConstants.hasMember,
                ReactomeJavaConstants.hasCandidate,
        };
    }

    @Override
    protected ResultTableModel getResultTableModel() throws Exception {
        ResultTableModel model = new ComponentTableModel();
        model.setColNames(new String[]{"Member", "Species"});
        return model;
    }
    
    @Override
    protected Set<GKInstance> filterInstancesForProject(Set<GKInstance> instances) {
        return filterInstancesForProject(instances, ReactomeJavaConstants.EntitySet);
    }
    
    protected void loadAttributes(Collection<GKInstance> instances) throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) dataSource;
        Set<GKInstance> toBeLoaded = loadEntitySetMembers(instances, dba);
        progressPane.setText("Load species...");
        loadSpeciesAttributeVAlues(toBeLoaded, dba);
    }
    
}
