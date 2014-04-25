/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;

/** 
 *  Stores the results of a test are stored internally, which can
 *  be retrieved with getFailingInstances.
 * @author croft
 */
public class IDGeneratorTest {
	public static String MULTIPLE_STABLE_IDS_FOR_SINGLE_DB_ID = "Multiple stable IDs exist for the instance";
	public static String DISCREPANCY_BETWEEN_IDDB_AND_PREVRELEASE = "Discrepancy between identifier in stable ID database and previous release database";
	public static String ST_IDS_INSERTED = "Total ST_IDs inserted"; // not strictly speaking a failure, but worth logging
	public static String ST_IDS_EXIST = "ST_IDs already exist";
	public static String NO_PREVIOUS_DB_ID = "No previous instance with the same DB_ID";
	public static String BIOLOGICAL_MEANING_CHANGED = "The biological meaning of the instance has changed";
	public static String ID_IN_OLD_RELEASE = "The DB_ID was found in a release not chosen by the user";
	public static String MISSING_STABLE_ID = "Stable identifier cannot be found in identifier database";
	public static String COULD_NOT_INC_ST_ID = "Could not increment the most recent stable ID counter";

	/** 
	 *  A descriptive name for the test
	 */
	private String name;
	
	/** 
	 *  A list of Instance DB_IDs that failed the test.
	 */
	private List dbIds;
	
	private static MySQLAdaptor currentDba;
	
	public IDGeneratorTest(String name) {
		this.name = name;
		dbIds = new ArrayList();
	}
	
	public static void setCurrentDba(MySQLAdaptor currentDba) {
		IDGeneratorTest.currentDba = currentDba;
	}

	public String getName() {
		return name;
	}
	
	/** 
	 *  Returns a list of DB_IDs for the instances for which
	 *  the test failed.
	 */
	public List getDbIds() {
		return dbIds;
	}
	
	/**
	 * Fetches an instance from the current release, based
	 * on the DB_ID.
	 * 
	 * @param dbId
	 * @return
	 */
	public static GKInstance fetchInstance(Long dbId) {
		if (dbId==null)
			return null;
		
		if (currentDba==null) {
			System.err.println("IDGeneratorTest.getInstance: WARNING - dba is null!!!!");
			return null;
		}
		
		GKInstance instance = null;
		try {
			instance = currentDba.fetchInstance(dbId);
		} catch (Exception e) {
			System.err.println("IDGeneratorTest.getInstance: WARNING - could not get instance with DB_ID: " + dbId);
			e.printStackTrace();
		}
		
		return instance;
	}
	
	/** 
	 *  Adds an instance for which the test failed.
	 */
	public void addInstance(GKInstance instance) {
		if (dbIds.indexOf(instance.getDBID())<0)
			dbIds.add(instance.getDBID());
	}
	
	public int getInstanceCount() {
		return dbIds.size();
	}
	
	/** 
	 *  Extracts the curators from the list of failing instances
	 *  and returns them as a list of "InstanceEdit" instances.
	 */
	public List getCurators() {
		List instanceEditList;
		Iterator it2;
		Iterator it3;
		GKInstance instanceEdit;
		GKInstance author;
		List authorList;
		List curators = new ArrayList();
		GKInstance instance;
		Long dbId;
		for (Iterator it = dbIds.iterator(); it.hasNext();) {
			dbId = (Long)it.next();
			instance = fetchInstance(dbId);
			if (instance==null) {
				if (dbId==null)
					System.err.println("IDGeneratorTest.getCurators: WARNING - dbId is null!!");
				else
					System.err.println("IDGeneratorTest.getCurators: WARNING - instance is null for dbId: " + dbId);
				continue;
			}
			try {
				instanceEditList = IDGenerationUtils.getInstanceEdits(instance);
				for (it2 = instanceEditList.iterator(); it2.hasNext();) {
					instanceEdit = (GKInstance)it2.next();
					authorList = null;
					try {
						authorList = instanceEdit.getAttributeValuesList("author");
					} catch (InvalidAttributeException e) {
					}
					if (authorList!=null) {
						for (it3 = authorList.iterator(); it3.hasNext();) {
							author = (GKInstance)it3.next();
							if (curators.indexOf(author)<0)
								curators.add(author);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("IDGeneratorTest.getCurators: could not get curator information");
				e.printStackTrace();
			}
		}
		
		return curators;
	}
	
	public int getCuratorCount() {
		return getCurators().size();
	}
	
	public String getInstanceDB_IDs() {
		String out = "";
		Long dbId;
		int instanceCounter = 0;
		for (Iterator it = dbIds.iterator(); it.hasNext();) {
			dbId = (Long)it.next();
			if (instanceCounter%5 == 0)
				out += "\n    ";
			out += dbId.toString() + " ";
			instanceCounter++;
		}
		
		return out;
	}
	
	public String toString() {
		String out = "";
		
		out += "    Instance count=" + getInstanceCount() + "\n";
		out += "    Curator count=" + getCuratorCount() + "\n";
		out += "    Instance DB_IDs=" + getInstanceDB_IDs() + "\n";
		
		return out;
	}
}