/*
 * Created on Jun 11, 2004
 */
package org.gk.gkCurator;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.util.GKApplicationUtilities;

/**
 * A helper class to export instances as a protege pin or ppj files.
 * @author wugm
 */
public class ProtegeExporter {
	private final String PARENT_DIR = "resources" + File.separator;
	// To control if a system wide default InstanceEdit is needed
	private boolean isDefaultInstanceNeeded;
	
	public ProtegeExporter() {
	}
	
	public void exportAsPinsFile(Component parentComp) {
		// Need to get a file for saving
		JFileChooser fileChooser = new JFileChooser();
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return true;
				String name = file.getName();
				if (name.endsWith(".pins"))
					return true;
				return false;
			}
			public String getDescription() {
				return "Protege Pins File";
			}
		};
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setDialogTitle("Choose a pins file name...");
		File file = GKApplicationUtilities.chooseSaveFile(fileChooser, ".pins", parentComp);
		if (file == null)
			return;
		try {			
			exportInstances(file);
		}
		catch (Exception e) {
			System.err.println("ProtegeExporter.exportAsPinsFile(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(parentComp,
			                              "Cannot export as protege pins file: " + e.getMessage(),
			                              "Error in Exporting",
			                              JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void validateDefaultInstanceEdit() {
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		try {
			Collection c = adaptor.fetchInstancesByClass("InstanceEdit");
			isDefaultInstanceNeeded = true;
			if (c != null) {
				GKInstance instanceEidt = null;
				Boolean isApplyToAll;
				for (Iterator it = c.iterator(); it.hasNext();) {
					instanceEidt = (GKInstance) it.next();
					isApplyToAll = (Boolean)instanceEidt.getAttributeValue("_applyToAllEditedInstances");
					if ((isApplyToAll != null) && (isApplyToAll.booleanValue())) {
						isDefaultInstanceNeeded = false;
						break;
					}
				}
			}
		}
		catch(Exception e) {
			System.err.println("ProtegeExporter.validateDefaultInstanceEdit(): " + e);
			e.printStackTrace();
		}
	}
	
	private void exportInstances(File file) throws Exception {
		// Have to make sure a default InstanceEdit exist
		validateDefaultInstanceEdit();
		XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		GKSchema schema = (GKSchema)adaptor.getSchema();
		GKSchemaClass schemaClass = null;
		GKInstance instance = null;
		FileWriter fileWriter = new FileWriter(file);
		PrintWriter writer = new PrintWriter(fileWriter);
		// To control StringBuffer usage
		StringBuffer buffer = new StringBuffer();
		// Call this before output so that the default person will be in the local project.
		// It might be fetched from the database during setting up the default InstanceEdit.
		if (isDefaultInstanceNeeded) {
		    // Use this method to force fetch
		    GKInstance defaultIE = SynchronizationManager.getManager().getDefaultInstanceEdit(null);
			if (defaultIE == null)
			    throw new IllegalStateException("ProtegeExporter.exportInstance(): Cannot create a default InstanceEdit.");
		}
		for (Iterator it = schema.getClasses().iterator(); it.hasNext();) {
			schemaClass = (GKSchemaClass)it.next();
			Collection instances = adaptor.fetchInstancesByClass(schemaClass, false);
			if (instances == null || instances.size() == 0)
				continue;
			for (Iterator it1 = instances.iterator(); it1.hasNext();) {
				instance = (GKInstance)it1.next();
				exportInstance(instance, schemaClass, writer, buffer);
				writer.println(); // A separator between instances
			}
		}
		// Write default InstanceEdit
		if (isDefaultInstanceNeeded) {
			GKInstance defaultIE = SynchronizationManager.getManager().getDefaultInstanceEdit();
			// Assign value temporarily
			Long tmpDBID = adaptor.getNextLocalID();
			defaultIE.setDBID(tmpDBID);
			defaultIE.setAttributeValue("_applyToAllEditedInstances", Boolean.TRUE);
			exportInstance(defaultIE, (GKSchemaClass)defaultIE.getSchemClass(), writer, buffer);
			defaultIE.removeAttributeValueNoCheck("_applyToAllEditedInstances", Boolean.TRUE);
			defaultIE.removeAttributeValueNoCheck("DB_ID", tmpDBID);
			writer.println();
		}
		writer.close();
		fileWriter.close();
	}
	
	private void exportInstance(GKInstance instance, GKSchemaClass cls, PrintWriter writer, StringBuffer buffer) 
				 throws Exception {
		// Instance Title
		buffer.setLength(0);
		buffer.append("([GK_");
		buffer.append(instance.getDBID());
		buffer.append("] of ");
		buffer.append(cls.getName());
		writer.println(buffer.toString());
		// Instance slots
		GKSchemaAttribute att = null;
		java.util.List values = null;
		// Attributes with values
		java.util.List valueAtts = new ArrayList();
		// To export __is_ghost value
		if (instance.isShell()) {
		    att = (GKSchemaAttribute) cls.getAttribute("_displayName");
		    valueAtts.add(att);
		    att = (GKSchemaAttribute) cls.getAttribute("DB_ID");
		    valueAtts.add(att);
		    att = (GKSchemaAttribute) cls.getAttribute("__is_ghost");
		    valueAtts.add(att);
		    instance.setAttributeValue(att, Boolean.TRUE);
		}
        else {
            for (Iterator it = cls.getAttributes().iterator(); it.hasNext();) {
                att = (GKSchemaAttribute) it.next();
                values = instance.getAttributeValuesList(att);
                if (values != null && values.size() > 0) {
                    // Don't export a negative DB_ID
                    if (att.getName().equals("DB_ID")) {
                        Long dbID = (Long) values.get(0);
                        if (dbID.longValue() < 0)
                            continue;
                    }
                    valueAtts.add(att);
                }
            }
        }
		boolean isLastOne = false;
		for (Iterator it = valueAtts.iterator(); it.hasNext();) {
			att = (GKSchemaAttribute) it.next();
			if (!it.hasNext())
				 isLastOne = true;
			values = instance.getAttributeValuesList(att);
			if (values.size() == 1) { // One line value
				buffer.setLength(0);
				buffer.append("\t("); 
				buffer.append(att.getName());
				buffer.append(" ");
				if (att.isInstanceTypeAttribute()) {
					GKInstance value = (GKInstance) values.get(0);
					buffer.append("[GK_");
					buffer.append(value.getDBID());
					buffer.append("])");
				}
				else {
					if (att.getTypeAsInt() == SchemaAttribute.STRING_TYPE) {
						buffer.append("\"");
						buffer.append(escape(values.get(0).toString()));
						buffer.append("\")");
					}
					else {
						buffer.append(values.get(0).toString().toUpperCase());
						buffer.append(")");
					}
				}
				if (isLastOne)
					buffer.append(")"); // To close the instance
				writer.println(buffer.toString());
			}
			else { // Multiple lines value
				buffer.setLength(0);
				buffer.append("\t(");
				buffer.append(att.getName());
				writer.println(buffer.toString());
				if (att.isInstanceTypeAttribute()) {
					for (Iterator it1 = values.iterator(); it1.hasNext();) {
						GKInstance value = (GKInstance) it1.next();
						buffer.setLength(0);
						buffer.append("\t\t[GK_");
						buffer.append(value.getDBID());
						buffer.append("]");
						if (!it1.hasNext()) {
							buffer.append(")"); // close it
							if (isLastOne)
								buffer.append(")"); // TO close the instance
						}
						writer.println(buffer.toString());
					}
				}
				else if (att.getTypeAsInt() == SchemaAttribute.STRING_TYPE){
					for (Iterator it1 = values.iterator(); it1.hasNext();) {
						Object value = it1.next();
						buffer.setLength(0);
						buffer.append("\t\t\"");
						buffer.append(escape(value.toString()));
						buffer.append("\"");
						if (!it1.hasNext()) {
							buffer.append(")");
							if (isLastOne)
								buffer.append(")");
						}
						writer.println(buffer.toString());
					}
				}
				else {
					for (Iterator it1 = values.iterator(); it1.hasNext();) {
						Object value = it1.next();
						buffer.setLength(0);
						buffer.append("\t\t");
						buffer.append(value.toString().toUpperCase());
						if (!it1.hasNext()) {
							buffer.append(")");
							if (isLastOne)
								buffer.append(")");
						}
						writer.println(buffer.toString());
					}
				}
			}
		}
		// Set back __is_ghost
		if (instance.isShell()) {
		    instance.setAttributeValue("__is_ghost", null);
		}
	}
	
	public void exportAsProject(Component parentComp) {
		// Need to get a file for saving
		JFileChooser fileChooser = new JFileChooser();
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return true;
				String name = file.getName();
				if (name.endsWith(".pprj"))
					return true;
				return false;
			}
			public String getDescription() {
				return "Protege Project File";
			}
		};
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setDialogTitle("Choose a project file name...");
		File file = GKApplicationUtilities.chooseSaveFile(fileChooser, ".pprj", parentComp);
		if (file == null)
			return;
		try {
			String ppjName = file.toString();
			int index = ppjName.lastIndexOf(".pprj");
			String pinsName = ppjName.substring(0, index) + ".pins";
			String pontName = ppjName.substring(0, index) + ".pont";
			createPinsFile(pinsName);
			// Copy project file from templates
			copyProjectFile(file);
			// Copy pont file
			copyPontFile(new File(pontName));
		}
		catch(Exception e) {
			System.err.println("ProtegeExporter.exportAsProject(): " + e);
			e.printStackTrace();
			JOptionPane.showMessageDialog(parentComp,
			                              "Cannot export as a protege project: " + e.getMessage(),
			                              "Error in Exporting",
			                              JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void createPinsFile(String pinsFileName) throws Exception {
		// create a temp file
		String tmpPinsName = pinsFileName.toString() + ".tmp";
		File tmpFile = new File(tmpPinsName);
		exportInstances(tmpFile); 
		// Copy stub
		String templateName = PARENT_DIR + "protege_stub.pins";
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 10); // 10k
		int size;
		FileInputStream fis = new FileInputStream(templateName);
		FileChannel source = fis.getChannel();
		FileOutputStream fos = new FileOutputStream(pinsFileName);
		FileChannel output = fos.getChannel();
		while ((size = source.read(buffer)) > 0) {
			buffer.flip();
			output.write(buffer);
			buffer.clear();
		}
		fis.close();
		source.close();
		// Add line sepeartor
		String lineSeperator = System.getProperty("line.separator");
		buffer.put(lineSeperator.getBytes());
		buffer.put(lineSeperator.getBytes()); // Need two lines
		buffer.flip();
		output.write(buffer);
		buffer.clear();
		// Copy contents
		fis = new FileInputStream(tmpFile);
		source = fis.getChannel();
		while ((size = source.read(buffer)) > 0) {
			buffer.flip();
			output.write(buffer);
			buffer.clear();
		}
		source.close();
		fis.close();
		output.close();
		fos.close();
		// Have to delete the temp file
		tmpFile.delete();
	}
	
	private void copyPontFile(File pontFile) throws IOException {
		String templateName = PARENT_DIR + "protege.pont";
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 10); // 10k
		int size;
		FileInputStream fis = new FileInputStream(templateName);
		FileChannel source = fis.getChannel();
		FileOutputStream fos = new FileOutputStream(pontFile);
		FileChannel output = fos.getChannel();
		while ((size = source.read(buffer)) > 0) {
			buffer.flip();
			output.write(buffer);
			buffer.clear();
		}
		source.close();
		fis.close();
		output.close();
		fos.close();
	}
	
	private void copyProjectFile(File pprjFile) throws IOException {
		String fileName = pprjFile.getName();
		int index = fileName.indexOf(".pprj");
		fileName = fileName.substring(0, index);
		String templateName = PARENT_DIR + "protege.pprj";
		FileReader reader = new FileReader(templateName);
		BufferedReader bufferedReader = new BufferedReader(reader);
		FileWriter writer = new FileWriter(pprjFile);
		PrintWriter printWriter = new PrintWriter(writer);
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
			index = line.indexOf("$1");
			if (index > 0) {
				if (line.indexOf(".pins", index) > 0) {
					line = line.replaceAll("\\$1.pins", fileName + ".pins");
				}
				else if (line.indexOf(".pont", index) > 0) {
					line = line.replaceAll("\\$1.pont", fileName + ".pont");
				}
			}
			printWriter.println(line);
		}
		writer.close();
		printWriter.close();
	}
	
	private String escape(String value) {
	    StringBuffer buffer = new StringBuffer();
	    int size = value.length();
	    char c;
	    for (int i = 0; i < size; i++) {
	        c = value.charAt(i);
	        if (c == '"')
	            buffer.append("\\\"");
	        else if (c == '\n')
	            buffer.append("\\n");
	        else
	            buffer.append(c);
	    }
	    return buffer.toString();
	}
}