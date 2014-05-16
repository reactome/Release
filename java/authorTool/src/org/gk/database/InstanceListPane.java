/*
 * Created on Sep 22, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionListener;

import org.gk.graphEditor.ArrayListTransferable;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaClass;

/**
 * This customized JPanel is used to display instances of a specified class.
 * @author wgm
 */
public class InstanceListPane extends JPanel {
	// All instances belong to this class
	private GKSchemaClass schemaClass = null;
	// To access the database
	private PersistenceAdaptor dba = null;
	// GUIs
	private JLabel titleLabel;
	private JList list;
    // to control is a setting box should be displayed
    private JLabel viewSettingLabel;
    private ViewSettingPanel viewSettingPanel;
	// To control double click action
	private boolean isViewable = true;
	private boolean isEditable = false;
	private Map selectedInstances;
	// For double click to display the contents of the instance
	private MouseListener showInstanceAction;
    // Some view setting
    private boolean isInstancesShouldBeGrouped;

	public InstanceListPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		titleLabel = new JLabel("Instances");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		titleLabel.setHorizontalAlignment(JLabel.LEFT);
		//add(titleLabel, BorderLayout.NORTH);
        JPanel titlePane = new JPanel();
        titlePane.setLayout(new BorderLayout());
        titlePane.add(titleLabel, BorderLayout.WEST);
//        groupBox = new JCheckBox("Aggregate");
//        groupBox.setToolTipText("Group changed instances together");
//        groupBox.setHorizontalTextPosition(JCheckBox.LEFT);
//        titlePane.add(groupBox, BorderLayout.EAST);
        viewSettingLabel = new JLabel("Show Settings");
        viewSettingLabel.setToolTipText("Click to show the setting panel");
        viewSettingLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
        viewSettingLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (viewSettingPanel == null)
                    viewSettingPanel = new ViewSettingPanel();
                if (viewSettingPanel.isVisible()) {
                    viewSettingPanel.hide(InstanceListPane.this);
                    viewSettingLabel.setText("Show Settings");
                    viewSettingLabel.setToolTipText("Click to show the setting panel");
                }
                else {
                    viewSettingPanel.show(InstanceListPane.this);
                    viewSettingLabel.setText("Hide Settings");
                    viewSettingLabel.setToolTipText("Click to hide the setting panel");
                }
            }
        });
        titlePane.add(viewSettingLabel, BorderLayout.EAST);
        add(titlePane, BorderLayout.NORTH);
        // Add a JList
		list = new JList();
		DefaultListModel listModel = new DefaultListModel();
		list.setModel(listModel);
		list.setCellRenderer(new InstanceCellRenderer());
		add(new JScrollPane(list), BorderLayout.CENTER);
		showInstanceAction = new MouseAdapter() {
			// Have to use mouseClicked method otherwise the frame for the instance
			// cannot be brought to the front after set list dragEnabled.
			public void mouseClicked(MouseEvent e) {
				if (isViewable && e.getClickCount() == 2 && list.getSelectedValues().length == 1 &&
                     e.getButton() == MouseEvent.BUTTON1) { // Only work for the first button to avoid 
                                                            // competing with the popup menu.
                     // Check the root window so that the attribute pane can be embedded in a JDialog if it is invoked from
                    // a JDialog or in a JFrame if it is invovled in a JFrame. This sorting seems more intuitive.
                      Window window = SwingUtilities.getWindowAncestor(InstanceListPane.this);
					GKInstance instance = (GKInstance)list.getSelectedValue();
					if (instance.isShell()) {
                         if (window instanceof JDialog)
                             FrameManager.getManager().showShellInstance(instance, InstanceListPane.this, (JDialog)window);
                         else
                             FrameManager.getManager().showShellInstance(instance, InstanceListPane.this);
                     }
					else {
                        if (window instanceof JDialog)
                            FrameManager.getManager().showInstance(instance, (JDialog)window, isEditable);
                        else
                            FrameManager.getManager().showInstance(instance, isEditable);
                    }
				}
			}
		};
		// Double click to open the selected instance
		list.addMouseListener(showInstanceAction);
		list.setDragEnabled(true);
		list.setTransferHandler(new InstanceTransferHandler());
        // Default hide viewsetting label
        viewSettingLabel.setVisible(false);
	}
    
    public void showViewSettings() {
        viewSettingLabel.setVisible(true);
    }
    
    public void setIsInstancesShouldBeGrouped(boolean isGrouped) {
        this.isInstancesShouldBeGrouped = isGrouped;
    }
    
    public boolean isInstancesShouldBeGrouped() {
        return this.isInstancesShouldBeGrouped;
    }
    
	public boolean isSpeciesAfterName() {
        InstanceCellRenderer renderer = (InstanceCellRenderer) list.getCellRenderer();
        return renderer.isSpeciesAfterName();
    }

    public void setSpeciesAfterName(boolean isSpeciesAfterName) {
        InstanceCellRenderer renderer = (InstanceCellRenderer) list.getCellRenderer();
        renderer.setIsSpeciesAfterName(isSpeciesAfterName);
    }

    public boolean isSpeciesDisplayed() {
        InstanceCellRenderer renderer = (InstanceCellRenderer) list.getCellRenderer();
        return renderer.isSpeciesDisplayed();
    }

    public void setSpeciesDisplayed(boolean isSpeciesDisplayed) {
        InstanceCellRenderer renderer = (InstanceCellRenderer) list.getCellRenderer();
        renderer.setIsSpeciesDisplayed(isSpeciesDisplayed);
    }

    public void setListCellRenderer(ListCellRenderer renderer) {
	    list.setCellRenderer(renderer);
	}
	
	/**
	 * Customize the action for mouse double-click action.
	 * @param action A MouseListener should implement mouseClicked method for double clicking.
	 */
	public void setMouseDoubleClickAction(MouseListener action) {
		list.removeMouseListener(showInstanceAction);
		showInstanceAction = null; // Mark for gc
		list.addMouseListener(action);
	}
	
	@SuppressWarnings("unchecked")
	public void searchInstance(final SearchPane searchPane) {
		if (!(searchPane.getSchemaClass() instanceof GKSchemaClass))
			return;
		this.schemaClass = (GKSchemaClass) searchPane.getSchemaClass();
		if (dba != null) {
			Thread t = new Thread() {
				public void run() {
					try {
						titleLabel.setText("Search instances of " + schemaClass.getName() + "...");
						list.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						Collection instances = searchPane.search(dba);
						// Need to sort it
						List arrayList = null;
						if (instances == null)
						    arrayList = new ArrayList<GKInstance>();
						else
						    arrayList = new ArrayList<GKInstance>(instances);
                        sortInstances(arrayList);
                        //The following way is a slower way since each addElement call will
                        // fire an action event.
                        //DefaultListModel listModel = (DefaultListModel)list.getModel();
                        //listModel.clear();
                        DefaultListModel listModel = new DefaultListModel();
                        for (Iterator it = arrayList.iterator(); it.hasNext();) {
                            GKInstance instance = (GKInstance) it.next();
                            listModel.addElement(instance);
                        }
                        list.clearSelection(); // To avoid null exception by clearing selection after setModel(): next statement.
                        list.setModel(listModel);
						// Set the title
						titleLabel.setText("Found instances of " + schemaClass.getName() + ": " + arrayList.size());
						list.setCursor(Cursor.getDefaultCursor());
					}
					catch (Exception e) {
						System.err.println("InstanceListPane.setSchemaClass(): " + e);
						e.printStackTrace();
					}
				}
			};
			t.start();
		}
	}
    
    private void sortInstances(List instances) {
        if (isInstancesShouldBeGrouped)
            InstanceUtilities.groupInstances(instances);
        else
            InstanceUtilities.sortInstances(instances);
    }
	
	public void setDisplayedInstanceDbIds(java.util.List dbIds) {
		DefaultListModel model = new DefaultListModel();
		Long dbId;
		GKInstance instance = null;
		if (dbIds != null) {
			for (Iterator it = dbIds.iterator(); it.hasNext();) {
				dbId = (Long)it.next();
				try {
					// TODO: could be dodgy using the global
					// schemaClass.
					instance = dba.fetchInstance(schemaClass.getName(), dbId);
				} catch (Exception e) {
					System.err.println("InstanceListPane.setDisplayedInstanceDbIds: WARNING - exception while fetching instance with DB_ID=" + dbId);
					e.printStackTrace();
				}
				if (instance==null) {
					System.err.println("InstanceListPane.setDisplayedInstanceDbIds: WARNING - instance is null, skipping!!");
					continue;
				}
				model.addElement(instance);
			}
		}
		list.setModel(model);
	}
	
	public void setDisplayedInstances(java.util.List instances) {
		DefaultListModel model = new DefaultListModel();
		if (instances != null) {
			for (Iterator it = instances.iterator(); it.hasNext();)
				model.addElement(it.next());
		}
		list.clearSelection();
		list.setModel(model);
	}
	
	public void addDisplayedInstances(java.util.List instances) {
		DefaultListModel model = (DefaultListModel)list.getModel();
		if (instances != null) {
			for (Iterator it = instances.iterator(); it.hasNext();)
				model.addElement(it.next());
		}
		list.setModel(model);
	}
	
	public void addDisplayedInstance(GKInstance instance) {
		DefaultListModel model = (DefaultListModel)list.getModel();
		model.addElement(instance);
	}
	
	public void deleteDisplayedInstance(GKInstance instance) {
		DefaultListModel model = (DefaultListModel)list.getModel();
		model.removeElement(instance);
	}
	
	public java.util.List getDisplayedInstances() {
		java.util.List rtn = new ArrayList();
		ListModel model = list.getModel();
		for (int i = 0; i < model.getSize(); i++)
			rtn.add(model.getElementAt(i));
		return rtn;
	}
	
	public void setTitle(String title) {
		titleLabel.setText(title);
	}
    
    public void hideTitle() {
        titleLabel.getParent().setVisible(false);
    }
	
	/**
	 * Set the SchemaClass for this InstanceListPane. A list of Instances
	 * will be loaded by using another thread.
	 * @param class1
	 */
	public void setSchemaClass(SchemaClass class1) {
		if (this.schemaClass == class1)
			return;
		if (class1 == null) {
			DefaultListModel model = (DefaultListModel) list.getModel();
			model.clear();
			setTitle("Nothing Displayed"); // Empty the title
			this.schemaClass = null;
			return ;
		}
		if (class1 instanceof GKSchemaClass) {
			schemaClass = (GKSchemaClass) class1;
			// This might be wrong if the user selects another SchemaClass during
			// loading.
			Thread t = new Thread() {
				public void run() {
					loadInstancesFor(schemaClass);
				}
			};
			t.start();
		}
	}
	
	public void refresh() {
		if (schemaClass == null)
			return;
		java.util.List selection = getSelection();
		titleLabel.setText("Loading instances of " + schemaClass.getName() + "...");
		list.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
            // Temporialy disable event firing
            ListSelectionListener[] listeners = list.getListSelectionListeners();
            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++)
                    list.removeListSelectionListener(listeners[i]);
            }
			Collection instances = dba.fetchInstancesByClass(schemaClass);
			DefaultListModel listModel = new DefaultListModel();
			java.util.List selectedIndex = new ArrayList(selection.size());
			if (instances != null && instances.size() > 0) {
				// Need to sort it
				java.util.List arrayList = new ArrayList(instances);
				sortInstances(arrayList);
				int index = 0;
				for (Iterator it = arrayList.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					listModel.addElement(instance);
					if (selection.contains(instance))
						selectedIndex.add(new Integer(index));
					index ++;
				}
			}
			list.setModel(listModel);
			// Stick to the original selection if any
			for (Iterator it = selectedIndex.iterator(); it.hasNext();) {
				Integer index = (Integer) it.next();
				list.addSelectionInterval(index.intValue(), index.intValue());
			}
			if (selectedIndex.size() > 0) {
				// Ensure the first is visible
				Integer index = (Integer) selectedIndex.get(0);
				list.ensureIndexIsVisible(index.intValue());
			}
            // Add back listeners
            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++)
                    list.addListSelectionListener(listeners[i]);
            }
		}
		catch (Exception e) {
			System.err.println("InstanceListPane.refresh(): " + e);
			e.printStackTrace();
		}
		// Set the title
		titleLabel.setText(schemaClass.getName() + ": " + list.getModel().getSize());
		list.setCursor(Cursor.getDefaultCursor());
	}
	
	/**
	 * Directly load a list of Instances for the specified SchemaClass. There is no
	 * extra Thread used to loading. The main thread will be waiting for the loading 
	 * done.
	 * This method is synchronzied so that no two threads can access it at the same time
	 * to avoid the conflicting and unpredicated behaviors.
	 * @param class1
	 * @see setSchemaClass(SchemaClass)
	 */
	public synchronized void loadInstancesFor(GKSchemaClass class1) {
		this.schemaClass = class1;
		if (dba == null)
			return;
		try {
			// To keep the original selection
			java.util.List selection = null;
			if (selectedInstances != null) {
				java.util.List l = (java.util.List) selectedInstances.get(schemaClass);
				if (l != null && l.size() > 0)
					selection = new ArrayList(l);
			}
			// Temporary disable selection listeners
			ListSelectionListener[] listeners = list.getListSelectionListeners();
			if (listeners != null) {
				for (int i = 0; i < listeners.length; i++)
					list.removeListSelectionListener(listeners[i]);
			}
			titleLabel.setText("Loading instances of " + schemaClass.getName() + "...");
			list.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Collection instances = dba.fetchInstancesByClass(schemaClass);
			DefaultListModel listModel = new DefaultListModel();
			if (instances != null && instances.size() > 0) {
				// Need to sort it
				java.util.List arrayList = new ArrayList(instances);
				sortInstances(arrayList);
				//The following way is a slower way since each addElement call will
				// fire an action event.
				//DefaultListModel listModel = (DefaultListModel)list.getModel();
				//listModel.clear();
				for (Iterator it = arrayList.iterator(); it.hasNext();) {
					GKInstance instance = (GKInstance)it.next();
					listModel.addElement(instance);
				}
			}
			// Call this method first to avoid a null exception from lis.setModel(listModel)
			list.clearSelection();
			list.setModel(listModel);
			// Set the title
			titleLabel.setText(schemaClass.getName() + ": " + listModel.getSize());
			list.setCursor(Cursor.getDefaultCursor());
			// Add back the listeners
			if (listeners != null) {
				for (int i = 0; i < listeners.length; i++)
					list.addListSelectionListener(listeners[i]);
			}
			// Stick to the original selection if any
			if (selection != null) {
				for (Iterator it = selection.iterator(); it.hasNext();) {
					Instance instance = (Instance)it.next();
					// Instance might be changed. Have to compare the DB_IDs
					for (int i = 0; i < listModel.getSize(); i++) {
						Instance tmp = (Instance) listModel.get(i);
						if (tmp.getDBID().equals(instance.getDBID())) {
							list.addSelectionInterval(i, i);
							break;
						}
					}
				}
				// Scroll to the first selection
				final int selectedIndex = list.getSelectedIndex();
				if (selectedIndex >= 0) {// Have to call later to ensure the correct scroll
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							list.ensureIndexIsVisible(selectedIndex);		
						}
					});
				}
			}
		}
		catch (Exception e) {
			System.err.println("InstanceListPane.setSchemaClass(): " + e);
			e.printStackTrace();
		}
	}
	
	public SchemaClass getSchemaClass() {
		return this.schemaClass;
	}
	
	public JList getInstanceList() {
		return list;
	}
	
	public void setPersistenceAdaptor(PersistenceAdaptor adaptor) {
		this.dba = adaptor;
		setSchemaClass(null); // Force the display to clear
	}
	
	public PersistenceAdaptor getPersistenceAdaptor() {
		return this.dba;
	}
	
	public void addSelectionListener(ListSelectionListener listener) {
		list.addListSelectionListener(listener);
	}
	
	public void removeSelectionListener(ListSelectionListener listener) {
		list.removeListSelectionListener(listener);
	}
	
	public java.util.List getSelection() {
		if (list.getSelectedValue() == null)
			return new ArrayList();
		return Arrays.asList(list.getSelectedValues());
	}
	
	public void setSelectionMode(int mode) {
		list.setSelectionMode(mode);
	}
	
	public void setSelection(Instance instance) {
		//if (instance.getSchemClass() == schemaClass) {
			list.setSelectedValue(instance, true);
		//}
	}
	
	/**
	 * Make the specified list of GKInstances selected.
	 * @param instances
	 */
	public void setSelection(java.util.List instances) {
	    list.clearSelection();
	    if (instances == null || instances.size() == 0) {
	        return;
	    }
	    DefaultListModel model = (DefaultListModel) list.getModel();
	    int index = -1;
	    for (int i = 0; i < instances.size(); i++) {
	        GKInstance instance = (GKInstance) instances.get(i);
	        index = model.indexOf(instance);
	        if (index < 0)
	            continue;
	        list.addSelectionInterval(index, index);
	        if (i == 0)
	            list.ensureIndexIsVisible(index);
	    }
	}
	
	public void addSelection(Instance instance) {
		if (instance.getSchemClass() == schemaClass) {
			DefaultListModel model = (DefaultListModel) list.getModel();
			int index = model.indexOf(instance);
			list.addSelectionInterval(index, index);
			list.ensureIndexIsVisible(index);
		}
	}
	
	/**
	 * A viewable InstanceListPane is like this: Double clicking an Instance
	 * will popup a frame to display the properties of the clicked intance.
	 * @param isViewable
	 */
	public void setIsViewable(boolean isViewable) {
		this.isViewable = isViewable;
	}
	
	public boolean isViewable() {
		return this.isViewable;
	}
	
	public boolean isEditable() {
		return this.isEditable;
	}
	
	public void setEditable(boolean editable) {
		this.isEditable = editable;
	}
	
	/**
	 * Do an update because of the instance list changes in the specified 
	 * SchemaClass.
	 * @param schemaClass
	 */
	public void addInstance(Instance instance) {
		// Since instances in a subclass is also displayed. So have to check based on hierarchical tree.
	    if (schemaClass != null && instance.getSchemClass().isa(schemaClass)) {
	        DefaultListModel model = (DefaultListModel) list.getModel();
			// Have to find the position for this instance
			int size = model.getSize();
			String title = schemaClass.getName() + ": " + (size + 1);
			if (instance.getDisplayName() != null) {
                String displayName = instance.getDisplayName();
                String tmpName = null;
			    for (int i = 0; i < size; i++) {
                    Instance tmp = (Instance) model.get(i);
                    tmpName = tmp.getDisplayName();
                    if (tmpName == null)
                        tmpName = tmp.toString();
                    if (tmpName.compareTo(displayName) > 0) {
                        model.add(i, instance);
                        list.ensureIndexIsVisible(i);
                        // Set the title
                        titleLabel.setText(title);
                        return;
                    }
                }
            }
			// Add at the last
			model.addElement(instance);
			titleLabel.setText(title);
		}
	}
	
	/**
	 * Set the selected instances so that the selected instances can be kept
	 * during switching.
	 * @param instancesMap
	 */
	public void setSelectedInstances(Map instancesMap) {
		this.selectedInstances = instancesMap;
	}
	
	public void markAsDirty(GKInstance instance) {
	    if (schemaClass == null)
	        return;
	    if (instance.getSchemClass().isa(schemaClass)) {
            if (isInstancesShouldBeGrouped) 
                refresh(); // A simple way. Probably there is a performance penality.
                           // Keep an eye on it if it is too slow.
            else
                list.repaint(list.getVisibleRect());
        }
	}
	
	public void removeDirtyFlag(GKInstance instance) {
	    if (schemaClass == null)
	        return;
	    if (instance.getSchemClass().isa(schemaClass)) {
            if (isInstancesShouldBeGrouped)
                refresh(); // A simple way. Might impact performance.
	        list.repaint(list.getVisibleRect());
	    }	    
	}
	
	/**
	 * Delete the selected GKInstances.
	 * @return true for a successful delete.
	 */
	public boolean deleteInstance(Instance instance) {
		boolean rtn = false;
		// Check SchemaClass
		if (schemaClass == null || instance.getSchemClass().isa(schemaClass)) {
		    DefaultListModel model = (DefaultListModel) list.getModel();
			// Have to find the position for this instance
			int size = model.getSize();
			if (instance.getSchemClass().isa(schemaClass)) {
				String title = titleLabel.getText();
				int index = title.lastIndexOf(": ");
				if (index >= 0) {
                    title = title.substring(0, index);
                    title = title + ": " + (size - 1);
                    titleLabel.setText(title);
                }
			}
			int index = model.indexOf(instance);
			if (index >= 0) {
				model.remove(index);
				rtn = true;
			}
			if (index >= 0 && index < model.size())
				list.addSelectionInterval(index, index);
		}
		return rtn;
	}
	
	/**
	 * Delete a list of GKInstance objects.
	 */
	public void deleteInstances(java.util.List instances) {
		if (instances == null || instances.size() == 0)
			return;
		DefaultListModel model = (DefaultListModel) list.getModel();
		for (Iterator it = instances.iterator(); it.hasNext();) {
			Object obj = it.next();
			model.removeElement(obj); 
		}
		String title = titleLabel.getText();
		int index = title.lastIndexOf(": ");
		if (index >= 0) {
            title = title.substring(0, index);
            title = title + ": " + model.getSize();
            titleLabel.setText(title);
        }
	}
	
	public void clearSelection() {
		list.clearSelection();
	}
    
    class ViewSettingPanel extends JPanel {
        private JCheckBox groupBox;
        private JCheckBox showSpeciesBox;
        private JRadioButton speciesBeforeBtn;
        private JRadioButton speciesAfterBtn;
        
        public ViewSettingPanel() {
            init();
        }
        
        public void show(InstanceListPane listPane) {
            JLayeredPane layeredPane = SwingUtilities.getRootPane(listPane).getLayeredPane();
            Rectangle viewBounds = listPane.getInstanceList().getParent().getBounds();
            Point p = SwingUtilities.convertPoint(listPane.getInstanceList().getParent(), 
                                                  viewBounds.x, 
                                                  viewBounds.y, 
                                                  layeredPane);
            setBounds(p.x, 
                      p.y, 
                      viewBounds.width, 
                      150);
            layeredPane.add(this, JLayeredPane.POPUP_LAYER);
            setValues();
            setVisible(true);
            layeredPane.revalidate();
            layeredPane.repaint();
        }
        
        private void setValues() {
            InstanceListPane listPane = InstanceListPane.this;
            groupBox.setSelected(listPane.isInstancesShouldBeGrouped());
            showSpeciesBox.setSelected(listPane.isSpeciesDisplayed());
            speciesAfterBtn.setEnabled(showSpeciesBox.isSelected());
            speciesBeforeBtn.setEnabled(showSpeciesBox.isSelected());
            speciesAfterBtn.setSelected(listPane.isSpeciesAfterName());
            speciesBeforeBtn.setSelected(!speciesAfterBtn.isSelected());
        }
        
        public void hide(InstanceListPane listPane) {
            JLayeredPane layeredPane = SwingUtilities.getRootPane(listPane).getLayeredPane();
            layeredPane.remove(this);
            setVisible(false);
            layeredPane.revalidate();
            layeredPane.repaint();
        }
        
        private void init() {
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 4, 2, 4);
            constraints.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel("Choose the settings for instance list:");
            groupBox = new JCheckBox("Aggregate changed instsances together");
            showSpeciesBox = new JCheckBox("Show species in the list:");
            speciesBeforeBtn = new JRadioButton("Before display name");
            speciesAfterBtn = new JRadioButton("After display name");
            ButtonGroup group = new ButtonGroup();
            group.add(speciesAfterBtn);
            group.add(speciesBeforeBtn);
            add(label, constraints);
            constraints.gridy = 1;
            add(groupBox, constraints);
            constraints.gridy = 2;
            add(showSpeciesBox, constraints);
            constraints.gridy = 3;
            constraints.insets = new Insets(2, 12, 2, 4);
            add(speciesBeforeBtn, constraints);
            constraints.gridy = 4;
            add(speciesAfterBtn, constraints);
            setVisible(false);
            speciesBeforeBtn.setEnabled(false);
            speciesAfterBtn.setEnabled(false);
            speciesBeforeBtn.setSelected(true); // default
            addChangeListners();
        }
        
        private void addChangeListners() {
            groupBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (InstanceListPane.this.isInstancesShouldBeGrouped() == groupBox.isSelected())
                        return;
                    InstanceListPane.this.setIsInstancesShouldBeGrouped(groupBox.isSelected());
                    InstanceListPane.this.refresh();
                }
            });
            showSpeciesBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (showSpeciesBox.isSelected() == 
                        InstanceListPane.this.isSpeciesDisplayed())
                        return;
                    speciesAfterBtn.setEnabled(showSpeciesBox.isSelected());
                    speciesBeforeBtn.setEnabled(showSpeciesBox.isSelected());
                    InstanceListPane.this.setSpeciesDisplayed(showSpeciesBox.isSelected());
                    InstanceListPane.this.setSpeciesAfterName(speciesAfterBtn.isSelected());
                    InstanceListPane.this.refresh();
                }
            });
            ActionListener radioAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (!showSpeciesBox.isSelected())
                        return;
                    InstanceListPane.this.setSpeciesAfterName(speciesAfterBtn.isSelected());
                    InstanceListPane.this.refresh();
                }
            };
            speciesBeforeBtn.addActionListener(radioAction);
            speciesAfterBtn.addActionListener(radioAction);
        }
    }
	
	class InstanceTransferHandler extends TransferHandler {
				
		/**
		 *@return an ArrayListTransferable object containing a list of
		 *Renderable objects that are converted from GKInstances.
		 */
		protected Transferable createTransferable(JComponent c) {
			JList iList = (JList) c;
			Object[] instances = list.getSelectedValues();
			if (instances != null && instances.length > 0) {
				SchemaClass cls = ((GKInstance)instances[0]).getSchemClass();
				if (cls.isa("Event") || cls.isa("PhysicalEntity")) {
					ArrayList list = new ArrayList(instances.length);
					try {
						for (int i = 0; i < instances.length; i++) {
							GKInstance instance = (GKInstance)instances[i];
							Renderable renderable = RenderUtility.convertToNode(instance, true);
							if (renderable != null)
								list.add(renderable);
						}
					}
					catch (Exception e) {
						System.err.println("InstanceListPane.createTransferable(): " + e);
						e.printStackTrace();
					}
					if (list.size() > 0) {
						list.add(0, "dbBrowser");
						return new ArrayListTransferable(list);
					}
				}
			}
			return null;
		}
		
		/**
		 * Only copy is allowed for DnD.
		 */
		public int getSourceActions(JComponent c) {
			return COPY; 
		}
	}	
}
