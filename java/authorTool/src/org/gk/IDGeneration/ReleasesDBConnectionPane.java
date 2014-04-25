/*
 * Created on Apr. 7, 2006
 */
package org.gk.IDGeneration;

import java.util.Properties;

import org.gk.model.GKInstance;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.MySQLAdaptor;

/**
 * This customized JPane for db connection info.
 * It inherits from DBConnectionPane but allows a more complex
 * database description, 
 * 
 * @author croft
 */
public class ReleasesDBConnectionPane extends DBConnectionPane {
	public static String SLICE = "sliceDbParams";
	public static String RELEASE = "releaseDbParams";
	private String dbParamsAttributeName = SLICE; // default
	private GKInstance release = null;
	private Properties simpleProp = null;
	
	public ReleasesDBConnectionPane() {
		super();
	}
	
	public ReleasesDBConnectionPane(boolean useTransactionBox) {
		super(useTransactionBox);
	}
	
	public void setDbParamsAttributeName(String dbParamsInstanceClassName) {
		this.dbParamsAttributeName = dbParamsInstanceClassName;
	}

	/**
	 * Initialize all values based on the properties.
	 * @param dbProp
	 */
	public void setValues(GKInstance release) {
		this.release = release;
		copyFromReleaseToSimpleProp();
		super.setValues(simpleProp);
	}
	
	public boolean commitForTab() {
		if (!super.commitForTab())
			return false;
		
		copyFromSimplePropToRelease();
		
		return true;
	}
	
	public boolean commit() {
		if (!super.commit())
			return false;
		
		copyFromSimplePropToRelease();
		
		return true;
	}
	
	private void copyFromReleaseToSimpleProp() {
		simpleProp = new Properties();
		
		if (release!=null) {
			try {
				GKInstance dbParams = (GKInstance)release.getAttributeValue(dbParamsAttributeName);
				if (dbParams!=null) {
					String value;
					value = (String)dbParams.getAttributeValue("host");
					if (value != null)
						simpleProp.setProperty("dbHost", value);
					value = (String)dbParams.getAttributeValue("user");
					if (value != null)
						simpleProp.setProperty("dbUser", value);
					value = (String)dbParams.getAttributeValue("dbName");
					if (value != null)
						simpleProp.setProperty("dbName", value);
					value = (String)dbParams.getAttributeValue("pwd");
					if (value != null)
						simpleProp.setProperty("dbPwd", value);
					value = (String)dbParams.getAttributeValue("port");
					if (value != null)
						simpleProp.setProperty("dbPort", value);
				}
			} catch (Exception e) {
				System.err.println("ReleasesDBConnectionPane.extractSimplePropFromRelease: WARNING - problem while getting DB parameters from release instance");
				e.printStackTrace();
			}
		}
	}
	private void copyFromSimplePropToRelease() {
		if (release!=null && simpleProp!=null) {
			try {
				GKInstance dbParams = (GKInstance)release.getAttributeValue(dbParamsAttributeName);
				MySQLAdaptor instanceDatabaseDba = IdentifierDatabase.getDba();
				if (dbParams==null) {
					// Insert a new DbParams instance, if necessary
					dbParams = new GKInstance();
					dbParams.setSchemaClass(instanceDatabaseDba.getSchema().getClassByName("DbParams"));
					dbParams.setDbAdaptor(instanceDatabaseDba);
					release.setAttributeValue(dbParamsAttributeName, dbParams);
				}
				dbParams.setAttributeValue("host", simpleProp.getProperty("dbHost"));
				dbParams.setAttributeValue("user", simpleProp.getProperty("dbUser"));
				dbParams.setAttributeValue("dbName", simpleProp.getProperty("dbName"));
				dbParams.setAttributeValue("pwd", simpleProp.getProperty("dbPwd"));
				dbParams.setAttributeValue("port", simpleProp.getProperty("dbPort"));
			} catch (Exception e) {
				System.err.println("ReleasesDBConnectionPane.copyFromSimplePropToRelease: WARNING - problem while setting DB parameters in release instance");
				e.printStackTrace();
			}
		}
	}
}
