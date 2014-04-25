/*
 * Created on May 2, 2005
 */
package org.gk.scripts;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;



/**
 * 
 * @author wgm
 */
public class DatabaseComparison {
    private MySQLAdaptor localDBA;
    private MySQLAdaptor remoteDBA;
    
    public DatabaseComparison(MySQLAdaptor localDBA, MySQLAdaptor remoteDBA) {
        this.localDBA = localDBA;
        this.remoteDBA = remoteDBA;
    }
    
    public void compare(String className) throws Exception {
        compare(className, localDBA, remoteDBA);
        compare(className, remoteDBA, localDBA);
    }
    
    private void compare(String className, MySQLAdaptor sourceDBA, MySQLAdaptor targetDBA) throws Exception {
        Collection sourceInstances = sourceDBA.fetchInstancesByClass(className);
        StringBuffer dbIDs = new StringBuffer();
        GKInstance reaction = null;
        for (Iterator it = sourceInstances.iterator(); it.hasNext();) {
            reaction = (GKInstance) it.next();
            dbIDs.append(reaction.getDBID());
            if (it.hasNext())
                dbIDs.append(",");
        }
        Connection targetConn = targetDBA.getConnection();
        Statement stat = targetConn.createStatement();
        String query = "SELECT DB_ID FROM " + className + " WHERE DB_ID NOT IN (" +
                       dbIDs + ")";
        ResultSet result = stat.executeQuery(query);
        System.out.println(className + " in " + targetDBA.toString() + " but not in " + sourceDBA + ":");
        int c = 0;
        dbIDs.setLength(0);
        while (result.next()) {
            long dbID = result.getLong(1);
            dbIDs.append(dbID);
            dbIDs.append(", ");
            c ++;
        }
        System.out.println(dbIDs);
        System.out.println("Total: " + c);        
    }
    

    public static void main(String[] args) {
        try {
            MySQLAdaptor localDBA = new MySQLAdaptor("localhost",
                                                     "test_slicing",
                                                     "wgm",
                                                     "wgm",
                                                     3306);
            MySQLAdaptor remoteDBA = new MySQLAdaptor("brie8",
                                                      "test_slicing",
                                                      "authortool",
                                                      "T001test",
                                                      3306);
            DatabaseComparison comparer = new DatabaseComparison(localDBA, remoteDBA);
            comparer.compare("Reaction");
            comparer.compare("Pathway");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
}
