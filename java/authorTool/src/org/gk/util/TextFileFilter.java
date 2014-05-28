/*
 * Created on Sep 5, 2012
 *
 */
package org.gk.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author gwu
 *
 */
public class TextFileFilter extends FileFilter {
    
    public TextFileFilter() {
    }
    
    @Override
    public boolean accept(File f) {
        String fileName = f.getName();
        return fileName.endsWith(".txt");
    }
    @Override
    public String getDescription() {
        return "Text File (.txt)";
    }                    
};