/*
 * Created on Sep 22, 2005
 *
 */
package org.gk.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.gk.model.DatabaseIdentifier;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.Summation;
import org.gk.render.InstanceToRenderableConverter;
import org.gk.schema.SchemaClass;

public class GKInstanceHTMLPropertyPane extends HTMLPropertyPane {
    
    private GKInstance instance;
    
    public GKInstanceHTMLPropertyPane() {
        super();
    }
    
    public GKInstanceHTMLPropertyPane(GKInstance instance) {
        super();
        setInstance(instance);
    }
    
    public void setInstance(GKInstance instance) {
        this.instance = instance;
        String text = generateHtml();
        setText(text);
        // Stay at the top as default
        setCaretPosition(0);
    }

    protected void generateHtmlForComplex(StringBuffer buffer) {
        try {
            List components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            if (components == null || components.isEmpty())
                return;
            Map stoichiometries = generateStoiMap(components);
            List list = new ArrayList(stoichiometries.keySet());
            InstanceUtilities.sortInstances(list);
            generateHTMLForList(list, stoichiometries, buffer, "Composition");
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.generateHtmlForComplex(): " + e);
            e.printStackTrace();
        }
    }
 
    private Map generateStoiMap(List components) {
        Map map = new HashMap();
        GKInstance comp = null;
        Integer value = null;
        for (Iterator it = components.iterator(); it.hasNext();) {
            comp = (GKInstance) it.next();
            value = (Integer) map.get(comp);
            if (value == null) 
                value = new Integer(1);
            else 
                value = new Integer(value.intValue() + 1);
            map.put(comp, value);
        }
        return map;
    }

    protected void generateHtmlForPathway(StringBuffer buffer) {
        try {
            List components = null;
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                components = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            if (components == null || components.isEmpty())
                return;
            Map stoi = new HashMap();
            generateHTMLForList(components, stoi, buffer, "Components");
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.generateHtmlForPathway(): " + e);
            e.printStackTrace();
        }
    }

    protected void generateHtmlForReaction(StringBuffer buffer) {
        try {     
            List list = instance.getAttributeValuesList(ReactomeJavaConstants.input);
            Map stoiMap = null;
            if (list != null && list.size() > 0) {
                stoiMap = generateStoiMap(list);
                List inputs = new ArrayList(stoiMap.keySet());
                InstanceUtilities.sortInstances(inputs);
                generateHTMLForList(inputs, stoiMap, buffer, "Input");
            }
            list = instance.getAttributeValuesList(ReactomeJavaConstants.output);
            if (list != null && list.size() > 0) {
                stoiMap = generateStoiMap(list);
                List outputs = new ArrayList(stoiMap.keySet());
                InstanceUtilities.sortInstances(outputs);
                generateHTMLForList(outputs, stoiMap, buffer, "Output");
            }
            // Get the cataylsts
            Set catalysts = getReactionCatalysts(instance);
            if (catalysts != null && catalysts.size() > 0) {
                stoiMap.clear();
                List catalystList = new ArrayList(catalysts);
                InstanceUtilities.sortInstances(catalystList);
                generateHTMLForList(catalystList, stoiMap, buffer, "Catalyst");
            }
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.generateHtmlForReaction(): " + e);
            e.printStackTrace();
        }
    }

    protected List getAliases() {
        try {
            List names = instance.getAttributeValuesList(ReactomeJavaConstants.name);
            return names;
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getAliases(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected DatabaseIdentifier getDatabaseIdentifier() {
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.PhysicalEntity))
            return null;
        try {
            GKInstance dbRef = null;
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity))
                dbRef = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (dbRef == null)
                dbRef = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.crossReference);
            if (dbRef == null)
                return null;
            DatabaseIdentifier rtn = new DatabaseIdentifier();
            GKInstance db = (GKInstance) dbRef.getAttributeValue(ReactomeJavaConstants.referenceDatabase);
            if (db != null)
                rtn.setDbName(db.getDisplayName());
            String identifier = (String) dbRef.getAttributeValue(ReactomeJavaConstants.identifier);
            if (identifier != null)
                rtn.setAccessNo(identifier);
            return rtn;
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getDatabaseIdentifier(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected Long getDBID() {
        return instance.getDBID();
    }

    protected String getDisplayName() {
        return instance.getDisplayName();
    }

    protected String getDisplayName(Object component) {
        if (component instanceof GKInstance)
            return ((GKInstance)component).getDisplayName();
        return null;
    }

    protected String getLocalization() {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment))
            return null;
        try {
            GKInstance localizationInstance = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.compartment);
            if (localizationInstance != null)
                return localizationInstance.getDisplayName();
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getLocalization(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected List getModifications() {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasModifiedResidue))
            return null;
        try {
            List modifiedResidues = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            return InstanceToRenderableConverter.convertModifications(modifiedResidues);
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getModifications(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected List getReferences() {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.literatureReference))
            return null;
        try {
            List literatureReferences = instance.getAttributeValuesList(ReactomeJavaConstants.literatureReference);
            return InstanceToRenderableConverter.extractReferences(literatureReferences);
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getRefernece(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected Summation getSummation() {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.summation))
            return null;
        try {
            GKInstance summation = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.summation);
            if (summation != null) {
                Summation rtn = InstanceToRenderableConverter.convertLiteratureReferenceToSummation(summation);
                return rtn;
            }
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getSummation(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected String getTaxon() {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.species))
            return null;
        try {
            GKInstance species = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.species);
            if (species != null)
                return species.getDisplayName();
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.getTaxon(): " + e);
            e.printStackTrace();
        }
        return null;
    }

    protected String getType() {
        SchemaClass cls = instance.getSchemClass();
        if (cls.isa(ReactomeJavaConstants.Pathway))
            return "Pathway";
        if (cls.isa(ReactomeJavaConstants.Reaction))
            return "Reaction";
        if (cls.isa(ReactomeJavaConstants.Complex))
            return "Complex";
        if (cls.isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
            cls.isa(ReactomeJavaConstants.ReferenceGeneProduct)) 
            return "Protein"; // Maybe other gene products. Use Proteins for the time being.
        if (cls.isa(ReactomeJavaConstants.ReferenceMolecule))
            return "Compound";
        return "Entity";
    }

    protected void processInstanceLink(String desc) {
        int index = desc.indexOf(":");
        String subunitName = desc.substring(index + 1);
        GKInstance subunit = searchComponent(subunitName);
        if (subunit != null) {
            JDialog parentDialog = (JDialog) SwingUtilities.getRoot(this);
            GKInstanceHTMLPropertyPane propertyPane = new GKInstanceHTMLPropertyPane(subunit);
            JDialog dialog = propertyPane.generateDialog(parentDialog);
            dialog.setSize(400, 600);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
            return;
        }
    }
    
    private Set getReactionCatalysts(GKInstance reaction) throws Exception {
        Set rtn = new HashSet();
        List catalystActivities = reaction.getAttributeValuesList(ReactomeJavaConstants.catalystActivity);
        if (catalystActivities != null) {
            for (Iterator it = catalystActivities.iterator(); it.hasNext();) {
                GKInstance ca = (GKInstance) it.next();
                GKInstance physicalEntity = (GKInstance) ca.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                if (physicalEntity != null)
                    rtn.add(physicalEntity);
            }
        }
        return rtn;
    }
    
    private GKInstance searchComponent(String displayName) {
        SchemaClass cls = instance.getSchemClass();
        try {
            Collection instanceCollection = null;
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                instanceCollection = instance.getAttributeValuesList(ReactomeJavaConstants.hasComponent);
            else if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                instanceCollection = instance.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            else if (cls.isa("Reaction")) {
                instanceCollection = new HashSet();
                List inputs = instance.getAttributeValuesList(ReactomeJavaConstants.input);
                if (inputs != null)
                    instanceCollection.addAll(inputs);
                List outputs = instance.getAttributeValuesList(ReactomeJavaConstants.output);
                if (outputs != null)
                    instanceCollection.addAll(outputs);
                Set catalysts = getReactionCatalysts(instance);
                instanceCollection.addAll(catalysts);
            }
            if (instanceCollection != null) {
                for (Iterator it = instanceCollection.iterator(); it.hasNext();) {
                    GKInstance comp = (GKInstance) it.next();
                    if (comp.getDisplayName().equals(displayName))
                        return comp;
                }
            }
        } 
        catch (Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.searchComponent(): " + e);
            e.printStackTrace();
        }
        return null;
    }
    
    private void generateHtmlForAccessNumber(StringBuffer buffer) throws Exception {
        if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier))
            return;
        String access = (String) instance.getAttributeValue(ReactomeJavaConstants.identifier);
        if (access == null || access.length() == 0)
            return;
        fillTableRow("Identifier", access, buffer);
        if (instance.getSchemClass().isValidAttribute("variantIdentifier")) {
            String variantIdentifier = (String) instance.getAttributeValue("variantIdentifier");
            if (variantIdentifier != null && 
                !access.equals(variantIdentifier))
                fillTableRow("Variant Identifier", 
                             variantIdentifier,
                             buffer);
        }
    }

    protected void generateHtmlForCompound(StringBuffer buffer) {
        try {
            // identifier first
            generateHtmlForAccessNumber(buffer);
            // formula
            String formula = (String) instance.getAttributeValue(ReactomeJavaConstants.formula);
            if (formula != null && formula.length() > 0) {
                fillTableRow("Formula", formula, buffer);
            }
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.generateHtmlForCompound(): " + e);
            e.printStackTrace();
        }
    }

    protected void generateHtmlForProtein(StringBuffer buffer) {
        try {
            // Identifier first
            generateHtmlForAccessNumber(buffer);
            // Secondary identifier
            List list = instance.getAttributeValuesList(ReactomeJavaConstants.secondaryIdentifier);
            if (list != null && list.size() > 0) {
                StringBuffer tmp = new StringBuffer();
                for (Iterator it = list.iterator(); it.hasNext();) {
                    tmp.append(it.next());
                    if (it.hasNext())
                        tmp.append(", ");
                }
                fillTableRow("Other Identifier", tmp.toString(), buffer);
            }
            // Gene Name
            list = instance.getAttributeValuesList(ReactomeJavaConstants.geneName);
            generateHtmlForStringList(buffer, list, "Gene Name");
            // comments
            list = instance.getAttributeValuesList(ReactomeJavaConstants.comment);
            generateHtmlForStringList(buffer, list, "Comment");
        }
        catch(Exception e) {
            System.err.println("GKInstanceHTMLPropertyPane.genearteHtmlForProtein(): " + e);
            e.printStackTrace();
        }
    }
    
}
