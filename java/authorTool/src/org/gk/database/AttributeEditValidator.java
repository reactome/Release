/*
 * Created on May 26, 2011
 *
 */
package org.gk.database;

import java.awt.Component;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.SchemaClass;

/**
 * This class is used to validate if a value entered in the attribute edit table is 
 * correct.
 * Currently there is only one class for this validation. This class may need to be
 * refactored for different cases.
 * @author wgm
 *
 */
public class AttributeEditValidator {
    
    public AttributeEditValidator() {
    }
    
    /**
     * The actual method to validate the value.
     * @param instance
     * @param attributeName
     * @param value
     * @return
     */
    public boolean validate(GKInstance instance,
                            String attName,
                            Object value,
                            Component parentComp) {
        if (value == null || instance == null)
            return true; // Don't care
        if (attName.equals(ReactomeJavaConstants.name)) {
            // Make sure no fancy symbols are used
            if (value.toString().contains("[") ||
                value.toString().contains("]")) {
                String message = "The name attribute cannot contain \"[\" or \"]\". Use \"(\" or \")\" instead.";
                JOptionPane.showMessageDialog(parentComp,
                                              message,
                                              "Error in Name",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        SchemaClass cls = instance.getSchemClass();
        // Make sure the accession number is in the correct GO id format
        if (cls.isa(ReactomeJavaConstants.GO_BiologicalProcess) ||
            cls.isa(ReactomeJavaConstants.GO_CellularComponent) ||
            cls.isa(ReactomeJavaConstants.GO_MolecularFunction)) {
            if (attName.equals(ReactomeJavaConstants.accession)) {
                String tmpValue = value.toString();
                if (tmpValue.matches("(\\d){7}"))
                    return true;
                String message = "Accession number for a GO related instance should be seven digits.\n" +
                                 "Please don't prefix GO to an accession number.";
                JOptionPane.showMessageDialog(parentComp,
                                              message,
                                              "Error in GO Accession", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        // To avoid self-containing relationship
        if (attName.equals(ReactomeJavaConstants.hasEvent) ||
            attName.equals(ReactomeJavaConstants.hasComponent) ||
            attName.equals(ReactomeJavaConstants.hasMember) ||
            attName.equals(ReactomeJavaConstants.hasCandidate)) { 
            if (value instanceof GKInstance) {
                GKInstance newValue = (GKInstance) value;
                if (instance == newValue ||
                    InstanceUtilities.isDescendentOf(instance, newValue)) {
                    JOptionPane.showMessageDialog(parentComp,
                                                  "A higher level instance or an instance itself cannot be used in attribute \"" + 
                                                  attName + "\":\n" + 
                                                  newValue.toString(),
                                                  "Error in Attribute Editing",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        // Make sure one Pathway instance should have one PathwayDiagram
        if (attName.equals(ReactomeJavaConstants.representedPathway) &&
            cls.isa(ReactomeJavaConstants.PathwayDiagram)) {
            if (value instanceof GKInstance) {
                GKInstance pathway = (GKInstance) value;
                try {
                    // Check if there is any PathwayDiagram associated with it
                    Collection<?> referred = pathway.getReferers(ReactomeJavaConstants.representedPathway);
                    if (referred != null && referred.size() > 0) {
                        JOptionPane.showMessageDialog(parentComp, 
                                                      "\"" + pathway.getDisplayName() + "\" has a diagram already. A Pathway can have one diagram only.\n" + 
                                                      "To assign a pathway to a PathwayDiagram instance, you have to delete the origial PathwayDiagram\n " +
                                                      "or remove this pathway from its attribute list first.", 
                                                      "Error in Attribute Editing", 
                                                      JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(parentComp, 
                                                  "Error during attribute validating: " + e, 
                                                  "Error in Attribute Validating", 
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        // Make sure AAes used in the alteredAminoAcids should be valid AAs
        if (attName.equals(ReactomeJavaConstants.alteredAminoAcidFragment)) {
            String text = value.toString();
            char[] chars = text.toCharArray();
            String allowed = AttributeEditConfig.getConfig().getAllowableAminoAcids();
            String[] tokens = allowed.split(",");
            List<String> list = Arrays.asList(tokens);
            for (int i = 0; i < chars.length; i++) {
                if (!list.contains(chars[i] + "")) {
                    String message = "Values in the " + ReactomeJavaConstants.alteredAminoAcidFragment + " attribute should contain\n" + 
                                     "amino acids in single letters only in the format like \"ADEF\". These\n" +
                                     "amino acids are allowed: " + allowed + ".";
                            ;
                    JOptionPane.showMessageDialog(parentComp,
                                                  message,
                                                  "Error in Attribute " + ReactomeJavaConstants.alteredAminoAcidFragment,
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * This method is used to check a list of values for a GKInstance object.
     * @param attName
     * @param newValues
     * @return
     */
    public boolean validate(GKInstance displayed, 
                            String attName,
                            List<?> newValues,
                            Component parentComp) {
        if (displayed == null || newValues == null || newValues.size() == 0)
            return true; // Don't care
        // Passed into another method
        for (Object value : newValues) {
            if (!validate(displayed, 
                          attName, 
                          value,
                          parentComp))
                return false;
        }
        return true;
    }
    
}
