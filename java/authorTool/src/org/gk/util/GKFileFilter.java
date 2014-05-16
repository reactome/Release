/*
 * Created on Feb 2, 2004
 */
package org.gk.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * A customized FileFilter for GKB files.
 */
public class GKFileFilter extends FileFilter {
	private String extName = ".gkb"; // Default for the author tool
	private String desc = "Reactome Project Files (*.gkb)";
    
	public GKFileFilter() {
	}
	
	public GKFileFilter(String extName, String desc) {
	    this.extName = extName;
	    this.desc = desc;
	}
	
	public boolean accept(File file) {
		if (file.isDirectory())
			return true;
		String fileName = file.getName();
		int index = fileName.lastIndexOf(".");
		if (index == -1)
		    return false; // No ext not accepted
		String ext = fileName.substring(index);
		if (extName.equalsIgnoreCase(ext))
			return true;
		return false;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public String getExtName() {
	    return extName;
	}
}