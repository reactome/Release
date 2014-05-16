/*
 * Created on Jun 23, 2004
 */
package org.gk.database.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JFileChooser;

import org.gk.database.FrameManager;
import org.gk.database.GKDatabaseBrowser;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;

/**
 * 
 * @author wugm
 */
public class ReactionToGoMFMap implements DBTool {

	public ReactionToGoMFMap() {
	}

	public void doAction() {
		GKDatabaseBrowser browser = FrameManager.getManager().getBrowser();
		if (browser == null)
			return; // Should work under the Browser enviornment.
		// Ask for a file name
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogTitle("Please choose a file to save the result...");
		int reply = dialog.showSaveDialog(browser);
		if (reply != JFileChooser.APPROVE_OPTION)
			return;
		File file = dialog.getSelectedFile();
		MySQLAdaptor adaptor = browser.getMySQLAdaptor();
		try {
			Collection events = adaptor.fetchInstancesByClass("Reaction");
			// Sort it
			java.util.List eventsList = new ArrayList(events);
			InstanceUtilities.sortInstances(eventsList);
			processReactions(eventsList, file);
		}
		catch(Exception e) {
			System.err.println("EventToGoMapTool.doAction(): " + e);
			e.printStackTrace();
		}
	}
	
	private void processReactions(Collection reactions, File file) {
		String fileName = file.toString();
		int index = fileName.lastIndexOf(".html");
		String txtFileName = null;
		if (index == -1) {
			txtFileName = fileName + ".txt";
		}
		else {
			txtFileName = fileName.substring(0, index) + ".txt";
		}
		File txtFile = new File(txtFileName);
		StringBuffer buffer = new StringBuffer(); 
		// Create a txt file first
		GKInstance reaction = null;
		GKInstance catalystActivity = null;
		GKInstance goActivity = null;
		GKInstance goID = null;
		try {
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				reaction = (GKInstance)it.next();
				buffer.append(reaction.getDisplayName());
				buffer.append(" [");
				buffer.append(reaction.getDBID());
				buffer.append("]");
				catalystActivity = (GKInstance) reaction.getAttributeValue("catalystActivity");
				if (catalystActivity != null) {
					goActivity = (GKInstance) catalystActivity.getAttributeValue("activity");
					if (goActivity != null) {
						goID = (GKInstance) goActivity.getAttributeValue("crossReference");
						if (goID != null) {
							buffer.append(" ----> ");
							buffer.append(goActivity.getDisplayName());
							buffer.append(" [");
							buffer.append(goID.getDisplayName());
							buffer.append("]");
						}
					}
				}
				buffer.append("\n");
			}
			FileWriter writer = new FileWriter(txtFile);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(buffer.toString());
			bufferedWriter.close();
			writer.close();
			// Generate html file
			buffer.setLength(0);
			buffer.append("<html>\n");
			buffer.append("<title>Reactome Reactions to GO Molecular Functions</title>\n");
			buffer.append("<style type=\"text/css\">\n");
			buffer.append("<!--\n");
			buffer.append("a.with {color: blue}\n");
			buffer.append("a.without {color: black}\n");
			buffer.append("-->\n");
			buffer.append("</style>\n");
			buffer.append("</header>\n");
			buffer.append("<body>\n");
			buffer.append("<ul>");
			String reactomeLink = "http://brie8.cshl.org/cgi-bin/eventbrowser?DB=gk_central&ID=";
			String reactomeTarget = "reactome";
			String goLink = "http://www.ebi.ac.uk/ego/QuickGO?mode=display&entry=";
			String goTarget = "go";
			for (Iterator it = reactions.iterator(); it.hasNext();) {
				reaction = (GKInstance)it.next();
				catalystActivity = (GKInstance) reaction.getAttributeValue("catalystActivity");
				goID = null;
				if (catalystActivity != null) {
					goActivity = (GKInstance) catalystActivity.getAttributeValue("activity");
					if (goActivity != null) {
						goID = (GKInstance) goActivity.getAttributeValue("crossReference");
					}
				}
				buffer.append("<li>");
				buffer.append("<a href=\"");
				buffer.append(reactomeLink);
				buffer.append(reaction.getDBID());
				buffer.append("\" target=\"reactome\"");
				if (goID != null)
					buffer.append(" class=\"with\">");
				else 
					buffer.append(" class=\"without\">");
				buffer.append(reaction.getDisplayName());
				buffer.append("</a>");
				if (goID != null) {
					buffer.append(" [<a href=\"");
					buffer.append(goLink);
					buffer.append(goID.getDisplayName());
					buffer.append("\" target=\"go\">");
					buffer.append(goActivity.getDisplayName());
					buffer.append("</a>]");					
				}
				buffer.append("</li>");
			}
			buffer.append("<ol>");
			buffer.append("</body>\n");
			buffer.append("</html>\n");
			writer = new FileWriter(file);
			bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(buffer.toString());
			bufferedWriter.close();
			writer.close();
		}
		catch (Exception e) {
			System.err.println("ReactionToGOMFMap.processReactions(): " + e);
			e.printStackTrace();
		}
	}

	public String getTitle() {
		return "Reaction to GO Map";
	}

}
