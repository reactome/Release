/*
 * Created on Feb 14, 2005
 *
 */
package org.gk.slicing;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.PropertyConfigurator;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.StringUtils;
import org.junit.Test;

/**
 * This class is used to take a slice from gk_central for releasing. The slicing works as the following:
 * Note: As of March 8, 2007, _doNotRelease has been changed to _doRelease. The first two steps will check
 * _doRelease instead of _doNotRelease.
 * 1). Prerequirement: _doRelease in events (Pathways and Reactions) should be set correctly.
 * 2). All events with _doRelease true will be in the slice.
 * 3). The reference graph rooted at events in step 2 will be in the slice except events which are handled in step 2.
 * 4). Regulations whose regulatedEvents are in the slice will be in the slice.
 * 5). ConcurrentEventSets whose concurrentEventSets contain instances in the slice will be in the slice.
 * 6). ReactionCoordinates whose locatedEvents are in the slice will be in the slice.
 * 7). All instances should be registered in table DatabaseObject. If not, it will be removed from the slice.
 * This is a database error and should not occur.
 * 8). All attributes referrred by instances in the slice should be in the slice. If not, these references will
 * be removed from attributes.
 * 9). If a species list file is provided, all species and their references will be in the slice (recursively).
 * @author wgm
 */
@SuppressWarnings("unchecked")
public class SlicingEngine {
    // Constants
    private final String DUMP_FILE_NAME = "slicingDump.sql";
    private final String SCHEMA_FILE_NAME = "slicingSchema.sql";
    private final String ONTOLOGY_FILE_NAME = "slicingOntology.sql";
    protected final static String REFERRER_ATTRIBUTE_KEY = "referrers";
    // Source
    protected MySQLAdaptor sourceDBA;
    // target
    private String targetDbHost;
    private String targetDbName;
    private String targetDbUser;
    private String targetDbPwd;
    private int targetDbPort = 3306;
    private MySQLAdaptor targetDBA;
    // For debug
    private boolean debug = false;
    // All instances should be in slicing: key DB_ID value: GKInstance
    private Map eventMap;
    private Map<Long, GKInstance> sliceMap;
    private Map floatingEventMap;
    // To control references checking
    private Set checkedIDs;
    // For referrer checking
    private Map referrersMap;
    // IDs from the top-level pathways
    protected List<Long> topLevelIDs;
    // File name for the top-level events
    protected String processFileName;
    // Release number
    protected String releaseNumber;
    // Release date
    protected String releaseDate;
    // This is used to set releaseStatus
    protected String lastReleaseDate;
    // These two variables are for species
    private List speciesIDs;
    private String speciesFileName;
    // Name for file used for logging validation results. It can be null.
    private String logFileName;
    // To control some parameters for testing
    private boolean isInDev = false;
    private String path = "/usr/local/mysql/bin/";
    
    /**
     * Default constructor
     *
     */
    public SlicingEngine() {
        sliceMap = new HashMap();
        checkedIDs = new HashSet();
    }
    
    /**
     * Set the data source for slicing. Usually this should be gk_central.
     * @param dba
     */
    public void setSource(MySQLAdaptor dba) {
        this.sourceDBA = dba;
    }
    
    /**
     * The name of the target database. This database will be created at the same host
     * as the data source.
     * @param dbName
     */
    public void setTargetDbName(String dbName) {
        this.targetDbName = dbName;
    }
    
    public void setTargetDbHost(String host) {
        this.targetDbHost = host;
    }
    
    public void setTargetDbUser(String dbUser) {
        this.targetDbUser = dbUser;
    }
    
    public void setTargetDbPwd(String pwd) {
        this.targetDbPwd = pwd;
    }
    
    public void setTargetDbPort(int dbPort) {
        this.targetDbPort = dbPort;
    }
    
    public void setProcessFileName(String fileName) {
        this.processFileName = fileName;
    }
    
    public void setSpeciesFileName(String fileName) {
        this.speciesFileName = fileName;
    }
    
    public void setLogFileName(String fileName) {
        this.logFileName = fileName;
    }

    public void setReleaseNumber(String number) {
        this.releaseNumber = number;
    }
    
    public String getReleaseNumber() {
        return this.releaseNumber;
    }
    
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public String getReleaseDate() {
        return this.releaseDate;
    }
    
    public void setLastReleaseDate(String lastReleaseDate) {
        this.lastReleaseDate = lastReleaseDate;
    }
    
    public String getLastReleaseDate() {
        return this.lastReleaseDate;
    }
    
    /**
     * The entry point for slicing. A client to this class should call this method.
     * @throws Exception
     */
    public void slice() throws Exception {
        validateConditions();
        topLevelIDs = getReleasedProcesses();
        speciesIDs = getSpeciesIDs();
        if(!prepareTargetDatabase())
            throw new IllegalStateException("SlicingEngine.slice(): " +
            		"target database cannot be set up.");
        eventMap = extractEvents();
        extractReferences();
        extractRegulations();
        extractConcurrentEventSets();
        extractReactionCoordinates();
        extractSpecies();
        extractPathwayDiagrams();
        PrintStream output = null;
        if (logFileName != null)
            output = new PrintStream(new FileOutputStream(logFileName));
        else
            output = System.err;
        validateExistence(output);
        validateAttributes(output);
        if (logFileName != null)
            output.close(); // Close it if output is opened by the application
        addReleaseStatus();
        // Need to fill values for Complex.includedLocation
        fillIncludedLocationForComplex(output);
        dumpInstances();
        addFrontPage();
        addReleaseNumber();
    }
    
    private void fillIncludedLocationForComplex(PrintStream ps) throws Exception {
        // This is just a sanity check so that this method will not work
        // for some old schema
        GKSchemaClass complexCls = (GKSchemaClass) sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Complex);
        if (!complexCls.isValidAttribute(ReactomeJavaConstants.includedLocation))
            return;
        for (Long dbId : sliceMap.keySet()) {
            GKInstance inst = sliceMap.get(dbId);
            if (!inst.getSchemClass().isa(ReactomeJavaConstants.Complex)) 
                continue;
            // Check if there is any value in the includedLocation. If true,
            // throw exception
            GKInstance includedLocation = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.includedLocation);
            if (includedLocation != null)
                ps.println(inst + " has value in its includedLocation. It should be empty!");
            Set<GKInstance> components = InstanceUtilities.getContainedComponents(inst);
            Set<GKInstance> compartments = new HashSet<GKInstance>();
            for (GKInstance component : components) {
                if (!component.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment)) 
                    continue;
                List<GKInstance> componentCompartments = component.getAttributeValuesList(ReactomeJavaConstants.compartment);
                if (componentCompartments != null)
                    compartments.addAll(componentCompartments);
            }
            List<GKInstance> complexCompartments = inst.getAttributeValuesList(ReactomeJavaConstants.compartment);
            compartments.removeAll(complexCompartments);
            List<GKInstance> list = new ArrayList<GKInstance>(compartments);
            InstanceUtilities.sortInstances(list);
            inst.setAttributeValue(ReactomeJavaConstants.includedLocation,
                                   list);
        }
    }
    
    private void extractPathwayDiagrams() throws Exception {
        PathwayDiagramSlicingHelper diagramHelper = new PathwayDiagramSlicingHelper();
        diagramHelper.isInDev = isInDev;
        for (Long dbID : topLevelIDs) {
            GKInstance process = sliceMap.get(dbID);
            // It may not be a realease ready. A mistake in the
            // topic file.
            if (process == null)
                continue;
            GKInstance diagram = diagramHelper.fetchDiagramForPathway(process, sourceDBA);
            if (diagram == null)
                continue; // Just ignore it for time being
            // Add this diagram to the slice map
            extractReferencesToInstance(diagram);
            // Need to pull up any contained sub-pathway diagrams for this process
            Set<GKInstance> subDiagrams = diagramHelper.loadContainedSubPathways(diagram, sourceDBA);
            for (GKInstance subDiagram : subDiagrams) {
                extractReferencesToInstance(subDiagram);
            }
        }
        // Additional step to remove events that should not be released but in the diagrams for some reasons
        for (Long dbId : sliceMap.keySet()) {
            GKInstance inst = sliceMap.get(dbId);
            if (inst.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                diagramHelper.removeDoNotReleaseEvents(inst, sourceDBA);
            }
        }
        if (debug) {
            System.out.println("extractPathwayDiagrams(): " + sliceMap.size());
        }
    }
    
    public Map extractEvents() throws Exception {
        Map eventMap = new HashMap();
        // To speed up the performance, get all events and their "hasEvent"
        // and "hasInstance" values first
        Collection events = sourceDBA.fetchInstancesByClass("Event");
        SchemaClass cls = sourceDBA.getSchema().getClassByName("Event");
        sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute("precedingEvent"));
        // _doNotRelease has been moved to Event since May, 2005.
        // _doNotRelease has been changed to _doRelease as of March 8, 2007
        sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants._doRelease));
        cls = sourceDBA.getSchema().getClassByName("Pathway");
        sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute(ReactomeJavaConstants.hasEvent));
        cls = sourceDBA.getSchema().getClassByName("ConceptualEvent");
        if (cls != null)
            sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute("hasSpecialisedForm"));
        cls = sourceDBA.getSchema().getClassByName("EquivalentEventSet");
        if (cls != null)
            sourceDBA.loadInstanceAttributeValues(events, cls.getAttribute("hasMember"));
        // Get the list of events based on _doRelease
        GKInstance event = null;
        for (Iterator it = events.iterator(); it.hasNext();) {
            event = (GKInstance) it.next();
            if (shouldEventInSlice(event))
                eventMap.put(event.getDBID(), event);
        }
        return eventMap;        
    }
    
    protected boolean shouldEventInSlice(GKInstance event) throws Exception {
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants._doRelease)) {
            Boolean dr = (Boolean) event.getAttributeValue(ReactomeJavaConstants._doRelease);
            if (dr != null && dr.booleanValue())
                return true;
        }
        return false;
    }
    
    /**
     * A new FrontPage instance should be created and saved into the database.
     */
    private void addFrontPage() {
        try {
            Schema schema = targetDBA.getSchema();
            SchemaClass cls = schema.getClassByName("FrontPage");
            GKInstance frontPage = createInstance(cls);
            // Need to set the attributes for the frontPageItem
            GKInstance process = null;
            for (Long dbID : topLevelIDs) {
                process = targetDBA.fetchInstance(dbID);
                if (process != null) {
                    frontPage.addAttributeValue("frontPageItem", process);
                }
                else
                    System.err.println("Specified top-level event is not in the slice: " + dbID);
            }
            targetDBA.storeInstance(frontPage);
        }
        catch(Exception e) {
            System.err.println("SlicingEngine.addFrontPage(): " + e);
            e.printStackTrace();
        }
    }

    private GKInstance createInstance(SchemaClass cls) {
        GKInstance frontPage = new GKInstance(cls);
        frontPage.setDbAdaptor(targetDBA);
        return frontPage;
    }
    
    /**
     * Add release number into the database: a new table will be created to store the release number
     * information.
     */
    private void addReleaseNumber() {
        try {
            SchemaClass releaseCls = targetDBA.getSchema().getClassByName(ReactomeJavaConstants._Release);
            if (releaseCls == null)
                return; // This is an old schema
            GKInstance release = createInstance(releaseCls);
            release.setAttributeValue(ReactomeJavaConstants.releaseNumber,
                                      new Integer(releaseNumber));
            release.setAttributeValue(ReactomeJavaConstants.releaseDate,
                                      releaseDate);
            targetDBA.storeInstance(release);
        }
        catch(Exception e) {
            System.err.println("SlicingEngine.addReleaseNumber(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Add values for releaseStatus. Old values in the release status should be removed first. The method works as follows,
     * which is based on original Gavin's implementation in the server side, EventHierarchy.java.
     * 1). Remove any _releaseStatus values in each Event instance.
     * 2). Check each Event instance and compare its releaseDate to the lastPreviousDate provided in the property
     * file.
     * 3). Results from 2: if releaseDate is after previousReleaseDate, label events as NEW. Otherwise, leave it blank.
     * 4). Check each Event that hasEvent as its valid attribute (aka Pathway instance) to see if this Event has not labeled
     * as "New". If any sub-pathway has been labeled as "NEW", label it as "UPDATED".
     */
    private void addReleaseStatus() throws Exception {
        // Check if new releaseStatus is supported
        SchemaClass eventCls = sourceDBA.getSchema().getClassByName(ReactomeJavaConstants.Event);
        if (!eventCls.isValidAttribute(ReactomeJavaConstants.releaseStatus))
            return; // This schema doesn't support the releaseStatus attribute.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date lastRelease = dateFormat.parse(lastReleaseDate);
        // Reset all flags: actually these values should be null in gk_central always
        // Assign NEW if the releaseDate in the Event is later than lastRelease
        for (Long dbId : sliceMap.keySet()) {
            GKInstance instance = sliceMap.get(dbId);
            if (!instance.getSchemClass().isa(ReactomeJavaConstants.Event))
                continue;
            instance.setAttributeValue(ReactomeJavaConstants.releaseStatus, null);
            String releaseDateValue = (String) instance.getAttributeValue(ReactomeJavaConstants.releaseDate);
            if (releaseDateValue == null)
                continue;
            Date releaseDate = dateFormat.parse(releaseDateValue);
            if (releaseDate.after(lastRelease)) {
                instance.setAttributeValue(ReactomeJavaConstants.releaseStatus, "NEW");
            }
        }
        // Assign UPDATED flag for event container that has its child event has been labeled "NEW".
        for (Long dbId : sliceMap.keySet()) {
            GKInstance instance = sliceMap.get(dbId);
            if (!instance.getSchemClass().isa(ReactomeJavaConstants.Event))
                continue;
            if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                continue;
            String releaseStatus = (String) instance.getAttributeValue(ReactomeJavaConstants.releaseStatus);
            if (releaseStatus != null)
                continue;
            boolean isUpdated = checkIsUpdated(instance);
            if (isUpdated)
                instance.setAttributeValue(ReactomeJavaConstants.releaseStatus, "UPDATED");
        }
    }
    
    /**
     * This recursive method can be used for an Event instance that has no releastStatus assigned yet.
     * @param event
     * @return
     * @throws Exception
     */
    private boolean checkIsUpdated(GKInstance event) throws Exception {
        if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)) {
            List<?> hasEvents = event.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
            if (hasEvents == null || hasEvents.size() == 0)
                return false;
            for (Iterator<?> it = hasEvents.iterator(); it.hasNext();) {
                GKInstance subEvent = (GKInstance) it.next();
                String subReleaseStatus = (String) subEvent.getAttributeValue(ReactomeJavaConstants.releaseStatus);
                if (subReleaseStatus != null && 
                   (subReleaseStatus.equals("NEW") || subReleaseStatus.equals("UPDATED")))
                    return true;
                boolean subReturned = checkIsUpdated(subEvent);
                if (subReturned)
                    return true;
            }
        }
        return false;
    }
    
    @Test
    public void testAddReleaseNumber() throws Exception {
        targetDBA = new MySQLAdaptor("localhost",
                                     "gk_current_ver37",
                                     "root", 
                                     "macmysql01");
        releaseNumber = "37";
        addReleaseNumber();
    }
    
    private void extractConcurrentEventSets() throws Exception {
        SchemaClass cls = sourceDBA.getSchema().getClassByName("ConcurrentEventSet");
        Collection ccEventSets = sourceDBA.fetchInstancesByClass(cls);
        sourceDBA.loadInstanceAttributeValues(ccEventSets, cls.getAttribute("concurrentEvents"));
        List ccEvents = null;
        GKInstance ccEventSet = null;
        for (Iterator it = ccEventSets.iterator(); it.hasNext();) {
            ccEventSet = (GKInstance) it.next();
            ccEvents = ccEventSet.getAttributeValuesList("concurrentEvents");
            if (ccEvents == null || ccEvents.size() == 0)
                continue;
            for (Iterator it1 = ccEvents.iterator(); it1.hasNext();) {
                GKInstance event = (GKInstance) it1.next();
                if (sliceMap.containsKey(event.getDBID())) {
                    extractReferencesToInstance(ccEventSet);
                    break;
                }
            }
        }
        if (debug)
            System.out.println("extractConcurrentEventSets: " + sliceMap.size() + " instances.");
    }
    
//    /**
//     * Put the FrontPage instance into the slice
//     * @throws Exception
//     */
//    private void extractFrontPage() throws Exception {
//        GKInstance frontPage = sourceDBA.fetchInstance("FrontPage", frontPageID);
//        if (frontPage != null)
//            extractReferencesToInstance(frontPage);
//    }
    
    /**
     * Get the containers in the floating events list if any of contained events
     * are in the slice.
     */
    private void extractReferrers() throws Exception {
        GKInstance instance = null;
        List referrers = null;
        GKInstance referrer = null;
        Long dbID = null;
        for (Iterator it = referrersMap.keySet().iterator(); it.hasNext();) {
            dbID = (Long) it.next();
            if (sliceMap.containsKey(dbID)) {
                instance = (GKInstance) referrersMap.get(dbID);
                // Take the referrers
                referrers = instance.getAttributeValuesListNoCheck(REFERRER_ATTRIBUTE_KEY);
                if (referrers != null) {
                    for (Iterator it1 = referrers.iterator(); it1.hasNext();) {
                        referrer = (GKInstance) it1.next();
                        extractReferencesToInstance(referrer);
                    }
                }
            }
        }
        if (debug)
            System.out.println("extractReferrers: " + sliceMap.size() + " instances.");
    }
    
    /**
     * Make sure all conditions are correct.
     *
     */
    private void validateConditions() {
        if (sourceDBA == null)
            throw new IllegalStateException("SlicingEngine.validateConditions(): " +
            		"source database is not specified.");
        if (targetDbName == null)
            throw new IllegalStateException("SlicingEngine.validateCondition(): " +
            		"target database is not specified.");
        // Target database should not the the same as source database
        if (targetDbHost.equals(sourceDBA.getDBHost()) &&
            targetDbName.equals(sourceDBA.getDBName()))
            throw new IllegalStateException("SlicingEngine.validateConditions(): " +
            		"source and target database are the same. This is not allowed.");
        // FronPageID should not be null
        if (processFileName == null)
            throw new IllegalStateException("SlicingEngine.validateConditions(): " +
            		                        "File for the list of releasing events is not specified.");
        if (releaseNumber == null || releaseNumber.length() == 0)
            throw new IllegalStateException("SlicingEngine.validateConditions(): " +
                                            "releaseNumber is not specified.");
    }
    
    /**
     * This check is to prevent some database errors: an Instance is used
     * in attribute table but this instance is not registered in DatabaseObject
     * table. Such instances should not be in the slice.
     * @throws SQLException
     */
	private void validateExistence(PrintStream output) throws SQLException {
	    SchemaClass root = ((GKSchema)sourceDBA.getSchema()).getRootClass();
	    List dbIDs = new ArrayList(sliceMap.keySet());
		String query = "SELECT DB_ID FROM " + root.getName() + " WHERE DB_ID IN (" + 
		                StringUtils.join(",", dbIDs) + ")";
		Set idsInDB = new HashSet();
		Statement stat = sourceDBA.getConnection().createStatement();
		ResultSet resultSet = stat.executeQuery(query);
	    while (resultSet.next()) {
	        long id = resultSet.getLong(1);
	        idsInDB.add(new Long(id));
	    }
	    resultSet.close();
	    stat.close();
	    dbIDs.removeAll(idsInDB);
	    Long dbID = null;
	    for (Iterator it = dbIDs.iterator(); it.hasNext();) {
	        dbID = (Long) it.next();
	        sliceMap.remove(dbID);
	        output.println("Instance with DB_ID \"" + dbID + "\" " +
	        		           "is used but not in table DatabaseObject!");
	    }
	    if (debug) {
	        System.out.println("validateExitence(): " + sliceMap.size() + " instances.");
	    }
	}

    
    /**
     * Make sure all instance references in the slice. If not, those references should be 
     * removed from the attribute list. This check is used in case there are some database
     * errors in the source. Another case is an unrelease event is used by a released event.
     * @throws Exception
     */
    private void validateAttributes(PrintStream output) throws Exception {
        GKInstance instance = null;
        List values = null;
        Long dbID = null;
        GKInstance ref = null;
        SchemaAttribute att = null;
        for (Iterator it = sliceMap.keySet().iterator(); it.hasNext();) {
            dbID = (Long) it.next();
            instance = (GKInstance) sliceMap.get(dbID);
            instance.setIsInflated(true); // To prevent to fetch values again.
            for (Iterator it1 = instance.getSchemClass().getAttributes().iterator(); it1.hasNext();) {
                att = (GKSchemaAttribute) it1.next();
                if (!att.isInstanceTypeAttribute())
                    continue;
                values = instance.getAttributeValuesList(att);
                if (values == null || values.size() == 0)
                    continue;
                for (Iterator it2 = values.iterator(); it2.hasNext();) {
                    ref = (GKInstance) it2.next();
                    if (!sliceMap.containsKey(ref.getDBID())) {
                        it2.remove();
                        output.println("\"" + ref.toString() + "\" in \"" + att.getName() + "\" for \"" + instance + 
                                           "\" is not in the slice and removed from the attribute list!");
                    }
                }
            }
        }
    }
    
    /**
     * Save GKInstances in the slice to the target database.
     * 
     * @throws Exception
     */
    private void dumpInstances() throws Exception {
        long time1 = System.currentTimeMillis();
        targetDBA = new MySQLAdaptor(targetDbHost,
                                     targetDbName, 
                                     targetDbUser, 
                                     targetDbPwd, 
                                     targetDbPort);
        // Try to use transaction
        boolean isTnSupported = targetDBA.supportsTransactions();
        if (isTnSupported)
            targetDBA.startTransaction();
        try {
            for (Long dbId : sliceMap.keySet()) {
                GKInstance instance = (GKInstance) sliceMap.get(dbId);
                storeInstance(instance, targetDBA);
            }
            if (isTnSupported)
                targetDBA.commit();
        }
        catch (Exception e) {
            if (isTnSupported)
                targetDBA.rollback();
            System.err.println("SlicingEngine.dumpInstances(): " + e);
            e.printStackTrace();
        }
        long time2 = System.currentTimeMillis();
        System.out.println("Time for dumpInstances(): " + (time2 - time1));
    }
    
    /**
     * This method is copied from MySQLAdaptor.storeInstance(GKInstance, boolean). However, storing is done
     * without recursiveness here.
     * @param instance
     * @throws Exception
     */
	public void storeInstance(GKInstance instance, MySQLAdaptor targetDBA) throws Exception {
        Long dbID = instance.getDBID();
        SchemaClass cls = instance.getSchemClass();
        GKSchema schema = (GKSchema) targetDBA.getSchema();
        // Change class to the target
        cls = schema.getClassByName(cls.getName());
        SchemaClass rootCls = schema.getRootClass();
        List classHierarchy = new ArrayList();
        classHierarchy.addAll(cls.getOrderedAncestors());
        classHierarchy.add(cls);
        Collection later = new ArrayList();
        for (Iterator ancI = classHierarchy.iterator(); ancI.hasNext();) {
            GKSchemaClass ancestor = (GKSchemaClass) ancI.next();
            StringBuffer statement = new StringBuffer("INSERT INTO " + ancestor.getName()
                            + " SET DB_ID=?");
            List values = new ArrayList();
            values.add(dbID);
            if (ancestor == rootCls) {
                statement.append(",_class=?");
                values.add(cls.getName());
            }
            Collection multiAtts = new ArrayList();
            for (Iterator attI = ancestor.getOwnAttributes().iterator(); attI.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) attI.next();
                if (att.getName().equals("DB_ID"))
                    continue;
                List attVals = instance.getAttributeValuesList(att.getName());
                if ((attVals == null) || (attVals.isEmpty()))
                    continue;
                if (att.isMultiple()) {
                    multiAtts.add(att);
                }
                else {
                    if (att.isInstanceTypeAttribute()) {
                        if (ancestor == rootCls) {
                            later.add(att);
                        }
                        else {
                            GKInstance val = (GKInstance) attVals.get(0);
                            statement.append("," + att.getName() + "=?");
                            values.add(val.getDBID());
                            statement.append("," + att.getName() + "_class=?");
                            values.add(val.getSchemClass().getName());
                        }
                    }
                    else {
                        statement.append("," + att.getName() + "=?");
                        values.add(instance.getAttributeValue(att.getName()));
                    }
                }
            }
            //System.out.println(instance + ": " + statement.toString() + "\n");
            PreparedStatement ps = targetDBA.getConnection().prepareStatement(statement.toString());
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
                }
                else {
                    throw (new Exception("Unable to get autoincremented value."));
                }
            }
            for (Iterator attI = multiAtts.iterator(); attI.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) attI.next();
                List attVals = instance.getAttributeValuesList(att.getName());
                StringBuffer statement2 = new StringBuffer("INSERT INTO " + ancestor.getName()
                                + "_2_" + att.getName() + " SET DB_ID=?," + att.getName() + "=?,"
                                + att.getName() + "_rank=?");
                if (att.isInstanceTypeAttribute()) {
                    statement2.append("," + att.getName() + "_class=?");
                }
                //System.out.println(statement2.toString() + "\n");
                PreparedStatement ps2 = targetDBA.getConnection().prepareStatement(
                                statement2.toString());
                for (int i = 0; i < attVals.size(); i++) {
                    ps2.setObject(1, dbID);
                    ps2.setInt(3, i);
                    if (att.isInstanceTypeAttribute()) {
                        GKInstance attVal = (GKInstance) attVals.get(i);
                        ps2.setObject(2, attVal.getDBID());
                        ps2.setString(4, attVal.getSchemClass().getName());
                    }
                    else {
                        ps2.setObject(2, attVals.get(i));
                    }
                    ps2.executeUpdate();
                }
            }
        }
        if (!later.isEmpty()) {
            StringBuffer statement = new StringBuffer("UPDATE " + rootCls.getName() + " SET ");
            List values = new ArrayList();
            for (Iterator li = later.iterator(); li.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) li.next();
                statement.append(att.getName() + "=?, " + att.getName() + "_class=?");
                if (li.hasNext())
                    statement.append(",");
                GKInstance value = (GKInstance) instance.getAttributeValue(att.getName());
                values.add(value.getDBID());
                values.add(value.getSchemClass().getName());
            }
            statement.append(" WHERE DB_ID=?");
            values.add(dbID);
            //System.out.println(statement.toString() + "\n");
            PreparedStatement ps = targetDBA.getConnection().prepareStatement(statement.toString());
            for (int i = 0; i < values.size(); i++) {
                ps.setObject(i + 1, values.get(i));
            }
            ps.executeUpdate();
        }
    }

    
    /**
     * Take only those cooridates whose locatedEvent are in the slice.
     *
     */
    private void extractReactionCoordinates() throws Exception {
        SchemaClass cls = sourceDBA.getSchema().getClassByName("ReactionCoordinates");
        Collection reactionCoordinates = sourceDBA.fetchInstancesByClass("ReactionCoordinates");
        sourceDBA.loadInstanceAttributeValues(reactionCoordinates, cls.getAttribute("locatedEvent"));
        GKInstance reactionCoordinate = null;
        GKInstance locatedEvent = null;
        for (Iterator it = reactionCoordinates.iterator(); it.hasNext();) {
            reactionCoordinate = (GKInstance) it.next();
            locatedEvent = (GKInstance) reactionCoordinate.getAttributeValue("locatedEvent");
            if (locatedEvent == null) {
                System.err.println("SlicingEngine.checkReactionCooridnates(): " + 
                                   reactionCoordinate.getDBID() + " has no locatedEvent!");
                continue; // Escape
            }
            if (sliceMap.containsKey(locatedEvent.getDBID()))
                extractReferencesToInstance(reactionCoordinate);
        }
        if (debug)
            System.out.println("extractReactionCoordinates: " + sliceMap.size() + " instances.");
    }
    
    public List getSpeciesIDs() throws IOException {
        if (speciesFileName == null)
            return null;
        File file = new File(speciesFileName);
        if (!file.exists()) {
            throw new IllegalStateException("SlicingEngine.getSpeciesIDs(): " +
                                            "specified file for species doesn't exist!");
        }
        return extractIDsFromFile(file);
    }
    
    /**
     * Get the species list provided by an external file and extract species if they are not in.
     * @throws Exception
     */
    private void extractSpecies() throws Exception {
        if (speciesIDs == null || speciesIDs.size() == 0)
            return; // Nothing to do
        GKInstance species = null;
        Long dbId = null;
        for (Iterator it = speciesIDs.iterator(); it.hasNext();) {
            dbId = (Long) it.next();
            species = sourceDBA.fetchInstance(dbId);;
            if (species != null)
                extractReferencesToInstance(species);
        }
    }
    
    /**
     * Go through the whole list of Regulation instances to see if any instances 
     * whose regulatedEntity values are in the slice. If true, those instances
     * should be in the slice.
     */
    protected void extractRegulations() throws Exception {
        SchemaClass cls = sourceDBA.getSchema().getClassByName("Regulation");
        Collection regulations = sourceDBA.fetchInstancesByClass("Regulation");
        sourceDBA.loadInstanceAttributeValues(regulations, cls.getAttribute("regulatedEntity"));
        GKInstance regulation = null;
        GKInstance regulatedEntity = null;
        for (Iterator it = regulations.iterator(); it.hasNext();) {
            regulation = (GKInstance) it.next();
            regulatedEntity = (GKInstance) regulation.getAttributeValue("regulatedEntity");
            if (regulatedEntity == null)
                continue;
            if (sliceMap.containsKey(regulatedEntity.getDBID())) {
                extractReferencesToInstance(regulation);
            }
        }
        if (debug)
            System.out.println("extractRegulations: " + sliceMap.size() + " instances.");
    }
    
    private void checkFollowingEventsInFloating() throws Exception {
        if (floatingEventMap == null || floatingEventMap.size() == 0)
            return;
        GKInstance reaction = null;
        Long dbID = null;
        List values = null;
        GKInstance ref = null;
        List dbIDs = new ArrayList(floatingEventMap.keySet());
        int preCount = dbIDs.size();
        while (true) {
            for (Iterator it = dbIDs.iterator(); it.hasNext();) {
                dbID = (Long) it.next();
                reaction = (GKInstance) floatingEventMap.get(dbID);
                values = reaction.getAttributeValuesList("precedingEvent");
                if (values == null || values.size() == 0)
                    continue;
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    ref = (GKInstance) it1.next();
                    if (sliceMap.containsKey(ref.getDBID())) {
                        extractReferencesToInstance(reaction);
                        it.remove();
                        break;
                    }
                }
            }
            if (dbIDs.size() == preCount)
                break;
            preCount = dbIDs.size();
        }
        if (debug)
            System.out.println("checkFollowingEvents: " + sliceMap.size() + " instances.");
    }
    
    
    private void extractReferences() throws Exception {
        // Check all references in the events
        GKInstance instance = null;
        Long dbID = null;
        long time1 = System.currentTimeMillis();
        for (Iterator it = eventMap.keySet().iterator(); it.hasNext();) {
            dbID = (Long) it.next();
            instance = (GKInstance) eventMap.get(dbID);
            extractReferencesToInstance(instance);
            if (debug)
                System.out.println("Total touched instances: " + checkedIDs.size());
        }
        if (debug)
            System.out.println("extractReferences(): " + sliceMap.size() + " instances");
        System.out.println("Time for extractReferences: " + (System.currentTimeMillis() - time1));
    }
    
    private void extractReferencesToInstance(GKInstance instance) throws Exception {
        Set current = new HashSet();
        Set next = new HashSet();
        current.add(instance);
        GKInstance tmp = null;
        List values = null;
        GKSchemaAttribute att = null;
        if (debug)
            System.out.println("Instance: " + instance);
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                tmp = (GKInstance) it.next();
                if (checkedIDs.contains(tmp.getDBID()))
                    continue; // It has been checked
                checkedIDs.add(tmp.getDBID());
                if (tmp.getSchemClass() == null)
                    System.out.println("Current event: " + tmp.getDBID());
                // Check if an event should be in a slice
                if (tmp.getSchemClass().isa("Event") &&
                    !eventMap.containsKey(tmp.getDBID())) 
                        continue;
                pushToMap(tmp, sliceMap);
                // Load all instances
                sourceDBA.loadInstanceAttributeValues(tmp);
                for (Iterator it1 = tmp.getSchemaAttributes().iterator(); it1.hasNext();) {
                    att = (GKSchemaAttribute) it1.next();
                    if (!att.isInstanceTypeAttribute())
                        continue;
                    values = tmp.getAttributeValuesList(att);
                    if (values == null || values.size() == 0)
                        continue;
                    for (Iterator it2 = values.iterator(); it2.hasNext();) {
                        GKInstance reference = (GKInstance) it2.next();
                        if (reference.getSchemClass() == null)
                            System.out.println("reference is wrong: " + reference);
                        if (checkedIDs.contains(reference.getDBID()))
                            continue;
                        next.add(reference);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
            if (debug)
                System.out.println("Current: " + current.size());
        }
    }
    
    private void pushToMap(GKInstance instance, Map map) {
        if (map.containsKey(instance.getDBID()))
            return;
        map.put(instance.getDBID(), instance);
    }
    
    /**
     * Get the list of top-level process names for slicing.
     * @return a list of DB_IDs 
     */
    protected List getReleasedProcesses() throws Exception {
        if (processFileName == null)
            throw new IllegalStateException("SlicingEngine.getReleasedProcesses(): " +
            		                        "releasing processes file is not specified.");
        File file = new File(processFileName);
        if (!file.exists()) {
            throw new IllegalStateException("SlicingEngine.getReleasedProcesses(): " +
            		                        "specified file for releasing processes doesn't exist!");
        }
        return extractIDsFromFile(file);
    }
    
    private List<Long> extractIDsFromFile(File file) throws IOException {
        List<Long> ids = new ArrayList<Long>();
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        String[] tokens = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.length() == 0)
                break;
            tokens = line.split("\\s");
            ids.add(new Long(tokens[0]));
        }
        return ids;
    }
    
    private boolean prepareTargetDatabase() throws Exception {
        if (sourceDBA == null)
            throw new IllegalStateException("SlicingEngine.prepareTargetDatabase(): source database is not specified.");
        if(!runDumpCommand(null, DUMP_FILE_NAME))
            return false;
        if(!runDumpCommand("DataModel", SCHEMA_FILE_NAME))
            return false;
        if (!runDumpCommand("Ontology", ONTOLOGY_FILE_NAME))
            return false;
        if(!createTargetDatabase())
            return false;
        if(!runImport(DUMP_FILE_NAME))
            return false;
        if(!runImport(SCHEMA_FILE_NAME))
            return false;
        if (!runImport(ONTOLOGY_FILE_NAME))
            return false;
        return true;
    }
    
    private boolean runImport(String fileName) throws Exception {
        StringBuilder importCommand = new StringBuilder();
        if (isInDev && path != null)
            importCommand.append(path);
        importCommand.append("mysql ");
        attachConnectInfo(importCommand,
                          targetDbHost,
                          targetDbUser,
                          targetDbPwd);
        importCommand.append(" ");
        importCommand.append(targetDbName);
        if (debug)
            System.out.println("runImport: " + importCommand.toString());
        Process process = Runtime.getRuntime().exec(importCommand.toString());
        OutputStream output = process.getOutputStream();
        InputStream input = new FileInputStream(fileName);
        pipe(input, output);
        String errorMessage = getErrorMessage(process);
        process.destroy();
        if (errorMessage.length() == 0)
            return true;
        return false;
    }
    
    private String getErrorMessage(Process process) throws Exception {
        InputStream is = process.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuffer buffer = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
            buffer.append("\n");
        }
        reader.close();
        is.close();
        System.err.println(buffer.toString());
        return buffer.toString();
    }
    
    private void attachConnectInfo(StringBuilder buffer,
                                   MySQLAdaptor dba) {
        attachConnectInfo(buffer, 
                          dba.getDBHost(),
                          dba.getDBUser(),
                          dba.getDBPwd());
    }
    
    private void attachConnectInfo(StringBuilder buffer,
                                   String dbHost,
                                   String dbUser,
                                   String dbPwd) {
        buffer.append("-h");
        buffer.append(dbHost);
        buffer.append(" -u");
        buffer.append(dbUser);
        buffer.append(" -p");
        buffer.append(dbPwd);
    }
    
    private boolean createTargetDatabase() throws Exception {
        StringBuilder createDB = new StringBuilder();
        if (isInDev && path != null)
            createDB.append(path);
        createDB.append("mysqladmin ");
        attachConnectInfo(createDB, 
                          targetDbHost,
                          targetDbUser,
                          targetDbPwd);
        createDB.append(" create ");
        createDB.append(targetDbName);
        if (debug) {
            System.out.println("createTargetDatabase: " + createDB.toString());
        }
        Process process = Runtime.getRuntime().exec(createDB.toString());
        String message = getErrorMessage(process);
        process.waitFor();
        process.destroy();
        if (message.length() > 0)
            return false;
        return true;
    }
    
    private void pipe(InputStream input, OutputStream output) throws Exception {
        // Reader and writer should not be used here since ad hoc bytes might be
        // in the database (e.g. Ontology table).
        ReadableByteChannel source = Channels.newChannel(input);
        ByteBuffer buffer = ByteBuffer.allocate(10 * 1024); // 10k
        WritableByteChannel sink = Channels.newChannel(output);
        int tmp = 0;
        while ((tmp = source.read(buffer)) > 0) {
            buffer.flip();
            sink.write(buffer);
            buffer.clear();
        }
        sink.close();
        output.close();
        source.close();
        input.close();
    }
    
    private boolean runDumpCommand(String tableName, String dumpFileName) throws Exception {
        StringBuilder mysqldump = new StringBuilder();
        if (isInDev && path != null)
            mysqldump.append(path);
        if (tableName == null)
            mysqldump.append("mysqldump --skip-lock-tables -d ");
        else
            mysqldump.append("mysqldump --skip-lock-tables -q --add-drop-table -e --add-locks ");
        attachConnectInfo(mysqldump, sourceDBA);
        mysqldump.append(" ");
        mysqldump.append(sourceDBA.getDBName());
        if (tableName != null) {
            mysqldump.append(" ");
            mysqldump.append(tableName);
        }
        // These cannot work
        //mysqldump.append(" > ");
        //mysqldump.append(dumpFileName);
        System.out.println("runDumpCommand: " + mysqldump.toString());
        Process process = Runtime.getRuntime().exec(mysqldump.toString());
        InputStream input = process.getInputStream();
        OutputStream output = new FileOutputStream(dumpFileName);
        pipe(input, output);
        String error = getErrorMessage(process);
        process.destroy();
        if (error.length() > 0)
            return false;
        return true;
    }

    public static void main(String[] args) {
        // Set up log4j
        PropertyConfigurator.configure("SliceLog4j.properties");
        // Get information from the command line
        //SlicingEngine engine = new SlicingEngine();
        // Try project based slicing engine
        SlicingEngine engine = new ProjectBasedSlicingEngine();
        try {
            Properties properties = loadProperties();
            // Check if it is in development mode
            String isInDev = properties.getProperty("isInDev");
            if (isInDev != null && isInDev.equals("true"))
                engine.isInDev = true;
            // Check if use for species is set
            String useForSpecies = properties.getProperty("useForSpecies");
            if (useForSpecies != null && useForSpecies.equals("true"))
                ((ProjectBasedSlicingEngine)engine).setUseForSpecies(true);
            String dbHost = properties.getProperty("dbHost");
            if (dbHost == null || dbHost.trim().length() == 0)
                dbHost = getInput("Please input the database host");
            String dbName = properties.getProperty("dbName");
            if (dbName == null || dbName.trim().length() == 0)
                dbName = getInput("Please input the source database name");
            String dbPort = properties.getProperty("dbPort");
            if (dbPort == null || dbPort.trim().length() == 0)
                dbPort = getInput("Please input the source databse port");
            String user = properties.getProperty("dbUser");
            if (user == null || user.trim().length() == 0)
                user = getInput("Please input the user name");
            String pwd = properties.getProperty("dbPwd");
            if (pwd == null || pwd.trim().length() == 0)
                pwd = getInput("Please input the password");
            String targetDbHost = properties.getProperty("slicingDbHost");
            if (targetDbHost == null || targetDbHost.trim().length() == 0)
                targetDbHost = getInput("Please input the slice databse host");
            String targetdbName = properties.getProperty("slicingDbName");
            if (targetdbName == null || targetdbName.trim().length() == 0)
                targetdbName = getInput("Please input the slice database name");
            String targetDbUser = properties.getProperty("slicingDbUser");
            if (targetDbUser == null || targetDbUser.trim().length() == 0)
                targetDbUser = getInput("Please input the slice database user");
            String targetDbPwd = properties.getProperty("slicingDbPwd");
            if (targetDbPwd == null || targetDbPwd.trim().length() == 0)
                targetDbPwd = getInput("Please input the slice database password");
            String targetDbPort = properties.getProperty("slicingDbPort");
            if (targetDbPort == null || targetDbPort.trim().length() == 0)
                targetDbPort = getInput("Please input the slice database port");
            String fileName = properties.getProperty("releaseTopicsFileName");
            if (fileName == null || fileName.trim().length() == 0)
                fileName = getInput("Please input the file name for releasing processes");
            String releaseNumber = properties.getProperty("releaseNumber");
            if (releaseNumber == null || releaseNumber.trim().length() == 0)
                releaseNumber = getInput("Please input the release number (e.g. 38)");
            String releaseDate = properties.getProperty("releaseDate");
            if (releaseDate == null || releaseDate.trim().length() == 0)
                releaseDate = getInput("Please input the current release date in the format of YYYY-MM-DD");
            String lastReleaseDate = properties.getProperty("lastReleaseDate");
            if (lastReleaseDate == null || lastReleaseDate.trim().length() == 0)
                lastReleaseDate = getInput("Please input the last release date in the format of YYYY-MM-DD");
            String speciesFileName = properties.getProperty("speciesFileName");
            if (speciesFileName == null || speciesFileName.trim().length() == 0)
                speciesFileName = getInput("If you want to extract species, please input file name for the species list. " +
                    "Otherwise, please input \"p\" to pass");
            if (speciesFileName.equalsIgnoreCase("p"))
                speciesFileName = null;
            String logFileName = properties.getProperty("logFileName");
            if (logFileName == null || logFileName.trim().length() == 0)
                logFileName = getInput("Please input the file name for logging validation results. " +
                    "Please input \"c\" to direct the validation results to the console");
            if (logFileName.equalsIgnoreCase("c"))
                logFileName = null;
            MySQLAdaptor sourceDBA = new MySQLAdaptor(dbHost,
                                                      dbName,
                                                      user,
                                                      pwd,
                                                      Integer.parseInt(dbPort));
//            MySQLAdaptor sourceDBA = new MySQLAdaptor("localhost",
//                                                      "pre_ver12",
//                                                      "wgm",
//                                                      "wgm",
//                                                      3310);
//            String fpIDStr = "158541";
            engine.setSource(sourceDBA);
            engine.setTargetDbName(targetdbName);
            engine.setTargetDbHost(targetDbHost);
            engine.setTargetDbUser(targetDbUser);
            engine.setTargetDbPwd(targetDbPwd);
            engine.setTargetDbPort(new Integer(targetDbPort));
            //engine.setFrontPageID(new Long(fpIDStr));
            engine.setProcessFileName(fileName);
            engine.setReleaseNumber(releaseNumber);
            engine.setReleaseDate(releaseDate);
            engine.setLastReleaseDate(lastReleaseDate);
            engine.setSpeciesFileName(speciesFileName);
            engine.setLogFileName(logFileName);
            engine.slice();
        }
        catch (Exception e) {
            System.err.println("SlicingEnginee.main(): " + e);
            e.printStackTrace();
        }
    }
    
    private static Properties loadProperties() throws IOException {
        FileInputStream fis = new FileInputStream("slicingTool.prop");
        Properties properties = new Properties();
        properties.load(fis);
        fis.close();
        return properties;
    }
    
    private static String getInput(String message) throws Exception {
        System.out.println(message + ": ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = reader.readLine();
        while (input == null || input.length() == 0) {
            System.out.println(message + " (type q or quit for aborting): ");
            input = reader.readLine();
        }
        if (input.equals("q") || input.equals("quit") || input.equals("e") || input.equals("exit"))
            System.exit(0); // Quit
        System.out.println(message + ": " + input);
        return input;
    }
    
    @Test
    public void testTopics() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("reactomedev.oicr.on.ca",
                                            "test_gk_central",
                                            "authortool",
                                            "**REMOVED**",
                                            3306);
        processFileName = "ver28_topics.txt";
        List list = getReleasedProcesses();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Long id = (Long) it.next();
            GKInstance instance = dba.fetchInstance(id);
            System.out.println(instance);
        }
    }
    
    @Test
    public void testInsert() throws Exception {
        MySQLAdaptor dba = new MySQLAdaptor("localhost", 
                                            "test_gk_central_slice",
                                            "root", 
                                            "macmysql01");
        Connection connection = dba.getConnection();
        String insert = "INSERT INTO Event SET DB_ID=?, _doRelease=?";
        PreparedStatement stat = connection.prepareStatement(insert);
        stat.setObject(1, "508370");
        stat.setObject(2, Boolean.FALSE);
        stat.executeUpdate();
        connection.close();
    }
}