/*
 * Created on Sep 9, 2009
 *
 */
package org.gk.database.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;

import uk.ac.ebi.demo.ols.soap.Query;
import uk.ac.ebi.demo.ols.soap.QueryServiceLocator;

/**
 * This class is used to fetch attributes for PsiMOD instance directly from the EBI web service server
 * via a Java API from the OLS-client project.
 * @author wgm
 *
 */
public class PsiModAttributeAutoFiller extends AbstractAttributeAutoFiller {
    protected String ONTOLOGY_NAME = "MOD";
    protected String displayOntologyName = "PsiMod";
    
    public PsiModAttributeAutoFiller() {
        
    }
    
    public void setOntologyName(String name) {
        this.ONTOLOGY_NAME = name;
    }
    
    public void setDisplayOntologyName(String name) {
        this.displayOntologyName = name;
    }
    
    @Override
    protected String getConfirmationMessage() {
        return "Do you want the tool to fetch information from the EBI ontology database \n" +
        		"for this " + displayOntologyName + " instance?";
    }
    
    @Override
    protected Object getRequiredAttribute(GKInstance instance) throws Exception {
        return instance.getAttributeValue(ReactomeJavaConstants.identifier);
    }
    
    @Override
    public void process(GKInstance instance, Component parentComp) throws Exception {
        // Need to query the EBI ontology web service server
        String identifier = (String) getRequiredAttribute(instance);
        if (identifier == null) { // Just in case: this should never occur
            if (parentComp != null)
                JOptionPane.showMessageDialog(parentComp, 
                                              "Please provide an " + ONTOLOGY_NAME + " id for the new instance.",
                                              "Error in Fetching",
                                              JOptionPane.ERROR_MESSAGE);
            return;
        }
        // We only need numbers in case the user provide the ontology name in the instance
        if (identifier.startsWith(ONTOLOGY_NAME)) {
            int index = identifier.indexOf(":");
            identifier = identifier.substring(index + 1).trim();
            instance.setAttributeValue(getIdentifierAttributeName(), 
                                       identifier);
        }
        String termId = ONTOLOGY_NAME + ":" + identifier;
        // Start fetch information
        QueryServiceLocator locator = new QueryServiceLocator();
        Query service = locator.getOntologyQuery();
        // Get the term name
        String term = service.getTermById(termId,
                                          ONTOLOGY_NAME);
        if (term == null) {
            if (parentComp != null)
                JOptionPane.showMessageDialog(parentComp,
                                              "No information available for the specified term id!",
                                              "Warning in Fetching",
                                              JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.name))
            instance.setAttributeValue(ReactomeJavaConstants.name, 
                                       term);
        mapMetaToAttributes(instance, termId, service);
        mapCrossReference(instance, termId, service);
    }
    
    /**
     * Get the name used for the identifier attribute. This may be different for different class.
     * @return
     */
    protected String getIdentifierAttributeName() {
        return ReactomeJavaConstants.identifier;
    }
    
    /**
     * Default implementation used as a template.
     * @param instance
     * @param term
     * @param services
     * @throws Exception
     */
    protected void mapCrossReference(GKInstance instance,
                                     String termId,
                                     Query services) throws Exception {
    }
    
    
    protected void mapMetaToAttributes(GKInstance instance,
                                       String termId,
                                       Query service) throws Exception {
        HashMap<String, String> meta = service.getTermMetadata(termId, 
                                                               ONTOLOGY_NAME);
        if (meta == null || meta.size() == 0)
            return;
        for (String key : meta.keySet()) {
            String value = meta.get(key);
            if (key == null)
                continue; // Sometime there is a null as a key, which may not be correct!
//            System.out.println(key + ": " + value);
            if (key.equals("definition")) {
                instance.setAttributeValue(ReactomeJavaConstants.definition, 
                                           value);
            }
        }
        List<String> synonyms = extractSynonym(meta);
        if (synonyms.size() > 0)
            instance.setAttributeValue(ReactomeJavaConstants.synonym, 
                                       synonyms);
        GKInstance referenceDb = getPsiModInstance();
        instance.setAttributeValue(ReactomeJavaConstants.referenceDatabase, 
                                   referenceDb);
    }
    
    protected List<String> extractSynonym(Map<String, String> meta) {
        List<String> synonyms = new ArrayList<String>();
        for (String key : meta.keySet()) {
            String value = meta.get(key);
            if (key == null)
                continue; // Sometime there is a null as a key, which may not be correct!
            if (key.endsWith("_synonym")) { // Based on the format used in the web service. I guess this may not be true in the future.
                synonyms.add(value);
            }
        }
        return synonyms;
    }
    
    private GKInstance getPsiModInstance() throws Exception {
        return getReferenceDatabasae(ONTOLOGY_NAME);
    }
    
}
