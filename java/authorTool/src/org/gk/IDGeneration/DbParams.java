/*
 * Created on June 6, 2011
 */
package org.gk.IDGeneration;

import java.sql.SQLException;

import org.gk.persistence.MySQLAdaptor;

/** 
 * Lightweight class for holding parameters for various
 * databases.
 *  
 * @author croft
 */
public class DbParams  {
	public String dbName = "";
	public String hostname = "";
	public String username = "";
	public String port = "";
	public String password = "";
	
	public MySQLAdaptor getDba() {
		MySQLAdaptor dba = null;
		
		if (dbName == null) {
			System.err.println("IDGenerationCommandLine.DbParams.getDba: dbName == null. aborting!");
			return dba;			
		}
		if (dbName.isEmpty()) {
			System.err.println("IDGenerationCommandLine.DbParams.getDba: dbName is empty aborting!");
			return dba;			
		}
		
		try {
			dba = new MySQLAdaptor(hostname, dbName, username, password, Integer.parseInt(port));
		} catch (NumberFormatException e) {
			System.err.println("IDGenerationCommandLine.DbParams.getDba: port number is strange: " + port);
			e.printStackTrace();
		} catch (SQLException e) {
			System.err.println("IDGenerationCommandLine.DbParams.getDba: something went wrong with mysql");
			if (dbName != null)
				System.err.println("IDGenerationCommandLine.DbParams.getDba: dbName=" + dbName);
			else
				System.err.println("IDGenerationCommandLine.DbParams.getDba: dbName is null!");
			if (hostname != null)
				System.err.println("IDGenerationCommandLine.DbParams.getDba: hostname=" + hostname);
			else
				System.err.println("IDGenerationCommandLine.DbParams.getDba: hostname is null!");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("IDGenerationCommandLine.DbParams.getDba: something went wrong");
			e.printStackTrace();
		}
		
		return dba;
	}
}