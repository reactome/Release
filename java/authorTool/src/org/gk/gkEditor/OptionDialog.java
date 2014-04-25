/*
 * Created on Aug 26, 2003
 */
package org.gk.gkEditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.Border;

import org.gk.database.FrameManager;
import org.gk.persistence.DBConnectionPane;
import org.gk.persistence.PersistenceManager;
import org.gk.render.Node;
import org.gk.render.RenderableEntity;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.HierarchicalLayout;

/**
 * A customized JDialog to set options.
 * @author wgm
 */
public class OptionDialog extends JDialog {
	// GUIs
	private JTabbedPane tabbedPane;
	private JComboBox lfBox;
	private JTextField browserTF;
	private JTextField widthTF;
	private GKEditorFrame editorFrame;
	private JTextField nodeDistTF;
	private JTextField layerDistTF;
	private DBConnectionPane dbPane;
	private JCheckBox toolbarTextBox;
	// A list of LookAndFeel
	private final String[] lfNames = new String[]{"aqua", 
                                                  "metal",
		                                          "windows"};
	private boolean isLFChanged;
	private boolean isBrowserChanged;
	private boolean isToolbarChanged;
	private int oldWidth; // Keep the old value

	public OptionDialog() {
		super();
		init();
	}
	
	public OptionDialog(GKEditorFrame parentFrame) {
		super(parentFrame);
		this.editorFrame = parentFrame;
		init();
	}
	
	public void setEditorFrame(GKEditorFrame parentFrame) {
		this.editorFrame = parentFrame;
	}
	
	private void init() {
		tabbedPane = new JTabbedPane();
		if (editorFrame == null || !editorFrame.isForCuratorTool) {
			JPanel generalPane = createGeneralPane();
			tabbedPane.addTab("General", generalPane);
		}
		// Add node display pane
		JPanel nodePane = createNodeDisplayPane();
		tabbedPane.add("Node Display", nodePane);
		// Add layout settings
		JPanel layoutPane = createLayoutPane();
		tabbedPane.add("Automatic Layout", layoutPane);
		if (editorFrame == null || !editorFrame.isForCuratorTool) {
			// Add a tab for the database connection
			dbPane = new DBConnectionPane();
			dbPane.setValues(editorFrame.getProperties());
			tabbedPane.add("Database Connection", dbPane);
		}
		// Control Pane
		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				commit();
			}
		});
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rollback();
			}
		});
		southPane.add(okBtn);
		southPane.add(cancelBtn);
		
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		getContentPane().add(southPane, BorderLayout.SOUTH);
		// Keep the old value to revert back
		oldWidth = Node.getNodeWidth();
	}
	
	private JPanel createGeneralPane() {
		JPanel generalPane = new JPanel();
		generalPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(4, 4, 4, 4);
		JLabel lfLabel = new JLabel("Look and Feel:");
		generalPane.add(lfLabel, constraints);
		lfBox = new JComboBox();
		initLFBox(lfBox);
		String lfName = editorFrame.getProperties().getProperty("lookAndFeel");
		if (lfName == null || lfName.length() == 0)
			lfBox.setSelectedItem(GKApplicationUtilities.getDefaultLF());
		else
			lfBox.setSelectedItem(lfName);
		lfBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					doLFChange((String)lfBox.getSelectedItem());
					isLFChanged = true;
				}
			}
		});
		constraints.gridx = 1;
		generalPane.add(lfBox, constraints);
		// No need to add browser for Mac
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("mac") == -1) {
			JLabel browserLabel = new JLabel("Default Web Browser:");
			constraints.gridx = 0;
			constraints.gridy = 1;
			generalPane.add(browserLabel, constraints);
			browserTF = new JTextField();
			browserTF.setPreferredSize(new Dimension(150, 25));
			String browser = BrowserLauncher.getBrowser();
			if (browser != null)
				browserTF.setText(browser);
			constraints.gridx = 1;
			generalPane.add(browserTF, constraints);
			JButton browserBtn = new JButton("...");
			browserBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Launch a GUI
					JFileChooser fileChooser = new JFileChooser();
					fileChooser.setDialogTitle("Choose A Web Browser");
					int rtn = fileChooser.showOpenDialog(OptionDialog.this);
					if (rtn == JFileChooser.APPROVE_OPTION) {
						String browserPath = fileChooser.getSelectedFile().getAbsolutePath();
						browserTF.setText(browserPath);
						isBrowserChanged = true;
					}
				}
			});
			browserBtn.setPreferredSize(new Dimension(25, 24));
			constraints.gridx = 2;
			constraints.insets = new Insets(4, 0, 4, 4);
			generalPane.add(browserBtn, constraints);
		}
		// Add a control to show text in the toolbar buttons
		toolbarTextBox = new JCheckBox("Show text in the toolbar: ");
		toolbarTextBox.setHorizontalTextPosition(JCheckBox.LEFT);
		constraints.gridx = 0;
		if (osName.contains("mac")) {
		    constraints.gridy = 1;
		    constraints.gridwidth = 2;
		}
		else {
		    constraints.gridy = 2;
		    constraints.gridwidth = 3;
		}
		generalPane.add(toolbarTextBox, constraints);
		String isVisible = editorFrame.getProperties().getProperty("toolbarText");
		if (isVisible == null || isVisible.equals("true"))
		    toolbarTextBox.setSelected(true);
		else
		    toolbarTextBox.setSelected(false);
		toolbarTextBox.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        editorFrame.toolPane.setTextInToolbarVisible(toolbarTextBox.isSelected());
		        isToolbarChanged = true;
		    }
		});
		return generalPane;
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
	
	private JPanel createNodeDisplayPane() {
		JPanel nodePane = new JPanel();
		nodePane.setLayout(new GridLayout(2, 1, 2, 4));
		Border etchedBorder = BorderFactory.createEtchedBorder();
		Border titleBorder = BorderFactory.createTitledBorder(etchedBorder, "Setting");
		JPanel settingPane = new JPanel();
		settingPane.setBorder(titleBorder);
		settingPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(4, 4, 4, 4);
		JLabel widthLabel = new JLabel("Node Width:");
		settingPane.add(widthLabel, constraints);
		widthTF = new JTextField();
		widthTF.setText(Node.getNodeWidth() + "");
		widthTF.setPreferredSize(new Dimension(50, 25));
		constraints.gridx = 1;
		settingPane.add(widthTF, constraints);
		nodePane.add(settingPane);
		// Add preview pane
		final PreViewPane preViewPane = new PreViewPane();
		titleBorder = BorderFactory.createTitledBorder(etchedBorder, "Sample View");
		preViewPane.setBorder(titleBorder);
		nodePane.add(preViewPane);
		widthTF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int width = extractWidth(widthTF, "Width");
				if (width == -1)
					return;
				if (width != Node.getNodeWidth()) {
					Node.setNodeWidth(width);
					preViewPane.entity.invalidateBounds();
					preViewPane.repaint();
				}
			}
		});
		return nodePane;
	}
	
	private JPanel createLayoutPane() {
		JPanel layoutPane = new JPanel();
		layoutPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 4, 4, 4);
		JLabel nodeLabel = new JLabel("Node Distance:");
		layoutPane.add(nodeLabel, constraints);
		nodeDistTF = new JTextField();
		Dimension tfSize = new Dimension(75, 25);
		nodeDistTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		layoutPane.add(nodeDistTF, constraints);
		JLabel layerLabel = new JLabel("Layer Distance:");
		constraints.gridx = 0;
		constraints.gridy = 1;
		layoutPane.add(layerLabel, constraints);
		layerDistTF = new JTextField();
		layerDistTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		layoutPane.add(layerDistTF, constraints);
		// Initialize the settings
		nodeDistTF.setText(HierarchicalLayout.getNodeDistance() + "");
		layerDistTF.setText(HierarchicalLayout.getLayerDistance() + "");
		return layoutPane;
	}
	
	private int extractWidth(JTextField widthTF, String name) {
		String text = widthTF.getText();
		try {
			int width = Integer.parseInt(text);
			if (width <= 0) {
				JOptionPane.showMessageDialog(OptionDialog.this,
											  name + " should be a positive integer.",
											  name + " Error",
											  JOptionPane.ERROR_MESSAGE);
				if (widthTF == this.widthTF)
					tabbedPane.setSelectedIndex(1);
				else
					tabbedPane.setSelectedIndex(2);
				widthTF.requestFocus();
				return -1;
			}
			return width;
		}
		catch (NumberFormatException e1) {
			JOptionPane.showMessageDialog(
				OptionDialog.this,
				name + " should be a positive integer.",
				name + " Error",
				JOptionPane.ERROR_MESSAGE);
			if (widthTF == this.widthTF)
				tabbedPane.setSelectedIndex(1);
			else
				tabbedPane.setSelectedIndex(2);
			widthTF.requestFocus();
			return -1;                          
		}
	}
	
	private void doLFChange(String newLF) {
		GKApplicationUtilities.setLookAndFeel(newLF);
		SwingUtilities.updateComponentTreeUI(editorFrame);
		SwingUtilities.updateComponentTreeUI(this);
		FrameManager.getManager().updateUI();
	}
	
	private void commit() {
		Properties prop = editorFrame.getProperties();
		PersistenceManager.ConnectInfo oldConnectInfo = new PersistenceManager.ConnectInfo(prop);
		if (isLFChanged) 
			prop.setProperty("lookAndFeel", (String)lfBox.getSelectedItem());
		if (isBrowserChanged) 
			BrowserLauncher.setBrowser(browserTF.getText().trim());
		// Check with toolbar text
		if (isToolbarChanged)
		    prop.setProperty("toolbarText", toolbarTextBox.isSelected() + "");
		int width = extractWidth(widthTF, "Width");
		if (width == -1) // Something is wrong
			return;
		if (oldWidth != width) { 
			Node.setNodeWidth(width);
			validateNodeWidth();
			prop.setProperty("nodeWidth", width + "");
		}
		// Check layout distance
		width = extractWidth(nodeDistTF, "Distance");
		if (width == -1)
			return;
		if (width != HierarchicalLayout.getNodeDistance()) {
			HierarchicalLayout.setNodeDistance(width);
			prop.setProperty("layoutNodeDist", width + "");
		}
		width = extractWidth(layerDistTF, "Distance");
		if (width == -1)
			return;
		if (width != HierarchicalLayout.getLayerDistance()) {
			HierarchicalLayout.setLayerDistance(width);
			prop.setProperty("layoutLayerDist", width + "");
		}
		/*
		// For inserting reactions
		String value = (String) reactionComboBox.getSelectedItem();
		String oldValue = prop.getProperty("insertReactionAs");
		if (oldValue == null || oldValue.length() == 0)
			oldValue = "node";
		if (!oldValue.equals(value))
			prop.setProperty("insertReactionAs", value);
		*/
		// Check for Database connection
		if (dbPane != null &&
			!dbPane.commitForTab()) {
			tabbedPane.setSelectedIndex(3);
			return;
		}
		PersistenceManager.ConnectInfo newConnectInfo = new PersistenceManager.ConnectInfo(prop);
		if (PersistenceManager.getManager().getActiveMySQLAdaptor() != null &&
			!newConnectInfo.equals(oldConnectInfo)) {
			// Need to reset the active MySQLAdaptor
			FrameManager.getManager().closeBrowser();
			PersistenceManager.getManager().setActiveMySQLAdaptor(null);
		}	
		dispose();
	}
	
	/**
	 * Invalidate all Node bounds.
	 */
	private void validateNodeWidth() {
		editorFrame.validateNodeWidth(); 
	}
	
	private void rollback() {
		Properties prop = editorFrame.getProperties();
		if (isLFChanged) {
			String lfName = prop.getProperty("lookAndFeel");
			doLFChange(lfName);
		}
		if (isToolbarChanged) {
		    String oldValue = prop.getProperty("toolbarText");
		    if (oldValue == null || oldValue.equals("true")) {
		        if (!toolbarTextBox.isSelected())
		            editorFrame.toolPane.setTextInToolbarVisible(true);
		    }
		    else if (toolbarTextBox.isSelected())
		        editorFrame.toolPane.setTextInToolbarVisible(false);
		}
		if (oldWidth != Node.getNodeWidth())
			Node.setNodeWidth(oldWidth);
		dispose();
	}
	
	class PreViewPane extends JPanel {
		RenderableEntity entity;
		
		PreViewPane() {
			entity = new RenderableEntity("fructose-bisphosphate aldolase B");
		}
		
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2 = (Graphics2D) g;
			// Set the renderable position
			int w = getWidth();
			int h = getHeight();
			entity.setPosition(new Point(w / 2, h / 2));
			entity.validateBounds(g);
			entity.render(g);
		}
	}

}
