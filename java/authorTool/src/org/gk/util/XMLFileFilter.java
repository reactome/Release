/*
 * Created on Feb 2, 2004
 */
package org.gk.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * A customized FileFilter for xml files. 
 */
public class XMLFileFilter extends FileFilter {
	
	public XMLFileFilter() {
	}
	
	public boolean accept(File file) {
		if (file.isDirectory())
			return true;
		String fileName = file.getName();
		int index = fileName.lastIndexOf(".");
		String extName = fileName.substring(index + 1);
		if (extName.equalsIgnoreCase("xml"))
			return true;
		return false;
	}
	
	public String getDescription() {
		return "XML Files (*.xml)";
	}
}