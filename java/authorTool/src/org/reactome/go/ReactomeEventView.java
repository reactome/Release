/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.gk.database.EventCellRenderer;
import org.gk.database.EventTreeBuildHelper;
import org.gk.database.HierarchicalEventPane;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized HierarchicalEventView.
 * @author wugm
 */
public class ReactomeEventView extends HierarchicalEventPane {
	private final String EVENT_URL = "http://brie8.cshl.org/cgi-bin/eventbrowser?DB=gk_central&ID=";
	
	public ReactomeEventView() {
		super();
		init();
	}
	
	private void init() {
		setSpeciesControlVisible(false);
		setIsForDB(true);
		titleLabel.setText("Reactome Events");
		eventTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getClickCount() == 2) {
					viewInstance();
				}
			}
		});
		eventTree.setToggleClickCount(3);
	}
	
	private void viewInstance() {
		java.util.List events = getSelection();
		if (events.size() > 0) {
			GKInstance event = (GKInstance) events.get(0);
			Long dbID = event.getDBID();
			String url = EVENT_URL + dbID;
			try {
				if (AppletHelper.getHelper() == null) {
					BrowserLauncher.displayURL(url, this);
				}
				else {
					AppletHelper.getHelper().showDocument(url, "Reactome");
				}
			}
			catch (Exception e) {
				System.err.println("ReactomeEventView.viewInstance(): " + e);
				e.printStackTrace();
			}
		}
	}
	
	protected EventCellRenderer createTreeCellRenderer() {
		return new AppletEventCellRenderer();
	}
	
	protected void initIcons() {
		if (AppletHelper.getHelper() == null) {
			iconIsA = GKApplicationUtilities.getIsAIcon();
			iconPartOf = GKApplicationUtilities.getIsPartOfIcon();
		}
		else {
			iconIsA = AppletHelper.getHelper().getIsAIcon();
			iconPartOf = AppletHelper.getHelper().getIsPartOfIcon();
		}
	}
	
	public void setMySQLAdaptor(MySQLAdaptor dba) {
		try {
			//dba.debug = true;
			// Refresh dba in case the tree is loaded before
			//dba.refresh();
			EventTreeBuildHelper helper = new EventTreeBuildHelper(dba);
			Collection topLevelPathways = helper.getTopLevelEvents();
			Collection c = helper.getAllEvents();
			helper.loadAttribtues(c);
			// Need to fill "hasInstance" for events to create the correct tree
			helper.cacheOfTypeValues(c);
			// Cache GO information
			dba.loadInstanceAttributeValues(c, dba.getSchema().getClassByName("Event").getAttribute("goBiologicalProcess"));
			extractDataForGO(c);
			
			ArrayList list = new ArrayList(topLevelPathways);
			Collections.sort(list, new Comparator() {
				public int compare(Object obj1, Object obj2) {
					GKInstance instance1 = (GKInstance)obj1;
					GKInstance instance2 = (GKInstance)obj2;
					String dn1 = instance1.getDisplayName();
					if (dn1 == null)
						dn1 = "";
					String dn2 = instance2.getDisplayName();
					if (dn2 == null)
						dn2 = "";
					return dn1.compareTo(dn2);
				}
			});
			setTopLevelEvents(list);
			// Have to set event cls as selected schema class manually
			GKSchemaClass eventCls = (GKSchemaClass) dba.getSchema().getClassByName("Event");
		}
		catch (Exception e) {
			System.err.println("ReactomeEventView.setMySQLAdaptor(): " + e);
			e.printStackTrace();
		}
	}
	
	private void extractDataForGO(Collection events) throws Exception {
		GKInstance event = null;
		GKInstance goTerm = null;
		for (Iterator it = events.iterator(); it.hasNext();) {
			event = (GKInstance) it.next();
			goTerm = (GKInstance) event.getAttributeValue("goBiologicalProcess");
			if (goTerm == null)
				continue;
			goTerm.addAttributeValueNoCheck("event", event);
		}
	}
	
	protected void doTreePopup(MouseEvent e) {
		// Overide actions in the superclass.
	}
	
	public void setHiliteGOEvents(boolean hilite) {
		AppletEventCellRenderer renderer = (AppletEventCellRenderer) eventTree.getCellRenderer();
		renderer.setNeedHilite(hilite);
	}
}
