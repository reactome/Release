/*
 * Created on Jan 20, 2004
 */
package org.gk.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.gk.persistence.DBConnectionPane;

/**
 * A helper class.
 * @author wugm
 */
public class IsamToInnoDB {

	public static void main(String[] args) {
		DBConnectionPane connectionPane = new DBConnectionPane();
		Properties prop = new Properties();
		prop.setProperty("dbHost", "localhost");
		prop.setProperty("dbPort", "3306");
		prop.setProperty("dbName", "gk_v6_test");
		prop.setProperty("dbUser", "wgm");
		prop.setProperty("dbPwd", "wgm");
		connectionPane.setValues(prop);
		//if (!connectionPane.showInDialog(null)) {
		//	System.exit(0);
		//}
		String dbDriver = "com.mysql.jdbc.Driver";
		Connection conn = null;
		try {
			Class.forName(dbDriver).newInstance();
			String host = prop.getProperty("dbHost");
			String port = prop.getProperty("dbPort");
			String dbName = prop.getProperty("dbName");
			String user = prop.getProperty("dbUser");
			String pwd = prop.getProperty("dbPwd");
			String connectionStr = "jdbc:mysql://" + host + ":" + Integer.parseInt(port) + "/" + dbName;
			conn = DriverManager.getConnection(connectionStr, user, pwd);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		java.util.List tables = getTables(conn);
		dropFullTextIndex(tables, conn);
		changeToInnoDB(tables, conn);
	}
	
	private static java.util.List getTables(Connection conn) {
		java.util.List tables = new ArrayList();
		try {
			Statement statement = conn.createStatement();
			ResultSet resultset = statement.executeQuery("Show Tables");
			while (resultset.next()) {
				String name = resultset.getString(1);
				tables.add(name);
			}
			resultset.close();
			statement.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return tables;
	}
	
	private static void dropFullTextIndex(java.util.List tables, Connection conn) {
		try {
			Statement statement = conn.createStatement();
			for (Iterator it = tables.iterator(); it.hasNext();) {
				String table = it.next().toString();
				ResultSet result = statement.executeQuery("SHOW INDEX FROM " + table);
				while(result.next()) {
					String indexType = result.getString(11);
					if (indexType != null && indexType.equals("FULLTEXT")) {
						String keyName = result.getString(3);
						System.out.println("Drop full text index: " + table + "." + keyName);
						conn.createStatement().execute("ALTER TABLE " + table + " DROP INDEX " + keyName);
					}
				}
				result.close();
			}
			statement.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void changeToInnoDB(java.util.List tables, Connection conn) {
		try {
			Statement statement = conn.createStatement();
			for (Iterator it = tables.iterator(); it.hasNext();) {
				String table = it.next().toString();
				System.out.println("Alter Table to InnoDB: " + table);
				try {			
					statement.execute("ALTER TABLE " + table + " ENGINE=InnoDB");
				}
				catch(SQLException e) {
					e.printStackTrace();
				}
			}
			statement.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
	}

}
