/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/** 
 *  This is the main class for the IDGeneration program,
 *  using a command line interface.  Usage:
 *  
 *  java org.gk.IDGeneration.IDGenerationCommandLine <options>
 *  
 *  The following options are available:
 *  
 *  -o orthology mode - tries to find stable IDs for orthology
 *     predicted instances in the release.
 *  -f force all questions to be answered with "yes"
 *     (allows non-interactive use)
 *  -t run in test mode (nothing inserted into datbases)
 *  -host <hostname> default hostname for all databases (e.g. picard.ebi.ac.uk)
 *  -gh <hostname> hostname for gk_central, if different from the other dbs (e.g. bones.ebi.ac.uk)
 *  -user <username> default user name for all databases (you may not need to specify this at all)
 *  -port <port> default port for all databases (e.g. 3306)
 *  -pass <password> default password for all databases (you may not need to specify this at all)
 *  -prnum <release> release number of previous release (e.g. 19)
 *  -cdbname <db name> database name of current slice (e.g. test_slice_20)
 *  -crdbname <db name> database name of current release (e.g. test_ortho_20)
 *  -crnum <release> release number of current release (e.g. 20)
 *  -idbname <db name> database name of identifier database (e.g. test_reactome_stable_identifiers)
 *  -gdbname <db name> database name of gk_central
 *  -s <schema classes> comma-separated list of schema classes
 *     e.g. Pathway,Reaction (leaving this option out causes all default schema classes to be used)
 *  -signore <attributes> comma-separated list of attributes to ignore while determining version number if the schema changes
 *     e.g. _doRelease,_class (leaving this option out causes all attributes to be used)
 *   -nullify Replace username and password with NULL in the identifier database, if requested.
 *    This is for security reasons.
 *  -project <project name> your project - not needed for human Reactome, but needed for other Reactomes (e.g. FlyReactome).
 *  -a disallow using IDs from unspecified releases.
 *  -del_ortho delete existing orthologous stable IDs back to source and replace with fresh ones.
 *  
 *  If you intend to run the program without human supervision
 *  (e.g. in a script) you should include the -f option
 *  in the command line, otherwise the program will get
 *  stuck as soon as it tries to interact with the user.
 *  
 *  If problems occur during ID generation, they will be
 *  reported on STDERR.  If the program runs successfully
 *  to completion, the results of the internally executed
 *  tests will be printed out, giving a summary of the
 *  ID generation process.
 *  
 * @author croft
 */
public class IDGenerationCommandLine {
	private IncludeInstances includeInstances;
	private boolean force;
	private boolean testMode;
	private boolean orthologyMode;
	private boolean nullifyUserAndPassword;
	private String previousReleaseNum;
	private String currentReleaseNum;
	private String currentReleaseDateTime;
	private String projectName;
	private GKInstance project;
	private DbParams currentSliceDbParams;
	private DbParams currentReleaseDbParams;
	private DbParams identifierDbParams;
	private DbParams gk_centralDbParams;
	private List schemaClasseNames;
	private List schemaChangeIgnoredAttributes;
	private IdentifierDatabase identifierDatabase = new IdentifierDatabase();
	private boolean allowIDsFromUnspecifiedReleases = true;
	private List<Long> limitingCurrentDbIds = null;
	private boolean deleteBackToSource = false;
	
	public IDGenerationCommandLine() {
		init();
	}
	
	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isTestMode() {
		return testMode;
	}

	public void setTestMode(boolean testMode) {
		this.testMode = testMode;
	}

	public boolean isOrthologyMode() {
		return orthologyMode;
	}

	public void setOrthologyMode(boolean orthologyMode) {
		this.orthologyMode = orthologyMode;
	}

	public boolean isNullifyUserAndPassword() {
		return nullifyUserAndPassword;
	}

	public void setNullifyUserAndPassword(boolean nullifyUserAndPassword) {
		this.nullifyUserAndPassword = nullifyUserAndPassword;
	}

	public boolean isDeleteBackToSource() {
		return deleteBackToSource;
	}

	public void setDeleteBackToSource(boolean deleteBackToSource) {
		this.deleteBackToSource = deleteBackToSource;
	}

	private void addLimitingCurrentDbId(Long limitingCurrentDbId) {
		if (limitingCurrentDbIds == null)
			limitingCurrentDbIds = new ArrayList<Long>();
		limitingCurrentDbIds.add(limitingCurrentDbId);
	}

	public void setHostname(String hostname) {
		if (currentSliceDbParams.hostname.equals(""))
			currentSliceDbParams.hostname = hostname;
		if (currentReleaseDbParams.hostname.equals(""))
			currentReleaseDbParams.hostname = hostname;
		if (identifierDbParams.hostname.equals(""))
			identifierDbParams.hostname = hostname;
		if (gk_centralDbParams.hostname.equals(""))
			gk_centralDbParams.hostname = hostname;
	}

	public void setGHostname(String hostname) {
		gk_centralDbParams.hostname = hostname;
	}

	public void setPassword(String password) {
		if (currentSliceDbParams.password.equals(""))
			currentSliceDbParams.password = password;
		if (currentReleaseDbParams.password.equals(""))
			currentReleaseDbParams.password = password;
		if (identifierDbParams.password.equals(""))
			identifierDbParams.password = password;
		if (gk_centralDbParams.password.equals(""))
			gk_centralDbParams.password = password;
	}

	public void setPort(String port) {
		if (currentSliceDbParams.port.equals(""))
			currentSliceDbParams.port = port;
		if (currentReleaseDbParams.port.equals(""))
			currentReleaseDbParams.port = port;
		if (identifierDbParams.port.equals(""))
			identifierDbParams.port = port;
		if (gk_centralDbParams.port.equals(""))
			gk_centralDbParams.port = port;
	}

	public void setUsername(String username) {
		if (currentSliceDbParams.username.equals(""))
			currentSliceDbParams.username = username;
		if (currentReleaseDbParams.username.equals(""))
			currentReleaseDbParams.username = username;
		if (identifierDbParams.username.equals(""))
			identifierDbParams.username = username;
		if (gk_centralDbParams.username.equals(""))
			gk_centralDbParams.username = username;
	}

	public String getPreviousReleaseNum() {
		return previousReleaseNum;
	}

	public void setPreviousReleaseNum(String previousReleaseNum) {
		this.previousReleaseNum = previousReleaseNum;
	}

	public String getCurrentReleaseNum() {
		return currentReleaseNum;
	}

	public void setCurrentReleaseNum(String currentReleaseNum) {
		this.currentReleaseNum = currentReleaseNum;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public List getSchemaClasseNames() {
		return schemaClasseNames;
	}

	public void setSchemaClasseNames(List schemaClasses) {
		this.schemaClasseNames = schemaClasses;
	}

	public List getSchemaChangeIgnoredAttributes() {
		return schemaChangeIgnoredAttributes;
	}

	public void setSchemaChangeIgnoredAttributes(List schemaChangeIgnoredAttributes) {
		this.schemaChangeIgnoredAttributes = schemaChangeIgnoredAttributes;
	}

	public boolean isAllowIDsFromUnspecifiedReleases() {
		return allowIDsFromUnspecifiedReleases;
	}

	public void setAllowIDsFromUnspecifiedReleases(
			boolean allowIDsFromUnspecifiedReleases) {
		this.allowIDsFromUnspecifiedReleases = allowIDsFromUnspecifiedReleases;
	}

	private void init() {
		includeInstances = new IncludeInstances();
		force = false;
		testMode = false;
		schemaClasseNames = new ArrayList();
		schemaChangeIgnoredAttributes = new ArrayList();
		currentSliceDbParams = new DbParams();
		currentReleaseDbParams = new DbParams();
		identifierDbParams = new DbParams();
		gk_centralDbParams = new DbParams();
		
		// Put the current date/time into this parameter,
		// just to make sure that something half sensible
		// is there.  The user can always change it by hand
		// in the SQL if it needs a different value.
		currentReleaseDateTime = GKApplicationUtilities.getDateTime();
	}
	
    public void run() {
    	// Check arguments
    	if (!testMode)
    		handleYesNo("Running this program will change live databases irreversibly.");
    	
    	if (orthologyMode) {
	    	identifierDatabase.setUsername(currentReleaseDbParams.username);
	    	identifierDatabase.setPassword(currentReleaseDbParams.password);
    	} else {
	    	identifierDatabase.setUsername(currentSliceDbParams.username);
	    	identifierDatabase.setPassword(currentSliceDbParams.password);
    	}
    	
		MySQLAdaptor identifierDbParamsDba = identifierDbParams.getDba();
		if (identifierDbParamsDba==null)
			handleError("Could not create connection to identifier database - maybe you entered the wrong parameters?");
		IdentifierDatabase.setDba(identifierDbParamsDba);
    	
    	// Set default project to be homo sapiens
		project = null;
    	if (projectName==null)
    		projectName = IdentifierDatabase.getDefaultProjectName();
    	Collection projects = null;
		try {
			// Make sure that project name is valid
			projects = identifierDbParamsDba.fetchInstancesByClass("Project");
		} catch (Exception e1) {
			handleError("Could not extract Project instances from identifier database");
		}
		try {
			String listProjectName;
			GKInstance listProject;
			boolean foundProjectFlag = false;
			String projectNames = "";
			for (Iterator it = projects.iterator(); it.hasNext();) {
				listProject = (GKInstance)it.next();
				listProjectName = (String)listProject.getAttributeValue("name");
				if (listProjectName.equals(projectName)) {
					project = listProject;
					foundProjectFlag = true;
				}
				projectNames += "\t" + listProjectName + "\n";
			}
			if (!foundProjectFlag)
				handleError("Unknown project: " + projectName + ", known projects are:\n" + projectNames);
		} catch (InvalidAttributeException e1) {
			handleError("Problem retrieving project names");
			e1.printStackTrace();
		} catch (Exception e1) {
			handleError("Problem retrieving project names");
			e1.printStackTrace();
		}

    	int previousReleaseNumInt = (-1);
    	int currentReleaseNumInt = (-1);
    	
		if (previousReleaseNum==null)
	    	handleYesNo("You have not specified a previous release, new stable IDs will be created from scratch.");
		else {
			// Check that the user entered a sensible number format
	    	try {
				previousReleaseNumInt = (new Integer(previousReleaseNum)).intValue();
				if (previousReleaseNumInt<1)
					handleError("Invalid previous release number " + previousReleaseNum);
			} catch (NumberFormatException e) {
				handleError("Previous release number (" + previousReleaseNum + ")is not recognisably numeric");
			}
		}

		if (currentReleaseNum==null)
	    	handleError("You have not specified a current release, aborting!");
		else {
			// Check that the user entered a sensible number format
			try {
				currentReleaseNumInt = (new Integer(currentReleaseNum)).intValue();
				if (currentReleaseNumInt<1)
					handleError("Invalid current release number " + currentReleaseNum);
			} catch (NumberFormatException e) {
				handleError("Previous release number (" + currentReleaseNum + ")is not recognisably numeric");
			}
		}
		
		if (previousReleaseNum!=null && currentReleaseNumInt - previousReleaseNumInt != 1)
			handleYesNo("Current release (" + currentReleaseNum + ") is not a direct successor of previous release (" + previousReleaseNum + ").");

    	// In "orthologyMode", look into release databases (rather than the
    	// default slice databases) for matching orthologous events that
    	// could be assigned stable IDs.
    	if (orthologyMode) {
    		identifierDatabase.setDbParamsAttribute(IdentifierDatabase.RELEASE);
    	}

		String lastNonNullReleaseNum = IdentifierDatabase.getLastNonNullReleaseNum(projectName);
		try {
			int lastNonNullReleaseNumInt = (new Integer(lastNonNullReleaseNum)).intValue();
			if (currentReleaseNumInt<lastNonNullReleaseNumInt)
				handleYesNo("Current release (" + currentReleaseNum + ") is not the last in the release list (" + lastNonNullReleaseNum + ").");
		} catch (NumberFormatException e) {
		}
		
    	// Set up a database adaptor for the previous release,
    	// if one has been specified.
		MySQLAdaptor previousDba = identifierDatabase.getReleaseDbaFromReleaseNum(previousReleaseNum, projectName);
		if (previousDba==null && previousReleaseNum!=null)
			handleError("Cannot find a previous release, with release number " + previousReleaseNum);
		
		// Check to see if the current release already exists (it shouldn't)
		MySQLAdaptor currentDba = identifierDatabase.getReleaseDbaFromReleaseNum(currentReleaseNum, projectName);
		if (!orthologyMode && currentDba!=null)
			handleError("Release " + currentReleaseNum + " already exists, cannot overwrite!");
		
		currentDba = null;
    	if (orthologyMode) {
			currentDba = currentReleaseDbParams.getDba();
			if (currentDba==null)
				handleError("Could not create connection to current release database - maybe you entered the wrong parameters?");
    	} else {
    		currentDba = currentSliceDbParams.getDba();
    		if (currentDba==null)
    			handleError("Could not create connection to current slice database - maybe you entered the wrong parameters?");
    	}
		MySQLAdaptor gk_centraldba = null;			
		if (gk_centralDbParams.dbName!=null && !(gk_centralDbParams.dbName.equals(""))) {
			gk_centraldba = gk_centralDbParams.getDba();			
			if (gk_centraldba==null)
				handleError("Could not create connection to " + gk_centralDbParams.dbName + " - maybe you entered the wrong parameters?");
		}
		
    	PreviousInstanceFinder previousInstanceFinder = new DbIdPreviousInstanceFinder(currentReleaseNum, previousDba, true, identifierDatabase);
    	// In "orthologyMode", find previous instances via orthologous
    	// event attributes, rather than by DB_ID
    	if (orthologyMode)
    		previousInstanceFinder = new OrthologousEventPreviousInstanceFinder(currentReleaseNum, previousDba, true, identifierDatabase, deleteBackToSource);
    	previousInstanceFinder.setTest(testMode);
    	previousInstanceFinder.setAllowIDsFromUnspecifiedReleases(allowIDsFromUnspecifiedReleases);
   	
		// Create a new release and add it to the identifier database
    	setCurrentReleaseParams();
    	
    	// Use the default class set if the user hasn't explicitly
    	// specified which classes should be used for stable ID
    	// generation.
    	if (schemaClasseNames.size()==0) {
    		String[] defaultClasses = includeInstances.getDefaultClasses();
    		for (int i=0; i<defaultClasses.length; i++)
    			schemaClasseNames.add(defaultClasses[i]);
    	}
    	
    	// Replace schema class names with true schema classes
    	List schemaClasses = new ArrayList();
    	try {
			Schema currentSchema = currentDba.fetchSchema();
			String schemaClassName;
			SchemaClass schemaClass;
			for (Iterator itc = schemaClasseNames.iterator(); itc.hasNext();) {
				schemaClassName = (String)itc.next();
				schemaClass = currentSchema.getClassByName(schemaClassName);
				if (schemaClass==null)
					handleError("Schema class " + schemaClassName + " not known in current release, dbname=" + currentDba.getDBName());
				schemaClasses.add(schemaClass);
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			handleError("Error getting schema classes");
		}

    	
    	// Get the highest level classes that include all of the
    	// classes specified.
    	List rootClasses = includeInstances.extractRootClasses(schemaClasses);
    	
    	// Make sure there are no "forbidden classes" in the list.
    	List cleanClasses = includeInstances.removeForbiddenClasses(schemaClasses);

    	if (gk_centraldba==null)
    		System.err.println("IDGenerationCommandline: before IDGenerator, gk_central dba is null");
    	else
    		System.err.println("IDGenerationCommandline: before IDGenerator, gk_central dba is not null");
    	
    	// Now do the actual stable ID generation
		IDGenerator idGenerator = new IDGenerator(previousDba, currentDba, gk_centraldba, identifierDatabase, previousInstanceFinder, limitingCurrentDbIds);
    	idGenerator.generateIDs(cleanClasses, testMode, schemaChangeIgnoredAttributes);
    	
    	// Print out the test results
    	idGenerator.testForRepeatedStableIdsInCurrentRelease();
    	idGenerator.testForRepeatedStableIdsInIdentifierDatabase();
    	IDGeneratorTests iDGeneratorTests = idGenerator.getTests();
    	System.err.print(iDGeneratorTests.toString());
    }
    
    /**
     * If necessary, creates a new release for the current release number.
     * Inserts appropriate DB parameters for slice and release, if
     * available.  WARNING: prexisting DB parameters will be overwritten.
     *
     */
    private void setCurrentReleaseParams() {
    	GKInstance release;

    	try {
			MySQLAdaptor instanceDatabaseDba = IdentifierDatabase.getDba();
			
			GKInstance sliceDbParams = null;
			GKInstance releaseDbParams = null;

			// TODO: There is a problem with orthology predicted stable IDs, probably because
			// this code is not successfully finding previous references to the current
			// release number in the release table.  I have inserted some diagnostics to
			// help get a handle on the problem, and made the code insert slice db params
			// when in orthology mode, to stop code elesewhere from breaking.
			// It might be worth trying the "MATCH" operator, instead of "=".
			Collection releases = instanceDatabaseDba.fetchInstanceByAttribute(IdentifierDatabase.getReleaseTable(), "num", "=", currentReleaseNum);

    		System.err.println("IDGenerationCommandline.setCurrentReleaseParams: currentReleaseNum=" + currentReleaseNum);			
    		System.err.println("IDGenerationCommandline.setCurrentReleaseParams: IdentifierDatabase.getReleaseTable()=" + IdentifierDatabase.getReleaseTable());			
    		System.err.println("IDGenerationCommandline.setCurrentReleaseParams: releases.size()=" + releases.size());			
			
			if (releases.size()>0)
				release = (GKInstance)releases.toArray()[0];
			else {
				release = IdentifierDatabase.createBlankRelease();
				release.setAttributeValue("num", currentReleaseNum);
				release.setAttributeValue("project", project);
				release.setAttributeValue("dateTime", currentReleaseDateTime);
			}
			
			if (currentSliceDbParams.dbName!=null && !currentSliceDbParams.dbName.equals("")) {
//			if (currentSliceDbParams.dbName!=null && !currentSliceDbParams.dbName.equals("") && !orthologyMode) {
				sliceDbParams = new GKInstance();
				sliceDbParams.setSchemaClass(instanceDatabaseDba.getSchema().getClassByName("DbParams"));
				sliceDbParams.setDbAdaptor(instanceDatabaseDba);
				sliceDbParams.setAttributeValue("host", currentSliceDbParams.hostname);
				sliceDbParams.setAttributeValue("dbName", currentSliceDbParams.dbName);
				sliceDbParams.setAttributeValue("port", currentSliceDbParams.port);
		    	// Replace username and password with NULL in the identifier
		    	// database, if requested.  This is for security reasons.
		    	if (nullifyUserAndPassword) {
					sliceDbParams.setAttributeValue("host", "localhost");
					sliceDbParams.setAttributeValue("user", "NULL");
					sliceDbParams.setAttributeValue("pwd", "NULL");
		    	} else {
					sliceDbParams.setAttributeValue("user", currentSliceDbParams.username);
					sliceDbParams.setAttributeValue("pwd", currentSliceDbParams.password);
		    	}
		    	
				instanceDatabaseDba.storeInstance(sliceDbParams);
				release.setAttributeValue("sliceDbParams", sliceDbParams);
			}
			if (currentReleaseDbParams.dbName!=null && !currentReleaseDbParams.dbName.equals("")) {
				releaseDbParams = new GKInstance();
				releaseDbParams.setSchemaClass(instanceDatabaseDba.getSchema().getClassByName("DbParams"));
				releaseDbParams.setDbAdaptor(instanceDatabaseDba);
				releaseDbParams.setAttributeValue("host", currentReleaseDbParams.hostname);
				releaseDbParams.setAttributeValue("dbName", currentReleaseDbParams.dbName);
				releaseDbParams.setAttributeValue("port", currentReleaseDbParams.port);
		    	// Replace username and password with NULL in the identifier
		    	// database, if requested.  This is for security reasons.
		    	if (nullifyUserAndPassword) {
		    		releaseDbParams.setAttributeValue("host", "localhost");
		    		releaseDbParams.setAttributeValue("user", "NULL");
		    		releaseDbParams.setAttributeValue("pwd", "NULL");
		    	} else {
		    		releaseDbParams.setAttributeValue("user", currentSliceDbParams.username);
		    		releaseDbParams.setAttributeValue("pwd", currentSliceDbParams.password);
		    	}
				
				instanceDatabaseDba.storeInstance(releaseDbParams);
				release.setAttributeValue("releaseDbParams", releaseDbParams);
			}
			
			// This stuff has to be inserted into the identifier
			// database, otherwise the rest of the code fails
			// These are new instances, so store them.
			if (releases.size()>0)
				instanceDatabaseDba.updateInstance(release);
			else
				instanceDatabaseDba.storeInstance(release);
		} catch (Exception e) {
			handleError("Problem setting database parameters for the current release");
		}
    }
    
    private void handleError(String text) {
    	System.err.println("IDGenerationCommandLine: " + text);
    	System.exit(1);
    }
    
    private void handleYesNo(String text) {
    	if (force)
    		return;
    	
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.err.println(text);
		System.err.print("Are you sure you want to do this?  (y/n) ");
		String answer = "n";
		try {
			answer = reader.readLine();
		} catch (IOException e) {
		}
		if (answer.equals("n"))
			System.exit(0);
    }
    
    static private void printHelp() {
    	System.err.println("Usage:");
    	System.err.println("");
    	System.err.println("java org.gk.IDGeneration.IDGenerationCommandLine <options>");
    	System.err.println("");
    	System.err.println("The following options are available:");
    	System.err.println("");
    	System.err.println(" -f force all questions to be answered with \"yes\"");
    	System.err.println("    (allows non-interactive use)");
    	System.err.println(" -o create only stable IDs for events derived from");
    	System.err.println("    orthology prediction (uses release DBs)");
    	System.err.println(" -t run in test mode (nothing inserted into datbases)");
    	System.err.println(" -host <hostname> default hostname for all databases (e.g. picard.ebi.ac.uk)");
    	System.err.println(" -ghost <hostname> hostname for gk_central if different from others (e.g. bones.ebi.ac.uk)");
    	System.err.println(" -user <username> default user name for all databases");
    	System.err.println(" -port <port> default port for all databases");
    	System.err.println(" -pass <password> default password for all databases");
    	System.err.println(" -prnum <release> release number of previous release");
    	System.err.println(" -cdbname <db name> database name of current slice (e.g. test_slice_20)");
    	System.err.println(" -crdbname <db name> database name of current release (e.g. test_ortho_20)");
    	System.err.println(" -crnum <release> release number of current release");
    	System.err.println(" -idbname <db name> database name of identifier database");
    	System.err.println(" -gdbname <db name> database name of gk_central");
    	System.err.println(" -s <schema classes> comma-separated list of schema classes");
    	System.err.println("    e.g. Pathway,Reaction (leaving this option out causes all");
    	System.err.println("    default schema classes to be used)");
    	System.err.println(" -signore <attributes> comma-separated list of ignored attributes");
    	System.err.println(" -nullify Replace username and password with NULL in the identifier database, if requested");
    	System.err.println("    This is for security reasons.");
    	System.err.println(" -project <project name> specify your project - not needed for human Reactome, but needed for other Reactomes (e.g. FlyReactome)");
    	System.err.println(" -a disallow using IDs from unspecified releases.");
    	System.err.println(" -del_ortho delete existing orthologous stable IDs back to source and replace with fresh ones.");

    	System.exit(0);
    }
    
	static public void main(String[] args) {
		IDGenerationCommandLine iDGenerationCommandLine = new IDGenerationCommandLine();
		
		// Parse arguments
		String s;
		for (int i=0; i<args.length; i++) {
			s = args[i];
			if (s.equals("-f"))
				iDGenerationCommandLine.setForce(true);
			else if (s.equals("-o"))
				iDGenerationCommandLine.setOrthologyMode(true);
			else if (s.equals("-t"))
				iDGenerationCommandLine.setTestMode(true);
			else if (s.equals("-nullify"))
				iDGenerationCommandLine.setNullifyUserAndPassword(true);
			else if (s.equals("-host")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setHostname(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-ghost")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setGHostname(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-user")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setUsername(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-port")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setPort(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-pass")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setPassword(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-prnum")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setPreviousReleaseNum(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-cdbname")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.currentSliceDbParams.dbName = args[i];
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-crdbname")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.currentReleaseDbParams.dbName = args[i];
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-crnum")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.setCurrentReleaseNum(args[i]);
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-idbname")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.identifierDbParams.dbName = args[i];
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-gdbname")) {
				i++;
				if (i<args.length)
					iDGenerationCommandLine.gk_centralDbParams.dbName = args[i];
				else {
					System.err.println("IDGenerationCommandLine: missing argument");
					System.exit(1);
				}
			}
			else if (s.equals("-s")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					List schemaClasseNames = new ArrayList();
					for (int j=0; j<splits.length; j++)
						schemaClasseNames.add(splits[j]);
					iDGenerationCommandLine.setSchemaClasseNames(schemaClasseNames);
				} else {
					iDGenerationCommandLine.handleError("IDGenerationCommandLine: missing argument");
				}
			}
			else if (s.equals("-signore")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					List schemaChangeIgnoredAttributes = new ArrayList();
					for (int j=0; j<splits.length; j++)
						schemaChangeIgnoredAttributes.add(splits[j]);
					iDGenerationCommandLine.setSchemaChangeIgnoredAttributes(schemaChangeIgnoredAttributes);
				} else {
					iDGenerationCommandLine.handleError("IDGenerationCommandLine: missing argument");
				}
			}
			else if (s.equals("-project")) {
				i++;
				if (i<args.length) {
					iDGenerationCommandLine.setProjectName(args[i]);
				} else {
					iDGenerationCommandLine.handleError("IDGenerationCommandLine: missing argument");
				}
			}
			else if (s.equals("-lcid")) {
				i++;
				if (i<args.length) {
					String[] splits = args[i].split(",");
					for (int j=0; j<splits.length; j++)
						try {
							iDGenerationCommandLine.addLimitingCurrentDbId(new Long(splits[j]));
						} catch (NumberFormatException e) {
							iDGenerationCommandLine.handleError("IDGenerationCommandLine: not a valid DB_ID: " + splits[j]);
						}
				} else {
					iDGenerationCommandLine.handleError("IDGenerationCommandLine: missing argument");
				}
			}
			else if (s.equals("-del_ortho")) {
				iDGenerationCommandLine.setDeleteBackToSource(true);
			} else if (s.equals("--help") || s.equals("-help")) {
					iDGenerationCommandLine.printHelp();
			} else if (s.equals("-a"))
				iDGenerationCommandLine.setAllowIDsFromUnspecifiedReleases(false);
			else
				iDGenerationCommandLine.handleError("Unknown argument" + args[i]);
		}
		
		// Alright, puff, pant, we now have all the things we need
		// to know to actually go ahead and generate stable IDs for
		// the new release.
		iDGenerationCommandLine.run();
	}
}
