/*
 * Created on Dec 21, 2005
 */
package org.gk.IDGeneration;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * Maintains persistence managers for multiple, named,
 * databases.  This is a singleton, if you want to use it,
 * you need to do something like: 
 * 
 * IDGenerationPersistenceManagers.getManagers()
 * 
 * To get an individual database manager (e.g. for the
 * identifier database), do something like:
 * 
 * IDGenerationPersistenceManagers.getManagers().getManager(IDGenerationPersistenceManagers.IDENTIFIER_MANAGER)
 * 
 * @author croft
 */
public class IDGenerationPersistenceManagers {
	// The sole instance
	private static IDGenerationPersistenceManagers manager = new IDGenerationPersistenceManagers();
	// The managers
	private static Map managers = new HashMap();
	// Managers names understood by this class
	public static String IDENTIFIER_MANAGER = "IdentifierDatabase";
	public static String GK_CENTRAL_MANAGER = "gk_central";
	public static String PREVIOUS_MANAGER = "PreviousReleaseDatabase";
	public static String CURRENT_MANAGER = "CurrentReleaseDatabase";
	
	private static Properties systemProperties = null;
	
	private IDGenerationPersistenceManagers() {
	}
	
	public static IDGenerationPersistenceManagers getManager() {
		return manager;
	}
	
	/**
	 * Gets a persistence manager for a named database.  If the
	 * manager doesn't already exist, it will be created.
	 * 
	 * @param dbName
	 * @return
	 */
	public static IDGenerationPersistenceManager getManager(String dbName) {
		if (managers.get(dbName)==null) {
			IDGenerationPersistenceManager iDGenerationPersistenceManager = new IDGenerationPersistenceManager(dbName);
			iDGenerationPersistenceManager.setDBConnectInfo(new Properties());
			managers.put(dbName, iDGenerationPersistenceManager);
		}
		
		return ((IDGenerationPersistenceManager)managers.get(dbName));
	}
	
	/**
	 * Gets a dba for the named database.  This presupposes
	 * that the user has already loaded appropriate parameters.
	 * If that *isn't* the case, then null will be returned.
	 * 
	 * @return
	 */
	public static MySQLAdaptor getDatabaseAdaptor(String dbName) {
		IDGenerationPersistenceManager pm = getManager(dbName);
		
		if (pm==null) {
			System.err.println("getDatabaseAdaptor: WARNING - persistence manager is null for dbName=" + dbName);
		}

		MySQLAdaptor mySQLAdaptor = null;
//		if (pm.getDBConnectInfo()==null)
//			mySQLAdaptor = null;
//		else
			try {
				mySQLAdaptor = pm.getActiveMySQLAdaptor(null);
			} catch (Exception e) {
				System.err.println("getDatabaseAdaptor: something went wrong when trying to get dba for " + dbName);
				e.printStackTrace();
			}
		
		return mySQLAdaptor;
	}
}
