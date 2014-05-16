/*
 * Created on Nov 12, 2003
 */
package org.gk.persistence;

import java.awt.Component;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.PathwayDiagramInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.util.AuthorToolAppletUtilities;

/**
 * This is a factory class that create and mange PersistenceAdaptor for both
 * the database and local file system.
 * @author wugm
 */
public class PersistenceManager {
	// The sole instance
	private static PersistenceManager manager;
	// MySQL connectionion 
	private Map adaptorMap;
	// The active MySQLAdaptor
	private MySQLAdaptor activeDBAdaptor;
	// The active file adptor
	private XMLFileAdaptor activeFileAdaptor;
	//DB connecting info
	private Properties dbConnectInfo;
	
	public PersistenceManager() {
		adaptorMap = new HashMap();
	}
	
	public static PersistenceManager getManager() {
		if (manager == null)
			manager = new PersistenceManager();
		return manager;
	}
	
	public MySQLAdaptor getActiveMySQLAdaptor() {
		return activeDBAdaptor;
	}
	
	public void setActiveMySQLAdaptor(MySQLAdaptor adaptor) {
		this.activeDBAdaptor = adaptor;
	}
	
	public void setDBConnectInfo(Properties prop) {
		this.dbConnectInfo = prop;
	}
	
	public Properties getDBConnectInfo() {
		return this.dbConnectInfo;
	}
	
	/**
	 * Return a MySQLAdaptor for a specified MySQL db. Calling this method will
	 * automatically set the active MySQLAdaptor to the returned one.
	 */
	public MySQLAdaptor getMySQLAdaptor(String host, 
	                                    String dbName, 
	                                    String user, 
	                                    String pwd, 
	                                    int port) {
		// Check if there is already one adaptor existence. 
		ConnectInfo info = new ConnectInfo(host, dbName, user, pwd, port);
		MySQLAdaptor adaptor = (MySQLAdaptor) adaptorMap.get(info);
		if (adaptor == null) {
			try {
				adaptor = new MySQLAdaptor(host, dbName, user, pwd, port);
				// Keep the connection active using a dumb thread
				// Use 10 minutes
				adaptor.initDumbThreadForConnection(10 * 60 * 1000);
				adaptorMap.put(info, adaptor);
			}
			catch(SQLException e) {
				System.err.println("PersistenceAdaptor.getMySQLAdaptor(): " + e);
				e.printStackTrace();
			}
		}
		setActiveMySQLAdaptor((MySQLAdaptor)adaptor);
		return adaptor;
	}	
    
    /**
     * An overloaded method to get the active MySQLAdaptor. If no active MySQLAdaptor,
     * this method will try to initiate one automatically.
     * @param comp 
     * @return the active MySQLAdaptor.
     * @see getActiveMySQLAdaptor()
     */
	public MySQLAdaptor getActiveMySQLAdaptor(Component comp) {
	    MySQLAdaptor dba = getActiveMySQLAdaptor();
	    if (dba != null)
	        return dba;
	    dba = initMySQLAdaptor(comp);
	    if (dba != null) {
	        boolean compare = compareLocalAndDbSchema(dba);
	        if (!compare) {
	            int reply = JOptionPane.showConfirmDialog(comp,
	                                                      "The schema used by the local project is not the same as in the database.\n" + 
	                                                      "You can continue to use the current local schema. However, you may get\n" +
	                                                      "exception during checking in/out. It is strongly recommend to update\n" +
	                                                      "schema from the database.\n" +
	                                                      "Do you want to continue working with the current local schema?", 
	                                                      "DB and Local Schema Not Same", 
	                                                      JOptionPane.YES_NO_OPTION);
	            if (reply == JOptionPane.NO_OPTION) {
	                setActiveMySQLAdaptor(null);
	                dba = null;
	            }
	        }
	    }
	    if (dba == null) {
	        // Remove Password so that the connection dialog can be displayed again
	        dbConnectInfo.remove("dbPwd");
        }
	    return dba;
	}
	
	/**
	 * Compare timpestamps of local and db schema to make sure they are the same.
	 * @param dba
	 * @return true if the two schemas have the same timestamp or the comparison
	 * cannot be done (e.g. the local schema is not loaded. or no timestamps for old
	 * schemas).
	 * @throws Exception
	 */
	private boolean compareLocalAndDbSchema(MySQLAdaptor dba) {
	    if (getActiveFileAdaptor() == null)
	        return true; // No need to compare
	    String dbTS = dba.getSchema().getTimestamp();
	    if (dbTS == null)
	        dbTS = ""; // To make compare easier
	    String localTS = getActiveFileAdaptor().getSchema().getTimestamp();
	    if (localTS == null)
	        localTS = "";
	    return dbTS.equals(localTS);
	}
	
	/**
	 * Set up a MySQLAdaptor and set it as the active MySQLAdaptor based on the cached 
	 * connecting information. Method setDBConnectInfo() should be called first. Otherwise,
	 * the call to this method is deemed to fail.
	 * @return the initilized MySQLAdaptor.
	 */
	private MySQLAdaptor initMySQLAdaptor(Component comp) {
		String dbHost = dbConnectInfo.getProperty("dbHost");
		String dbName = dbConnectInfo.getProperty("dbName");
		String dbPort = dbConnectInfo.getProperty("dbPort");
		String dbUser = dbConnectInfo.getProperty("dbUser");
		String dbPwd = dbConnectInfo.getProperty("dbPwd");
		// To display the info if one of them is empty
		if (dbHost == null || dbHost.length() == 0 ||
		    dbName == null || dbName.length() == 0 ||
		    dbPort == null || dbPort.length() == 0 ||
		    dbUser == null || dbUser.length() == 0 ||
		    dbPwd == null || dbPwd.length() == 0) {
			DBConnectionPane connectionPane = new DBConnectionPane();
			connectionPane.setValues(dbConnectInfo);
			if(connectionPane.showInDialog(comp)) {
				connectionPane.commit();
				dbHost = dbConnectInfo.getProperty("dbHost");
				dbName = dbConnectInfo.getProperty("dbName");
				dbPort = dbConnectInfo.getProperty("dbPort");
				dbUser = dbConnectInfo.getProperty("dbUser");
				dbPwd = dbConnectInfo.getProperty("dbPwd");
			}
			else
				return null;
		}
		return getMySQLAdaptor(dbHost, dbName, dbUser, dbPwd, Integer.parseInt(dbPort));
	}
	
	public boolean initDatabaseConnection(Component comp) {
        // To avoid null exception
        if (dbConnectInfo == null) {
            try {
                dbConnectInfo = AuthorToolAppletUtilities.loadProperties("gkEditor.prop");
                // Need to decript the password
                String pwd = dbConnectInfo.getProperty("dbPwd");
                if (pwd != null) {
                    pwd = AuthorToolAppletUtilities.decrypt(pwd);
                    dbConnectInfo.setProperty("dbPwd", pwd);
                }
            }
            catch(IOException e) {
                dbConnectInfo = new Properties();
            }
        }
        MySQLAdaptor dbAdaptor = getActiveMySQLAdaptor(comp);
        return dbAdaptor != null;
	}
	
	public XMLFileAdaptor getFileAdaptor(String dir) {
		XMLFileAdaptor adaptor = (XMLFileAdaptor) adaptorMap.get(dir);
		if (adaptor == null) {
			try {
				adaptor = new XMLFileAdaptor(dir);
				adaptorMap.put(dir, adaptor);
			}
			catch(Exception e) {
				System.err.println("PersisrenceAdaptor.getFileAdaptor(): " + e);
				e.printStackTrace();
			}
		}
		return adaptor;
	}
	
	public void setActiveFileAdaptor(XMLFileAdaptor adaptor) {
		this.activeFileAdaptor = adaptor;
	}
	
	public XMLFileAdaptor getActiveFileAdaptor() {
		return this.activeFileAdaptor;
	}
	
    public void updateLocalSchema(Component comp) throws IOException {
        if (activeFileAdaptor == null)
            throw new IllegalArgumentException("PersistenceManager.updateLocalSchema(): No local file adaptor defined.");
        if (activeDBAdaptor == null)
            activeDBAdaptor = initMySQLAdaptor(comp);
        if (activeDBAdaptor == null) {
            throw new IllegalStateException("PersistenceManager.updateLocalSchema(): Cannot connect to the database.");
        }
        Schema schema = activeDBAdaptor.getSchema();
        activeFileAdaptor.saveSchema(schema);
    }
    
    /**
     * Update the local instance from the db instance. 
     * Note: changes of the local instance will be overwritten.
     * @param localCopy
     * @param dbCopy
     * @throws Exception
     */
    public void updateLocalFromDB(GKInstance localCopy,
                                  GKInstance dbCopy) throws Exception {
        GKSchemaAttribute att = null;
        String attName = null;
        for (Iterator it = localCopy.getSchemClass().getAttributes().iterator(); it.hasNext();) {
            att = (GKSchemaAttribute) it.next();
            attName = att.getName();
            // Just in case
            if (!dbCopy.getSchemClass().isValidAttribute(attName))
                continue;
            java.util.List valueList = dbCopy.getAttributeValuesList(attName);
            // Have to convert the referred DB Instances to the local instance
            if (valueList != null && valueList.size() > 0 && 
                    att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
                java.util.List localList = new ArrayList(valueList.size());
                for (Iterator it1 = valueList.iterator(); it1.hasNext();) {
                    GKInstance dbRefCopy = (GKInstance) it1.next();
                    GKInstance localRefCopy = getLocalReference(dbRefCopy);
                    if (localRefCopy != null)
                        localList.add(localRefCopy);
                }
                localCopy.setAttributeValueNoCheck(attName, localList);
            }
            else {
                if (valueList == null)
                    localCopy.setAttributeValueNoCheck(attName, null);
                else {
                    // A special case
                    // This is a special case
                    // TODO: This code is very ugly. Should consider use polymophism or
                    // other OOP tricks to simplify the code.
                    if (localCopy instanceof PathwayDiagramInstance &&
                        attName.equals(ReactomeJavaConstants.storedATXML)) {
                        String xml = (String) valueList.get(0);
                        XMLFileAdaptor fileAdaptor = (XMLFileAdaptor) localCopy.getDbAdaptor();
                        fileAdaptor.copyStoredATXMLFromDBToLocal(localCopy,
                                                                 xml);
                    }
                    else 
                        localCopy.setAttributeValueNoCheck(attName, new ArrayList(valueList));
                }
            }
        }
        // Set isShell to false in case shell instances are updated
        localCopy.setIsShell(false);
    }
    
	/**
	 * Get the local copy for the specified Instance object. A local copy has the
	 * same DB_ID as the specified instance. The method try to get a GKInstance from 
	 * the local file system by using the active FileAdaptor object. If it cannot be 
	 * found from the local file system, a shell copy will be created based on DB_ID
	 * and displayName of the specified instance. This shell copy will be saved in 
	 * the local file if the user saves all changes.
	 * @param instance can be either from db or from local.
	 */
	public GKInstance getLocalReference(GKInstance instance) {
		if (activeFileAdaptor == null)
			return null;
		if (instance.getDbAdaptor() == activeFileAdaptor)
			return instance; // instance is the local copy
		GKInstance localCopy = null;
		try {
            // This might be wrong if a local instance's class has been changed. It is 
            // possible to create a duplicate.
            // Have to use DB_ID only. Otherwise, if an instance's type has been changed,
            // a shell instance will be checked out. Two instsances will have the same DB_ID.
			//localCopy = activeFileAdaptor.fetchInstance(instance.getSchemClass().getName(),
		    //                                            instance.getDBID());
            localCopy = activeFileAdaptor.fetchInstance(instance.getDBID());
		}
		catch(Exception e) {
			System.err.println("PersistenceManager.getLocalCopy(): " + e);
			e.printStackTrace();
		}
		// Try to download a shell copy of instance from db
		if (localCopy == null) {
			localCopy = new GKInstance();
			localCopy.setDBID(instance.getDBID());
			localCopy.setDisplayName(instance.getDisplayName());
			// Have to use local copy of SchemaClass
			String clsName = instance.getSchemClass().getName();
			localCopy.setSchemaClass(activeFileAdaptor.getSchema().getClassByName(clsName));
			// This should be a shell copy
			localCopy.setIsShell(true);
			localCopy.setDbAdaptor(activeFileAdaptor);
			activeFileAdaptor.addNewInstance(localCopy);
		}
		return localCopy;
	}
	
	/**
	 * To store connecting info for comparison.
	 */
	public static class ConnectInfo {
		String host = "";
		String dbName = "";
		String user = "";
		String pwd = "";
		int port;
		
		public ConnectInfo(String host, String dbName, String user, String pwd,
		                   int port) {
			this.host = host;
			this.dbName = dbName;
			this.user = user;
			this.pwd = pwd;
			this.port = port;
		}
		
		public ConnectInfo(Properties prop) {
			host = prop.getProperty("dbHost", "");
			dbName = prop.getProperty("dbName", "");
			user = prop.getProperty("dbUser", "");
			pwd = prop.getProperty("dbPwd", "");
			String portStr = prop.getProperty("dbPort");
			if (portStr != null)
				port = Integer.parseInt(portStr);
		}
		
		public boolean equals(Object obj) {
			if (!(obj instanceof ConnectInfo))
				return false;
			ConnectInfo info = (ConnectInfo) obj;
			String dbName1 = info.dbName;
			if (host.equals(info.host) &&
			    dbName.equals(info.dbName) &&
			    user.equals(info.user) &&
			    pwd.equals(info.pwd) &&
			    port == info.port) 
			    return true;
		    else
		    	return false;
		}
		
		public int hashCode() {
			String str = host + dbName + user + pwd + port;
			return str.hashCode();
		}
	}                                                 	                                                 	
}