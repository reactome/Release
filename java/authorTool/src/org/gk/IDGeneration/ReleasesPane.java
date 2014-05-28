/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;

import org.gk.persistence.MySQLAdaptor;

/** 
 *  Allows the user to select previous and current release from a
 *  list of known releases.  Also allows the user to add, delete
 *  or modify releases.  These changes are directly reflected as
 *  changes to the identifier database.
 *  
 *  Other classes can get the user-selected previous and current
 *  releases via the methods:
 *  
 *  getPreviousRelease()
 *  getCurrentRelease()
 *  
 * @author croft
 */
public class ReleasesPane extends JPanel {
	private ReleasesController controller;
	private IdentifierDatabase identifierDatabase = null;
	private JTable releasesTable;
	private JButton addBtn;
	private JButton delBtn;
	private JButton sliceDbparamsBtn;
	private JButton releaseDbparamsBtn;
	private JButton copyDbparamsBtn;
	private JButton editBtn;
	private IDGenerationPane iDGenerationPane;
	private ReleasePanel newReleasePanel;
	private ReleasePanel previousReleasePanel;
	private ReleasePanel currentReleasePanel;
	
	public ReleasesPane(IDGenerationPane iDGenerationPane) {
		init();
		this.iDGenerationPane = iDGenerationPane;
	}
	
	private void init() {
		controller = new ReleasesController(this);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		JTextField dummyTextField = new JTextField(5);
		int minimumTextHeight = dummyTextField.getMinimumSize().height;
		
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel titleLabel = new JLabel("Releases");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		titlePanel.add(titleLabel);
		titlePanel.setMaximumSize(new Dimension(titlePanel.getMaximumSize().width, minimumTextHeight));
		add(titlePanel);

		JPanel listButtonPanel = new JPanel();
		listButtonPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		addBtn = new JButton(controller.getAddReleaseAction());
		listButtonPanel.add(addBtn);
		delBtn = new JButton(controller.getDeleteReleaseAction());
		listButtonPanel.add(delBtn);
		sliceDbparamsBtn = new JButton(controller.getSliceDbParamsReleaseAction());
		listButtonPanel.add(sliceDbparamsBtn);
		releaseDbparamsBtn = new JButton(controller.getReleaseDbParamsReleaseAction());
		listButtonPanel.add(releaseDbparamsBtn);
		copyDbparamsBtn = new JButton(controller.getCopyDbParamsReleaseAction());
		listButtonPanel.add(copyDbparamsBtn);
		editBtn = new JButton(controller.getEditReleaseAction());
		listButtonPanel.add(editBtn);
		listButtonPanel.setMaximumSize(new Dimension(listButtonPanel.getMaximumSize().width, minimumTextHeight));
		add(listButtonPanel);
		
		releasesTable = new JTable();
		refreshTable();
		releasesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		releasesTable.setDefaultEditor(String.class, new ReleaseCellEditor(new JTextField()));
		JScrollPane releasesScrollPane = new JScrollPane(releasesTable);
		add(releasesScrollPane);
				
		previousReleasePanel = new ReleasePanel("Previous", minimumTextHeight, IDGenerationPersistenceManagers.PREVIOUS_MANAGER);
		previousReleasePanel.getSelectBtn().addActionListener(controller.getPreviousReleaseAction());
		previousReleasePanel.getClearBtn().addActionListener(controller.getPreviousClearReleaseAction());
		add(previousReleasePanel);
		
		currentReleasePanel = new ReleasePanel("Current", minimumTextHeight, IDGenerationPersistenceManagers.CURRENT_MANAGER);
		currentReleasePanel.getSelectBtn().addActionListener(controller.getCurrentReleaseAction());
		currentReleasePanel.getClearBtn().addActionListener(controller.getCurrentClearReleaseAction());
		add(currentReleasePanel);
		
		setPreferredSize(new Dimension(190, getPreferredSize().height));
	}
	
	public void refreshTable() {
		controller.refreshModel();
		ReleasesModel releasesModel = controller.getModel();
		releasesTable.setModel(releasesModel);
	}
	
	public ReleasesController getController() {
		return controller;
	}
	
	public JTable getReleasesTable() {
		return releasesTable;
	}

	public IDGenerationPane getIDGenerationPane() {
		return iDGenerationPane;
	}
	
	public ReleasePanel getNewReleasePanel() {
		return newReleasePanel;
	}
	
	public ReleasePanel getPreviousReleasePanel() {
		return previousReleasePanel;
	}
	
	public ReleasePanel getCurrentReleasePanel() {
		return currentReleasePanel;
	}
	
	/**
	 * Returns true if there is something in the current release
	 * text field.
	 * 
	 * @return
	 */
	public boolean isCurrentReleaseSelected() {
		String currentRelease = currentReleasePanel.getRelease();
		
		return currentRelease!=null && currentRelease.length()>0;
	}
	
	public class ReleasePanel extends JPanel {
		private String text;
		private int minimumTextHeight;
		private String releaseDatabaseManagerName;
		private JTextField releaseText;
		private int TEXT_FIELD_LENGTH = 15;
		private JButton selectBtn;
		private JButton clearBtn;
		private Action clearAction = null;
		
		public ReleasePanel(String text, int minimumTextHeight, String releaseDatabaseManagerName) {
			this.text = text;
			this.minimumTextHeight = minimumTextHeight;
			this.releaseDatabaseManagerName = releaseDatabaseManagerName;
			init();
		}
		
		private void init() {
			setLayout(new FlowLayout(FlowLayout.LEADING));
			setBorder(new LineBorder(Color.BLACK));
			setMaximumSize(new Dimension(getMaximumSize().width, minimumTextHeight));
			
			// Title
			int textFieldLength = text.length();
			// Truncate
			if (textFieldLength>TEXT_FIELD_LENGTH)
				text = text.substring(0, TEXT_FIELD_LENGTH-1);
			// Pad
			if (textFieldLength<TEXT_FIELD_LENGTH) {
				int diff = TEXT_FIELD_LENGTH - textFieldLength;
				int i;
				for (i=0; i<diff; i++)
					text += " ";
			}
			JLabel currentLabel = new JLabel(text);
			add(currentLabel);
			
			// Select button
			selectBtn = new JButton("Select");
			add(selectBtn);
			
			// Clear button
			clearBtn = new JButton(getClearAction());
			add(clearBtn);
			
			// Release num display
			releaseText = new JTextField(5);
			if (releaseDatabaseManagerName!=null) {
				MySQLAdaptor dba = IDGenerationPersistenceManagers.getManager().getDatabaseAdaptor(releaseDatabaseManagerName);
				String releaseNum = null;
				try {
					if (identifierDatabase==null)
						identifierDatabase = getIDGenerationPane().getFrame().getController().getIdentifierDatabase();
					releaseNum = identifierDatabase.getReleaseNumFromReleaseDba(dba);
				} catch (NullPointerException e) {
				}
				if (releaseNum!=null)
					// Set default initial release num
					releaseText.setText(releaseNum);
			}
			add(releaseText);
		}
		
		public String getRelease() {
			return releaseText.getText();
		}

		public void setRelease(String releaseNum) {
			releaseText.setText(releaseNum);
		}

		public JButton getSelectBtn() {
			return selectBtn;
		}
		
		public JButton getClearBtn() {
			return clearBtn;
		}
		
		private Action getClearAction() {
	    	if (clearAction == null) {
	    		clearAction = new AbstractAction("Clear") {
	    			public void actionPerformed(ActionEvent e) {
	    				releaseText.setText("");
	    			}
	    		};
	    	}
	    	return clearAction;
		}
	}
	
	/**
	 * Provides a JTextField for editing the release and
	 * date fields.
	 * 
	 * @author croft
	 *
	 */
	class ReleaseCellEditor extends DefaultCellEditor {
		// Sets the number of clicks needed to start editing a cell
		protected int DEFAULT_CLICK_COUNT = 2;

		private Object editingValue;
		private int editingRow;
		private JTextField tf; // For other values

		/**
		 * @param textField is ignored, use null.
		 */
		public ReleaseCellEditor(JTextField textField) {
			super(textField);
			propertyCellEditorInit();
		}
		
		private void propertyCellEditorInit() {
			tf = new JTextField();
			// Force all pended edting to commit
			tf.addFocusListener(new FocusAdapter() {
			    public void focusLost(FocusEvent e) {
			        stopCellEditing();
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
			this.editingRow = row;
			String text = null;
			tf.setFont(table.getFont());
			if (value != null) {
				text = value.toString();
				tf.setText(text);
			}
			tf.requestFocus();
			return tf;
		}
		
		/**
		 * Pulls a value from the current cell editor component.
		 */
		public Object getCellEditorValue() {
			String str = tf.getText();
			if (str == null || str.length() == 0)
				return null;
			return str;
		}
		
		public JComponent getCurrentComponent() {
			return tf;
		}
		
		/**
		 * Validte the input data if necessary.
		 */
		public boolean stopCellEditing() {
			fireEditingStopped();
			controller.getModel().updateRelease(editingRow);
			return true;
		}
	}
}