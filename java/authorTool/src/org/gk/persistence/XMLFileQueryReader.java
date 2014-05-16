/*
 * Created on Feb 20, 2009
 *
 */
package org.gk.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * This XML file reader is used to load DB_IDs and other essential information (e.g default person)
 * from a Reactome Curator Tool project.
 * @author wgm
 *
 */
public class XMLFileQueryReader {
    private List<Long> dbIds;
    private Long defaultPerson;
    
    public XMLFileQueryReader() {
    }
    
    /**
     * The main method to parse a curator tool project.
     * @param fileName the full file name including the whole path.
     * @throws Exception
     */
    public void read(File file) throws Exception {
        if (dbIds == null)
            dbIds = new ArrayList<Long>();
        else
            dbIds.clear();
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        Element root = document.getRootElement();
        // Use some XPath to speed up the performance
        String path = "//instance"; // Get all instances
        List nodes = XPath.selectNodes(root, path);
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            Element elm = (Element) it.next();
            String isShell = elm.getAttributeValue("isShell");
            if (isShell.equals("true"))
                continue;
            String dbId = elm.getAttributeValue("DB_ID");
            dbIds.add(new Long(dbId));
        }
        // Read the default person is any
        path = "//defaultPerson";
        Element elm = (Element) XPath.selectSingleNode(root, path);
        if (elm != null) {
            String value = elm.getAttributeValue("dbId");
            if (value != null && value.length() > 0) {
                defaultPerson = new Long(value);
            }
        }
    }
    
    public List<Long> getNonShellDBIds() {
        return dbIds;
    }
    
    public Long getDefaultPersonId() {
        return this.defaultPerson;
    }
    
}
