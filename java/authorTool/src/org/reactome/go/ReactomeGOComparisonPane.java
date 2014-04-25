/*
 * Created on Jun 16, 2004
 */
package org.reactome.go;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.database.SearchPane;
import org.gk.model.GKInstance;
import org.gk.persistence.MySQLAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JPanel for comparing Events in Reactome and Biological Processes
 * in Gene Ontology (GO).
 * @author wugm
 */
public class ReactomeGOComparisonPane extends JPanel {
	// For reactome
	private ReactomeEventView eventPane;
	private AppletSingleEventView singleEventView;
	// For GO
	private GOBPTreeView goPane;
	private SingleGOBPTreeView singleGOView;
	// To synchronize selection between goPane and eventPane
	private JCheckBox synchronizeBox;
	private TreeSelectionListener eventListener;
	private TreeSelectionListener goListener;

	public ReactomeGOComparisonPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		singleEventView = new AppletSingleEventView();
		singleGOView = new SingleGOBPTreeView();
		JSplitPane singleViewJSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
		                                          singleEventView,
		                                          singleGOView);
		singleViewJSP.setOneTouchExpandable(true);
		singleViewJSP.setResizeWeight(0.5);
		eventPane = new ReactomeEventView();
		JSplitPane leftJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		                                    eventPane,
		                                    singleViewJSP);
		leftJSP.setResizeWeight(0.5);
		leftJSP.setOneTouchExpandable(true);
		goPane = new GOBPTreeView();
		JSplitPane rightJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		                                     leftJSP,
		                                     goPane);
		rightJSP.setOneTouchExpandable(true);
		rightJSP.setResizeWeight(0.67);
		add(rightJSP, BorderLayout.CENTER);
		// Make sure scrollable
		Dimension minSize = new Dimension(20, 20);
		singleEventView.setMinimumSize(minSize);
		singleGOView.setMinimumSize(minSize);
		eventPane.setMinimumSize(minSize);
		goPane.setMinimumSize(minSize);
		add(createToolbar(), BorderLayout.NORTH);
		// Install listeners
		installListeners();
	}
	
	private JToolBar createToolbar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		JButton searchBtn = new JButton("Search",
		                                createIcon("Search16.gif"));
		searchBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		toolbar.add(searchBtn);
		final JCheckBox hiliteEventBox = new JCheckBox("Highlight Events with GO Assignment");
		hiliteEventBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				eventPane.setHiliteGOEvents(hiliteEventBox.isSelected());
				eventPane.repaint();
				singleEventView.setHiliteGOEvents(hiliteEventBox.isSelected());
				singleEventView.repaint();
			}
		});
		toolbar.add(hiliteEventBox);
		final JCheckBox hiliteGOBox = new JCheckBox("Highlight GO Terms assigned to Events");
		hiliteGOBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				goPane.setNeedHilite(hiliteGOBox.isSelected());
				goPane.repaint();
				singleGOView.setNeedHilite(hiliteGOBox.isSelected());
				singleGOView.repaint();
			}
		});
		toolbar.add(hiliteGOBox);
		synchronizeBox = new JCheckBox("Synchronize Selection");
		toolbar.add(synchronizeBox);
		return toolbar;
	}
	
	private void search() {
		Window window = (Window) SwingUtilities.getRoot(this);
		SearchDialog searchDialog = null;
		if (window instanceof Frame) {
			searchDialog = new SearchDialog((Frame)window, "Search");
		}
		else {
			searchDialog = new SearchDialog((Dialog)window, "Search");
		}
		MySQLAdaptor dba = goPane.getMySQLAdaptor();
		java.util.List clses = new ArrayList();
		clses.add(dba.getSchema().getClassByName("Event"));
		clses.add(dba.getSchema().getClassByName("GO_BiologicalProcess"));
		searchDialog.searchPane.setSelectableClasses(clses);
		searchDialog.setSize(350, 200);
		searchDialog.setLocationRelativeTo(this);
		searchDialog.setModal(true);
		searchDialog.setVisible(true);
		if (searchDialog.isOKClicked) {
			search(searchDialog.searchPane);
		}
	}
	
	private void search(SearchPane searchPane) {
		Collection c = null;
		String operator = searchPane.getOperator();
		GKSchemaAttribute att = searchPane.getAttribute();
		String value = searchPane.getText();
		MySQLAdaptor adaptor = goPane.getMySQLAdaptor();
		GKSchemaClass cls = searchPane.getSchemaClass();
		try {
			if (!(operator.equals("IS NULL"))
				&& !(operator.equals("IS NOT NULL"))
				&& att.getTypeAsInt() == SchemaAttribute.INSTANCE_TYPE) {
				c = new HashSet();
				// Search another instance first
				Collection schemaClasses = null;
				schemaClasses = att.getAllowedClasses();
				for (Iterator i = schemaClasses.iterator(); i.hasNext();) {
					GKSchemaClass class2 = (GKSchemaClass)i.next();
					Collection attInstances =
						adaptor.fetchInstanceByAttribute(class2.getName(), "name", operator, value);
					if (attInstances != null) {
						for (Iterator it = attInstances.iterator(); it.hasNext();) {
							GKInstance attInstance = (GKInstance)it.next();
							Collection instances1 =
								adaptor.fetchInstanceByAttribute(cls.getName(), att.getName(), "=", attInstance);
							c.addAll(instances1);
						}
					}
				}
			}
			else
				c = adaptor.fetchInstanceByAttribute(cls.getName(), att.getName(), operator, value);
		}
		catch (Exception e) {
			System.err.println("ReactomeGOComparisonPane.search(): " + e);
			e.printStackTrace();
		}
		if (c == null || c.size() == 0) {
			JOptionPane.showMessageDialog(this, 
			                              "No instances found.",
			                              "Empty Search Results",
			                              JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		if (cls.getName().equals("Event")) {
			eventPane.select(c);
		}
		else if (cls.getName().equals("GO_BiologicalProcess")) {
			goPane.select(c);
		}
	}
	
	private Icon createIcon(String fileName) {
		if (AppletHelper.getHelper() == null)
			return GKApplicationUtilities.createImageIcon(getClass(), fileName);
		else
			return AppletHelper.getHelper().getIcon(fileName);
	}
	
	private void installListeners() {
		eventPane.addSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				java.util.List instances = eventPane.getSelection();
				if (instances.size() != 1) {
					singleEventView.setEvent(null);
				}
				else {
					GKInstance event = (GKInstance) instances.get(0);
					singleEventView.setEvent(event);
				}
			}
		});
		
		goPane.addSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				Collection c = goPane.getSelectedInstances();
				if (c.size() != 1) {
					singleGOView.setEvent(null);
				}
				else {
					GKInstance instance = (GKInstance) c.iterator().next();
					singleGOView.setEvent(instance);
				}
			}
		});
		// To synchronize selection action
		eventListener = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (!synchronizeBox.isSelected())
					return;
				goPane.removeSelectionListener(goListener);
				java.util.List events = eventPane.getSelection();
				if (events.size() == 1) {
					GKInstance event = (GKInstance) events.get(0);
					try {
						GKInstance goInstance = (GKInstance) event.getAttributeValue("goBiologicalProcess");
						if (goInstance != null) {
							java.util.List l = new ArrayList(1);
							l.add(goInstance);
							goPane.collapseAllNodes();
							goPane.select(l);
						}
						else {
							goPane.clearSelection();
						}
					}
					catch(Exception e1) {
					}
				}
				goPane.addSelectionListener(goListener);
			}
		};
		eventPane.addSelectionListener(eventListener);
		goListener = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				if (!synchronizeBox.isSelected())
					return;
				eventPane.removeSelectionListener(eventListener);
				Collection events = goPane.getSelectedInstances();
				if (events.size() == 1) {
					GKInstance event = (GKInstance) events.iterator().next();
					java.util.List list = event.getAttributeValuesListNoCheck("event");
					if (list != null && list.size() > 0) {
						eventPane.collapseAllNodes();
						eventPane.select(list);
					}
					else
						eventPane.clearSelection();
				}
				eventPane.addSelectionListener(eventListener);
			}
		};
		goPane.addSelectionListener(goListener);
	}
	
	public void setMySQLAdaptor(MySQLAdaptor adaptor) {
		eventPane.setMySQLAdaptor(adaptor);
		goPane.setMySQLAdaptor(adaptor);
	}
	
	public void cleanUp() {
		MySQLAdaptor adaptor = goPane.getMySQLAdaptor();
		try {
			adaptor.cleanUp();
		}
		catch(Exception e) {
			System.err.println("ReactomeGOComparisonPane.cleanUp(): " + e);
			e.printStackTrace();
		}
	}
	
	class SearchDialog extends JDialog {
		private SearchPane searchPane;
		private boolean isOKClicked = false;
		private JButton okBtn;
		private JButton cancelBtn;
		
		SearchDialog(Dialog parentDialog, String title) {
			super(parentDialog, title);
			init();
		}

		SearchDialog(Frame parentDialog, String title) {
			super(parentDialog, title);
			init();
		}
		
		private void init() {
			searchPane = new SearchPane();
			searchPane.setSearchButtonVisible(false);
			searchPane.setBorder(BorderFactory.createRaisedBevelBorder());
			getContentPane().add(searchPane, BorderLayout.CENTER);
			
			JPanel controlPane = new JPanel();
			controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
			okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			okBtn.setDefaultCapable(true);
			getRootPane().setDefaultButton(okBtn);
			cancelBtn = new JButton("Cancel");
			cancelBtn.setMnemonic('C');
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			controlPane.add(okBtn);
			controlPane.add(cancelBtn);
			getContentPane().add(controlPane, BorderLayout.SOUTH);
			// Add listeners
			ActionListener l = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
					Object src = e.getSource();
					if (src == okBtn)
						isOKClicked = true;
					else
						isOKClicked = false;
				}
			};
			okBtn.addActionListener(l);
			cancelBtn.addActionListener(l);
		}
		
		public boolean isOKClicked() {
			return isOKClicked;
		}
	}
}
