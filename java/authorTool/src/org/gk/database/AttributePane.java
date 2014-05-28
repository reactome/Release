/*
 * Created on Sep 22, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.graphEditor.ArrayListTransferable;
import org.gk.model.Bookmark;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.model.StoichiometryInstance;
import org.gk.persistence.Bookmarks;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPane is used to display attributes for a GKInstance.
 * @author wgm
 */
public class AttributePane extends JPanel {
	// Icon for table renderer
	private Icon instanceIcon = GKApplicationUtilities.createImageIcon(getClass(), "Instance.gif");
	//GUIs
	private JLabel titleLabel;
	private AttributeTable propTable;
	// For launch a view for cell value
	private MouseListener cellMouseAdaptor;	
	private GKInstance instance;
	// A flag to mark as read-only
	private boolean isEditable;
	// Allows the automatic generation of empty slots in multiple-
	// value entries.
	private boolean emptySlotsInMultiple;
	// A flag to control the attribute editing
	protected boolean localChangeOnly;
	// Actions
	private JToolBar toolbar;
	// Allows user to switch attribute combo box editor on and off.
	private JButton allowComboBoxEditorBtn;
	private boolean allowComboBoxEditor = AttributeEditConfig.getConfig().isAllowComboBoxEditor();
	// For shell instance
	private ShellPane shellPane;
	// A system wide properties
	private java.util.List uneditableAttNames = AttributeEditConfig.getConfig().getUneditableAttNames();
	// For editing actions
	private AttributePaneController controller;
	// Sometimes all slots need to be editable, e.g., during merging
	private boolean isAllSlotsEditable = false;
	// Sets the number of clicks needed to start editing a cell
	protected static int DEFAULT_CLICK_COUNT = 2; // orig. val. = 2
	// Some special attribute names are hardcoded here
	protected static String ATTRIBUTE_NAME_DATABASE_ID = "DB_ID";
	protected static String ATTRIBUTE_NAME_DISPLAY_NAME = "_displayName";
	// To control the sort mechanism
    private boolean isGroupedByCategories;
    // To validate the editing
    private AttributeEditValidator validator;
    
	public AttributePane() {
		init();
	}

	public AttributePane(GKInstance newInstance) {
		this();
		setInstance(newInstance);
	}
	
	public void setEditable(boolean editable) {
		this.isEditable = editable;
		toolbar.setVisible(isEditable);
	}
	
	public void setEmptySlotsInMultiple(boolean emptySlotsInMultiple) {
		this.emptySlotsInMultiple = emptySlotsInMultiple;
	}
	
	public void stopEditing() {
		if (propTable.isEditing()) {
			CellEditor editor = propTable.getCellEditor();
			editor.stopCellEditing();
		}
	}
	
	public boolean isEditable() {
		return this.isEditable;
	}
	
	public AttributeEditValidator getEditValidator() {
	    if (validator == null)
	        validator = new AttributeEditValidator();
	    return this.validator;
	}

    public boolean isUneditableAttributeName(String attributeName) {
        if (uneditableAttNames==null)
            return false;
        return uneditableAttNames.contains(attributeName);
    }
    
	public void showCheckoutButton() {
	    // Add a download button to the bottom
	    final JPanel panel = new JPanel();
	    JLabel label = new JLabel("Click to check out this instance to the opened project:");
	    JButton btn = new JButton("Check Out");
	    panel.setBorder(BorderFactory.createRaisedBevelBorder());
	    panel.setLayout(new GridBagLayout());
	    GridBagConstraints constraints = new GridBagConstraints();
	    constraints.anchor = GridBagConstraints.WEST;
	    constraints.insets = new Insets(6, 8, 8, 8);
	    constraints.weightx = 0.2;
	    panel.add(label, constraints);
	    constraints.gridx = 1;
	    panel.add(btn, constraints);
	    add(panel, BorderLayout.SOUTH);
	    // Add action
	    btn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            Window parentWindow = (Window)SwingUtilities.getRoot(AttributePane.this);
	            List list = new ArrayList(1);
	            list.add(instance);
	            try {
                    SynchronizationManager.getManager().checkOut(list, parentWindow);
                    remove(panel);
                    validate();
                }
                catch (Exception e1) {
                    System.err.println("AttibutePane.showCheckOutButton(): " + e);
                    e1.printStackTrace();
                }
	        }
	    });
	}
	
	/**
	 * Set all slots editable. This setting will overwrite the default unedtiable slots setting.
	 * However, the client still should call setEdtiable(boolean) first to make this AttributePane
	 * editable.
	 * @param editable
	 */
	public void setIsAllSlotEditable(boolean editable) {
	    this.isAllSlotsEditable = editable;
	}
	
//	/**
//	 * If a change somewhere in the CuratorTool neccessitates an
//	 * update to one of the buttons in AttributePane, this class
//	 * will perform the update as soon as the user moves the mouse
//	 * into the area of the AttributePane.
//	 * 
//	 * @author croft
//	 *
//	 */
//	class ButtonUpdateMouseListener implements MouseListener {
//        public void mouseEntered(MouseEvent e) {
//        	allowComboBoxEditorPropagate();
//        }
//        
//        public void mouseExited(MouseEvent e) {
//        	allowComboBoxEditorPropagate();
//        }
//        
//        public void mouseClicked(MouseEvent e) {
//        }
//        
//        public void mousePressed(MouseEvent e) {
//        	allowComboBoxEditorPropagate();
//        }
//        
//        public void mouseReleased(MouseEvent e) {
//        }
//        
//    	// Make sure editing status gets propagated from
//    	// other views.
//        private void allowComboBoxEditorPropagate() {
//        	setAllowComboBoxEditor(AttributeEditConfig.getConfig().isAllowComboBoxEditor());
//        }
//        
//    }
	
	public void setAllowComboBoxEditor(boolean allowComboBoxEditor) {
	    this.allowComboBoxEditor = allowComboBoxEditor;
	    allowComboBoxEditorBtn.setIcon(findAllowComboBoxEditorIcon());
	}
	
	private void init() {
	    // Using mouse entering or exiting to change the UI is not
        // a good GUI programming practive. The user will be feeling
        // puzzled. This change is handled by a PropertyChangeListnere
        // in GKCuratorFrame.init().
        //this.addMouseListener(new ButtonUpdateMouseListener());
	    
	    // To control actions
	    controller = new AttributePaneController(this);
	    
	    setLayout(new BorderLayout());
	    JPanel northPane = new JPanel();
	    northPane.setLayout(new BorderLayout());
	    titleLabel = new JLabel("Properties");
	    titleLabel.setToolTipText("Properties");
	    titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
	    titleLabel.setHorizontalAlignment(JLabel.LEFT);
	    northPane.add(titleLabel, BorderLayout.WEST);
	    
	    // Add actions
	    controller.initActions();
	    toolbar = new JToolBar();
	    Dimension btnSize = new Dimension(20, 20);
	    
	    allowComboBoxEditorBtn = new JButton(findAllowComboBoxEditorIcon());
	    allowComboBoxEditorBtn.setPreferredSize(btnSize);
	    setTooltipForAllowCBEditorButton();
	    // Should not be selected! If selected, a dark background will be used.
        // However, different icons have been used to indicate the status. Another 
        // problem is that the selection backgroun is not gone after allowComboBoxEditor
        // is set to false.
        //allowComboBoxEditorBtn.setSelected(allowComboBoxEditor);
	    allowComboBoxEditorBtn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            if (allowComboBoxEditor) {
	                allowComboBoxEditor = false;
	                PropertyCellEditor editor = (PropertyCellEditor) propTable.getCellEditor();
	                if (editor != null)
	                    editor.setComboBox(null);
	            } else {
	                allowComboBoxEditor = true;
	            }
	            AttributeEditConfig.getConfig().setAllowComboBoxEditor(allowComboBoxEditor); // set user preference
	            allowComboBoxEditorBtn.setIcon(findAllowComboBoxEditorIcon());
	            setTooltipForAllowCBEditorButton();
	        }
	    });
	    toolbar.add(allowComboBoxEditorBtn);
	    
	    JButton btn;
	    btn = toolbar.add(controller.getSortByAlphabetAction());
	    btn.setPreferredSize(btnSize);
        btn = toolbar.add(controller.getSortByCategoryAction());
        btn.setPreferredSize(btnSize);
	    btn = toolbar.add(controller.getViewAction());
	    btn.setPreferredSize(btnSize);
	    btn = toolbar.add(controller.getAddAction());
	    btn.setPreferredSize(btnSize);
	    btn = toolbar.add(controller.getRemoveAction());
	    btn.setPreferredSize(btnSize);
	    northPane.add(toolbar, BorderLayout.EAST);
	    add(northPane, BorderLayout.NORTH);
	    toolbar.setVisible(isEditable);
	    
	    propTable = new AttributeTable();
	    PropertyTableModel model = new PropertyTableModel();
	    propTable.setModel(model);
	    propTable.setDefaultEditor(String.class, new PropertyCellEditor(new JTextField()));
	    // To popup a new AttributePane
	    propTable.addMouseListener(getCellMouseAdaptor());
	    //propTable.addMouseListener(new ButtonUpdateMouseListener());
	    // Don't allow pending editing. Commit all changes when the focus
	    // is moving out of the table.
	    propTable.addFocusListener(new FocusAdapter() {
	        public void focusLost(FocusEvent e) {
	            PropertyCellEditor editor = (PropertyCellEditor) propTable.getCellEditor();
	            if (editor != null && editor.getCurrentComponent() == e.getOppositeComponent())
	                return;
	            stopEditing();
	        }
	    });
	    // To update the actions
	    ListSelectionListener l = new ListSelectionListener() {
	        public void valueChanged(ListSelectionEvent e) {
	            controller.validateActions();
	        }
	    };
	    propTable.getSelectionModel().addListSelectionListener(l);
	    // Also need to add to column listeners.
	    propTable.getColumnModel().getSelectionModel().addListSelectionListener(l);
	    add(new JScrollPane(propTable), BorderLayout.CENTER);
	    // Initialize ShellPane
	    shellPane = new ShellPane();
	    shellPane.setBorder(BorderFactory.createRaisedBevelBorder());
	    shellPane.viewInDBBtn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            if (instance.isShell()) {
	                Component root = SwingUtilities.getRoot(AttributePane.this);
	                if (root instanceof JDialog)
	                    FrameManager.getManager().showShellInstanceInDB(instance, AttributePane.this, (JDialog)root);
	                else
	                    FrameManager.getManager().showShellInstanceInDB(instance, AttributePane.this);
	            }
	        }
	    });
	    shellPane.downloadBtn.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	            if (instance.isShell()) {
	                controller.downloadShellInstance(instance);
	                // After download, make sure the focus is still in the pane
	                AttributePane.this.requestFocus();
	            }
	        }
	    });
        // To control the order of the attributes
        setGroupAttributesByCategory(AttributeEditConfig.getConfig().isGroupAttributesByCategories());
	    // To support DnD
	    propTable.setDragEnabled(true);
	    propTable.setTransferHandler(new ListValueTransferHandler());
	}
	
	private ImageIcon findAllowComboBoxEditorIcon() {
		if (allowComboBoxEditor) {
			//return new ImageIcon("images/AllowComboBoxEditor.gif");
			// Use this method to get an ImageIcon so that it can be loaded with this class is
			// deployed as Java Web Start, applets, etc.
			return GKApplicationUtilities.createImageIcon(getClass(), "AllowComboBoxEditor.gif");
		}
		else {
			//return new ImageIcon("images/DontAllowComboBoxEditor.gif");
			return GKApplicationUtilities.createImageIcon(getClass(), "DontAllowComboBoxEditor.gif");
		}
	}
	
	private void setTooltipForAllowCBEditorButton() {
	    if (allowComboBoxEditor)
	        allowComboBoxEditorBtn.setToolTipText("Click to disable pull down list");
	    else
	        allowComboBoxEditorBtn.setToolTipText("Click to enable pull down list");
	}
    
    public void setGroupAttributesByCategory(boolean value) {
        if (isGroupedByCategories != value) {
            isGroupedByCategories = value;
            PropertyTableModel model = (PropertyTableModel) propTable.getModel();
            if (value) 
                model.setAttributeComparator(new AttributeCategoryGroupingComparator());
            else
                model.setAttributeComparator(new AttributeAlphabeticalComparator());
            refresh(); // Have to refresh the whole display
        }   
    }
    
    public boolean isGroupAttributesByCategory() {
        return this.isGroupedByCategories;
    }
	
	public void setInstance(Instance newInstance) {
		if (instance == newInstance)
			return; 
        if (propTable.isEditing())
            propTable.getCellEditor().stopCellEditing();
		if (newInstance == null) {
			instance = null;
			PropertyTableModel model = (PropertyTableModel) propTable.getModel();
			model.setIntance(null);
			return;
		}
		if (newInstance instanceof GKInstance) {
			GKInstance gkInstance = (GKInstance) newInstance;
			if (gkInstance.isShell() && (instance == null || !instance.isShell())) {
				add(shellPane, BorderLayout.SOUTH);
				toolbar.setVisible(false);
                 validate();
			}
			else if (!gkInstance.isShell() && (instance == null || instance.isShell())) {
				// Remove shell pane
				remove(shellPane);
				toolbar.setVisible(isEditable);
                 validate();
			}
			instance = gkInstance;
            PropertyTableModel model = (PropertyTableModel) propTable.getModel();
            model.setIntance(instance);
			
			// Use information that the attribute table model
			// knows about the attributes to color code the
			// rows in the table.
			propTable.setGrayRows(model.getDefiningRows());			
		}
        updateTitle(newInstance);
	}
	
	/**
	 * Refresh the property table. This method will only refresh the property table and
	 * the button pane for shell instances. If you want to refresh the graph display, you need to call updateGraphDisplay
	 * explictly.
	 */
	public void refresh() {
	    if (instance == null)
	        return;
		if (propTable.isEditing()) {
			CellEditor editor = propTable.getCellEditor();
			editor.cancelCellEditing();
		}
		PropertyTableModel model = (PropertyTableModel) propTable.getModel();
		model.refresh();
        propTable.setGrayRows(model.getDefiningRows());
        updateTitle(instance);
        // Have to update 
		if (instance.isShell()) {
			// Check if shellPane is in
			boolean hasShellPane = false;
			for (int i = 0; i < getComponentCount(); i++) {
				Component comp = getComponent(i);
				if (comp == shellPane) {
					hasShellPane = true;
					break;
				}
			}
			if (!hasShellPane) {
				add(shellPane, BorderLayout.SOUTH);
				toolbar.setVisible(false);
			}
		}
		else {
			remove(shellPane); // It is OK to call this method even though 
							   // shellPane is not there
			toolbar.setVisible(isEditable);							   
		}
	}
    
    private void updateTitle(Instance instance) {
        if (instance != null) {
            titleLabel.setText(instance.getSchemClass().getName() + " Properties");
            titleLabel.setToolTipText("Properties of " + instance.toString());
        }
        else {
            titleLabel.setText("Properties");
            titleLabel.setToolTipText(null);
        }
    }
	
	public void setTitle(String title) {
		titleLabel.setText(title);
        titleLabel.setToolTipText(title);
	}
	
	public Instance getInstance() {
		return this.instance;
	}
	
	/**
	 * All attributes containing nothing more than empty Strings
	 * will be removed.
	 * 
	 * @param instance
	 * @return
	 */
	public void removeEmptyAttributesFromInstance() {
		// Examine attributes, filtering out the empty
		// ones as we go.
		Collection attributes = instance.getSchemaAttributes();
		Object attributeValue;
		List attributeList;
		for (Iterator it = attributes.iterator(); it.hasNext();) {
			try {
				attributeValue = instance.getAttributeValuesList((SchemaAttribute)it.next());
				
				if (attributeValue!=null && attributeValue instanceof List) {
					attributeList = (List)attributeValue;
					while (attributeList.remove("")) {}
				}
			} catch (Exception e) {
				System.err.println("Woops, something nasty happened while examinig an attribute");
				e.printStackTrace();
			}
			
		}		
	}

	/**
	 * Decides what to do if the mouse is clicked in a cell.
	 * E.g. pop up a popup menu, giving the user editing
	 * options.
	 * 
	 * @return
	 */
	private MouseListener getCellMouseAdaptor() {
		if (cellMouseAdaptor == null) {
			cellMouseAdaptor = new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					JComponent source = (JComponent) e.getSource();
					// Blocks launching an editing popup if a cell
					// contains a component that itself is capable
					// of modifying the cell's contents.
					if (source instanceof JTextField || source instanceof JLabel ||
					    source instanceof JComboBox) {
						source.requestFocus();
						e.consume();
						return;
					}
					if (e.isPopupTrigger()) {
						doEditingPopup(e);
						return;
					}
				}
				
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						doEditingPopup(e);
					if (e.getSource() == propTable &&
					    e.getClickCount() == DEFAULT_CLICK_COUNT) {
						viewSelectedCell();
					}
				}
			};
		}
		return cellMouseAdaptor;
	}	
	
	private void viewSelectedCell() {
		int rowCount = propTable.getSelectedRowCount();
		int colCount = propTable.getSelectedColumnCount();
		if (rowCount != 1 || colCount != 1)
			return;
		int row = propTable.getSelectedRow();
		int col = propTable.getSelectedColumn();
		if (col == 0)
			return;
		Object value = propTable.getValueAt(row, col);
		PropertyCellEditor editor = (PropertyCellEditor) propTable.getCellEditor();
		if (!isEditable || (editor != null && editor.getComboBox()==null))
		    controller.viewSelectedCell();
// Commented out to allow combo-box editing of slots...
// otherwise, a new attribute view appears.  I have
// not seen any obvious sideeffects of this.  DC.
// TODO: find out sideeffects.
//		// Go to editing mode for other types of data.
//		else if (value instanceof GKInstance)
//		    controller.viewSelectedCell();
	}
		
	/**
	 * Pops up a little menu that allows a user to select an
	 * action for dealing with a selected attribute slot.
	 * Typical actions are view, edit or delete that slot.
	 * Typical event for triggering this popup is a right
	 * mouse click.
	 * 
	 * @param e the event that should trigger the popup
	 */
	public void doEditingPopup(MouseEvent e) {
	    if (instance.isShell())
	        return;
		JPopupMenu popup = new JPopupMenu();
	    if (!isEditable) {
			// Want to show referrers action only
		    popup.add(controller.getViewReferrersAction());
		}
	    else {
	        popup.add(controller.getViewAction());
	        popup.add(controller.getAddAction());
	        popup.add(controller.getRemoveAction());
	        if (controller.shouldSetStoiActionDisplayed()) {
	            popup.add(controller.getSetStoiAction());
	        }
	        popup.addSeparator();
	        popup.add(controller.getCutAction());
	        popup.add(controller.getCopyAction());
	        popup.add(controller.getPasteAction());
	        popup.add(controller.getExportAction());
	        popup.addSeparator();
	        popup.add(controller.getViewReferrersAction());
	    }
		JComponent comp = (JComponent) e.getSource();
		popup.show(comp, e.getX(), e.getY());
	}
	
	/**
	 * Add a single instance to a multi-instance slot
	 * 
	 * @param topLevelClasses
	 * @param att
	 * @param parentFrame
	 */
	public void addInstanceValue(Collection topLevelClasses, SchemaAttribute att, JFrame parentFrame) {
		controller.addInstanceValue(topLevelClasses, att, parentFrame);
	}
	
	public void removeValue(Object value, String attName) {
		controller.removeValue(value, attName);
	}
	
	/**
	 * 
	 * Put in an extra empty slot, to allow the user to
	 * add extra instances using a combo box.  This is
	 * only relevant to slots that can accept multiple
	 * values.
	 */
	private void addEmptyValue(Object value) {
		PropertyTableModel model = (PropertyTableModel)propTable.getModel();
		int row = propTable.getSelectedRow();
		String attName = model.getKeyAt(row);
		if (value!=null && controller.isMultipleAttribute(attName))
			controller.addEmptyValue(attName);
	}
	
	class PropertyCellEditor extends DefaultCellEditor {
		// Give the user a chooser for editing appropriate cells
		javax.swing.JMenu menu; // experimental
		AttributeComboBox comboBox;
		//Schema schema = null;
		Object instanceArray[] = null;
		
		// For editing text related info
		private JLabel label; // For GKInstance
		private JTextField tf; // For other values
		private JComboBox jcb; // For boolean values
		// used a enum type as String
		private JComboBox enumJcb;
		private Object editingValue;
		// The current data type
		private int valueType;
		
		/**
		 * As far as I am able to tell, this class does not use the
		 * text field supplied here as an argument.  It will bomb
		 * out though if you are foolish enough to supply a null
		 * value.
		 * A JComponent (one of JTextField, JComboBox, or JList) is required for
		 * DefaultCellEditor as described in Java API. -- WGM
		 * 
		 * @param tf1
		 */
		public PropertyCellEditor(JTextField tf1) {
			super(tf1);
			propertyCellEditorInit();
		}
		
		public void setComboBox(AttributeComboBox comboBox) {
			this.comboBox = comboBox;
		}
		
		public AttributeComboBox getComboBox() {
			return comboBox;
		}
		
		/**
		 * Pretty little combo box with its own listeners.  Implemented as
		 * an own class, because many new instances have to be created.
		 * 
		 * @author croft
		 *
		 */
		class AttributeComboBox extends JComboBox {
			public AttributeComboBox() {
				this.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						fireEditingStopped();
					}
				});
			}
		}
		
		private void propertyCellEditorInit() {
			// Give the user a chooser for editing appropriate cells
			comboBox = new AttributeComboBox();
			// Original editor components
			label = new JLabel();
			label.setOpaque(true);
			tf = new JTextField();
			// Force all pended editing to commit
			// This seems not necessary now. Also it makes calling
			// stopCellEditing() twice after press the return key in TF editing.
//			tf.addFocusListener(new FocusAdapter() {
//			    public void focusLost(FocusEvent e) {
//			        stopCellEditing();
//			    }
//			});
			label.setBorder(tf.getBorder());
			label.addMouseListener(getCellMouseAdaptor());
			tf.addMouseListener(getCellMouseAdaptor());
			jcb = new JComboBox();
			jcb.addItem(null);
			jcb.addItem("true");
			jcb.addItem("false");
			jcb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
			enumJcb = new JComboBox();
			enumJcb.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
			setClickCountToStart(DEFAULT_CLICK_COUNT);
		}
		
		/**
		 * Get a table cell editor, dependant on the schema attribute
		 * type for the current row.
		 */
		public Component getTableCellEditorComponent(
			JTable table,
			Object value,
			boolean isSelected,
			int row,
			int col) {
			this.editingValue = value;
			String text = null;
			// Have to find what type instance it should be
			PropertyTableModel model = (PropertyTableModel)propTable.getModel();
			GKSchemaAttribute att = model.getAttribute(row);
			valueType = att.getTypeAsInt();
//			valueType = model.getValueType(row);
			
			switch (valueType) {
				case SchemaAttribute.INSTANCE_TYPE :
					
					label.setFont(table.getFont());
					label.setForeground(table.getSelectionForeground());
					label.setBackground(table.getSelectionBackground());
					
					Component chooserComp = getChooserTableCellEditorComponent(table, value, row, model);
					// Give the user a chooser for editing appropriate cells
					if (chooserComp != null)
						return chooserComp;

					if (value instanceof GKInstance) {
						// This only seems to be reached if you double
						// click on one instance in a multiple-entry slot.
						GKInstance instance = (GKInstance)value;
						text = instance.getDisplayName();
						if (text == null || (text != null && text.length() == 0)) {
							text = instance.getExtendedDisplayName();
						}
						label.setIcon(instanceIcon);
						label.setText(text);
					}
					else {
						// This is the original code
						label.setIcon(null);
						label.setText("");
					}
					
					label.requestFocus();
					return label;
				case SchemaAttribute.BOOLEAN_TYPE :
					jcb.setFont(table.getFont());
					jcb.requestFocus();
					return jcb;
				case SchemaAttribute.ENUM_TYPE :
				    enumJcb.removeAllItems();
                    enumJcb.addItem(null); // Default should be null
                    for (String allowed : att.getAllowedValues())
                        enumJcb.addItem(allowed);
                    enumJcb.setSelectedItem(value);
                    enumJcb.setFont(table.getFont());
                    enumJcb.requestFocus();
                    return enumJcb;
				default : // all other schema attribute types
					// This is the original code
					tf.setFont(table.getFont());
					if (value != null) {
						text = value.toString();
						tf.setText(text);
					}
					else
						tf.setText("");
					tf.requestFocus();
					return tf;
			}
		}
		
		/**
		 * Returns a component that allows the user to choose an
		 * attribute value from a set of existing instances.  Will
		 * return a null value if no instances can be found or if
		 * allowComboBoxEditor is false.
		 * 
		 * @param table
		 * @param value
		 * @param row
		 * @param model
		 * @return
		 */
		public Component getChooserTableCellEditorComponent(JTable table, Object value, int row, PropertyTableModel model) {
			if (!allowComboBoxEditor) {
				comboBox = null;
				return comboBox;
			}
			
			// This stuff creates a combo box presenting the user
			// with a number of instances of the given class, taken from
			// the local repository.
			try {
				String key = model.getKeyAt(row);
				Collection instances = deriveInstancesFromAttributeName(key);
				if (instances!=null) {
					instanceArray = instances.toArray();

					comboBox = new AttributeComboBox();
					comboBox.setFont(table.getFont());
					
					// allows user to leave item empty, if wanted
					comboBox.addItem(null);
						
					for (int j=0;j<instanceArray.length;j++) {
						comboBox.addItem(new ComboBoxInstanceWrapper((GKInstance)instanceArray[j]));
						
						// Restore previous selection, if known.
                        if (value instanceof StoichiometryInstance) {
                            GKInstance wrappedInstance = ((StoichiometryInstance)value).getInstance();
                            if (wrappedInstance == instanceArray[j])
                                comboBox.setSelectedIndex(j + 1);
                        }
                        else if ((value instanceof GKInstance) && 
                                 (value==instanceArray[j]))
							comboBox.setSelectedIndex(j+1); // add 1 because of empty item
					}
							
					comboBox.requestFocus();

					return comboBox;
				} 
			} catch (Exception e) {
				System.err.println("AttributePane.getChooserTableCellEditorComponent: something went wrong while trying to get instances for row "+row);
				e.printStackTrace();
			}

			return null;
		}
		
		/**
		 * Wrapper class for a GKInstance that tries to provide a concise
		 * instance name via the toString method.  Combo boxes look better
		 * when you use this class, rather than a naked instance.
		 * 
		 * @author croft
		 *
		 */
		private class ComboBoxInstanceWrapper {
			private GKInstance instance = null;
			
			public ComboBoxInstanceWrapper(GKInstance instance) {
				this.instance = instance;
			}
			
			public GKInstance getInstance() {
				return(instance);
			}
			
			public String toString() {
				if (instance == null)
					return("");
				
				String name = instance.getDisplayName();
				if (name==null || name.equals(""))
					name = instance.getExtendedDisplayName();
				if (name==null || name.equals(""))
					name = instance.toString();
				if (name==null || name.equals(""))
					name = "Unknown instance of " + instance.getSchemClass().getName();
				
				return(name);
			}
		}
		
		/**
		 * Derive all instances associated with an attribute name
		 * 
 		 * @param attName
		 * @return
		 */
		private Collection deriveInstancesFromAttributeName(String attName) {
			if (attName==null)
				return null;
			
			// Get all the schema classes associated with the current
			// attribute (in some cases, there may be more than one).
			List schemaClasses = deriveSchemaClassesFromAttributeName(attName);
			
			// Find all of the instances in the local repository
			// that realize the schema classes.
			List instances = new ArrayList();
			for (Iterator it = schemaClasses.iterator(); it.hasNext();)
				instances.addAll(deriveInstancesFromSchemaClass((GKSchemaClass)it.next()));
			
			if (instances==null)
				return null;
			
			// Sort the instances to make them more user-friendly
			Collections.sort(instances, new InstanceComparator());
			
			return instances;
		}
		
		private void addAllNonRedundant(ArrayList baseList, Collection stuffToAdd) {
			Object addMe;
			for (Iterator it = stuffToAdd.iterator(); it.hasNext();) {
				addMe = it.next();
				if (addMe!=null && !baseList.contains(addMe))
					baseList.add(addMe);
			}
		}
		
		private List deriveInstancesFromSchemaClass(GKSchemaClass schemaClass) {
			if (schemaClass==null)
				return null;
			
			ArrayList instances = new ArrayList();
			Collection schemaClassInstances;
			try {
				// Add the instances found for the current schema
				// class, if any.
				schemaClassInstances = PersistenceManager.getManager().getActiveFileAdaptor().fetchInstancesByClass(schemaClass);
				if (schemaClassInstances!=null)
					addAllNonRedundant(instances, schemaClassInstances);
			} catch (Exception e) {
				System.err.println("AttributePane.deriveInstancesFromSchemaClass(): problem while trying to get some instances for the schema class "+schemaClass.getName());
				e.printStackTrace();
			}
			
			// Add the instances found for each subclass.  If there
			// are no further subclasses, then the recursion is
			// terminated.
			Collection subclasses = schemaClass.getSubClasses();
			schemaClassInstances = null;
			if (subclasses!=null) {
				GKSchemaClass attributeSchemaClass;
				for (Iterator it = subclasses.iterator(); it.hasNext();) {
					attributeSchemaClass = (GKSchemaClass)it.next();
					schemaClassInstances = deriveInstancesFromSchemaClass(attributeSchemaClass);
					if (schemaClassInstances!=null)
						addAllNonRedundant(instances, schemaClassInstances);
				}
			}
			
			return instances;
		}
		
		/**
		 * Find the schema class corresponding to the given attribute name.
		 * This tells you what kind of object is found in what slot.
		 * 
		 * TODO: there is some overlap with code in AttributePaneController
		 * here, it would be good if this could be merged somehow.
		 * 
		 * @param attName
		 * @return
		 */
		protected List deriveSchemaClassesFromAttributeName(String attName) {
			SchemaAttribute att = null;
			GKInstance instance = (GKInstance)getInstance();
			try {
				att = instance.getSchemClass().getAttribute(attName);
			}
			catch(InvalidAttributeException e) {
				System.err.println("AttributePane.deriveSchemaClassFromAttributeName(): " + e);
				e.printStackTrace();
			}
			if (att==null)
				return null;
			
			Collection allowedClasses = att.getAllowedClasses();
			if (allowedClasses == null || allowedClasses.size() == 0) {
				System.err.println("AttributePane.deriveSchemaClassFromAttributeName(): There are no allowed classes for the attribute "+attName);
				return null;
			}
			List schemaClasses = new ArrayList();
			for (Iterator it = allowedClasses.iterator(); it.hasNext();)
				schemaClasses.add(it.next());
			
			return schemaClasses;
		}
		
		/**
		 * Used for comparing pairs of instances, to determine
		 * their ordering in a sort.
		 * 
		 * @author croft
		 *
		 */
		class InstanceComparator implements Comparator {
			public int compare(Object o1, Object o2) {
				GKInstance instance1 = null;
				GKInstance instance2 = null;
				try {
					instance1 = (GKInstance)o1;
					instance2 = (GKInstance)o2;
				} catch (ClassCastException e) {
					System.err.println("AttributePane.InstanceComparator.compare(): One of the objects for comparison does not have type GKInstance");
					e.printStackTrace();
					return 0;
				}
				
				String instanceName1 = instance1.getDisplayName();
				if (instanceName1==null)
					instanceName1 = instance1.getExtendedDisplayName();
				String instanceName2 = instance2.getDisplayName();
				if (instanceName2==null)
					instanceName2 = instance2.getExtendedDisplayName();
				
				// It doesn't make sense to try sorting things that don't
				// have names.
				if (instanceName1==null || instanceName2==null)
					return 0;
				
				// Do a simple lexicographic comparison of the attribute
				// names.
				return instanceName1.compareToIgnoreCase(instanceName2);
			}
		}
		
		/**
		 * Pulls a value from the current cell editor component.
		 */
		public Object getCellEditorValue() {
			String str;
			switch (valueType) {
				case SchemaAttribute.INSTANCE_TYPE :
					// Give the user a chooser for editing appropriate cells
					if (comboBox!=null) {
						Object selectedItem = comboBox.getSelectedItem();
						
						// Make sure that nothing gets transferred from one slot
						// to another.
//						comboBox.removeAllItems();
						
						if (selectedItem==null)
							return null;
						else if (selectedItem instanceof ComboBoxInstanceWrapper)
							return ((ComboBoxInstanceWrapper)selectedItem).getInstance();
						else
							return trimmedStringFromObject(selectedItem);
					}
					
					return editingValue;
				case SchemaAttribute.BOOLEAN_TYPE :
					str = trimmedStringFromObject(jcb.getSelectedItem());
					if (str == null || str.length() == 0)
						return null;
					return new Boolean(str);
				case SchemaAttribute.ENUM_TYPE :
				    str = (String) enumJcb.getSelectedItem();
				    if (str != null && str.length() == 0)
				        return null;
				    return str;
				case SchemaAttribute.STRING_TYPE :
					str = trimmedStringFromObject(tf.getText());
					if (str == null || str.length() == 0)
						return null;
					return str;
				case SchemaAttribute.INTEGER_TYPE :
					str = trimmedStringFromObject(tf.getText());
                     if (str.length() == 0)
                         return null;
					return new Integer(str);
				case SchemaAttribute.LONG_TYPE :
					str = trimmedStringFromObject(tf.getText());
                     if (str.length() == 0)
                         return null;
					return new Long(str);
				case SchemaAttribute.FLOAT_TYPE :
					str = trimmedStringFromObject(tf.getText());
                     if (str.length() == 0)
                         return null;
					return new Float(str);
				default :
					// Should not come to here.
					return tf.getText();
			}
		}
		
		private String trimmedStringFromObject(Object o) {
			return o==null?null:o.toString().trim();
		}
		
		public JComponent getCurrentComponent() {
			switch (valueType) {
				case SchemaAttribute.INSTANCE_TYPE :
					// Give the user a chooser for editing appropriate cells
					if (comboBox!=null)
						return comboBox;
					
					return label;
				case SchemaAttribute.BOOLEAN_TYPE :
					return jcb;
				case SchemaAttribute.ENUM_TYPE :
				    return enumJcb;
				default :
					
					return tf;
			}
		}
		
		/**
		 * Validate the input data. This method is synchronized to avoid multiple
		 * call at the same time.
		 */
		public synchronized boolean stopCellEditing() {
			// Only numeric values are checked here.
			if (valueType == SchemaAttribute.INSTANCE_TYPE ||
			    valueType == SchemaAttribute.BOOLEAN_TYPE ||
			    valueType == SchemaAttribute.ENUM_TYPE) {
			    fireEditingStopped();
			    return true;
			}
			String text = tf.getText().trim();
			// It can be empty.    
			if (text.length() == 0) {
				fireEditingStopped();
				return true;
			}
			switch (valueType) {
				case SchemaAttribute.INTEGER_TYPE :
					try {
						Integer.parseInt(text);
					}
					catch(NumberFormatException e) {
						JOptionPane.showMessageDialog(propTable, 
						     "Please input an integer number",
						     "Input Error",
						     JOptionPane.ERROR_MESSAGE);
						return false;
					}
					break;
				case SchemaAttribute.LONG_TYPE :
					try {
						Long.parseLong(text);
					}
					catch(NumberFormatException e) {
						JOptionPane.showMessageDialog(propTable,
						     "Please input a long number",
						     "Input Error",
						     JOptionPane.ERROR_MESSAGE);
						return false;
					}
					break;
				case SchemaAttribute.FLOAT_TYPE :
					try {
						Float.parseFloat(text);
					}
					catch(NumberFormatException e) {
						JOptionPane.showMessageDialog(propTable,
						       "Please input a float number",
						       "Input Error",
						       JOptionPane.ERROR_MESSAGE);
						return false;
					}
					break;
			}
			fireEditingStopped();
			return true;
		}
	}
	
	/**
	 * @param localChangeOnly true if the attribute editing will not propagate to other
	 * classes.
	 */
	public void setLocalChangeOnly(boolean localChangeOnly) {
		this.localChangeOnly = localChangeOnly;
	}
	
	public JTable getPropertyTable() {
		return propTable;
	}
	
	/**
	 * Get a list of any values that have been selected in this Object.
	 * @return
	 */
	public List<Object> getSelectedValues() {
	    List<Object> values = new ArrayList<Object>();
	    if (propTable.getSelectedColumnCount() > 1 || propTable.getSelectedColumn() == 0)
	        return values; // No value has been selected or too many has been selected
	    if (propTable.getSelectedRowCount() > 0 && propTable.getSelectedColumn() == 1) {
	        for (int index : propTable.getSelectedRows()) {
	            values.add(propTable.getValueAt(index, 1));
	        }
	    }
	    return values;
	}
	
	/**
	 * Get the attribute name for the selected value.
	 * @return
	 */
	public String getSelectedAttributeName() {
	    PropertyTableModel model = (PropertyTableModel) propTable.getModel();
	    int row = propTable.getSelectedRow();
	    int type = model.getValueType(row);
	    String attName = model.getKeyAt(row);
	    return attName;
	}
    
    public AttributePaneController getController() {
        return controller;
    }

	class PropertyTableModel extends AttributeTableModel {
		private String[] headers = new String[] { "Property Name", "Value" };
		private Map valueMap = new HashMap(); // Maps attribute names to slot values.
		                                       // Key: attribute name, values: value list from GKInstance
		private Map emptyCellMap = new HashMap(); // Maps attribute names to the existence of empty slots
		private Map currentSchemaClassMap = new HashMap(); // Maps attribute names to schema classes for slots
		private java.util.List keys = new ArrayList();
		private Object[][] values;
		private GKInstance instance;
		// Use to control hidden attributes
		private List hiddenAttNames = AttributeEditConfig.getConfig().getHiddenAttNames();
		// Cell span
		private CellSpan cellSpan;
		// For attributes sorting
        private Comparator attributeComparator;
        
		public PropertyTableModel() {
			cellSpan = new CellSpan();
			values = new Object[1][1];
		}

		public CellSpan getCellSpan() {
			return cellSpan;
		}

		/**
		 * Restricts the cells that the user is allowed to edit.
		 * The first column contains attribute names and the user
		 * is not allowed to edit those.  Additionally, some
		 * attribute values may not be edited.  The names of
		 * these attributes are used to tell the system about
		 * their editability; you can set them using the
		 * setUneditableAttNames method.
		 */
		public boolean isCellEditable(int row, int col) {
			if (!isEditable || instance.isShell())
				return false;
			if (col == 0) {
				return false;
			}
			if (isAllSlotsEditable)
			    return true;
			
			String key = getKeyAt(row);
            return !isUneditableAttributeName(key);
		}
		
		/**
		 * Import an instance and set up the table model to
		 * reflect the new instance.
		 * 
		 * @param instance
		 */
		public void setIntance(GKInstance instance) {
			// Only clear the empty cells and schema classes if a
			// totally new instance is being imported.
			if (this.instance != instance) {
				emptyCellMap.clear();
				currentSchemaClassMap.clear();
			}
			this.instance = instance;
			refresh();
		}
		
		/**
		 * @return Returns the current instance.
		 */
		public GKInstance getInstance() {
			return instance;
		}
        
        /**
         * Required by the super class AttributeTableModel.
         * @return the SchemaClass object used by the displayed Instance object.
         */
        public SchemaClass getSchemaClass() {
            if (instance == null)
                return null;
            return instance.getSchemClass();
        }
        
        public void setAttributeComparator(Comparator comparator) {
            this.attributeComparator = comparator;
        }
        
        /**
		 * Synchronises table-model-internal data structures
		 * with the most recently imported instance.
		 *
		 */
		public void refresh() {
			valueMap.clear();
			if (instance == null) {
				values = new Object[1][1];
				cellSpan.reset();
				fireTableDataChanged();
				return;
			}
			try {
                // Try to load values first
                if (instance.getDbAdaptor() instanceof MySQLAdaptor) {
                    if (!instance.isInflated()) {
                        inflateInstance();
                    }
                }
				ArrayList schemaAttributes = new ArrayList(instance.getSchemaAttributes());
                 if (attributeComparator == null)
                     attributeComparator = new AttributeAlphabeticalComparator(); // default
				Collections.sort(schemaAttributes, attributeComparator); // do sort first
				keys = new ArrayList();
				String attributeName;
				GKInstance attributeInstance;
				for (Iterator it = schemaAttributes.iterator(); it.hasNext();) {
					GKSchemaAttribute attribute = (GKSchemaAttribute)it.next();
					attributeName = attribute.getName();
					if (hiddenAttNames.contains(attributeName))
						continue;
//					// Don't include attributes that have no possible
//					// relevance to the curator.
//					if (isIrrelevantAttributeName(attributeName))
//						continue;
//					
//					// Don't include uneditable attributes in the table,
//					// if the table is editable.  Otherwise, do include
//					// them.  The rationale is that a non-editable table
//					// is one that the user is simply viewing, so that
//					// the uneditable attributes are also likely to be
//					// interesting.
//					if (isUneditableAttributeName(attributeName) && isEditable())
//						continue;
					
					Collection vals = instance.getAttributeValuesList(attribute);
                    // A special case: use stoichiometry for complex hasComponent.
                    // It might be extended to input and output for Reaction.
                    if (instance.getSchemClass().isa("Complex") && attributeName.equals(ReactomeJavaConstants.hasComponent)) {
                        vals = generateStoichiometries((List)vals);
                    }
					valueMap.put(attributeName, vals);
					keys.add(attributeName); // add here, to make sure things stay in order
					
					// Make a note of the schema class of the value being set,
					// for future reference.
					if (vals!=null) {
						Iterator valsIterator = vals.iterator();
						// just take the first value, if there is more than one.
						if (valsIterator.hasNext()) {
							Object value = valsIterator.next();
							if (value!=null && value instanceof GKInstance) {
								attributeInstance = (GKInstance)value;
								setSchemaClass(attributeName, (GKSchemaClass)attributeInstance.getSchemClass());
							}
						}
					}
					
				}
				setValues(valueMap, emptyCellMap, keys);
				cellSpan.initSpansWithEmptyCells(valueMap, emptyCellMap, keys);
				fireTableDataChanged();
			}
			catch(Exception e) {
				System.err.println("AttributePane.PropertyTableModel.refresh(): " + e);
				e.printStackTrace();
			}
		}

        private void inflateInstance() throws Exception {
            MySQLAdaptor dba = (MySQLAdaptor) instance.getDbAdaptor();
            dba.fastLoadInstanceAttributeValues(instance);
            // Want to make sure all _displayNames have been loaded
            Set unloaded = new HashSet();
            for (Iterator it = instance.getSchemClass().getAttributes().iterator(); it.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) it.next();
                if (!att.isInstanceTypeAttribute())
                    continue;
                List values = instance.getAttributeValuesList(att);
                if (values == null || values.size() == 0)
                    continue;
                for (Iterator it1 = values.iterator(); it1.hasNext();) {
                    GKInstance value = (GKInstance) it1.next();
                    String _displayName = (String) value.getAttributeValueNoCheck(ReactomeJavaConstants._displayName);
                    if (_displayName == null)
                        unloaded.add(value);
                }
            }
            if (unloaded.size() > 0) {
                dba.loadInstanceAttributeValues(unloaded, 
                                                new String[]{ReactomeJavaConstants._displayName});
            }
        }
        
        private List generateStoichiometries(List components) {
            if (components == null)
                return null;
            Map map = new HashMap();
            GKInstance component = null;
            Integer value = null;
            for (Iterator it = components.iterator(); it.hasNext();) {
                component = (GKInstance) it.next();
                value = (Integer) map.get(component);
                if (value == null) 
                    value = new Integer(1);
                else 
                    value = new Integer(value.intValue() + 1);
                map.put(component, value);
            }
            List rtn = new ArrayList();
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
                component = (GKInstance) it.next();
                value = (Integer) map.get(component);
                StoichiometryInstance tmp = new StoichiometryInstance(component, value.intValue());
                rtn.add(tmp);
            }
            // Sort the list based on _displayName
            Collections.sort(rtn);
            return rtn;
        }
		
		/**
		 * Builds up the (global) "values" array.  This maps table row
		 * number to row content.  What makes this complicated is the
		 * fact that some attributes can have multiple values.  One
		 * row is assigned per value.  This means that there is no
		 * one-to-one mapping between "valueMap" and "values".
		 * 
		 * @param valueMap
		 * @param keys
		 */
		private void setValues(Map valueMap, Map emptyCellMap, java.util.List keys) {
			int col = 2;
			int row = 0;
			// Calculate the total row number
			for (Iterator it = valueMap.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				Object value = valueMap.get(key);
				if (value instanceof Collection) {
					Collection c = (Collection)value;
					row += (c.size() == 0 ? 1 : c.size());
				}
				else
					row++;
				
				// Increment row counter if an empty cell is present
				if (emptyCellMap.get(key)!=null)
					row++;
			}
			values = new Object[row][col];
			//row2rank = new int[row];
			// Set the cell spans
			row = 0;
			for (Iterator it = keys.iterator(); it.hasNext();) {
                Object key = it.next();
                Object value = valueMap.get(key);
                if (value instanceof Collection) {
                    Collection c = (Collection) value;
                    int size = c.size() == 0 ? 1 : c.size();
                    values[row][0] = key;
                    int i = 0;
                    // Have to encode line separators
                    for (Iterator it1 = c.iterator(); it1.hasNext();) {
                        values[row + i][1] = it1.next();
                        //row2rank[row + i] = i;
                        i++;
                    }
                    row += size;
                }
                else {
                    values[row][0] = key;
                    values[row][1] = value;
                    //row2rank[row] = 0;
                    row++;
                }
                
                // Put in an empty cell, if one is required
				if (emptyCellMap.get(key)!=null) {
					values[row][1] = null;
					row++;
				}
            }
        }
		
		/**
		 * Gets a list of row numbers corresponding to rows containing
		 * defining attributes.
		 * 
		 * @return
		 */
		public int[] getDefiningRows() {
			ArrayList definingAttributeNames = getDefiningAttributeNames();
			
			if (definingAttributeNames==null)
				return new int[0];
			
			// Loop over each of the defining attributes and check
			// to see if they are in the table; if so, make a note
			// of the corresponding row number.
			ArrayList definingRowList = new ArrayList();
			String attributeName;
			int row;
			Object value, oldValue;
			for (Iterator it = definingAttributeNames.iterator(); it.hasNext();) {
				attributeName = (String)it.next();
				oldValue = null;
				for (row=0;row<values.length;row++) {
					value = values[row][0];
					if (value!=null)
						oldValue = value;
					if (oldValue!=null && oldValue.toString().equals(attributeName))
						definingRowList.add(new Integer(row));
				}
			}
			
			// Copy the row numbers from the ArrayList to an int array.
			int[] definingRows = new int[definingRowList.size()];
			for (int i=0;i<definingRowList.size();i++)
				definingRows[i] = ((Integer)(definingRowList.get(i))).intValue();
			
			return definingRows;
		}
		
		private ArrayList getDefiningAttributeNames() {
			if (instance==null)
				return null;
			
			// Get a list of defining attributes from the instance
			Collection definingAttributes = null;
			try {
				definingAttributes = ((GKSchemaClass)(instance.getSchemClass())).getDefiningAttributes();
			} catch (Exception e) {
				System.err.println("Tsk, something unexpected happened while trying to get the defining attributes");
				e.printStackTrace();
			}
			if (definingAttributes==null)
				return null;
			
			GKSchemaAttribute attribute;
			ArrayList definingAttributeNames = new ArrayList();
			for (Iterator it = definingAttributes.iterator(); it.hasNext();) {
				attribute = (GKSchemaAttribute)it.next();
				definingAttributeNames.add(attribute.getName());
			}
			
			return definingAttributeNames;
		}
		
		public boolean isDefiningAttributeName(String attributeName) {
			ArrayList definingAttributeNames = getDefiningAttributeNames();
			
			if (definingAttributeNames==null)
				return false;
			
			return definingAttributeNames.contains(attributeName);
		}
		
		public int getRowCount() {
			return values.length;
		}

		public int getColumnCount() {
			return headers.length;
		}

		public String getColumnName(int col) {
			return headers[col];
		}

		public Object getValueAt(int row, int col) {
			if (cellSpan.isVisible(row, col)) {
				return values[row][col];
			}
			return null;
		}
		
		/**
		 * Delete a value at the specified row.
		 * @param row
		 */ 
		public Object removeValueAt(int row) {
			// Need to find the key for the value
			Object key = null;
			int i = row;
			for (; i >= 0; i--) {
				key = values[i][0];
				if (key != null)
					break;
			}
			int index = row - i;
			// Update the schema value
			java.util.List list = (java.util.List)valueMap.get(key);
			if (list != null) {
                Object value = list.get(index);
                Object rtn = null;
                if (value instanceof StoichiometryInstance) {
                    list.remove(index);
                    rtn = ((StoichiometryInstance)value).getInstance();
                    try {
                        List valueList = instance.getAttributeValuesList(key.toString());
                        // Remove all subunits
                        int stoi = ((StoichiometryInstance)value).getStoichiometry();
                        for (int j = 0; j < stoi; j++)
                            valueList.remove(rtn);
                    } 
                    catch (Exception e) {
                        System.err.println("AttributePane.removeValueAt(): " + e);
                        e.printStackTrace();
                    }
                }
                else 
                    rtn = list.remove(index);
				if (list.size() == 0) { // Need to empty a slot
                    try {
                        instance.emptyAttributeValues(key.toString());
                    }
                    catch(InvalidAttributeException e) {
                        System.err.println("AttributePane.removeValueAt(): " + e);
                        e.printStackTrace();
                    }
                 }
				refresh(); // Update the whole table. This is a lazy way. It
				           // should be possible to remove or update a single cell 
				           // to improve the performance.
				return rtn;
			}
			return null;
		}

		/**
		 * This method is used to edit a value in the cell.
		 */
		public void setValueAt(Object value, int row, int col) {
		    if (col == 0 || row < 0 || col < 0)
		        return;
		    // Do nothing is the new value is the same as the old one
		    Object oldValue = getValueAt(row, col);
		    if (oldValue == value)
		        return ;
		    if (value != null && oldValue != null && oldValue.equals(value))
		        return;
            if (oldValue instanceof StoichiometryInstance) { // A special case
                GKInstance oldInstance = ((StoichiometryInstance)oldValue).getInstance();
                if (oldInstance == value)
                    return;
            }
		    // Need to find the key for the value
		    Object key = null;
		    int i = row;
		    for (; i >= 0; i--) {
		        key = values[i][0];
		        if (key != null)
		            break;
		    }
		    if (key==null) {
		        System.err.println("AttributePane.PropertyTableModel.setValueAt(): key==null, that should never happen!");
		        return;
		    }
		    if (validator == null)
		        validator = new AttributeEditValidator();
		    if (!validator.validate(instance, 
		                            key.toString(), 
		                            value, 
		                            AttributePane.this))
		        return;
		    // Make sure it is correct value
		    if (value instanceof GKInstance) {
		        List newValues = new ArrayList(1);
		        newValues.add(value);
//		        if (!validator.validateTreeStructure(newValues, 
//		                                             key.toString(),
//		                                             instance,
//		                                             AttributePane.this))
//		            return; // Do nothing. Not correct value!
		        // Make sure inversible slot is handled
		        try {
		            GKSchemaAttribute att = (GKSchemaAttribute) instance.getSchemClass().getAttribute(key.toString());
		            controller.handleInverseAttribute(att, newValues);
		        }
		        catch (InvalidAttributeException e1) {
		            System.err.println("AttributePane.setValueAt(): " + e1);
		            e1.printStackTrace();
		        }
		    }
		    int index = row - i;
		    // Need to update valueMap
		    java.util.List list = (java.util.List)valueMap.get(key); // It is a List!
		    if (controller.shouldStoichiometryInstanceBeUsed(row, col)) {
		        try { 
		            if (value != null) {
		                GKInstance newInstance = (GKInstance) value;
		                StoichiometryInstance newStoiInstance = new StoichiometryInstance(newInstance, 1);
		                StoichiometryInstance oldStoiInstance = (StoichiometryInstance) getValueAt(row, col);
		                List valueList = instance.getAttributeValuesList(key.toString());
		                if (oldStoiInstance != null) {
		                    // Valid instance value
		                    for (int j = 0; j < oldStoiInstance.getStoichiometry(); j++)
		                        valueList.remove(oldStoiInstance.getInstance());
		                    // There is only 1
		                    valueList.add(value);
		                    // Valid display
                            list.set(index, newStoiInstance);
                            values[row][col] = newStoiInstance;
		                }
		                else {
		                    if (list == null) {
		                        list = new ArrayList();
		                        valueMap.put(key, list);
		                    }
		                    list.add(index, newStoiInstance);
                             values[row][col] = newStoiInstance;
		                    instance.addAttributeValue(key.toString(), value);
		                }
                        fireTableCellUpdated(row, col);
		            }
		            else if (list != null){ // More like deletion
		                StoichiometryInstance oldStoiInstance = (StoichiometryInstance) getValueAt(row, col);
                        List valueList = instance.getAttributeValuesList(key.toString());
                        for (int j = 0; j < oldStoiInstance.getStoichiometry(); j++)
                            valueList.remove(oldStoiInstance.getInstance());
                        refresh();
		            }
		        }
		        catch(Exception e) {
		            System.err.println("AttributePane.PropertyTableModel.setValue(): " + e);
                    e.printStackTrace();
		        }
		    }
		    else {
		        if (list != null) {
		            if (value != null) {
		                // A list may be just an empty one
		                if (index > list.size() - 1) {
		                    list.add(value);
		                }
		                else
		                    list.set(index, value);
		                values[row][col] = value;
		                fireTableCellUpdated(row, col);
		            }
		            else {
		                if (index < list.size()) {
		                    list.remove(index);
		                    refresh();
		                }
		            }
		        }
		        else if (value != null) { // Need to update the attribute value
		            list = new ArrayList();
		            list.add(value);
		            valueMap.put(key, list);
		            instance.setAttributeValueNoCheck(key.toString(), list);
		            values[row][col] = value;
		            fireTableCellUpdated(row, col);
		        }
		    }
		    // Need to update the title
		    if (key.toString().equals(ATTRIBUTE_NAME_DISPLAY_NAME)) {
		        updateTitle(instance);
		    }
		    // Special case for database ID
		    if (key.toString().equals(ATTRIBUTE_NAME_DATABASE_ID))
		        instance.setDBID(new Long(value.toString()));
		    
		    // Try adding a new empty cell at this point
		    addEmptyValue(value);
		    
		    // Make a note of the schema class of the value being set,
		    // for future reference.
		    if (value instanceof GKInstance) { // Null checking is not needed. instanceof operator will
		        // take care of this checking.
		        GKInstance attInstance = (GKInstance)value;
		        setSchemaClassAt(row, (GKSchemaClass)attInstance.getSchemClass());
		    }
		    
		    // Only for editable value
		    // Some stuff commented out here, because changes in instance
		    // attributes were not being "noticed" by persistence mechanism.
		    // The danger here is that user changes might not get saved.
		    // Possible sideeffects of this commenting are unknown - I
		    // havn't yet noticed anything.  DC.
		    // TODO: find out sideeffects.
//		    try {
		    
//		    if (!instance.getSchemClass().getAttribute(key.toString()).isInstanceTypeAttribute()) {
		    // Fire edit event
		    AttributeEditEvent e = new AttributeEditEvent(AttributePane.this, instance, key.toString());
		    e.setEditingComponent(AttributePane.this);
		    controller.postProcessAttributeEdit(e);
//		    }
//		    }
//		    catch (InvalidAttributeException e) {
//		    System.err.println("AttributePane.PropertyTableModel(): " + e);
//		    e.printStackTrace();
//		    }
		}
		
		public void setEmptyCell(boolean value, String attName) {
			Boolean objectValue = new Boolean(true);
			if (value)
				emptyCellMap.put(attName, objectValue);
			else
				emptyCellMap.remove(attName);
		}

		public boolean getEmptyCell(String attName) {
			return emptyCellMap.get(attName)!=null;
		}

		/**
		 * Check if the value for the specified row should be an Instance type.
		 */
		public int getValueType(int row) {
			String key = getKeyAt(row);
			try {
				SchemaAttribute att = instance.getSchemClass().getAttribute(key.toString());
				return att.getTypeAsInt();
			}
			catch(InvalidAttributeException e) {
				System.err.println("AttributePane.PropertyTableModel(): " + e);
				e.printStackTrace();
			}
			// The most generic type probably.
			return SchemaAttribute.STRING_TYPE;
		}
		
		public GKSchemaAttribute getAttribute(int row) {
		    String key = getKeyAt(row);
		    try {
		        GKSchemaAttribute att = (GKSchemaAttribute) instance.getSchemClass().getAttribute(key.toString());
		        return att;
		    }
		    catch(InvalidAttributeException e) {
		        System.err.println("AttrinutePane.PropertyTable(): " + e);
		        e.printStackTrace();
		    }
		    return null;
		}
		
		public String getKeyAt(int row) {
			Object key = null;
			for (int i = row; i >= 0; i--) {
				key = values[i][0];
				if (key != null)
					break;
			}
			if (key==null)
				return null;
			return key.toString();
		}
		
		public GKSchemaClass getSchemaClassAt(int row) {
			return (GKSchemaClass)currentSchemaClassMap.get(getKeyAt(row));
		}
		
		public void setSchemaClassAt(int row, GKSchemaClass schemaClass) {
			setSchemaClass(getKeyAt(row), schemaClass);
		}
		
		public void setSchemaClass(String attName, GKSchemaClass schemaClass) {
			currentSchemaClassMap.put(attName, schemaClass);
		}
		
		public int getLastRowForAttributeAt(int row) {
			// Search down
			Object key = null;
			for (int i = row + 1; i < values.length; i++) {
				key = values[i][0];
				if (key != null)
					return i - 1;
			}
			return row;
		}
	}
	
	class ShellPane extends JPanel {
		private JButton viewInDBBtn;
		private JButton downloadBtn;
		
		public ShellPane() {
			super();
			init();
		}
		
		private void init() {
			setLayout(new BorderLayout());
			JTextArea ta = new JTextArea();
			ta.setEditable(false);
			ta.setBackground(getBackground());
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			ta.setText("This is a shell instance that is not editable. Click \"View in DB\" " +				"to view its attributes in the database, or \"Download\" to download and edit it:");
			add(ta, BorderLayout.NORTH);
			// Buttons
			JPanel btnPane = new JPanel();
			btnPane.setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(8, 4, 8, 4);
			viewInDBBtn = new JButton("View in DB");
			btnPane.add(viewInDBBtn, constraints);
			downloadBtn = new JButton("Download");
			downloadBtn.setPreferredSize(viewInDBBtn.getPreferredSize());
			constraints.gridx = 1;
			btnPane.add(downloadBtn, constraints);
			add(btnPane, BorderLayout.CENTER);
		}
	}
	
	/**
	 * To enable DnD in the table for changing the order of the values.
	 */
	class ListValueTransferHandler extends TransferHandler {
		private DataFlavor serialArrayListFlavor;
		private DataFlavor bookmarksFlavor;
		private String selectedAtt;
		
		public ListValueTransferHandler() {
			serialArrayListFlavor = new DataFlavor(ArrayList.class,
											       "ArrayList");
			bookmarksFlavor = new DataFlavor(Bookmarks.class, "Bookmarks");
		}
		
		private boolean hasSerialArrayListFlavor(DataFlavor[] flavors) {
			if (serialArrayListFlavor == null) {
				return false;
			}

			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].equals(serialArrayListFlavor)) {
					return true;
				}
			}
			return false;
		}
		
		private boolean hasBookmarksFlavor(DataFlavor[] flavors) {
		    for (int i = 0; i < flavors.length; i++) {
		        if (flavors[i].equals(bookmarksFlavor)) {
		            return true;
		        }
		    }
		    return false;
		}
	
		public boolean canImport(JComponent c, DataFlavor[] flavors) {
		    if (!isEditable)
				return false;
		    JTable table = (JTable) c;
		    int col = table.getSelectedColumn();
		    if (col != 1) // Only value column can be edited
		        return false; 
		    int row = table.getSelectedRow();
		    if (row == -1)
		        return false;
		    PropertyTableModel model = (PropertyTableModel) table.getModel();
		    String attName = model.getKeyAt(row);
		    if (uneditableAttNames.contains(attName))
		        return false;
			if (hasSerialArrayListFlavor(flavors)) { 
				if (attName != selectedAtt)
					return false;
				return true; 
			}
			else if (hasBookmarksFlavor(flavors)) {
			    try {
                    SchemaAttribute att = instance.getSchemClass().getAttribute(attName);
                    // Only instance type attribute can accept bookmark value
                    if (att.isInstanceTypeAttribute())
                        return true;
                }
                catch (InvalidAttributeException e) {
                    System.err.println("AttributePane.canImport(): " + e);
                    e.printStackTrace();
                }
			}
			return false;
		}

		public boolean importData(JComponent c, Transferable t) {
		    ArrayList aList = null;
			if (!canImport(c, t.getTransferDataFlavors())) {
				return false;
			}
			try {
				if (hasSerialArrayListFlavor(t.getTransferDataFlavors())) {
				    aList = (ArrayList)t.getTransferData(serialArrayListFlavor);
				    if (aList != null && aList.size() > 1) {
						String attName = (String) aList.get(0);
						int valueIndex = ((Integer)aList.get(1)).intValue();
						int row = propTable.getSelectedRow();
						Object anchorValue = propTable.getValueAt(row, 1);
						controller.reorder(attName, valueIndex, anchorValue);
				    }
				    return true;
				} 
				else if (hasBookmarksFlavor(t.getTransferDataFlavors())){
					List bookmarks = (ArrayList) t.getTransferData(bookmarksFlavor);
					List instances = convertBookmarksToInstance(bookmarks);
					if (instances.size() > 0) {
					    int selectedRow = propTable.getSelectedRow();
					    int selectedCol = propTable.getSelectedColumn();
						PropertyTableModel model = (PropertyTableModel) propTable.getModel();
						String attName = model.getKeyAt(selectedRow);
						SchemaAttribute att = instance.getSchemClass().getAttribute(attName);
						Object value = model.getValueAt(selectedRow, selectedCol);
						int index = getIndex(attName, value);
						boolean needRefresh = false;
						for (Iterator it = instances.iterator(); it.hasNext();) {
						    GKInstance instance = (GKInstance) it.next();
						    needRefresh = controller.insertValue(att, instance, index);
						}
						if (needRefresh)
						    refresh();
					}
				    return true;
				}
				else
				    return false;
			} 
			catch (Exception e) {
				System.err.println("AttributePane.importData(): " + e);
				e.printStackTrace();
				return false;
			} 
		}
		
		private List convertBookmarksToInstance(List bookmarks) {
		    List instances = new ArrayList(bookmarks.size());
		    try {
		        Bookmark bookmark = null;
		        GKInstance instance = null;
		        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
		        for (Iterator it = bookmarks.iterator(); it.hasNext();) {
		            bookmark = (Bookmark) it.next();
		            instance = fileAdaptor.fetchInstance(bookmark.getType(), bookmark.getDbID());
		            if (instance != null)
		                instances.add(instance);
		        }
		    }
		    catch(Exception e) {
		        System.err.println("AttributePane.convertBookmarksToInstance(): " + e);
		        e.printStackTrace();
		    }
		    return instances;
		}
		
		protected Transferable createTransferable(JComponent c) {
			JTable table = (JTable) c;
			int col = table.getSelectedColumn();
			if (col == 0)
				return null;
			int row = table.getSelectedRow();
			PropertyTableModel model = (PropertyTableModel) table.getModel();
			String attName = model.getKeyAt(row);
			Object value = model.getValueAt(row, col);
			if (value == null)
				return null;
			int index = getIndex(attName, value);
			ArrayList list = new ArrayList(2);
			list.add(attName);
			list.add(new Integer(index));
			this.selectedAtt = attName;
			return new ArrayListTransferable(list);
		}
		
		/**
		 * Only move is allowed for DnD.
		 */
		public int getSourceActions(JComponent c) {
			return MOVE; 
		}
		
		protected void exportDone(JComponent source, Transferable data, int action) {
			if (action == MOVE) {
				refresh();
			}
		}
		
		private int getIndex(String attName, Object value) {
			try {
				java.util.List list = instance.getAttributeValuesList(attName);
				if ((list == null || list.size() == 0) && (value == null))
				    return 0; // Assume the first one
				return list.indexOf(value);
			}
			catch(Exception e) {
				System.err.println("AttributePane.getIndex(): " + e);
				e.printStackTrace();
				return -1;
			}
		}
	}
    
    /**
     * Used for comparing pairs of attributes, to determine
     * their ordering in a sort.
     * 
     * @author croft
     *
     */
    private class AttributeAlphabeticalComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            GKSchemaAttribute attribute1 = null;
            GKSchemaAttribute attribute2 = null;
            try {
                attribute1 = (GKSchemaAttribute)o1;
                attribute2 = (GKSchemaAttribute)o2;
            } catch (ClassCastException e) {
                System.err.println("One of the objects for comparison does not have type GKSchemaAttribute");
                e.printStackTrace();
                return 0;
            }
            
            String attributeName1 = attribute1.getName();
            String attributeName2 = attribute2.getName();
            // isdefiningAttributeName(String) is a length operation. Use this simple way.
            //boolean isDefiningAttribute1 = isDefiningAttributeName(attributeName1);
            //boolean isDefiningAttribute2 = isDefiningAttributeName(attributeName2);
            boolean isDefiningAttribute1 = false;
             if (attribute1.getDefiningType() == SchemaAttribute.ALL_DEFINING || 
                 attribute1.getDefiningType() == SchemaAttribute.ANY_DEFINING)
                 isDefiningAttribute1 = true;
             boolean isDefiningAttribute2 = false;
             if (attribute2.getDefiningType() == SchemaAttribute.ALL_DEFINING ||
                 attribute2.getDefiningType() == SchemaAttribute.ANY_DEFINING)
                 isDefiningAttribute2 = true;
            
            // If one of the two attributes is defining and the other
            // is not, then give preference to the one that is defining.
            // If both are defining or neither are defining, then "fall
            // through" to the next comparison operation.
            if (isDefiningAttribute1 && !isDefiningAttribute2)
                return (-1);
            if (!isDefiningAttribute1 && isDefiningAttribute2)
                return 1;
            
            // Do a simple lexicographic comparison of the attribute
            // names.
            return attributeName1.compareTo(attributeName2);
        }
    }

    /**
     * This Compartor is used to group attributes in the same category together and then sort them 
     * based on attribute names. The order of the categories is: Mandatory, Required, Optional and
     * NO_Manual_Edit.
     * @author guanming
     *
     */
    private class AttributeCategoryGroupingComparator implements Comparator {
        public int compare(Object obj1, Object obj2) {
            // This class will be used for attributes only
            SchemaAttribute att1 = (SchemaAttribute) obj1;
            SchemaAttribute att2 = (SchemaAttribute) obj2;
            
            String attributeName1 = att1.getName();
            String attributeName2 = att2.getName();
            
            // Categories sort as following: Mandantory, Required, Optional, NoManualEdit.
            // The integer values defined in class SchemaAttribute can be used for sorting.
            // But they may be not reliable and should NOT be assumed. So a local value is
            // used
            int category1 = att1.getCategory();
            category1 = getLocalCategory(category1);
            int category2 = att2.getCategory();
            category2 = getLocalCategory(category2);
            int comp = category1 - category2;
            if (comp == 0)
                return attributeName1.compareTo(attributeName2);
            else
                return comp;
        }
        
        private int getLocalCategory(int category) {
            switch(category) {
                case SchemaAttribute.MANDATORY :
                    return 1;
                case SchemaAttribute.REQUIRED :
                    return 2;
                case SchemaAttribute.OPTIONAL :
                    return 3; 
                case SchemaAttribute.NOMANUALEDIT :
                    return 4;
                default : // Default as the last one to be listed as the end.
                    return 5;
            }
        }
    }
}
