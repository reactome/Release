/*
 * Created on Jan 18, 2005
 *
 */
package org.gk.database;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to help set up the default InstanceEdit that will be used
 * during commiting any changes to the database.
 * @author wgm
 */
public class DefaultInstanceEditHelper {
	private GKInstance instanceEdit; // The default InstanceEdit
	private GKInstance lastInstanceEdit = null; // Optional most recent InstanceEdit
	private Long personID;
	
	public DefaultInstanceEditHelper() {
	}
	
	/**
	 * @return Returns the lastInstanceEdit.
	 */
	public GKInstance getLastInstanceEdit() {
		return lastInstanceEdit;
	}
	
	/**
	 * @param lastInstanceEdit The lastInstanceEdit to set.
	 */
	public void setLastInstanceEdit(GKInstance lastInstanceEdit) {
		this.lastInstanceEdit = lastInstanceEdit;
	}

	public void setDefaultPerson(Long personID) {
		this.personID = personID;
	}
    
    public Long getDefaultPerson() {
        return this.personID;
    }
	
	/**
	 * Get the default InstanceEdit. If null, null will be returned.
	 * @return 
	 * @see getDefaultInstanceEdit(Window)
	 */
	public GKInstance getDefaultInstanceEdit() {
		return instanceEdit;
	}
	
	/**
	 * Get the default InstanceEdit. If null, this method will try to
	 * construct one and cach it if successfully.
	 * @param parentWindow
	 * @return
	 * @see getDefaultInstance()
	 */
	public GKInstance getDefaultInstanceEdit(Window parentWindow) {
		if (instanceEdit != null)
			return instanceEdit;
		GKInstance person = fetchPerson(parentWindow);
		if (person == null)
			return null; // Probably canceled
		instanceEdit = createDefaultInstanceEdit(person);
		return instanceEdit;
	}

	/**
	 * Create a default IE based on a default Person instance. The returned 
	 * GKInstance has not filled.
	 * @param person
	 */
	public GKInstance createDefaultInstanceEdit(GKInstance person) {
	    GKInstance instanceEdit = new GKInstance();
	    PersistenceAdaptor adaptor = person.getDbAdaptor();
	    instanceEdit.setDbAdaptor(adaptor);
	    SchemaClass cls = adaptor.getSchema().getClassByName(ReactomeJavaConstants.InstanceEdit);
	    instanceEdit.setSchemaClass(cls);
	    try {
	        instanceEdit.addAttributeValue(ReactomeJavaConstants.author, person);
	    }
	    catch(Exception e) {
	        System.err.println("DefaultInstanceEdit.getDefaultInstanceEdit(): " + e);
	        e.printStackTrace();
	    }
	    return instanceEdit;
	}
	
	/**
	 * Checks the supplied InstanceEdit and if it isn't too old, returns it
	 * unchanged.  If it is older than a certain time, then a freshly
	 * created InstanceEdit with the current date/time is returned.
	 * Returns null if there were any problems.
	 * 
	 * @param instanceEdit
	 * @return Returns an up-to-date InstanceEdit.
	 */
	public GKInstance refreshInstanceEdit(GKInstance instanceEdit) {
		if (instanceEdit==null)
			return null;
		Calendar instanceEditDateTime = null;
		Calendar currentDateTime = null;
		try {
			instanceEditDateTime = GKApplicationUtilities.getCalendar((String)instanceEdit.getAttributeValue("dateTime"));
			currentDateTime = GKApplicationUtilities.getCalendar(GKApplicationUtilities.getDateTime());
		} catch (Exception e) {
			System.err.println("refreshInstanceEdit: problem retrieving attribute \"dateTime\"");
			e.printStackTrace();
			return null;
		}
		if (instanceEditDateTime==null || currentDateTime==null)
			return null;
		
		// Return the old InstanceEdit if it isn't older than 1 minute
		if (instanceEditDateTime.get(Calendar.YEAR)==currentDateTime.get(Calendar.YEAR) &&
			instanceEditDateTime.get(Calendar.MONTH)==currentDateTime.get(Calendar.MONTH) &&
			instanceEditDateTime.get(Calendar.DAY_OF_MONTH)==currentDateTime.get(Calendar.DAY_OF_MONTH) &&
			instanceEditDateTime.get(Calendar.HOUR_OF_DAY)==currentDateTime.get(Calendar.HOUR_OF_DAY) &&
			currentDateTime.get(Calendar.MINUTE) - instanceEditDateTime.get(Calendar.MINUTE) < 2
		)
			return instanceEdit;
		else
			return(createActiveDefaultInstanceEdit(instanceEdit));
	}
	
	private GKInstance fetchPerson(Window parentWindow) {
		GKInstance person = null;
		if (personID == null) {
			// Reminding
			int reply = JOptionPane.showConfirmDialog(parentWindow,
							                         "No person instance specified for the default InstanceEdit. You have to choose\n"
									                 + "a Person instance before committing. Do you want to continue?",
							                         "Set up person?", 
													 JOptionPane.YES_NO_OPTION);
			if (reply != JOptionPane.YES_OPTION)
				return null;
			person = selectPerson(parentWindow);
		}
		else {
			// Person ID is specified
			person = fetchPerson(personID, parentWindow);
			if (person == null) 
				person = selectPerson(parentWindow);
		}
		return person;
	}
	
	/**
	 * @param parentWindow
	 * @return
	 */
	private GKInstance selectPerson(Window parentWindow) {
		InstanceSelectDialog dialog = null;
		String title = "Choose a Person instance for the default InstanceEdit:";
		if (parentWindow instanceof JDialog)
			dialog = new InstanceSelectDialog((JDialog) parentWindow, title);
		else if (parentWindow instanceof JFrame)
			dialog = new InstanceSelectDialog((JFrame) parentWindow, title);
		if (dialog == null)
			return null;
		List cls = new ArrayList(1);
		cls.add(PersistenceManager.getManager().getActiveFileAdaptor()
				.getSchema().getClassByName("Person"));
		dialog.setTopLevelSchemaClasses(cls);
		dialog.setIsMultipleValue(false);
		dialog.setModal(true);
		dialog.setSize(1000, 700);
		GKApplicationUtilities.center(dialog);
		dialog.setVisible(true);
		if (dialog.isOKClicked()) {
			List list = dialog.getSelectedInstances();
			if (list != null && list.size() > 0) {
				GKInstance person = (GKInstance) list.get(0);
                PersistenceManager.getManager().getActiveFileAdaptor().setDefaultPersonId(person.getDBID());
                return person;
            }
		}
		return null;
	}

	private GKInstance fetchPerson(Long personID,
                                   Window parentWindow) {
		GKInstance person = null;
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		try {
			person = (GKInstance) fileAdaptor.fetchInstance("Person", personID);
			if (person == null) {
				// Try db
				if (personID.longValue() > -1) {
					MySQLAdaptor dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(parentWindow);
					person = (GKInstance) dbAdaptor.fetchInstance("Person", personID);
					if (person != null) {
						GKInstance localCopy = PersistenceManager.getManager().getLocalReference(person);
						// Need the whole thing instead of the shell instance.
						SynchronizationManager.getManager().updateFromDB(localCopy, person);
						person = localCopy;
					}
					else {
						JOptionPane.showMessageDialog(parentWindow,
								"The previously used Person instance for the default InstanceEdit has been\n" +
								"deleted from the database. You have to select another one.",
								"Fetching Person for Default InstanceEdit",
								JOptionPane.INFORMATION_MESSAGE);
					}					
				}
				else {
					JOptionPane.showMessageDialog(parentWindow,
							"The previously used Person instance for the default InstanceEdit has been\n" +
							"deleted. You have to select another one.",
							"Fetching Person for Default InstanceEdit",
							JOptionPane.INFORMATION_MESSAGE);					
				}
			}
		}
		catch(Exception e) {
			System.err.println("DefaultInstanceEditHelper.fetchPerson(): " + e);
			e.printStackTrace();
		}
		return person;
	}
	
	public GKInstance createFilledInstanceEdit(GKInstance defaultInstanceEdit) {
		if (defaultInstanceEdit == null)
			return null;
		GKInstance editClone = (GKInstance) defaultInstanceEdit.clone();
		try {
			editClone.addAttributeValue("dateTime", GKApplicationUtilities.getDateTime());
		} 
		catch (InvalidAttributeException e) {
			System.err.println("DefaultInstanceEditHelper.createFilledInstanceEdit(): WARNING - problem assigning date/time to InstanceEdit");
			e.printStackTrace();
		} 
		catch (InvalidAttributeValueException e) {
			System.err.println("DefaultInstanceEditHelper.createFilledInstanceEdit(): WARNING - problem assigning date/time to InstanceEdit");
			e.printStackTrace();
		}
		// If an exception is thrown, the whole method should be aborted since there is no reason to return
		// an invlaid InstanceEdit. However, this exception will never be thrown. But, logically, this implementation
		// is NOT correct!
		editClone.setDisplayName(InstanceDisplayNameGenerator.generateDisplayName(editClone));
	    editClone.setIsInflated(true);

	    return editClone;
	}
	
	/**
	 * Helper method that creates a new default InstanceEdit from the supplied
	 * InstanceEdit.  The new value has date and name set to nice values and
	 * also has a DB_ID assigned to it.
	 * 
	 * @param defaultInstanceEdit
	 * @return
	 */
	public GKInstance createActiveDefaultInstanceEdit(GKInstance defaultInstanceEdit) {
		if (defaultInstanceEdit == null)
			return null;
		GKInstance editClone = (GKInstance) defaultInstanceEdit.clone();
		try {
			editClone.addAttributeValue("dateTime", GKApplicationUtilities.getDateTime());
			InstanceDisplayNameGenerator.setDisplayName(editClone);
			editClone.setIsInflated(true);
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            editClone.setDBID(fileAdaptor.getNextLocalID());
            fileAdaptor.addNewInstance(editClone);
			return editClone; 
		}
		catch (Exception e) {
			System.err.println("DefaultInstanceEditHelper.attachDefaultInstanceEdit(): " + e);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Attach the default InstanceEdit GKInstnce to the GKInstance objects
	 * @param localSet the list of newly created GKInstance objects
	 * @param dbList the list of modified GKInstance objects.
	 * @return the attached GKInstance object for InstanceEdit.
	 */
	public GKInstance attachDefaultInstanceEdit(Collection localSet, 
	                                            Collection dbList, 
	                                            GKInstance attachingIE) throws Exception {
	    if (attachingIE == null)
	        return null;
	    boolean isUsed = false;
	    GKInstance tmp = null;
	    if (localSet != null) {
	        for (Iterator it = localSet.iterator(); it.hasNext();) {
	            tmp = (GKInstance)it.next();
	            // Escape it if there is a created assigned
	            if (tmp.getAttributeValue("created") != null)
	                continue;
	            tmp.addAttributeValue("created", attachingIE);
	            isUsed = true;
	        }
	    }
	    if (dbList != null) {
	        for (Iterator it = dbList.iterator(); it.hasNext();) {
	            tmp = (GKInstance)it.next();
	            tmp.addAttributeValue("modified", attachingIE);
	            isUsed = true;
	        }
	    }
	    if (isUsed) {
	        return attachingIE; 
	    }
	    else
	        return null; // null if no assignment is done.
	}
	
	public GKInstance attachDefaultIEToDBInstances(List instances, GKInstance defaultIE) {
	    if (instances == null || instances.size() == 0)
	        return null;
	    if (defaultIE == null)
	        return null;
	    GKInstance editClone = (GKInstance) defaultIE.clone();
	    try {
	        editClone.addAttributeValue("dateTime", GKApplicationUtilities.getDateTime());
	        editClone.setDisplayName(InstanceDisplayNameGenerator.generateDisplayName(editClone));
	        GKInstance tmp = null;
	        for (Iterator it = instances.iterator(); it.hasNext();) {
	            tmp = (GKInstance)it.next();
	            tmp.addAttributeValueNoCheck("modified", editClone); // Since they are database instances, 
	                                                          // only modified slot should be changed.
	        }
	        return editClone;
	    }
	    catch (Exception e) {
	        System.err.println("DefaultInstanceEditHeleper.attachDefaultInstanceEdit(): " + e);
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public void detachDefaultInstanceEdit(GKInstance instanceEdit, 
	                                      Collection localSet, 
	                                      Collection dbList) {
		if (instanceEdit == null)
			return ;
		if (localSet != null) {
			for (Iterator it = localSet.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				instance.removeAttributeValueNoCheck("created", instanceEdit);
			}
		}
		if (dbList != null) {
			for (Iterator it = dbList.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				instance.removeAttributeValueNoCheck("modified", instanceEdit);
			}
		}
		// Have to delete InstanceEdit
		PersistenceManager.getManager().getActiveFileAdaptor().deleteInstance(instanceEdit);
	}
	
	public void detachDefaultInstanceEditFromDBInstances(List instances, 
	                                                     GKInstance instanceEdit) {
	    if (instanceEdit == null || instances == null || instances.size() == 0)
	        return ;
	    GKInstance event = null;
	    for (Iterator it = instances.iterator(); it.hasNext();) {
	        event = (GKInstance) it.next();
	        event.removeAttributeValueNoCheck("created", instanceEdit);
	    }
	}
	
	public void refresh() {
		instanceEdit = null; // Force it to reload
	}

}