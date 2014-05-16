/*
 * Created on Mar 11, 2004
 */
package org.gk.pathView;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.util.GKApplicationUtilities;


/**
 * A customized JMenuBar for the GK visualization tool.
 * @author wugm
 */
public class VisualizationMenuBar extends JMenuBar {
	private final String GENE_X_IMPORTER_PROP = "geneXImporter.prop";
	
	private GKVisualizationPane tool;
	private JMenuItem animateItem;
	// For gene expression data
	private GeneExpressionDataImporter importer;
	private JMenuItem unloadDataItem;
	private JMenuItem loadDataItem;
	private JMenu zoomMenu;
	private Timer timer;

	public VisualizationMenuBar() {
		super();
		init();
	}
	
	public VisualizationMenuBar(GKVisualizationPane app) {
		super();
		setTool(app);
		init();
	}
	
	public void setTool(GKVisualizationPane app) {
		this.tool = app;
	}
	
	public GKVisualizationPane getTool() {
		return tool;
	}
	
	private void init() {
		VisualizationToolActions actions = tool.getToolActions();
		ActionListener actionListener = createActionListener();
		int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		// File menu
		JMenu file = new JMenu("File");
		file.setMnemonic('F');
		file.add(actions.getExportSVGAction());
		file.addSeparator();
		JMenuItem saveItem = file.add(actions.getSaveAction());
		saveItem.setAccelerator(KeyStroke.getKeyStroke('S', shortcutKey));
		file.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setActionCommand("exit");
		exitItem.addActionListener(actionListener);
		file.add(exitItem);
		// Edit menu
		JMenu edit = new JMenu("Edit");
		edit.setMnemonic('E');
		JMenuItem undoItem = edit.add(actions.getUndoAction());
		undoItem.setAccelerator(KeyStroke.getKeyStroke('Z', shortcutKey));
		JMenuItem redoItem = edit.add(actions.getRedoAction());
		redoItem.setAccelerator(KeyStroke.getKeyStroke('Y', shortcutKey));
		edit.addSeparator();
		JMenuItem item = edit.add(actions.getArrangeVerticalLineAction());
		item = edit.add(actions.getArrangeHorizontalLineAction());
		edit.addSeparator();
		item = edit.add(actions.getParallelArrowVerticalAction());
		item = edit.add(actions.getParallelArrowHorizontalAction());
		edit.addSeparator();
		item = edit.add(actions.getArrangeVerticalAction());
		item = edit.add(actions.getArrangeHorizontalAction());
		edit.addSeparator();
		item = edit.add(actions.getAssignLengthAction());
		edit.addSeparator();
		item = edit.add(actions.getSelectAllAction());
		item.setAccelerator(KeyStroke.getKeyStroke('A', shortcutKey));
		JMenu graphMenu = createGraphMenu(actions);
		// View menu
		JMenu view = new JMenu("View");
		view.setMnemonic('V');
		JMenuItem overallViewItem = view.add(actions.getOverallViewAction());
		overallViewItem.setAccelerator(KeyStroke.getKeyStroke('O', shortcutKey));
		view.addSeparator();
		JMenuItem searchItem = view.add(actions.getSearchAction());
		searchItem.setAccelerator(KeyStroke.getKeyStroke('F', shortcutKey));
		// For diff between two databases.
		JMenu diffMenu = new JMenu("Diff");
		diffMenu.setMnemonic('f');
    JMenuItem CompareRxnInSkyItem = diffMenu.add(actions.getCompareReactionsInSkyAction());
    diffMenu.addSeparator();
		JMenuItem compareDBItem = diffMenu.add(actions.getCompareDBAction());
    diffMenu.addSeparator();
		JMenuItem closeComparisontItem = diffMenu.add(actions.getCloseCompareDBAction());
		
		add(file);
		add(graphMenu);
		add(edit);
		add(view);
		add(diffMenu);
		// For expression data
		//if (!GKApplicationUtilities.isDeployed) {
			JMenu data = new JMenu("Data");
			data.setMnemonic('D');
			loadDataItem = new JMenuItem("Load Expression Data");
			loadDataItem.setActionCommand("loadData");
			loadDataItem.addActionListener(actionListener);
			data.add(loadDataItem);
			unloadDataItem = new JMenuItem("Unload Data");
			unloadDataItem.setActionCommand("unloadData");
			unloadDataItem.addActionListener(actionListener);
			data.add(unloadDataItem);
			animateItem = new JMenuItem("Start Animation");
			animateItem.setAccelerator(KeyStroke.getKeyStroke('S', KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
			animateItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String txt = animateItem.getText();
					if (txt.startsWith("Start")) // Start animation
						startAnimation();
					else 
						stopAnimation();
				}
			});
			data.addSeparator();
			data.add(animateItem);
			add(data);
			// Default 
			unloadDataItem.setEnabled(false);
			animateItem.setEnabled(false);
			ChangeListener changeListener = new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (importer != null) {
						int value = tool.slider.getValue();
						importer.setDataIndex(value);
						tool.repaint(tool.getVisibleRect());
						String label = importer.getDataPoints().get(value).toString();
						if (label != null) {
							tool.statusLabel.setText("Exp: " + label);
						}
						else 
							tool.statusLabel.setText("Exp: unknown");
					}
				}
			};
			tool.slider.addChangeListener(changeListener);
		//}
	}
	
	private JMenu createGraphMenu(VisualizationToolActions actions) {
		JMenu graphMenu = new JMenu("Graph");
		graphMenu.setMnemonic('G');
		graphMenu.addSeparator();
		zoomMenu = createZoomMenu();
		graphMenu.add(zoomMenu);
		graphMenu.addSeparator();
		JMenuItem loadSelectedItem = graphMenu.add(actions.getLoadSelectedAction());
		loadSelectedItem.setAccelerator(KeyStroke.getKeyStroke('L', KeyEvent.CTRL_MASK));
		JMenuItem loadSelectedAndConnectedItem = graphMenu.add(actions.getLoadSelectedAndConnectedAction());
		loadSelectedAndConnectedItem.setAccelerator(KeyStroke.getKeyStroke('L', KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
		JMenuItem unloadItem = graphMenu.add(actions.getUnloadReactionAction());
		graphMenu.addSeparator();
		JMenuItem fixSelectedItem = graphMenu.add(actions.getFixSelectedAction());
		JMenuItem fixAllItem = graphMenu.add(actions.getFixAllAction());
		graphMenu.addSeparator();
		JMenuItem releaseSelectedItem = graphMenu.add(actions.getReleaseSelectedAction());
		JMenuItem releaseAllItem = graphMenu.add(actions.getReleaseAllAction());
		graphMenu.addSeparator();
		JMenuItem deleteItem = graphMenu.add(actions.getDeleteAction());
		if (!GKApplicationUtilities.isMac()) {
		    graphMenu.addSeparator();
		    // JCheckBox cannot be displayed correctly under MacOS X
		    JCheckBox runLayoutBox = new JCheckBox("Run Layout");
		    runLayoutBox.addItemListener(new ItemListener() {
		        public void itemStateChanged(ItemEvent e) {
		            if (e.getStateChange() == ItemEvent.DESELECTED) {
		                tool.relaxerSuspended = true;
		            } else {
		                tool.relaxerSuspended = false;
		            }
		        }
		    });
		    //runLayoutBox.setMnemonic('R');
		    // Default disable
		    runLayoutBox.setSelected(false);
		    graphMenu.add(runLayoutBox);
		    JCheckBox updateScreenBox = new JCheckBox("Update Screen");
		    updateScreenBox.addItemListener(new ItemListener() {
		        public void itemStateChanged(ItemEvent e) {
		            if (e.getStateChange() == ItemEvent.DESELECTED) {
		                tool.relaxWithoutRepaint = true;
		            } 
                    else {
		                tool.relaxWithoutRepaint = false;
		            }
		        }
		    });
		    updateScreenBox.setSelected(true);
		    graphMenu.add(updateScreenBox);
		}
		return graphMenu;
	}
	
	private JMenu createZoomMenu() {
		JMenu zoomMenu = new JMenu("Zoom");
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JRadioButton item = (JRadioButton) e.getSource();
				String text = item.getText();
				double factor = Double.parseDouble(text);
				tool.zoom(factor);
			}
		};
		ButtonGroup group = new ButtonGroup();
		JRadioButton item;
		double[] zoomFactors = tool.zoomFactors;
		for (int i = 0; i < zoomFactors.length; i++) {
			item = new JRadioButton(zoomFactors[i] + "");
			group.add(item);
			zoomMenu.add(item);
			// Default selected
			if (Math.abs(zoomFactors[i] - 1.0) < 0.001)
			    item.setSelected(true);
			item.addActionListener(l);
		}
		return zoomMenu;
	}
	
	protected JMenu getZoomMenu() {
	    return this.zoomMenu;
	}
	
	private void startAnimation() {
		if (timer == null) {
			ActionListener l = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (importer == null || importer.getDataPoints() == null ||
					    importer.getDataPoints().size() < 2)
					    return;
					int nextValue = tool.slider.getValue() + 1;
					if (nextValue > tool.slider.getMaximum())
						nextValue = tool.slider.getMinimum();
					tool.slider.setValue(nextValue);
				}
			};
			timer = new Timer(2000, l);
		}
		tool.slider.setValue(tool.slider.getMinimum());
		timer.start();
		animateItem.setText("Stop Animation");	
	}
	
	private void stopAnimation() {
		if (timer != null && timer.isRunning()) {
			timer.stop();
			animateItem.setText("Start Animation");
		}
	}

	private ActionListener createActionListener() {
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String cmd = e.getActionCommand();
				if (cmd.equals("loadData")) {
					importGeneXData();
				}
				else if (cmd.equals("unloadData")) {
					if (importer != null) {
						tool.slider.setVisible(false);
						importer.resetEdges();
						tool.repaint(getVisibleRect());
					}
					if (timer != null && timer.isRunning()) {
						timer.stop();
					}
					animateItem.setText("Start Animation");
					animateItem.setEnabled(false);
					unloadDataItem.setEnabled(false);
					tool.statusLabel.setText("Viewbox: " + tool.dimension.width + " : " + tool.dimension.height);
				}
				else if (cmd.equals("exit"))
					tool.exit();
			}
		};
		return l;
	}
	
	private void importGeneXData() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select Data File...");
		int reply = fileChooser.showOpenDialog(this);
		if (reply != JFileChooser.APPROVE_OPTION)
			return;
		File file = fileChooser.getSelectedFile();
		try {
			if (importer == null)
				initGeneXImporter();
			if (importer == null)
				return; // Cannot load an importer
			java.util.List colorableEdges = new ArrayList();
			for (Iterator it = tool.edges.iterator(); it.hasNext();) {
				IEdge edge = (IEdge) it.next();
				if (edge.getType() == IEdge.REACTION_EDGE)
					colorableEdges.add(edge);
			}
			importer.setColorableEdge(colorableEdges);
			importer.setDataSource(file);
			configureSlider(tool.slider, 
			                importer.getDataPoints(), 
			                importer.getMajorTickSpacing(), 
			                importer.isDataPointsUsedForLabels());
			tool.slider.setValue(0); // Start from the frist data point.
			// Need to call this method explicitly
			//importer.setDataIndex(0);
			//tool.repaint(tool.getVisibleRect());
			unloadDataItem.setEnabled(true);
			animateItem.setEnabled(true);
		}
		catch (IOException e) {
			System.err.println("ColorableGraphEditorPane.loadData(): " + e);
			e.printStackTrace();
		}
	}
	
	private void initGeneXImporter() {
		// Check if there is an importer specified
		Properties prop = new Properties();
		File file = new File("resources" + File.separator + GENE_X_IMPORTER_PROP);
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				prop.load(fis);
				fis.close();
				String clsName = prop.getProperty("importer");
				importer = (GeneExpressionDataImporter)Class.forName(clsName).newInstance();
			}
			catch (Exception e) {
				System.err.println("VisualizationMenuBar.initGeneXImporter(): " + e);
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
				                              "Cannot load data importer: " + e.getMessage(),
				                              "Error",
				                              JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		else
			importer = new DefaultGeneExpressionDataImporter();
	}
	
	private void configureSlider(JSlider slider, java.util.List dataPoints, int majorTickSpacing, boolean useDataPointsForLabels) {
		slider.setPaintLabels(true);
		slider.setMinimum(0);
		slider.setMaximum(dataPoints.size() - 1);
		slider.setMajorTickSpacing(majorTickSpacing);
		if (majorTickSpacing > 1)
			slider.setMinorTickSpacing(1);
		slider.setSnapToTicks(true);
		slider.setValue(0);
		if (useDataPointsForLabels) {
			Hashtable labelTable = new Hashtable();
			for (int i = 0; i < dataPoints.size(); i++)
				labelTable.put(new Integer(i), new JLabel(dataPoints.get(i).toString()));
			slider.setLabelTable(labelTable);
		}
		slider.setVisible(true);		
	}
}
