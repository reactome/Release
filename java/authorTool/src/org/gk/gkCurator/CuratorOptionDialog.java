/*
 * Created on Jan 21, 2004
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.database.AttributeEditConfig;
import org.gk.database.FrameManager;
import org.gk.database.SynchronizationManager;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.PersistenceManager;
import org.gk.util.GKApplicationUtilities;

/**
 * A customized JDialog to set up the options for the curator tool.
 * @author wugm
 */
public class CuratorOptionDialog extends JDialog {
	// GUIs
    private JCheckBox elvDrawingToolBox;
	private JComboBox lfBox;
    private JCheckBox groupAttByCategoriesBox;
	private DBConnectionPane dbPane;
	private AttributeEditOptionPane editOptionPane;
	private JTabbedPane tabbedPane;
    private JSpinner minSpinner;
    private JCheckBox autoSaveBox;
//    private JCheckBox showReactionInTopBox;
    private JLabel minutesLabel;
	private GKCuratorFrame curatorFrame;
	// For Look and Feel
	// A list of LookAndFeel
	private final String[] lfNames = new String[]{"aqua", "metal",
												  "whistler", "windows",
												  "xpluna"};
	private boolean isLFChanged = false;
    // Check if autosave is changed
    private boolean isAutoSaveChanged = false;
    // Check if evl option is changed
    private boolean isElvDrawingToolBoxChanged = false;

	public CuratorOptionDialog(GKCuratorFrame frame) {
		super(frame);
		curatorFrame = frame;
		init();
	}
	
	private void setProperties(Properties prop) {
	    dbPane.setValues(prop);
	    // For look and feel
	    String lfName = prop.getProperty("lookAndFeel");
	    if (lfName == null || lfName.length() == 0)
	        lfBox.setSelectedItem(GKApplicationUtilities.getDefaultLF());
	    else
	        lfBox.setSelectedItem(lfName);
	    boolean isAttGrouped = AttributeEditConfig.getConfig().isGroupAttributesByCategories();
	    groupAttByCategoriesBox.setSelected(isAttGrouped);
        String autoSave = prop.getProperty("autoSave");
        // temp disable statechange listener
        ChangeListener l = autoSaveBox.getChangeListeners()[0];
        autoSaveBox.removeChangeListener(l);
        minSpinner.removeChangeListener(l);
        if (autoSave != null && autoSave.equals("false"))
            autoSaveBox.setSelected(false);
        else {
            autoSaveBox.setSelected(true);
        }
        String autoSaveMin = prop.getProperty("autoSaveMin");
        if (autoSaveMin != null)
            minSpinner.setValue(new Integer(autoSaveMin));
        else
            minSpinner.setValue(new Integer(10));
        minSpinner.setEnabled(autoSaveBox.isSelected());
        minutesLabel.setEnabled(autoSaveBox.isSelected());
        autoSaveBox.addChangeListener(l);
        minSpinner.addChangeListener(l);
        editOptionPane.initValues(prop);
        elvDrawingToolBox.setSelected(curatorFrame.getEntityLevelView().isUsedAsDrawingTool());
	}
	
	private void init() {
		JPanel generalPane = createGeneralPane();
		tabbedPane = new JTabbedPane();
		tabbedPane.add(generalPane, "General");
//		JPanel userInfoPane = createUserInfoPane();
//		tabbedPane.add(userInfoPane, "User Info");
		editOptionPane = new AttributeEditOptionPane();
		tabbedPane.add(editOptionPane, "Attribute Edit");
		dbPane = new DBConnectionPane();
		tabbedPane.add(dbPane, "Database Info");
		// Add a control Pane
		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
		});
		okBtn.setMnemonic('O');
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancelAction();
			}
		});
		cancelBtn.setMnemonic('C');
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		southPane.add(okBtn);
		southPane.add(cancelBtn);
		if (curatorFrame != null)
			setProperties(curatorFrame.getSystemProperties());		
		getContentPane().add(southPane, BorderLayout.SOUTH);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		setSize(400, 415);
		//GKApplicationUtilities.center(this);
		setLocationRelativeTo(getOwner());
		setTitle("Options for Reactome Curator Tool");
		setModal(true);
	}

	private void cancelAction() {
		Properties prop = curatorFrame.getSystemProperties();
		if (isLFChanged) {
			String lfName = prop.getProperty("lookAndFeel");
			String newLFName = (String) lfBox.getSelectedItem();
			if (!lfName.equals(newLFName))
				doLFChange(lfName);
		}
		dispose();
	}

	private void okAction() {
	    Properties prop = curatorFrame.getSystemProperties();
	    PersistenceManager.ConnectInfo oldConnectInfo = new PersistenceManager.ConnectInfo(prop);
	    if(!dbPane.commitForTab())
	        return;
	    PersistenceManager.ConnectInfo newConnectInfo = new PersistenceManager.ConnectInfo(prop);
	    if (PersistenceManager.getManager().getActiveMySQLAdaptor() != null &&
	        !newConnectInfo.equals(oldConnectInfo)) {
	        // Need to reset the active MySQLAdaptor
	        FrameManager.getManager().closeBrowser();
	        PersistenceManager.getManager().setActiveMySQLAdaptor(null);
	        SynchronizationManager.getManager().refresh();
	        //PersistenceManager.getManager().initMySQLAdaptor(this);	
	    }	
	    prop.setProperty("lookAndFeel", (String)lfBox.getSelectedItem());
	    // Check attribute edit pane
	    if (editOptionPane.isChanged()) {
	        editOptionPane.commit(prop);
            AttributeEditConfig.getConfig().loadProperties(prop);
	    }
	    // Attribute sort mode
        boolean isGroupAttByCategories = groupAttByCategoriesBox.isSelected();
        AttributeEditConfig.getConfig().setGroupAttributesByCategories(isGroupAttByCategories);
	    // Autosave setting
        if (isAutoSaveChanged) {
            // Need to commit changes
            // autosave
            prop.setProperty("autoSave", autoSaveBox.isSelected() + "");
            prop.setProperty("autoSaveMin", minSpinner.getValue() + "");
            curatorFrame.autoSaveChanged();
        }
        if (isElvDrawingToolBoxChanged) {
            prop.setProperty("useELVasDrawingTool", elvDrawingToolBox.isSelected() + "");
            curatorFrame.getEntityLevelView().setUseAsDrawingTool(elvDrawingToolBox.isSelected());
        }
        dispose();
	    // Show restart info
//	    if (defaultPersonChanged || needRestart) {
//	    int reply = JOptionPane.showConfirmDialog(curatorFrame,
//	    "You have to restart the application to make changes effect.\n" +
//	    "Do you want to restart now?",
//	    "Restar?",
//	    JOptionPane.YES_NO_OPTION);
//	    if (reply == JOptionPane.YES_OPTION) {
//	    if (defaultPersonChanged)
//	    curatorFrame.setSaveDefaultPerson(false);
//	    GKCuratorFrame.restart();
//	    }
//	    }
	}
	
	private void doLFChange(String newLF) {
		GKApplicationUtilities.setLookAndFeel(newLF);
		SwingUtilities.updateComponentTreeUI(curatorFrame);
		SwingUtilities.updateComponentTreeUI(this);
		FrameManager.getManager().updateUI();
	}
	
	private JPanel createGeneralPane() {
	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
	    panel.setLayout(new GridBagLayout());
	    GridBagConstraints constraints = new GridBagConstraints();
	    constraints.insets = new Insets(4, 4, 4, 4);
	    constraints.anchor = GridBagConstraints.WEST;
	    // To control L&F
	    JLabel lfLabel = new JLabel("Look and Feel:");
	    panel.add(lfLabel, constraints);
	    lfBox = new JComboBox();
	    initLFBox(lfBox);
	    lfBox.addItemListener(new ItemListener() {
	        public void itemStateChanged(ItemEvent e) {
	            if (e.getStateChange() == ItemEvent.SELECTED) {
	                doLFChange((String)lfBox.getSelectedItem());
	                isLFChanged = true;
	            }
	        }
	    });
	    constraints.gridx = 1;
	    constraints.gridwidth = 2;
	    constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
	    panel.add(lfBox, constraints);
	    // To control if the ELV is used only as a drawing tool. If the ELV is used
	    // as a drawing tool only, no editing will be done except for drawing
	    elvDrawingToolBox  = new JCheckBox("Use ELV as a drawing tool");
	    constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        panel.add(elvDrawingToolBox, constraints);
        elvDrawingToolBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                isElvDrawingToolBoxChanged = true;
            }
        });
        // To control the general setting for sorting attributes in all places.
	    // Some AttributePanes can have sort actions themselves. The setting in
	    // these Objects cannot be applied to other places and cannot be persisted
	    // between sessions.
	    groupAttByCategoriesBox = new JCheckBox("Group Attributes by Categories");
        constraints.gridx = 0;
	    constraints.gridy = 2;
	    constraints.gridwidth = 3;
	    panel.add(groupAttByCategoriesBox, constraints);
	    // To control if autosave should be enabled. The default is enabled. This may
        // impact the performance a little bit.
        autoSaveBox = new JCheckBox("Auto-save the project into a temp file every: ");
        SpinnerNumberModel model = new SpinnerNumberModel(10,
                                                          1,
                                                          60,
                                                          1);
        minSpinner = new JSpinner(model);
        minutesLabel = new JLabel("minutes");
        ChangeListener stateChangeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                isAutoSaveChanged = true;
                if (e.getSource() == autoSaveBox) {
                    minSpinner.setEnabled(autoSaveBox.isSelected());
                    minutesLabel.setEnabled(autoSaveBox.isSelected());
                }
            }
        };
        autoSaveBox.addChangeListener(stateChangeListener);
        minSpinner.addChangeListener(stateChangeListener);
        JTextField tf = ((JSpinner.DefaultEditor)minSpinner.getEditor()).getTextField();
        Color bg = tf.getBackground();
        tf.setEditable(false);
        tf.setBackground(bg); // Want to use the original background
        constraints.gridy = 3;
        panel.add(autoSaveBox, constraints);
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        panel.add(minSpinner, constraints);
        constraints.gridx = 2;
        panel.add(minutesLabel, constraints);
//        showReactionInTopBox = new JCheckBox("Hide Reactions in Top-level Event Hierarchy");
//        constraints.gridx = 0;
//        constraints.gridy = 5;
//        constraints.gridwidth = 3;
//        panel.add(showReactionInTopBox, constraints);
        return panel;
	}
	
	private void initLFBox(JComboBox lfBox) {
		String osName = System.getProperty("os.name").toLowerCase();
		boolean isWindows = (osName.indexOf("win") > - 1) ? true : false;
		for (int i = 0; i < lfNames.length; i++) {
			if (i == 3 && !isWindows)
				continue;
			lfBox.addItem(lfNames[i]);
		}
	}
	
	class AttributeEditOptionPane extends JPanel {
		// A flag to indicate something has been changed
		private boolean isChanged = false;
		// GUIs whose states are cared for
		private JCheckBox eventSpeciesBox;
		private JCheckBox eventDrBox;
		private JCheckBox reactionCompartmentBox;
		private JCheckBox complexSpeciesBox;
		private JCheckBox complexCompartmentBox;
		private JCheckBox allowComboBoxEditorBox;
        private JCheckBox eventAuthoredBox;
        private JCheckBox eventReviewedBox;

		public AttributeEditOptionPane() {
			init();
		}
		
		private void init() {
			setLayout(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.anchor = GridBagConstraints.WEST;
			JLabel label = new JLabel("Check to enable setting propagation:");
			Font labelFont = label.getFont().deriveFont(Font.BOLD);
			label.setFont(labelFont);
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.gridwidth = 3;
			add(label, constraints);
			JLabel eventLabel = new JLabel("Event: ");
			constraints.gridy = 1;
			constraints.gridwidth = 1;
			constraints.insets = new Insets(4, 4, 0, 4);
			add(eventLabel, constraints);
             eventAuthoredBox = new JCheckBox(ReactomeJavaConstants.authored);
             constraints.gridx = 1;
             add(eventAuthoredBox, constraints);
             eventReviewedBox = new JCheckBox(ReactomeJavaConstants.reviewed);
             constraints.gridx = 1;
             constraints.gridy = 2;
             add(eventReviewedBox, constraints);
			eventSpeciesBox = new JCheckBox(ReactomeJavaConstants.species);
			constraints.gridx = 2;
             constraints.gridy = 1;
			add(eventSpeciesBox, constraints);
			eventDrBox = new JCheckBox(ReactomeJavaConstants._doRelease);
			constraints.gridy = 2;
			constraints.insets = new Insets(0, 4, 4, 4);
			add(eventDrBox, constraints);
			JLabel reactionLabel = new JLabel("Reaction: ");
			constraints.gridx = 0;
			constraints.gridy = 3;
             constraints.insets = new Insets(4, 4, 4, 4);
			add(reactionLabel, constraints);
			reactionCompartmentBox = new JCheckBox(ReactomeJavaConstants.compartment);
			constraints.gridx = 1;
            constraints.gridwidth = 2;
            add(reactionCompartmentBox, constraints);
			JLabel complexLabel = new JLabel("Complex: ");
			constraints.gridx = 0;
			constraints.gridy = 4;
            constraints.gridwidth = 1;
			constraints.insets = new Insets(4, 4, 0, 4);
			add(complexLabel, constraints);
			complexSpeciesBox = new JCheckBox(ReactomeJavaConstants.species);
			constraints.gridx = 1;
             constraints.gridwidth = 2;
			add(complexSpeciesBox, constraints);
			complexCompartmentBox = new JCheckBox(ReactomeJavaConstants.compartment);
			constraints.gridy = 5;
			constraints.insets = new Insets(0, 4, 4, 4);
			add(complexCompartmentBox, constraints);
			
			// Add a section for attribute editing
			JLabel myLabel = new JLabel("Attribute editing properties:");
			myLabel.setFont(labelFont);
			constraints.gridx = 0;
			constraints.gridy = 6;
			constraints.gridwidth = 3;
			constraints.insets = new Insets(12, 4, 4, 4);
			add(myLabel, constraints);
			JLabel allowComboBoxEditorLabel = new JLabel("Allow combo box editor: ");
			constraints.gridy = 7;
			constraints.gridwidth = 2;
             constraints.insets = new Insets(4, 4, 4, 4);
			add(allowComboBoxEditorLabel, constraints);
			allowComboBoxEditorBox = new JCheckBox("");
			constraints.gridx = 2;
			add(allowComboBoxEditorBox, constraints);
			
			// Set the flag if any box is clicked
			ActionListener l = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isChanged = true;
				}
			};
			eventSpeciesBox.addActionListener(l);
			eventDrBox.addActionListener(l);
			eventAuthoredBox.addActionListener(l);
             eventReviewedBox.addActionListener(l);
             reactionCompartmentBox.addActionListener(l);
			complexSpeciesBox.addActionListener(l);
			complexCompartmentBox.addActionListener(l);
			allowComboBoxEditorBox.addActionListener(l);
             // All should be selected as default
			eventSpeciesBox.setSelected(true);
			eventDrBox.setSelected(true);
             eventAuthoredBox.setSelected(true);
             eventReviewedBox.setSelected(true);
			reactionCompartmentBox.setSelected(true);
			complexSpeciesBox.setSelected(true);
			complexCompartmentBox.setSelected(true);
			allowComboBoxEditorBox.setSelected(true);
		}
		
		public void initValues(Properties prop) {
		    String value = prop.getProperty(AttributeEditConfig.DISABLE_AUTO_SETTING);
		    if (value != null && value.length() > 0) {
		        StringTokenizer tokenizer = new StringTokenizer(value, ", ");
		        String token = null;
		        while (tokenizer.hasMoreTokens()) {
		            token = tokenizer.nextToken();
		            if (token.equals("Event." + ReactomeJavaConstants.species))
		                eventSpeciesBox.setSelected(false);
		            else if (token.equals("Event." + ReactomeJavaConstants._doRelease))
		                eventDrBox.setSelected(false);
		            else if (token.equals("Reaction." + ReactomeJavaConstants.compartment))
		                reactionCompartmentBox.setSelected(false);
		            else if (token.equals("Complex." + ReactomeJavaConstants.species))
		                complexSpeciesBox.setSelected(false);
		            else if (token.equals("Complex." + ReactomeJavaConstants.compartment))
		                complexCompartmentBox.setSelected(false);
                    else if (token.equals("Event." + ReactomeJavaConstants.authored))
                        eventAuthoredBox.setSelected(false);
                    else if (token.equals("Event." + ReactomeJavaConstants.reviewed))
                        eventReviewedBox.setSelected(false);
		        }
		    }
            // AttributeEditConfig should be used as a singleton. No need to check is null
			//if (AttributeEditConfig.getConfig()!=null)
		   allowComboBoxEditorBox.setSelected(AttributeEditConfig.getConfig().isAllowComboBoxEditor());
			//else {
			//	String setting = prop.getProperty("AttributeEdit.allowComboBoxEditor");
				//if (setting==null)
				//	allowComboBoxEditorBox.setSelected(false);			
				//else
				//	allowComboBoxEditorBox.setSelected(true);			
			//}
		}
		
		/**
		 * This method should be called if OK button is clicked.
		 * @param prop
		 */
		public void commit(Properties prop) {
			String DISABLE_AUTO_SETTING = AttributeEditConfig.DISABLE_AUTO_SETTING;
			StringBuffer buffer = new StringBuffer();
			if (!eventSpeciesBox.isSelected()) 
			    buffer.append("Event." + eventSpeciesBox.getText());
			if (!eventDrBox.isSelected())
			    buffer.append(",Event." + eventDrBox.getText());
			if (!eventAuthoredBox.isSelected())
                buffer.append(",Event." + eventAuthoredBox.getText());
             if (!eventReviewedBox.isSelected())
                 buffer.append(",Event." + eventReviewedBox.getText());
             if (!reactionCompartmentBox.isSelected())
				buffer.append(",Reaction." + reactionCompartmentBox.getText());
			if (!complexSpeciesBox.isSelected())
				buffer.append(",Complex." + complexSpeciesBox.getText());
			if (!complexCompartmentBox.isSelected())
				buffer.append(",Complex." + complexCompartmentBox.getText());
             if (buffer.length() > 0)
			    prop.setProperty(DISABLE_AUTO_SETTING, buffer.toString());
			else
			    prop.remove(DISABLE_AUTO_SETTING);
             // Use properties as shuttle to extract this setting in other place.
			if (allowComboBoxEditorBox.isSelected())
				prop.setProperty("AttributeEdit.allowComboBoxEditor", true + "");
			else
				prop.remove("AttributeEdit.allowComboBoxEditor");
		}
		
		public boolean isChanged() {
			return this.isChanged;
		}
		
	}

}
