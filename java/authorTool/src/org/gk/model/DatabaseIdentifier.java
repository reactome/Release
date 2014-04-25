/*
 * Created on Feb 3, 2004
 */
package org.gk.model;

import java.io.Serializable;

/**
 * To store the database identifier information for entities.
 * @author wugm
 */
public class DatabaseIdentifier implements Serializable {
	private String dbName; 
	private String accessNo;
	private Long db_id; // DB_ID from the gk_central.

	public DatabaseIdentifier() {
	}
	
	public DatabaseIdentifier(String dbName, String accessNo, Long db_ID) {
		this.dbName = dbName;
		this.accessNo = accessNo;
		this.db_id = db_ID;
	}

	public String getAccessNo() {
		return accessNo;
	}

	public Long getDB_ID() {
		return db_id;
	}

	public String getDbName() {
		return dbName;
	}

	public void setAccessNo(String string) {
		accessNo = string;
	}

	public void setDB_ID(Long long1) {
		db_id = long1;
	}

	public void setDbName(String string) {
		dbName = string;
	}
	
	/**
	 * Check if there is any information in this object.
	 * @return true if accessNo, db_id and dbName are null, otherwise
	 * false.
	 */
	public boolean isEmpty() {
		if (accessNo == null && db_id == null && dbName == null)
			return true;
		return false;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DatabaseIdentifier))
			return false;
		DatabaseIdentifier another = (DatabaseIdentifier) obj;
		// Check DB_ID
		if (db_id == null) {
			if (another.getDB_ID() != null)
				return false;
		}
		else {
			if (!db_id.equals(another.getDB_ID()))
				return false;
		}
		// Check db_name
		if (dbName == null) {
			if (another.getDbName() != null)
				return false;
		}
		else {
			if (!dbName.equals(another.getDbName()))
				return false;
		}
		// Check accession 
		if (accessNo == null) {
			if (another.getAccessNo() != null)
				return false;
		}
		else {
			if (!accessNo.equals(another.getAccessNo()))
				return false;
		}
		return true;
	}

}
