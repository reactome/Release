/*
 * Created on Dec 22, 2005
 */
package org.gk.IDGeneration;

import java.awt.Component;
import java.util.Properties;

import org.gk.model.GKInstance;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.schema.InvalidAttributeException;

/**
 * A persistence manager for databases, modifies the
 * functionality of PersistenceManager.
 * 
 * Special methods available for converting
 * from the regular properties to the internally used
 * properties in the persistence managers.
 * @author croft
 */
public class IDGenerationPersistenceManager extends PersistenceManager {
	private Properties dbProp = null;
	private String dbName = null;
	
	public IDGenerationPersistenceManager() {
		super();
	}
	
	public IDGenerationPersistenceManager(String dbName) {
		super();
		this.dbName = dbName;
		
		Properties systemProperties = SystemProperties.retrieveSystemProperties();
		Properties dbConnectInfo = getDBConnectInfo();
		if (dbConnectInfo==null) {
			dbConnectInfo = createSimplePropFromDbProp(dbName, systemProperties);
			if (dbConnectInfo!=null)
				setDBConnectInfo(dbConnectInfo);
		}
	}

	/**
	 * Overwrite supclass method to allow null/empty password or port.
	 * 
	 * @return the initilized MySQLAdaptor.
	 */
	protected MySQLAdaptor initMySQLAdaptor(Component comp) {
		Properties dbConnectInfo = getDBConnectInfo();
		
		if (dbConnectInfo==null)
			return null;
		
		String dbHost = dbConnectInfo.getProperty(dbName + ".dbHost");
		String xdbName = dbConnectInfo.getProperty(dbName + ".dbName");
		String dbPort = dbConnectInfo.getProperty(dbName + ".dbPort");
		String dbUser = dbConnectInfo.getProperty(dbName + ".dbUser");
		String dbPwd = dbConnectInfo.getProperty(dbName + ".dbPwd");
		
		// To display the info if one of them is empty
		if (dbHost == null || dbHost.length() == 0 ||
		    xdbName == null || xdbName.length() == 0 ||
		    dbUser == null || dbUser.length() == 0) {
			DBConnectionPane connectionPane = new DBConnectionPane();
			connectionPane.setValues(dbConnectInfo);
			if(comp!=null && connectionPane.showInDialog(comp)) {
				connectionPane.commit();
				dbHost = dbConnectInfo.getProperty(dbName + ".dbHost");
				xdbName = dbConnectInfo.getProperty(dbName + ".dbName");
				dbPort = dbConnectInfo.getProperty(dbName + ".dbPort");
				dbUser = dbConnectInfo.getProperty(dbName + ".dbUser");
				dbPwd = dbConnectInfo.getProperty(dbName + ".dbPwd");
			}
			else
				return null;
		}
		
		// Establish innocuous defaults
		if (dbPort==null || dbPort.equals(""))
			dbPort = "3306";
		if (dbPwd==null)
			dbPwd = "";
		
		return getMySQLAdaptor(dbHost, xdbName, dbUser, dbPwd, Integer.parseInt(dbPort));
	}
	
	public void unsetDBConnectInfo() {
		super.setDBConnectInfo(createSimplePropFromDbProp(dbName, null));
		
		this.dbProp = null;
	}

	/**
	 * Passes database connection info to the persistence manager.
	 * Uses the name of the database (e.g. previous release database)
	 * in order to extract the appropriate parameters.
	 * 
	 * @param dbProp
	 */
	public void setDBConnectInfo(Properties dbProp) {
		super.setDBConnectInfo(createSimplePropFromDbProp(dbName, dbProp));
		
		this.dbProp = dbProp;
	}
	
	/**
	 * Passes database connection info to the persistence manager.
	 * 
	 * @param dbProp
	 */
	public void setDBConnectInfo(String dbHost, String dbName, String dbPort, String dbUser, String dbPwd) {
		Properties simpleProp = new Properties();
		
		if (dbHost!=null)
			simpleProp.setProperty("dbHost", dbHost);
		if (dbName!=null)
			simpleProp.setProperty("dbName", dbName);
		if (dbPort!=null)
			simpleProp.setProperty("dbPort", dbPort);
		if (dbUser!=null)
			simpleProp.setProperty("dbUser", dbUser);
		if (dbPwd!=null)
			simpleProp.setProperty("dbPwd", dbPwd);

		if (dbHost==null || dbName==null || dbUser==null) {
			super.setDBConnectInfo(null);
			dbProp = null;
		} else {
			super.setDBConnectInfo(simpleProp);
			initMySQLAdaptor(null);
			
			dbProp = new Properties();
			copySimplePropToDbProp(simpleProp, this.dbName, dbProp);
		}
	}
	
	/**
	 * Gets the database connection information from the persistence
	 * manager and drops it into the properties object.  Uses database
	 * name so that the information is put into the correct properties.
	 * 
	 * @param dbName
	 * @return
	 */
	public Properties getDBConnectInfo() {
		if (dbProp==null)
			return null;
		
		Properties simpleProp = super.getDBConnectInfo();
		
		if (simpleProp==null ||
				simpleProp.get("dbHost")==null || simpleProp.get("dbHost").equals("") ||
				simpleProp.get("dbName")==null || simpleProp.get("dbName").equals("") ||
				simpleProp.get("dbUser")==null || simpleProp.get("dbUser").equals("")
				) {
			simpleProp = createSimplePropFromDbProp(dbName, SystemProperties.retrieveSystemProperties());
			super.setDBConnectInfo(simpleProp);
		}
		
		dbProp = new Properties();
		
		copySimplePropToDbProp(simpleProp, dbName, dbProp);
		
		return dbProp;
	}
	
	public static void copySimplePropToDbProp(Properties simpleProp, String dbName, Properties dbProp) {
		if (simpleProp==null || dbName==null || dbProp==null)
			return;
		
		String value = simpleProp.getProperty("dbHost", "");
		dbProp.setProperty(dbName + ".dbHost", value);
		value = simpleProp.getProperty("dbName", "");
		dbProp.setProperty(dbName + ".dbName", value);
		value = simpleProp.getProperty("dbPort", "");
		dbProp.setProperty(dbName + ".dbPort", value);
		value = simpleProp.getProperty("dbUser", "");
		dbProp.setProperty(dbName + ".dbUser", value);
		value = simpleProp.getProperty("dbPwd", "");
		dbProp.setProperty(dbName + ".dbPwd", value);		
	}
	
	public static Properties createSimplePropFromDbProp(String dbName, Properties dbProp) {
		if (dbProp==null)
			return null;
		
		Properties simpleProp = new Properties();
		String value = dbProp.getProperty(dbName + ".dbHost", "");
		if (value==null)
			return null;
		simpleProp.setProperty("dbHost", value);
		
		value = dbProp.getProperty(dbName + ".dbName", "");
		if (value==null)
			return null;
		simpleProp.setProperty("dbName", value);
		
		value = dbProp.getProperty(dbName + ".dbPort", "");
		simpleProp.setProperty("dbPort", value);
		value = dbProp.getProperty(dbName + ".dbUser", "");
		simpleProp.setProperty("dbUser", value);
		value = dbProp.getProperty(dbName + ".dbPwd", "");
		simpleProp.setProperty("dbPwd", value);
		
		return simpleProp;
	}
	
    /**
     * An overloaded method to get the active MySQLAdaptor.
     * @param comp 
     * @return the active MySQLAdaptor.
     * @see getActiveMySQLAdaptor()
     */
	public MySQLAdaptor getActiveMySQLAdaptor(Component comp) {
		if (dbProp==null)
			return null;
		
		MySQLAdaptor dba = initMySQLAdaptor(comp);
		
		return dba;
	}
}
