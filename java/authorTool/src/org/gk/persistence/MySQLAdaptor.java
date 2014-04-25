/*
 * MySQLAdaptor.java
 *
 * Created on August 18, 2003, 3:31 PM
 */

package org.gk.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.gk.model.DBIDNotSetException;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceCache;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.schema.SchemaParser;
import org.gk.util.StringUtils;

/**
 *
 * @author  vastrik
 */
@SuppressWarnings("rawtypes")
public class MySQLAdaptor implements PersistenceAdaptor {
    private String host;
	private String database;
	private String username;
	private String password;
    // Make it protected to be used by subclass
	protected Connection conn;
	private int port = 3306;
	public static String SCHEMA_TABLE_NAME = "DataModel";
	public static final String DB_ID_NAME = "DB_ID";
	private Schema schema;
	private InstanceCache instanceCache = new InstanceCache();
	private Map classMap;
	private boolean supportsTransactions = false;
	private boolean supportsTransactionsIsSet = false;
	private boolean inTransaction = false;
	public boolean debug = false;
	public static final int MAX_JOINS_PER_QUERY = 4;
	private boolean useCache = true;

    /**
     * This default constructor is used for subclassing.
     */
    protected MySQLAdaptor() {
    }
    
	/** Creates a new instance of MySQLAdaptor */
	public MySQLAdaptor(
		String host,
		String database,
		String username,
		String password)
		throws SQLException {
		this(host,database,username,password,3306);
	}

	public MySQLAdaptor(String host,String database,String username,String password,int port)
		throws SQLException {
		this.host = host;
		this.database = database;
		this.username = username;
		this.password = password;
		this.port = port;
		installDriver();
		connect();
		try {
			fetchSchema();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public MySQLAdaptor(String host,String database,String username,String password,int port,Map classMap)
		throws SQLException {
		this(host,database,username,password,port);
		this.setClassMap(classMap);
	}
	
	public String getDBHost() {
		return this.host;
	}
	
	public String getDBName() {
		return this.database;
	}
	
	public String getDBUser() {
		return this.username;
	}
	
	public String getDBPwd() {
	    return this.password;
	}
	
	public int getDBPort() {
	    return this.port;
	}
	
	public String toString() {
		return database + "@" + host;
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

	private void connect() throws SQLException {
		String connectionStr = "jdbc:mysql://" + host + ":" + port + "/" + database;
			 //+ "?autoReconnect=true";
		Properties prop = new Properties();
		prop.setProperty("user", username);
		prop.setProperty("password", password);
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
//		// test code
//		prop.put("profileSQL", "true");
//		prop.put("slowQueryThresholdMillis", "0");
//		prop.put("logSlowQueries", "true");
//		prop.put("explainSlowQueries", "true");
		
		conn = DriverManager.getConnection(connectionStr, prop);
		//conn = DriverManager.getConnection(connectionStr, username, password);
	}

	/**
	 * There is a time-out problem. When the tool idles for a while, the established
	 * connection cannot be used anymore. About ten minutes are needed to re-create
	 * a connection. A possible fix is to check the idle time, if the idle time is
	 * longer than a certain number, re-create a connection.
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return conn;
	}
	
    /**
     * This thread is used to handle time-out exception using MySQLAdaptor in a
     * sever environment. A lower priority thread will call the database every
     * 4 hours to keep the connection active. For the future, a connection pool
     * should be used.
     */
	public void initDumbThreadForConnection(final int millsecond) {
	    final long time1 = System.currentTimeMillis();
	    Thread t = new Thread() {
	        public void run() {
	            // Force this thread into a infinity circle
	            while (true) {
	                try {
	                    Thread.sleep(millsecond);
	                }
	                catch(InterruptedException e) {
	                    System.err.println(e.getMessage());
	                }
	                try {
	                    Connection connection = getConnection();
	                    String query = "SHOW TABLES";     
	                    Statement stat = connection.createStatement();
	                    ResultSet result = stat.executeQuery(query);
	                    result.close();
	                    stat.close();
	                }
	                catch(SQLException e) {
	                    System.err.println(e.getMessage());
	                }
	            }
	        }
	    };
	    t.setPriority(Thread.MIN_PRIORITY);
	    t.start();
	}
	
	/**
	 * Use 4 hours as the default time to do a query in order to keep the connection live.
	 */
	public void initDumbThreadForConnection() {
	    initDumbThreadForConnection(4 * 60 * 60 * 1000);
	}

	public Schema fetchSchema() throws Exception {
		this.setSchemaTableName();
		String query =
			"SELECT thing,thing_class,property_name,property_value,property_value_type,property_value_rank FROM "
				+ SCHEMA_TABLE_NAME;
		ResultSet rs = getConnection().createStatement().executeQuery(query);
		SchemaParser sf = new SchemaParser();
		schema = sf.parseResultSet(rs);
		return schema;
	}

	// Older databases use a table named 'Schema' for holding the
	// Reactome data model.  The following code is designed to
	// detect that case and correct for it.  It also switches
	// the data model table name back to the more modern 'DataModel'
	// if appropriate.
	// TODO: old databases should have 'Schema' tables changed to
	// 'DataModel' to make them MySQL 5 compliant.
	protected void setSchemaTableName() throws Exception {
		if (getConnection().getMetaData().getTables(null,null,"DataModel",null).next()) {
			SCHEMA_TABLE_NAME = "DataModel";
		} else if (getConnection().getMetaData().getTables(null,null,"Schema",null).next()) {
			SCHEMA_TABLE_NAME = "Schema";
		} else {
			System.err.println("MySQLAdaptor.setSchemaTableName: WARNING - could not find schema table in:" + database);
		}
	}
	
	public Schema getSchema() {
		return schema;
	}
	
	/**
	 * Reload everything from the database. The saved Schema will be not re-loaded.
	 * The InstanceCache will be cleared.
	 */
	public void refresh() throws Exception {
		instanceCache.clear();
	}
	
	
	public void cleanUp() throws Exception {
		schema = null;
		instanceCache.clear();
		if (conn != null && !conn.isClosed()) {
			conn.close();
			conn = null;
		}
	}

	/**
	 * 
	 * @param instance
	 * @throws Exception
	 * 
	 * (Re)loads all attribute values from the database.
	 */
	public void loadInstanceAttributeValues(GKInstance instance) throws Exception {
		//for (Iterator ai = instance.getSchemaAttributes().iterator(); ai.hasNext();) {
		//	GKSchemaAttribute att = (GKSchemaAttribute)	ai.next();
		//	loadInstanceAttributeValues(instance, att);
		//}
		Collection singleValueAtts = new HashSet();
		java.util.List list = new ArrayList(1);
		list.add(instance);
		for (Iterator ai = instance.getSchemaAttributes().iterator(); ai.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
			if (att.isOriginMuliple()) {
				loadInstanceAttributeValues(list, att);
			} else {
				singleValueAtts.add(att);
			}
		}
		loadSingleValueAttributeValues(list, singleValueAtts);
		instance.setIsInflated(true);
	}
    
    public void fastLoadInstanceAttributeValues(GKInstance instance) throws Exception {
        Collection singleValueAtts = new HashSet();
        List mulInstanceAtts = new ArrayList();
        List mulAtts = new ArrayList();
        java.util.List list = new ArrayList(1);
        list.add(instance);
        for (Iterator ai = instance.getSchemaAttributes().iterator(); ai.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
            if (att.isOriginMuliple()) {
                if (att.isInstanceTypeAttribute())
                    mulInstanceAtts.add(att);
                else
                    mulAtts.add(att);
                //loadInstanceAttributeValues(list, att);
            } else {
                singleValueAtts.add(att);
            }
        }
        loadSingleValueAttributeValues(list, singleValueAtts);
        fastLoadMultipleAttributeValues(instance, mulInstanceAtts, true);
        fastLoadMultipleAttributeValues(instance, mulAtts, false);
        instance.setIsInflated(true);
    }
    
    private void fastLoadMultipleAttributeValues(GKInstance instance,
                                                 List multipleAtts,
                                                 boolean isInstanceType) throws Exception {
        if (multipleAtts == null || multipleAtts.size() == 0)
            return;
        // Use the new feature in mysql: union to load all mutliple values since
        // they have the same table structure. 
        StringBuffer buffer = new StringBuffer();
        // construct query
        int index = 0;
        for (Iterator it = multipleAtts.iterator(); it.hasNext();) {
            SchemaAttribute att = (SchemaAttribute) it.next();
            String name = att.getName();
            SchemaClass original = att.getOrigin();
            if (isInstanceType) {
                buffer.append("SELECT " + index + "," + index + "," + index + ",\'" + name + "\' UNION ALL ");
                buffer.append("SELECT DB_ID, " + name + "," + name + "_rank," + name + "_class FROM ");
            }
            else {
                buffer.append("SELECT " + index + "," + index + "," + index + " UNION ALL ");
                buffer.append("SELECT DB_ID, " + name + "," + name + "_rank FROM ");
            }
            buffer.append(original.getName() + "_2_" + name + " WHERE DB_ID=" + instance.getDBID());
            index ++;
            if (it.hasNext())
                buffer.append(" UNION ALL ");
        }
        Statement stat = getConnection().createStatement();
        //System.out.println("Query: " + buffer.toString());
        ResultSet resultSet = stat.executeQuery(buffer.toString());
        if (isInstanceType) {
            // Need to handle result
            MultiInstanceAttributeHandler handler = null;
            while(resultSet.next()) {
                // If the first three columns are the same, that row should be the flag.
                long first = resultSet.getLong(1);
                long second = resultSet.getLong(2);
                int third = resultSet.getInt(3);
                String fourth = resultSet.getString(4);
                if (first == second && first == third) {
                    SchemaAttribute att = (SchemaAttribute) multipleAtts.get(third);
                    handler = new MultiInstanceAttributeHandler(att,
                                                                2,
                                                                4,
                                                                3);    
                }
                else {
                    handler.handleAttributeValue(instance, resultSet);
                }
            }
        }
        else {
            MultiValueAttributeHandler handler = null;
            while(resultSet.next()) {
                // If the first three columns are the same, that row should be the flag.
                long first = resultSet.getLong(1);
                Object second = resultSet.getObject(2);
                int secondNumber = -1;
                try {
                    secondNumber = Integer.parseInt(second.toString());
                }
                catch(NumberFormatException e) {}
                int third = resultSet.getInt(3);
                if (first == secondNumber && first == third) {
                    SchemaAttribute att = (SchemaAttribute) multipleAtts.get(third);
                    handler = new MultiValueAttributeHandler(att,
                                                             2,
                                                             3);    
                }
                else {
                    handler.handleAttributeValue(instance, resultSet);
                }
            }
        }
        // don't forget to close them
        resultSet.close();
        stat.close();
    }

	/**
	 * Should throws DBIDNotSetException if the instance the attributes of which are being loaded does
	 * not have a DB_ID. However, In this point I've just put in the print statement in case this is too
	 * radical change
	 */
	public void loadInstanceAttributeValues(GKInstance instance, SchemaAttribute attribute) throws Exception {
		//System.err.println("public void loadInstanceAttributeValues(GKInstance instance, SchemaAttribute attribute)");
		Long dbId = instance.getDBID();
		if ((dbId == null) || (dbId.longValue() < 1)) {
			//throw(new DBIDNotSetException(instance));
// TODO: this can cause stack overflow errors!
//			System.err.println(new DBIDNotSetException(instance));
		}
		ResultSetWithHandlers rswh = createAttributeLoadingResultSetWithHandlers(instance, attribute);
		int row = handleResultSet(instance, rswh);
		//if (row == 0) { // Mark this attribute touched.
		//	instance.setAttributeValueNoCheck(attribute, null);
		//}
	}

	/**
	 * NOTE: This method (or rather loadSingleValueInstanceAttributeValues which is relied
	 * upon) expects instances to be found in InstanceCache.
	 * @param instances
	 * @param attributes
	 * @throws Exception
	 */
	public void loadInstanceAttributeValues(Collection instances, Collection attributes) throws Exception {
		Collection singleValueAtts = new HashSet();
		for (Iterator ai = attributes.iterator(); ai.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
			if (att.isOriginMuliple()) {
				loadInstanceAttributeValues(instances, att);
			} else {
				singleValueAtts.add(att);
			}
		}
		loadSingleValueAttributeValues(instances,singleValueAtts);
	}

	public void loadInstanceAttributeValues(Collection instances, String[] attNames) throws Exception {
		GKSchema s = (GKSchema) getSchema();
		Set attributes = new HashSet();
		for (int i = 0; i < attNames.length; i++) {
			attributes.addAll(s.getOriginalAttributesByName(attNames[i]));
		}
		loadInstanceAttributeValues(instances,attributes);
	}
	
	public void loadInstanceAttributeValues(Collection instances) throws Exception {
		Set classes = new HashSet();
		//Find out what classes we have
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance i = (GKInstance) ii.next();
			classes.add(i.getSchemClass());
		}
		Set tmp = new HashSet();
		Set tmp2 = new HashSet();
		tmp.addAll(classes);
		//Find the subclasses of the classes
		while (! tmp.isEmpty()) {
			for (Iterator ti = tmp.iterator(); ti.hasNext();) {
				GKSchemaClass c = (GKSchemaClass) ti.next();
				tmp2.addAll(c.getSubClasses());
			}
			tmp.clear();
			tmp.addAll(tmp2);
			classes.addAll(tmp2);
			tmp2.clear();
		}
		Set attributes = new HashSet();
		//Find all original attributes that those classes have
		for (Iterator ci = classes.iterator(); ci.hasNext();) {
			GKSchemaClass cls = (GKSchemaClass) ci.next();
			attributes.addAll(cls.getAttributes());
		}
		//Load the values of those attributes
		loadInstanceAttributeValues(instances,attributes);
	}

	/*
	 * NOTE: this method may try to load attribute values to instances which do not
	 * have this attribute. This is fine, since if there aren't any values, they
	 * can't be loaded. However, this also means that instances collection can be
	 * heterogenous and the user does not need to worry about that all the instances
	 * have the given attribute.
	 */
	public void loadInstanceAttributeValues(Collection instances, SchemaAttribute attribute) throws Exception {
		//System.err.println("public void loadInstanceAttributeValues(GKInstance instance, SchemaAttribute attribute)");
		if (instances.isEmpty()) {
			return;
		}
		ResultSetWithHandlers rswh = createAttributeLoadingResultSetWithHandlers(instances, attribute);
//		ResultSet rs = rswh.getResultSet();
//		GKInstance instance = null;
//		Long dbId = new Long(0);
//		while (rs.next()) {
//			if (rs.getLong(1) != dbId.longValue()) {
//				dbId = new Long(rs.getLong(1));
//				instance = instanceCache.get(dbId);
//				if (instance == null) {
//					throw(new Exception("Instance with DB_ID " + dbId + " not found in InstanceCache."));
//				}
//			}
//			for (Iterator i = rswh.getAttributeHandlers().iterator(); i.hasNext();) {
//				AttributeHandler handler = (AttributeHandler) i.next();
//				handler.handleAttributeValue(instance, rs);
//			}
//		}
		handleResultSet(instances,rswh);
		// Mark the attribute as loaded to avoid querying the database unnecessarily.
		String attName = attribute.getName(); // Different classes use different attributes instances. Only
		                                      // names are the same.
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance i = (GKInstance) ii.next();
			if (i.getSchemClass().isValidAttribute(attName)) {
				GKSchemaAttribute att = (GKSchemaAttribute) i.getSchemClass().getAttribute(attName);
				if (! i.isAttributeValueLoaded(att)) {
					i.setAttributeValueNoCheck(att, null);
				}
			}
		}
	}

	public void loadInstanceReverseAttributeValues(Collection instances, Collection attributes) throws Exception {
		for (Iterator ai = attributes.iterator(); ai.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) ai.next();
			loadInstanceReverseAttributeValues(instances, att);
		}
	}

	public void loadInstanceReverseAttributeValues(Collection instances, String[] attNames) throws Exception {
		GKSchema s = (GKSchema) getSchema();
		Set attributes = new HashSet();
		for (int i = 0; i < attNames.length; i++) {
			attributes.addAll(s.getOriginalAttributesByName(attNames[i]));
		}
		loadInstanceReverseAttributeValues(instances,attributes);
	}

	/*
	 * NOTE: this method may try to load reverse attribute values to instances which do not
	 * have this attribute. This is fine, since if there aren't any values, they
	 * can't be loaded. However, this also means that instances collection can be
	 * heterogenous and the user does not need to worry about that all the instances
	 * have the given reverse attribute.
	 */
	public void loadInstanceReverseAttributeValues(Collection instances, SchemaAttribute attribute) throws Exception {
		if (instances.isEmpty()) {
			return;
		}
		if (! ((GKSchemaAttribute) attribute).isInstanceTypeAttribute()) {
			throw new Exception("Attribute " + attribute.getName() + " is not instance type attribute.");
		}
		String attName = attribute.getName();
		String tableName = (attribute.isMultiple())
			? attribute.getOrigin().getName() + "_2_" + attName
			: attribute.getOrigin().getName();
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		String rootClassAlias = "A_1";
		String[] tmp = new String[instances.size()];
		Arrays.fill(tmp, "?");
		StringBuffer sql = new StringBuffer(
			"SELECT " + rootClassAlias + "." + DB_ID_NAME + "," +
			rootClassAlias + "._class," +
			rootClassAlias + "._displayName," +
			tableName + "." + attName + "," +
			tableName + "." + attName + "_class" +
			"\nFROM " + rootClassName + " AS " + rootClassAlias + "," + tableName +
			"\nWHERE " + rootClassAlias + "." + DB_ID_NAME + "=" + tableName + "." + DB_ID_NAME +
			"\nAND " + tableName + "." + attName + " IN(" + 
			StringUtils.join(",",Arrays.asList(tmp)) + ")"
			//+ "\nORDER BY " + tableName + "." + DB_ID_NAME
			);
		SchemaAttribute _displayName = ((GKSchema) schema).getRootClass().getAttribute("_displayName");
		AttributeHandler ah = new SingleValueAttributeHandler(_displayName, 3);
		if (debug) System.out.println("\n" + sql.toString());
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		int j = 0;
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance ins = (GKInstance) ii.next();
			ps.setObject(++j, ins.getDBID());
			if (debug) System.out.println("\t" + ins.getDBID());
		}
		ResultSet rs = ps.executeQuery();		
		while (rs.next()) {
			Long refererDbId = new Long(rs.getLong(1));
			String refererClass = rs.getString(2);
			Long refereeDbId = new Long(rs.getLong(4));
			String refereeClass = rs.getString(5);
			GKInstance referer = (GKInstance) getInstance(refererClass, refererDbId);
			GKInstance referee = (GKInstance) getInstance(refereeClass, refereeDbId);
			referee.addRefererNoCheck(attribute, referer);
		}
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance i = (GKInstance) ii.next();
			if (((GKSchemaClass) i.getSchemClass()).isValidReverseAttribute(attribute)) {
				if (! i.isRefererValueLoaded(attribute)) {
//					i.setRefererNoCheck(attribute, null);
				}
			}
		}
	}

	private void loadSingleValueAttributeValues(Collection instances, Collection attributes) throws Exception {
		if (attributes.isEmpty() || instances.isEmpty()) {
			return;
		}
		HashSet tables = new HashSet();
		List select = new ArrayList();
		List attributeHandlers = new ArrayList();
		int colCount = 1;
		for (Iterator ai = attributes.iterator(); ai.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
			if (att.isOriginMuliple()) {
					throw(new Exception("Attribute " + att + " is a multi-value attribute. " +
					"This method can only load single-value attributes."));
			}	
			String attName = att.getName();
			SchemaClass origin = att.getOrigin();
			String tableName = origin.getName();
			tables.add(tableName);
			if (att.isInstanceTypeAttribute()) {
				select.add(tableName + "." + attName);
				select.add(tableName + "." + attName + "_class");
				attributeHandlers.add(new SingleInstanceAttributeHandler(att, ++colCount, ++colCount));
			} else {
				select.add(tableName + "." + attName);
				attributeHandlers.add(new SingleValueAttributeHandler(att, ++colCount));
			}
		}
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		String rcid = rootClassName + "." + DB_ID_NAME;
		tables.remove(rootClassName);
		StringBuffer queryStr = new StringBuffer("SELECT " + rcid + ",");
		queryStr.append(StringUtils.join(",",select));
		queryStr.append(" FROM " + rootClassName);
		for (Iterator ti = tables.iterator(); ti.hasNext();) {
			String table = (String) ti.next();
			queryStr.append(" LEFT JOIN " + table + " ON ("
							+ rcid + " = " + table + "." + DB_ID_NAME + ")");
		}
		queryStr.append(" WHERE " + rcid + " IN(");
		String[] tmp = new String[instances.size()];
		Arrays.fill(tmp, "?");
		queryStr.append(StringUtils.join(",",Arrays.asList(tmp)) + ")");
		queryStr.append(" ORDER BY " + rcid);
		if (debug) System.out.println(queryStr.toString());
		PreparedStatement ps = getConnection().prepareStatement(queryStr.toString());
		int i = 0;
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance ins = (GKInstance) ii.next();
			ps.setObject(++i, ins.getDBID());
//			if (debug) System.out.println("\t" + ins.getDBID());
		}
		ResultSet rs = ps.executeQuery();
		handleResultSet(instances, new ResultSetWithHandlers(rs, attributeHandlers));
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance ins = (GKInstance) ii.next();
			for (Iterator ai = attributes.iterator(); ai.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) ai.next();
				if (ins.getSchemClass().isValidAttribute(att)) {
					if (! ins.isAttributeValueLoaded(att)) {
						ins.setAttributeValueNoCheck(att, null);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return the total rows that are handled by the handlers.
	 */
	private int handleResultSet(GKInstance instance, ResultSetWithHandlers rswh) throws Exception {
		ResultSet rs = rswh.getResultSet();
		int c = 0;
		while (rs.next()) {
			for (Iterator i = rswh.getAttributeHandlers().iterator(); i.hasNext();) {
				AttributeHandler handler = (AttributeHandler) i.next();
				handler.handleAttributeValue(instance, rs);
			}
			c ++;
		}
		return c;
	}

	/**
	 * @param instances
	 * @param rswh
	 * @return the number of rows processed
	 * @throws Exception
	 * NOTE: This method expects the instances specified in the 1st collection to be
	 * found in the InstanceCache.
	 */
	private int handleResultSet(Collection instances, ResultSetWithHandlers rswh) throws Exception {
		ResultSet rs = rswh.getResultSet();
		List handlers = rswh.getAttributeHandlers();
		int c = 0;
		GKInstance instance = null;
		Long dbId = new Long(0);
		while (rs.next()) {
			if (rs.getLong(1) != dbId.longValue()) {
				dbId = new Long(rs.getLong(1));
				instance = instanceCache.get(dbId);
				if (instance == null) {
					throw (
						new Exception("Instance with DB_ID " + dbId
									+ " not found in InstanceCache."));
				}
			}
			for (Iterator i = handlers.iterator(); i.hasNext();) {
				AttributeHandler handler = (AttributeHandler) i.next();
				handler.handleAttributeValue(instance, rs);
			}
			c ++;
		}
		return c;
	}
	
	/**
	 * If a "true" argument is supplied, instance caching will be switched on.
	 * A "false" argument will switch instance caching off.  If you switch it off,
	 * you will force the dba to pull instances out of the database every time
	 * they are requested, which can be very time consuming, but you will reduce
	 * the memory footprint.
	 * 
	 * @param useCache
	 */
	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	/**
	 * Returns true if instance caching is active, false if not.
	 * 
	 * @return
	 */
	public boolean isUseCache() {
		return useCache;
	}

	/**
	 * Tries to get an instance of the given class, with the given DB_ID, from
	 * instance cache, if possible.  Otherwise, creates a new instance with
	 * the given DB_ID.  This new instance will be cached even if caching is
	 * switched off.
	 * 
	 * @param className
	 * @param dbID
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public Instance getInstance(String className, Long dbID) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		GKInstance instance;
		if (!useCache || (instance = instanceCache.get(dbID)) == null) {
			if (classMap != null && classMap.containsKey(className)) {
				String targetClassName = (String) classMap.get(className);
				instance = (GKInstance) Class.forName(targetClassName).newInstance();				
			} else {
				instance = new GKInstance();
			}
			instance.setSchemaClass(getSchema().getClassByName(className));
			instance.setDBID(dbID);
			instance.setDbAdaptor(this);
			instanceCache.put(instance);
		}
		return instance;
	}

	private ResultSetWithHandlers createAttributeLoadingResultSetWithHandlers(
		Instance instance,
		SchemaAttribute attribute)
		throws SQLException, InvalidAttributeException {
		SchemaAttribute originalAttribute =
			attribute.getOrigin().getAttribute(attribute.getName());
		String attributeName = attribute.getName();
		StringBuffer queryString = new StringBuffer("SELECT " + attributeName);
		StringBuffer orderBy = new StringBuffer();
		AttributeHandler attributeHandler;
		if (originalAttribute.isMultiple()) {
			if (originalAttribute.isInstanceTypeAttribute()) {
				queryString.append(
					"," + attributeName + "_class," + attributeName + "_rank");
				attributeHandler =
					new MultiInstanceAttributeHandler(attribute, 1, 2, 3);
			} else {
				queryString.append("," + attributeName + "_rank");
				attributeHandler =
					new MultiValueAttributeHandler(attribute, 1, 2);
			}
			queryString.append(
				" FROM " + attribute.getOrigin().getName() + "_2_" + attributeName);
			orderBy.append(" ORDER BY " + attributeName + "_rank");
		} else {
			if (originalAttribute.isInstanceTypeAttribute()) {
				queryString.append("," + attributeName + "_class");
				attributeHandler =
					new SingleInstanceAttributeHandler(attribute, 1, 2);
			} else {
				attributeHandler =
					new SingleValueAttributeHandler(attribute, 1);
			}
			queryString.append(" FROM " + attribute.getOrigin().getName());
		}
		queryString.append(" WHERE " + DB_ID_NAME + "=?" + orderBy.toString());
        if (debug) System.out.println(queryString.toString() + "\n\t" + instance.getDBID());
		PreparedStatement ps = getConnection().prepareStatement(queryString.toString());
		ps.setObject(1, instance.getDBID());
		ResultSet rs = ps.executeQuery();
		List attributeHandlers = new ArrayList();
		attributeHandlers.add(attributeHandler);
		return new ResultSetWithHandlers(rs, attributeHandlers);
	}

	private ResultSetWithHandlers createAttributeLoadingResultSetWithHandlers(
		Collection instances,
		SchemaAttribute attribute)
		throws SQLException, InvalidAttributeException {
		SchemaAttribute originalAttribute =
			attribute.getOrigin().getAttribute(attribute.getName());
		String attributeName = attribute.getName();
		StringBuffer queryString = new StringBuffer("SELECT " + DB_ID_NAME + "," + attributeName);
		AttributeHandler attributeHandler;
		StringBuffer orderByRank = new StringBuffer("");
		if (originalAttribute.isMultiple()) {
			if (originalAttribute.isInstanceTypeAttribute()) {
				queryString.append(
					"," + attributeName + "_class," + attributeName + "_rank");
				attributeHandler =
					new MultiInstanceAttributeHandler(attribute, 2, 3, 4);
			} else {
				queryString.append("," + attributeName + "_rank");
				attributeHandler =
					new MultiValueAttributeHandler(attribute, 2, 3);
			}
			queryString.append(
				" FROM " + attribute.getOrigin().getName() + "_2_" + attributeName);
			orderByRank.append(" ," + attributeName + "_rank");
		} else {
			if (originalAttribute.isInstanceTypeAttribute()) {
				queryString.append("," + attributeName + "_class");
				attributeHandler =
					new SingleInstanceAttributeHandler(attribute, 2, 3);
			} else {
				attributeHandler =
					new SingleValueAttributeHandler(attribute, 2);
			}
			queryString.append(" FROM " + attribute.getOrigin().getName());
		}
		queryString.append(" WHERE " + DB_ID_NAME + " IN(");
		String[] tmp = new String[instances.size()];
		Arrays.fill(tmp, "?");
		queryString.append(StringUtils.join(",",Arrays.asList(tmp)) + ")");
		queryString.append(" ORDER BY " + DB_ID_NAME + orderByRank.toString());
		if (debug) System.out.println(queryString.toString());
		PreparedStatement ps = getConnection().prepareStatement(queryString.toString());
		int i = 0;
		for (Iterator ii = instances.iterator(); ii.hasNext();) {
			GKInstance ins = (GKInstance) ii.next();
			ps.setObject(++i, ins.getDBID());
			if (debug) System.out.println("\t" + ins.getDBID());
		}
		ResultSet rs = ps.executeQuery();
		List attributeHandlers = new ArrayList();
		attributeHandlers.add(attributeHandler);
		return new ResultSetWithHandlers(rs, attributeHandlers);
	}

	public Collection fetchInstanceByAttribute(SchemaAttribute att, String operator, Object value) throws Exception {
			AttributeQueryRequest aqr = new AttributeQueryRequest(att, operator, value);
			return fetchInstance(aqr);
		}

	public Collection fetchInstanceByAttribute(String cls, String att, String operator, Object value) throws Exception {
		AttributeQueryRequest aqr = new AttributeQueryRequest(cls, att, operator, value);
		return fetchInstance(aqr);
	}

	public Collection fetchInstance(QueryRequest aqr) throws Exception {
		List aqrList = new ArrayList();
		aqrList.add(aqr);
		return fetchInstance(aqrList);
	}
	
	public GKInstance fetchInstance(String className, Long dbID) throws Exception {
		GKInstance instance = null;
		
		// Try to take the instance from the cache first. If not, get it from the database.
		if (useCache) {
			instance = instanceCache.get(dbID);
			if (instance != null)
				return instance;
		}
		
		Collection c = fetchInstanceByAttribute(className, "DB_ID", "=", dbID);
		if (c != null && c.size() == 1) {
			instance = (GKInstance) c.iterator().next();
		}
		return instance;
	}
	
	public GKInstance fetchInstance(Long dbID) throws Exception {
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		return fetchInstance(rootClassName, dbID);
	}

	/**
	 * Method for fetching instances with given DB_IDs
	 * @param dbIDs
	 * @return Collection of instances
	 * @throws Exception
	 */
	public Collection fetchInstance(Collection dbIDs) throws Exception {
		return fetchInstanceByAttribute(((GKSchema) schema).getRootClass().getName(), "DB_ID", "=", dbIDs);
	}
	
	public Set _fetchInstance(List aqrList) throws Exception {
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		StringBuffer sql = new StringBuffer("SELECT " + rootClassName + "." + DB_ID_NAME + "," +
											rootClassName + "._class," +
											rootClassName + "._displayName" +
											fromWhereJoinString(aqrList) +
											"\nORDER BY " + rootClassName + "." + DB_ID_NAME);
		SchemaAttribute _displayName = ((GKSchema) schema).getRootClass().getAttribute("_displayName");
		AttributeHandler ah = new SingleValueAttributeHandler(_displayName, 3);
		if (debug) System.out.println("\n" + sql.toString());
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		int index = 0;
		for (Iterator i = aqrList.iterator(); i.hasNext(); ) {
			QueryRequest aqr = (QueryRequest) i.next();
			if (! aqr.getOperator().equals("IS NULL") && ! aqr.getOperator().equals("IS NOT NULL")) {
				index++;
				Object value = aqr.getValue();
				if (value instanceof String) {
					ps.setString(index, (String) value);
					if (debug) System.out.println("\t" + value.toString());
				}
				else if (value instanceof Integer) {
					ps.setInt(index, ((Integer) value).intValue());
					if (debug) System.out.println("\t" + value);
				}
				else if (value instanceof Float) {
					ps.setFloat(index, ((Float) value).floatValue());
					if (debug) System.out.println("\t" + value);
				}
				else if (value instanceof Boolean) {
					//ps.setBoolean(index, ((Boolean) value).booleanValue());
					// As boolean are implemented as enums in the db we have 
					// to bind the String value.
					ps.setString(index, value.toString());
					if (debug) System.out.println("\t" + value);
				}
				else if (value instanceof Long) {
					ps.setLong(index, ((Long) value).longValue());
					if (debug) System.out.println("\t" + value);
				}
				else if (value instanceof org.gk.model.Instance) {
					ps.setLong(index, ((Instance) value).getDBID().longValue());
					if (debug) System.out.println("\t" + ((Instance) value).getDBID());
				}
				else if (value instanceof Collection) {
					for (Iterator ci = ((Collection) value).iterator(); ci.hasNext();) {
						Object o = ci.next();
						if (o instanceof org.gk.model.Instance)	{
							ps.setLong(index, ((Instance) o).getDBID().longValue());
							if (debug) System.out.println("\t" + ((Instance) o).getDBID());
						} else {
							ps.setString(index, o.toString());
							if (debug) System.out.println("\t" + o);
						}
						if (ci.hasNext()) index++;
					}
				} else {
					throw(new Exception("Don't know how to handle value '" + value.toString() + "'."));
				}
			}
		}
		ResultSet rs = ps.executeQuery();
		Set instances = new HashSet();
		Long dbId = new Long(0);
		Instance instance = null;
		while (rs.next()) {
			if (rs.getLong(1) != dbId.longValue()) {
				dbId = new Long(rs.getLong(1));
				String clsName = rs.getString(2);
				instance = getInstance(clsName, dbId);
				instances.add(instance);
			}
			ah.handleAttributeValue((GKInstance) instance, rs);
		}
		return instances;
	}

	private List replaceSubQueriesWithValues (List aqrList) throws Exception {
		for (Iterator ai = aqrList.iterator(); ai.hasNext();) {
			QueryRequest qr = (QueryRequest) ai.next();
			if (qr.getValue() instanceof QueryRequestList) {
				Set tmp = fetchInstance((List) qr.getValue());
				if (tmp == null || tmp.isEmpty()) {
					//Subquery did not find anything and as everything is ANDed together
					//the whole query can't return anything either.
					return null;
				} else {
					qr.setValue(tmp);
				}
			} else if (qr.getValue() instanceof QueryRequest) {
				Collection tmp = fetchInstance((QueryRequest) qr.getValue());
				if (tmp == null || tmp.isEmpty()) {
					//Subquery did not find anything and as everything is ANDed together
					//the whole query can't return anything either.
					return null;
				} else {
					qr.setValue(tmp);
				}
			}
		}
		return aqrList;
	}

	public Set fetchInstance(List aqrList) throws Exception {
		aqrList = replaceSubQueriesWithValues(aqrList);
		Set out = new HashSet();
		if (aqrList == null) return out;
		int size = aqrList.size();
		//Divide query into chunks
		//1st come up with a chunk size so that the last chunk contains
		//more than 1 subquery. My fear is that a single subquery could
		//return too many results.
		if (size <= MAX_JOINS_PER_QUERY) {
			return _fetchInstance(aqrList);
		}
		int chunkSize = MAX_JOINS_PER_QUERY;
		int mod = size % chunkSize;
		while ((mod > 0) && (mod < 2)) {
			chunkSize--;
			mod = size % chunkSize;
		}
		if (debug) System.out.println("chunkSize = " + chunkSize);
		for (int i = 0; i < size; i += chunkSize) {
			int to = i + chunkSize;
			if (to > size) to = size;
			if (debug) System.out.println("Chunk " + i + " .. " + to);
			List subList = aqrList.subList(i, to);
			Set subSet = _fetchInstance(subList);
			if (subSet.isEmpty()) return subSet;
			if (out.isEmpty()) {
				out = subSet;
			} else {
				Set tmp = new HashSet();
				for (Iterator ssi = subSet.iterator(); ssi.hasNext();) {
					GKInstance ins = (GKInstance) ssi.next();
					if (out.contains(ins)) tmp.add(ins);
				}
				out = tmp;
			}
			if (out.isEmpty()) return out;
		}
		return out;
	}

	public Collection fetchInstancesByClass(SchemaClass schemaClass) throws Exception {
		return fetchInstancesByClass(schemaClass.getName());
	}
	
	/**
	 * Fetch instances from a specied class and a list of db ids in that class.
	 * @param className
	 * @param dbIds
	 * @return
	 * @throws Exception
	 */
	public Collection fetchInstances(String className, List dbIds) throws Exception {
	    if (dbIds == null || dbIds.size() == 0)
	        return new ArrayList();
		((GKSchema) schema).isValidClassOrThrow(className);
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		StringBuffer sql = new StringBuffer("SELECT " + rootClassName + "." + DB_ID_NAME + "," +
											rootClassName + "._class," +
											rootClassName + "._displayName" +
											"\nFROM " + rootClassName);
		if (!className.equals(rootClassName)) {
			sql.append(", " + className
					   + " WHERE " + rootClassName + "." + DB_ID_NAME + "=" + className + "." + DB_ID_NAME +
					   " AND " + rootClassName + "." + DB_ID_NAME + " IN (" + StringUtils.join(",", dbIds) + ")");
		}
		else
		    sql.append(" WHERE " + rootClassName + "." + DB_ID_NAME + " IN (" +
		    		   StringUtils.join(",", dbIds) + ")");
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		Set instances = new HashSet();
		Long dbId = new Long(0);
		Instance instance = null;
		while (rs.next()) {
			long newID = rs.getLong(1); 
			if (newID != dbId.longValue()) {
				dbId = new Long(newID);
				String clsName = rs.getString(2);
				//System.out.println(clsName + ":" + dbId);
				instance = getInstance(clsName, dbId);
				instances.add(instance);
			}
			// Want to mark _displayName loaded if _displayName is null. Don't want to modify 
			// SinleValueAttributeHandler. So I directly modify code here. ---- Guanming
			if (instance != null) {
				String displayName = rs.getString(3);
				instance.setDisplayName(displayName);
			}
		}
		return instances;
	}

	public Collection fetchInstancesByClass(String className) throws Exception {
		((GKSchema) schema).isValidClassOrThrow(className);
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		StringBuffer sql = new StringBuffer("SELECT " + rootClassName + "." + DB_ID_NAME + "," +
											rootClassName + "._class," +
											rootClassName + "._displayName" +
											"\nFROM " + rootClassName);
		if (!className.equals(rootClassName)) {
			sql.append(
					   "," + className
					   + "\nWHERE " + rootClassName + "." + DB_ID_NAME + "=" + className + "." + DB_ID_NAME
					   );
		}
		//SchemaAttribute _displayName = ((GKSchema) schema).getRootClass().getAttribute("_displayName");
		//AttributeHandler ah = new SingleValueAttributeHandler(_displayName, 3);
		//System.out.println("\n" + sql.toString());
		PreparedStatement ps = getConnection().prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		Set instances = new HashSet();
		Long dbId = new Long(0);
		Instance instance = null;
		while (rs.next()) {
			long newID = rs.getLong(1); // To save a little memory
			if (newID != dbId.longValue()) {
				dbId = new Long(newID);
				String clsName = rs.getString(2);
				//System.out.println(clsName + ":" + dbId);
				instance = getInstance(clsName, dbId);
				instances.add(instance);
			}
			// Want to mark _displayName loaded if _displayName is null. Don't want to modify 
			// SinleValueAttributeHandler. So I directly modify code here. ---- Guanming
			if (instance != null) {
				String displayName = rs.getString(3);
				instance.setDisplayName(displayName);
			}
			//ah.handleAttributeValue((GKInstance) instance, rs);
		}
		return instances;
	}

	public long getClassInstanceCount(SchemaClass cls) throws InvalidClassException, SQLException {
		return getClassInstanceCount(cls.getName());
	}
	
	public long getClassInstanceCount(String className) throws InvalidClassException, SQLException {
		((GKSchema) schema).isValidClassOrThrow(className);
		String sql = new String("SELECT COUNT(*) FROM " + className);
		PreparedStatement ps = getConnection().prepareStatement(sql);
		ResultSet rs = ps.executeQuery();
		rs.next();
		long rtn = rs.getLong(1);
        rs.close();
        ps.close();
        return rtn;
	}
    
    public Map getAllInstanceCounts() throws Exception {
        Map map = new HashMap();
        String query = "select count(DB_ID), _class from DatabaseObject group by _class";
        Connection connection = getConnection();
        Statement stat = connection.createStatement();
        ResultSet result = stat.executeQuery(query);
        while (result.next()) {
            long count = result.getLong(1);
            String cls = result.getString(2);
            map.put(cls, new Long(count));
        }
        result.close();
        stat.close();
        return map;
    }

	public Collection fetchInstancesOfAllowedClasses(GKSchemaAttribute attribute) {
		Set results = new HashSet();
		for (Iterator i = attribute.getAllowedClasses().iterator(); i.hasNext();) {
			SchemaClass cls = (SchemaClass) i.next();
			try {
				results.addAll(fetchInstancesByClass(cls));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return results;
	}
	
	private String fromWhereJoinString(List aqrList) throws Exception {
		List from = new ArrayList();
		List where = new ArrayList();
		List join = new ArrayList();
		String rootClassDbIdStr = ((GKSchema) schema).getRootClass().getName() + "." + DB_ID_NAME;
		for (int i = 0; i < aqrList.size(); i++) {
			QueryRequest aqr = (QueryRequest) aqrList.get(i);
			String attName = aqr.getAttribute().getName();
			String tableName = (aqr.getAttribute().isMultiple())
				? aqr.getAttribute().getOrigin().getName() + "_2_" + attName
				: aqr.getAttribute().getOrigin().getName();
			String alias = "A" + i;
			if (aqr instanceof ReverseAttributeQueryRequest) {
				//TODO: use left join only for IS NULL queries.
				join.add("LEFT JOIN " + tableName + " AS " + alias +
					" ON (" + alias + "." + attName + "=" + rootClassDbIdStr + ")");
				if (aqr.getOperator().equals("IS NULL")) {
					where.add(alias + "." + attName + " IS NULL");
				} else if (aqr.getOperator().equals("IS NOT NULL")) {
					where.add(alias + "." + attName + " IS NOT NULL");
				} else {
					if (aqr.getValue() instanceof Collection) {
						if (((Collection) aqr.getValue()).isEmpty()) {
							throw new Exception("Empty Collection used in query.");
						}
						StringBuffer sb = new StringBuffer(alias + "." + DB_ID_NAME + " IN(");
						String[] tmp = new String[((Collection) aqr.getValue()).size()];
						Arrays.fill(tmp, "?");
						sb.append(StringUtils.join(",",Arrays.asList(tmp)) + ")");
						where.add(sb.toString());
					} else {
						where.add(alias + "." + DB_ID_NAME + "=?");
					}
				}
			} else {
				if (aqr.getOperator().equals("IS NULL")) {
					join.add("LEFT JOIN " + tableName + " AS " + alias +
							 " ON (" + alias + "." + DB_ID_NAME + "=" + rootClassDbIdStr + ")");
					where.add(alias + "." + attName + " IS NULL");
				} else {
					from.add(tableName + " AS " + alias);
					where.add(alias + "." + DB_ID_NAME + "=" + rootClassDbIdStr);
					if (aqr.getOperator().equals("IS NOT NULL")) {
						where.add(alias + "." + attName + " IS NOT NULL");
					}
					else if (aqr.getValue() instanceof Collection) {
						if (((Collection) aqr.getValue()).isEmpty()) {
							throw new Exception("Empty Collection used in query.");
						}
						StringBuffer sb = new StringBuffer(alias + "." + attName + " IN(");
						String[] tmp = new String[((Collection) aqr.getValue()).size()];
						Arrays.fill(tmp, "?");
						sb.append(StringUtils.join(",",Arrays.asList(tmp)) + ")");
						where.add(sb.toString());
					}
					else if (aqr.getOperator().equals("MATCH")) {
						where.add("MATCH(" + alias + "." + attName +") AGAINST(?)");
					}
					else if (aqr.getOperator().equals("MATCH IN BOOLEAN MODE")) {
						where.add("MATCH(" + alias + "." + attName +") AGAINST(? IN BOOLEAN MODE)");
					}
					else {
						where.add(alias + "." + attName + " " + aqr.getOperator() + " ?");
					}
				}
			}
		}
		/*
		 * Do the class restriction here.
		 */
        from.add(((QueryRequest) aqrList.get(0)).getCls().getName() + " AS A");
        where.add("A." + DB_ID_NAME + "=" + rootClassDbIdStr);
		/*
		 * Put the root class name last on the list to make left join work with MySQL 5.
		 */
		from.add(((GKSchema) schema).getRootClass().getName());
		StringBuffer out = new StringBuffer();
		out.append(" FROM " + StringUtils.join(",\n", from));
		if (join.size() > 0) {
			out.append("\n" + StringUtils.join("\n", join));
		}
		out.append("\nWHERE\n" + StringUtils.join("\nAND ", where));
		return out.toString();
	}
	
	public AttributeQueryRequest createAttributeQueryRequest(String clsName, String attName, String operator, Object value) throws InvalidClassException, InvalidAttributeException {
		return new AttributeQueryRequest(clsName, attName, operator, value);
	}

	public AttributeQueryRequest createAttributeQueryRequest(SchemaAttribute attribute, String operator, Object value) throws InvalidAttributeException {
		return new AttributeQueryRequest(attribute, operator, value);
	}

	public ReverseAttributeQueryRequest createReverseAttributeQueryRequest(String clsName, String attName, String operator, Object value) throws Exception {
		return new ReverseAttributeQueryRequest(clsName, attName, operator, value);
	}
	
	public ReverseAttributeQueryRequest createReverseAttributeQueryRequest(SchemaClass cls,
	                                                                      SchemaAttribute att,
	                                                                      String operator,
	                                                                      Object value) throws InvalidAttributeException {
	    return new ReverseAttributeQueryRequest(cls, att, operator, value);
	}
	                                    

	public class QueryRequestList extends ArrayList {
		
	}

	public abstract class QueryRequest {
		protected SchemaClass cls;
		protected SchemaAttribute attribute;
		protected String operator;
		protected Object value;
		
		/**
		 * @return
		 */
		public SchemaAttribute getAttribute() {
			return attribute;
		}

		/**
		 * @return
		 */
		public SchemaClass getCls() {
			return cls;
		}

		/**
		 * @return
		 */
		public String getOperator() {
			return operator;
		}

		/**
		 * @return
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * @param attribute
		 */
		public void setAttribute(SchemaAttribute attribute) {
			this.attribute = attribute;
		}

		/**
		 * @param class1
		 */
		public void setCls(SchemaClass class1) {
			cls = class1;
		}

		/**
		 * @param string
		 */
		public void setOperator(String string) {
			operator = string;
		}

		/**
		 * @param object
		 */
		public void setValue(Object object) {
			value = object;
		}
	}

	public class ReverseAttributeQueryRequest extends QueryRequest {
		
		public ReverseAttributeQueryRequest(SchemaAttribute attribute, String operator, Long dbID) throws InvalidAttributeException {
			cls = attribute.getOrigin();
			//this.attribute = attribute;
			this.attribute = attribute.getOrigin().getAttribute(attribute.getName());
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = dbID;
		}
		
		public ReverseAttributeQueryRequest(String clsName, String attName, String operator, Object value) throws Exception {
			schema.isValidClassOrThrow(clsName);
			cls = schema.getClassByName(clsName);
			Collection<GKSchemaAttribute> reverseAttributes = ((GKSchemaClass) cls).getReferersByName(attName);
			Set<GKSchemaClass> origins = new HashSet<GKSchemaClass>();
			for (GKSchemaAttribute revAtt : reverseAttributes) {
				origins.add((GKSchemaClass) revAtt.getOrigin());
			}
			//System.out.println(origins);
			if (origins.size() > 1) {
				throw new Exception("Class '" + clsName + "' has many referers with attribute '" + attName  + 
						"' - don't know which one to use. Use ReverseAttributeQueryRequest(SchemaClass cls, " + 
						"SchemaAttribute att, String operator, Object value) to construct this query.");
			}
			attribute = origins.iterator().next().getAttribute(attName);
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = value;
		}
		
		/**
		 * An overloaded constructor with class and attributes specified so no checking is needed.
		 * @param cls
		 * @param att
		 * @param operator
		 * @param value
		 * @throws InvalidClassException
		 * @throws InvalidAttributeException
		 */
		public ReverseAttributeQueryRequest(SchemaClass cls, SchemaAttribute att, String operator, Object value) 
		                                    throws InvalidAttributeException {
		    this.cls = cls;
		    attribute = att.getOrigin().getAttribute(att.getName());
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = value;
		}
		
	}

	public class AttributeQueryRequest extends QueryRequest {
		
		public AttributeQueryRequest(String clsName, String attName, String operator, Object value) throws InvalidClassException, InvalidAttributeException {
			schema.isValidClassOrThrow(clsName);
			cls = schema.getClassByName(clsName);
			// getAttribute checks for the validity
			attribute = cls.getAttribute(attName).getOrigin().getAttribute(attName);
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = value;
		}
		
		public AttributeQueryRequest(SchemaAttribute attribute, String operator, Object value) throws InvalidAttributeException {
			cls = attribute.getOrigin();
			//this.attribute = attribute;
			this.attribute = attribute.getOrigin().getAttribute(attribute.getName());
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = value;
		}

		public AttributeQueryRequest(SchemaClass cls, SchemaAttribute attribute, String operator, Object value) throws InvalidAttributeException {
			this.cls = cls;
			this.attribute = attribute.getOrigin().getAttribute(attribute.getName());
			if (operator == null || operator.equals("")) {
				operator = "=";
			}
			this.operator = operator.toUpperCase();
			this.value = value;
		}
		
	}

	class ResultSetWithHandlers {
		private ResultSet rs;
		private List attributeHandlers;

		public ResultSetWithHandlers(ResultSet rs, List attributeHandlers) {
			this.rs = rs;
			this.attributeHandlers = attributeHandlers;
		}
		/**
		 * @return
		 */
		public List getAttributeHandlers() {
			return attributeHandlers;
		}

		/**
		 * @return
		 */
		public ResultSet getResultSet() {
			return rs;
		}

	}

	abstract class AttributeHandler {
		protected SchemaAttribute attribute;
		protected int valueColIndex;
		protected int classColIndex;
		protected int rankColIndex;

		public abstract void handleAttributeValue(GKInstance instance, ResultSet rs) throws Exception;

	}

	class SingleValueAttributeHandler extends AttributeHandler {
		public SingleValueAttributeHandler(
			SchemaAttribute attribute,
			int valueColIndex) {
			this.attribute = attribute;
			this.valueColIndex = valueColIndex;
		}
		
		public void handleAttributeValue(GKInstance instance, ResultSet rs) throws Exception {
			if (rs.getString(valueColIndex) != null) {
				switch(attribute.getTypeAsInt()) {
					case SchemaAttribute.STRING_TYPE : case SchemaAttribute.ENUM_TYPE :
						String stringValue = rs.getString(valueColIndex);
						instance.setAttributeValueNoCheck(attribute, stringValue);
						break;
					case SchemaAttribute.INTEGER_TYPE :
						int intValue = rs.getInt(valueColIndex);
						instance.setAttributeValueNoCheck(attribute, new Integer(intValue));
						break;
					case SchemaAttribute.LONG_TYPE :
						long longValue = rs.getLong(valueColIndex);
						instance.setAttributeValueNoCheck(attribute, new Long(longValue));
						break;
					case SchemaAttribute.FLOAT_TYPE :
						float floatValue = rs.getFloat(valueColIndex);
						instance.setAttributeValueNoCheck(attribute, new Float(floatValue));
						break;
					case SchemaAttribute.BOOLEAN_TYPE :
						boolean booleanValue = rs.getBoolean(valueColIndex);
						instance.setAttributeValueNoCheck(attribute, new Boolean(booleanValue));
						break;
					default :
						throw new Exception("Unknow value type: " + attribute.getTypeAsInt());
				}
			}
		}
	}

	class MultiValueAttributeHandler extends AttributeHandler {
		public MultiValueAttributeHandler(
			SchemaAttribute attribute,
			int valueColIndex,
			int rankColIndex) {
			this.attribute = attribute;
			this.valueColIndex = valueColIndex;
			this.rankColIndex = rankColIndex;
		}

		public void handleAttributeValue(GKInstance instance, ResultSet rs) throws Exception {
			int index = rs.getInt(rankColIndex);
			if (rs.getString(valueColIndex) != null) {
				switch(attribute.getTypeAsInt()) {
					case SchemaAttribute.STRING_TYPE :
						String stringValue = rs.getString(valueColIndex);
						//instance.addAttributeValueNoCheck(attribute, stringValue);
						instance.setAttributeValuePositionNoCheck(attribute,index,stringValue);
						break;
					case SchemaAttribute.INTEGER_TYPE :
						int intValue = rs.getInt(valueColIndex);
						//instance.addAttributeValueNoCheck(attribute, new Integer(intValue));
						instance.setAttributeValuePositionNoCheck(attribute,index,new Integer(intValue));
						break;
					case SchemaAttribute.LONG_TYPE :
						long longValue = rs.getLong(valueColIndex);
						//instance.addAttributeValueNoCheck(attribute, new Long(longValue));
						instance.setAttributeValuePositionNoCheck(attribute,index,new Long(longValue));
						break;
					case SchemaAttribute.FLOAT_TYPE :
						float floatValue = rs.getFloat(valueColIndex);
						//instance.addAttributeValueNoCheck(attribute, new Float(floatValue));
						instance.setAttributeValuePositionNoCheck(attribute,index,new Float(floatValue));
						break;
					case SchemaAttribute.BOOLEAN_TYPE :
						boolean booleanValue = rs.getBoolean(valueColIndex);
						//instance.addAttributeValueNoCheck(attribute, new Boolean(booleanValue));
						instance.setAttributeValuePositionNoCheck(attribute,index,new Boolean(booleanValue));
						break;
					default :
						throw new Exception("Unknow value type: " + attribute.getTypeAsInt());
				}
			}
		}
	}

	class SingleInstanceAttributeHandler extends AttributeHandler {
		public SingleInstanceAttributeHandler(
			SchemaAttribute attribute,
			int valueColIndex,
			int classColIndex) {
			this.attribute = attribute;
			this.valueColIndex = valueColIndex;
			this.classColIndex = classColIndex;
		}
		
		public void handleAttributeValue(GKInstance instance, ResultSet rs) throws Exception {
			if (rs.getString(valueColIndex) != null) {
				Long dbID = new Long(rs.getLong(valueColIndex));
				String className = rs.getString(classColIndex);
				Instance valueInstance = getInstance(className, dbID);
				instance.setAttributeValueNoCheck(attribute, valueInstance);
			}
		}
	}

	class MultiInstanceAttributeHandler extends AttributeHandler {
		public MultiInstanceAttributeHandler(
			SchemaAttribute attribute,
			int valueColIndex,
			int classColIndex,
			int rankColIndex) {
			this.attribute = attribute;
			this.valueColIndex = valueColIndex;
			this.classColIndex = classColIndex;
			this.rankColIndex = rankColIndex;
		}
		
		public void handleAttributeValue(GKInstance instance, ResultSet rs) throws Exception {
			Long dbID = new Long(rs.getLong(valueColIndex));
			String className = rs.getString(classColIndex);
			int index = rs.getInt(rankColIndex);
			Instance valueInstance = getInstance(className, dbID);
			//instance.addAttributeValueNoCheck(attribute, valueInstance);
			instance.setAttributeValuePositionNoCheck(attribute,index,valueInstance);
		}
	}

	public static void main(String[] args) {
		if (args.length == 9) {
			try {
				MySQLAdaptor dba =
					new MySQLAdaptor(
						args[0],
						args[1],
						args[2],
						args[3],
						Integer.parseInt(args[4]));
				
				Collection c =
					dba.fetchInstanceByAttribute(
						args[5],
						args[6],
						args[7],
						args[8]);
				System.out.println("Got " + c.size() + " instance(s):");
				for (Iterator i = c.iterator(); i.hasNext();) {
					GKInstance ins = (GKInstance) i.next();
					//System.out.println(ins.getExtendedDisplayName());
					System.out.println(ins.toStanza());
					System.out.println(ins.toStanza());
				}
//				Collection c = dba.fetchInstancesByClass("Reaction");
//				java.util.List list = new ArrayList();
//				boolean contains = false;
//				for (Iterator it = c.iterator(); it.hasNext();) {
//					GKInstance i = (GKInstance) it.next();
//					if (i.getDisplayName().length() == 0)
//						continue;
//					contains = false;
//					for (Iterator it1 = list.iterator(); it1.hasNext();) {
//						GKInstance i1 = (GKInstance) it1.next();
//						if (i1.getDisplayName().equals(i.getDisplayName())) {
//							contains = true;
//						}
//					}
//					if (contains)
//						continue;
//					list.add(i);
//				}
//				Collections.sort(list, new Comparator() {
//					public int compare(Object obj1, Object obj2) {
//						GKInstance instance1 = (GKInstance) obj1;
//						GKInstance instance2 = (GKInstance) obj2;
//						return instance1.getDisplayName().compareTo(instance2.getDisplayName());
//					}
//				});
//				PrintWriter writer = new PrintWriter(new FileWriter("Reaction.txt"));
//				for (Iterator it = list.iterator();  it.hasNext();) {
//					GKInstance instance = (GKInstance) it.next();
//					String name = instance.getDisplayName();
//					writer.println(instance.getDisplayName());
//				}
//				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("java MySQLAdaptor host database user password port Class Attribute operator value");
		}
	}
	/**
	 * @return
	 */
	public Map getClassMap() {
		return classMap;
	}
	/**
	 * @param map
	 */
	public void setClassMap(Map map) {
		classMap = map;
	}
	
	/**
	 * Store a list of GKInstance objects that created from the application tool. These
	 * objects might be referred to each other. 
	 * @param localInstances a list of GKInstance objects with negative DB_ID
	 * @return a list of DB_IDs that are generated from the DB. The sequence of this list
	 * is the same as localInstances.
	 */
	public java.util.List storeLocalInstances(java.util.List localInstances)
            throws Exception {
        if (localInstances == null || localInstances.size() == 0)
            return new ArrayList(0);
        java.util.List dbIDs = new ArrayList(localInstances.size());
        // Have to get the DB_IDs first to resolve the reference issues.
        String rootName = ((GKSchema) schema).getRootClass().getName();
        // Always use null for DB_ID to get a new ID from the database.
        String insert = new String("INSERT INTO " + rootName + " SET DB_ID=NULL");
        Statement stat = getConnection().createStatement();
        for (Iterator it = localInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            stat.executeUpdate(insert);
            ResultSet rs = stat.getGeneratedKeys();
            if (rs.next()) {
                Long dbID = new Long(rs.getLong(1));
                instance.setDBID(dbID);
                dbIDs.add(dbID);
            } else
                throw new SQLException("Unable to get autoincremented value");
        }
        // Save instances one by one
        String delete = new String("DELETE FROM " + rootName + " WHERE DB_ID=?");
        PreparedStatement ps = getConnection().prepareStatement(delete);
        for (Iterator it = localInstances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            ps.setObject(1, instance.getDBID());
            if (ps.executeUpdate() == 1)
                storeInstance(instance, true);
            else
                throw new SQLException("Unable to delete an empty DB_ID");
        }
        return dbIDs;
    }

	public Long storeInstance(GKInstance instance) throws Exception {
		return storeInstance(instance, false);
	}

	/**
	 * Store a GKInstance object into the database. The implementaion of this method will store
	 * all newly created, referred GKInstances by the specified GKInstance if they are not in the
	 * database. 
	 * @param instance it might be from the local file system.
	 * @param forceStore
	 * @return
	 * @throws Exception
	 */
	public Long storeInstance(GKInstance instance, boolean forceStore) throws Exception {
		Long dbID = null;
		if (forceStore) {
			dbID = instance.getDBID();
		} else if ((dbID = instance.getDBID()) != null) { 
			return dbID;
		}
		SchemaClass cls = instance.getSchemClass();
		// cls might be from a local Schema copy. Convert it to the db copy.
		cls = schema.getClassByName(cls.getName());
		SchemaClass rootCls = ((GKSchema) getSchema()).getRootClass();
		List classHierarchy = new ArrayList();
		classHierarchy.addAll(cls.getOrderedAncestors());
		classHierarchy.add(cls);
		Collection later = new ArrayList();
		for (Iterator ancI = classHierarchy.iterator(); ancI.hasNext();) {
			GKSchemaClass ancestor = (GKSchemaClass) ancI.next();
			StringBuffer statement = new StringBuffer("INSERT INTO " + ancestor.getName() + " SET DB_ID=?");
			List values = new ArrayList();
			values.add(dbID);
			if (ancestor == rootCls) {
				statement.append(",_class=?");
				values.add(cls.getName());
			}
			Collection multiAtts = new ArrayList();
			for (Iterator attI = ancestor.getOwnAttributes().iterator(); attI.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) attI.next();
				if (att.getName().equals("DB_ID")) continue;
				List attVals = instance.getAttributeValuesList(att.getName());
				if ((attVals == null) || (attVals.isEmpty())) continue;
				if (att.isMultiple()) {
					multiAtts.add(att);
				} else {
					if (att.isInstanceTypeAttribute()) {
						if (ancestor == rootCls) {
							later.add(att);
						} else {
							GKInstance val = (GKInstance) attVals.get(0);
							statement.append("," + att.getName() + "=?");
							values.add(storeInstance(val));
							statement.append("," + att.getName() + "_class=?");
							values.add(val.getSchemClass().getName());
						}
					} else {
						statement.append("," + att.getName() + "=?");
						values.add(instance.getAttributeValue(att.getName()));
					}
				}
			}
			PreparedStatement ps = getConnection().prepareStatement(statement.toString());
			for (int i = 0; i < values.size(); i++) {
				Object value = values.get(i);
				
				// Boolean is a special case.  It maps on to enum('TRUE','FALSE')
				// in the database, but MYSQL doesn't make a very good job of
				// generating these values.  Boolean.TRUE maps on to 'TRUE' in
				// the database, which is fine, but Boolean.FALSE maps onto
				// an empty cell, which is not nice.  This behaviour has been
				// observed in both MYSQL 4.0.24 and MYSQL 4.1.9.  The fix here
				// is to supply the strings "TRUE" or "FALSE" instead of a
				// Boolean value.  This has been tested and works for both of
				// the abovementioned MYSQL versions.
				if (value instanceof Boolean)
					value = ((Boolean)value).booleanValue()?"TRUE":"FALSE";
				
				ps.setObject(i + 1, value);
			}
			ps.executeUpdate();
			if (dbID == null) {
				ResultSet rs = ps.getGeneratedKeys();
				if (rs.next()) {
					dbID = new Long(rs.getLong(1));
					instance.setDBID(dbID);
				} else {
					throw(new Exception("Unable to get autoincremented value."));
				}
			}
			for (Iterator attI = multiAtts.iterator(); attI.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) attI.next();
				List attVals = instance.getAttributeValuesList(att.getName());
				StringBuffer statement2 =
					new StringBuffer("INSERT INTO " + ancestor.getName() + "_2_" + att.getName() +
						" SET DB_ID=?," + att.getName() + "=?," + att.getName() + "_rank=?");
				if (att.isInstanceTypeAttribute()) {
					statement2.append("," + att.getName() + "_class=?");
				}
				//System.out.println(statement2.toString() + "\n");
				PreparedStatement ps2 = getConnection().prepareStatement(statement2.toString());
				for (int i = 0; i < attVals.size(); i++) {
					ps2.setObject(1, dbID);
					ps2.setInt(3, i);
					if (att.isInstanceTypeAttribute()) {
						GKInstance attVal = (GKInstance) attVals.get(i);
						ps2.setObject(2, storeInstance(attVal));
						ps2.setString(4, attVal.getSchemClass().getName());
					} else {
						ps2.setObject(2, attVals.get(i));
					}
					ps2.executeUpdate();
				}
			}
		}
		if (! later.isEmpty()) {
			StringBuffer statement = new StringBuffer("UPDATE " + rootCls.getName() + " SET ");
			List values = new ArrayList();
			for (Iterator li = later.iterator(); li.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) li.next();
				statement.append(att.getName() + "=?, " + att.getName() + "_class=?");
				if (li.hasNext()) statement.append(",");
				GKInstance value = (GKInstance) instance.getAttributeValue(att.getName());
				values.add(storeInstance(value));
				values.add(value.getSchemClass().getName());
			}
			statement.append(" WHERE DB_ID=?");
			values.add(dbID);
			//System.out.println(statement.toString() + "\n");
			PreparedStatement ps = getConnection().prepareStatement(statement.toString());
			for (int i = 0; i < values.size(); i++) {
				ps.setObject(i + 1, values.get(i));
			}
			ps.executeUpdate();
		}
		return dbID;
	}

	public void updateInstanceAttribute (GKInstance instance, SchemaAttribute attribute) throws Exception {
		updateInstanceAttribute(instance, attribute.getName());
	}

	public void updateInstanceAttribute (GKInstance instance, String attributeName) throws Exception {
		if (instance.getDBID() == null) {
			throw(new DBIDNotSetException(instance));
		}
		SchemaAttribute attribute = instance.getSchemClass().getAttribute(attributeName);
		if (attribute.isMultiple()) {
			updateInstanceMultivalueAttribute(instance,attribute);
		} else {
			updateInstanceSinglevalueAttribute(instance,attribute);
		}
	}
	
	/**
	 * Query the release number. If no release number information stored in the database, a null will 
	 * returned.
	 * @return
	 * @throws Exception 
	 */
	public Integer getReleaseNumber() throws Exception {
	    if (!getSchema().isValidClass(ReactomeJavaConstants._Release))
	        return null;
	    Collection<?> c = fetchInstancesByClass(ReactomeJavaConstants._Release);
	    if (c == null || c.size() == 0)
	        return null;
	    GKInstance release = (GKInstance) c.iterator().next();
	    Integer releaseNumber = (Integer) release.getAttributeValue(ReactomeJavaConstants.releaseNumber);
	    return releaseNumber;
	}
	
	private void updateInstanceSinglevalueAttribute (GKInstance instance, SchemaAttribute attribute) throws Exception {
		String tableName = attribute.getOrigin().getName();
		List values = new ArrayList();
		StringBuffer statement = new StringBuffer("UPDATE " + tableName + " SET " + attribute.getName() + "=?");
		if (attribute.isInstanceTypeAttribute()) {
			statement.append("," + attribute.getName() + "_class=?");
			GKInstance attVal;
			if ((attVal = (GKInstance) instance.getAttributeValue(attribute)) != null) {
				values.add(storeInstance(attVal));
				values.add(attVal.getSchemClass().getName());
			} else {
				values.add(null);
				values.add(null);
			}
		} else {
			values.add(instance.getAttributeValue(attribute));
		}
		statement.append(" WHERE DB_ID=?");
		values.add(instance.getDBID());
		PreparedStatement ps = getConnection().prepareStatement(statement.toString());
		for (int i = 0; i < values.size(); i++) {
			ps.setObject(i + 1, values.get(i));
		}
		ps.executeUpdate();
	}
	
	private void deleteInstanceMultivalueAttributeValues(GKInstance instance, SchemaAttribute attribute) throws SQLException {
		String attName = attribute.getName();
		String tableName = new String(attribute.getOrigin().getName() + "_2_" + attName);
		StringBuffer statement = new StringBuffer("DELETE FROM " + tableName + " WHERE DB_ID=?");
		PreparedStatement ps = getConnection().prepareStatement(statement.toString());
		ps.setObject(1, instance.getDBID());
		ps.executeUpdate();
	}
	
	private void updateInstanceMultivalueAttribute (GKInstance instance, SchemaAttribute attribute) throws Exception {
		//System.out.println("Updating attribute " + attribute + " of " + instance.getExtendedDisplayName());
		if (! instance.isAttributeValueLoaded(attribute)) {
			throw new Exception("Updating intance " + instance + " attribute '" + attribute +
						  "' without attribute values being set.");
		}
		deleteInstanceMultivalueAttributeValues(instance, attribute);
		List attVals;
		if ((attVals = instance.getAttributeValuesList(attribute)) != null) {
			// This shoouldn't be necessary any more
			//// Have to make sure all instances exist. Some in list might be already deleted in deleteTemporarily method.
		    //validateExistence(attVals);
		    String attName = attribute.getName();
			String tableName = new String(attribute.getOrigin().getName() + "_2_" + attName);
			StringBuffer statement = new StringBuffer("INSERT INTO " + tableName + " SET DB_ID=?," + attName + "_rank=?," + attName + "=?");
			if (attribute.isInstanceTypeAttribute()) {
				statement.append("," + attName + "_class=?");
			}
			PreparedStatement ps = getConnection().prepareStatement(statement.toString());
			for (int i = 0; i < attVals.size(); i++) {
				ps.setObject(1, instance.getDBID());
				ps.setInt(2, i);
				if (attribute.isInstanceTypeAttribute()) {
					GKInstance valInstance = (GKInstance) attVals.get(i);
					ps.setObject(3, storeInstance(valInstance));
					ps.setString(4, valInstance.getSchemClass().getName());
				} else {
					ps.setObject(3, attVals.get(i));
				}
				ps.executeUpdate();
			}
		}
	}
	
	private void validateExistence(java.util.List instances) {
	    if (instances == null)
	        return;
	    if (instances != null) {
	        GKInstance instance = null;
	        for (Iterator it = instances.iterator(); it.hasNext();) {
	            instance = (GKInstance) it.next();
	            if(!exist(instance.getDBID()))
	                it.remove();
	        }
	    }
	}
	
	public String fetchSchemaClassnameByDBID(Long dbID) throws SQLException {
		String statement = "SELECT _class FROM " + ((GKSchema) schema).getRootClass().getName() + " WHERE DB_ID=?";
		PreparedStatement ps = getConnection().prepareStatement(statement);
		ps.setObject(1, dbID);
		ResultSet rs = ps.executeQuery();
		rs.next();
		return rs.getString(1);
	}
	
	public SchemaClass fetchSchemaClassByDBID(Long dbID) throws SQLException {
		return schema.getClassByName(fetchSchemaClassnameByDBID(dbID));
	}	

	public void updateInstance(GKInstance instance) throws Exception {
		Long dbID = instance.getDBID();
		if (dbID == null) {
			throw(new DBIDNotSetException(instance));
		}
		SchemaClass oldClass = fetchSchemaClassByDBID(dbID);
		deleteInstanceTemporarily(oldClass, dbID);
		storeInstance(instance,true);
		if (! oldClass.getName().equals(instance.getSchemClass().getName())) {
			//Referers will be refering to the old instance. However, since the cache
			//contains new instance this gets loaded as the value to the referers.
			updateReferers(instance, oldClass);
		}
		// Deflate the cached copy (if any) of the updated instance. This way it
		// will be loaded with new values when they are asked for.
		GKInstance cachedInstance;
		if ((cachedInstance = (GKInstance) instanceCache.get(dbID)) != null) {
		    SchemaClass newCls = schema.getClassByName(instance.getSchemClass().getName());
		    cachedInstance.setSchemaClass(newCls);
			//cachedInstance.setSchemaClass(instance.getSchemClass());
			cachedInstance.deflate();
			fastLoadInstanceAttributeValues(cachedInstance);
            //loadInstanceAttributeValues(cachedInstance);
			// A bug in schema: DB_ID is Long in GKInstance but Integer in schema
			// Manual reset it
			cachedInstance.setDBID(cachedInstance.getDBID());
		}
	}	

	private void updateReferers(GKInstance instance, SchemaClass oldClass) throws Exception {
		String newClassName = instance.getSchemClass().getName();
		Long dbID = instance.getDBID();
		PreparedStatement ps;
		for (Iterator ri = oldClass.getReferers().iterator(); ri.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) ri.next();
			SchemaAttribute originalAtt = att.getOriginalAttribute();
			String attName = originalAtt.getName();
			SchemaClass origin = originalAtt.getOrigin();
			String tableName;
			if (originalAtt.isMultiple()) {
				tableName = origin.getName() + "_2_" + attName;
			} else {
				tableName = origin.getName();
			}
			String statement = "UPDATE " + tableName + " SET " + attName + "_class = ?" +
							  " WHERE " + attName + " = ?";
			ps = getConnection().prepareStatement(statement);
			ps.setString(1, newClassName);
			ps.setObject(2, dbID);
			//System.out.println(statement + "\n" + newClassName + "\n" + dbID);
			ps.executeUpdate();
		}
	}

	public void deleteByDBID(Long dbID) throws Exception {
		GKInstance instance = fetchInstance(((GKSchema) schema).getRootClass().getName(), dbID);
		deleteInstance(instance);
	}
	
	/**
	 * Deletes the supplied instance from the database.  Also deletes the instance
	 * from all referrers, in the sense that if the instance is referred to in
	 * an attribute of a referrer, it will be removed from that attribute.
	 * @param instance
	 * @return Set of GKInstances which were referring to the
	 * deleted instance.
	 * @throws Exception
	 */
	public void deleteInstance(GKInstance instance) throws Exception {
		Long dbID = instance.getDBID();
        // In case this instance is in the referrers cache of its references
        cleanUpReferences(instance);
		SchemaClass cls = fetchSchemaClassByDBID(dbID);
		deleteInstanceTemporarily(cls, dbID);
		//Have to delete the Instance from the cache,
		//but only after it has been deleted from referers.
		instanceCache.remove(instance.getDBID());
	}
    
    /**
     * This method is used to cleanup the referrers cache for a reference to a deleted GKInstance.
     * If this method is not called, a deleted GKInstance might be stuck in the referrers map.
     * @param instance
     * @throws Exception
     */
    private void cleanUpReferences(GKInstance instance) throws Exception {
        SchemaAttribute att = null;
        for (Iterator it = instance.getSchemClass().getAttributes().iterator(); it.hasNext();) {
            att = (SchemaAttribute) it.next();
            if (!att.isInstanceTypeAttribute()) 
                continue;
            List values = instance.getAttributeValuesList(att);
            if (values == null || values.size() == 0)
                continue;
            for (Iterator it1 = values.iterator(); it1.hasNext();) {
                GKInstance references = (GKInstance) it1.next();
                references.clearReferers(); // Just to refresh the whole referers map
            }
        }
    }

	private void deleteInstanceTemporarily(SchemaClass cls, Long dbID) throws Exception {
		List classHierarchy = new ArrayList();
		classHierarchy.addAll(cls.getOrderedAncestors());
		classHierarchy.add(cls);
		String statement;
		PreparedStatement ps;
		for (Iterator ci = classHierarchy.iterator(); ci.hasNext();) {
			GKSchemaClass currentCls = (GKSchemaClass) ci.next();
			statement = "DELETE FROM " + currentCls.getName() + " WHERE DB_ID=?";
			ps = getConnection().prepareStatement(statement);
			ps.setObject(1, dbID);
			ps.executeUpdate();
			for (Iterator ai = currentCls.getOwnAttributes().iterator(); ai.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) ai.next();
				if (att.isMultiple()) {
					statement = "DELETE FROM " + currentCls.getName() + "_2_" + att.getName() + " WHERE DB_ID=?";
					ps = getConnection().prepareStatement(statement);
					ps.setObject(1, dbID);
					ps.executeUpdate();
				}
			}
		}
	}	
	
	/**
	 * Check if a list of DB_IDs exit in the database.
	 * @param dbIds
	 * @return
	 */
	public boolean exist(List dbIds) throws SQLException {
	    if (dbIds == null || dbIds.size() == 0)
	        return true;
	    List needCheckedList = new ArrayList(dbIds);
	    for (Iterator it = needCheckedList.iterator(); it.hasNext();) {
	        Long dbID = (Long) it.next();
	        if (instanceCache.containsKey(dbID))
	            it.remove();
	    }
	    if (needCheckedList.size() == 0)
	        return true;
		SchemaClass root = ((GKSchema)schema).getRootClass();
		String query = "SELECT DB_ID FROM " + root.getName() + " WHERE DB_ID IN (" + 
		                StringUtils.join(",", needCheckedList) + ")";
		Set idsInDB = new HashSet();
		Statement stat = getConnection().createStatement();
		ResultSet resultSet = stat.executeQuery(query);
	    while (resultSet.next()) {
	        long id = resultSet.getLong(1);
	        idsInDB.add(new Long(id));
	    }
	    resultSet.close();
	    stat.close();
	    for (Iterator it = needCheckedList.iterator(); it.hasNext();) {
	        Long id = (Long) it.next();
	        if (!idsInDB.contains(id))
	            return false;
	    }
		return true;
	}
	
	/**
	 * Take the given instance and see if the corresponding instance
	 * in the database is older.  If there is no corresponding instance
	 * in the database, returns true.
	 * 
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	public boolean isInstanceNewerThanInDb(GKInstance instance) {
		if (instance==null)
			return true;
		
		Long dbId = instance.getDBID();
		
		// If only a local instance exists...
		if (dbId.longValue()<0)
			return true;
		
		try {
			GKInstance dbInstance = fetchInstance(dbId);
			
			// This should never happen...
			if (dbInstance==null)
				return true;
			
			List modifiedValues = instance.getAttributeValuesList("modified");
			List dbModifiedValues = dbInstance.getAttributeValuesList("modified");
			
			if (modifiedValues==null) {
				System.err.println("No \" modified\" attribute was found in local instance!");
				return true;
			}
			
			if (dbModifiedValues==null) {
				System.err.println("No \" modified\" attribute was found in database instance!");
				return true;
			}
			
			int modifiedValueCount = modifiedValues.size();
			int dbModifiedValueCount = dbModifiedValues.size();
			
			// This is the crux of the biscuit
			if (modifiedValueCount<dbModifiedValueCount)
				return false;
		} catch (Exception e) {
			System.err.println("Woops, something untoward occurred while trying to access the database");
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * Check if there exists a GKInstance object with the specified DB_ID
	 * in the database. This method will query the database directly. No 
	 * InstanceCache is not used.
	 * @param dbID
	 * @return true if existing
	 */
	public boolean exist(Long dbID) {
		SchemaClass root = ((GKSchema)schema).getRootClass();
		boolean rtn = false;
		Statement stat = null;
		ResultSet resultSet = null;
		try {
			stat = getConnection().createStatement();
			resultSet = stat.executeQuery("SELECT DB_ID FROM " + root.getName() + " WHERE DB_ID = " + dbID);
			if (resultSet.next()) 
				rtn = true;
		}
		catch(SQLException e) {
			System.err.println("MySQLAdaptor.exist(): " + e);
			e.printStackTrace();
		}
		finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				}
				catch(SQLException e) {} // Ignore it
			}
			if (stat != null) {
				try {
					stat.close();
				}
				catch(SQLException e) {} // Ignore it
			}
		}
		return rtn;
	}
	
	/**
	 * 
	 * @return true if all tables in the db have type 'InnoDB' and hence they all support
	 * transactions.
	 */
	public boolean supportsTransactions() throws SQLException {
	    if (! supportsTransactionsIsSet) {
	        Statement st;
	        ResultSet rs;
	        Set typeSet = new HashSet();
	        st = getConnection().createStatement();
	        rs = st.executeQuery("SHOW TABLE STATUS");
	        while (rs.next()) {
	            typeSet.add(rs.getString(2));
	        }
	        if ((typeSet.size() == 1) && (((String) typeSet.iterator().next()).matches("InnoDB"))) {
	            supportsTransactions = true;
	        }
	        supportsTransactionsIsSet = true;
	    }
		return supportsTransactions;
	}

	public Long txStoreInstance(GKInstance instance) throws Exception {
		return txStoreInstance(instance, false);
	}
	
	/**
	 * storeInstance wrapped in transaction. Instances referred by the instance
	 * passed into this function are stored in the same transaction.
	 * @param instance
	 * @param forceStore
	 * @return Long dbID
	 * @throws Exception
	 */
	public Long txStoreInstance(GKInstance instance, boolean forceStore) throws Exception {
		startTransaction();
		Long dbID = null;
		try {
			dbID = storeInstance(instance, forceStore);
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}
		return dbID;
	}
	
	/**
	 * updateInstance wrapped in transaction. Unstored instances referred
	 *  by the instance being updated are stored in the same transaction.
	 * @param instance
	 * @throws Exception
	 */
	public void txUpdateInstance(GKInstance instance) throws Exception {
		startTransaction();
		try {
			updateInstance(instance);
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}	
	}

	/**
	 * updateInstanceAttribute wrapped in transaction. Unstored instances referred
	 * by the instance attribute being updated are stored in the same transaction.
	 * @param instance
	 * @param attributeName
	 * @throws Exception
	 */
	public void txUpdateInstanceAttribute (GKInstance instance, String attributeName) throws Exception {
		startTransaction();
		try {
			updateInstanceAttribute(instance, attributeName);
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}
	}

	public void txUpdateInstanceAttribute (GKInstance instance, SchemaAttribute attribute) throws Exception {
		txUpdateInstanceAttribute(instance, attribute.getName());
	}
	
	/**
	 * deleteInstance wrapped in transaction. Instances referring to the instance
	 * being deleted are update in the same transaction.
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	public void txDeleteInstance(GKInstance instance) throws Exception {
		startTransaction();
		try {
			deleteInstance(instance);
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}
	}

	/**
	 * deleteByDBID wrapped in transaction. Instances referring to the instance
	 * being deleted are update in the same transaction.
	 * @param dbID
	 * @return
	 * @throws Exception
	 */
	public void txDeleteByDBID(Long dbID) throws Exception {
		startTransaction();
		try {
			deleteByDBID(dbID);
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}
	}
	
	/**
	 * IMPORTANT: this method updates only those instances in the given collection
	 * and not those referred but not contained in the collection. Storage works for both.
	 * @param instances
	 */
	public void txStoreOrUpdate(Collection instances) throws Exception {
		startTransaction();
		try {
			for (Iterator ii = instances.iterator(); ii.hasNext();) {
				GKInstance i = (GKInstance) ii.next();
				if (i.getDBID() == null) {
					storeInstance(i);
				} else {
					updateInstance(i);
				}
			}
			commit();
		} catch (Exception e) {
			rollback();
			throw(e);
		}
	}
	
	/**
	 * IMPORTANT: this method updates only those instances in the given collection
	 * and not those referred but not contained in the collection. Storage works for both.
	 * @param instances
	 */
	public void storeOrUpdate(Collection instances) throws Exception {
		try {
			for (Iterator ii = instances.iterator(); ii.hasNext();) {
				GKInstance i = (GKInstance) ii.next();
				if (i.getDBID() == null) {
					storeInstance(i);
				} else {
					updateInstance(i);
				}
			}
		} catch (Exception e) {
			throw(e);
		}
	}
	
	public void startTransaction() throws SQLException, TransactionsNotSupportedException {
		if (! supportsTransactions()) {
			throw(new TransactionsNotSupportedException(this));
		}
		getConnection().setAutoCommit(false);
		Statement st = getConnection().createStatement();
		st.executeUpdate("START TRANSACTION");
		inTransaction = true;
	}
	
	public void commit() throws SQLException {
		Statement st = getConnection().createStatement();
		st.executeUpdate("COMMIT");
		inTransaction = false;
		getConnection().setAutoCommit(true);
	}
	
	public void rollback() throws SQLException {
		Statement st = getConnection().createStatement();
		st.executeUpdate("ROLLBACK");
		inTransaction = false;
		getConnection().setAutoCommit(true);
	}
	
	public boolean isInTransaction() {
		return inTransaction;
	}

	private List makeAqrs(GKInstance instance) throws Exception {
		List aqrList = new ArrayList();
		GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
		Collection attColl =
			cls.getDefiningAttributes(SchemaAttribute.ALL_DEFINING);
		//System.out.println("makeAqrs\t" + instance + "\t" + attColl);
		if (attColl != null) {
			for (Iterator ai = attColl.iterator(); ai.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
				List allList = null;
				if (att.isOriginMuliple()) {
					allList = makeMultiValueTypeALLaqrs(instance, att);
				} else {
					allList = makeSingleValueTypeALLaqrs(instance, att);
				}
				if (allList == null) {
					return null;
				} else {
					aqrList.addAll(allList);
				}
			}
		}
		attColl = cls.getDefiningAttributes(SchemaAttribute.ANY_DEFINING);
		if (attColl != null) {
			for (Iterator ai = attColl.iterator(); ai.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) ai.next();
				List anyList = makeTypeANYaqrs(instance, att);
				if (anyList == null) {
					return null;
				} else {
					aqrList.addAll(anyList);
				}
			}
		}
		//Check if all queries have operator 'IS NULL' in which case there's no point
		//in executing the query and hence return empty array.
		int c = 0;
		for (Iterator ali = aqrList.iterator(); ali.hasNext();) {
			AttributeQueryRequest aqr = (AttributeQueryRequest) ali.next();
			if (aqr.getOperator().equalsIgnoreCase("IS NULL"))
				c++;
		}
		if (c == aqrList.size())
			aqrList.clear();
		return aqrList;
	}

	private List makeSingleValueTypeALLaqrs(GKInstance instance, GKSchemaAttribute att) throws Exception {
		List aqrList = new ArrayList();
		List values = instance.getAttributeValuesList(att.getName());
		//System.out.println("makeTypeALLaqrs\t" + att + "\t" + values);
		if ((values == null) || (values.isEmpty())) {
			aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"IS NULL",null));
			return aqrList;
		}
		Set seen = new HashSet();
		if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
			for (Iterator vi = values.iterator(); vi.hasNext();) {
				GKInstance valIns = (GKInstance) vi.next();
				Object tmp = null;
				Long valInsDBID = null;
				if ((tmp = valIns.getDBID()) != null) {
					if (! seen.contains(tmp)) {
						seen.add(tmp);
						aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"=",tmp));
					}
				}
				//This is only relevant if, say, the extracted instances keep their
				//DB_ID as the value of respectively named attribute.
				else if ((tmp = valIns.getAttributeValueNoCheck(Schema.DB_ID_NAME)) != null) {
					if (! seen.contains(tmp)) {
						seen.add(tmp);
						aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"=",tmp));
					}
				}
				else if ((tmp = fetchIdenticalInstances(valIns)) != null) {
					Collection identicals = new ArrayList();
					for (Iterator ti = ((Set) tmp).iterator(); ti.hasNext();) {
						Object o2 = ti.next();
						if (! seen.contains(o2)) {
							seen.add(o2);
							identicals.add(o2);
						}
					}
					if (! identicals.isEmpty()) {
						aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"=",identicals));
					}
				} else {
					// no identical instance to <i>valIns</i>. Hence there can't be an identical
					// instance to <i>instance</i>.
					return null;
				}
			}
		} else {
			for (Iterator vi = values.iterator(); vi.hasNext();) {
				Object o = vi.next();
				if (! seen.contains(o)) {
					seen.add(o);
					aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"=",o));
				}						
			}
		}
		return aqrList;
	}

	private List makeMultiValueTypeALLaqrs(GKInstance instance, GKSchemaAttribute att) throws Exception {
		List aqrList = new ArrayList();
		List values = instance.getAttributeValuesList(att.getName());
		//System.out.println("makeTypeALLaqrs\t" + att + "\t" + values);
		if ((values == null) || (values.isEmpty())) {
			aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"IS NULL",null));
			return aqrList;
		}
		String rootClassName = ((GKSchema) schema).getRootClass().getName();
		Map counter = new HashMap();
		for (Iterator vi = values.iterator(); vi.hasNext();) {
			Object value = vi.next();
			Integer count;
			if ((count = (Integer) counter.get(value)) == null) {
				counter.put(value, new Integer(1));
			} else {
				counter.put(value, new Integer(count.intValue() + 1));
			}
		}
		if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
			for (Iterator vi = counter.keySet().iterator(); vi.hasNext();) {
				GKInstance valIns = (GKInstance) vi.next();
				Collection dbIDs = new ArrayList();
				Object tmp = null;
				Long valInsDBID = null;
				if ((valInsDBID = valIns.getDBID()) != null) {
					dbIDs.add(valInsDBID);
				}
				//This is only relevant if, say, the extracted instances keep their
				//DB_ID as the value of respectively named attribute.
				else if ((valInsDBID = (Long )valIns.getAttributeValueNoCheck(Schema.DB_ID_NAME)) != null) {
					dbIDs.add(valInsDBID);
				}
				else if ((tmp = fetchIdenticalInstances(valIns)) != null) {
					for (Iterator ti = ((Set) tmp).iterator(); ti.hasNext();) {
						GKInstance identicalIns = (GKInstance) ti.next();
						dbIDs.add(identicalIns.getDBID());
					}
				}
				if (dbIDs.isEmpty()) {
					return null;
				}
				Collection matchingDBIDs = fetchDBIDsByAttributeValueCount(att, dbIDs, (Integer) counter.get(valIns));
				if (matchingDBIDs == null) {
					return null;
				} else {
					aqrList.add(new AttributeQueryRequest(rootClassName, Schema.DB_ID_NAME,"=",matchingDBIDs));
				}
			}
		} else {
			for (Iterator vi = counter.keySet().iterator(); vi.hasNext();) {
				Object value = vi.next();
				Collection tmp = new ArrayList();
				tmp.add(value);
				Collection matchingDBIDs = fetchDBIDsByAttributeValueCount(att, tmp, (Integer) counter.get(value));
				if (matchingDBIDs == null) {
					return null;
				} else {
					aqrList.add(new AttributeQueryRequest(rootClassName, Schema.DB_ID_NAME,"=",matchingDBIDs));
				}
			}
		}
		return aqrList;
	}

	/**
	 * 
	 * @param instance
	 * @param att
	 * @return A "list" of one AttributeQueryRequest. Null indicates that no match can
	 * be found.
	 */
	private List makeTypeANYaqrs(GKInstance instance, GKSchemaAttribute att) throws Exception {
		List aqrList = new ArrayList();
		List values = instance.getAttributeValuesList(att.getName());
		if ((values == null) || (values.isEmpty())) {
			aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"IS NULL",null));
			return aqrList;
		}
		Set seen = new HashSet();
		boolean hasUnmatchedValues = false;
		if (att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
			for (Iterator vi = values.iterator(); vi.hasNext();) {
				GKInstance valIns = (GKInstance) vi.next();
				Object tmp = null;
				if ((tmp = valIns.getDBID()) != null) {
					if (! seen.contains(tmp)) {
						seen.add(tmp);
					}
				}
				//This is only relevant if, say, the extracted instances keep their
				//DB_ID as the value of respectively named attribute.
				else if ((tmp = valIns.getAttributeValueNoCheck(Schema.DB_ID_NAME)) != null) {
					if (! seen.contains(tmp)) {
						seen.add(tmp);
					}
				}
				else if ((tmp = fetchIdenticalInstances(valIns)) != null) {
					for (Iterator ti = ((Set) tmp).iterator(); ti.hasNext();) {
						Object o2 = ti.next();
						if (! seen.contains(o2)) {
							seen.add(o2);
						}
					}
				} else {
					hasUnmatchedValues = true;
				}
			}
		} else {
			for (Iterator vi = values.iterator(); vi.hasNext();) {
				Object o = vi.next();			
				if (! seen.contains(o)) {
					seen.add(o);
				}						
			}
		}
		if (! seen.isEmpty()) {
			aqrList.add(new AttributeQueryRequest(instance.getSchemClass(),att,"=",seen));
		} else if (hasUnmatchedValues) {
			aqrList = null;
		}
		return aqrList;
	}

	/**
	 * Fetchies a Set of instances from db which look idential to the given instace
	 * based on the values of defining attributes.
	 * The method is recursive i.e. if a value instance of the given instance does
	 * not have DB_ID (i.e. it hasn't been stored in db yet) fetchIdenticalInstance is
	 * called on this instance. DB_IDs of identicals to the latter instance are then used
	 * to figure out if the given instance is identical so something else.
	 * @param instance
	 * @return
	 * @throws Exception
	 */
	public Set fetchIdenticalInstances(GKInstance instance) throws Exception {
		Set identicals = null;
		if (((GKSchemaClass) instance.getSchemClass()).getDefiningAttributes() == null) {
			if (debug) System.out.println(instance + "\tno defining attributes.");
			return null;
		}
		List aqrList = makeAqrs(instance);
		if (aqrList == null) {
			if (debug) System.out.println(instance + "\tno matching instances due to unmatching attribute value(s).");
		} else if (aqrList.isEmpty()) {
			if (debug) System.out.println(instance + "\tno defining attribute values.");
		} else {
			identicals = fetchInstance(aqrList);
			identicals.remove(instance);
			identicals = grepReasonablyIdenticalInstances(instance, identicals);
			if (identicals.isEmpty()) identicals = null;
			if (debug) System.out.println(instance + "\t" + identicals);
		}
		return identicals;
	}
	
	/**
	 * A method for weeding out instances retrieved as identicals but which are not.
	 * E.g. a Complex with comonents (A:A:B:B) would also fetch a Complex with 
	 * components (A:A:B:B:C) as an identical instance (although the reverse is not true).
	 * As I don't kwno how to change the sql in a way that this wouldn't be a case it has
	 * to be sorted out programmatically.
	 * @param instance
	 * @param identicals
	 * @return
	 * @throws Exception
	 */
	private Set grepReasonablyIdenticalInstances(GKInstance instance, Set identicals) throws Exception {
		Set out = new HashSet();
		for (Iterator ii = identicals.iterator(); ii.hasNext();) {
			GKInstance identicalInDb = (GKInstance) ii.next();
			if (areReasonablyIdentical(instance,identicalInDb)) out.add(identicalInDb);
		}
		return out;
	}
		
	/**
	 * Returns true if the specified instances have equal number of values in all
	 * of their type ALL defining attributes or if there are no type ALL defining
	 * attributes.
	 * @param instance
	 * @param inDb
	 * @return
	 * @throws Exception
	 */
	private boolean areReasonablyIdentical(GKInstance instance, GKInstance inDb) throws Exception {
		GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
		Collection attColl =
			cls.getDefiningAttributes(SchemaAttribute.ALL_DEFINING);
		if (attColl != null) {
			for (Iterator dai = attColl.iterator(); dai.hasNext();) {
				SchemaAttribute att = (SchemaAttribute) dai.next();
				String attName = att.getName();
				List vals1 = instance.getAttributeValuesList(attName);
				int count1 = 0;
				if (vals1 != null) count1 = vals1.size();
				List vals2 = inDb.getAttributeValuesList(attName);
				int count2 = 0;
				if (vals2 != null) count2 = vals2.size();
				if (count1 != count2) return false;
			}
		}
		return true;
	}

	/**
	 * This method fetches DB_IDs of instances which have given number
	 * (as specified by <i>count</i>) of values (as specified in <i>values</i>)
	 * as values of the attribute <i>att</i>.
	 * E.g. if att is Complex.hasComponent, count =2 and values = [33,44] then
	 * the result will be a collection of DB_IDs of instances like:
	 * hasComponent(33,44), hasComponent(33,33), hasComponent(33,44,55) etc, but not
	 * hasComponent(33,33,44), hasComponent(33,33,33) or hasComponent(33,55).
	 * I know, this is a rather horrible explanation ;-).
	 * @param att
	 * @param values - Collection of DB_IDs or non-instance attribute values.
	 * @param count - number of times the given values must occur as the values
	 * of the given attribute.
	 * @return Collection of (Long) DB_IDs
 	 * @throws Exception
	 */
	private Collection fetchDBIDsByAttributeValueCount(
		GKSchemaAttribute att,
		Collection values,
		Integer count)
		throws Exception {
		SchemaClass origin = att.getOrigin();
		if (!att.isOriginMuliple()) {
			throw (
				new Exception(
					"Attribute "
						+ att
						+ " is a single-value attribute and hence query by value count does not make sense."));
		}
		if (values.isEmpty()) {
			throw(new Exception("The Collection of values is empty!"));
		} 
		String attName = att.getName();
		StringBuffer sb =
			new StringBuffer(
				"SELECT "
					+ Schema.DB_ID_NAME
					+ ", COUNT("
					+ attName
					+ ") AS N FROM "
					+ origin.getName()
					+ "_2_"
					+ attName
					+ " WHERE "
					+ attName);
		if (values.size() == 1) {
			sb.append("=?");
		} else {
			String[] tmp = new String[values.size()];
			Arrays.fill(tmp, "?");
			sb.append(" IN(" + StringUtils.join(",",Arrays.asList(tmp)) + ")");
		}
		sb.append(" GROUP BY " + Schema.DB_ID_NAME);
		sb.append(" HAVING N=?");
		PreparedStatement ps = getConnection().prepareStatement(sb.toString());
		if (debug) System.out.println(sb.toString());
		int index = 1;
		for (Iterator i = values.iterator(); i.hasNext(); ) {
			Object value = i.next();
			ps.setObject(index, value);
			index++;
			if (debug) System.out.println("\t" + value);
		}
		ps.setObject(index, count);
		if (debug) System.out.println("\t" + count);
		ResultSet rs = ps.executeQuery();
		Collection out = new ArrayList();
		Long dbId = null;
		while (rs.next()) {
			out.add(new Long(rs.getLong(1)));
		}
		if (out.isEmpty()) {
			return null;
		} else {
			return out;
		}
	}
	
	/**
	 * Method which runs teh query as specifies by queryString and bindValues.
	 * The latter can be null, i.e. if you don't have any bind values.
	 * @param queryString
	 * @param bindValues
	 * @return ResultSet containing the query results
	 * @throws SQLException
	 */
	public ResultSet executeQuery (String queryString, List bindValues) throws SQLException {
		if (debug) System.out.println(queryString);
		PreparedStatement ps = getConnection().prepareStatement(queryString);
		if (bindValues != null) {
			for (int i = 0; i < bindValues.size(); i++) {
				Object value = bindValues.get(i);
				if (debug) System.out.println("\t" + value);
				ps.setObject(i + 1, value);
			}
		}
		ResultSet rs = ps.executeQuery();
		return rs;
	}
	
	/**
	 * Finds the largest DB_ID currently in the database.  Returns
	 * -1 if something went wrong during execution.
	 * 
	 * @return max DB_ID
	 */
	public long fetchMaxDbId() {
		long dbId = (-1);
		Statement stat = null;
		ResultSet resultSet = null;
		try {
			stat = getConnection().createStatement();
			resultSet = stat.executeQuery("SELECT MAX(DB_ID) FROM DatabaseObject");
			if (resultSet.next())
				dbId = resultSet.getLong(1);
		}
		catch(SQLException e) {
			System.err.println("MySQLAdaptor.exist(): " + e);
			e.printStackTrace();
		}
		finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				}
				catch(SQLException e) {} // Ignore it
			}
			if (stat != null) {
				try {
					stat.close();
				}
				catch(SQLException e) {} // Ignore it
			}
		}
		return dbId;
	}
}
