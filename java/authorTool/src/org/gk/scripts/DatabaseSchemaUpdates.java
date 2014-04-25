/*
 * Created on Sep 11, 2012
 *
 */
package org.gk.scripts;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;

/**
 * Methods related to database schema update should be placed here.
 * @author gwu
 *
 */
public class DatabaseSchemaUpdates {
    
    public DatabaseSchemaUpdates() {
        
    }
    
    @Test
    public void copyAccessionToIdentifierForSO() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca", 
                                            "gk_central", 
                                            "authortool", 
                                            "T001test");
        Collection<GKInstance> instances = dba.fetchInstancesByClass(ReactomeJavaConstants.SequenceOntology);
        System.out.println("Total SequenceOntology instances: " + instances.size());
        try {
            dba.startTransaction();
            Long defaultPersonId = 140537L; // For Guanming Wu at CSHL
            GKInstance defaultIE = ScriptUtilities.createDefaultIE(dba,
                                                                   defaultPersonId,
                                                                   true);
            for (GKInstance inst : instances) {
                String accession = (String) inst.getAttributeValue(ReactomeJavaConstants.accession);
                if (accession == null)
                    continue; // Just in case
                inst.setAttributeValue(ReactomeJavaConstants.identifier,
                                       accession);
                dba.updateInstanceAttribute(inst,
                                            ReactomeJavaConstants.identifier);
                // Have to load attribute value first
                inst.getAttributeValuesList(ReactomeJavaConstants.modified);
                inst.addAttributeValue(ReactomeJavaConstants.modified, 
                                       defaultIE);
                dba.updateInstanceAttribute(inst,
                                            ReactomeJavaConstants.modified);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
        }
    }
    
}
