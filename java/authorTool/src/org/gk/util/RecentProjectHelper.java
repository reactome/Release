/*
 * Created on Jan 25, 2005
 *
 */
package org.gk.util;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * This is a helper class to handle recent project menus that can be attached
 * to file menu. Those project menus are used in the author tool and the curator
 * tool.
 * 
 * @author wgm
 */
public class RecentProjectHelper {
    private final String PROJECT_KEY = "recentProject";
    private int totalPrjNumber; // The total list project
    private JMenuItem topAnchorItem; // Project menus should be listed under
                                   // this menu item with a separator. It can be a JMenu
    private JMenuItem bottomAnchorItem; // Project menus should be listed above
                                        // this menu item with a separator.
    private JMenu fileMenu; // The project menu items should be attached to this JMenu
    private ActionListener projectAction; // To be attached to project menu
    private List projectNames; // A list of project names, which should be file names.
    
    public RecentProjectHelper() {
    }
    
    public void setTotalProjectNumber(int number) {
        this.totalPrjNumber = number;
    }
    
    public int getTotalProjectNumber() {
        return this.totalPrjNumber;
    }
    
    public void setFileMenu(JMenu menu) {
        this.fileMenu = menu;
    }
    
    public void setProjectActionListener(ActionListener l) {
        this.projectAction = l;
    }
    
    public void setTopAnchorItem(JMenuItem item) {
        this.topAnchorItem = item;
    }
    
    public void setBottomAnchorItem(JMenuItem item) {
        this.bottomAnchorItem = item;
    }
    
    /**
     * Set a list of project names.
     * @param projects a list of file names.
     */
    public void setRecentProjects(List projects) {
        projectNames = projects;
    }
    
    public List getRecentProjects() {
        return this.projectNames;
    }
    
    private void updateRecentProjectItems() {
       	// Add a new JMenuItem
		int index = 0;
		for (int i = 0; i < fileMenu.getItemCount(); i ++) {
			if (fileMenu.getItem(i) == topAnchorItem) {
				index = i + 1; // Consider Separator.
				break;
			}
		}  
		// Rebuild the menu items below index
		if (index < fileMenu.getItemCount()) {
		    int remaining = fileMenu.getItemCount();
		    for (int i = index; i < remaining; i++)
		        fileMenu.remove(index);
		}
		// Check if there are any recent project items
		if (projectNames.size() > 0) {
			JMenuItem item = null;
			String sourceName = null;
			fileMenu.addSeparator();
		    for (int i = 0; i < projectNames.size(); i++) {
		        sourceName = (String) projectNames.get(i);
		        item = new JMenuItem((i + 1) + " " + generateMenuLabel(sourceName));
		        item.setToolTipText(sourceName);
		        fileMenu.add(item);
		        item.addActionListener(projectAction);
		    }
		}
		// Add back the bottom menu item
		if (bottomAnchorItem != null) {
		    fileMenu.addSeparator();
		    fileMenu.add(bottomAnchorItem);
		}
    }

    private String generateMenuLabel(String projectName) {
    	int max = 25;
    	if (projectName.length() < max)
    		return projectName;
    	int index = projectName.indexOf(File.separator);
    	String driverLabel = projectName.substring(0, index);
    	String rtn = null;
    	int totalLength = projectName.length();
    	index = totalLength;
    	int prevIndex = index;
    	int c = 0;
    	while (index >= 0) {
    		index = projectName.lastIndexOf(File.separator, index - 1);
			if (totalLength - index > max) {
				String tmp = null;
				if (c == 0) // Use the file name only.
					tmp = projectName.substring(index + 1, index + max) + "...";
				else
					tmp = projectName.substring(prevIndex + 1);
				rtn = driverLabel + File.separator + "..." + File.separator + tmp;
				break;
			}
			prevIndex = index;
			c ++;
		}
    	return rtn;
    }
    
    public void addRecentProject(String projectName) {
    	if (projectNames == null)
    	    projectNames = new ArrayList();
        if (projectNames.contains(projectName)) {
    		return;
    	}
    	projectNames.add(0, projectName);
    	// To control the size of the recent project
    	if (projectNames.size() > totalPrjNumber) {
    		projectNames.remove(totalPrjNumber);
    	}
    	updateRecentProjectItems();
    }
    
    public String getRecentProject(int index) {
        if (projectNames == null)
            return null;
        return (String) projectNames.get(index);
    }
    
    public void removeProject(int index) {
        if (projectNames == null)
            return ;
        projectNames.remove(index);
        updateRecentProjectItems();
    }
    
    /**
     * Switch the project specied by index to the top.
     * @param index
     */
    public void switchToTop(int index) {
        if (projectNames == null)
            return;
        if (index == 0)
            return; // Already at the top of the list.
        String prjName = (String) projectNames.remove(index);
        projectNames.add(0, prjName);
        updateRecentProjectItems();
    }
    
    public List getProjectMenus() {
        if (projectNames == null || projectNames.size() == 0 || projectAction == null)
            return new ArrayList();
        List rtn = new ArrayList(projectNames.size());
        int index = 1;
        for (Iterator it = projectNames.iterator(); it.hasNext();) {
            String prjName = (String) it.next();
            JMenuItem item = new JMenuItem();
            item.setText(index + " " + generateMenuLabel(prjName));
            item.setToolTipText(prjName); // In case the name is too long
            item.addActionListener(projectAction);
            rtn.add(item);
            index ++;
        }
        return rtn;
    }
    
    public void storeProjects(Properties prop) {
        // Clear them in case the old names are not correct
        for (int i = 0; i < totalPrjNumber; i++)
            prop.remove(PROJECT_KEY + i);
        if (projectNames != null && projectNames.size() > 0) {
            for (int i = 0; i < projectNames.size(); i++) {
                prop.setProperty(PROJECT_KEY + i, projectNames.get(i).toString());
            }
        }
    }
    
    public void loadProjects(Properties prop) {
        if (projectNames == null)
            projectNames = new ArrayList(totalPrjNumber);
        else
            projectNames.clear();
        for (int i = 0; i < totalPrjNumber; i++) {
            String fileName = prop.getProperty(PROJECT_KEY + i);
            if (fileName != null)
                projectNames.add(fileName);
            else
                break;
        }
    }
}
