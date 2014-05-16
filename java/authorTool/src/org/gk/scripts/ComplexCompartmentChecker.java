/*
 * Created on Apr 30, 2012
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This class is used to check complex compartments.
 * @author gwu
 *
 */
public class ComplexCompartmentChecker {
    
    public ComplexCompartmentChecker() {
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void compareComplexes() throws Exception {
        MySQLAdaptor srcDBA = new MySQLAdaptor("localhost", 
                                               "gk_current_ver43",
                                               "root",
                                               "macmysql01");
        MySQLAdaptor targetDBA = new MySQLAdaptor("localhost", 
                                                  "gk_central_021213",
                                                  "root",
                                                  "macmysql01");
        GKInstance human = targetDBA.fetchInstance(48887L);
        Collection<GKInstance> complexes = targetDBA.fetchInstanceByAttribute(ReactomeJavaConstants.Complex,
                                                                              ReactomeJavaConstants.species, 
                                                                              "=", 
                                                                              human);
        int count = 0;
        System.out.println("DB_ID\tDisplayName\tRelease43_components\tgk_central_components(02.12.12)");
        for (GKInstance inst : complexes) {
            GKInstance srcComplex = srcDBA.fetchInstance(inst.getDBID());
            // Check if there is any complex component change
            if (srcComplex == null)
                continue;
            // It is possible the class type may be changed
            if (!srcComplex.getSchemClass().isa(ReactomeJavaConstants.Complex))
                continue;
            // A very simple test
            GKInstance srcSpecies = (GKInstance) srcComplex.getAttributeValue(ReactomeJavaConstants.species);
            if (!srcSpecies.getDBID().equals(human.getDBID())) // This may be a predicted complex
                continue;
            List<GKInstance> srcComps = srcComplex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            List<GKInstance> targetComps = inst.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            List<Long> srcDBIDs = getIds(srcComps);
            List<Long> targetDBIDs = getIds(targetComps);
//            if (!srcDBIDs.equals(targetDBIDs)) {
            if (srcDBIDs.size() > targetDBIDs.size()) {
                System.out.println(inst.getDBID() + "\t" + 
                                   inst.getDisplayName() + "\t" + 
                                   generateTextForList(srcComps) + "\t" + 
                                   generateTextForList(targetComps));
                count ++;
            }
        }
        System.out.println("Total: " + count);
    }
    
    private List<Long> getIds(List<GKInstance> instances) {
        List<Long> rtn = new ArrayList<Long>();
        for (GKInstance inst : instances)
            rtn.add(inst.getDBID());
        Collections.sort(rtn);
        return rtn;
    }
    
    private String generateTextForList(List<GKInstance> instances) {
        StringBuilder builder = new StringBuilder();
        InstanceUtilities.sortInstances(instances);
        for (GKInstance inst : instances) {
            builder.append(inst.getDisplayName());
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
    
    private MySQLAdaptor getDBA() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "gk_central",
                                            "authortool",
                                            "T001test");
        return dba;
    }
    
    
    @Test
    public void checkComplexCompartments() throws Exception {
        MySQLAdaptor dba = getDBA();
        Map<Long, Boolean> complexIdToFix = loadCompartmentList();
        int total = 0;
        int toBeFixed = 0;
        for (Long dbId : complexIdToFix.keySet()) {
            GKInstance complex = dba.fetchInstance(dbId);
            if (complex == null) {
                System.out.println(dbId + " doesn't exist!");
                continue;
            }
            List<?> compartments = complex.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (compartments != null && compartments.size() > 1) {
                System.out.println(complex + " has more than one compartment: " + compartments.size());
                total ++;
                if (complexIdToFix.get(dbId)) {
                    System.out.println("\tWill be fixed!");
                    toBeFixed ++;
                }
            }
        }
        System.out.println("Total complexes: " + total);
        System.out.println("\tToBeFixed: " + toBeFixed);
    }
    
    private Map<Long, Boolean> loadCompartmentList() throws Exception {
        String fileName = "/Users/gwu/Documents/gkteam/Lisa/multicompartment_complexes_for_review%20-%20multicompartment_complexes_for_.tsv";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = fu.readLine();
        Map<Long, Boolean> complexToFix = new HashMap<Long, Boolean>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            complexToFix.put(new Long(tokens[0]),
                             tokens[5].toLowerCase().equals("yes") ? Boolean.TRUE : Boolean.FALSE);
        }
        fu.close();
        return complexToFix;
    }
    
}
