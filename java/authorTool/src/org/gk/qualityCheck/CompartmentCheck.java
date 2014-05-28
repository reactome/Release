/*
 * Created on Aug 17, 2009
 *
 */
package org.gk.qualityCheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.util.GKApplicationUtilities;

public abstract class CompartmentCheck extends SingleAttributeClassBasedCheck {

    protected final List<GKInstance> EMPTY_LIST = new ArrayList<GKInstance>();
    protected Map<Long, List<Long>> neighbors = null;
    
    public CompartmentCheck() {
        checkAttribute = "compartment";
    }
    
    protected Map<Long, List<Long>> getNeighbors() {
        if (neighbors == null)
            neighbors = loadNeighbors();
        return neighbors;
    }
    
    @Override
    protected boolean checkInstance(GKInstance instance) throws Exception {
        return checkCompartment(instance);
    }

    /**
     * The class specific way to check compartment values.
     * @param containedCompartments
     * @param containerCompartments
     * @return
     * @throws Exception
     */
    protected abstract boolean compareCompartments(Set containedCompartments,
                                                   List containerCompartments) throws Exception;
    
    protected Map<Long, List<Long>> loadNeighbors() {
        Map<Long, List<Long>> map = new HashMap<Long, List<Long>>();
        try {
            InputStream input = GKApplicationUtilities.getConfig("AdjacentCompartments.txt");
            InputStreamReader ris = new InputStreamReader(input);
            BufferedReader bufferedReader = new BufferedReader(ris);
            String line = null;
            int index = 0;
            while ((line = bufferedReader.readLine()) != null) {
                // 70101,12045 #cytosol - ER membrane
                index = line.indexOf("#");
                String sub = line.substring(0, index).trim();
                String[] tokens = sub.split(",");
                Long dbId1 = new Long(tokens[0]);
                Long dbId2 = new Long(tokens[1]);
                List<Long> neighborIds = (List<Long>) map.get(dbId1);
                if (neighborIds == null) {
                    neighborIds = new ArrayList<Long>();
                    map.put(dbId1, neighborIds);
                }
                neighborIds.add(dbId2);
            }
            bufferedReader.close();
            ris.close();
            input.close();
        }
        catch(IOException e) {
            System.err.println("CompartmentChecker.loadNeighbors(): " + e);
            e.printStackTrace();
        }
        return map;
    }
    
    /**
     * Check if the compartment setting in a container is consistent with its contained instances.
     * @param container
     * @return false for error in a compartment setting
     * @throws Exception
     */
    protected boolean checkCompartment(GKInstance container) throws Exception {
        Set contained = getAllContainedEntities(container);
        // Skip checking for shell instances
        if (containShellInstances(contained))
            return true;
        // Nothing to check if contained is empty: probably 
        // the instance is just starting to annotation or
        // container is used as a place holder
        if (contained.size() == 0)
            return true;
        // Get the compartment setting: compartments should be a list since
        // it is used as a multiple value attribute.
        Set containedCompartments = new HashSet();
        for (Iterator it = contained.iterator(); it.hasNext();) {
            GKInstance comp = (GKInstance) it.next();
            List compartments = comp.getAttributeValuesList(ReactomeJavaConstants.compartment);
            if (compartments != null)
                containedCompartments.addAll(compartments);
        }
        List containerCompartments = container.getAttributeValuesList(ReactomeJavaConstants.compartment);
        // To make compare easier
        if (containerCompartments == null)
            containerCompartments = EMPTY_LIST;
        return compareCompartments(containedCompartments, containerCompartments);
    }
    
}
