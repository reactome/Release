/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidClassException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/** 
 *  This provides an API to the identifier database, allowing
 *  various operations to be performed that are useful in stable
 *  ID generation.
 *  
 *  To get a completely new and fresh StableIdentifier instance
 *  that has never been used by anybody before, do the following:
 *  
 *  incrementMostRecentStableIdentifier();
 *  newStableIdentifier = getMostRecentStableIdentifierFromState();
 *  
 *  TODO: this class could probably be made Static, because it
 *  is essentially stateless.
 *  
 * @author croft
 */
public class IdentifierDatabase {
	private static MySQLAdaptor dba = null; // DBA for identifier database
	private static String releaseTable = null;
	private static String releaseColumn = null;
	private Map releaseDbas = null;  // DBAs for release databases (cache)
	public static String SLICE = "sliceDbParams"; // attribute name
	public static String RELEASE = "releaseDbParams"; // attribute name
	private String dbParamsAttribute = SLICE;
	private Map releaseNumToDbaHash;
	private static String IDERROR = "error";
	private String username; // alternative to value in identifier database
	private String password; // alternative to value in identifier database
	private static String defaultProjectName = "Reactome";
	
	public IdentifierDatabase() {
		releaseNumToDbaHash = null;
		username = null;
		password = null;
	}
	
	/**
	 * Set to either SLICE or RELEASE.  This decides whether database
	 * parameters for slice or release will be used.
	 * 
	 * @param dbParamsAttribute
	 */
	public void setDbParamsAttribute(String dbParamsAttribute) {
		this.dbParamsAttribute = dbParamsAttribute;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Creates a new, blank Release instance, inserts it into
	 * the identifier database and also returns it.
	 * @return
	 */
	public static GKInstance createBlankRelease() {
		MySQLAdaptor dba = getDba();
		Schema schema = dba.getSchema();
		
		// Create a new instance of type Release
		GKInstance release = new GKInstance();
		SchemaClass releaseSchemaClass = schema.getClassByName(getReleaseTable());
		release.setSchemaClass(releaseSchemaClass);
		release.setDbAdaptor(dba);
		
		try {
			release.setAttributeValue("_displayName", "");
		} catch (Exception e) {
			System.err.print("addRelease: er sorry mate looks like attribute val couldnt be set");
			if (releaseSchemaClass==null)
				System.err.print(", releaseSchemaClass==null");
			System.err.println("");
			e.printStackTrace();
		}

		return release;
	}
	
	/**
	 * Gets the State instance (of which there
	 * should be either 0 or 1 occurrence in the database).
	 * @return
	 */
	public static GKInstance getState() {
		GKInstance state = null;
		
		MySQLAdaptor dba = getDba();
		if (dba==null)
			return state;
		
		try {
			List states = new ArrayList(dba.fetchInstancesByClass("State"));
			if (states.size()>0) {
				if (states.size()>1)
					System.err.println("IdentifierDatabase.getState: WARNING - more than one State object found in database - using first one!");
				state = (GKInstance)states.get(0);
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getState: something went wrong when trying to get State instances");
			e.printStackTrace();
		}
		
		return state;
	}
	
	/**
	 * Does a getState, and then extracts the StableIdentifier
	 * instance from the "mostRecentStableIdentifier" slot and returns it. 
	 * 
	 * @return
	 */
	public static GKInstance getMostRecentStableIdentifierFromState() {
		GKInstance mostRecentStableIdentifier = null;
		
		GKInstance state = getState();
		
		if (state!=null)
			try {
				mostRecentStableIdentifier = (GKInstance)state.getAttributeValue("mostRecentStableIdentifier");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getMostRecentStableIdentifierFromState: something went wrong when trying to get StableIdentifier instances");
				e.printStackTrace();
			}
		
		return mostRecentStableIdentifier;
		
	}
	
	/**
	 * Gets a StableIdentifier instance based on the supplied ID. If one
	 * or more instances already exist in the database, then the first of
	 * these will be retrieved.  If no StableIdentifier instances for the
	 * given ID exists, null will be returned.
	 * The version number of the new instance will be left empty.
	 * @param identifierString
	 * @return
	 */
	public static GKInstance getStableIdentifierInstance(String identifierString, boolean createNewIfNotInDb) {
		if (identifierString==null || identifierString.equals("")) {
			System.err.println("IdentifierDatabase.getStableIdentifierInstance: no identifierString available!");
			return null;
		}
		MySQLAdaptor dba = getDba();
		if (dba==null) {
			System.err.println("IdentifierDatabase.getStableIdentifierInstance: no dba available!");
			return null;
		}

		// Try to retrieve a StableIdentifier instance for the given
		// stableIdentifierString.  If one doesn't exist, then create
		// a new one.
		GKInstance stableIdentifier = null;
		try {
			List query = new ArrayList();
			// Find Identifier instance containing numerical stub of stable ID
			Collection stableIdentifiers = dba.fetchInstanceByAttribute("StableIdentifier", "identifierString", "=", identifierString);
			if (stableIdentifiers==null || stableIdentifiers.size()==0) {
				if (createNewIfNotInDb) {
					// Create new one
					stableIdentifier = new GKInstance();
					stableIdentifier.setSchemaClass(dba.getSchema().getClassByName("StableIdentifier"));
					stableIdentifier.setDbAdaptor(dba);
					createIdentifierFromIdentifierString(stableIdentifier, identifierString);

					// Do this just in case the value of identifierString is larger
					// than the numerical stub of the stable identifier stored in
					// the State table.
					updateStateIfNecessary(stableIdentifier);

					dba.storeInstance(stableIdentifier);
				}
			} else {
				// Assume that there is only a single pre-existing one -
				// there should only be one single StableIdentifier
				// instance per stable ID.
				stableIdentifier =(GKInstance)stableIdentifiers.toArray()[0];
				
				if (stableIdentifiers.size() > 1)
					System.err.println("IdentifierDatabase.getStableIdentifierInstance: WARNING - stableIdentifiers.size()=" + stableIdentifiers.size());
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierInstance: WARNING - something nasty happened while trying to get or generate a StableIdentifier instance for the identifier database");
			e.printStackTrace(System.err);
		}

		return stableIdentifier;
	}
	
	/**
	 * Given the numerical stub for a stable identifier, create an Identifier
	 * instance containing this stub and the string version of the stable
	 * identifier.
	 * 
	 * @param numericalStub
	 * @return
	 */
	private static void createIdentifierFromNumericalStub(GKInstance stableIdentifier, String numericalStub) {
		createIdentifier(stableIdentifier, new Integer(numericalStub), "REACT_" + numericalStub);
	}
	
	private static void createIdentifierFromIdentifierString(GKInstance stableIdentifier, String identifierString) {
		Integer integerNumericalStub = getNumericalStubFromIdentifier(identifierString);
		createIdentifier(stableIdentifier, integerNumericalStub, identifierString);
	}
	
	private static void createIdentifier(GKInstance stableIdentifier, Integer numericalStub, String identifierString) {
		try {
			stableIdentifier.setAttributeValue("numericalStub", numericalStub);
			stableIdentifier.setAttributeValue("identifierString", identifierString);
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.createIdentifierFromNumericalStub: something nasty happened while trying to get or generate an Identifier instance for the identifier database");
			e.printStackTrace();
		}
	}
	
	public static Integer getNumericalStubFromIdentifier(String identifierString) {
		String numericalStub = identifierString.substring(6);
		Integer integerNumericalStub = new Integer("-1");
		try {
			integerNumericalStub = new Integer(numericalStub);
		} catch (NumberFormatException e) {
			System.err.println("IdentifierDatabase.createIdentifierFromIdentifierString: numericalStub=" + numericalStub + " is not a valid number!!");
			e.printStackTrace();
		}
		
		return integerNumericalStub;
	}
	
	/**
	 * If the identifier database contains a State instance,
	 * the stable ID will be extracted, incremented, and put back.  If no
	 * such instance exists, a fresh new instance will be created.
	 * 
	 * If a problem occurred while doing the increment which resulted in
	 * no increment taking place and no new StableIdentifier instance
	 * being inserted into the database, then this method will return
	 * false.  If everything went OK, it will return true.
	 *
	 */
	public boolean incrementMostRecentStableIdentifier() {
		GKInstance state = getState();
		
		MySQLAdaptor dba = getDba();
		if (dba==null)
			return false;
		
		GKInstance mostRecentStableIdentifier = null;
		
		// A null value most probably means that the identifier database
		// has been freshly created, so create a very first
		// State instance
		boolean mostRecentStableIdentifierAlreadyExists = false;
		if (state==null) {
			state = new GKInstance();
			state.setSchemaClass(dba.getSchema().getClassByName("State"));
			state.setDbAdaptor(dba);
			
			mostRecentStableIdentifier = new GKInstance();
			mostRecentStableIdentifier.setSchemaClass(dba.getSchema().getClassByName("StableIdentifier"));
			mostRecentStableIdentifier.setDbAdaptor(dba);
			
			createIdentifierFromNumericalStub(mostRecentStableIdentifier,"0"); // The very first stable ID
			
			try {
				state.setAttributeValue("mostRecentStableIdentifier", mostRecentStableIdentifier);
				
				dba.storeInstance(mostRecentStableIdentifier);
				dba.storeInstance(state);
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.incrementMostRecentStableIdentifier: not able to create State");
				e.printStackTrace();
				return false;
			}
			
			mostRecentStableIdentifierAlreadyExists = true;
		}
		
		// Pull mostRecentStableIdentifier from State instance, increment its
		// stable ID, insert a new mostRecentStableIdentifier containing the
		// incrementedstable ID.
		int stableIdentifierInt = (-1); // This value should never be used
		try {
			if (!mostRecentStableIdentifierAlreadyExists) {
				mostRecentStableIdentifier = (GKInstance)state.getAttributeValue("mostRecentStableIdentifier");
//				dba.loadInstanceAttributeValues(mostRecentStableIdentifier); // TODO: we really only need numericalStub
			}
			if (mostRecentStableIdentifier==null) {
				System.err.println("IdentifierDatabase.incrementMostRecentStableIdentifier: stableIdentifierInstanceis null!!");
				return false;
			} else {
				Object value = mostRecentStableIdentifier.getAttributeValue("numericalStub");
				if (value == null) {
					System.err.println("IdentifierDatabase.incrementMostRecentStableIdentifier: value is null!!");
					return false;
				} else {
					try {
						stableIdentifierInt = ((Integer) value).intValue();
						stableIdentifierInt++;
					} catch (NumberFormatException e) {
						System.err.println("IdentifierDatabase.incrementMostRecentStableIdentifier: testStableIdentifier has a strange format, should be an integer but is: "+ value);
						e.printStackTrace();
						return false;
					}
				}
			}
			
			// Create a new StableIdentifier instance containing the
			// updated stableIdentifier and stash it in the database.
			mostRecentStableIdentifier = new GKInstance();
			mostRecentStableIdentifier.setSchemaClass(dba.getSchema().getClassByName("StableIdentifier"));
			mostRecentStableIdentifier.setDbAdaptor(dba);
			createIdentifierFromNumericalStub(mostRecentStableIdentifier, (new Integer(stableIdentifierInt)).toString());
			mostRecentStableIdentifier.setAttributeValue("dateTime", GKApplicationUtilities.getDateTime());

			dba.storeInstance(mostRecentStableIdentifier);
			
			// Overwrite the existing StableIdentifier instance with
			// the new one in the "most recent" instance.
			state.setAttributeValue("mostRecentStableIdentifier", mostRecentStableIdentifier);

			// Save the change to the DB
			dba.updateInstanceAttribute(state, "mostRecentStableIdentifier");
//			dba.updateInstance(state);
			
			
			
			
			
			// This is here as a test only
			state = getState();
			mostRecentStableIdentifier = (GKInstance)state.getAttributeValue("mostRecentStableIdentifier");
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.incrementMostRecentStableIdentifier: problem getting mostRecentStableIdentifier");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/** 
	 *  Returns a list of "StableIdentifier" instances.
	 */
	public static List getStableIdentifiers() {
		return getInstances("StableIdentifier");
	}
	
	/** 
	 *  Returns a list of instances of the given class from the identifier database.
	 */
	public static List getInstances(String instanceClass) {
		return getInstances(instanceClass, getDba());
	}
	
	/** 
	 *  Returns a list of instances of the given class from the given database.
	 *  Wraps fetchInstancesByClass to fix a little bug.
	 */
	public static List<GKInstance> getInstances(String instanceClass, MySQLAdaptor dba) {
		List<GKInstance> instances = new ArrayList<GKInstance>();
		
		if (dba==null)
			return instances;
		
		try {
			instances = new ArrayList<GKInstance>(dba.fetchInstancesByClass(instanceClass));
		} catch (InvalidClassException e) {
			// This exception gets thrown (wrongly) if there are no instances
			// of the given class.
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getInstances: something went wrong when trying to get instances");
			e.printStackTrace();
		}
		
		return instances;
	}
	
	/** 
	 *  Returns the highest numbered instance found in the
	 *  StableIdentifier table.
	 *  
	 *  If a problem arises during this method, or no StableIdentifier
	 *  instances were present, null will be returned.
	 */
	public static GKInstance getMaxIdentifierDatabaseStableIdentifier() {
		List stableIdentifiers = getStableIdentifiers();
		GKInstance stableIdentifier;
		GKInstance maxStableIdentifier = null;
		Integer numericalStub;
		int numericalStubInt;
		int maxNumericalStubInt = Integer.MIN_VALUE;
		for (Iterator ri = stableIdentifiers.iterator(); ri.hasNext();) {
			stableIdentifier = (GKInstance)ri.next();
			
			try {
				numericalStub = (Integer)stableIdentifier.getAttributeValue("numericalStub");
				numericalStubInt = numericalStub.intValue();
				
				if (numericalStubInt>maxNumericalStubInt) {
					maxNumericalStubInt = numericalStubInt;
					maxStableIdentifier = stableIdentifier;
				}
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getMaxIdentifierDatabaseStableIdentifier: something nasty happened while trying to get an Identifier instance for the identifier database");
				e.printStackTrace();
			}
		}
		
		return maxStableIdentifier;
	}
	
	/** 
	 *  Returns the highest numbered instance found in the
	 *  StableIdentifier table.
	 *  
	 *  If a problem arises during this method, or no StableIdentifier
	 *  instances were present, null will be returned.
	 */
	public static GKInstance getMaxReleaseStableIdentifier(MySQLAdaptor releaseDba) {
		List stableIdentifiers = getInstances("StableIdentifier", releaseDba);
		GKInstance stableIdentifier;
		GKInstance maxStableIdentifier = null;
		String identifier;
		Integer numericalStub;
		int numericalStubInt;
		int maxNumericalStubInt = Integer.MIN_VALUE;
		for (Iterator ri = stableIdentifiers.iterator(); ri.hasNext();) {
			stableIdentifier = (GKInstance)ri.next();
			
			try {
				identifier = (String)stableIdentifier.getAttributeValue("identifier");
				numericalStub = getNumericalStubFromIdentifier(identifier);
				numericalStubInt = numericalStub.intValue();
				
				if (numericalStubInt>maxNumericalStubInt) {
					maxNumericalStubInt = numericalStubInt;
					maxStableIdentifier = stableIdentifier;
				}
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getMaxReleaseStableIdentifier: something nasty happened while trying to get an Identifier instance for the identifier database");
				e.printStackTrace();
			}
		}
		
		return maxStableIdentifier;
	}
	
	/**
	 * Does a double-check on the most recent stable identifier
	 * stored in the State table, by comparing it's numerical
	 * stub with the highest numbered numerical stub in the
	 * StableIdentifier table.  If the latter is greater (it
	 * should be the same), then substitute it into the stable
	 * identifier stored in State.
	 * 
	 * This is a bit time-consuming but ensures that stable ID
	 * generation does not produce any duplicated stable IDs.
	 * 
	 * Really, this situation should never arise.
	 *
	 */
	public static void sanitizeMostRecentStableIdentifierFromState() {
		GKInstance maxStableIdentifier = getMaxIdentifierDatabaseStableIdentifier();

		updateStateIfNecessary(maxStableIdentifier);
	}
	
	/**
	 * Runs a sanitizeMostRecentStableIdentifierFromState(), then
	 * checks through the StableIdentifier instances in the given
	 * release and picks out the largest stable identifier.  Takes
	 * this ID and makes sure that the State table is updated if
	 * the known max stable ID is smaller than the one found in the
	 * release.
	 * @param releaseDba
	 */
	public static void sanitizeMostRecentStableIdentifierFromState(MySQLAdaptor releaseDba) {
		sanitizeMostRecentStableIdentifierFromState();
		
		GKInstance maxStableIdentifier = getMaxReleaseStableIdentifier(releaseDba);
		try {
			String identifier = (String)maxStableIdentifier.getAttributeValue("identifier");
			getStableIdentifierInstance(identifier, true);
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.sanitizeMostRecentStableIdentifierFromState: something nasty happened while trying to get an Identifier instance for the identifier database");
			e.printStackTrace();
		}
	}
	
	/**
	 * If the supplied stable identifier has a higher ID than the
	 * stable identifier stored in State, replace the one in State.
	 * 
	 * @param stableIdentifier
	 */
	public static void updateStateIfNecessary(GKInstance stableIdentifier) {
		GKInstance mostRecentStableIdentifierFromState = getMostRecentStableIdentifierFromState();
		
		try {
			Integer numericalStub = (Integer)stableIdentifier.getAttributeValue("numericalStub");
			Integer mostRecentNumericalStub = (Integer)mostRecentStableIdentifierFromState.getAttributeValue("numericalStub");
			int numericalStubInt = numericalStub.intValue();
			int mostRecentNumericalStubInt = mostRecentNumericalStub.intValue();
			
			if (mostRecentNumericalStubInt<numericalStubInt) {
				System.err.println("IdentifierDatabase.updateStateIfNecessary: WARNING - stable identifier stored in State (" + mostRecentNumericalStub + " is smaller than in StableIdentifier (" + numericalStub + ", correcting!");
				GKInstance state = getState();
				state.setAttributeValue("mostRecentStableIdentifier", stableIdentifier);
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.updateStateIfNecessary: something nasty happened while trying to get an Identifier instance for the identifier database");
			e.printStackTrace();
		}
	}
	
	/**
	 * Takes the supplied StableIdentifier instance and updates the appropriate
	 * hierarchy of instances under it to reflect the new version number.
	 * 
	 * @param stableIdentifier - a StableIdentifier instance from an identifier database
	 * @param stableVersionString
	 * @param currentReleaseNum
	 * @param currentProjectName
	 * @param testMode - if true, nothing will be inserted into database
	 */
	public void insertNewVersion(GKInstance stableIdentifier, String stableIdentifierVersionString, String currentReleaseNum, String currentProjectName, Long DB_ID, boolean testMode) {
		insertNewVersion(stableIdentifier, stableIdentifierVersionString, currentReleaseNum, currentProjectName, DB_ID, testMode, null);
	}
	
	/**
	 * Inserts test results into the database, storing by release number.
	 * @param releaseNum
	 * @param tests
	 */
	public void insertTests(String releaseNum, String projectName, IDGeneratorTests tests) {
		GKInstance release = getReleaseInstance(releaseNum, projectName);
        if (release==null)
        	return;
	
			Collection testList = tests.getTests().values();
			IDGeneratorTest test;
			for (Iterator ri = testList.iterator(); ri.hasNext();) {
				test = (IDGeneratorTest)ri.next();
				insertTest(release, test);
			}

	}
	
	/**
	 * Inserts test result into the database, storing by release number.
	 * @param releaseNum
	 * @param test
	 */
	public void insertTest(GKInstance release, IDGeneratorTest test) {
		String name = test.getName();
		int instanceCount = test.getInstanceCount();
		int curatorCount = test.getCuratorCount();
		List dbIds = test.getDbIds();
		
        try {
    		MySQLAdaptor dba = getDba();

        	List testResults = release.getAttributeValuesList("testResults");
        	GKInstance testResult = null;
        	
        	// Look to see if we already have an old version of this
        	// test in the database
        	boolean testAlreadyExists = true;
        	if (testResults!=null) {
        		String testResultName;
    			for (Iterator ri = testResults.iterator(); ri.hasNext();) {
    				testResult = (GKInstance)ri.next();
    				testResultName = (String)testResult.getAttributeValue("testName");
    				if (name.equals(testResultName))
    					break;
    			}
        	}
        	
        	// If this is the first time we have encountered this
        	// test, create a new instance for it
        	if (testResult==null) {
        		testAlreadyExists = false;
        		testResult = new GKInstance();
        		testResult.setSchemaClass(dba.getSchema().getClassByName("TestResults"));
//            	testResult.setDbAdaptor(dba);
        		testResult.setIsInflated(true);
        	}
        	
        	testResult.setAttributeValue("testName", name);
        	testResult.setAttributeValue("instanceCount", new Integer(instanceCount));
        	testResult.setAttributeValue("curatorCount", new Integer(curatorCount));
        	testResult.setAttributeValueNoCheck("instanceDB_IDs", dbIds);
        	
        	// Now store the information
        	if (testAlreadyExists) {
        		dba.updateInstanceAttribute(testResult, "testName");
        		dba.updateInstanceAttribute(testResult, "instanceCount");
        		dba.updateInstanceAttribute(testResult, "curatorCount");
        		dba.updateInstanceAttribute(testResult, "instanceDB_IDs");
        	} else
        		dba.storeInstance(testResult);
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.insertTest: WARNING - problem getting test results from release");
			e.printStackTrace();
		}		
	}
	
	/**
	 * Inserts test results into the database, storing by release number.
	 * @param releaseNum
	 * @param tests
	 */
	public void insertMaxDbId(String releaseNum, String projectName, long maxDbId) {
		if (releaseNum==null || releaseNum.equals("")) {
			System.err.println("IdentifierDatabase.insertMaxDbId: WARNING - releaseNum missing!");
			return;
		}
		if (projectName==null || projectName.equals("")) {
			System.err.println("IdentifierDatabase.insertMaxDbId: WARNING - projectName missing!");
			return;
		}
		
		GKInstance release = getReleaseInstance(releaseNum, projectName);
        if (release==null) {
			System.err.println("IdentifierDatabase.insertMaxDbId: WARNING - could not find a release corresponding to releaseNum=" + releaseNum);
        	return;
        }
	
        try {
			release.setAttributeValue("maxDB_ID", new Integer((new Long(maxDbId)).intValue()));
			dba.updateInstanceAttribute(release, "maxDB_ID");
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.insertMaxDbId: WARNING - problem inserting max DB_ID into release corresponding to releaseNum=" + releaseNum);
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with identifier and check to see if
	 * any of them have the version number in identifierVersion.
	 * If yes, return it, otherwise, return null.
	 **/
	public GKInstance getStableIdentifierVersion(String identifier, String identifierVersion) {
		GKInstance stableIdentifier = getStableIdentifierInstance(identifier, false);
		
		return getStableIdentifierVersion(stableIdentifier, identifierVersion);
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with stableIdentifier and check to see if
	 * any of them have the version number in identifierVersion.
	 * If yes, return it, otherwise, return null.
	 **/
	public GKInstance getStableIdentifierVersion(GKInstance stableIdentifier, String identifierVersion) {
		GKInstance stableIdentifierVersion = null;
		
		try {
			List stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
			GKInstance existingStableIdentifierVersion;
			String existingIdentifierVersion;
			for (Iterator ri = stableIdentifierVersions.iterator(); ri.hasNext();) {
				existingStableIdentifierVersion = (GKInstance)ri.next();
				existingIdentifierVersion = (String)existingStableIdentifierVersion.getAttributeValue("identifierVersion");
				if (existingIdentifierVersion.equals(identifierVersion)) {
					stableIdentifierVersion = existingStableIdentifierVersion;
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierVersion: WARNING - problem getting stuff from stable identifier database");
			e.printStackTrace();
		}
		
		return stableIdentifierVersion;
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with identifier, find the one associated with
	 * the given release number, and return it.
	 * If nothing could be found, return null.
	 **/
	public static GKInstance getStableIdentifierVersionForRelease(String identifier, String releaseNum, String projectName) {
		GKInstance stableIdentifier = getStableIdentifierInstance(identifier, false);
		
		GKInstance stableIdentifierVersion = null;
		
		try {
			List stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
			List releaseIds;
			GKInstance releaseId;
			GKInstance release;
			String existingReleaseNum;
			String existingProjectName;
			GKInstance existingStableIdentifierVersion;
			for (Iterator rv = stableIdentifierVersions.iterator(); rv.hasNext();) {
				existingStableIdentifierVersion = (GKInstance)rv.next();
				releaseIds = existingStableIdentifierVersion.getAttributeValuesList("releaseIds");
				for (Iterator rr = releaseIds.iterator(); rr.hasNext();) {
					releaseId = (GKInstance)rr.next();
					release = (GKInstance)releaseId.getAttributeValue(getReleaseColumn());
					existingReleaseNum = (String)release.getAttributeValue("num");
					existingProjectName = (String)((GKInstance)release.getAttributeValue("project")).getAttributeValue("name");
					if (existingReleaseNum.equals(releaseNum) && existingProjectName.equals(existingProjectName)) {
						stableIdentifierVersion = existingStableIdentifierVersion;
						break;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierVersion: WARNING - problem getting stuff from stable identifier database");
			e.printStackTrace();
		}
		
		return stableIdentifierVersion;
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with identifier, find the one associated with
	 * the given release number, and return a string representing the version
	 * number.
	 * If nothing could be found, return null.
	 **/
	public static String getStableIdentifierVersionForReleaseString(String identifier, String releaseNum, String projectName) {
		GKInstance stableIdentifierVersion = IdentifierDatabase.getStableIdentifierVersionForRelease(identifier, releaseNum, projectName);
		String identifierVersion = null;
		
		if (stableIdentifierVersion != null)
			try {
				identifierVersion = (String)stableIdentifierVersion.getAttributeValue("identifierVersion");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getStableIdentifierVersionString: WARNING - problem getting stuff from stable identifier database");
				e.printStackTrace();
			}
		
		return identifierVersion;
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with identifier, find the one with the highest
	 * version number, and return it.
	 * If nothing could be found, return null.
	 **/
	public static GKInstance getStableIdentifierVersion(String identifier) {
		GKInstance stableIdentifier = getStableIdentifierInstance(identifier, false);
		
		GKInstance stableIdentifierVersion = null;
		String identifierVersion = null;
		
		try {
			List stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
			GKInstance existingStableIdentifierVersion;
			String existingIdentifierVersion;
			for (Iterator ri = stableIdentifierVersions.iterator(); ri.hasNext();) {
				existingStableIdentifierVersion = (GKInstance)ri.next();
				existingIdentifierVersion = (String)existingStableIdentifierVersion.getAttributeValue("identifierVersion");
				if (identifierVersion==null || (new Integer(existingIdentifierVersion)).intValue() > (new Integer(identifierVersion)).intValue()) {
					identifierVersion = existingIdentifierVersion;
					stableIdentifierVersion = existingStableIdentifierVersion;
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierVersion: WARNING - problem getting stuff from stable identifier database");
			e.printStackTrace();
		}
		
		return stableIdentifierVersion;
	}
	
	/**
	 * 
	 * Go through all of the StableIdentifierVersion instances
	 * associated with identifier, find the one with the highest
	 * version number, and return a string representing the version
	 * number.
	 * If nothing could be found, return null.
	 **/
	public static String getStableIdentifierVersionString(String identifier) {
		GKInstance stableIdentifierVersion = IdentifierDatabase.getStableIdentifierVersion(identifier);
		String identifierVersion = null;
		
		if (stableIdentifierVersion != null)
			try {
				identifierVersion = (String)stableIdentifierVersion.getAttributeValue("identifierVersion");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getStableIdentifierVersionString: WARNING - problem getting stuff from stable identifier database");
				e.printStackTrace();
			}
		
		return identifierVersion;
	}
	
	/**
	 * Takes the supplied StableIdentifier instance and updates the appropriate
	 * hierarchy of instances under it to reflect the new version number.
	 * 
	 * @param stableIdentifier - a StableIdentifier instance from an identifier database
	 * @param stableVersionString
	 * @param currentReleaseNum
	 * @param testMode - if true, nothing will be inserted into database
	 * @param instanceBiologicalMeaning - contains info about changes in current instance
	 */
	public void insertNewVersion(GKInstance stableIdentifier, String stableIdentifierVersionString, String currentReleaseNum, String currentProjectName, Long DB_ID, boolean testMode, InstanceBiologicalMeaning instanceBiologicalMeaning) {
		if (stableIdentifier==null) {
        	System.err.println("IdentifierDatabase.insertNewVersion: WARNING - stableIdentifier is null!!  Aborting.");
        	return;
		}
		
		try {
			MySQLAdaptor dba = getDba();

			// We successfully inserted a new StableIdentifier instance,
			// meaning that there was a corresponding instance in the
			// release database, so the doNotRelease flag must have
			// been unset - make a note of that by unsetting it in the
			// identifier database too.
			stableIdentifier.setAttributeValue("doNotRelease", null);
			if (!testMode)
				dba.updateInstanceAttribute(stableIdentifier, "doNotRelease");

			GKInstance stableIdentifierVersion = getStableIdentifierVersion(stableIdentifier, stableIdentifierVersionString);
			GKInstance release = getReleaseInstance(currentReleaseNum, currentProjectName);
            if (release==null)
            	return;
			// Look for an existing ReleaseId instance that would correspond
			// to the supplied currentReleaseNum/DB_ID pair, otherwise create one
			// if nothing was found.  Actually, you would normally expect
            // to create a new one.  If one already exists, it means that
            // somebody (probably you) has already done a stable ID run with
            // the given release.  A potentially dodgy thing to do!
            List query = new ArrayList();
            query.add(dba.createAttributeQueryRequest("ReleaseId", getReleaseColumn(), "=", release));
            query.add(dba.createAttributeQueryRequest("ReleaseId", "instanceDB_ID", "=", DB_ID));
            Set releaseIds = dba.fetchInstance(query);
            GKInstance releaseId;
            if (releaseIds==null || releaseIds.size()==0) {
            	releaseId = new GKInstance();
            	releaseId.setSchemaClass(dba.getSchema().getClassByName("ReleaseId"));
//            	releaseId.setDbAdaptor(dba);
            	releaseId.setIsInflated(true);
            	releaseId.setAttributeValue(getReleaseColumn(), release);
            	releaseId.setAttributeValue("instanceDB_ID", new Integer(DB_ID.intValue())); // TODO: odd, that I have to cast to Integer
//            	if (!testMode)
//            		dba.storeInstance(releaseId);
            } else {
            	releaseId = (GKInstance)releaseIds.toArray()[0]; // should only be one value in array
//            	System.err.println("IdentifierDatabase.insertNewVersion: WARNING - releaseId already exits for DB_ID=" + DB_ID + ", will overwrite!!");
            }
			
			// Create and store a new version instance
			if (stableIdentifierVersion==null) {
				stableIdentifierVersion = new GKInstance();
				stableIdentifierVersion.setSchemaClass(dba.getSchema().getClassByName("StableIdentifierVersion"));
//				stableIdentifierVersion.setDbAdaptor(dba);
				stableIdentifierVersion.setIsInflated(true);
				stableIdentifierVersion.setAttributeValue("identifierVersion", stableIdentifierVersionString);
				stableIdentifierVersion.addAttributeValue("releaseIds", releaseId);
				stableIdentifierVersion.addAttributeValue("dateTime", GKApplicationUtilities.getDateTime());
//	        	if (!testMode)
//	        		dba.storeInstance(stableIdentifierVersion);
				
				// Insert the new version instance into the stable ID instance
				// and update db
				stableIdentifier.addAttributeValue("stableIdentifierVersion", stableIdentifierVersion);
				if (!testMode)
					dba.updateInstanceAttribute(stableIdentifier, "stableIdentifierVersion");
			} else {
				stableIdentifierVersion.getAttributeValue("releaseIds"); // make sure this is instantiated!
				
				// Do some sanity checking to see if for previous releases,
				// multiple occurrances of the same ReleaseId arise.
				List releaseIdsList = stableIdentifierVersion.getAttributeValuesList("releaseIds"); 
				GKInstance loopReleaseId;
				int duplicatedReleaseIdCounter = 0;
				Long loopDB_ID;
				String loopRelaseNum;
				String loopProjectName;
				for (Iterator ri = releaseIdsList.iterator(); ri.hasNext();) {
					loopReleaseId = (GKInstance)ri.next();
					loopDB_ID = new Long(((Integer)loopReleaseId.getAttributeValue("instanceDB_ID")).longValue());
					loopRelaseNum = (String)((GKInstance)loopReleaseId.getAttributeValue(getReleaseColumn())).getAttributeValue("num");
					loopProjectName = (String)((GKInstance)((GKInstance)loopReleaseId.getAttributeValue(getReleaseColumn())).getAttributeValue("project")).getAttributeValue("name");
					if (loopDB_ID == DB_ID && loopRelaseNum.equals(currentReleaseNum) && loopProjectName.equals(currentProjectName))
						duplicatedReleaseIdCounter++;
				}
				if (duplicatedReleaseIdCounter>0) {
					System.err.println("IdentifierDatabase.insertNewVersion: " + duplicatedReleaseIdCounter + " repeats of DB_ID=" + DB_ID + " at release " + currentReleaseNum + "; it is likely that stable ID generation has been run twice during a release");
				} else {
					// Check for the preexistence of releaseId before
					// adding it.
					stableIdentifierVersion.addAttributeValue("releaseIds", releaseId);
		        	if (!testMode)
		        		dba.updateInstanceAttribute(stableIdentifierVersion, "releaseIds");
				}
        	}
			
			if (instanceBiologicalMeaning!=null) {
				// If we have information about what changes took place
				// in this instance from one release to the next, note
				// that information in the database
				String newSchemaClassName = instanceBiologicalMeaning.getNewSchemaClassName();
				if (newSchemaClassName!=null) {
					stableIdentifierVersion.setAttributeValue("newSchemaClassName", newSchemaClassName);
		        	if (!testMode)
		        		dba.updateInstanceAttribute(stableIdentifierVersion, "newSchemaClassName");
				}
				List deletedAttributesWithContent = instanceBiologicalMeaning.getDeletedAttributesWithContent();
				if (deletedAttributesWithContent!=null && deletedAttributesWithContent.size()>0) {
					stableIdentifierVersion.setAttributeValue("deletedAttributesWithContent", deletedAttributesWithContent);
		        	if (!testMode)
		        		dba.updateInstanceAttribute(stableIdentifierVersion, "deletedAttributesWithContent");
				}
				List addedAttributesWithContent = instanceBiologicalMeaning.getAddedAttributesWithContent();
				if (addedAttributesWithContent!=null && addedAttributesWithContent.size()>0) {
					stableIdentifierVersion.setAttributeValue("addedAttributesWithContent", addedAttributesWithContent);
		        	if (!testMode)
		        		dba.updateInstanceAttribute(stableIdentifierVersion, "addedAttributesWithContent");
				}
				List changedAttributes = instanceBiologicalMeaning.getChangedAttributes();
				if (changedAttributes!=null && changedAttributes.size()>0) {
					stableIdentifierVersion.setAttributeValue("changedAttributes", changedAttributes);
		        	if (!testMode)
		        		dba.updateInstanceAttribute(stableIdentifierVersion, "changedAttributes");
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.insertNewVersion: could not get stableIdentifierVersion");
			e.printStackTrace();
		}
	}
	
	/**
	 * Takes the supplied StableIdentifier instance, deletes it and also deletes the appropriate
	 * hierarchy of instances under it, dependent on the supplied version number.
	 * 
	 * @param stableIdentifier - a StableIdentifier instance from an identifier database
	 * @param stableVersionString
	 * @param currentReleaseNum
	 * @param testMode - if true, nothing will be inserted into database
	 */
	// TODO: this needs some attention - most of the command line args don't do anything.
	public void deleteVersion(GKInstance stableIdentifier, String stableIdentifierVersionString, String currentReleaseNum, String currentProjectName, Long DB_ID) {
		try {
			MySQLAdaptor dba = getDba();
			
			// Retrieve the current release instance
            List query = new ArrayList();
			List projectSubquery = new ArrayList();
			projectSubquery.add(dba.createAttributeQueryRequest("Project","name","=",currentProjectName));
			Set projects = dba.fetchInstance(projectSubquery);
//			query.add(dba.createAttributeQueryRequest(getReleaseTable(),"project","=",projectSubquery));
			query.add(dba.createAttributeQueryRequest(getReleaseTable(),"project","=",projects));
            query.add(dba.createAttributeQueryRequest(getReleaseTable(), "num", "=", currentReleaseNum));
            Set releases = dba.fetchInstance(query);
            GKInstance release = null;
            if (releases==null || releases.size()==0) {
            	System.err.println("IdentifierDatabase.deleteVersion: could not find a release instance for release num: " + currentReleaseNum);
            	return;
            } else
            	release =(GKInstance)releases.toArray()[0];

            if (release!=null)
            	rollbackRelease(release);
            else
            	System.err.println("IdentifierDatabase.deleteVersion: release is null!!\n");
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.deleteVersion: something went wrong when trying to get Release instances");
			e.printStackTrace();
		}
	}
	
	/** 
	 *  Returns a count of the "Release" instances for a given project.
	 */
	public static int getReleasesCount(String projectName) {
		List releases = IdentifierDatabase.getReleases(projectName);
		
		int releasesCount = 0;
		if (releases!=null)
			releasesCount = releases.size();
		
		return releasesCount;
	}
	
	/** 
	 *  Returns a list of "Release" instances, one for each of
	 *  the releases known to the identifier database.
	 */
	public static List getReleases() {
		return getReleases(null);
	}
	
	/** 
	 *  Returns a list of "Release" instances, one for each of
	 *  the releases known to the identifier database.
	 */
	public static List getReleases(String projectName) {
		Collection releases;
		try {
			releases = dba.fetchInstancesByClass(getReleaseTable());
		} catch (Exception e1) {
			System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: WARNING - unable to get releases");
			e1.printStackTrace();
			return null;
		}
		
		List projectReleases = new ArrayList();
		String listProjectName;
		for (Iterator ri = releases.iterator(); ri.hasNext();) {
			GKInstance release = (GKInstance)ri.next();
			try {
				GKInstance project = (GKInstance)release.getAttributeValue("project");
				if (project==null) {
					System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: WARNING - project is null for this release");
					System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: release num: " + release.getAttributeValue("num"));
				} else {
					listProjectName = (String)project.getAttributeValue("name");
					if (listProjectName==null)
						System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: WARNING - listProjectName=null!!");
					else {
						if (projectName==null || listProjectName.equals(projectName)) {
							projectReleases.add(release);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: WARNING - problem whilst getting the project name!");
				e.printStackTrace();
			}
		}
		
		return projectReleases;
	}
	
	/** 
	 *  Returns a list of "Release" instances, one for each of
	 *  the releases known to the identifier database.  Releases
	 *  will be sorted by release number, lowest first.
	 */
	public static List getReleasesSorted(String projectName) {
		IdentifierDatabase identifierDatabase = new IdentifierDatabase();
		List releases = identifierDatabase.getReleases(projectName);

		if (releases!=null && releases.size()>0) {
			// Sort the releases so that the highest release number
			// comes last.
			IdentifierDatabase.ReleaseComparator releaseComparator = identifierDatabase.getNewReleaseComparator();
			Collections.sort(releases, releaseComparator);
		}
		
		return releases;
	}
	
	public ReleaseComparator getNewReleaseComparator() {
		IdentifierDatabase.ReleaseComparator releaseComparator = new IdentifierDatabase.ReleaseComparator();
		
		return releaseComparator;
	}
	
	public static String getLastNonNullReleaseNum(String projectName) {
		List releases = IdentifierDatabase.getReleasesSorted(projectName);
		String lastNonNullReleaseNumString = null;
		if (releases==null)
			return lastNonNullReleaseNumString;
		
		GKInstance release;
		String releaseNumString;
		for (Iterator ri = releases.iterator(); ri.hasNext();) {
			release = (GKInstance)ri.next();
			try {
				releaseNumString = (String)release.getAttributeValue("num");
				if (releaseNumString!=null && !releaseNumString.equals("")) {
					lastNonNullReleaseNumString = releaseNumString;
				}
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getLastNonNullReleaseNum: something went awry whilst getting the release number!");
				e.printStackTrace();
			}
			release = null;
		}

		return lastNonNullReleaseNumString;		
	}
	
	/** 
	 *  Takes the given instance (assumed to be of type "Release")
	 *  and stores it in to the database.  It is assumed that the
	 *  instance does not yet exist.
	 */
	public void storeRelease(GKInstance release) {
		MySQLAdaptor dba = getDba(); // instance database adaptor
		if (dba!=null)
			try {
				// Store the actual release
				dba.storeInstance(release);
				
				// Store the database access info too
				GKInstance dbParams = (GKInstance)release.getAttributeValue(dbParamsAttribute);
				if (dbParams!=null)
					dba.storeInstance(dbParams);
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.storeRelease: something went wrong when trying to get Release instances");
				e.printStackTrace();
			}
	}
	
	/** 
	 *  Takes the given instance (assumed to be of type "Release")
	 *  and stores it in to the database.  It is assumed that the
	 *  instance already exists.
	 */
	public static void updateRelease(GKInstance release) {
		MySQLAdaptor dba = getDba(); // instance database adaptor
		if (dba!=null)
			try {
//				// Update the database access info too
//				GKInstance dbParams = (GKInstance)release.getAttributeValue(dbParamsAttribute);
//				if (dbParams!=null) {
//					Long dbID = dbParams.getDBID();
//					if (dbID==null)
//						dba.storeInstance(dbParams);
//					else
//						dba.updateInstance(dbParams); // TODO: could be dangerous, keep an eye on it!
//				}
				
				// Explicitly update the attributes that may have changed
				dba.updateInstanceAttribute(release, "num");
				dba.updateInstanceAttribute(release, "dateTime");
				dba.updateInstanceAttribute(release, "sliceDbParams");
				dba.updateInstanceAttribute(release, "releaseDbParams");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.updateRelease: something went wrong when trying to get Release instances");
				e.printStackTrace();
			}
	}
	
	/** 
	 *  Deletes the Release instance corresponding to the
	 *  supplied release number and performs a rollbackRelease.
	 */
	public void deleteRelease(String releaseNumber, String projectName) {
		GKInstance release = getReleaseInstance(releaseNumber, projectName);

		rollbackRelease(release);
		deleteInstance(release);
	}
	
	/** 
	 *  Removes all references to instances that got a new
	 *  version number in the named release.
	 */
	public void rollbackRelease(GKInstance release) {
		if (release==null)
			return;
		
		MySQLAdaptor dba = getDba();
		if (dba==null)
			return;
		
		try {
			Collection instances = dba.fetchInstanceByAttribute("ReleaseId", getReleaseColumn(), "=", release);
			
			// Remove the ReleaseId instances containing this release
			for (Iterator ri = instances.iterator(); ri.hasNext();) {
				GKInstance releaseId = (GKInstance)ri.next();
				rollbackReleaseId(releaseId);
				deleteInstance(releaseId);
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.rollbackRelease: something went awry fetching instances!");
			e.printStackTrace();
		}
	}
	
	public void rollbackReleaseId(GKInstance releaseId) {
		if (releaseId==null)
			return;
		
		MySQLAdaptor dba = getDba();
		if (dba==null)
			return;
		
		try {
			Collection instances = dba.fetchInstanceByAttribute("StableIdentifierVersion", "releaseIds", "=", releaseId);
			
			// Remove the StableIdentifierVersion instances containing this releaseId
			for (Iterator ri = instances.iterator(); ri.hasNext();) {
				GKInstance stableIdentifierVersion = (GKInstance)ri.next();
				rollbackStableIdentifierVersion(stableIdentifierVersion);
				deleteInstance(stableIdentifierVersion);
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.rollbackReleaseId: something went awry fetching instances!");
			e.printStackTrace();
		}
	}
	
	public void rollbackStableIdentifierVersion(GKInstance stableIdentifierVersion) {
		if (stableIdentifierVersion==null)
			return;
		
		MySQLAdaptor dba = getDba();
		if (dba==null)
			return;
		
		try {
			Collection instances = dba.fetchInstanceByAttribute("StableIdentifier", "stableIdentifierVersion", "=", stableIdentifierVersion);
			
			// Remove the StableIdentifierVersion instances containing this releaseId
			for (Iterator ri = instances.iterator(); ri.hasNext();) {
				GKInstance stableIdentifier = (GKInstance)ri.next();
				
				// Delete stable ID if it only has one version,
				// namely, the one being rolled back.  If there
				// are multiple versions, remove from
				// "stableIdentifierVersion" attribute
				List stableIdentifierVersions = stableIdentifier.getAttributeValuesList("stableIdentifierVersion");
				if (stableIdentifierVersions.size()==1)
					deleteInstance(stableIdentifier);
				else if (stableIdentifierVersions.size()>1) {
					stableIdentifier.removeAttributeValueNoCheck("stableIdentifierVersion", stableIdentifierVersion);
					// TODO: update db to reflect the deleted attribute
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.rollbackReleaseId: something went awry fetching instances!");
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the StableIdentifier instances from the identifier database
	 * corresponding to a DB_ID in a release.  Generally speaking, there
	 * should only be zero or one instance in the returned Set.
	 */
	public static Set getStableIdentifiersFromReleaseDB_ID(String releaseNum, String projectName, Long DB_ID) {
		MySQLAdaptor dba = getDba();
	    
		Set stableIdentifiers = null;
		String stableIdentifierClass = "StableIdentifier";
		try {
			// Build a nested query the Java way.
			MySQLAdaptor.QueryRequestList projectSubquery = dba.new QueryRequestList();
			MySQLAdaptor.QueryRequestList releaseSubquery = dba.new QueryRequestList();
			MySQLAdaptor.QueryRequestList releaseIdsSubquery = dba.new QueryRequestList();
			MySQLAdaptor.QueryRequestList stableIdentifierVersionSubquery = dba.new QueryRequestList();

			projectSubquery.add(dba.createAttributeQueryRequest("Project","name","=",projectName));
			Set projects = dba.fetchInstance(projectSubquery);
			releaseSubquery.add(dba.createAttributeQueryRequest(getReleaseTable(),"project","=",projects));
			releaseSubquery.add(dba.createAttributeQueryRequest(getReleaseTable(),"num","=",releaseNum));
			releaseIdsSubquery.add(dba.createAttributeQueryRequest("ReleaseId",getReleaseColumn(),"=",releaseSubquery));
			releaseIdsSubquery.add(dba.createAttributeQueryRequest("ReleaseId","instanceDB_ID","=",DB_ID));
			stableIdentifierVersionSubquery.add(dba.createAttributeQueryRequest("StableIdentifierVersion","releaseIds","=",releaseIdsSubquery));

			List query = new ArrayList();
			query.add(dba.createAttributeQueryRequest(stableIdentifierClass,"stableIdentifierVersion","=",stableIdentifierVersionSubquery));

			stableIdentifiers = dba.fetchInstance(query);
		} catch (InvalidClassException e) {
			System.err.println("IdentifierDatabase.getStableIdentifierFromReleaseDB_ID: invalid class " + stableIdentifierClass + "!");
			e.printStackTrace();
		} catch (InvalidAttributeException e) {
			System.err.println("IdentifierDatabase.getStableIdentifierFromReleaseDB_ID: one of the attributes is not understood");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getStableIdentifierFromReleaseDB_ID: generic exception");
			e.printStackTrace();
		}

		return stableIdentifiers;
	}

	/**
	 * Given a release number, get the first matching release
	 * instance from the database.  If no corresponding release
	 * can be found, return null.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public static GKInstance getReleaseInstance(String releaseNum, String projectName) {
		MySQLAdaptor dba = getDba();

		// Retrieve the current Release instance matching
		// currentReleaseNum.
        GKInstance release = null;
        try {
			List query = new ArrayList();
			List projectSubquery = new ArrayList();
			projectSubquery.add(dba.createAttributeQueryRequest("Project","name","=",projectName));
			Set projects = dba.fetchInstance(projectSubquery);
//			query.add(dba.createAttributeQueryRequest(getReleaseTable(),"project","=",projectSubquery));
			query.add(dba.createAttributeQueryRequest(getReleaseTable(),"project","=",projects));
			query.add(dba.createAttributeQueryRequest(getReleaseTable(), "num", "=", releaseNum));
			Set releases = dba.fetchInstance(query);
			if (releases!=null && releases.size()>0)
				release = (GKInstance)releases.toArray()[0];
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getReleaseInstance: WARNING - problem running a query to fetch current release");
			e.printStackTrace();
		}

		return release;
	}
	
	/**
	 * Given a release database name, get the first matching release
	 * instance from the database.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public GKInstance getReleaseInstanceFromDbName(String dbName) {
		GKInstance release = null;
		List releases = getReleases();
		if (releases==null)
			return release;

		for (Iterator ri = releases.iterator(); ri.hasNext();) {
			release = (GKInstance)ri.next();
			try {
				GKInstance sliceDbParams = (GKInstance)release.getAttributeValue(dbParamsAttribute);
				if (sliceDbParams!=null) {
					String releaseDbName = (String)sliceDbParams.getAttributeValue("dbName");
					if (releaseDbName!=null && releaseDbName.equals(dbName))
						break;
				}
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getReleaseInstanceFromDbName: something went awry whilst getting the database name!");
				e.printStackTrace();
			}
			release = null;
		}
		
		return release;
	}
	
	/**
	 * Given a release database name, get the first matching release
	 * instance from the database and return its release number.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public String getReleaseNumFromDbName(String dbName) {
		String releaseNum = null;
		GKInstance release = getReleaseInstanceFromDbName(dbName);
		if (release!=null)
			try {
				releaseNum = (String)release.getAttributeValue("num");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getReleaseNumFromDbName: something went awry whilst getting the release number!");
				e.printStackTrace();
			}
		
		return releaseNum;
	}
	
	/**
	 * Given a release database name, get the first matching release
	 * instance from the database and return its project name.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public String getProjectNameFromDbName(String dbName) {
		String projectName = null;
		GKInstance release = getReleaseInstanceFromDbName(dbName);
		if (release!=null)
			try {
				projectName = (String)((GKInstance)release.getAttributeValue("project")).getAttributeValue("name");
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.getProjectNameFromDbName: something went awry whilst getting the project name!");
				e.printStackTrace();
			}
		
		return projectName;
	}
	
	/**
	 * Given a release database adaptor, get the first matching release
	 * instance from the identifier database and return its release number.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public String getReleaseNumFromReleaseDba(MySQLAdaptor releaseDba) {
		String releaseNum = null;
		
		if (releaseDba==null)
			return releaseNum;
		
		String dbName = releaseDba.getDBName();
		if (dbName!=null) {
			releaseNum = getReleaseNumFromDbName(dbName);
		}
		
		return releaseNum;
	}
	
	/**
	 * Given a release database adaptor, get the first matching release
	 * instance from the identifier database and return its project name.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public String getProjectNameFromReleaseDba(MySQLAdaptor releaseDba) {
		String projectName = null;
		
		if (releaseDba==null)
			return projectName;
		
		String dbName = releaseDba.getDBName();
		if (dbName!=null) {
			projectName = getProjectNameFromDbName(dbName);
		}
		
		return projectName;
	}
	
	/**
	 * Given a DbParams instance, get a database adaptor for the
	 * associated release database.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public MySQLAdaptor getReleaseDbaFromDbParams(GKInstance dbParams) {
		return(getReleaseDbaFromDbParams(dbParams, null, null));
	}
	
	/**
	 * Given a DbParams instance, get a database adaptor for the
	 * associated release database.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public MySQLAdaptor getReleaseDbaFromDbParams(GKInstance dbParams, String username, String password) {
		MySQLAdaptor dba = null;
		String hostname = null;
		String dbName = null;
		String port = null;
		try {
			hostname = (String)dbParams.getAttributeValue("host");
			dbName = (String)dbParams.getAttributeValue("dbName");
			if (username==null)
				username = (String)dbParams.getAttributeValue("user");
			if (username==null && password==null)
				password = (String)dbParams.getAttributeValue("pwd");
			port = (String)dbParams.getAttributeValue("port");
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getReleaseDgetReleaseDbaFromDbParamsbaFromReleaseNum: problem getting an attribute value");
			e.printStackTrace();
			return dba;
		}
		
		if (username==null)
			username = "";
		if (password==null)
			password = "";

		// Create a key for caching the dba
		String key = "";
		if (hostname!=null)
			key += hostname;
		if (dbName!=null)
			key += dbName;
		if (username!=null)
			key += username;
		if (password!=null)
			key += password;
		if (port!=null)
			key += port;
		if (key.equals(""))
			System.err.println("IdentifierDatabase.getReleaseDgetReleaseDbaFromDbParamsbaFromReleaseNum: empty key string, thats bad!!");

		if (releaseDbas==null)
			releaseDbas = new HashMap();
		
		// Use cached DBA, if available
		dba = (MySQLAdaptor)releaseDbas.get(key);
		if (dba==null) {
			try {
				Integer integerPort = null;
				if (port == null || port.equals(""))
					dba = new MySQLAdaptor(hostname, dbName, username, password);
				else
					dba = new MySQLAdaptor(hostname, dbName, username, password, Integer.parseInt(port));
				if (dba == null)
					System.err.println("IdentifierDatabase.getReleaseDbaFromDbParams: dba == null for database name: " + dbName);
				else
					releaseDbas.put(key, dba);
			} catch (NumberFormatException e) {
				System.err.println("IdentifierDatabase.getReleaseDbaFromDbParams: port number " + port + "is strange for database name: " + dbName);
				e.printStackTrace();
			} catch (SQLException e1) {
				System.err.println("IdentifierDatabase.getReleaseDbaFromDbParams: something went wrong with mysql for database name: " + dbName);
				e1.printStackTrace();
			} catch (Exception e2) {
				System.err.println("IdentifierDatabase.getReleaseDbaFromDbParams: unknown problem with dbName=" + dbName + ", username=" + username + ", password=" + password + ", port=" + port);
				e2.printStackTrace();
			}
		}
		
		return dba;
	}
	
	/**
	 * Given a release number, get a database adaptor for the
	 * associated release database.  To be more precise, the
	 * DBA for the *slice* taken at the time of the given
	 * release is returned.
	 * 
	 * @param releaseNumber
	 * @return
	 */
	public MySQLAdaptor getReleaseDbaFromReleaseNum(String releaseNum, String projectName) {
		MySQLAdaptor dba = null;
		
		if (releaseNum==null) {
			System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: WARNING - releaseNum is null");
			return dba;
		}
		if (projectName==null) {
			System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: WARNING - projectName is null");
			return dba;
		}
		
		String key = releaseNum + "." + projectName;

		if (releaseNumToDbaHash==null) {
			releaseNumToDbaHash = new HashMap();
		}
		Object o = releaseNumToDbaHash.get(key);
		if (o==IDERROR) {
			return null;
		} else if (o!=null)
			return (MySQLAdaptor)o;
		
		GKInstance release = IdentifierDatabase.getReleaseInstance(releaseNum, projectName);
		
		if (release==null) {
//			System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: WARNING - release is null for releaseNum=" + releaseNum);
			return dba;
		}
		
		GKInstance sliceDbParams = null;
		try {
			sliceDbParams = (GKInstance)release.getAttributeValue(dbParamsAttribute);
			
			if (sliceDbParams==null)
				System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: WARNING - sliceDbParams is null for releaseNum=" + releaseNum + ", dbParamsAttribute=" + dbParamsAttribute);
			else {
				dba = getReleaseDbaFromDbParams(sliceDbParams);
				
				// If the DBA could not be found, it may be due
				// to the fact that the identifier database does
				// not contain user information, for security
				// reasons.  In this case, try out the username
				// and password stored in the object, maybe they
				// will work...
				if (dba==null) {
					System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: Retry getReleaseDbaFromReleaseNum using user and password from current release");
					dba = getReleaseDbaFromDbParams(sliceDbParams, username, password);
				}
			}
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: something horrid happened for releaseNum=" + releaseNum + ", dbParamsAttribute=" + dbParamsAttribute);
			if (sliceDbParams!=null) {
				try {
					System.err.println("IdentifierDatabase.getReleaseDbaFromReleaseNum: sliceDbParams=" + sliceDbParams.toStanza());
				} catch (Exception e1) {
				}
			}
			e.printStackTrace();
		}
		
		if (dba==null)
			releaseNumToDbaHash.put(key, IDERROR);
		else
			releaseNumToDbaHash.put(key, dba);
		
		return dba;
	}
	
	/** 
	 *  Takes the given instance (assumed to be of type "Release")
	 *  and deletes the corresponding instance (plus all referrers
	 *  to it) from the identifier database.
	 */
	public static void deleteInstance(GKInstance instance) {
		if (instance==null)
			return;
		
		MySQLAdaptor dba = getDba();
		if (dba!=null)
			try {
				dba.deleteInstance(instance);
			} catch (Exception e) {
				System.err.println("IdentifierDatabase.deleteInstance: something went wrong when trying to get Release instances");
				e.printStackTrace();
			}
	}
	
	/**
	 * Sets the dba for the identifier database.
	 * 
	 * @return
	 */
	public static void setDba(MySQLAdaptor dba) {
		if (dba != null) {
			// 'Release' bacame a reserved word as of MySQL 5, so
			// the name of this attribute got changed to 'ReactomeRelease'.
			// In order to make this class backwards compatible,
			// check to see which table name is actually being used.
			String releaseTable = null;
			String releaseColumn = null;
			if (dba.getSchema().isValidClass("ReactomeRelease")) {
				releaseTable = "ReactomeRelease";
				releaseColumn = "reactomeRelease";
			} else if (dba.getSchema().isValidClass("Release")) {
				releaseTable = "Release";
				releaseColumn = "release";
			}
			
			if (releaseTable != null) {
				IdentifierDatabase.dba = dba;
				
				IdentifierDatabase.setReleaseTable(releaseTable);
				IdentifierDatabase.setReleaseColumn(releaseColumn);
			} else
				System.err.println("IdentifierDatabase.setDba: WARNING - could not find table names ReactomeRelease or Release, this might not be a stable identifier database!");
		}
	}
	
	/**
	 * 
	 * Returns the name of the release table.  This changed when we switched to MySQL 5.
	 */
	public static String getReleaseTable() {
		return releaseTable;
	}

	public static void setReleaseTable(String releaseTable) {
		IdentifierDatabase.releaseTable = releaseTable;
	}

	public static String getReleaseColumn() {
		return releaseColumn;
	}

	public static void setReleaseColumn(String releaseColumn) {
		IdentifierDatabase.releaseColumn = releaseColumn;
	}

	/**
	 * Gets a dba for the identifier database.  This presupposes
	 * that the user has already loaded appropriate parameters.
	 * If that *isn't* the case, then null will be returned.
	 * 
	 * @return
	 */
	public static MySQLAdaptor getDba() {
		return dba;
	}
	
	public GKInstance createDeleted() {
		if (dba==null)
			return null;
		
		GKInstance instance = new GKInstance();
		instance.setSchemaClass(dba.getSchema().getClassByName("Deleted"));
		instance.setDbAdaptor(dba);
		
		return instance;
	}
	
	public GKInstance createReplacement() {
		if (dba==null)
			return null;
		
		GKInstance instance = new GKInstance();
		instance.setSchemaClass(dba.getSchema().getClassByName("Replacement"));
		instance.setDbAdaptor(dba);
		
		return instance;
	}
	
	/**
	 * For the given release, copies DbParams from slice to release.  If
	 * release already contained parameters, they will be overwritten.
	 * 
	 * @param release
	 */
	public static void copySliceToReleaseDbParams(GKInstance release) {
		if (release==null)
			return;
		
        try {
    		MySQLAdaptor dba = getDba();

    		GKInstance sliceDbParams = (GKInstance)release.getAttributeValue("sliceDbParams");
    		// Nothing to copy
    		if (sliceDbParams==null)
        		return;

    		GKInstance releaseDbParams = (GKInstance)release.getAttributeValue("releaseDbParams");
        	
        	// If this is the first time we have encountered these
        	// parameters, create a new instance for them
    		boolean releaseDbParamsAlreadyExists = true;
        	if (releaseDbParams==null) {
        		releaseDbParamsAlreadyExists = false;
        		releaseDbParams = new GKInstance();
        		releaseDbParams.setSchemaClass(dba.getSchema().getClassByName("DbParams"));
//            	releaseDbParams.setDbAdaptor(dba);
        		releaseDbParams.setIsInflated(true);
        	}
        	
        	// Copy parameters
        	releaseDbParams.setAttributeValueNoCheck("dbName", sliceDbParams.getAttributeValue("dbName"));
        	releaseDbParams.setAttributeValueNoCheck("host", sliceDbParams.getAttributeValue("host"));
        	releaseDbParams.setAttributeValueNoCheck("port", sliceDbParams.getAttributeValue("port"));
        	releaseDbParams.setAttributeValueNoCheck("user", sliceDbParams.getAttributeValue("user"));
        	releaseDbParams.setAttributeValueNoCheck("pwd", sliceDbParams.getAttributeValue("pwd"));
        	release.setAttributeValueNoCheck("releaseDbParams", releaseDbParams);
        	
        	// Now store the information
        	if (releaseDbParamsAlreadyExists) {
        		dba.updateInstanceAttribute(releaseDbParams, "dbName");
        		dba.updateInstanceAttribute(releaseDbParams, "host");
        		dba.updateInstanceAttribute(releaseDbParams, "port");
        		dba.updateInstanceAttribute(releaseDbParams, "user");
        		dba.updateInstanceAttribute(releaseDbParams, "pwd");
        	} else
        		dba.storeInstance(releaseDbParams);
    		dba.updateInstanceAttribute(release, "releaseDbParams");
		} catch (Exception e) {
			System.err.println("IdentifierDatabase.copySliceToReleaseDbParams: WARNING - problem getting test results from release");
			e.printStackTrace();
		}		
	}

	public static String getDefaultProjectName() {
		return defaultProjectName;
	}

	/**
	 * Used for comparing pairs of releases, to determine
	 * their ordering in a sort.
	 * 
	 * It is up to you to ensure that both releases are from the
	 * same project, this is not checked.
	 * 
	 * @author croft
	 *
	 */
	public class ReleaseComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			GKInstance release1 = null;
			GKInstance release2 = null;
			try {
				release1 = (GKInstance)o1;
				release2 = (GKInstance)o2;
			} catch (ClassCastException e) {
				System.err.println("IdentifierDatabase.ReleaseComparator.compare(): One of the objects for comparison does not have type GKInstance");
				e.printStackTrace();
				return 0;
			}
			
			String releaseNum1 = null;
			String releaseNum2 = null;
			try {
				releaseNum1 = (String)release1.getAttributeValue("num");
				releaseNum2 = (String)release2.getAttributeValue("num");
			} catch (Exception e) {
				System.err.println("IdentifierDatabaseReleaseComparator.compare(): problem arose while trying to get release number from Release instance");
				e.printStackTrace();
			}
			
			// Decide what to do if one of the release numbers is
			// null or empty
			if ((releaseNum1==null || releaseNum1.equals("")) && (releaseNum2==null || releaseNum2.equals("")))
				return 0;
			if ((releaseNum1==null || releaseNum1.equals("")) && releaseNum2!=null && !(releaseNum2.equals("")))
				return 1;
			if (releaseNum1!=null && !(releaseNum1.equals("")) && (releaseNum2==null || releaseNum2.equals("")))
				return (-1);
			
			// If both can be interpreted as numbers, do a numeric comparison
			try {
				int num1 = (new Integer(releaseNum1)).intValue();
				int num2 = (new Integer(releaseNum2)).intValue();
				if (num1>num2)
					return 1;
				if (num1<num2)
					return (-1);
				return 0;
			} catch (NumberFormatException e) {
			}
			
			// Do a simple lexicographic comparison of the release
			// numbers.
			return releaseNum1.compareToIgnoreCase(releaseNum2);
		}
	}
}