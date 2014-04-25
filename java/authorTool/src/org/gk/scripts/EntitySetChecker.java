/*
 * Created on Feb 9, 2012
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.StringUtils;
import org.junit.Test;

/**
 * Do something related to EntitySet information.
 * @author gwu
 *
 */
public class EntitySetChecker {
    
    public EntitySetChecker() {
        
    }
    
    private MySQLAdaptor getDBA() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_central_020912",
                                            "root",
                                            "macmysql01");
        return dba;
    }
    
    @Test
    public void checkEntitySets() throws Exception {
        MySQLAdaptor dba = getDBA();
        Collection<?> sets = dba.fetchInstancesByClass(ReactomeJavaConstants.EntitySet);
        dba.loadInstanceAttributeValues(sets,
                                        new String[]{ReactomeJavaConstants.hasMember, ReactomeJavaConstants.hasCandidate});
        Map<GKInstance, String> setToCauses = new HashMap<GKInstance, String>();
        System.out.println("Total EntitySets: " + sets.size());
        for (Object obj : sets) {
            GKInstance inst = (GKInstance) obj;
            // Get all members
            Set<GKInstance> members = new HashSet<GKInstance>();
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasMember)) {
                List<GKInstance> values = (List<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                if (values != null)
                    members.addAll(values);
            }
            if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
                List<GKInstance> values = (List<GKInstance>) inst.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
                if (values != null)
                    members.addAll(values);
            }
            if (members.size() == 0) {
//                System.out.println(inst + " has no members!");
                continue;
            }
            if (!validateHasDomain(members)) {
                pushIntoMap(setToCauses, inst, "hasDomain");
            }
            if (!validateCoordinates(members)) {
                pushIntoMap(setToCauses, inst, "coordinates");
            }
            if (!validateHasModifiedResidues(members)) {
                pushIntoMap(setToCauses, inst, "hasModifiedResidues");
            }
        }
        System.out.println("Index\tEntitySet\tCauses");
        int index = 1;
        for (GKInstance set : setToCauses.keySet()) {
            System.out.println(index + "\t" + set + "\t" + setToCauses.get(set));
            index ++;
        }
    }

    private void pushIntoMap(Map<GKInstance, String> setToCauses,
                             GKInstance inst,
                             String newCause) {
        String cause = setToCauses.get(inst);
        if (cause == null)
            setToCauses.put(inst, newCause);
        else
            setToCauses.put(inst, cause + ", " + newCause);
    }
    
    private boolean validateHasModifiedResidues(Set<GKInstance> members) throws Exception {
        // Use text to record modification information
        List<String> allModifications = new ArrayList<String>();
        for (GKInstance member : members) {
            if (!member.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue)) {
                allModifications.add("");
                continue;
            }
            List<?> modifications = member.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            if (modifications == null || modifications.size() == 0)
                allModifications.add("");
            else {
                List<String> list = new ArrayList<String>();
                for (Object obj : modifications) {
                    GKInstance modification = (GKInstance) obj;
                    if (modification.getSchemClass().isValidAttribute(ReactomeJavaConstants.psiMod)) { 
                        List<GKInstance> psiMods = modification.getAttributeValuesList(ReactomeJavaConstants.psiMod);
                        if (psiMods == null)
                            list.add(null);
                        else
                            list.add(getText(psiMods, ":"));
                    }
                }
                Collections.sort(list);
                allModifications.add(StringUtils.join(",", list));
            }                
        }
        // Make sure only one type
        Set<String> set = new HashSet<String>(allModifications);
        if (allModifications.size() == members.size() && set.size() == 1)
            return true;
        return false;
    }
    
    private String getText(List<GKInstance> instances, String delim) {
        InstanceUtilities.sortInstances(instances);
        StringBuilder builder = new StringBuilder();
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            builder.append(inst.toString());
            if (it.hasNext())
                builder.append(delim);
        }
        return builder.toString();
    }
    
    /**
     * Check if all members have coordinates assigned.
     * @param members
     * @return
     * @throws Exception
     */
    private boolean validateCoordinates(Set<GKInstance> members) throws Exception {
        List<GKInstance> assigned = new ArrayList<GKInstance>();
        for (GKInstance member : members) {
            if (!member.getSchemClass().isValidAttribute(ReactomeJavaConstants.startCoordinate))
                continue;
            Integer start = (Integer) member.getAttributeValue(ReactomeJavaConstants.startCoordinate);
            Integer end = (Integer) member.getAttributeValue(ReactomeJavaConstants.endCoordinate);
            if (start == null && end == null)
                continue;
            assigned.add(member);
        }
        // No assigned should be fine
        return assigned.size() == 0 || assigned.size() == members.size();
    }
    
    /**
     * Check if all members have hasDomain values and same hasDomain values.
     * @param members
     * @return
     * @throws Exception
     */
    private boolean validateHasDomain(Set<GKInstance> members) throws Exception {
        List<Integer> counts = new ArrayList<Integer>();
        for (GKInstance member : members) {
            List<?> values = member.getAttributeValuesList(ReactomeJavaConstants.hasDomain);
            if (values == null)
                counts.add(0);
            else
                counts.add(values.size());
        }
        Set<Integer> set = new HashSet<Integer>(counts);
        if (counts.size() == members.size() && set.size() == 1)
            return true;
        return false;
    }
    
}
