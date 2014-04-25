/*
 * Created on May 25, 2006
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.List;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

/** 
 * Utility methods
 *  
 * @author croft
 */
public class IDGenerationUtils {
	public IDGenerationUtils() {
	}
	
	/**
	 * Pulls InstanceEdits from those slots that contain them.
	 * 
	 * @param instance
	 */
	public static List getInstanceEdits(GKInstance instance) {
		List instanceEditList = new ArrayList();
		List slotInstanceEditList;
		
		try {
			slotInstanceEditList = instance.getAttributeValuesList("authored");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from authored slot");
			e.printStackTrace();
		}
		try {
			slotInstanceEditList = instance.getAttributeValuesList("created");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from created slot");
			e.printStackTrace();
		}
		try {
			slotInstanceEditList = instance.getAttributeValuesList("edited");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from edited slot");
			e.printStackTrace();
		}
		try {
			slotInstanceEditList = instance.getAttributeValuesList("modified");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from modified slot");
			e.printStackTrace();
		}
		try {
			slotInstanceEditList = instance.getAttributeValuesList("reviewed");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from reviewed slot");
			e.printStackTrace();
		}
		try {
			slotInstanceEditList = instance.getAttributeValuesList("revised");
			if (slotInstanceEditList!=null)
				instanceEditList.addAll(slotInstanceEditList);
		} catch (InvalidAttributeException e) {
		} catch (Exception e) {
			System.err.println("IDGenerationUtils.getInstanceEdits: WARNING - problem getting InstanceEdits from revised slot");
			e.printStackTrace();
		}
		
		return instanceEditList;
	}
}