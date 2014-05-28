/*
 * Created on Jun 13, 2012
 *
 */
package org.gk.scripts;

import java.util.Collection;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * This simple class is used to do SO term related stuff.
 * @author gwu
 *
 */
public class SOTermScript {
    
    public SOTermScript() {
        
    }
    
    /**
     * Get SO terms from a local file and add them into a local project.
     * @throws Exception
     */
    @Test
    public void addSoTerms() throws Exception {
        String dirName = "/Users/gwu/Documents/wgm/work/reactome/";
        // This project has downloaded all SO terms from gk_central
        String srcFileName = dirName + "SOTerms.rtpj";
        XMLFileAdaptor fileAdaptor = new XMLFileAdaptor();
        fileAdaptor.setSource(srcFileName);
        // Read SO terms from file
        String soFileName = "/Users/gwu/Documents/gkteam/Marija/ShortListSOTerms.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(soFileName);
        String line = null;
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            String id = tokens[0].trim();
            String name = tokens[1].trim();
            // Search if a term has been created already.
            GKInstance soInst = fetchSO(fileAdaptor, id);
            if (soInst == null) {
                soInst = fileAdaptor.createNewInstance(ReactomeJavaConstants.SequenceOntology);
                soInst.setAttributeValue(ReactomeJavaConstants.accession, id);
                soInst.setAttributeValue(ReactomeJavaConstants.name, name);
                InstanceDisplayNameGenerator.setDisplayName(soInst);
            }
        }
        fileAdaptor.save(dirName + "SOTerms_Loaded.rtpj");
    }
    
    @SuppressWarnings("unchecked")
    private GKInstance fetchSO(XMLFileAdaptor fileAdaptor, String identifier) throws Exception {
        Collection<GKInstance> c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.SequenceOntology,
                                                                       ReactomeJavaConstants.accession, 
                                                                       "=",
                                                                       identifier);
        if (c == null || c.size() == 0) {
            // Try without SO in the identifier
            int index = identifier.indexOf(":");
            identifier = identifier.substring(index + 1);
            c = fileAdaptor.fetchInstanceByAttribute(ReactomeJavaConstants.SequenceOntology,
                                                     ReactomeJavaConstants.accession, 
                                                     "=",
                                                     identifier);
        }
        if (c == null || c.size() == 0)
            return null;
        return c.iterator().next();
    }
    
}
