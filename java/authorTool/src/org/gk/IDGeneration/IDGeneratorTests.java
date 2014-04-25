/*
 * Created on Apr 10, 2006
 */
package org.gk.IDGeneration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;

/** 
 *  Stores the results of all known tests.
 * @author croft
 */
public class IDGeneratorTests {
	private Map tests;
	
	public IDGeneratorTests() {
		tests = new HashMap();
		
		// Add known tests
		createTest(IDGeneratorTest.ST_IDS_INSERTED);
		createTest(IDGeneratorTest.ST_IDS_EXIST);
		createTest(IDGeneratorTest.NO_PREVIOUS_DB_ID);
		createTest(IDGeneratorTest.BIOLOGICAL_MEANING_CHANGED);
		createTest(IDGeneratorTest.ID_IN_OLD_RELEASE);
		createTest(IDGeneratorTest.MISSING_STABLE_ID);
		createTest(IDGeneratorTest.COULD_NOT_INC_ST_ID);
		createTest(IDGeneratorTest.DISCREPANCY_BETWEEN_IDDB_AND_PREVRELEASE);
		createTest(IDGeneratorTest.MULTIPLE_STABLE_IDS_FOR_SINGLE_DB_ID);
	}
	
	public void setCurrentDba(MySQLAdaptor currentDba) {		
		IDGeneratorTest.setCurrentDba(currentDba);		
	}
	
	/** 
	 *  Create a new test and add it to the list
	 */
	private void createTest(String name) {
		IDGeneratorTest test = new IDGeneratorTest(name);
		tests.put(name, test);
	}
	
	public Map getTests() {
		return tests;
	}

	/**
	 * Given an instance and the name of a test, add the instance
	 * to the test's list of failing instances.
	 * 
	 * @param name
	 * @param instance
	 */
	public void addInstance(String name, GKInstance instance) {
		IDGeneratorTest test = (IDGeneratorTest)tests.get(name);
		test.addInstance(instance);
	}
	
	public int size() {
		return tests.size();
	}
	
	public String toString() {
		String out = "";
		String name;
		IDGeneratorTest test;
		for (Iterator itc = tests.keySet().iterator(); itc.hasNext();) {
			name = (String)itc.next();
			test = (IDGeneratorTest)tests.get(name);
			
			out += name + "\n" + test.toString() + "\n\n";
		}
		
		return out;
	}
}