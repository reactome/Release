/*
 * Created on May 17, 2004
 */
package org.gk.database.util;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JFileChooser;

import org.gk.database.FrameManager;
import org.gk.database.GKDatabaseBrowser;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;

/**
 * 
 * @author wugm
 */
public class EventToGoMapTool implements DBTool {

	public EventToGoMapTool() {
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
			Collection events = adaptor.fetchInstancesByClass("Event");
			processEvents(events, file);
			String newFile = file.getAbsolutePath() + "empty";
			processEvents1(events, new File(newFile));
		}
		catch(Exception e) {
			System.err.println("EventToGoMapTool.doAction(): " + e);
			e.printStackTrace();
		}
	}

	public String getTitle() {
		return "Map Events to GO";
	}
	
	private void processEvents1(Collection events, File file) throws Exception {
		GKInstance event = null;
		GKInstance goProcess = null;
		FileWriter writer = new FileWriter(file);
		PrintWriter printWriter = new PrintWriter(writer);
		StringBuffer buffer = new StringBuffer();
		// Title name
		buffer.append("GK_ID");
		buffer.append("\t");
		buffer.append("GK_DisplayName");
		printWriter.println(buffer.toString());
		int c = 0;
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
			goProcess = (GKInstance) event.getAttributeValue("goBiologicalProcess");
			if (goProcess != null)
				continue;
			buffer.setLength(0);
			buffer.append(event.getDBID());
			buffer.append("\t");
			buffer.append(event.getDisplayName());
			printWriter.println(buffer.toString());
			c ++;
		}
		printWriter.close();
		writer.close();
		System.out.println("Count: " + c);
	}
	
	private void processEvents(Collection events, File file) throws Exception {
		GKInstance event = null;
		GKInstance goProcess = null;
		GKInstance crossRef = null;
		FileWriter writer = new FileWriter(file);
		PrintWriter printWriter = new PrintWriter(writer);
		StringBuffer buffer = new StringBuffer();
		// Title name
		buffer.append("GK_ID");
		buffer.append("\t");
		buffer.append("GK_DisplayName");
		buffer.append("\t");
		buffer.append("GO_ID");
		buffer.append("\t");
		buffer.append("GO_DisplayName");
		printWriter.println(buffer.toString());
		int c = 0;
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
			goProcess = (GKInstance) event.getAttributeValue("goBiologicalProcess");
			if (goProcess == null)
				continue;
			crossRef = (GKInstance) goProcess.getAttributeValue("crossReference");
			buffer.setLength(0);
			buffer.append(event.getDBID());
			buffer.append("\t");
			buffer.append(event.getDisplayName());
			buffer.append("\t");
			if (crossRef != null)
				buffer.append(crossRef.getDisplayName());
			else 
				buffer.append("null");
			buffer.append("\t");
			buffer.append(goProcess.getDisplayName());
			printWriter.println(buffer.toString());
			c ++;
		}
		printWriter.close();
		writer.close();
		System.out.println("Count: " + c);
	}

}
