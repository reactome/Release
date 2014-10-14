/*
 * Created on Jan 16, 2013
 *
 */
package org.gk.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * @author gwu
 *
 */
public class MySQLBinaryLogAnalyzer {
    
    public MySQLBinaryLogAnalyzer() {
        
    }
    
    @Test
    public void checkDeletion() throws Exception {
        String fileName = "/Users/gwu/Documents/mysqldumps/gk_central.000114_1.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        List<Long> dbIds = new ArrayList<Long>();
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("DELETE FROM DatabaseObject WHERE DB_ID")) {
                // Get DB_ID from this line
                Pattern pattern = Pattern.compile("(\\d)+");
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    String dbId = matcher.group(0);
                    dbIds.add(new Long(dbId));
                }
            }
        }
        fu.close();
        Collections.sort(dbIds);
//        for (Long dbId : dbIds)
//            System.out.println(dbId);
        System.out.println("Total: " + dbIds.size());
        
        MySQLAdaptor dba = new MySQLAdaptor("localhost",
                                            "gk_central_011313", 
                                            "root",
                                            "macmysql01");
        StringBuilder builder = new StringBuilder();
        for (Long dbId : dbIds) {
            if (dba.exist(dbId)) {
                GKInstance instance = dba.fetchInstance(dbId);
                System.out.println(dbId + "\t" + instance);
                builder.append(dbId + "|");
            }
            else {
                System.out.println(dbId);
            }
        }
        System.out.println(builder.toString());
    }
    
    @Test
    public void checkIEReferrers() throws Exception {
        String fileName = "/Users/gwu/Documents/mysqldumps/gk_central.000114_1.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        List<Long> dbIds = new ArrayList<Long>();
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("INSERT INTO DatabaseObject SET DB_ID") &&
                    line.contains("_class='InstanceEdit'")) {
                // Get DB_ID from this line
                Pattern pattern = Pattern.compile("(\\d)+");
                Matcher matcher = pattern.matcher(line);
                if(matcher.find()) {
                    String dbId = matcher.group(0);
                    dbIds.add(new Long(dbId));
                }
            }
        }
        fu.close();
        System.out.println("Total IEs: " + dbIds.size());
        
        MySQLAdaptor dba = new MySQLAdaptor("reactomecurator.oicr.on.ca",
                                            "gk_central",
                                            "authortool", 
                                            "T001test");
        for (Long dbId : dbIds) {
            GKInstance ie = dba.fetchInstance(dbId);
            Collection<GKInstance> referrers = ie.getReferers(ReactomeJavaConstants.modified);
            System.out.println(ie + "\t" + referrers.size());
            for (GKInstance referrer : referrers) {
                if (referrer.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagramItem))
                    continue;
                System.out.println(referrer);
            }
            System.out.println();
        }
    }
    
}
