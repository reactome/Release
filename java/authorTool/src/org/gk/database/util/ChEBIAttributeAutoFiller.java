/*
 * Created on Oct 21, 2010
 *
 */
package org.gk.database.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.junit.Test;

import uk.ac.ebi.demo.ols.soap.Query;
import uk.ac.ebi.demo.ols.soap.QueryServiceLocator;

/**
 * This method is used to auto fill values for ReferenceMolecule based on ChEBI.
 * @author wgm
 *
 */
public class ChEBIAttributeAutoFiller extends PsiModAttributeAutoFiller {
    
    public ChEBIAttributeAutoFiller() {
        ONTOLOGY_NAME = "CHEBI";
        displayOntologyName = "ReferenceMolecule";
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void mapMetaToAttributes(GKInstance instance,
                                       String termId,
                                       Query query)  throws Exception {
        GKInstance dbInst = getReferenceDatabasae("ChEBI");
        instance.setAttributeValue(ReactomeJavaConstants.referenceDatabase, dbInst);
        Map<String, String> meta = query.getTermMetadata(termId, ONTOLOGY_NAME);
        if (meta == null || meta.size() == 0) {
            InstanceDisplayNameGenerator.setDisplayName(instance);
            return;
        }
        for (String name : meta.keySet()) {
            String value = meta.get(name);
            if (value == null || value.length() == 0)
                continue;
            if (name.startsWith("related_synonym")) {
                instance.addAttributeValue(ReactomeJavaConstants.name, value);
            }
            else if (name.startsWith("FORMULA_synonym")) {
                instance.setAttributeValue(ReactomeJavaConstants.formula, value);
            }
        }
        // Set the display name
        InstanceDisplayNameGenerator.setDisplayName(instance);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void mapCrossReference(GKInstance instance, 
                                     String termId,
                                     Query query) throws Exception {
        Map<String, String> xrefs = query.getTermXrefs(termId, ONTOLOGY_NAME);
        if (xrefs == null || xrefs.size() == 0)
            return;
        // Just want to map to KEGG compound only.
        Pattern pattern = Pattern.compile("KEGG COMPOUND:(C(\\d){5})");
        for (String name : xrefs.keySet()) {
            String value = xrefs.get(name);
            if (value == null || value.length() == 0)
                continue;
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String group = matcher.group(1);
                GKInstance xrefInstance = getCompoundInstance(group);
                if (xrefInstance != null)
                    instance.addAttributeValue(ReactomeJavaConstants.crossReference, xrefInstance);
            }
        }
    }
    
    private GKInstance getCompoundInstance(String id) throws Exception {
        Collection<?> c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                           ReactomeJavaConstants.identifier,
                                                           "=", 
                                                           id);
        String displayName = "COMPOUND:" + id;
        if (c != null && c.size() > 0) {
            for (Iterator<?> it = c.iterator(); it.hasNext();) {
                GKInstance inst = (GKInstance) it.next();
                if (inst.getDisplayName().equals(displayName)) {
                    // In case it is a shell instance, to compare it with name
                    return inst;
                }
            }
        }
        GKInstance rtn = null;
        // Need to create a new instance
        if (adaptor instanceof XMLFileAdaptor) {
            rtn = ((XMLFileAdaptor)adaptor).createNewInstance(ReactomeJavaConstants.DatabaseIdentifier);
            GKInstance refDb = getReferenceDatabasae("COMPOUND");
            rtn.setAttributeValue(ReactomeJavaConstants.referenceDatabase, refDb);
            rtn.setAttributeValue(ReactomeJavaConstants.identifier, id);
            InstanceDisplayNameGenerator.setDisplayName(rtn);
        }
        return rtn;
    }
    
    /**
     * Check time used for query the SOAP WS server.
     * @throws Exception
     */
    @Test
    public void speedCheck() throws Exception {
        long time1 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            QueryServiceLocator locator = new QueryServiceLocator();
            Query service = locator.getOntologyQuery();
            Map<String, String> meta = service.getTermMetadata("CHEBI:17794", ONTOLOGY_NAME);
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Total time: " + (time2 - time1));
    }
    
}
