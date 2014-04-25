/*
 * Created on Dec 29, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.model.AttributeClassNotAllowedException;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PathwayDiagramInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Methods for synchronizing the local and the db repositories. The functions of this class
 * is similar to org.gk.persistence.PersistenceManager. However, there are some GUIs in org.gk.database
 * to be needed, so it is used as an independent class in the current package.
 * When a newly created GKInstance is checked into the database, the database will be searched for a matched
 * GKInstance object. If such an instance can be found, the local repository will be checked to see if this
 * instance has been alreay checked out. If this instance has been checked out, identity will be checked. 
 * If the local and db copies are not the same, the user will be asked to compare the local and db copy. He/She
 * should do merge, update or check-in. Afterwards, another matching will be carried out to see if the original
 * matched instance is still matched. If true, the selected action (one of Merge, Use DB Copy) will be carried 
 * out. Otherwise, the user will get another window for selection. This schema can also be applied in a batch mode 
 * (i.e. for a list of GKInstance objects).
 * @author wugm
 */
public class SynchronizationManager {
	// Actions for matched instances
	private final int SAVE_AS_NEW_ACTION = 0;
	private final int USE_DB_COPY_ACTION = 1;
	private final int MERGE_ACTION = 2;
	private String[] actions = {"Save as New",
								"Use the Selected",
								"Merge New to Selected"};
	private DefaultInstanceEditHelper defaultIEHelper = new DefaultInstanceEditHelper();
	// For transaction setting
	private Properties prop;
	// A parent component to be used by JOptionPane to display some message
	private Component parentComp;
    // A flag that is used to allow commit local instances having
    // unexpected InstanceEdit (e.g. Instances from MOD)
    private boolean isLocalHasUnexpIECommitAllowed = false;
	
	private static SynchronizationManager manager;
	
	public static SynchronizationManager getManager() {
		if (manager == null)
			manager = new SynchronizationManager();
		return manager;
	}
	
	private SynchronizationManager() {
	    checkIsLocalUnExpAllowed();
    }
    
    public boolean isLocalHasUnexpIECommitAllowed() {
        return this.isLocalHasUnexpIECommitAllowed;
    }
    
    private void checkIsLocalUnExpAllowed() {
        isLocalHasUnexpIECommitAllowed = false;
        try {
            InputStream metaConfig = GKApplicationUtilities.getConfig("curator.xml");
            if (metaConfig == null)
                return;
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(metaConfig);
            Element elm = (Element) XPath.selectSingleNode(doc.getRootElement(), 
                                                           "enableCommitLocalHasMoreInstance");
            if (elm == null)
                return;
            String value = elm.getText();
            isLocalHasUnexpIECommitAllowed = new Boolean(value).booleanValue();
        }
        catch (IOException e) {
            // Don't do anything if there is an exception.
            e.printStackTrace();
        }
        catch (JDOMException e) {
            e.printStackTrace();
        }
    }
	
	public void setProperties(Properties prop) {
		this.prop = prop;
	}
	
	public void setParentComponent(Component comp) {
	    this.parentComp = comp;
	}
	
	public Component getParentComponent() {
	    return parentComp;
	}
	
	/**
	 * Get the default InstanceEdit. When one or some GKInstances are checked into the database,
	 * a new InstanceEdit object (actually a GKInstance) will be generated based on the previous
	 * one. This new InstanceEdit has the check-in dateTime assigned so that the client can call
	 * this method to get the current default InstanceEdit. However, if no default InstanceEdit
	 * is assigned, this method will return null.
	 * @return a GKInstance for the default InstanceEdit will the nearest check-in dateTime. It
	 * might return null.
	 */
	public GKInstance getDefaultInstanceEdit(Window parentDialog) {
		return defaultIEHelper.getDefaultInstanceEdit(parentDialog);
	}	
	
	public GKInstance getDefaultInstanceEdit() {
		return defaultIEHelper.getDefaultInstanceEdit();
	}
	
	public DefaultInstanceEditHelper getDefaultIEHelper() {
	    return defaultIEHelper;
	}
	
	public void setDefaultPerson(Long personId) {
		defaultIEHelper.setDefaultPerson(personId);
	}
    
    public Long getDefaultPerson() {
        return defaultIEHelper.getDefaultPerson();
    }
	
	public void refresh() {
		defaultIEHelper.refresh();
	}
	
	/**
	 * Use this method if a dialog is needed to warn the user the local changes will
	 * be overwritten.
	 * @param localCopy
	 * @param dbCopy
	 * @param parentComp
	 */
	public boolean updateFromDB(GKInstance localCopy, GKInstance dbCopy, Component parentComp) {
	    // Generate a warning dialog
        if (localCopy.isDirty()) {
            String message = "There may be local changes in instance \""
                            + localCopy.toString()
                            + "\".\n"
                            + "These changes will be overwritten. Are you sure you want to continue?";
            int reply = JOptionPane.showConfirmDialog(parentComp, message, "Overwrite Warning",
                            JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        try {
            updateFromDB(localCopy, dbCopy);
            return true;
        }
        catch (Exception e) {
            System.err.println("SynchronizationManager.updateFromDB(): " + e);
            e.printStackTrace();
            return false;
        }
    }
	
	/**
	 * The class type might be difference in the local copy to a reference to a DBInstance because of type switch 
	 * in DB and/or in local. This type might not be allowed for DBInstance. 
	 * @param dbInstance the database instance should be validated
	 * @throws AttributeClassNotAllowedException if an error found
	 * @throws Exception if attribute loading is in an error.
	 */
	private void validateAttClsForDBInstance(GKInstance dbInstance) throws AttributeClassNotAllowedException, Exception {
	    GKSchemaAttribute att = null;
	    List values; 
	    GKInstance value;
	    XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	    Schema schema = dbInstance.getDbAdaptor().getSchema();
	    for (Iterator it = dbInstance.getSchemClass().getAttributes().iterator(); 
	    	it.hasNext();) {
	        att = (GKSchemaAttribute) it.next();
	        if (!att.isInstanceTypeAttribute())
	            continue;
	        values = dbInstance.getAttributeValuesList(att);
	        if (values == null || values.size() == 0)
	            continue;
	        for (Iterator it1 = values.iterator(); it1.hasNext();) {
	            GKInstance reference = (GKInstance) it1.next();
	            GKInstance localCopy = fileAdaptor.fetchInstance(reference.getDBID());
	            if (localCopy == null)
	                continue;
	            // Need the get the database copy of schema class
	            SchemaClass cls = schema.getClassByName(localCopy.getSchemClass().getName());
	            if (!att.isValidClass(cls)) {
	                throw new AttributeClassNotAllowedException(dbInstance, localCopy, att);
	            }
	        }
	    }
	}
	
	public void updateFromDB(GKInstance localCopy, GKInstance dbCopy) throws AttributeClassNotAllowedException,
	                                                                         Exception {
	    try {
	        validateAttClsForDBInstance(dbCopy);
	    }
	    catch(AttributeClassNotAllowedException e) {
	        // Ask the user if he or she wants to switch the type. If not, rethrown the exception
	        int reply = JOptionPane.showConfirmDialog(parentComp,
	                                                  e.getMessage() + "\n" + 
	                                                  "You have to switch the type of the local instance \"" + e.getReference() + "\" before updating. \n" +
	                                                  "Do you want to switch type for it?",
	                                                  "Error in Updating",
	                                                  JOptionPane.YES_NO_OPTION);
	        if (reply != JOptionPane.YES_OPTION)
	            throw e;
	        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	        // Need to get the local class for reference
	        List values = e.getInstance().getAttributeValuesList(e.getSchemaAttribute().getName());
	        String targetClsName = null;
	        for (Iterator it = values.iterator(); it.hasNext();) {
	            GKInstance tmp = (GKInstance) it.next();
	            if (tmp.getDBID().equals(e.getReference().getDBID())) {
	                targetClsName = tmp.getSchemClass().getName();
	            }
	        }
	        GKSchemaClass localCls = (GKSchemaClass) fileAdaptor.getSchema().getClassByName(targetClsName);
	        if (e.getReference().getSchemClass() == localCls) {
	            JOptionPane.showMessageDialog(parentComp,
	                                          "Local and DB class types are the same. Cannot switch class type!",
	                                          "Error in Switch Class",
	                                          JOptionPane.ERROR_MESSAGE);
	            throw e;
	        }
	        fileAdaptor.switchType(e.getReference(), localCls);
	    }
	    catch(Exception e) {
	        throw e;
	    }
	    // Check if localCopy and dbCopy are the same type
	    if (!localCopy.getSchemClass().getName().equals(dbCopy.getSchemClass().getName())) {
	        // Switch localCopy type to dbCopy
	        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
	        GKSchemaClass newCls = (GKSchemaClass)fileAdaptor.getSchema().getClassByName(dbCopy.getSchemClass().getName());
	        if (newCls == null) {
	            throw new IllegalStateException("SynchronizationManager.updateFrom(): Local schema is not the same as db schema.");
	        }
	        fileAdaptor.switchType(localCopy, newCls);
	    }
	    PersistenceManager.getManager().updateLocalFromDB(localCopy, dbCopy);
	}
	
	/**
	 * Gets a set of local instances referring to the instances supplied as an
	 * argument.
	 * 
	 * @param instance
	 * @return Set of referring instances
	 * @throws Exception
	 */
	private Set getLocalReferingInstances(List instances, MySQLAdaptor dbAdaptor,XMLFileAdaptor fileAdaptor) {
	    Set referers = new HashSet();
	    SchemaClass cls;
	    GKInstance instance;
	    
		for (Iterator ri = instances.iterator(); ri.hasNext();) {
			instance = (GKInstance) ri.next();
			cls = instance.getSchemClass();
			
			referers.addAll(getLocalReferingInstances(instance, dbAdaptor, fileAdaptor));
		}
		
		return referers;
	}
	
	/**
	 * Gets a set of local instances referring to the instance supplied as an
	 * argument.
	 * 
	 * @param instance
	 * @return Set of referring instances
	 * @throws Exception
	 */
	private Set getLocalReferingInstances(GKInstance instance, MySQLAdaptor dbAdaptor, XMLFileAdaptor fileAdaptor) {
	    Set referers = new HashSet();
		SchemaClass cls = instance.getSchemClass();
		GKInstance localInstance;
		
		for (Iterator ri = cls.getReferers().iterator(); ri.hasNext();) {
			SchemaAttribute att = (SchemaAttribute) ri.next();
			Collection vals;
			try {
				vals = instance.getReferers(att.getName());
			} catch (Exception e) {
				System.err.println("getLocalReferingInstances: something odd happened while trying to get the referer for an attribute");
				e.printStackTrace();
				continue;
			}
			if (vals != null) {
				for (Iterator vi = vals.iterator(); vi.hasNext();) {
					GKInstance referer = (GKInstance) vi.next();
					if (isLocalReferingInstance(referer, dbAdaptor, fileAdaptor))
						referers.add(referer);
				}
			}
		}
		
		return referers;
	}
	
	/**
	 * The instance supplied to this method is assumed to be a referrer to
	 * an instance which has been deleted.  Returns true if the given
	 * instance has been changed or created locally.
	 * 
	 * @param instance
	 * @param dbAdaptor
	 * @param fileAdaptor
	 * @return
	 */
	private boolean isLocalReferingInstance(GKInstance instance, MySQLAdaptor dbAdaptor, XMLFileAdaptor fileAdaptor) {
		Long dbId = instance.getDBID();
		GKInstance localInstance = fileAdaptor.fetchInstance(dbId);
        if (dbId.longValue() <= 0)
            return true;
        // In case there is an error: defensive programming
        // LocalInstance has been changed while the original DB instance is deleted
        if (localInstance != null && localInstance.isDirty() && !dbAdaptor.exist(dbId))
            return true; 
        
		//if (dbId.longValue()<=0 || !dbAdaptor.exist(dbId))
		//	return true;
		//
		//GKInstance localInstance = fileAdaptor.fetchInstance(dbId);
		//if (localInstance != null && localInstance.isDirty())
		//	return true;
		
		return false;
	}
	
	public java.util.List deleteInstancesInDB(MySQLAdaptor dbAdaptor,
											  XMLFileAdaptor fileAdaptor,
	                                          java.util.List instances,
	                                          Window parentComp) {
		// Alert the user if any of the instances to be deleted have
		// references to local instances that have been changed.  Give
		// the user the choice to abort the check in at this point,
		// since these local instances will be automatically checked
		// in, something the user might not want.
		Set localReferingInstances = getLocalReferingInstances(instances, dbAdaptor, fileAdaptor);
        if (localReferingInstances.size() > 0) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("The following instances are referers of instances you want to delete,\n" +
            			  "which have also been changed locally. If you check the deleted instances\n" +
                		  "in, these referers will automatically checked in too.  Are you sure\n" +
						  "you want to continue with deletion?\n\n");
            GKInstance instance = null;
            for (Iterator it = localReferingInstances.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                buffer.append(instance.toString());
                if (it.hasNext())
                    buffer.append("\n");
            }
            int reply = JOptionPane.showConfirmDialog(parentComp,
                                                      buffer.toString(),
                                                      "Check in referers",
                                                      JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
            	return new ArrayList();
        }
        java.util.List deleted = new ArrayList(instances.size());
        boolean needTransaction = true;
        try {
            // The user is happy with going ahead with checking in,
            // so here we go...
            needTransaction = checkTransaction(dbAdaptor, parentComp);
            // Wrap all deletion in one single transaction
            if (needTransaction)
                dbAdaptor.startTransaction();
            for (Iterator it = instances.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance)it.next();
                // First delete the things that refer to this instance...
                deleteInstanceFromReferers(instance, dbAdaptor, fileAdaptor, parentComp);
                // ...then delete the instance itself.
                //if (needTransaction) 
                //	dbAdaptor.txDeleteByDBID(instance.getDBID());
                //else
                dbAdaptor.deleteByDBID(instance.getDBID());
                deleted.add(instance);
            }
            if (needTransaction)
                dbAdaptor.commit();
        }
        catch(Exception e) {
            if (needTransaction) {
                try {
                    dbAdaptor.rollback();
                }
                catch(Exception e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(parentComp,
                                                  "Cannot rollback. The database connection might be lost.",
                                                  "Error in Database",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
            System.err.println("SynchronizationManager.deleteInstancesInDB(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentComp,
                                          "Cannot commit deletion to the database.",
                                          "Error in Committing",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
		return deleted;
	}
	
	/**
	 * Deletes the instance from all referrers, in the sense that if the
	 * instance is referred to in an attribute of a referrer, it will be
	 * removed from that attribute.  Additionally, for all referrers
	 * where such a deletion has taken place, the  "modified" slot will
	 * be updated.
	 * 
	 * @param instance
	 * @throws Exception
	 */
	private void deleteInstanceFromReferers(GKInstance instance,
									      MySQLAdaptor dbAdaptor,
									      XMLFileAdaptor fileAdaptor,
									      Window parentDialog) throws Exception {
        // Have to clear referrers to force reloading all referrers. A referrer might be changed
        // and not be a referrer any more. This happens when two newly checked out instances are
        // merged and both of these two instances are loaded in the referrers.
        instance.clearReferers(); 
	    SchemaClass cls = instance.getSchemClass();
	    SchemaAttribute att;
	    Collection vals;
	    
	    // Pre-scan to see if any referers exist.
	    boolean referersExist = false;
	    for (Iterator ri = cls.getReferers().iterator(); ri.hasNext();) {
	        att = (SchemaAttribute) ri.next();
	        vals = instance.getReferers(att.getName());
	        if (vals != null && vals.size()>0) {
	            referersExist = true;
	            break;
	        }
	    }
	    
	    // No referers found - we can safely quit without proceding
	    // any further.
	    if (!referersExist)
	        return;
	    
	    // Get default instance edit and inform the DB adaptor about it,
	    // so that it can be put into the "modified" slot of anything
	    // that gets changed.
	    GKInstance defaultInstanceEdit = defaultIEHelper.refreshInstanceEdit(defaultIEHelper.getLastInstanceEdit());
	    if (defaultInstanceEdit==null)
	        defaultInstanceEdit = defaultIEHelper.createActiveDefaultInstanceEdit(getDefaultInstanceEdit(parentDialog));
	    if (defaultInstanceEdit == null)
	        throw new IllegalStateException("deleteInstanceFromReferers(): no default InstanceEdit specified.");
	    
	    // Store the instance edit too, if it is fresh.
	    if (defaultInstanceEdit.getDBID().longValue()<0) {
	        List instanceEditList = new ArrayList();
	        defaultInstanceEdit.setIsDirty(false);
	        instanceEditList.add(defaultInstanceEdit);
	        dbAdaptor.storeLocalInstances(instanceEditList);
	    }
	    
        // DB version of IE is used for checking
        GKInstance dbDefaultIE = dbAdaptor.fetchInstance(defaultInstanceEdit.getDBID());
        // This clever nested loop is designed to find all referrers.
	    for (Iterator ri = cls.getReferers().iterator(); ri.hasNext();) {
	        att = (SchemaAttribute) ri.next();
	        //vals = instance.getReferers(att.getName());
            // The above call will pull out referrers multiple times.
            // See example for UniProt used by RefPepSeqs.
	        vals = instance.getReferers(att);
            if (vals == null)
	            continue;
	        
	        for (Iterator vi = vals.iterator(); vi.hasNext();) {
	            GKInstance referer = (GKInstance) vi.next();
	            
	            // If the referrer is local and has been changed (e.g. as
	            // a result of the delete itself), then check it in to the
	            // database.  The user should be warned beforehand that
	            // this will happen.
	            if (isLocalReferingInstance(referer, dbAdaptor, fileAdaptor)) {
	                GKInstance localInstance = fileAdaptor.fetchInstance(referer.getDBID());
	                
	                // Check to see if the corresponding database instance
	                // has been changed more recently than the local
	                // instance.  If so, this is a conflict, and the user
	                // is warned.  The conflicting instance will not
	                // be checked in.  Instead, the database version of
	                // this instance will be updated.  This will lead
	                // to an inconsistency between the database and the
	                // local instance (not good), but will make sure that
	                // there are no internal inconsistencies within the
	                // database (very bad).
	                if (checkinConflict(localInstance, dbAdaptor)) {
	                    JOptionPane.showMessageDialog(parentDialog, 
                                "Conflicts have been found in the referrs to the instance you wish to delete, " +
                                "cannot check in.\n" + localInstance.getDisplayName(),
	                            "Conflicts in referrers", JOptionPane.INFORMATION_MESSAGE);
	                } 
	                else {
	                    fileAdaptor.removeDirtyFlag(localInstance);
                        List modified = localInstance.getAttributeValuesList(ReactomeJavaConstants.modified);
                        // This is a local instance.
                        if (!modified.contains(defaultInstanceEdit)) {
                            localInstance.addAttributeValueNoCheck(ReactomeJavaConstants.modified, 
                                                                   defaultInstanceEdit);
                            fileAdaptor.updateInstanceAttribute(localInstance,
                                                                ReactomeJavaConstants.modified);
                        }
	                    dbAdaptor.updateInstance(localInstance);
	                    referer = null; // acts as a flag
	                }
	            }
	            
	            // Update the referrer in the database if not local or if
	            // a conflict has been detected.
	            if (referer != null) {
	                //Load attribute values 1st.
	                //This only loads them from db if they haven't been set already!
	                referer.getAttributeValue(att.getName()); // Use attribute name. The attribute might be
	                                                          // different between super and sub classes.
	                referer.removeAttributeValueNoCheck(att.getName(), instance);
	                dbAdaptor.updateInstanceAttribute(referer, att.getName());
	                
	                // Add a new InstanceEdit to modified slot
	                // Do a local refresh, so that if something gets
	                // added to the "modified" slot later, preexisting
	                // entries will also be included.
                    // the default instance IE might be added already by some other reference. For example,
                    // RefPepSeq can be pulled out by both ReferenceDatabase and Species
	                List modified = referer.getAttributeValuesList(ReactomeJavaConstants.modified);
	                if (modified != null &&
                        !(modified.contains(dbDefaultIE))) { // This is a list from database
	                    // Add the new InstanceEdit to the modified slot of the referer
	                    referer.addAttributeValueNoCheck(ReactomeJavaConstants.modified, 
	                                                     defaultInstanceEdit);
	                    
	                    // Make sure that the change gets added to the db.
	                    dbAdaptor.updateInstanceAttribute(referer, ReactomeJavaConstants.modified);
	                }
	            }
	        }
	    }
	}
	
	private boolean checkTransaction(MySQLAdaptor adaptor, Component parentComp) throws SQLException {
	    if (prop != null) {
			String value = prop.getProperty("useTransaction");
			if (value != null && value.equals("false"))
				return false;
			// Have to make sure that the db is transaction enabled
			if (adaptor.supportsTransactions())
				return true;
			else {
				JOptionPane.showMessageDialog(parentComp, 
				                              "Transaction cannot be used for the specified database. " +
				                              "The option \n\"Use Transaction for Updating\" will be set to false.",
				                              "Error in Transaction",
				                              JOptionPane.ERROR_MESSAGE);
				//prop.setProperty("useTransaction", "false");
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Look into the database to see if the supplied list of instances has already 
	 * been checked in before by other users. Return a list of all instances that 
	 * have already been checked in (conflicts).
	 * 
	 * @param instances
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	private List<GKInstance> checkinConflictsForList(List<GKInstance> instances,
	                                                 MySQLAdaptor dbAdaptor) throws Exception {
	    List<GKInstance> rtn = new ArrayList<GKInstance>();
		InstanceComparer comparer = new InstanceComparer();
	    for (GKInstance instance : instances) {
	        if (instance.getDBID().longValue() < 0)
	            continue; // Local Instances
	        dbAdaptor.setUseCache(false);
			GKInstance remoteCopy = dbAdaptor.fetchInstance(instance.getDBID());
			dbAdaptor.setUseCache(true);
            if (remoteCopy != null) {
                int reply = comparer.compare(instance, remoteCopy);
                if (reply == InstanceComparer.CONFLICT_CHANGE ||
                    reply == InstanceComparer.NEW_CHANGE_IN_DB ||
                    (reply == InstanceComparer.LOCAL_HAS_MORE_IE && !isLocalHasUnexpIECommitAllowed)) {
                    rtn.add(instance);
                }
            }
	    }
	    return rtn;
	}
	
	/**
	 * Look into the database to see if the supplied instance has already 
	 * been checked in before by other users. Return true if so, false
	 * otherwise.
	 * 
	 * @param instance
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	private boolean checkinConflict(GKInstance instance, MySQLAdaptor dbAdaptor) throws Exception {
	    boolean rtn = false;
	    if (instance.getDBID().longValue() >= 0) {
		    dbAdaptor.setUseCache(false);
		    GKInstance remoteCopy = dbAdaptor.fetchInstance(instance.getDBID());
			dbAdaptor.setUseCache(true);
	        if (remoteCopy != null) {
	    		InstanceComparer comparer = new InstanceComparer();
	            int reply = comparer.compare(instance, remoteCopy);
	            if (reply != InstanceComparer.IS_IDENTICAL && reply != InstanceComparer.NEW_CHANGE_IN_LOCAL)
	                rtn = true;
	        }
	    }
	    return rtn;
	}
	
	/**
	 * Check references used in GKInstances in the specified list.
	 * 
	 * Returns a list of instances for which one or more referred-to (i.e. attribute)
	 * instances are no longer in the database.
	 * 
	 * @param instances
	 * @return a list of GKInstances that have not passed the checking.
	 * @throws Exception
	 */
	private List<GKInstance> checkReferencesStillInDbForList(List<GKInstance> instances, 
	                                                        MySQLAdaptor dbAdaptor) throws Exception {
	    List<GKInstance> rtn = new ArrayList<GKInstance>(instances.size());
	    for (GKInstance instance : instances) {
	        if (!checkReferencesStillInDb(instance, dbAdaptor))
	            rtn.add(instance);
	    }
	    return rtn;
	}
	
	/**
	 * A helper method to make sure all GKInstance referred by the specified instance is
	 * still in the database.
	 * 
	 * Returns true if the instance and all referred-to (i.e. attribute) non-local instances still exist
	 * in the database, false othewise.
	 * 
	 * @param instance
	 * @return true for all references exist
	 */
	private boolean checkReferencesStillInDb(GKInstance instance, MySQLAdaptor dbAdaptor) throws Exception {
		Long dbid = instance.getDBID();
		if (dbid.longValue() >= 0 && !dbAdaptor.exist((dbid)))
			return false;
		
		// Check the attributes too - even if the supplied instance still
		// exists in the database, some of the attribute instances may
		// have been deleted
	    GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
	    GKSchemaAttribute att = null;
	    List values = null;
	    Set allValues = new HashSet();
	    
	    // Go through all attributes and collect the referred-to
	    // non-local instances into a single list (allValues).
	    for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
	        att = (GKSchemaAttribute) it.next();
	        // Don't bother with stuff like text.
	        if (!att.isInstanceTypeAttribute())
	            continue;
	        values = instance.getAttributeValuesList(att);
	        if (values == null || values.size() == 0)
	            continue;
	        // Loop over attribute values and stash them if they are non-local.
	        // Don't put local instances in the list
	        for (Iterator it1 = values.iterator(); it1.hasNext();) {
	            GKInstance value = (GKInstance) it1.next();
	            if (value.getDBID().longValue() > -1) // Only for db id
	                allValues.add(value);
	        }
	    }
	    // Loop over all non-local attribute values and store their IDs
	    List dbIds = new ArrayList(allValues.size());
	    for (Iterator it = allValues.iterator(); it.hasNext();) {
	        instance = (GKInstance) it.next();
	        dbIds.add(instance.getDBID());
	    }
	    
	    // Return true if all IDs still exist in the database, false otherwise.
	    // If dbIds is an empty list, returns true.
	    return dbAdaptor.exist(dbIds);
	}

	/**
	 * Checks every instance in the supplied list of instances and if
	 * any of the referrers of these instances have had their classes
	 * changed locally, adds them to the list of instances too.
	 * 
	 * @param instances
	 * @param dbAdaptor
	 * @throws Exception
	 */
	private List<GKInstance> addReferrersWithChangedClasses(List<GKInstance> instances, 
	                                                        MySQLAdaptor dbAdaptor) throws Exception {
		// Copy existing instance list
		List<GKInstance> newInstances = new ArrayList<GKInstance>(instances);
	    // Loop over instances in list and add any referrers with
	    // changed classes to the list of instances to be checked in.
	    for (GKInstance instance : instances) {
	        List referrersWithChangedClasses = findReferrersWithChangedClasses(instance,
	                                                                           dbAdaptor);
	        for (Iterator itcc = referrersWithChangedClasses.iterator(); itcc.hasNext();) {
	        	GKInstance tmp = (GKInstance) itcc.next();
	        	if (!newInstances.contains(tmp))
	        		newInstances.add(tmp);
	        }
	    }
	    return newInstances;
	}
	
	/**
	 * Given an instance, check through all of the referrers and add any
	 * that have had their classes changed locally to a list, which
	 * is then returned by this method.
	 * 
	 * @param instance
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	private List findReferrersWithChangedClasses(GKInstance instance, 
	                                             MySQLAdaptor dbAdaptor) throws Exception {
		List referrersWithChangedClasses = new ArrayList();
		
	    GKSchemaClass cls = (GKSchemaClass) instance.getSchemClass();
	    GKSchemaAttribute att;
	    List values;

	    // Go through all attributes and collect ones where the
	    // class has changed from what it was in the database.
	    for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
	        att = (GKSchemaAttribute) it.next();
	        if (!att.isInstanceTypeAttribute())
	            continue;
	        values = instance.getAttributeValuesList(att);
	        if (values == null || values.size() == 0)
	            continue;
	        
	        // Loop over all instances for this attribute
	        for (Iterator it1 = values.iterator(); it1.hasNext();) {
	            GKInstance attributeInstance = (GKInstance) it1.next();
		        // Don't put local instances in the list
	            if (attributeInstance.getDBID().longValue() > -1 &&
	            		!isInstanceClassSameInDb(attributeInstance, dbAdaptor))
	            	referrersWithChangedClasses.add(attributeInstance);
	        }
	    }
	    
	    // Check the referrers to the referrers, to see if they also have had
	    // their classes changed and add to the list if so.  This is recursive
	    // and could lead to infinite loops if there is any circularity in
	    // the local instances.
	    referrersWithChangedClasses = addReferrersWithChangedClasses(referrersWithChangedClasses, dbAdaptor);
	    
		return referrersWithChangedClasses;
	}
	
	/**
	 * Returns true if the class of the supplied instance is the same as
	 * the class of the corresponding instance in the database.
	 * @param instance
	 * @param dbAdaptor
	 * @return
	 * @throws Exception
	 */
	private boolean isInstanceClassSameInDb(GKInstance instance, MySQLAdaptor dbAdaptor) throws Exception {
		// Assume everything is fine if instance hasn't been changed.
		if (!instance.isDirty())
			return true;
		
		dbAdaptor.setUseCache(false);
		GKInstance remoteInstance = dbAdaptor.fetchInstance(instance.getDBID());
		dbAdaptor.setUseCache(true);
		if (remoteInstance != null) {
			String schemaClass = instance.getSchemClass().getName();
			String remoteSchemaClass = remoteInstance.getSchemClass().getName();
				
			if (schemaClass.equals(remoteSchemaClass))
				return true;
		}		
		
		return false;
	}

	
	/**
	 * Commit a list of GKInstance Objects to the database. The default InstanceEdit 
	 * will be assigned to the slot "created" for a newly created GKInstance, and the slot
	 * "modified" for a modified created GKInstance. This assignment will be done before check-in.
	 * If anything is wrong during check-in, the default InstanceEdit assigned to them will be 
	 * removed to keep the slot meaningful.
	 * @param instances the list of GKInstance objects that are needed to be commit.
	 * @param fileAdaptor the PersistenceAdaptor to the local repository.
	 * @param dbAdaptor the MySQLAdaptor to the database repository.
	 * @param committedInstancesRtnOnly true for return only committed instances in returned list, while false
	 * for return all instances handled in this method (include those handled in match checking with 
	 * db_copy or merging action selected).
	 * @param parentDialog the parent JDialog or JFrame. It should be JDialog, JFrame or null.
	 * @return a list of GKInstances that have been commit to the database.
	 */
	public synchronized List<GKInstance> commitToDB(List<GKInstance> instances, 
	                                                XMLFileAdaptor fileAdaptor, 
	                                                MySQLAdaptor dbAdaptor,
	                                                boolean committedInstancesRtnOnly,
	                                                Window parentDialog) {
	    if (instances == null || instances.size() == 0)
            return null;
        // Make sure every thing is saved
        if (fileAdaptor.isDirty())
            throw new IllegalStateException("SynchronizationManager.commitToDB: There are unsaved changes. "
                                            + "You have to save changes first before committing.");
        // Check if the following step is needed.
        // Assign GKInstance to Renderable objects in pathway diagrams to avoid DB_ID inconsistences:
        // For example, a local EWAS is recorded by its DB_ID. Now, by calling the following method,
        // this EWAS will be directly queried to get DB_ID. This should work for local save/write too.
        for (GKInstance instance : instances) {
            if (instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                fileAdaptor.assignInstancesToDiagrams();
                break;
            }
        }
        // Get a default InstanceEdit.  This will be put into the "created"
        // or "modified" slots of any instances committed, so that creation
        // and changes to instances can be tracked.
        GKInstance defaultInstanceEdit = defaultIEHelper.createActiveDefaultInstanceEdit(getDefaultInstanceEdit(parentDialog));
        // Make a backup of this, we'll need it for deleting later on
        defaultIEHelper.setLastInstanceEdit(defaultInstanceEdit);
        if (defaultInstanceEdit == null) {
        	JOptionPane.showMessageDialog(parentDialog,
        			                      "No default InstanceEdit specified. You have to specify a default Person before committing.",
										  "Error in Committing",
										  JOptionPane.ERROR_MESSAGE);
        	throw new IllegalStateException("SynchronizationManager.commitToDB: no default InstanceEdit specified." +
        			                        "You have to specify a default Person before committing.");
        }
        
        // Make sure all references used in the instances to be checked-in
        // exist in the database still, have no conflicts and have no referrers
        // whose classes have changed.
        try {
        	// Take the supplied list of instances to be checked in and add to it
        	// any instances which are referrers AND whose type has changed.
            instances = addReferrersWithChangedClasses(instances, dbAdaptor);
            
            // Decide what to do about instances which are still available locally
            // but which have been deleted from the database.
        	List<GKInstance> deletedInDatabaseList = checkReferencesStillInDbForList(instances, dbAdaptor);
            if (deletedInDatabaseList.size() > 0) {
                StringBuffer buffer = new StringBuffer();
                buffer.append("The following instances have been deleted from the database by another user. If you\n" +
                    		  "check them in, you will undo somebody elses commit.  Are you sure you want to do this?\n\n");
                GKInstance instance = null;
                for (Iterator it = deletedInDatabaseList.iterator(); it.hasNext();) {
                    instance = (GKInstance) it.next();
                    buffer.append(instance.toString());
                    if (it.hasNext())
                        buffer.append("\n");
                }
                int reply = JOptionPane.showConfirmDialog(parentDialog,
                                                          buffer.toString(),
                                                          "Deleted in database",
                                                          JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION) {
                    instances.removeAll(deletedInDatabaseList);
                }
            }
            
            // Let the user decide what to do if some of the instances to be checked in
            // produce conflicts.
            List<GKInstance> checkinConflictList = checkinConflictsForList(instances, dbAdaptor);            
            if (checkinConflictList.size() > 0) {
                instances.removeAll(checkinConflictList);
                StringBuffer buffer = new StringBuffer();
                buffer.append("The following instances produce conflicts and cannot be checked in. You will\n" +
                    		  "need to either update or merge these instances before you can proceed with them");
                if (instances.size() == 0)
                    buffer.append(":\n");
                else
                    buffer.append(". But you can continue checking in for other instances. Do you want to conitnue?\n\n");
                GKInstance instance = null;
                for (Iterator<GKInstance> it = checkinConflictList.iterator(); it.hasNext();) {
                    instance = it.next();
                    buffer.append(instance.toString());
                    if (it.hasNext())
                        buffer.append("\n");
                }
                if (instances.size() == 0) {
                    JOptionPane.showMessageDialog(parentDialog,
                                                  buffer.toString(),
                                                  "Results in Reference Checking",
                                                  JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
                int reply = JOptionPane.showConfirmDialog(parentDialog,
                                                          buffer.toString(),
                                                          "Results in Reference Checking",
                                                          JOptionPane.YES_NO_OPTION);
                if (reply != JOptionPane.YES_OPTION)
                    return null;
            }
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(parentDialog,
                                          "An exception is thrown during reference checking: \n" + e.toString(),
                                          "Error during References Checking",
                                          JOptionPane.ERROR_MESSAGE);
            System.err.println("SynchronizationManager.commitToDB(): " + e);
            e.printStackTrace();
            return null;
        }
        // Get all local GKInstances
        // localSet contains:
        // 1. local instances referred to by the instances being checked in;
        // 2. all local instances with a negative DB_ID.
        Set<GKInstance> localSet = new HashSet<GKInstance>();
        
        // dbList contains everything in "instances" with a non-negative
        // DB_ID.
        List<GKInstance> dbList = new ArrayList<GKInstance>();
        
        Set<GKInstance> touchedInstances = new HashSet<GKInstance>();
        Set<GKInstance> localSet1 = new HashSet<GKInstance>();
        for (GKInstance instance : instances) {
            localSet1.clear();
            touchedInstances.clear();
            getLocalReferences(instance,
                               localSet1, 
                               touchedInstances);
            localSet.addAll(localSet1);
        }
        // Need to explicitly add default person and default affiliation, since these two instances
        // will be used by default InstanceEdit. Default InstanceEdit will be automatically attached
        // to the instances checked into the db.
        if (defaultInstanceEdit != null) {
            localSet1.clear();
            touchedInstances.clear();
            getLocalReferences(defaultInstanceEdit, 
                               localSet1,
                               touchedInstances);
            localSet.addAll(localSet1);
        }
        // Need to get rid of the selected instances
        localSet.removeAll(instances);
        if (localSet.size() > 0) {
            InstanceListDialog listDialog = null;
            if (parentDialog instanceof JDialog)
                listDialog = new InstanceListDialog((JDialog) parentDialog, "Local Reference List", true);
            else if (parentDialog instanceof JFrame)
                listDialog = new InstanceListDialog((JFrame) parentDialog, "Local Reference List", true);
            else
                listDialog = new InstanceListDialog("Local Reference List", true);
            List<GKInstance> list = new ArrayList<GKInstance>(localSet);
            // Need to sort
            InstanceUtilities.sortInstances(list);
            listDialog.setDisplayedInstances(list);
            listDialog.setSize(600, 400);
            listDialog.setSubTitle("These local instance objects in the list are referred by the selected "
                                 + "instances. To keep data integrity, Checking in the selected "
                                 + "instances will also check in these objects. Do you want to continue?");
            GKApplicationUtilities.center(listDialog);
            listDialog.setModal(true);
            listDialog.setVisible(true);
            if (!listDialog.isOKClicked())
                return null;
        }
        for (GKInstance instance : instances) {
            if (instance.getDBID().longValue() < 0)
                localSet.add(instance); // More local set
            else
                dbList.add(instance);
        }
        //check matched instances, save as new, merge or use the db copy
        // Keep a copy for comparison
        List<GKInstance> handledLocalList = new ArrayList<GKInstance>(localSet);
        // Set localSet will be changed after the following call.
        // The remained elements in the localSet are those that are not handled. 
        if (!checkMatchedInstances(localSet, 
                                   dbAdaptor, 
                                   fileAdaptor, 
                                   parentDialog))
            return null;
        // See how many are handled
        handledLocalList.removeAll(localSet);
        if (localSet.size() == 0 && dbList.size() == 0) {
            // In case there are changes
            try {
                // There is no chance an XMLFileAdaptor has no source name defined since dirty 
                // checking is done early. So the following call should be save.
                fileAdaptor.save(); 
            }
            catch (Exception e) {
                System.err.println("SynchronizationManager.commitToDB(): " + e);
                e.printStackTrace();
            }
            if (committedInstancesRtnOnly)
                return null;
            else
                return handledLocalList;
        }
        // Have to mark as dirty for referers to these local GKInstances since
        // DB_IDs will be changed
        // Save the old DB_ID for update the data structure in FileAdaptor.
        Map<GKInstance, Long> dbIDMap = new HashMap<GKInstance, Long>();
        GKInstance instanceEdit = null;
        boolean needTransaction = true; // Basic requirement. No transaction is a special case.
        try { 
            // Attach default InstanceEdit: created for localSet and modified for dbList
            instanceEdit = defaultIEHelper.attachDefaultInstanceEdit(localSet, 
                                                                     dbList, 
                                                                     defaultInstanceEdit);
            for (GKInstance instance : localSet) {
                dbIDMap.put(instance, instance.getDBID());
                // Have to call here since old DB_IDs are used
                // Probably the following call is not needed: W.G. May 27, 2009.
                List<GKInstance> referers = fileAdaptor.getReferers(instance);
                for (GKInstance referer: referers) {
                    fileAdaptor.markAsDirty(referer);
                }
            }
            // need to check into the database. Put it after the above checking since it
            // is not necessary to check its referrers.
            if (instanceEdit != null) // Should not add instanceEdit if it is null
                localSet.add(instanceEdit);
            needTransaction = checkTransaction(dbAdaptor, parentDialog);
            // Start transaction
            if (needTransaction)
                dbAdaptor.startTransaction();
            //TODO: check if it is possible: after a transaction has started, there is a db connection
            // dropped out. After that, the connection is created. In this newly created connection,
            // actually there is no transaction enabled.
            // Check in local GKInstances
            dbAdaptor.storeLocalInstances(new ArrayList(localSet));
            // Update GKIntances already in the db.
            for (Iterator it = dbList.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                // Check if this GKInstance exists in the database
                if (dbAdaptor.exist(instance.getDBID()))
                    dbAdaptor.updateInstance(instance);
                else
                    dbAdaptor.storeInstance(instance, true);
                // Have to mark as dirty because of instanceedit
                fileAdaptor.markAsDirty(instance);
            }
            if (needTransaction)
                dbAdaptor.commit();
        }
        catch (Exception e) {
            // Have to detach default InstanceEdit since it is not checked
            defaultIEHelper.detachDefaultInstanceEdit(instanceEdit, localSet, dbList);
            // In case DB_ID changed
            for (Iterator it = localSet.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                Long dbID = (Long) dbIDMap.get(instance);
                instance.setDBID(dbID);
            }
            if (needTransaction) {
                try {
                    dbAdaptor.rollback();
                }
                catch (Exception e2) {
                    JOptionPane.showMessageDialog(parentDialog, 
                                                  "Cannot rollback. The database connection might be lost.", 
                                                  "Error in Database",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
            System.err.println("SynchronizationManager.commitToDB(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentDialog,
                                          "Cannot commit data to the database.",
                                          "Error in Committing",
                                          JOptionPane.ERROR_MESSAGE);
            return null;
        }
        // Have to mark as dirty for DB_ID change
        for (Iterator it = localSet.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Long oldDBID = (Long) dbIDMap.get(instance);
            if (oldDBID == null)
                continue; // Escape the attached InstanceEdit
            fileAdaptor.dbIDUpdated(oldDBID, instance);
        }
        List<GKInstance> rtnList = new ArrayList<GKInstance>();
        rtnList.addAll(localSet);
        rtnList.addAll(dbList);
        try {
            // Remove dirty flag
            for (GKInstance instance : rtnList)
                fileAdaptor.removeDirtyFlag(instance);
            // Have to save all changes
            fileAdaptor.save();
            if (!committedInstancesRtnOnly)
                rtnList.addAll(handledLocalList);
            return rtnList;
        }
        catch (Exception e) {
            System.err.println("SynchronizationManager.commitToDB(): " + e);
            e.printStackTrace();
        }
        return null;
    }
	
	
	/**
     * Search matched instances for a specified local GKInstance object.
     * @param localInstance
     * @param parentDialog
     */
    public void matchInstanceInDB(GKInstance localInstance, Window parentDialog) {
        MySQLAdaptor dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(parentDialog);
        if (dbAdaptor == null)
            return;
        Collection matchedInstances = null;
        try {
            matchedInstances = dbAdaptor.fetchIdenticalInstances(localInstance);
        }
        catch (Exception e) {
            System.err.println("SynchronizationManager.matchInstanceInDB(): " + e);
            e.printStackTrace();
        }
        if (matchedInstances == null || matchedInstances.size() == 0) {
            JOptionPane.showMessageDialog(parentDialog, "Cannot find a matched instance in the database.",
                    "Matching Result", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        OneInstanceMatchDialog dialog = null;
        if (parentDialog instanceof JDialog)
            dialog = new OneInstanceMatchDialog((JDialog) parentDialog);
        else if (parentDialog instanceof JFrame)
            dialog = new OneInstanceMatchDialog((JFrame) parentDialog);
        else
            dialog = new OneInstanceMatchDialog();
        // Don't need save as new
        dialog.hideSaveAsNewButton();
        dialog.setLocalInstance(localInstance);
        dialog.setInstanceList(new ArrayList(matchedInstances));
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(parentDialog);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked || dialog.getSelectedAction() == SAVE_AS_NEW_ACTION)
            return;
        GKInstance dbCopy = dialog.getSelectedInstance();
        int action = dialog.getSelectedAction();
        // Handle only merge to db and use db actions here.
        handleMatchedInstance(localInstance, dbCopy, action, parentDialog);
    }

	/**
	 * Have to specify parentDialog to display the matched instances.
	 * @param localSet
	 * @param dbAdaptor
	 * @param fileAdaptor
	 * @param parentDialog
	 */
	private boolean checkMatchedInstances(Set localSet, 
									      MySQLAdaptor dbAdaptor,
	                                      XMLFileAdaptor fileAdaptor,
	                                      Window parentDialog) {
		if (localSet == null || localSet.size() == 0)
			return true;
		Map matchedMap =  new HashMap();
		try {
			for (Iterator it = localSet.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance)it.next();
				Collection c = dbAdaptor.fetchIdenticalInstances(instance);
				if (c != null && c.size() > 0) {
					matchedMap.put(instance, c);
				}
			}
		}
		catch (Exception e) {
			System.err.println("SynchronizationManager.checkMatchedInstances(): " + e);
			e.printStackTrace();
		}
		if (matchedMap.size() == 0)
			return true;
		if (matchedMap.size() == 1) {
			GKInstance instance = (GKInstance) matchedMap.keySet().iterator().next();
			OneInstanceMatchDialog dialog = null;
			if (parentDialog instanceof JDialog) 
				dialog = new OneInstanceMatchDialog((JDialog)parentDialog);
			else if (parentDialog instanceof JFrame)
				dialog = new OneInstanceMatchDialog((JFrame)parentDialog);
			if (dialog == null) // Cannot display it.
				return false;
			dialog.setLocalInstance(instance);
			final Collection c = (Collection) matchedMap.get(instance);
			if (c instanceof java.util.List)
				dialog.setInstanceList((java.util.List)c);
			else
				dialog.setInstanceList(new ArrayList(c));
			dialog.setSize(600, 500);
			dialog.setLocationRelativeTo(parentDialog);
			dialog.setModal(true);
			dialog.setVisible(true);
			if (!dialog.isOKClicked)
				return false;
			if (dialog.getSelectedAction() == SAVE_AS_NEW_ACTION) {
				// Don't need do anything. Just return.
				return true;
			}
			GKInstance dbCopy = dialog.getSelectedInstance();
			int action = dialog.getSelectedAction();
			if(!handleMatchedInstance(instance, dbCopy, action, parentDialog))
			    return false;
			// Either MERGE or USE_DB_COPY. No need to go down
			localSet.remove(instance); // No need to save instance
			return true;
		}
		else {
			MultipleMatchDialog dialog = null;
			if (parentDialog instanceof JDialog)
				dialog = new MultipleMatchDialog((JDialog)parentDialog);
			else if (parentDialog instanceof JFrame)
				dialog = new MultipleMatchDialog((JFrame)parentDialog);
			else
				dialog = new MultipleMatchDialog();
			dialog.setMatchedInstances(matchedMap);
			dialog.setSize(600, 400);
			dialog.setLocationRelativeTo(parentDialog);
			dialog.setModal(true);
			dialog.setVisible(true);
			if (!dialog.isOKClicked)
				return false;
			Map actionMap = dialog.getSelectedActions();
			Map selectedMatchedMap = dialog.getSelectedMatchedInstances();
			// Handle matched actions one by one
			for (Iterator it = actionMap.keySet().iterator(); it.hasNext();) {
				GKInstance newInstance = (GKInstance) it.next();
				Integer actionValue = (Integer) actionMap.get(newInstance);
				int action = actionValue.intValue();
				if (action == SAVE_AS_NEW_ACTION) // Do nothing with this option
					continue; 
				GKInstance matchedInstance = (GKInstance) selectedMatchedMap.get(newInstance);
				handleMatchedInstance(newInstance, matchedInstance, action, parentDialog);
				// Don't need to save
				localSet.remove(newInstance);
			}
			return true;
		}
	}
	
	/**
	 * Do a shallow check-out, i.e., check out the attributes for the specified GKInstance with
	 * shell instance objects as references.
	 * @param instance a GKInstance in the database to be checked out.
	 * @return GKInstance in the local repository.
	 */
	public GKInstance checkOutShallowly(GKInstance instance){
		try {
			GKInstance localCopy = PersistenceManager.getManager().getLocalReference(instance);
			// Need the whole thing instead of the shell instance.
			updateFromDB(localCopy, instance);
			return localCopy;
		}
		catch (Exception e) {
			System.err.println("SynchronizationManager.checkOutShallowly(): " + e);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * All Instances in the specified list instances should be from the same
	 * SchemaClass.
	 * This method is thread safe because some status should be kept consistent among threads (e.g.
	 * existence of a local instance).
	 * @param instances
	 */
	public synchronized void checkOut(java.util.List instances, Window parentDialog) throws Exception {
		if (instances == null || instances.size() == 0)
			return;
		// Have to save changes first
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		// Check instances in the list to see if there are any instances have been checked out before
		java.util.List checkedOutList = new ArrayList();
		java.util.List checkingList = new ArrayList();
		GKInstance instance = null;
		String clsName = null;
		Long dbID = null;
		GKInstance localCopy = null;
        List localList = new ArrayList();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            clsName = instance.getSchemClass().getName();
            dbID = instance.getDBID();
            localCopy = fileAdaptor.fetchInstance(clsName, dbID);
            if (localCopy != null) {
                localList.add(localCopy);
                if (localCopy.isShell()) // A shell Instance is still needed to
                                         // be checked out
                    checkingList.add(instance);
                else
                    checkedOutList.add(instance);
            }
            else
                checkingList.add(instance);
        }
        if (checkedOutList.size() > 0) {
            // A special case
            if (instances.size() == checkedOutList.size()) {
                String instanceStr = instances.size() == 1 ? "instance has" : "instances have";
                JOptionPane.showMessageDialog(parentDialog, "The selected " + instanceStr
                                + " been checked out. \n" + "Please use update.", "Warning",
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            else {
                InstanceListDialog listDialog = null;
                if (parentDialog instanceof JDialog)
                    listDialog = new InstanceListDialog((JDialog) parentDialog,
                                    "Check Out Message", true);
                else
                    listDialog = new InstanceListDialog((JFrame) parentDialog, "Check Out Message",
                                    true);
                listDialog.setDisplayedInstances(checkedOutList);
                String instanceStr = instances.size() == 1 ? "instance has" : "instances have";
                listDialog.setSubTitle("The following " + instanceStr
                                + " been checked out. Please use update.");
                listDialog.setSize(600, 400);
                listDialog.setModal(true);
                GKApplicationUtilities.center(listDialog);
                listDialog.setVisible(true);
                if (!listDialog.isOKClicked())
                    return;
            }
        }
        Map checkOutMap = new HashMap(); // All checked out Instances
        for (Iterator it = checkingList.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            Map<SchemaClass, List<GKInstance>> schemaMap = InstanceUtilities.listDownloadableInstances(instance);
            // Have to merge schemaMap to checkOutMap
            for (Iterator it1 = schemaMap.keySet().iterator(); it1.hasNext();) {
                Object key = it1.next();
                java.util.List list = (java.util.List) schemaMap.get(key);
                if (list != null && list.size() > 0) {
                    java.util.List list1 = (java.util.List) checkOutMap.get(key);
                    if (list1 == null) {
                        list1 = new ArrayList();
                        checkOutMap.put(key, list1);
                    }
                    list1.removeAll(list);
                    list1.addAll(list);
                }
            }
        }
        // Need reset isShell flags since they might be wrong for instances in checkingList
        // because of listAllAttributes
        for (Iterator it = checkingList.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            instance.setIsShell(false);
        }
        fileAdaptor.store(checkOutMap);
        InstanceUtilities.clearShellFlags(checkOutMap);
        // Update the view etc. Have to call the above method first to get the correct
        // behaviors.
        for (Iterator it = localList.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            AttributeEditManager.getManager().attributeEdit(instance);
            // The above line flags instance as dirty. Have to remove it.
            // This is not an optimized code!!!
            fileAdaptor.removeDirtyFlag(instance);
        }
    }
	
	/**
	 * A helper method to handle the matching between the newly created local GKInstance and
	 * the matched GKInstance in the database. This method take care of two actions: MERGE and
	 * USE_DB_COPY, since no action is needed for SAVE_AS_NEW.
	 * @param newInstance the newly created local GKInstance
	 * @param matchedInstance the matched GKInstance in the database.
	 */
	private boolean handleMatchedInstance(GKInstance newInstance, 
	                                   GKInstance matchedInstance, 
	                                   int action,
	                                   Component comp) {
		XMLFileAdaptor fileAdaptor = (XMLFileAdaptor) newInstance.getDbAdaptor();
		MySQLAdaptor dbAdapotr = (MySQLAdaptor) matchedInstance.getDbAdaptor();
		// Check if there is a local copy 
		GKInstance localCopy = null;
		try {
			localCopy = fileAdaptor.fetchInstance(matchedInstance.getSchemClass().getName(), 
			                                      matchedInstance.getDBID());
		}
		catch (Exception e) {
			System.err.println("SynchronizationManager.checkMatchedInstances(): " + e);
			e.printStackTrace();
		}
		if (localCopy == null) {
			// Ask to check out the a copy of matchedInstance from the database
			localCopy = checkOutShallowly(matchedInstance);
		}
		if (localCopy == null) {
			JOptionPane.showMessageDialog(comp,
			                              "Cannot checked out the matched instance.",
			                              "Error",
			                              JOptionPane.ERROR_MESSAGE);
			return false;                              
		}
		if (localCopy.isShell()) {
		    try {
                updateFromDB(localCopy, matchedInstance);
            }
            catch (Exception e1) {
                System.err.println("SynchronizationManager.handleMatchedInstance(): " + e1);
                e1.printStackTrace();
            }
		}
		if (localCopy != null) {
			// Check if local Copy is the same as the one in the database.
			if (!InstanceUtilities.compare(localCopy, matchedInstance)) {
				// Have to make the localCopy and matchedInstance identical.
				// Otherwise, some unexpected behaviors will occur.
				JOptionPane.showMessageDialog(comp,
				                              "The selected matched instance has been checked out. The local copy will be used.\n" +
				                              "However, there are differences between the local and db copies. You have to\n" +
				                              "resolve these differences before continuing.",
				                              "Conflicts in Matched Instances",
				                              JOptionPane.INFORMATION_MESSAGE);
				InstanceComparisonPane comparisonPane = new InstanceComparisonPane();
				comparisonPane.setInstances(localCopy, matchedInstance);
				String title = "Comparing Instances in Class \"" + localCopy.getSchemClass().getName() + "\"";
				JDialog dialog = GKApplicationUtilities.createDialog(comp, title);
				dialog.getContentPane().add(comparisonPane, BorderLayout.CENTER);
				dialog.setModal(true);
				dialog.setSize(800, 600);
				GKApplicationUtilities.center(dialog);
				dialog.setModal(true);
				dialog.setVisible(true);
				// Whatever is done to the local copy will be regarded as the user's options
				// Go to the next step
			}
			if (action == MERGE_ACTION) { // Need to merge something to the local copy from the newInstance
				// Do another merge
				final InstanceMergingPane mergingPane = new InstanceMergingPane();
				mergingPane.setInstances(localCopy, newInstance);
				String title = "Merge New Instance to Old Instance in Class \"" + localCopy.getSchemClass().getName() + "\"";
				JDialog dialog = GKApplicationUtilities.createDialog(comp, title);
				dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				dialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						mergingPane.dispose();
					}
				});
				dialog.getContentPane().add(mergingPane, BorderLayout.CENTER);
				dialog.setModal(true);
				dialog.setSize(800, 600);
				GKApplicationUtilities.center(dialog);
				dialog.setModal(true);
				dialog.setVisible(true);
				if (!mergingPane.isOKClicked())
				    return false;
			}
			// action should be either MERGE_ACTION or USE_DB_COPY_ACTION;
			if (action == USE_DB_COPY_ACTION || action == MERGE_ACTION) {
				// Replace the references
				try {
					java.util.List referrers = fileAdaptor.getReferers(newInstance);
					if (referrers != null && referrers.size() > 0) {
						for (Iterator it = referrers.iterator(); it.hasNext();) {
							GKInstance referrer = (GKInstance) it.next();
							InstanceUtilities.replaceReference(referrer, newInstance, localCopy);
							//fileAdaptor.markAsDirty(referrer);
						}
					}
					//A special case for default InstanceEdit
					GKInstance defaultInstanceEdit = getDefaultInstanceEdit(SwingUtilities.getWindowAncestor(comp));
					if (defaultInstanceEdit != null && newInstance.getSchemClass().isa("Person")) {
						java.util.List authors = defaultInstanceEdit.getAttributeValuesList("author");
						int index = authors.indexOf(newInstance);
						if (index >= 0)
							authors.set(index, localCopy);
					}
					// Delete the local copy
					fileAdaptor.deleteInstance(newInstance);
					return true;
				}
				catch(Exception e) {
					System.err.println("SyncrhonizationManager.handleMatchedInstance(): " + e);
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}
	
	/**
	 * A helper method to get a set of local references GKInstances that are
	 * referred by a specified instance. This will go to the reference graph and 
	 * get all local GKInstance in the graph.
	 * @param instance
	 * @return
	 */
	private void getLocalReferences(GKInstance instance, 
	                                Set<GKInstance> references, 
	                                Set<GKInstance> touchedInstances) {
		if (touchedInstances.contains(instance))
			return;
		else
			touchedInstances.add(instance);
		GKSchemaAttribute att = null;
		java.util.List valueList = null;
		for (Iterator it = instance.getSchemaAttributes().iterator(); it.hasNext();) {
		    att = (GKSchemaAttribute) it.next();
		    if (!att.isInstanceTypeAttribute())
		        continue;
		    try {
		        valueList = instance.getAttributeValuesList(att);
		        if (valueList == null || valueList.size() == 0)
		            continue;
		        for (Iterator it1 = valueList.iterator(); it1.hasNext();) {
		            GKInstance valueInstance = (GKInstance) it1.next();
		            if (valueInstance.getDBID().longValue() < 0) {
		                if(references.add(valueInstance))
		                    getLocalReferences(valueInstance,
		                                       references,
		                                       touchedInstances);
		            }
		        }
		    }
		    catch(Exception e) {
		        System.err.println("SynchronizationManager.getLocalRefererences(): " + e);
		        e.printStackTrace();
		    }
		}
		// A simple way to get local instance for diagram
		if (instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
		    // Get the diagram
		    try {
		        GKInstance pathway = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.representedPathway);
		        if (pathway == null)
		            return;
		        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		        RenderablePathway diagram = fileAdaptor.getDiagram(pathway);
		        if (diagram == null)
		            return;
		        // Get local reference for the diagram
		        if (diagram.getComponents() != null && diagram.getComponents().size() > 0) {
		            for (Iterator it = diagram.getComponents().iterator(); it.hasNext();) {
		                Renderable r = (Renderable) it.next();
		                if (r.getReactomeId() == null)
		                    continue;
		                GKInstance inst = fileAdaptor.fetchInstance(r.getReactomeId());
		                if (inst != null && inst.getDBID() < 0) {
		                    if (references.add(inst))
		                        getLocalReferences(inst, 
		                                           references,
		                                           touchedInstances);
		                }
		            }
		        }
		        // Don't forget the check in the pathway too: we don't want to have a null pathway for
		        // a committed diagram
		        if (pathway.getDBID() < 0) {
		            if (references.add(pathway))
		                getLocalReferences(pathway, 
		                                   references,
		                                   touchedInstances);
		        }
		    }
		    catch(Exception e) {
		        System.err.println("SynchronizationManager.getLocalReferences(): " + e);
		        e.printStackTrace();
		    }
		}
	}
	
	class OneInstanceMatchDialog extends JDialog {
		// GUIs
		private JRadioButton saveAsNew;
		private JRadioButton useDBCopy;
		private JRadioButton merge;
		private InstanceListPane listPane;
		private AttributePane attPane;
		// Local one
		private GKInstance localInstance;
		boolean isOKClicked = false;
		
		public OneInstanceMatchDialog(JDialog parentDialog) {
			super(parentDialog);
			init();
		}
		
		public OneInstanceMatchDialog(JFrame parentFrame) {
			super(parentFrame);
			init();
		}
		
		public OneInstanceMatchDialog() {
		    super();
		    init();
		}
		
		private void init() {
			JPanel centerPane = new JPanel();
			centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
			centerPane.setLayout(new BorderLayout());
			listPane = new InstanceListPane();
			attPane = new AttributePane();
			listPane.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			listPane.addSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					java.util.List selection = listPane.getSelection();
					if (selection == null || selection.size() == 0) {
						attPane.setInstance(null);
						merge.setEnabled(false);
						useDBCopy.setEnabled(false);
						saveAsNew.setSelected(true);
					}
					else {
						attPane.setInstance((GKInstance)selection.get(0));
						merge.setEnabled(true);
						useDBCopy.setEnabled(true);
						// default with useDBCopy
						useDBCopy.setSelected(true);
					}
				}
			});
			JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			                                listPane,
			                                attPane);
			jsp.setDividerLocation(300);
			jsp.setResizeWeight(0.5);
			centerPane.add(jsp, BorderLayout.CENTER);                                
			
			JPanel optionPane = new JPanel();
			optionPane.setLayout(new GridBagLayout());
			optionPane.setBorder(BorderFactory.createEtchedBorder());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			constraints.insets = new Insets(4, 8, 4, 4);
			constraints.weightx = 0.5;
			JLabel optionLabel = new JLabel("Choose one of the following actions:");
			optionPane.add(optionLabel, constraints);
			saveAsNew = new JRadioButton("Save as New Instance");
			constraints.gridy = 1;
			optionPane.add(saveAsNew, constraints);
			useDBCopy = new JRadioButton("Use the selected Instance");
			constraints.gridy = 2;
			optionPane.add(useDBCopy, constraints);
			merge = new JRadioButton("Merge the local copy to the selected one");
			constraints.gridy = 3;
			optionPane.add(merge, constraints);
			ButtonGroup btnGroup = new ButtonGroup();
			btnGroup.add(saveAsNew);
			btnGroup.add(useDBCopy);
			btnGroup.add(merge);
			// Default 
			saveAsNew.setSelected(true);
			useDBCopy.setEnabled(false);
			merge.setEnabled(false);
			centerPane.add(optionPane, BorderLayout.SOUTH);
			getContentPane().add(centerPane, BorderLayout.CENTER);
			
			// OK and cancel pane
			JPanel okCancelPane = new JPanel();
			okCancelPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));			                                	
			final JButton okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.setMnemonic('C');
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					dispose();
				}
			});
			okCancelPane.add(okBtn);
			okCancelPane.add(cancelBtn);
			getContentPane().add(okCancelPane, BorderLayout.SOUTH);
		}
		
		public void setLocalInstance(GKInstance instance) {
			localInstance = instance;
			// Set title
			setTitle("Matched Instances for \"" + instance.getDisplayName() + "\"");
		}
		
		public void setInstanceList(java.util.List list) {
			InstanceUtilities.sortInstances(list);
			listPane.setDisplayedInstances(list);
			listPane.setTitle("Matched instances in the database: " + list.size());
			if (list.size() > 0)
			    listPane.setSelection((GKInstance)list.get(0));
		}
		
		public void select(GKInstance instance) {
		    listPane.setSelection(instance);
		}
		
		public int getSelectedAction() {
			if (useDBCopy.isSelected())
				return USE_DB_COPY_ACTION;
			if (merge.isSelected())
				return MERGE_ACTION;
			return SAVE_AS_NEW_ACTION;
		}
		
		public GKInstance getSelectedInstance() {
			return (GKInstance) attPane.getInstance();
		}
		
		public void hideSaveAsNewButton() {
		    saveAsNew.setVisible(false);
		}
	}
	
//	class InstanceMergingPane extends InstanceComparisonPane {
//		private boolean isSaved = false;
//		
//		public InstanceMergingPane() {
//		}
//		
//		public void setInstances(GKInstance instance1, GKInstance instance2) {
//			super.setInstances(instance1, instance2);
//			merge();
//		}
//		
//		protected void merge() {
//			super.merge();
//			// Always make it invisile.
//			mergeBtn.setVisible(false);
//		}
//		
//		protected void dispose() {
//			// Have to check if the merged is saved.
//			if (!isSaved) {
//				JOptionPane.showMessageDialog(this,
//				                              "You have to click the button \"Save Merged\" to save\n" +//				                              "the merged attributes first before closing.",
//				                              "Error",
//				                              JOptionPane.ERROR_MESSAGE);
//				return ;
//			}
//			super.dispose();
//		}
//		
//		protected void saveMerging() {
//			GKInstance merged = (GKInstance) attPane.getInstance();
//			// Always save the changes to the first GKInstance.
//			replaceFirstInstance(merged);
//			isSaved = true;
//		}
//	}
//	
	class MultipleMatchDialog extends JDialog {
		private AttributeTable table;
		private boolean isOKClicked = false;
		
		public MultipleMatchDialog(JDialog parentDialog) {
			super(parentDialog);
			init();
		}
		
		public MultipleMatchDialog(JFrame parentFrame) {
			super(parentFrame);
			init();
		}
		
		public MultipleMatchDialog() {
			super();
			init();
		}
		
		public void setMatchedInstances(Map matchedMap) {
			MatchingTableModel model = (MatchingTableModel) table.getModel();
			model.setMatchedInstances(matchedMap);
		}
		
		protected void viewSelectedCell() {
			MatchingTableModel model = (MatchingTableModel)table.getModel();
			// Only need to handle a single cell selection.
			int rowCount = table.getSelectedRowCount();
			int colCount = table.getSelectedColumnCount();
			if (rowCount == 1 && colCount == 1) {
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				Object obj = model.getValueAt(row, col);
				if (obj instanceof GKInstance) {
					GKInstance instance = (GKInstance)obj;
					FrameManager.getManager().showInstance(instance, this);
				}
			}
		}	
		
		private void init() {
		    // Add a textarea for label
		    JTextArea ta = new JTextArea();
		    ta.setText("Some of newly created local instances can be matched to instances in the database. " +
		    		   "To reduce duplication, it is recomended that you use matched instances in the database. " +
		    		   "To do so, select a matched db instance by clicking the checkbox right to it and choose " +
		    		   "action \"Use the Selected\" or \"Merge New to Selected\" in the Actions column. Otherwise, " +
		    		   "use action \"Save as New\" to save a new instance to the database.");
		    ta.setLineWrap(true);
		    ta.setWrapStyleWord(true);
		    ta.setEditable(false);
		    ta.setFont(ta.getFont().deriveFont(Font.BOLD));
		    ta.setBackground(getContentPane().getBackground());
		    ta.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		    getContentPane().add(ta, BorderLayout.NORTH);
			table = new AttributeTable();
			MatchingTableModel model = new MatchingTableModel();
			table.setModel(model);
			table.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (e.getClickCount() == 2) {
						viewSelectedCell();
					}
				}
			});
			// Use a JComboBox for cell editing for column 3
			JComboBox jcb = new JComboBox(actions);
			DefaultCellEditor editor = new DefaultCellEditor(jcb);
			table.getColumnModel().getColumn(3).setCellEditor(editor);
			getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
			// Control pane
			JPanel controlPane = new JPanel();
			controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			JButton okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.setMnemonic('C');
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			controlPane.add(okBtn);
			controlPane.add(cancelBtn);
			getContentPane().add(controlPane, BorderLayout.SOUTH);
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					dispose();
				}
			});
			
			String title = new String("Resolving Matched Instances");
			setTitle(title);
		}
		
		public Map getSelectedActions() {
			MatchingTableModel model = (MatchingTableModel) table.getModel();
			return model.getSelectedActions();
		}
		
		public Map getSelectedMatchedInstances() {
			MatchingTableModel model = (MatchingTableModel) table.getModel();
			return model.getSelectedMatchedInstances();
		}
	}
	
	/**
	 * To display a multispan table.
	 */
	class MatchingTableModel extends AttributeTableModel {
		private String[] headers = {"New Local Instances", 
                                    "Matched DB Instances", 
                                    "Selected",                            
								  "Actions"};
		private CellSpan cellSpan;
		private Object[][] values;
		private java.util.List sortedList;
		private JTable table;
		
		MatchingTableModel() {
			sortedList = new ArrayList();
			cellSpan = new CellSpan();
			values = new Object[1][1];
		}
		
		public void setTable(JTable table) {
			this.table = table;
		}
		
		public String getColumnName(int col) {
			return headers[col];
		}
		
		public CellSpan getCellSpan() {
			return cellSpan;
		}
		
		public int getRowCount() {
			return values.length;
		}
		
		public int getColumnCount() {
			return headers.length;
		}
		
		public boolean isCellEditable(int row, int col) {
			if (col == 2 || col == 3)
				return true;
			return false;
		}
		
		public void setMatchedInstances(Map matchedInstances) {
			// Find how many rows
			int rows = 0;
			sortedList.clear();
			for (Iterator it = matchedInstances.keySet().iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance)it.next();
				Collection list = (Collection) matchedInstances.get(instance);
				if (list == null || list.size() == 0)
					continue;
				sortedList.add(instance);
				rows += list.size();
			}
			values = new Object[rows][4];
			cellSpan = new CellSpan(rows, 4);
			InstanceUtilities.sortInstances(sortedList);
			// initialize the values
			int row = 0;
			boolean isFirst = true;
			for (Iterator it = sortedList.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				values[row][0] = instance;
				values[row][3] = actions[1]; // Use DB Copy as default
				Collection list = (Collection) matchedInstances.get(instance);
				isFirst = true;
				for (Iterator i = list.iterator(); i.hasNext();) {
					GKInstance matched = (GKInstance) i.next();
					if (isFirst) {
					    values[row][2] = Boolean.TRUE;
					    isFirst = false;
					}
					else
					    values[row][2] = Boolean.FALSE;
					values[row][1] = matched;
					cellSpan.setSpan(1, row, 1);
					cellSpan.setSpan(1, row, 2);
					row ++;
				}
				cellSpan.setSpan(list.size(), row - list.size(), 0);
				cellSpan.setSpan(list.size(), row - list.size(), 3);
			}
		}
		
		public Object getValueAt(int row, int col) {
			if (cellSpan.isVisible(row, col))
				return values[row][col];
			return null;
		}
		
		public void setValueAt(Object value, int row, int col) {
			if (col == 3) {
				boolean hasSelected = false;
				Boolean isSelected = (Boolean) values[row][2];
				int selectedRow = -1;
				if (isSelected.booleanValue()) {
					hasSelected = true;
					selectedRow = row;
				}
				if (!hasSelected) {
					for (int i = row + 1; i < values.length; i++) {
						if (values[i][0] != null)
							break;
						isSelected = (Boolean) values[i][2];
						if (isSelected.booleanValue()) {
							hasSelected = true;
							selectedRow = row;
							break;
						}
					}
				}
				if (hasSelected) {
					values[row][col] = value;
					if (value.equals(actions[0])) {
					    // de-select
					    values[selectedRow][2] = Boolean.FALSE;
					    fireTableCellUpdated(selectedRow, 2);
					}
				}
				else { // Only Save as New can be used.
					if (value.equals(actions[0])) {
						values[row][col] = value;
					}
					else {
						JOptionPane.showMessageDialog(table,
						                              "Because you have not selected any matched instance.\n" +
						                              "You can only use action \"Save as New\"",
						                              "Warning",
						                              JOptionPane.WARNING_MESSAGE);
						values[row][col] = actions[0];
					}
				}
				return;
			}
			values[row][col] = value;
			// Have to take extra action when row is 1
			// Get the group row indices
			if (col == 2) {
				Boolean selected = (Boolean) value;
				if (selected.booleanValue()) {
					// Go down
					for (int i = row + 1; i < values.length; i++) {
						if (values[i][0] != null)
							break;
						values[i][col] = Boolean.FALSE;
					}
					// Go up
					for (int i = row; i >= 0; i--) {
						if (i < row)
							values[i][col] = Boolean.FALSE;
						if (values[i][0] != null)
							break;
					}
					// Use the default value
					int anchorRow = getAnchorRow(row);
					if (anchorRow > -1) {
					    String action = (String) getValueAt(anchorRow, 3);
					    if (action.equals(actions[0])) {
					        values[anchorRow][3] = actions[1];
					        fireTableCellUpdated(anchorRow, 3);
					    }
					}
				}
				else {
				    int anchorRow = getAnchorRow(row);
				    if (anchorRow > -1) {
					    String action = (String) getValueAt(anchorRow, 3);
					    if (!action.equals(actions[0])) {
					        values[anchorRow][3] = actions[0];
					        fireTableCellUpdated(anchorRow, 3);
					    }
				    }
				}
			}
		}
		
		/**
		 * An anchor row is a row with all four columns have values
		 * @param row
		 * @return
		 */
		private int getAnchorRow(int row) {
		    for (int i = row; i >= 0; i--) {
		        if (values[i][0] != null)
		            return i;
		    }
		    return -1;
		}
		
		public Class getColumnClass(int col) {
			if (col == 2)
				return Boolean.class;
			return String.class;
		}
		
		public Map getSelectedActions() {
			Map map = new HashMap();
			for (int i = 0; i < values.length; i++) {
				if (values[i][0] != null) {
					// Get the action
					String action = values[i][3].toString();
					map.put(values[i][0], new Integer(mapAction(action)));
				}
			}
			return map;
		}
		
		private int mapAction(String action) {
			for (int i = 0; i < actions.length; i++) {
				if (action.equals(actions[i]))
					return i;
			}
			return SAVE_AS_NEW_ACTION;
		}
		
		public Map getSelectedMatchedInstances() {
			Map map = new HashMap();
			for (int i = 0; i < values.length; i++) {
				if (values[i][0] != null) {
					// Get the selected values
					for (int j = i; j < values.length; j++) {
						if (j > i && values[j][0] != null) // Go to too far
							break;
						Boolean selected = (Boolean) values[j][2];
						if (selected.booleanValue()) {
							map.put(values[i][0], values[j][1]);
							break;
						}
					}
				}
			}
			return map;
		}
        
        public SchemaClass getSchemaClass() {
            return null;
        }
	}
}
