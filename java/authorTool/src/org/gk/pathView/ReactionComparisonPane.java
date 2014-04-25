/*
 * Created on Jun 2, 2004
 */
package org.gk.pathView;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.database.InstanceComparisonPane;
import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JPanel to list reactions that are different between two
 * reactome databases.
 * @author wugm
 */
public class ReactionComparisonPane extends JPanel {
	private InstanceListPane newReactionPane;
	private InstanceListPane deleteReactionPane;
	private InstanceListPane changedReactionPane; 
	// Cache MySQLAdaptor information
	private MySQLAdaptor oldAdaptor;
	private MySQLAdaptor newAdaptor;
	// To control the diff JFrames
	private Map diffFrameMap = new HashMap();

	public ReactionComparisonPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		newReactionPane = new InstanceListPane();
		newReactionPane.setTitle("New Reactions");
		deleteReactionPane = new InstanceListPane();
		deleteReactionPane.setTitle("Deleted Reactions"); 
		changedReactionPane = new InstanceListPane();
		changedReactionPane.setTitle("Reactions with precedingEvent Slot Changed");
		// To customize the double click actions
		changedReactionPane.setMouseDoubleClickAction(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					java.util.List selected = changedReactionPane.getSelection();
					if (selected.size() != 1)
						return;
					GKInstance instance = (GKInstance) selected.get(0); // This instance if from newAdaptor
					showDiff(instance);
				}
			}
		});
		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, newReactionPane, deleteReactionPane);
		JSplitPane jsp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jsp, changedReactionPane);
		jsp.setResizeWeight(0.5);
		jsp1.setResizeWeight(0.67);
		jsp.setOneTouchExpandable(true);
		jsp1.setOneTouchExpandable(true);
		add(jsp1, BorderLayout.CENTER);
		// To control the selection
		newReactionPane.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		deleteReactionPane.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		changedReactionPane.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		newReactionPane.addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (newReactionPane.getSelection().size() > 0) {
					deleteReactionPane.clearSelection();
					changedReactionPane.clearSelection();
				}
			}
		});
		deleteReactionPane.addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (deleteReactionPane.getSelection().size() > 0) {
					newReactionPane.clearSelection();
					changedReactionPane.clearSelection();
				}
			}
		});
		changedReactionPane.addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (changedReactionPane.getSelection().size() > 0) {
					newReactionPane.clearSelection();
					deleteReactionPane.clearSelection();
				}
			}
		});
	}
	
	/**
	 * Show difference for an Instance in the changedReactionPane.
	 * @param instance a new GKInstance listed in the changedReactionPane.
	 */
	private void showDiff(final GKInstance newInstance) {
		JFrame diffFrame = (JFrame) diffFrameMap.get(newInstance);
		if (diffFrame != null) {
			diffFrame.setState(JFrame.NORMAL);
			diffFrame.toFront();
			return;
		}
		try {
			GKInstance oldInstance = oldAdaptor.fetchInstance("Reaction", newInstance.getDBID());
			InstanceComparisonPane comparisonPane = new InstanceComparisonPane();
			comparisonPane.setInstances(oldInstance, newInstance, oldAdaptor.toString(), newAdaptor.toString());
			comparisonPane.hideMergeBtn();
			diffFrame = new JFrame("Reaction Comparison for \"" + newInstance.getDisplayName() + "\"");
			diffFrame.setSize(600, 800);
			GKApplicationUtilities.center(diffFrame);
			diffFrame.getContentPane().add(comparisonPane, BorderLayout.CENTER);
			diffFrameMap.put(newInstance, diffFrame);
			diffFrame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					diffFrameMap.remove(newInstance);
				}
				public void windowClosed(WindowEvent e) {
					diffFrameMap.remove(newInstance);
				}
			});
			diffFrame.setVisible(true);
		}
		catch(Exception e) {
			System.err.println("ReactionComparisonPane.showDiff(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the MySQLAdatptors for the two databases.
	 * @param adaptor1 the old database.
	 * @param adaptor2 the new database
	 */
	public void setMySQLAdaptors(MySQLAdaptor adaptor1, MySQLAdaptor adaptor2) {
		try {
			Collection events1 = adaptor1.fetchInstancesByClass("Reaction");
			// Convert to map to increase the performace
			Map eventMap1 = new HashMap();
			GKInstance instance = null;
			for (Iterator it = events1.iterator(); it.hasNext();) {
				instance = (GKInstance) it.next();
				eventMap1.put(instance.getDBID(), instance);
			}
			Collection events2 = adaptor2.fetchInstancesByClass("Reaction");
			Map eventMap2 = new HashMap();
			for (Iterator it = events2.iterator(); it.hasNext();) {
				instance = (GKInstance) it.next();
				eventMap2.put(instance.getDBID(), instance);
			}
			setUpGUIs(eventMap1, eventMap2);
			oldAdaptor = adaptor1;
			newAdaptor = adaptor2;
			// Change the titles
			newReactionPane.setTitle("Reactions added at " + adaptor2.toString());
			deleteReactionPane.setTitle("Reactions deleted at " + adaptor2.toString());
		}
		catch(Exception e) {
			System.err.println("ReactionComparisonPane.setMySQLAdaptor(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * @param eventMap1 from the old database
	 * @param eventMap2 from the new database
	 */
	private void setUpGUIs(Map eventMap1, Map eventMap2) {
		// Check if any new events are added in the new database
		java.util.List newEvents = new ArrayList();
		Object dbID = null;
		for (Iterator it = eventMap2.keySet().iterator(); it.hasNext();) {
			dbID = it.next();
			if (!eventMap1.containsKey(dbID)) {
				newEvents.add(eventMap2.get(dbID));
			}
		}
		if (newEvents.size() > 0) {
			InstanceUtilities.sortInstances(newEvents);
			newReactionPane.setDisplayedInstances(newEvents);
		}
		// Check if any events are deleted in the new database
		java.util.List deleteEvents = new ArrayList();
		for (Iterator it = eventMap1.keySet().iterator(); it.hasNext();) {
			dbID = it.next();
			if (!eventMap2.containsKey(dbID))
				deleteEvents.add(eventMap1.get(dbID));
		}
		if (deleteEvents.size() > 0){
			InstanceUtilities.sortInstances(deleteEvents);
			deleteReactionPane.setDisplayedInstances(deleteEvents);
		}
		// Check if any events whose precedingEvent slot have been changed.
		java.util.List changedEvents = new ArrayList();
		for (Iterator it = eventMap1.keySet().iterator(); it.hasNext();) {
			dbID = it.next();
			GKInstance event1 = (GKInstance) eventMap1.get(dbID);
			GKInstance event2 = (GKInstance) eventMap2.get(dbID);
			if (event1 == null || event2 == null)
				continue;
			if (!comparePrecedingEvents(event1, event2))
				changedEvents.add(event2);
		}
		if (changedEvents.size() > 0) {
			InstanceUtilities.sortInstances(changedEvents);
			changedReactionPane.setDisplayedInstances(changedEvents);
		}
	}
	
	/**
	 * Compare the precedingEvent slot values between two GKInstance objects.
	 * @param event1
	 * @param event2
	 * @return
	 */
	private boolean comparePrecedingEvents(GKInstance event1, GKInstance event2) {
		try {
			java.util.List preEvents1 = event1.getAttributeValuesList("precedingEvent");
			Set set1 = null;
			GKInstance instance = null;
			if (preEvents1 != null) {
				set1 = new HashSet(preEvents1.size());
				for (Iterator it = preEvents1.iterator(); it.hasNext();) {
					instance = (GKInstance) it.next();
					set1.add(instance.getDBID());
				}
			}
			else
				set1 = new HashSet();
			java.util.List preEvents2 = event2.getAttributeValuesList("precedingEvent");
			Set set2 = null;
			if (preEvents2 != null) {
				set2 = new HashSet(preEvents2.size());
				for (Iterator it = preEvents2.iterator(); it.hasNext();) {
					instance = (GKInstance) it.next();
					set2.add(instance.getDBID());
				}
			}
			else
				set2 = new HashSet();
			return set1.equals(set2);
		}
		catch (Exception e) {
			System.err.println("ReactionComparisonPane.comparePrecedingEvents(): " + e);
			e.printStackTrace();
			return false;
		}
	}
	
	public InstanceListPane getNewReactionPane() {
		return newReactionPane;
	}
	
	public InstanceListPane getDeleteReactionPane() {
		return deleteReactionPane;
	}
	
	public InstanceListPane getChangedReactionPane() {
		return changedReactionPane;
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ReactionComparisonPane pane = new ReactionComparisonPane();
		frame.getContentPane().add(pane, BorderLayout.CENTER);
		frame.setSize(400, 800);
		frame.setVisible(true);
		try {
			MySQLAdaptor dba1 = new MySQLAdaptor("localhost",
			                                     "gk_sky",
			                                     "wgm",
			                                     "wgm",
			                                     3306);
			MySQLAdaptor dba2 = new MySQLAdaptor("localhost",
												 "gk_central_innodb",
												 "wgm",
												 "wgm",
												 3306);
			pane.setMySQLAdaptors(dba1, dba2);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
