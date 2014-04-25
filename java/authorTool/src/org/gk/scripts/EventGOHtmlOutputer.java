/*
 * Created on May 18, 2005
 */
package org.gk.scripts;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.gk.database.EventTreeBuildHelper;
import org.gk.database.HierarchicalEventPane;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to output event tree to a html file containing linkings to GO terms.
 * @author wgm
 */
public class EventGOHtmlOutputer {
    private final String url = "http://brie8.cshl.org/cgi-bin/eventbrowser?DB=gk_central&ID=";
    private final String goURL = "http://www.ebi.ac.uk/ego/QuickGO?mode=display&entry=";
    private final String IsAImage = "<img src=\"IsA.gif\"/>";
    private final String partOfImage = "<img src=\"PartOf.gif\"/>";
    private Map node2IconMap = null;
    private Icon isAIcon;
    private Icon partOfIcon;
    
    public EventGOHtmlOutputer() {
        isAIcon = GKApplicationUtilities.getIsAIcon();
        partOfIcon = GKApplicationUtilities.getIsPartOfIcon();
    }
    
    public void generateHTML(MySQLAdaptor dba) throws Exception {
        EventTreeBuildHelper helper = new EventTreeBuildHelper(dba);
        preLoadAttributes(dba);
        Collection topEvents = helper.getTopLevelEvents();
        HierarchicalEventPane eventPane = new HierarchicalEventPane();
        eventPane.setTopLevelEvents(new ArrayList(topEvents));
        node2IconMap = eventPane.getNode2IconMap();
        DefaultTreeModel model = (DefaultTreeModel) eventPane.getTree().getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>\n");
        buffer.append("<header>\n");
        buffer.append("<title>Reactome Events</title>\n");
        buffer.append("<style type=\"text/css\">\n");
        buffer.append("<!--\n");
        buffer.append("a.with {color: blue}\n");
        buffer.append("a.without {color: black}\n");
        buffer.append("a.withBoth {color: green}\n");
        buffer.append("a.goBP {color: teal}\n");
        buffer.append("a.goCompartment {color: cyan}\n");
        buffer.append("-->\n");
        buffer.append("</style>\n");
        buffer.append("</header>\n");
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) root.getChildAt(i);
            generateHTML(childNode, buffer);
        }
        buffer.append("</html>\n");
        try {
            FileWriter writer = new FileWriter("/home/wgm/gkteam/suzi/reactomeEvents.html");
            writer.write(buffer.toString());
            writer.flush();
            writer.close(); 
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void preLoadAttributes(MySQLAdaptor dba) throws Exception {
        Collection events = dba.fetchInstancesByClass("Pathway");
        SchemaClass pathwayCls = dba.getSchema().getClassByName("Pathway");
        SchemaAttribute att = pathwayCls.getAttribute("hasComponent");
        dba.loadInstanceAttributeValues(events, att);
        att = dba.getSchema().getClassByName("GenericEvent").getAttribute("hasInstance");
        dba.loadInstanceAttributeValues(events, att);
        Collection goBPs = dba.fetchInstancesByClass("GO_BiologicalProcess");
        att = dba.getSchema().getClassByName("GO_BiologicalProcess").getAttribute("accession");
        dba.loadInstanceAttributeValues(goBPs, att);
    }
    
    private void generateHTML(DefaultMutableTreeNode node, StringBuffer buffer) throws Exception {
        GKInstance event = (GKInstance) node.getUserObject();
        Icon icon = (Icon) node2IconMap.get(node);
        generateLink(event, icon, buffer);
        int size = node.getChildCount();
        if (size > 0) {
            buffer.append("<ul>\n");
            for (int i = 0; i < size; i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
                generateHTML(childNode, buffer);
            }
            buffer.append("</ul>\n");
        }
    }
    
    private void generateLink(GKInstance event, Icon icon, StringBuffer buffer) throws Exception {
        if (event.getDisplayName() == null)
            return;
        buffer.append("<li>");
        if (icon == isAIcon)
            buffer.append(IsAImage);
        else if (icon == partOfIcon)
            buffer.append(partOfImage);
        buffer.append("<a href=\"");
        buffer.append(url + event.getDBID());
        buffer.append("\" target=\"reactome\"");
        // Check GO assignment
        GKInstance goBP = (GKInstance) event.getAttributeValue("goBiologicalProcess");
        GKInstance goCompartment = (GKInstance) event.getAttributeValue("compartment");
        if (goBP != null && goCompartment != null)
            buffer.append(" class=\"withBoth\">");
        else if (goBP != null || goCompartment != null)
            buffer.append(" class=\"with\">");
        else
            buffer.append(" class=\"without\">");
        buffer.append(event.getDisplayName());
        buffer.append("</a>");
        if (goBP == null && goCompartment == null)
            buffer.append("</li>\n");
        else {
            if (goBP != null) {
                buffer.append(" [");
                String accession = (String) goBP.getAttributeValue("accession");
                String goTermName = "GO:" + accession;
                buffer.append("<a href=\"");
                buffer.append(goURL);
                buffer.append(goTermName);
                buffer.append("\" target=\"go\" class=\"goBP\">");
                buffer.append(goTermName);
                buffer.append("</a>]");
            }
            if (goCompartment != null) {
                buffer.append(" [");
                String accession = (String) goCompartment.getAttributeValue("accession");
                String goTermName = "GO:" + accession;
                buffer.append("<a href=\"");
                buffer.append(goURL);
                buffer.append(goTermName);
                buffer.append("\" target=\"go\" class=\"goCompartment\">");
                buffer.append(goTermName);
                buffer.append("</a>]");                
            }
            buffer.append("</li>\n");
        }
    }    
    
    public static void main(String[] args) {
        try {
            MySQLAdaptor dba = new MySQLAdaptor("brie8",
                                                "gk_central",
                                                "authortool",
                                                "T001test",
                                                3306);
            new EventGOHtmlOutputer().generateHTML(dba);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
