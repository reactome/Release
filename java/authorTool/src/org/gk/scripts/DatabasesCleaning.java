/*
 * Created on Dec 6, 2011
 *
 */
package org.gk.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gk.database.ReverseAttributePane;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.FileUtilities;
import org.junit.Test;

/**
 * Some programs to clean-up databases at reactomedev.
 * @author gwu
 *
 */
public class DatabasesCleaning {
    
    public DatabasesCleaning() {
    }
    
    @Test
    public void checkInstancesInClasses() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "gk_central_092412",
                                            "root", 
                                            "macmysql01");
        Map<String, Long> classToCounts = dba.getAllInstanceCounts();
        long total = 0;
        for (String clsName : classToCounts.keySet()) {
            Long count = classToCounts.get(clsName);
            total += count;
            System.out.println(clsName + ": " + count);
        }
        System.out.println("Total: " + total);
    }
    
    @Test
    public void searchForInnodbs() throws Exception {
        Connection connect = connect();
        Statement stat = connect.createStatement();
        String query = "SHOW DATABASES";
        ResultSet results = stat.executeQuery(query);
        List<String> dbNames = new ArrayList<String>();
        while (results.next()) {
            String dbName = results.getString(1);
            dbNames.add(dbName);
        }
        results.close();
        System.out.println("Total dbs: " + dbNames.size());
        
        for (String dbName : dbNames) {
            if (dbName.endsWith(";") || dbName.endsWith(")") ||
                dbName.equals("test_slice_30"))
                continue;
            System.out.println(dbName);
            query = "USE " + dbName;
            stat.executeUpdate(query);
            query = "SHOW TABLE STATUS";
            results = stat.executeQuery(query);
            int count = 0;
            while (results.next()) {
                count ++;
                String engine = results.getString(2);
                if (engine == null)
                    continue;
                if (engine.equalsIgnoreCase("innodb")) {
                    System.out.println("\tIs Innodb");
                    break;
                }
            }
            if (count == 0)
                System.out.println("\tIs empty!");
            results.close();
        }
        stat.close();
        connect.close();
    }
    
    @Test
    public void dropInnodbs() throws Exception {
        String dirName = "/Users/gwu/Documents/wgm/work/reactome/";
//        String fileName = dirName + "reactomedev_db_list_120611.txt";
//        String fileName = dirName + "reactomedev_db_list_120611_droplist_1.txt";
//        String fileName = dirName + "reactomedev_db_list_120611_droplist_2.txt";
//        String fileName = dirName + "reactomedev_db_list_120611_droplist_3.txt";
        String fileName = dirName + "reactomedev_db_list_120611_droplist_5.txt";
        FileUtilities fu = new FileUtilities();
        fu.setInput(fileName);
        String line = null;
        List<String> dbNames = new ArrayList<String>();
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("( )+");
//            System.out.println(tokens[1]);
//            dbNames.add(tokens[1]);
            System.out.println(tokens[0]);
            dbNames.add(tokens[0]);
        }
        fu.close();
//        if (true)
//            return;
        Connection connect = connect();
        Statement stat = connect.createStatement();
        for (String dbName : dbNames) {
            System.out.println("Drop " + dbName);
            String query = "DROP DATABASE " + dbName;
            int result = stat.executeUpdate(query);
            System.out.println("Result: " + result);
        }
        stat.close();
        connect.close();
    }
    
    @Test
    public void dropTestDatabases() throws Exception {
        Connection connect = connect();
        String query = "SHOW DATABASES";
        Statement stat = connect.createStatement();
        ResultSet results = stat.executeQuery(query);
        List<String> testDatabases = new ArrayList<String>();
        while (results.next()) {
            String name = results.getString(1);
            if (name.startsWith("test_reactome_3232")) {
//            if (name.startsWith("test_") && name.contains("slice")) {
                System.out.println(name);
                testDatabases.add(name);
            }
        }
        results.close();
        System.out.println("Total test databases: " + testDatabases.size());
        for (String name : testDatabases) {
            if (name.equals("test_slice)") || name.endsWith(";") || name.equals("test_slice_30"))
                continue; // Cannot work
            System.out.println("Dropping " + name + "...");
            query = "DROP DATABASE " + name;
            int result = stat.executeUpdate(query);
            System.out.println("Dropped database: " + name);
//            break;
        }
        stat.close();
        connect.close();
    }
    
    @Test
    public void testDropDb() throws Exception {
        Connection connect = connect();
        String query = "SHOW DATABASES";
        Statement stat = connect.createStatement();
        ResultSet results = stat.executeQuery(query);
        while (results.next()) {
            String name = results.getString(1);
            System.out.println(name);
        }
        results.close();
        query = "DROP DATABASE test_slice_32h";
        int result = stat.executeUpdate(query);
        System.out.println("Drop database: " + result);
        stat.close();
        connect.close();
    }
    
    private Connection connect() throws SQLException {
        installDriver();
        String connectionStr = "jdbc:mysql://reactome.oicr.on.ca:3306/test_reactome_39";
        Properties prop = new Properties();
        prop.setProperty("user", "authortool");
        prop.setProperty("password", "T001test");
        prop.setProperty("autoReconnect", "true");
        // The following two lines have been commented out so that
        // the default charset between server and client can be used.
        // Right now latin1 is used in both server and gk_central database.
        // If there is any non-latin1 characters are used in the application,
        // they should be converted to latin1 automatically (not tested).
        //prop.setProperty("useUnicode", "true");
        //prop.setProperty("characterEncoding", "UTF-8");
        prop.setProperty("useOldUTF8Behavior", "true");
        // Some dataTime values are 0000-00-00. This should be convert to null.
        prop.setProperty("zeroDateTimeBehavior", "convertToNull");
        // Please see http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-configuration-properties.html
        // for new version of JDBC driver: 5.1.7. This is very important, esp. in the slicing tool. Otherwise,
        // an out of memory exception will be thrown very easy even with a very large heap size assignment.
        // However, apparently this property can work with 5.0.8 version too.
        prop.setProperty("dontTrackOpenResources", "true");
//      // test code
//      prop.put("profileSQL", "true");
//      prop.put("slowQueryThresholdMillis", "0");
//      prop.put("logSlowQueries", "true");
//      prop.put("explainSlowQueries", "true");
        
        Connection conn = DriverManager.getConnection(connectionStr, prop);
        return conn;
        //conn = DriverManager.getConnection(connectionStr, username, password);
    }
    
    private void installDriver() {
        String dbDriver = "com.mysql.jdbc.Driver";
        try {
            Class.forName(dbDriver).newInstance();
        } catch (Exception e) {
            System.err.println("Failed to load database driver: " + dbDriver);
            e.printStackTrace();
        }
    }
    
    /**
     * There are 393 instances starting with SO: which have not been used at all! These instances
     * will be deleted using this method.
     * @throws Exception
     */
    @Test
    public void deleteUnwantedSOInstances() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca", 
                                            "gk_central",
                                            "authortool",
                                            "T001test");
        Collection<GKInstance> soInstances = dba.fetchInstanceByAttribute(ReactomeJavaConstants.DatabaseIdentifier,
                                                                          ReactomeJavaConstants._displayName, 
                                                                          "like", 
                                                                          "SO:%");
        System.out.println("Total SO Instances: " + soInstances.size());
        // An internal check 
//        GKInstance so = dba.fetchInstance(5L);
//        soInstances.add(so);
        ReverseAttributePane helper = new ReverseAttributePane();
        for (GKInstance inst : soInstances) {
            Map<String, List<GKInstance>> referrers = helper.getReferrersMapForDBInstance(inst);
            if (referrers != null && referrers.size() > 0)
                throw new IllegalStateException(inst + " has referrers: " + referrers.size());
        }
        try {
            dba.startTransaction();
            for (GKInstance inst : soInstances) {
                dba.deleteInstance(inst);
            }
            dba.commit();
        }
        catch(Exception e) {
            dba.rollback();
        }
        dba.cleanUp();
    }
}
