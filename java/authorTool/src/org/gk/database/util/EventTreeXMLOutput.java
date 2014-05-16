/*
 * Created on Mar 18, 2005
 *
 */
package org.gk.database.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.database.EventTreeBuildHelper;
import org.gk.database.HierarchicalEventPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.w3c.dom.Document;

/**
 * This utility class is used to output the event tree as an xml file.
 * @author wgm
 *
 */
public class EventTreeXMLOutput {
    private Map speciesMap;
    private Map speciesEventMap;

    public EventTreeXMLOutput() {
        speciesMap = new HashMap();
        speciesMap.put("Homo sapiens", "hsa");
        speciesMap.put("Mus musculus", "mmu");
        speciesMap.put("Rattus norvegicus", "rno");
        speciesMap.put("Gallus gallus", "gga");
        speciesMap.put("Fugu rubripes", "fru");
        speciesMap.put("Danio rerio", "dre");
        speciesEventMap = new HashMap();
    }
    
//    public void outputTree(MySQLAdaptor dba, String fileName) throws Exception {
//		EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
//		Collection topLevelPathways = treeHelper.getTopLevelEvents();
//		Collection c = treeHelper.getAllEvents();
//		treeHelper.loadAttribtues(c);
//		ArrayList list = new ArrayList(topLevelPathways);
//		HierarchicalEventPane eventPane = new HierarchicalEventPane();
//		eventPane.setTopLevelEvents(list);
//		Document doc = eventPane.convertTreeToXML();
//		FileOutputStream xmlOut = new FileOutputStream(fileName);
//		TransformerFactory tffactory = TransformerFactory.newInstance();
//		Transformer transformer = tffactory.newTransformer();
//		DOMSource source = new DOMSource(doc);
//		StreamResult result = new StreamResult(xmlOut);
//		transformer.transform(source, result);
//    }
    
    public void outputTree(MySQLAdaptor dba, String fileName) throws Exception {
        // Get the front page first
        GKInstance frontPage = (GKInstance) dba.fetchInstancesByClass("FrontPage").iterator().next();
        List topLevelPathways = frontPage.getAttributeValuesList("frontPageItem");
        getOrthologous(topLevelPathways);
		EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
		Collection c = treeHelper.getAllEvents();
		treeHelper.loadAttribtues(c);
		HierarchicalEventPane eventPane = new HierarchicalEventPane();
		for (Iterator it = speciesEventMap.keySet().iterator(); it.hasNext();) {
		    String speciesName = (String) it.next();
		    List events = (List) speciesEventMap.get(speciesName);
            eventPane.setTopLevelEvents(events);
            Document doc = eventPane.convertTreeToXML();
            File file = new File(fileName + "." + speciesName);
            GKApplicationUtilities.outputXML(doc, file);
        }
    }
    
    public void outputTreeForAllEvents(MySQLAdaptor dba, String fileName, String speciesName) throws Exception {
        EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
        Collection topEvents = treeHelper.getTopLevelEvents();
        Collection allEvents = treeHelper.getAllEvents();
        treeHelper.loadAttribtues(allEvents);
        HierarchicalEventPane eventPane = new HierarchicalEventPane();
        eventPane.setTopLevelEvents(new ArrayList(topEvents));
        eventPane.setSelectedSpecies("Homo sapiens");
        Document doc = eventPane.convertTreeToXML();
        File file = new File(fileName);
        GKApplicationUtilities.outputXML(doc, file);
    }
    
    public void outputTreeForAllPathwaysToSimpleText(MySQLAdaptor dba, String fileName, String speciesName) throws Exception {
        EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
        Collection topEvents = treeHelper.getTopLevelEvents();
        Collection allEvents = treeHelper.getAllEvents();
        treeHelper.loadAttribtues(allEvents);
        List topList = new ArrayList(topEvents);
        InstanceUtilities.sortInstances(topList);
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = topList.iterator(); it.hasNext();) {
            GKInstance event = (GKInstance) it.next();
            if (event.getSchemClass().isa("Reaction"))
                continue;
            GKInstance taxon = (GKInstance) event.getAttributeValue("taxon");
            if (taxon == null)
                continue;
            if (!taxon.getDisplayName().equals(speciesName))
                continue;
            outputToSimpleText(event, buffer, "");
        }
        FileWriter fileWrite = new FileWriter(fileName);
        BufferedWriter writer = new BufferedWriter(fileWrite);
        writer.write(buffer.toString());
        writer.close();
        fileWrite.close();
    }
    
    private void outputToSimpleText(GKInstance event, StringBuffer buffer, String indent) throws Exception {
        buffer.append(indent).append("[").append(event.getDBID()).append("]").append(event.getDisplayName()).append("\n");
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List components = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (components == null || components.size() == 0)
                return;
            for (Iterator it = components.iterator(); it.hasNext();) {
                GKInstance sub = (GKInstance) it.next();
                if (sub.getSchemClass().isa("Reaction"))
                    continue;
                outputToSimpleText(sub, buffer, indent + "    ");
            }
        }
    }
    
    private void getOrthologous(List topLevelPathways) throws Exception {
        // The list in FrontPage is for homo sapiens
        speciesEventMap.put(speciesMap.get("Homo sapiens"), topLevelPathways);
        // Need to get the list for other species
        GKInstance event = null;
        GKInstance species = null;
        String speciesName = null;
        for (Iterator it = topLevelPathways.iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            List orthoEvents = event.getAttributeValuesList("orthologousEvent");
            for (Iterator it1 = orthoEvents.iterator(); it1.hasNext();) {
                GKInstance tmp = (GKInstance) it1.next();
                species = (GKInstance) tmp.getAttributeValue("taxon");
                speciesName = (String) speciesMap.get(species.getDisplayName());
                List list = (List) speciesEventMap.get(speciesName);
                if (list == null) {
                    list = new ArrayList();
                    speciesEventMap.put(speciesName, list);
                }
                list.add(tmp);
            }
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Usage: java org.gk.database.util.EventTreeXMLOutput dbhost dbName user pwd port outputFileName");
            System.exit(1);
        }
        try {
            MySQLAdaptor dba = new MySQLAdaptor(args[0],
                            		            args[1],
                                                args[2],
                                                args[3],
                                                Integer.parseInt(args[4]));
            String fileName = args[5];
            System.out.println("Starting outputing...");
            //new EventTreeXMLOutput().outputTree(dba, fileName);
            new EventTreeXMLOutput().outputTreeForAllPathwaysToSimpleText(dba, fileName, "Homo sapiens");
            System.out.println("Done outputing");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
        
}
