/*
 * Created on Aug 4, 2005
 *
 */
package org.gk.database.util;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
;

/**
 * This class is used to fetch detailed information from UniProt database by specifiy
 * a database identity. The XML format from the web site is downloaded and parsed to 
 * extract information for attributes in ReferencePeptideSequence.
 * @author guanming
 *
 */
public class ReferencePeptideSequenceAutoFiller extends AbstractAttributeAutoFiller {
    // XML download site in the uniprot web site
    private static String UNIPROT_DOWNLOAD_URL = "http://www.uniprot.org/uniprot/";
    private static String UNIPROT_XML_FORMAT = ".xml";
    private static String UNIPROT_FLAT_FORMAT = ".txt";
    // This instance handled by this object
    private GKInstance startingInstance = null;
    
    // Load configuration first before anything can be done
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        try {
            InputStream is = GKApplicationUtilities.getConfig("curator.xml");
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(is);
            Element elm = (Element) XPath.selectSingleNode(document.getRootElement(), 
                                                           "uniprot");
//            <uniprot>
//                <UNIPROT_DOWNLOAD_URL>http://beta.uniprot.org/uniprot/</UNIPROT_DOWNLOAD_URL>
//                <UNIPROT_XML_FORMAT>.xml</UNIPROT_XML_FORMAT>
//                <UNIPROT_FLAT_FORMAT>.txt</UNIPROT_FLAT_FORMAT>
//            </uniprot>
            List list = elm.getChildren();
            for (Iterator it = list.iterator(); it.hasNext();) {
                Element child = (Element) it.next();
                String name = child.getName();
                String value = child.getText();
                if (name.equals("UNIPROT_DOWNLOAD_URL"))
                    UNIPROT_DOWNLOAD_URL = value;
                else if (name.equals("UNIPROT_XML_FORMAT"))
                    UNIPROT_XML_FORMAT = value;
                else if (name.equals("UNIPROT_FLAT_FORMAT"))
                    UNIPROT_FLAT_FORMAT = value;
            }
        }
        catch(Exception e) {
            System.err.println("ReferencePeptideSequenceAutoFiller.loadConfig(): " + e);
        }
    }
    
    public ReferencePeptideSequenceAutoFiller() {
    }

    protected Object getRequiredAttribute(GKInstance instance) throws Exception {
        return instance.getAttributeValue("identifier");
    }
    
    protected String getConfirmationMessage() {
        return "Do you want the tool to fetch information from UniProt database?";
    }
    
    /**
     * A simple utility method to fetch detailed information from the database for a GKInstance
     * with identifier specified. 
     * Note: This method should NOT be used in the curator tool to avoid losing of ReferenceIsoform
     * instances.
     * @param instance
     * @throws Exception
     */
    public void process(GKInstance instance) throws Exception {
        this.startingInstance = instance;
        if (autoCreatedInstances == null)
            autoCreatedInstances = new ArrayList<GKInstance>();
        else
            autoCreatedInstances.clear();
        process(instance, null, false);
    }
    
    @Override
    public void process(GKInstance instance, Component parentComp) throws Exception {
        // Initialize some member variables
        this.startingInstance = instance;
        if (autoCreatedInstances == null)
            autoCreatedInstances = new ArrayList<GKInstance>();
        else
            autoCreatedInstances.clear();
        process(instance, parentComp, true);
    }
    
    public int[] fetchCoordinates(String identifier,
                                 Component parentComp) throws Exception {
        String urlName = UNIPROT_DOWNLOAD_URL + identifier + UNIPROT_FLAT_FORMAT;
        URL url = new URL(urlName);
        InputStream is = url.openStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
        String line = null;
        String chainLine = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("FT   CHAIN")) {
                chainLine = line;
                break;
            }
        }
        reader.close();
        isr.close();
        is.close();
        if (chainLine == null)
            return null;
        int[] coordinates = new int[2];
        coordinates[0] = coordinates[1] = -1; // initialize as -1
        String[] tokens = line.split("(\\s)+");
        if (tokens.length > 2) {
            try {
                coordinates[0] = Integer.parseInt(tokens[2]);
            }
            catch(NumberFormatException e) {} // Just do nothing
        }
        if (tokens.length > 3) {
            try {
                coordinates[1] = Integer.parseInt(tokens[3]);
            }
            catch(NumberFormatException e) {}
        }
        return coordinates;
    }
    
    private void process(GKInstance instance, 
                         Component parentComp, 
                         boolean needHandleIsoForms) throws Exception {
        Object identifier = getRequiredAttribute(instance);
        if (identifier == null)
            return ;
        // Need to check if the passed identifier is correct or not
        String value = identifier.toString();
        // This is a very coarse check: make sure it has 6 alphanumerical characters only.
        if (!value.matches("([A-Z]|\\d){6}")) {
            JOptionPane.showMessageDialog(parentComp,
                                          "The provided UniProt identifier is not correct. " +
                                          "It should consisit of 6 alphanumerical characters (upper case only).",
                                          "Error in Identifier",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        String url = UNIPROT_DOWNLOAD_URL + identifier + UNIPROT_XML_FORMAT;
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new URL(url));
        Element rootElm = document.getRootElement();
        // Use entry only
        Element entry = rootElm.getChild("entry", rootElm.getNamespace());
        String path = "*[local-name()='name']/text()";
        Text nameNode = (Text) XPath.selectSingleNode(entry, path);
        // As one of secondary identifier
        instance.addAttributeValue("secondaryIdentifier", nameNode.getText());
        path = "*[local-name()='protein']/*[local-name()='name']/text()";
        List proteinNameNodes = XPath.selectNodes(entry, path);
        for (Iterator it = proteinNameNodes.iterator(); it.hasNext();) {
            Text text = (Text) it.next();
            // protein name
            instance.addAttributeValue("name", text.getText());
        }
        path = "*[local-name()='gene']/*[local-name()='name']/text()";
        List geneNameNodes = XPath.selectNodes(entry, path);
        for (Iterator it = geneNameNodes.iterator(); it.hasNext();) {
            Text text = (Text) it.next();
            // Gene name
            instance.addAttributeValue("geneName", text.getText());
        }
        path = "*[local-name()='keyword']/text()";
        List keywordNodes = XPath.selectNodes(entry, path);
        for (Iterator it = keywordNodes.iterator(); it.hasNext();) {
            Text text = (Text) it.next();
            // Keyword
            instance.addAttributeValue("keyword", text.getText());
        }
        path = "*[local-name()='accession']/text()";
        List accessionNodes = XPath.selectNodes(entry, path);
        for (Iterator it = accessionNodes.iterator(); it.hasNext();) {
            Text text = (Text) it.next();
            if (text.getText().equals(identifier))
                continue;
            // More secondaryIdentifier
            instance.addAttributeValue("secondaryIdentifier", text.getText());
        }
        // Get the species
        path = "*[local-name()='organism']/*[local-name()='name'][@type='scientific']/text()";
        Text speciesNode = (Text) XPath.selectSingleNode(entry, path);
        // In some rare cases, full name might be used for organism (e.g. Q8LI45). This is a bug
        // found by Peter
        if (speciesNode == null) {
            path = "*[local-name()='organism']/*[local-name()='name'][@type='full']/text()";
            speciesNode = (Text) XPath.selectSingleNode(entry, path);
        }
        if (speciesNode != null) {
            GKInstance species = getSpecies(speciesNode.getText());
            if (species != null)
                instance.setAttributeValue("species", species);
        }
        processComment(instance, parentComp);
        // Add the database
        GKInstance uniProt = getUniProtInstance();
        if (uniProt != null)
            instance.setAttributeValue("referenceDatabase", uniProt);
        // Get the comments for alternative products
        path = "*[local-name()='comment'][@type='alternative products']//*[local-name()='isoform']/*[local-name()='id']/text()";
        List isoformIds = XPath.selectNodes(entry, path);
        if (isoformIds != null && isoformIds.size() > 0) {
            // Don't care the database
            if (needHandleIsoForms && isoformIds.size() > 1) {
                int reply = JOptionPane.showConfirmDialog(parentComp,
                                                          "Alternative products exist for " + identifier + ". Do you want to create instances for all of them?",
                                                          "Create Alternative Products?",
                                                          JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION) {
                    if (instance.getDbAdaptor().getSchema().isValidClass(ReactomeJavaConstants.ReferenceGeneProduct)) {
                        generateReferenceIsoforms(isoformIds, 
                                                  instance,
                                                  parentComp);
                    }
                    else {
                        // Create instances for other alternative
                        for (int i = 1; i < isoformIds.size(); i++) {
                            String isoformID = ((Text) isoformIds.get(i)).getText();
                            GKInstance clone = (GKInstance) instance.clone();
                            if (clone.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
                                clone.setAttributeValue(ReactomeJavaConstants.variantIdentifier, 
                                                        isoformID);
                            if (adaptor instanceof XMLFileAdaptor) {
                                Long dbID = ((XMLFileAdaptor)adaptor).getNextLocalID();
                                clone.setDBID(dbID);
                                ((XMLFileAdaptor)adaptor).addNewInstance(clone);
                            }
                            InstanceDisplayNameGenerator.setDisplayName(clone);
                            autoCreatedInstances.add(clone);
                        }
                    }
                }
            }
            if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                String firstID = ((Text)isoformIds.get(0)).getText();
                instance.setAttributeValue(ReactomeJavaConstants.variantIdentifier,
                                           firstID);
            }
        }
        InstanceDisplayNameGenerator.setDisplayName(instance);
    }
    
    private void generateReferenceIsoforms(List isoformIds,
                                           GKInstance refGeneProduct,
                                           Component parentComp) throws Exception {
        SchemaClass refIsoformCls = refGeneProduct.getDbAdaptor().getSchema().getClassByName(ReactomeJavaConstants.ReferenceIsoform);
        // Create instances for other alternative
        for (int i = 0; i < isoformIds.size(); i++) {
            String isoformID = ((Text) isoformIds.get(i)).getText();
            // Make sure isoformId and identifier should be the same in a RefereneIsoform instance
            // Make sure this should be handled first to ensure the correct parent and child relationship
            int index = isoformID.indexOf("-");
            String childId = isoformID.substring(0, index);
            String parentId = (String) refGeneProduct.getAttributeValue(ReactomeJavaConstants.identifier);
            GKInstance newParent = null;
            if (!childId.equals(parentId)) {
                // Show a message to the user
                JOptionPane.showMessageDialog(parentComp,
                                              "An isoform, " + isoformID + ", has different identifier from its isoformParent.\n" +
                                              "Its parent will be created automatically from the UniProt database if it doesn't exist.",
                                              "Handling Isoform",
                                              JOptionPane.INFORMATION_MESSAGE);
                newParent = fetchRefGeneProduct(childId, parentComp);
            }
            // Check if this isoform has existed in the local project already
            Collection c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceIsoform,
                                                            ReactomeJavaConstants.variantIdentifier,
                                                            "=", 
                                                            isoformID);
            if (c != null && c.size() > 0) {
                // Hopefully there is only one instance.
                GKInstance isoform = (GKInstance) c.iterator().next();
                // Check if its isoformParent has refGeneProduct assigned
                List<GKInstance> parents = isoform.getAttributeValuesList(ReactomeJavaConstants.isoformParent);
                if (parents == null || !parents.contains(refGeneProduct)) {
                    isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, 
                                              refGeneProduct);
                }
                // Parents should not be an empty list any more
                // Call it again to make sure in case this list has not been changed after the above modification block.
                parents = isoform.getAttributeValuesList(ReactomeJavaConstants.isoformParent);
                if (newParent != null && !parents.contains(newParent))
                    isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, 
                                              newParent);
                continue;
            }
            GKInstance isoform = null;
            if (newParent != null)
                isoform = (GKInstance) newParent.clone();
            else
                isoform = (GKInstance) refGeneProduct.clone();
            // It should be a new class in the new schema (as of 2/12/09)
            isoform.setSchemaClass(refIsoformCls);
            if (isoform.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
                isoform.setAttributeValue(ReactomeJavaConstants.variantIdentifier, 
                                          isoformID);
            if (isoform.getSchemClass().isValidAttribute(ReactomeJavaConstants.isoformParent)) {
                isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, 
                                          refGeneProduct);
                if (newParent != null)
                    isoform.addAttributeValue(ReactomeJavaConstants.isoformParent, 
                                              newParent);
            }
            if (adaptor instanceof XMLFileAdaptor) {
                Long dbID = ((XMLFileAdaptor)adaptor).getNextLocalID();
                isoform.setDBID(dbID);
                ((XMLFileAdaptor)adaptor).addNewInstance(isoform);
            }
            InstanceDisplayNameGenerator.setDisplayName(isoform);
            autoCreatedInstances.add(isoform);
        }
    }
    
    private GKInstance fetchRefGeneProduct(String identifier,
                                           Component parentComp) throws Exception {
        if (startingInstance.getSchemClass().getName().equals(ReactomeJavaConstants.ReferenceGeneProduct)) {
            String originalId = (String) startingInstance.getAttributeValue(ReactomeJavaConstants.identifier);
            if (originalId.equals(identifier))
                return startingInstance;
        }
        Collection c = adaptor.fetchInstanceByAttribute(ReactomeJavaConstants.ReferenceGeneProduct,
                                                        ReactomeJavaConstants.identifier,
                                                        "=",
                                                        identifier);
        if (c != null && c.size() > 0) {
            // The returned list may contain ReferneceGeneProduct's subclass.
            for (Iterator it = c.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                if (instance.getSchemClass().getName().equals(ReactomeJavaConstants.ReferenceGeneProduct))
                    return instance;
            }
        }
        // Need to create a new instance
        GKInstance rtn = ((XMLFileAdaptor) adaptor).createNewInstance(ReactomeJavaConstants.ReferenceGeneProduct);
        autoCreatedInstances.add(rtn);
        rtn.setAttributeValue(ReactomeJavaConstants.identifier, identifier);
        process(rtn, parentComp, true);
        return rtn;
    }
    
    private GKInstance getSpecies(String speciesName) throws Exception {
        // Some species name from UniProt is as this: Influenza A virus (strain A/Duck/Czechoslovakia/1956 H4N6)
        // We only need the first part. Text in parenthesises should be discarded.
        int index = speciesName.indexOf("(");
        if (index > 0)
            speciesName = speciesName.substring(0, index).trim();
        // Use _displayName since it might be a shell instance.
        Collection list = adaptor.fetchInstanceByAttribute("Species", "_displayName", "=", speciesName);
        GKInstance species = null;
        if (list.size() > 0)
            species = (GKInstance) list.iterator().next();
        if (species == null) {
            if (adaptor instanceof XMLFileAdaptor) {
                species = ((XMLFileAdaptor)adaptor).createNewInstance("Species");
                species.setAttributeValue("name", speciesName);
                InstanceDisplayNameGenerator.setDisplayName(species);
                autoCreatedInstances.add(species);
            }
        }
        return species;
    }
    
    private GKInstance getUniProtInstance() throws Exception {
        return getReferenceDatabasae("UniProt");
    }
    
    /**
     * Get the comment. To avoid the daunting parsing from the XML document, the UniProt web site
     * is queried for the second time to get the flat file.
     * @param instance
     * @param parentComp
     * @throws Exception
     */
    private void processComment(GKInstance instance, Component parentComp) throws Exception {
        Object identifier = getRequiredAttribute(instance);
        URL url = new URL(UNIPROT_DOWNLOAD_URL + identifier + UNIPROT_FLAT_FORMAT);
        InputStream input = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line = null;
        List commentLines = new ArrayList();
        // An exmpale:
//        CC   -!- FUNCTION: Adapter protein implicated in the regulation of a large
//        CC       spectrum of both general and specialized signaling pathway. Binds
//        CC       to a large number of partners, usually by recognition of a
//        CC       phosphoserine or phosphothreonine motif. Binding generally results
//        CC       in the modulation of the activity of the binding partner.
//        CC   -!- SUBUNIT: Homodimer.
        while ((line = reader.readLine()) != null) {
             if (line.startsWith("CC")) {
                 line = line.substring(2).trim();
                 if (line.startsWith("-------")) 
                     break; // Just comment
                 commentLines.add(line);
             }
        }
        reader.close();
        input.close();
        // Parse the actual line
        List comments = new ArrayList();
        StringBuffer buffer = null;
        for (Iterator it = commentLines.iterator(); it.hasNext();) {
            line = (String) it.next();
            if (line.startsWith("-!-")) {
                // Save the old one
                if (buffer != null)
                    comments.add(buffer.toString());
                buffer = new StringBuffer();
                buffer.append(line.substring(4));
            }
            else if (buffer != null) // Need an extra space here
                buffer.append(" ").append(line);
        }
        // Add comments to instance
        for (Iterator it = comments.iterator(); it.hasNext();) {
            instance.addAttributeValue("comment", it.next().toString());
        }
    }
}
