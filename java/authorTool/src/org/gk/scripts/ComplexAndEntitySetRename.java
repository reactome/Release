/*
 * Created on Jun 18, 2013
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This class is used to rename Complex and EntitySet.
 * @author gwu
 *
 */
@SuppressWarnings("unchecked")
public class ComplexAndEntitySetRename extends PhysicalEntityRename {
    private final int MAX_NAME_LENGTH = 35;
    private final boolean FIRST_LEVEL = false;
    // Record renamed PEs (Complex or EntitySet) so that
    // they don't need to be renamed in a recursive method
    private Map<GKInstance, String> peToNewName;
    
    public ComplexAndEntitySetRename() {
    }
    
    /**
     * If a complex contains no-complex and no-entityset, it is regarded as first level
     * complex.
     * @param complex
     * @return
     * @throws Exception
     */
    private boolean isFirstLevelComplex(GKInstance complex) throws Exception {
        List<GKInstance> components = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        if (components == null || components.size() == 0)
            return true;
        for (GKInstance comp : components) {
            if (comp.getSchemClass().isa(ReactomeJavaConstants.Complex) ||
                comp.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                return false;
        }
        return true;
    }
    
    @Test
    public void checkComplexes() throws Exception {
        peToNewName = new HashMap<GKInstance, String>();
//        String fileName = DIR_NAME + "ComplexRename_061813.txt";
//        String fileName = DIR_NAME + "ComplexRename_with_length_061813.txt";
//        String fileName = DIR_NAME + "ComplexRename_062713.txt";
        String fileName = DIR_NAME + "ComplexRename_MaxLength_071113.txt";
//        String fileName = DIR_NAME + "ComplexRename_MaxLength_FirstLevel_071113.txt";
        MySQLAdaptor dba = getDBA();
        // For human complexes only
        GKInstance human = dba.fetchInstance(48887L);
        Collection<GKInstance> complexes = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Complex,
                                                                        ReactomeJavaConstants.species,
                                                                        "=",
                                                                        human);
        System.out.println("Total human complexes: " + complexes.size());
        // Load hasComponent values
        dba.loadInstanceAttributeValues(complexes, 
                                        new String[]{ReactomeJavaConstants._displayName, ReactomeJavaConstants.hasComponent});
        // Get a list of all components
        Set<GKInstance> pes = new HashSet<GKInstance>();
        Set<GKInstance> entitySets = new HashSet<GKInstance>();
        Set<GKInstance> candidateSets = new HashSet<GKInstance>();
        for (GKInstance complex : complexes) {
            List<GKInstance> list = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            pes.addAll(list);
            for (GKInstance subunit : list) {
                if (subunit.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
                    entitySets.add(subunit);
                if (subunit.getSchemClass().isa(ReactomeJavaConstants.CandidateSet))
                    candidateSets.add(subunit);
            }
        }
        dba.loadInstanceAttributeValues(pes, 
                                        new String[]{ReactomeJavaConstants.name});
        dba.loadInstanceAttributeValues(entitySets, 
                                        new String[]{ReactomeJavaConstants.hasMember});
        dba.loadInstanceAttributeValues(candidateSets, 
                                        new String[]{ReactomeJavaConstants.hasCandidate});
        List<GKInstance> complexList = new ArrayList<GKInstance>(complexes);
        InstanceUtilities.sortInstances(complexList);
        FileUtilities fu = new FileUtilities();
        fu.setOutput(fileName);
        fu.printLine("DB_ID\tTotal_Component\tCurrent_DisplayName\tNew_Name_Length\tNew_DisplayName\tIs_Changed");
        int count = 0;
        for (GKInstance complex : complexList) {
            if (FIRST_LEVEL && !isFirstLevelComplex(complex))
                continue;
            System.out.println(count + ": " + complex);
            String newName = getComplexNewName(complex);
            List<GKInstance> subunits = getContainedInstances(complex,
                                                              ReactomeJavaConstants.hasComponent);
            String newDisplayName = attachCompartment(complex.getDisplayName(), newName);
            fu.printLine(complex.getDBID() + "\t" + 
                    new HashSet<GKInstance>(subunits).size() + "\t" +
                    complex.getDisplayName() + "\t" +
                    getNameLength(newName) + "\t" +
                    newDisplayName + "\t" + 
                    !newDisplayName.equals(complex.getDisplayName()));
            count ++;
        }
        fu.close();
    }
    
    private int getNameLength(String name) {
        int index = name.lastIndexOf("[");
        if (index <= 0)
            return name.length();
        String tmpName = name.substring(0, index).trim();
        return tmpName.length();
    }
    
    private String getComplexNewName(GKInstance complex) throws Exception {
        String newName = peToNewName.get(complex);
        if (newName != null)
            return newName;
        List<GKInstance> subunits = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        newName = getComplexNewName(subunits);
        if (newName == null || newName.length() > MAX_NAME_LENGTH)
            newName = (String) complex.getAttributeValue(ReactomeJavaConstants.name);
        peToNewName.put(complex, newName);
        return newName;
    }
    
    private String getEntitySetName(GKInstance entitySet) throws Exception {
        String newName = peToNewName.get(entitySet);
        if (newName != null)
            return newName;
        List<GKInstance> members = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
        List<GKInstance> candidates = new ArrayList<GKInstance>();
        if (entitySet.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasCandidate)) {
            candidates = entitySet.getAttributeValuesList(ReactomeJavaConstants.hasCandidate);
        }
        newName = getEntitySetName(members, candidates);
        if (newName == null || newName.length() > MAX_NAME_LENGTH)
            newName = (String) entitySet.getAttributeValue(ReactomeJavaConstants.name);
        peToNewName.put(entitySet, newName);
        return newName;
    }
    
    private String getEntitySetName(List<GKInstance> members,
                                    List<GKInstance> candidates) throws Exception {
        if (members.size() == 0 && candidates.size() == 0)
            return null;
        String newName = "";
        if (members.size() > 0)
            newName = createNameFromComps(members, ",");
        if (candidates.size() > 0) {
            String candidateName = createNameFromComps(candidates, ",");
            newName = newName + ",(" + candidateName + ")";
        }
        return newName;
    }
    
    private String getComplexNewName(final List<GKInstance> subunits) throws Exception {
        if (subunits.size() == 0) {
//            return "NO_SUBUNIT_IN_COMPLEX";
            return null;
        }
        String newName = createNameFromComps(subunits, ":");
        return newName;
    }

    private String attachCompartment(String oldName, String newName) {
        // Check if any comparment there
        int index = oldName.indexOf("[");
        if (index > 0) {
            newName = newName + " " + oldName.substring(index);
        }
        return newName;
    }

    private String createNameFromComps(final List<GKInstance> subunits,
                                       String delimit) throws InvalidAttributeException, Exception {
        Map<GKInstance, Integer> subunitToCount = countSubunits(subunits);
        List<GKInstance> subunitList = new ArrayList<GKInstance>(subunitToCount.keySet());
        // We want to keep the original order in the attribute values
        Collections.sort(subunitList, new Comparator<GKInstance>() {
            public int compare(GKInstance inst1, GKInstance inst2) {
                int index1 = subunits.indexOf(inst1);
                int index2 = subunits.indexOf(inst2);
                return index1 - index2;
            }
        });
        StringBuilder builder = new StringBuilder();
        for (GKInstance inst : subunitList) {
            Integer count = subunitToCount.get(inst);
            String name = getEntityName(inst);
            if (count > 1)
                builder.append(count).append("x").append(name);
            else
                builder.append(name);
            builder.append(delimit);
        }
        String newName = builder.substring(0, builder.length() - 1); // Remove the last delimit
        return newName;
    }
    
    private String getEntityName(GKInstance comp) throws Exception {
        if (comp.getSchemClass().isa(ReactomeJavaConstants.Complex))
            return getComplexNewName(comp);
        if (comp.getSchemClass().isa(ReactomeJavaConstants.EntitySet))
            return getEntitySetName(comp);
        return (String) comp.getAttributeValue(ReactomeJavaConstants.name);
    }
    
    private Map<GKInstance, Integer> countSubunits(List<GKInstance> subunits) {
        Map<GKInstance, Integer> instToCount = new HashMap<GKInstance, Integer>();
        for (GKInstance subunit : subunits) {
            Integer count = instToCount.get(subunit);
            if (count == null)
                instToCount.put(subunit, 1);
            else
                instToCount.put(subunit, ++count);
        }
        return instToCount;
    }
    
    /**
     * Get all contained instance in a list so that duplications can be counted too.
     * @param container
     * @param attName
     * @return
     * @throws Exception
     */
    private List<GKInstance> getContainedInstances(GKInstance container, 
                                                  String attName) throws Exception {
        List<GKInstance> list = new ArrayList<GKInstance>();
        List<GKInstance> current = new ArrayList<GKInstance>();
        current.add(container);
        List<GKInstance> next = new ArrayList<GKInstance>();
        while (current.size() > 0) {
            for (GKInstance tmp : current) {
                list.add(tmp);
                if (tmp.getSchemClass().isValidAttribute(attName)) {
                    List<GKInstance> values = tmp.getAttributeValuesList(attName);
                    if (values != null)
                        next.addAll(values);
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        // Don't need the container itself.
        list.remove(container);
        return list;
    }
    
}
