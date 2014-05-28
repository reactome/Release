/*
 * Created on an 4, 2006
 */
package org.gk.IDGeneration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.gk.util.GKApplicationUtilities;

/**
 * Provides a utility class for accessing system properties.
 * 
 * @author croft
 */
public class SystemProperties {
	private static Properties systemProperties = null;
	private static String SYSTEM_PROPERTIES_FILENAME = "id_generation.prop";
	
	private SystemProperties() {
	}
	
	/** 
	 *  Finds property file and reads in properties from it,
	 *  if properties have not yet been read in already.
	 */
	public static Properties retrieveSystemProperties() {
		if (systemProperties!=null)
			return systemProperties;
		
		systemProperties = new Properties();
		File file = null;
		try {
		    file = GKApplicationUtilities.getPropertyFile(SYSTEM_PROPERTIES_FILENAME);
		}
		catch(Exception e) {
		    System.err.println("IDGenerationPersistenceManagers.getSystemProperties(): " + e);
		    e.printStackTrace();
		}
		if (file != null && file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				systemProperties.load(fis);
				fis.close();
			}
			catch (IOException e) {
				System.err.println("IDGenerationPersistenceManagers.retrieveSystemProperties: problem getting properties from file");
				e.printStackTrace();
			}
		}
		
		return systemProperties;
	}
	
	/** 
	 *  Finds property file and writes properties to it.
	 */
	public static void storeSystemProperties() {
		if (systemProperties==null)
			return;
		
		File file = null;
		try {
		    file = GKApplicationUtilities.getPropertyFile(SYSTEM_PROPERTIES_FILENAME);
		}
		catch(Exception e) {
		    System.err.println("IDGenerationPersistenceManagers.storeSystemProperties(): " + e);
		    e.printStackTrace();
		}
		if (file != null && file.exists()) {
			try {
				FileOutputStream fos = new FileOutputStream(file);
			    systemProperties.store(fos, SYSTEM_PROPERTIES_FILENAME);
				fos.close();
			}
			catch (IOException e) {
				System.err.println("IDGenerationPersistenceManagers.storeSystemProperties(): " + e);
				e.printStackTrace();
			}
		}
	}
}
