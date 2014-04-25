/*
 * Created on Dec 4, 2008
 *
 */
package org.gk.elv;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.database.AttributeEditConfig;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;

/**
 * This helper class is used to clone an Instance.
 * @author wgm
 *
 */
public class InstanceCloneHelper {
    
    public InstanceCloneHelper() {
    }
    
    public List<GKInstance> cloneInstances(List<GKInstance> selection,
                                           Component parentComp) {
        List<GKInstance> newInstances = new ArrayList<GKInstance>();
        if (selection != null && selection.size() > 0) {
            // To be selected
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            for (Iterator it = selection.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                if (instance.isShell())
                    continue; // Should not occur since Clone action should be disabled
                GKInstance copy = cloneInstance(instance, fileAdaptor);
                newInstances.add(copy);
                // Check if components in a Complex should be copied recursively
                if (instance.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                    int reply = JOptionPane.showConfirmDialog(parentComp,
                                                              "Do you want to clone the contained subunits recursively?",
                                                              "Complex Clone",
                                                              JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION) {
                        List newComponents = cloneComplexSubunits(copy, 
                                                                  fileAdaptor,
                                                                  parentComp);
                        newInstances.addAll(newComponents);
                    }
                }
            }
        }
        return newInstances;
    }
    
    public GKInstance cloneInstance(GKInstance instance, 
                                     XMLFileAdaptor fileAdaptor) {
        if (instance.isShell())
            throw new IllegalArgumentException("CuratorActionCollection.cloneInstance(): " +
                    "A shell instance cannot be cloned.");
        GKInstance copy = (GKInstance) instance.clone();
        // Have to check un-editable attributes.
        // It seems that this is not an elegant way?
        java.util.List uneditableAttNames = AttributeEditConfig.getConfig().getUneditableAttNames();
        if (uneditableAttNames != null && uneditableAttNames.size() > 0) {
            SchemaClass cls = copy.getSchemClass();
            for (Iterator it1 = uneditableAttNames.iterator(); it1.hasNext();) {
                String attName = (String) it1.next();
                if (cls.isValidAttribute(attName))
                    copy.setAttributeValueNoCheck(attName, null);
            }
            // It is possible the display name is gone. Use "Copy of " for _displayName.
            copy.setDisplayName("Clone of " + instance.getDisplayName());
        }
        // Have to set a new DB_ID explicitly
        copy.setDBID(fileAdaptor.getNextLocalID());
        fileAdaptor.addNewInstance(copy);
        return copy;
    }
    
    private List cloneComplexSubunits(GKInstance complex, 
                                      XMLFileAdaptor fileAdaptor,
                                      Component parentComp) {
        // Need to check if a shell instance is included in the hasComponent hierarchy.
        // If true, deep cloning cannot work
        List subunits = null;
        try {
            subunits = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        }
        catch(Exception e) {
            System.err.println("CuratorActionCollection.cloneComplexSubunits(): " + e);
            e.printStackTrace();
        }
        if (subunits == null || subunits.size() == 0)
            return new ArrayList(); // Nothing to do. Just return
        Set current = new HashSet();
        Set next = new HashSet();
        current.addAll(subunits);
        while (current.size() > 0) {
            try {
                for (Iterator it = current.iterator(); it.hasNext();) {
                    GKInstance tmp = (GKInstance) it.next();
                    if (tmp.isShell()) {
                        JOptionPane.showMessageDialog(parentComp,
                                                      "A shell instance is contained in the hasComponent hierarchy.\n"
                                                      +"A recursize cloning cannot be applied to such a complex.",
                                                      "Error in Cloning",
                                                      JOptionPane.ERROR_MESSAGE);
                        return new ArrayList(); // Return an empty Array to make the caller happy
                    }
                    if (tmp.getSchemClass().isa(ReactomeJavaConstants.Complex)) {
                        List list = tmp.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
                        if (list != null && list.size() > 0)
                            next.addAll(list);
                    }
                }
            }  
            catch (Exception e) {
                System.err.println("CuratorActionCollection.cloneComplexSubunits(): " + e);
                e.printStackTrace();
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
        // Map: from old GKInstance to cloned GKInstance
        // Use this map to avoid clone the same Subunit more than once 
        // since a subunit can be contained more than multiple times in
        // one Complex or its contained complex
        Map oldToNewMap = new HashMap();
        cloneComplex(complex, oldToNewMap, fileAdaptor);
        return new ArrayList(oldToNewMap.values());
    }
    
    private void cloneComplex(GKInstance complex, Map oldToNewMap, XMLFileAdaptor fileAdaptor) {
        List subunits = null;
        try {
            subunits = complex.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
        }
        catch(Exception e) {
            System.err.println("CuratorActionCollection.cloneComplexSubunits(): " + e);
            e.printStackTrace();
        }
        if (subunits == null || subunits.size() == 0)
            return;
        GKInstance subunit = null;
        GKInstance clone = null;
        List newValues = new ArrayList(subunits.size());
        for (Iterator it = subunits.iterator(); it.hasNext();) {
            subunit = (GKInstance) it.next();
            clone = (GKInstance) oldToNewMap.get(subunit);
            if (clone == null) {
                clone = cloneInstance(subunit, fileAdaptor);
                oldToNewMap.put(subunit, clone);
                if (subunit.getSchemClass().isa(ReactomeJavaConstants.Complex))
                    cloneComplex(clone, oldToNewMap, fileAdaptor);
            }
            newValues.add(clone);
        }
        complex.setAttributeValueNoCheck(ReactomeJavaConstants.hasComponent,
                                         newValues);
    }
    
}
