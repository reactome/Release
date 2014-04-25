/*
 * Created on Nov 10, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.gk.graphEditor.GraphEditorPane;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.ReactionNodeGraphEditor;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.render.Node;
import org.gk.render.ReactionNode;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.schema.GKSchemaClass;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.GraphLayoutEngine;
import org.gk.util.ProgressPane;

/**
 * A customized JPanel for holding a hierarchical view of events.
 * @author wugm
 */
public class EventCentricViewPane extends JPanel {
	private HierarchicalEventPane eventPane;
	private AttributePane attributePane;
	private SearchPane searchPane;
	private MultipleAttributeSearchPane multSearchPanel;
	private JLabel searchLabel;
	// Mark the change
	private boolean isDirty = true;
	// Container for graph pane
	private JPanel graphContainer;
	protected JLabel graphTitleLabel;
	private JScrollPane graphScrollPane;
	private GraphEditorController graphController;
	protected JToolBar graphToolbar;

	public EventCentricViewPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		eventPane = new HierarchicalEventPane();
		// Check a previous selection action
		String prop = GKApplicationUtilities.getApplicationProperties().getProperty("ehvHideReactionsAtTop", // Event Hiearchical View
		                                                                            Boolean.FALSE + "");
		eventPane.setHideReactionsInTopLevel(new Boolean(prop));
        eventPane.addPropertyChangeListener(new PropertyChangeListener() {
            
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("hideReactionAtTop")) {
                    GKApplicationUtilities.getApplicationProperties().setProperty("ehvHideReactionsAtTop",
                                                                                  evt.getNewValue() + "");
                }
            }
        });
		attributePane = new AttributePane();
		searchPane = new SearchPane();
		searchPane.addSearchActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search();
			}
		});
		searchPane.setBorder(BorderFactory.createEtchedBorder());
		searchPane.addSearchMoreAction(new ActionListener() {
            
            public void actionPerformed(ActionEvent e) {
                searchInstances();
            }
        });
		multSearchPanel = new MultipleAttributeSearchPane();
		multSearchPanel.addSearchActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doMultAttSearch();
            }
        });
		//searchPane.hideClassFields();
		// Create the left Pane
		JPanel leftPane = new JPanel();
		leftPane.setLayout(new BorderLayout());
		leftPane.add(eventPane, BorderLayout.CENTER);
		// For search pane
		JPanel pane1 = new JPanel();
		pane1.setBorder(BorderFactory.createLoweredBevelBorder());
		pane1.setLayout(new BorderLayout());
		searchLabel = new JLabel("Search Events");
		searchLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		pane1.add(searchLabel, BorderLayout.NORTH);
		pane1.add(searchPane, BorderLayout.CENTER);
		leftPane.add(pane1, BorderLayout.SOUTH);
		// Create a link
		eventPane.addSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath[] paths = eventPane.getTree().getSelectionPaths();
				if (paths == null || paths.length != 1) {
					attributePane.setInstance(null);
					display(null);
				}
				else {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
					GKInstance instance = (GKInstance) treeNode.getUserObject();
					if (attributePane.getInstance() != instance) {
						attributePane.setInstance(instance);
						display(instance);
					}
				}
			}
		});
		leftPane.setMinimumSize(new Dimension(20, 100));
		attributePane.setMinimumSize(new Dimension(20, 100));
		
		graphContainer = createGraphContainer();
		JSplitPane rightJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
		                                     graphContainer,
		                                     attributePane);
		rightJSP.setDividerLocation(350);
		rightJSP.setResizeWeight(0.5);
		rightJSP.setOneTouchExpandable(true);
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
		                                leftPane, 
		                                rightJSP);
		add(jsp, BorderLayout.CENTER);
		jsp.setDividerLocation(400);
		jsp.setResizeWeight(0.33);
		jsp.setOneTouchExpandable(true);
	}
	
	private JPanel createGraphContainer() {
		JPanel graphPane = new JPanel();
		graphPane.setLayout(new BorderLayout());
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		graphTitleLabel = new JLabel("Graphic Display");
        graphTitleLabel.setToolTipText("Graphic Display");
		graphTitleLabel.setBorder(GKApplicationUtilities.getTitleBorder());
		northPane.add(graphTitleLabel, BorderLayout.WEST);
		// Set up the toolbar
		graphToolbar = new JToolBar();
		graphController = new GraphEditorController(this);
		northPane.add(graphToolbar, BorderLayout.EAST);
		Dimension btnSize = GKApplicationUtilities.getToolBarBtnSize();
		JButton btn = graphToolbar.add(graphController.getInsertEntityAction());
		btn.setPreferredSize(new Dimension(26, 20));
		btn = graphToolbar.add(graphController.getInsertEventAction());
		btn.setPreferredSize(btnSize);
		btn = graphToolbar.add(graphController.getDeleteAction());
		btn.setPreferredSize(btnSize);
		btn = graphToolbar.add(graphController.getLayoutAction());
		btn.setPreferredSize(btnSize);
		btn = graphToolbar.add(graphController.getLayoutEdgeAction());
		btn.setPreferredSize(btnSize);
        btn = graphToolbar.add(graphController.getZoomInAction());
        btn.setPreferredSize(btnSize);
        btn = graphToolbar.add(graphController.getZoomOutAction());
        btn.setPreferredSize(btnSize);
		graphToolbar.setVisible(false);
		
		graphPane.add(northPane, BorderLayout.NORTH);
		graphScrollPane = new JScrollPane();
		graphPane.add(graphScrollPane, BorderLayout.CENTER); 
		// Have to update changes if any related information changed in the AttributePane.
		AttributeEditManager.getManager().addAttributeEditListener(new AttributeEditListener() {
			public void attributeEdit(AttributeEditEvent e) {
				handleAttributeEdit(e);
			}
		});
		graphPane.setMinimumSize(new Dimension(20, 20));
		return graphPane;
	}
	
	private void handleAttributeEdit(AttributeEditEvent e) {
		if (e.getEditingInstance() != attributePane.getInstance())
			return;
		String attName = e.getAttributeName();
		if (attName == null) {
			display(e.getEditingInstance()); // Re-display
			return;
		}
		GraphEditorPane graphPane = getDisplayGraphPane();
		if ((attName.equals(ReactomeJavaConstants.hasComponent) ||
             attName.equals(ReactomeJavaConstants.hasEvent)) && 
            graphPane instanceof PathwayEditor) {
			if (e.getEditingType() == AttributeEditEvent.ADDING)
				graphController.addInstances(e.getAddedInstances());
			else if (e.getEditingType() == AttributeEditEvent.REMOVING) 
				graphController.removeInstances(e.getRemovedInstances());
		}
		else if ((attName.equals("input") || attName.equals("output")) &&
				 (graphPane instanceof ReactionNodeGraphEditor)) {
			if (e.getEditingType() == AttributeEditEvent.ADDING) {
				graphController.addInstances(e.getAddedInstances(), attName);
			}
			else if (e.getEditingType() == AttributeEditEvent.REMOVING) {
				graphController.removeInstances(e.getRemovedInstances());
			}
		}
		else if (attName.equals("catalystActivity") && graphPane instanceof ReactionNodeGraphEditor) {
			if (e.getEditingType() == AttributeEditEvent.ADDING) {
				graphController.addInstances(e.getAddedInstances(), attName);
			}
			else if (e.getEditingType() == AttributeEditEvent.REMOVING) {
				// Convert to PhysicalEntity
				java.util.List entities = new ArrayList(e.getRemovedInstances().size());
				GKInstance ca = null;
				try {
					for (Iterator it = e.getRemovedInstances().iterator(); it.hasNext();) {
						ca = (GKInstance)it.next();
						GKInstance entity = (GKInstance)ca.getAttributeValue("physicalEntity");
						if (entity != null)
							entities.add(entity);
					}
					graphController.removeInstances(entities);
				}
				catch (Exception e1) {
					System.err.println("EventCentricViewPane.handleAttributeEdit(): " + e1);
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void display(final GKInstance instance) {
	    GraphEditorPane graphPane = null;
	    if (instance != null && !instance.isShell())
	        graphPane = graphController.getGraphEditor(instance);
	    if (graphPane == null) {
	    		graphScrollPane.getViewport().setView(null);
			//graphTitleLabel.setText("Graphic Display");
             graphTitleLabel.setToolTipText("Graphic Display");
			graphToolbar.setVisible(false);
		}
		else {
			if (graphScrollPane.getViewport().getView() != graphPane) {
				graphScrollPane.getViewport().setView(graphPane);
				graphController.validateToolBar(graphPane);
			}
			layoutRenderable(graphPane);  
             // Auto zoom to fit
            final GraphEditorPane tmpPane = graphPane;
            // Use a new thread so that graphic context can be figured out correctly.
            // Only do an auto zoom for pathway
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (instance.getSchemClass().isa(ReactomeJavaConstants.Pathway)) {
                        Dimension prefSize = tmpPane.getPreferredSize();
                        Dimension visSize = tmpPane.getVisibleRect().getSize();
                        if ((prefSize.getWidth() > visSize.getWidth()) ||
                            (prefSize.getHeight() > visSize.getHeight()))
                            tmpPane.zoomToFit();
                        else // No need to zoom if prefSize is smaller than visSize
                            tmpPane.zoom(1.0d, 1.0d); 
                    }
                    else { // Only for reactions
                        tmpPane.zoom(1.0d, 1.0d);
                    }
                }
            });
            // Use tooltip text instead of text to avoid clutter in the title
			graphTitleLabel.setToolTipText("Graphic Display for " + instance.getExtendedDisplayName());
        }
	}
	
	private void layoutRenderable(GraphEditorPane graphPane) {
		if (graphPane.getGraphics() == null)
			return;
		java.util.List components = graphPane.getDisplayedObjects();
		if (components == null || components.size() == 0)
			return;
		Graphics2D g2 = (Graphics2D) graphPane.getGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						   RenderingHints.VALUE_ANTIALIAS_ON);
		org.gk.render.Renderer renderer = null;
		Renderable r = graphPane.getRenderable();
		for (Iterator it = components.iterator(); it.hasNext();) {
			Renderable tmp = (Renderable) it.next();
			if (tmp instanceof Node) {
			    Node node = (Node) tmp;
                node.validateBounds(g2);
            }
		}
		if (r instanceof RenderablePathway) {
			RenderablePathway pathway = (RenderablePathway) r;
			// Using hierarchical layout as default.
			pathway.layout(GraphLayoutEngine.HIERARCHICAL_LAYOUT);
			RenderUtility.center(pathway, graphScrollPane.getSize());
		}
		else if (r instanceof ReactionNode) {
			RenderableReaction reaction = ((ReactionNode)r).getReaction();
			Dimension size = graphScrollPane.getSize();
			int x = size.width / 2;
			int y = size.height / 2;
			reaction.layout(new Point(x, y));
			graphPane.repaint();
		}
	}
	
	public GraphEditorPane getDisplayGraphPane() {
		if (graphScrollPane.getViewport().getView() == null)
			return null;
		return (GraphEditorPane) graphScrollPane.getViewport().getView();
	}
	
	public void setIsForDB(boolean isForDB) {
		eventPane.setIsForDB(isForDB);
		if (isForDB) {
			setEditable(false);
		}
	}
	
	public void setEditable(boolean editable) {
		attributePane.setEditable(editable);
		eventPane.setEditable(editable);
	}
	
	public boolean isEditable() {
		return attributePane.isEditable();
	}
	
	public HierarchicalEventPane getEventPane() {
		return this.eventPane;
	}
	
	public AttributePane getAttributePane() {
		return attributePane;
	}
	
	public SearchPane getSearchPane() {
		return searchPane;
	}
		
	public void setTopLevelEvents(java.util.List events) {
		eventPane.setTopLevelEvents(events);
	}
	
	public void addTopLevelEvent(GKInstance event) {
		eventPane.addTopLevelEvent(event);
	}
	
	public java.util.List getTopLevelEvents() {
		return eventPane.getTopLevelEvents();
	}
	
	/**
	 * Delete an event.
	 * @param event
	 */
	public void deleteInstance(Instance event) {
		if (event.getSchemClass().isa("Event"))
			eventPane.deleteInstance(event);
		attributePane.refresh();
		GraphEditorPane graphPane = getDisplayGraphPane();
		if (graphPane != null) {
		    java.util.List renderables = graphPane.getDisplayedObjects();
		    for (Iterator it = renderables.iterator(); it.hasNext();) {
		        Renderable r = (Renderable) it.next();
		        Long dbId = r.getReactomeId();
		        if (dbId == null)
		            continue; // For flowline
                if (dbId.equals(event.getDBID())) {
		            graphPane.delete(r);
		            graphPane.repaint(graphPane.getVisibleRect());
		            break;
		        }
		    }
		}
	}
	
	public void addInstance(Instance event) {
		if (event.getSchemClass().isa("Event"))
			eventPane.addInstance(event);
	}
	
	/**
	 * Update the tree view on the account of the changes in a specified
	 * event Instance.
	 * @param event
	 */
	public void updateInstance(AttributeEditEvent editingEvent) {
		GKInstance instance = editingEvent.getEditingInstance();
		if(instance.getSchemClass().isa(ReactomeJavaConstants.Event))
			eventPane.updateInstance(editingEvent);
	}
    
    public void markAsDirty(GKInstance instance) {
        if (instance.getSchemClass().isa(ReactomeJavaConstants.Event))
            eventPane.markAsDirty(instance);
    }
	
	public void setMySQLAdaptor(final MySQLAdaptor dba) {
		if (dba == null)
			return ;
		Thread t = new Thread() {
			public void run() {
				// Add a progress pane
			    JFrame frame = (JFrame) SwingUtilities.getRoot(EventCentricViewPane.this);
				final ProgressPane progressPane = new ProgressPane();
				frame.setGlassPane(progressPane);
				progressPane.setIndeterminate(true);
				progressPane.setText("Fetching events...");
				PropertyChangeListener l = new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent e) {
						String name = e.getPropertyName();
						if (name.equals("buildTree")) {
							int newValue = ((Integer)e.getNewValue()).intValue();
							progressPane.setValue(newValue);
						}
					}
				};
				eventPane.addPropertyChangeListener(l);
				frame.getGlassPane().setVisible(true);
				try {
					//dba.debug = true;
					// Refresh dba in case the tree is loaded before
					dba.refresh();
					EventTreeBuildHelper treeHelper = new EventTreeBuildHelper(dba);
					progressPane.setText("Fetching top level events...");
					Collection topLevelPathways = treeHelper.getTopLevelEvents();
					progressPane.setText("Fetching all events...");
					Collection c = treeHelper.getAllEvents();
					progressPane.setText("Fetching attribute values...");
					treeHelper.loadAttribtues(c);
					// Need to fill "hasInstance" for events to create the correct tree
					//setHasInstanceForEvents(c);
					progressPane.setText("Building tree...");
					ArrayList list = new ArrayList(topLevelPathways);
					progressPane.setMinimum(0);
					progressPane.setMaximum(list.size());
					progressPane.setIndeterminate(false);
					setTopLevelEvents(list);
					// Have to set event cls as selected schema class manually
					GKSchemaClass eventCls = (GKSchemaClass)dba.getSchema().getClassByName("Event");
					java.util.List topCls = new ArrayList(1);
					topCls.add(eventCls);
					searchPane.setTopLevelSchemaClasses(topCls);
					searchPane.setSelectedClass(eventCls);
					multSearchPanel.setTopLevelSchemaClasses(topCls);
					multSearchPanel.setSelectedClass(eventCls);
				}
				catch (Exception e) {
					System.err.println("EventCentricViewFrame.setMySQLAdaptor(): " + e);
					e.printStackTrace();
				}
				eventPane.removePropertyChangeListener(l);
				frame.getGlassPane().setVisible(false);
			}
		};
		t.start();
	}

	public String getSelectedSpecies() {
		return eventPane.getSelectedSpecies();
	}
	
	public void setSelectedSpecies(String species) {
		eventPane.setSelectedSpecies(species);
	}
	
	public java.util.List getSelection() {
		return eventPane.getSelection();
	}
	
	public void setSelectedEvent(GKInstance instance) {
		eventPane.setSelectedEvent(instance);
	}
	
	public void rebuildTree() {
		try {
			// Rebuild the whole tree
			java.util.List events = PersistenceManager.getManager().getActiveFileAdaptor().fetchAllEvents();
			java.util.List selections = getSelection();
			// Disable all selection events
			TreeSelectionListener[] listeners = eventPane
					.getSelectionListeners();
			if (listeners != null) {
				for (int i = 0; i < listeners.length; i++)
					eventPane.removeSelectionListener(listeners[i]);
			}
			eventPane.setAllEvents(events);
			if (selections.size() > 0)
				setSelectedEvent((GKInstance) selections.get(0));
			if (listeners != null) {
				for (int i = 0; i < listeners.length; i++)
					eventPane.addSelectionListener(listeners[i]);
			}
		} catch (Exception e) {
			System.err.println("EventCentricViewPane.rebuildTree(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Call this method if an Instance's type (i.e. class) is changed.
	 * @param oldCls the old SchemaClass.
	 * @param instance the instance whose SchemaClass has been changed.
	 */
	public void switchedType(GKSchemaClass oldCls, GKInstance instance) {
	    // Check if the graph display is needed to updated.
		GraphEditorPane graphPane = getDisplayGraphPane();
		if (graphPane != null) {
		    java.util.List renderables = graphPane.getDisplayedObjects();
		    for (Iterator it = renderables.iterator(); it.hasNext();) {
		        Renderable r = (Renderable) it.next();
                Long dbId = r.getReactomeId();
                if (instance.getDBID().equals(dbId)) {
		            display((GKInstance)attributePane.getInstance());
		            break;
		        }
		    }
		}
		markAsDirty(instance);
	}
	
	public boolean isDirty() {
		return this.isDirty;
	}
	
	public void searchInstances() {
	    multSearchPanel.setSelectedClass(searchPane.getSchemaClass());
	    JFrame parent = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
	    multSearchPanel.showSearch(parent);
	}
	
	private void doMultAttSearch() {
        searchLabel.setText("Search...");
        searchPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        PersistenceAdaptor persistenceAdaptor = eventPane.getPersistenceAdaptor();
        try {           
            List<GKInstance> found = multSearchPanel.search(persistenceAdaptor);
            if (found == null || found.size() == 0) {
                JOptionPane.showMessageDialog(EventCentricViewPane.this,
                                              "No " + multSearchPanel.getSchemaClass().getName() + " found.",
                                              "Search Result",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
            else
                eventPane.select(found);
        }
        catch (Exception e) {
            System.err.println("EventCentricViewPane.search(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Error in search: " + e,
                                          "Error in Search",
                                          JOptionPane.ERROR_MESSAGE);
        }
        searchPane.setCursor(Cursor.getDefaultCursor());
        searchLabel.setText("Search Events");
	}
	
	private void search() {
		// Put into another thread
		//Thread t = new Thread() {
		//	public void run() {
		// Don't use a thread. A thread will make the display of Pathways or Reactions not correct
		// after the event centric view is just shown. Try to search pathway 110526 after the event
		// centric view is launched.
				searchLabel.setText("Search...");
				searchPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				Collection c = null;
				PersistenceAdaptor persistenceAdaptor = eventPane.getPersistenceAdaptor();
				try {		    
				    c = searchPane.search(persistenceAdaptor);
				}
				catch (Exception e) {
				    System.err.println("EventCentricViewPane.search(): " + e);
				    e.printStackTrace();
				}
				if (c == null || c.size() == 0) {
				    JOptionPane.showMessageDialog(EventCentricViewPane.this,
				                                  "No " + searchPane.getSchemaClass().getName() + " found.",
				                                  "Search Result",
				                                  JOptionPane.INFORMATION_MESSAGE);
				}
				else {
					// Check if there are too many instances to be displayed
				    if (c.size() > 500) { // 500 is arbitrary
				        JOptionPane.showMessageDialog(EventCentricViewPane.this, 
				                                      c.size() + " instances have been found for this search. Only 500 instances will be selected\n" + 
				                                      "in the tree view. You can use \"Search More\" feature to limit your search criteria.",
				                                      "Search Result",
				                                      JOptionPane.WARNING_MESSAGE);
				        List<GKInstance> list = new ArrayList<GKInstance>(c);
				        InstanceUtilities.sortInstances(list);
				        eventPane.select(list.subList(0, 500));
				    }
				    else
				        eventPane.select(c);
				}
				searchPane.setCursor(Cursor.getDefaultCursor());
				searchLabel.setText("Search Events");
		//	}
		//};
		//t.start();
	}
	
	public void setUpLocalView() {
		XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		if (fileAdaptor == null)
			return;
		try {
			java.util.List events = fileAdaptor.fetchAllEvents();
			eventPane.setAllEvents(events);
		} 
		catch (Exception e) {
			System.err.println("EventCentricViewPane.setUpLocalView(): " + e);
			e.printStackTrace();
		}
		GKSchemaClass eventCls = (GKSchemaClass) fileAdaptor.getSchema().getClassByName("Event");
		java.util.List list = new ArrayList(1);
		list.add(eventCls);
		searchPane.setTopLevelSchemaClasses(list);
		searchPane.setSelectedClass(eventCls);
		multSearchPanel.setTopLevelSchemaClasses(list);
		multSearchPanel.setSelectedClass(eventCls);
	}
	
}
