/*
 * Created on Aug 12, 2011
 *
 */
package org.reactome.core.controller;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.OutputStream;
import java.util.*;

/**
 * This class is used to export gene set in XML.
 * @author gwu
 */
public class GeneSetXMLExporter {
    
    public GeneSetXMLExporter() {
    }
    
    public void exportToXML(MySQLAdaptor dba, 
                            Map<String, String> parameters, 
                            OutputStream os) throws Exception {
        // Fetch human pathways only
        GKInstance human = dba.fetchInstance(48887L);
        Collection<?> pathways = dba.fetchInstanceByAttribute(ReactomeJavaConstants.Pathway,
                                                              ReactomeJavaConstants.species,
                                                              "=",
                                                              human);
        // Get a list of pathways having pathway diagrams drawn
        List<GKInstance> selectedPathways = new ArrayList<GKInstance>();
        for (Iterator<?> it = pathways.iterator(); it.hasNext();) {
            GKInstance pathway = (GKInstance) it.next();
            // Check if there is a pathway diagram for it
            Collection<?> c = dba.fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                           ReactomeJavaConstants.representedPathway,
                                                           "=",
                                                           pathway);
            if (c != null && c.size() > 0)
                selectedPathways.add(pathway);
        }
        // Generate an XML document
        Document document = new Document();
        Element rootElement = new Element("Pathways");
        document.setRootElement(rootElement);
        for (GKInstance pathway : selectedPathways) {
            Element pathwayElm = generatePathwayElm(pathway);
            rootElement.addContent(pathwayElm);
        }
        // Export JDOM document
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(document, os);
    }
    
    private Element generatePathwayElm(GKInstance pathway) throws Exception {
        Element pathwayElm = createPathwayElm(pathway);
        Set<GKInstance> participants = InstanceUtilities.grepPathwayParticipants(pathway);
        if (participants != null && participants.size() > 0) {
            for (GKInstance participant : participants) {
                Element participantElm = generateParticipantElm(participant);
                pathwayElm.addContent(participantElm);
            }
        }
        return pathwayElm;
    }

    private Element createPathwayElm(GKInstance pathway) throws InvalidAttributeException, Exception {
        Element pathwayElm = new Element("Pathway");
        // Export some values
        pathwayElm.setAttribute("dbId", pathway.getDBID().toString());
        GKInstance stableId = (GKInstance) pathway.getAttributeValue(ReactomeJavaConstants.stableIdentifier);
        if (stableId != null)
            pathwayElm.setAttribute("stableId", 
                                    (String) stableId.getAttributeValue(ReactomeJavaConstants.identifier));
        pathwayElm.setAttribute("displayName", pathway.getDisplayName());
        return pathwayElm;
    }
    
    private Element generateParticipantElm(GKInstance participant) throws Exception {
        Element partElm = new Element(participant.getSchemClass().getName());
        // Simple attributes
        partElm.setAttribute("dbId", participant.getDBID().toString());
        // Get a simple name
        String name = (String) participant.getAttributeValue(ReactomeJavaConstants.name);
        partElm.setAttribute("displayName", name);
        // Need to get a list of genes if any
        Set<GKInstance> referenceEntities = InstanceUtilities.grepRefPepSeqsFromPhysicalEntity(participant);
        if (referenceEntities != null && referenceEntities.size() > 0) {
            Set<String> geneNames = new HashSet<String>();
            for (GKInstance refEntity : referenceEntities) {
                if (refEntity.getSchemClass().isValidAttribute(ReactomeJavaConstants.geneName)) {
                    String geneName = (String) refEntity.getAttributeValue(ReactomeJavaConstants.geneName);
                    if (geneName != null)
                        geneNames.add(geneName);
                }
            }
            for (String geneName : geneNames) {
                Element geneElm = new Element("Gene");
                geneElm.setAttribute("name", geneName);
                partElm.addContent(geneElm);
            }
        }
        return partElm;
    }
    
//    @Test
//    public void testExport() throws Exception {
//        long time1 = System.currentTimeMillis();
//        MySQLAdaptor dba = new MySQLAdaptor("localhost",
//                                            "gk_current_ver42",
//                                            "root",
//                                            "macmysql01");
//        FileOutputStream fos = new FileOutputStream("GeneSetInXML.xml");
//        exportToXML(dba,
//                    new HashMap<String, String>(),
//                    fos);
//        long time2 = System.currentTimeMillis();
//        System.out.println("Total time: " + (time2 - time1));
//    }
}
