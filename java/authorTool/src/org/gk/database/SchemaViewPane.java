/*
 * Created on Nov 12, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.gk.model.Bookmark;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaClass;
import org.gk.util.ProgressPane;

/**
 * A customized JPanel for schema view of instances.
 * @author wugm
 */
public class SchemaViewPane extends JPanel {
	// GUIs
	private SchemaDisplayPane schemaPane;
	private InstanceListPane instancePane;
	private AttributePane attributePane;
	private BookmarkView bookmarkView;
	private JSplitPane bookmarkJSP;
	private JSplitPane listAttributeJSP;
	private JSplitPane schemaJSP;
	private int bookmarkJSPDividerPos;
	// For the db connection
	private boolean isForDB = false;
	// Cache attribute search pane for recording user's choice
	private MultipleAttributeSearchPane searchPane;
	
	public SchemaViewPane() {
		init();
	}
	
	public void setForDB(boolean isForDB) {
		this.isForDB = isForDB;
	}
	
	public void setPersistenceAdaptor(PersistenceAdaptor adaptor) {
		try {
			GKSchema schema = (GKSchema)adaptor.getSchema();
			// Get the counters for classes
			Map countMap = new HashMap();
            if (adaptor instanceof MySQLAdaptor) {
                Map clsNameToCount = ((MySQLAdaptor)adaptor).getAllInstanceCounts();
                changeClsNameToClsInCountMap(clsNameToCount,
                                             countMap,
                                             schema);
            }
            else
                getClassCounters((GKSchemaClass)schema.getRootClass(), countMap, adaptor);
			schemaPane.setClassCounts(countMap); // Call this before setSchema to 
			// Only need for the local repository
			Map isDirtyMap = getClassDirtyMap(schema, adaptor);
			schemaPane.setIsDirtyMap(isDirtyMap);
			// make the tree display correctly
			schemaPane.setSchema(schema);
			instancePane.setPersistenceAdaptor(adaptor);
		}
		catch (Exception e) {
			System.err.println("SchemaViewPane.setPersistenceAdaptor(): " + e);
			e.printStackTrace();
		}
	}
    
    private void changeClsNameToClsInCountMap(Map clsNameToCount,
                                              Map clsToCount,
                                              GKSchema schema) {
        GKSchemaClass root = (GKSchemaClass) schema.getRootClass();
        changeClsNameToClsInCountMap(clsNameToCount, 
                                     clsToCount, 
                                     root);
    }
    
    private void changeClsNameToClsInCountMap(Map clsNameToCount,
                                              Map clsToCount,
                                              GKSchemaClass cls) {
        Collection children = cls.getSubClasses();
        // Leaf class
        if (children == null | children.size() == 0) {
            Long count = (Long) clsNameToCount.get(cls.getName());
            if (count == null)
                clsToCount.put(cls, new Long(0));
            else
                clsToCount.put(cls, count);    
            return;
        }
        for (Iterator it = children.iterator(); it.hasNext();) {
            GKSchemaClass child = (GKSchemaClass) it.next();
            changeClsNameToClsInCountMap(clsNameToCount, 
                                         clsToCount, 
                                         child);
        }
        // All count in children should be done
        long total = 0;
        for (Iterator it = children.iterator(); it.hasNext();) {
            GKSchemaClass child = (GKSchemaClass) it.next();
            Long c = (Long) clsToCount.get(child);
            total += c.longValue();
        }
        // Should get its own count
        Long c = (Long) clsNameToCount.get(cls.getName());
        if (c != null)
            total += c.longValue();
        clsToCount.put(cls, new Long(total));
    }
	
	private Map getClassDirtyMap(GKSchema schema, PersistenceAdaptor adaptor) {
		if (!(adaptor instanceof XMLFileAdaptor))
			return new HashMap();
		XMLFileAdaptor fileAdaptor = (XMLFileAdaptor) adaptor;
		Map map = new HashMap();
		GKSchemaClass cls = null;
		try {
			for (Iterator it = schema.getClasses().iterator(); it.hasNext();) {
				cls = (GKSchemaClass) it.next();
				Collection c = fileAdaptor.fetchInstancesByClass(cls, false);
				if (c == null || c.size() == 0)
					continue;
				for (Iterator it1 = c.iterator(); it1.hasNext();) {
					GKInstance instance = (GKInstance) it1.next();
					if (instance.isDirty()) {
						map.put(cls, Boolean.TRUE);
						break;
					}
				}
			}
			// Need to check deletion map
			Map deletion = ((XMLFileAdaptor)adaptor).getDeleteMap();
			Set clsNameSet = new HashSet(deletion.values());
			for (Iterator it = clsNameSet.iterator(); it.hasNext();) {
				String clsName = (String) it.next();
				cls = (GKSchemaClass) adaptor.getSchema().getClassByName(clsName);
				map.put(cls, Boolean.TRUE);
			}
			// Need to set the superclasses info
			Set dirtyCls = new HashSet();
			for (Iterator it = map.keySet().iterator(); it.hasNext();) {
			    cls = (GKSchemaClass) it.next();
			    Boolean value = (Boolean) map.get(cls);
			    if (!value.booleanValue())
			        continue;
			    for (Iterator it1 = cls.getOrderedAncestors().iterator(); it1.hasNext();)
			        dirtyCls.add(it1.next());
			}
			for (Iterator it = dirtyCls.iterator(); it.hasNext();)
			    map.put(it.next(), Boolean.TRUE);
		} 
		catch (Exception e) {
			System.err.println("SchemaViewPane.getClassDirtyMap(): " + e);
			e.printStackTrace();
		}
		return map;
	}
	
	public void setTopLevelSchemaClasses(Collection schemaClasses, PersistenceAdaptor adaptor) {
		if (adaptor != null) {
			Map counterMap = new HashMap();
			try {
				for (Iterator it = schemaClasses.iterator(); it.hasNext();) {
					GKSchemaClass schemaClass = (GKSchemaClass)it.next();
					getClassCounters(schemaClass, counterMap, adaptor);
				}
			}
			catch (Exception e) {
				System.err.println("SchemaViewPane.setTopLevelSchemaClasses(): " + e);
				e.printStackTrace();
			}
			// Have to call this method first to render the number correctly.
			schemaPane.setClassCounts(counterMap);
			instancePane.setPersistenceAdaptor(adaptor);
		}
		schemaPane.setTopLevelSchemaClasses(schemaClasses);
	}
	
	private void getClassCounters(GKSchemaClass schemaClass, Map counterMap, PersistenceAdaptor adaptor)
				 throws Exception {
		long counter = adaptor.getClassInstanceCount(schemaClass);
		counterMap.put(schemaClass, new Long(counter));
		if (schemaClass.getSubClasses() != null && schemaClass.getSubClasses().size() > 0) {
			for (Iterator it = schemaClass.getSubClasses().iterator(); it.hasNext();) {
				GKSchemaClass subClass = (GKSchemaClass) it.next();
				getClassCounters(subClass, counterMap, adaptor);
			}
		}
	}
	
	public void searchInstances() {
	    if (searchPane == null) {
	        searchPane = new MultipleAttributeSearchPane();
	        // There are two cases
	        if (schemaPane.getSchema() == null) {
	            List<SchemaClass> displayClses = schemaPane.getDisplayedClasses();
	            searchPane.setSelectableClasses(displayClses);
	        }
	        else
	            searchPane.setSchema((GKSchema)schemaPane.getSchema());
	        searchPane.addSearchActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doSearchInstances();
                }
            });
	    }
	    // Set the selected class
	    searchPane.setSelectedClass(schemaPane.getSearchPane().getSchemaClass());
	    JFrame parent = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this);
	    searchPane.showSearch(parent);
	}
	
	/**
	 * Do an actual search in another thread.
	 */
	private void doSearchInstances() {
	    if (!searchPane.isSearchable())
	        return;
	    Thread t = new Thread() {
	        public void run() {
	            // Use ProgressPane
	            final ProgressPane progressPane = new ProgressPane();
	            progressPane.setText("Searching in progress. Please wait...");
	            JFrame parentFrame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, 
	                                                                            SchemaViewPane.this);
	            progressPane.setIndeterminate(true);
	            parentFrame.setGlassPane(progressPane);
	            parentFrame.getGlassPane().setVisible(true);
	            try {
	                //TODO: A little weid here to get PersistenceAdaptor from InstancePane. This should be changed!
	                List<GKInstance> foundInstances = searchPane.search(instancePane.getPersistenceAdaptor());
	                InstanceUtilities.sortInstances(foundInstances);
	                instancePane.setTitle("Found instances of " + searchPane.getSchemaClass().getName() + ": " + foundInstances.size());
	                instancePane.setDisplayedInstances(foundInstances);
	            }
	            catch(Exception e) {
                    System.err.println("SchemaViewPane.doSearchInstnaces(): " + e);
                    e.printStackTrace();
	                JOptionPane.showMessageDialog(parentFrame, 
	                                              "Error in search: " + e,
	                                              "Error in Search",
	                                              JOptionPane.ERROR_MESSAGE);
	            }
	            parentFrame.getGlassPane().setVisible(false);
	        }
	    };
	    t.start();
	}
	
	public InstanceListPane getInstancePane() {
		return this.instancePane;
	}
	
	public SchemaDisplayPane getSchemaPane() {
		return this.schemaPane;
	}
	
	public AttributePane getAttributePane() {
		return this.attributePane;
	}
	
	public void setEditable(boolean editable) {
		instancePane.setEditable(editable);
		attributePane.setEditable(editable);
	}
	
	public void setIsViewable(boolean isViewable) {
		instancePane.setIsViewable(isViewable);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		Dimension miniSize = new Dimension(10, 100);
		schemaPane = new SchemaDisplayPane();
		schemaPane.setMinimumSize(miniSize);
		instancePane = new InstanceListPane();
		instancePane.setMinimumSize(miniSize);
		bookmarkView = new BookmarkView();
		bookmarkView.addCloseAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hideBookmarkView();
			}
		});
		attributePane = new AttributePane();
		attributePane.setMinimumSize(miniSize);
		listAttributeJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, instancePane, attributePane);
		bookmarkJSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listAttributeJSP, bookmarkView);
		schemaJSP = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, schemaPane, bookmarkJSP);
		add(schemaJSP, BorderLayout.CENTER);
		// Add listeners
		schemaPane.addSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent event) {
				GKSchemaClass schemaClass = schemaPane.getSelectedClass();
				instancePane.setSchemaClass(schemaClass);
			}
		});
		instancePane.addSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				java.util.List selected = instancePane.getSelection();
				if (selected.size() == 1) 
					attributePane.setInstance((Instance)selected.get(0));
				else
					attributePane.setInstance(null);
			}
		});
		schemaPane.getSearchPane().addSearchActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SearchPane searchPane = schemaPane.getSearchPane();
				instancePane.searchInstance(searchPane);
			}
		});
		schemaPane.getSearchPane().addSearchMoreAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchInstances();
            }
        });
		// For bookmark
		bookmarkView.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String propName = e.getPropertyName();
				if (propName.equals("bookmark")) {
					Bookmark bookmark = (Bookmark) e.getNewValue();
					XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
					try {
						GKInstance instance = adaptor.fetchInstance(bookmark.getType(),
						                                            bookmark.getDbID());
						if (instance != null) {
							setSelection(instance);
						}                                           
					}
					catch(Exception e1) {
						System.err.println("SchemaViewPane.init(): " + e1);
						e1.printStackTrace();
					}
				}
			}
		});
		listAttributeJSP.setDividerLocation(350);
		listAttributeJSP.setResizeWeight(0.5);
		bookmarkJSP.setDividerLocation(550);
		bookmarkJSP.setResizeWeight(0.7);
		schemaJSP.setDividerLocation(350);
		schemaJSP.setResizeWeight(0.3);
	}
	
	/**
	 * Call this method so that three sections of this view can share the same width.
	 * @param width
	 */
	public void distributeWidthEvenly(int width) {
	    int evenWidth = width / 3;
	    schemaJSP.setDividerLocation(evenWidth);
	    listAttributeJSP.setDividerLocation(evenWidth);
	}
	
	/**
	 * Set the JSplitPanes' locations.
	 * @param location1 the location for the JSplitPane for InstanceListPane and AttributePane.
	 * @param location2 the location for the JSplitPane for BookmarkView and the above JSplitPane.
	 * @param location3 the location for the JSplitPane for the SchemaDisplayPane and the above JSplitPane.
	 */
	public void setJSPDividerLocations(int location1, int location2, int location3) {
		listAttributeJSP.setDividerLocation(location1);
		bookmarkJSP.setDividerLocation(location2);
		schemaJSP.setDividerLocation(location3);
	}
	
	/**
	 * Return the locations for the used JSplitPanes. 
	 * @return
	 * @see setJSPDividerLocations(int, int, int)
	 */
	public int[] getJSPDividerLocations() {
		int[] locations = new int[]{listAttributeJSP.getDividerLocation(),
			                        bookmarkJSP.getBottomComponent() == null ? bookmarkJSPDividerPos : bookmarkJSP.getDividerLocation(),
			                        schemaJSP.getDividerLocation()};
		return locations;
	}
	
	public void refresh(PersistenceAdaptor adaptor) {
		SchemaClass selectedSchema = schemaPane.getSelectedClass();
		java.util.List selectedInstances = instancePane.getSelection();
		setPersistenceAdaptor(adaptor);
		if (selectedSchema != null) {
			Schema schema = adaptor.getSchema();
			// SchemaClass has been changed since a new Schema is loaded.
			SchemaClass newCls = schema.getClassByName(selectedSchema.getName());
			Map map = new HashMap();
			map.put(newCls, selectedInstances);
			instancePane.setSelectedInstances(map);
			schemaPane.setSelectedClass(newCls);
		}
	}
	
	public void addInstance(Instance instance) {
		schemaPane.addInstance(instance);
		instancePane.addInstance(instance);
	}
	
	public void deleteInstance(Instance instance) {
		schemaPane.deleteInstance(instance);
		instancePane.deleteInstance(instance);
		if (attributePane.getInstance() == instance) // null it
			attributePane.setInstance(null);
	}
	
	/**
	 * Call this method if the insance's type (i.e. SchemaClass) is changed.
	 * @param oldCls the original GKSchemaClass.
	 * @param instance the instance whose SchemaClass has been changed.
	 */
	public void switchedType(GKSchemaClass oldCls, GKInstance instance) {
	    schemaPane.deleteInstance(oldCls);
	    java.util.List list = new ArrayList(1);
	    list.add(instance);
	    instancePane.deleteInstances(list);
	    addInstance(instance);
	}
	
	public void markAsDirty(GKInstance instance) {
		schemaPane.markAsDirty(instance);
		instancePane.markAsDirty(instance);
	}
	
	public void removeDirtyFlag(GKInstance instance) {
	    schemaPane.removeDirtyFlag(instance);
	    instancePane.removeDirtyFlag(instance);
	}
	
	public void clearDeleteRecord(Collection cls) {
	    schemaPane.clearDeleteRecord(cls);
	}
	
	/**
	 * Add an Instance to the selection list.
	 * @param instance
	 */
	public void addSelection(Instance instance) {
		GKSchemaClass schemaClass = (GKSchemaClass) instance.getSchemClass();
		if (instancePane.getSchemaClass() != schemaClass)
			instancePane.loadInstancesFor(schemaClass);
		instancePane.addSelection(instance);
		schemaPane.setSelectedClass(schemaClass);
	}
	
	/**
	 * Set the selection to the specified Instance.
	 * @param instance
	 */
	public void setSelection(Instance instance) {
		GKSchemaClass schemaClass = (GKSchemaClass) instance.getSchemClass();
		// Set up the InstanceListPane first so that no loading needs to be done.
		if (instancePane.getSchemaClass() != schemaClass)
			instancePane.loadInstancesFor(schemaClass);
		instancePane.setSelection(instance);
		// Call this method after the above two methods.
		schemaPane.setSelectedClass(schemaClass);
	}
	
	/**
	 * Get a list of selected Instance objects from the InstanceListPane.
	 * @return
	 */
	public java.util.List getSelection() {
		return instancePane.getSelection();
	}
	
	public GKSchemaClass getSelectedClass() {
	    return schemaPane.getSelectedClass();
	}
	
	public void hideBookmarkView() {
		bookmarkJSP.remove(bookmarkView);
		bookmarkJSP.setDividerSize(0);
		bookmarkJSPDividerPos = bookmarkJSP.getDividerLocation();
		firePropertyChange("hideBookmarkView", false, true);
	}
	
	public void showBookmarkView() {
		for (int i = 0; i < bookmarkJSP.getComponentCount(); i++) {
			if (bookmarkJSP.getComponent(i) == bookmarkJSP)
				return;
		}
		bookmarkJSP.setBottomComponent(bookmarkView);
		int size = UIManager.getInt("SplitPane.dividerSize");
		if (size == 0)
			size = 5;
		bookmarkJSP.setDividerSize(size);
		bookmarkJSP.setDividerLocation(bookmarkJSPDividerPos);
        firePropertyChange("hideBookmarkView", true, false);
	}
	
	public void addBookmark(GKInstance instance) {
		bookmarkView.addBookmark(instance);
	}
	
	public BookmarkView getBookmarkView() {
		return bookmarkView;
	}
}
