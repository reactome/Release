/*
 * Created on Dec 21, 2005
 */
package org.gk.IDGeneration;

import java.util.Properties;

import org.gk.persistence.DBConnectionPane;

/**
 * This customized JPane for db connection info.
 * It inherits from DBConnectionPane but allows a more complex
 * database description, since the ID generation stuff actually
 * needs to deal with at least 3 databases simultaneously.
 * 
 * @author croft
 */
public class IDGenerationDBConnectionPane extends DBConnectionPane {
	private Properties simpleProp = null;
	private String dbName;
	private Properties dbProp = null;
	
	public IDGenerationDBConnectionPane() {
		super();
	}
	
	public IDGenerationDBConnectionPane(boolean useTransactionBox) {
		super(useTransactionBox);
	}
	
	/**
	 * Initialize all values based on the properties.
	 * @param dbProp
	 */
	public void setValues(String dbName, Properties dbProp) {
		simpleProp = IDGenerationPersistenceManager.createSimplePropFromDbProp(dbName, dbProp);
				
		super.setValues(simpleProp);
		
		this.dbName = dbName;
		this.dbProp = dbProp;
	}
	
	public boolean commitForTab() {
		if (!super.commitForTab())
			return false;
		
		return localCommit();
	}
	
	public boolean commit() {
		if (!super.commit())
			return false;
		
		return localCommit();
	}
	
	public boolean localCommit() {
		IDGenerationPersistenceManager.copySimplePropToDbProp(simpleProp, dbName, dbProp);
		IDGenerationPersistenceManagers.getManager().getManager(dbName).setDBConnectInfo(simpleProp);
		
		return true;
	}
}
