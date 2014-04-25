/*
 * Created on Sep 28, 2011
 *
 */
package org.gk.scripts;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

/**
 * This class is used to generate the pathway hierachy as displayed in the web pathway browser.
 * @author gwu
 *
 */
public class PathwayHierachyGenerator {
    
    public PathwayHierachyGenerator() {
    }
    
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage java -Xmx1024m dbHost dbName dbUser dbPwd dbPort directory");
            System.exit(1);
        }
        try {
            MySQLAdaptor dba = new MySQLAdaptor(args[0], 
                                                args[1],
                                                args[2],
                                                args[3],
                                                new Integer(args[4]));
            PathwayHierachyGenerator generator = new PathwayHierachyGenerator();
            generator.generateHierarchies(dba, args[5]);
            // Want to zip all XML files
            GKApplicationUtilities.zipDir(new File(args[5]),
                                          new File(args[5], "PathwayHierarchy.zip"), 
                                          "xml", 
                                          false);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * @throws Exception
     */
    public void generateHierarchies(MySQLAdaptor dba, 
                                    String dir) throws Exception {
        Integer release = dba.getReleaseNumber();
        String releaseLabel = (release == null ? "" : "release_" + release.toString());
                
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        // There should be only one FrontPageItem
        GKInstance frontPageItem = (GKInstance) c.iterator().next();
        // The following is used to get the whole list of species
        Set<GKInstance> speciesSet = new HashSet<GKInstance>();
        List<?> items = frontPageItem.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
        for (Object obj : items) {
            GKInstance event = (GKInstance) obj;
            GKInstance species = (GKInstance) event.getAttributeValue(ReactomeJavaConstants.species);
            speciesSet.add(species);
            List<?> orthologousEvents = event.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
            for (Object obj1 : orthologousEvents) {
                GKInstance orthologousEvent = (GKInstance) obj1;
                species = (GKInstance) orthologousEvent.getAttributeValue(ReactomeJavaConstants.species);
                speciesSet.add(species);
            }
        }
        for (GKInstance species : speciesSet) {
            Element root = new Element("Pathways");
            root.setAttribute("Species", species.getDisplayName());
            if (species.getDisplayName().equals("Homo sapiens")) {
                // Something special
                for (Object obj : items) {
                    GKInstance event = (GKInstance) obj;
                    addEvent(event, root);
                }
            }
            else {
                for (Object obj : items) {
                    GKInstance event = (GKInstance) obj;
                    List<?> orthologousEvents = event.getAttributeValuesList(ReactomeJavaConstants.orthologousEvent);
                    for (Object obj1 : orthologousEvents) {
                        GKInstance orthologousEvent = (GKInstance) obj1;
                        GKInstance species1 = (GKInstance) orthologousEvent.getAttributeValue(ReactomeJavaConstants.species);
                        if (species == species1) {
                            addEvent(orthologousEvent, root);
                            break;
                        }
                    }
                }
            }
            String fileName = "Pathway_Hierarchy_" + 
                              species.getDisplayName().replaceAll(" ", "_") + "_" +
                              releaseLabel + ".xml";
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(root, new FileOutputStream(new File(dir, fileName)));
        }
    }
    
    @Test
    public void generateHierarchyInXML() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_current_ver39",
                                            "root",
                                            "macmysql01");
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.FrontPage);
        GKInstance frontPageItem = (GKInstance) c.iterator().next();
        List<?> items = frontPageItem.getAttributeValuesList(ReactomeJavaConstants.frontPageItem);
        Element root = new Element("Pathways");
        for (Object obj : items) {
            GKInstance inst = (GKInstance) obj;
            addEvent(inst, root);
        }
        // Output
        String fileName = "tmp/PathwayHierarchy_HS_Release39.xml";
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(root, new FileOutputStream(fileName));
    }
    
    private void addEvent(GKInstance inst, Element parentElm) throws Exception {
        Element elm = new Element(inst.getSchemClass().getName());
        elm.setAttribute("DB_ID", inst.getDBID().toString());
        elm.setAttribute("DisplayName", inst.getDisplayName());
        parentElm.addContent(elm);
        // Add children
        if (inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List<?> children = inst.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            for (Object obj : children) {
                GKInstance childInst = (GKInstance) obj;
                addEvent(childInst, elm);
            }
        }
    }
    
}
