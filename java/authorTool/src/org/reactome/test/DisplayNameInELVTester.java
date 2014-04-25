/*
 * Created on Apr 27, 2011
 *
 */
package org.reactome.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.junit.Test;

/**
 * This class is used to check different uses of display names.
 * @author wgm
 *
 */
public class DisplayNameInELVTester {
    
    public DisplayNameInELVTester() {
        
    }
    
    public void checkWithShortestName() throws Exception {
        String dirName = "/Users/wgm/Documents/wgm/work/reactome/";
    }
    
    @Test
    public void checkPathwayDiagrams() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "test_slice_37b",
                                            "authortool",
                                            "T001test");
        Collection<?> c = dba.fetchInstancesByClass(ReactomeJavaConstants.PathwayDiagram);
        dba.loadInstanceAttributeValues(c, new String[]{ReactomeJavaConstants.storedATXML});
        String regex = "reactomeId=\"((\\d)+)\"";
        Pattern pattern = Pattern.compile(regex);
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            GKInstance inst = (GKInstance) it.next();
            String xml = (String) inst.getAttributeValue(ReactomeJavaConstants.storedATXML);
//            System.out.println(xml);
//            break;
            // Get the first line
            String[] lines = xml.split("\n");
//            System.out.println(lines[1]);
            // Get id
            Matcher matcher = pattern.matcher(lines[1]);
            if(matcher.find()) {
                String idText = matcher.group(1);
//                System.out.println("ID: " + idText);
                Long dbId = new Long(idText);
                GKInstance pathway = dba.fetchInstance(dbId);
                if (pathway == null) {
                    System.out.println(inst + " has null pathways title!");
                    System.out.println(lines[1]);
                }
            }
        }
    }
    
}
