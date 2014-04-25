/*
 * Created on Apr 6, 2004
 */
package org.gk.graphEditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JViewport;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.gk.model.DatabaseIdentifier;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderableComplex;
import org.gk.render.RenderableEntity;

/**
 * Change the color for displaying Renderable objects. This class is
 * designed for overlay gene expression data by wrapping a GraphEditorPane.
 * @author wugm
 */
public class ColorableGraphEditorPane extends JPanel {
	private GraphEditorPane graphPane;
	private JSlider slider;
	private JButton loadBtn;
	private JButton unloadBtn;
	private JButton animateBtn;
	// A list of RenderableEntity data can be overlayed
	private java.util.List colorableNodes;
	// data to be used
	private Map dataMap; // Key: Colorable Node. Value: double array for data
	// To track the change
	private int preIndex;
	// Record the original color to revert back after unloading data
	private Map originalColorMap;
	// For animate
	private Timer timer;

	public ColorableGraphEditorPane() {
		init();
	}
	
	public ColorableGraphEditorPane(GraphEditorPane graphPane) {
		this();
		setGraphEditorPane(graphPane);
	}
	
	private void init() {
		setLayout(new BorderLayout());
		JPanel controlPane = new JPanel();
		controlPane.setBorder(BorderFactory.createLoweredBevelBorder());
		controlPane.setLayout(new BorderLayout());
		slider = new JSlider();
		slider.setPaintLabels(true);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				colorNodes(slider.getValue());
			}
		});
		controlPane.add(slider, BorderLayout.CENTER);
		JPanel btnPane = new JPanel();
		btnPane.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
		btnPane.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		loadBtn = new JButton("Load Data...");
		loadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadData();
			}
		});
		loadBtn.setMnemonic('L');
		animateBtn = new JButton("Start Animation");
		animateBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = animateBtn.getText();
				if (text.startsWith("Start")) {
					startAnimation();
				}
				else {
					stopAnimation();
				}
			}
		});
		animateBtn.setMnemonic('A');
		unloadBtn = new JButton("Unload Data");
		unloadBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				unloadData();
			}
		});
		unloadBtn.setMnemonic('U');
		btnPane.add(loadBtn);
		btnPane.add(animateBtn);
		btnPane.add(unloadBtn);
		controlPane.add(btnPane, BorderLayout.SOUTH);
		add(controlPane, BorderLayout.SOUTH);
		// Default make slider invisible
		slider.setVisible(false);
		// Default should disbaled. Enable it after loading data.
		animateBtn.setEnabled(false);
		unloadBtn.setEnabled(false);
	}
	
	public void setGraphEditorPane(GraphEditorPane graphPane) {
		if (this.graphPane != null) {
			JViewport viewPort = (JViewport) this.graphPane.getParent();
			viewPort.setView(graphPane);
			this.graphPane = graphPane;
		}
		else {
			this.graphPane = graphPane;
			add(new JScrollPane(graphPane), BorderLayout.CENTER);
		}
	}

	public GraphEditorPane getGraphEditorPane() {
		return this.graphPane;
	}
	
	public void setColorableNodes(java.util.List nodes) {
		this.colorableNodes = nodes;
	}
	
	public java.util.List getColorableNodes() {
		return this.colorableNodes;
	}
	
	private void loadData() {
		stopAnimation();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select Data File...");
		int reply = fileChooser.showOpenDialog(this);
		if (reply != JFileChooser.APPROVE_OPTION)
			return;
		File file = fileChooser.getSelectedFile();
		try {
			loadData(file);
			slider.setVisible(true);
			animateBtn.setEnabled(true);
			unloadBtn.setEnabled(true);
		}
		catch (IOException e) {
			System.err.println("ColorableGraphEditorPane.loadData(): " + e);
			e.printStackTrace();
		}
	}
	
	private void loadData(File file) throws IOException {
		preIndex = -1; // Reset
		FileReader fileReader = new FileReader(file);
		BufferedReader reader = new BufferedReader(fileReader);
		String line = reader.readLine(); // It should be the title line
		StringTokenizer tokenizer = new StringTokenizer(line, "\t");
		// Fist token should be SwissProt
		tokenizer.nextToken();
		java.util.List dataPoints = new ArrayList();
		while (tokenizer.hasMoreTokens()) {
			dataPoints.add(new Integer(tokenizer.nextToken()));
		}
		// Loaded Data
		Map loadedDataMap = new HashMap(); // Key: SwissID, Values: double array
		String swissID = null;
		int index = 0;
		while ((line = reader.readLine()) != null) {
			tokenizer = new StringTokenizer(line);
			swissID = tokenizer.nextToken();
			double[] data = new double[dataPoints.size()];
			index = 0;
			while (tokenizer.hasMoreTokens())
				data[index ++] = Double.parseDouble(tokenizer.nextToken());
			loadedDataMap.put(swissID, data);
		}
		// Populate the data to dataMap to cache all useful data.
		if (dataMap == null)
			dataMap = new HashMap();
		else
			dataMap.clear();
		for (Iterator it = colorableNodes.iterator(); it.hasNext();) {
			Renderable node = (Renderable) it.next();
			if (node instanceof RenderableEntity) {
				DatabaseIdentifier di = (DatabaseIdentifier) node.getAttributeValue("databaseIdentifier");
				if (di != null && di.getAccessNo() != null) {
					Object data = loadedDataMap.get(di.getAccessNo());
					if (data != null)
						dataMap.put(node, data);
				}
			}
			else if (node instanceof RenderableComplex) {
				double[] data = computeComplexData((RenderableComplex)node, dataPoints.size(), loadedDataMap);
				if (data != null) //Don't use empty data
					dataMap.put(node, data);
			}
			else if (node instanceof ReactionNode) {
				double[] data = computeReactionData((ReactionNode)node, dataPoints.size(), loadedDataMap);
				if (data != null)
					dataMap.put(node, data);
			}
		}
		// To record the original color for rolling back
		if (originalColorMap == null)
			originalColorMap = new HashMap();
		else
			originalColorMap.clear();
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			originalColorMap.put(r, r.getBackgroundColor());
		}
		if (dataPoints.size() > 1) {
			configureSlider(dataPoints);
		}
		else {// Don't need a slider.
			slider.setVisible(false);
			animateBtn.setEnabled(false);
		}
	}
	
	private void configureSlider(java.util.List dataPoints) {
		slider.setMinimum(0);
		slider.setMaximum(dataPoints.size() - 1);
		slider.setMajorTickSpacing(1);
		slider.setSnapToTicks(true);
		slider.setValue(0);
		Hashtable labelTable = new Hashtable();
		for (int i = 0; i < dataPoints.size(); i++)
			labelTable.put(new Integer(i), new JLabel(dataPoints.get(i).toString()));
		slider.setLabelTable(labelTable);		
	}
	
	private double[] computeComplexData(RenderableComplex complex, int dataLength, Map loadedDataMap) {
		java.util.List entities = new ArrayList();
		getContainedEntities(complex, entities);
		// Get the average for all entities
		double[] complexData = new double[dataLength];
		double total = 0;
		int c = 0;
		for (Iterator it = entities.iterator(); it.hasNext();) {
			Renderable subunit = (Renderable) it.next();
			DatabaseIdentifier di = (DatabaseIdentifier) subunit.getAttributeValue("databaseIdentifier");
			if (di != null && di.getAccessNo() != null) {
				double[] data = (double[]) loadedDataMap.get(di.getAccessNo());
				if (data != null) {
					for (int i = 0; i < complexData.length; i++)
						complexData[i] += data[i];
					c ++;
				}
			}
		}
		if (c > 1) { // Average
			for (int i = 0; i < complexData.length; i++) {
				complexData[i] /= c;
			}
		}
		if (c > 0)
			return complexData;
		else
			return null;
	}
	
	private double[] computeReactionData(ReactionNode reaction, int dataLength, Map loadedDataMap) {
		if (reaction.getComponents() == null ||
		    reaction.getReaction() == null)
		    return null;
		double[] reactionData = new double[dataLength];
		int c = 0;
		java.util.List positiveNodes = new ArrayList();
		java.util.List inputs = reaction.getReaction().getInputNodes();
		if (inputs != null)
			positiveNodes.addAll(inputs);
		java.util.List catalysts = reaction.getReaction().getHelperNodes();
		if (catalysts != null)
			positiveNodes.addAll(catalysts);
		java.util.List activators = reaction.getReaction().getActivatorNodes();
		if (activators != null)
			positiveNodes.addAll(activators);
		for (Iterator it = positiveNodes.iterator(); it.hasNext();) {
			Renderable node = (Renderable) it.next();
			if (node instanceof RenderableEntity) {
				DatabaseIdentifier di = (DatabaseIdentifier)node.getAttributeValue("databaseIdentifier");
				if (di != null && di.getAccessNo() != null) {
					double[] data = (double[])loadedDataMap.get(di.getAccessNo());
					if (data != null) {
						for (int i = 0; i < reactionData.length; i++)
							reactionData[i] += data[i];
						c++;
					}
				}
			}
			else if (node instanceof RenderableComplex){
				double[] complexData = computeComplexData((RenderableComplex)node, dataLength, loadedDataMap);
				if (complexData != null) {
					for (int i = 0; i < reactionData.length; i++)
						reactionData[i] += complexData[i];
					c++;
				}
			}
		}
		if (c > 1) { // Average
			for (int i = 0; i < reactionData.length; i++) {
				reactionData[i] /= c;
			}
		}
		if (c > 0)
			return reactionData;
		else
			return null;
	}
	
	private void colorNodes(int dataIndex) {
		if (preIndex == dataIndex)
			return;
		for (Iterator it = dataMap.keySet().iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			double[] data = (double[]) dataMap.get(r);
			double value = data[dataIndex];
			float rValue = (float) (value + 2.0 / 4.0);
			if (rValue > 1.0f)
				rValue = 1.0f;
			if (rValue < 0.0f)
				rValue = 0.0f;
			Color c = new Color(rValue, 1.0f - rValue, 0.0f);
			r.setBackgroundColor(c);
		}
		graphPane.repaint(graphPane.getVisibleRect());
		preIndex = dataIndex;	
	}
	
	private void unloadData() {
		stopAnimation();
		animateBtn.setEnabled(false);
		unloadBtn.setEnabled(false);
		slider.setVisible(false);
		dataMap.clear();
		// Revert to original color
		for (Iterator it = originalColorMap.keySet().iterator(); it.hasNext();) {
			Renderable r = (Renderable) it.next();
			Color c = (Color) originalColorMap.get(r);
			r.setBackgroundColor(c);
		}
		originalColorMap.clear();
	}
	
	private void startAnimation() {
		if (timer == null) {
			ActionListener listener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int nextValue = slider.getValue() + slider.getMajorTickSpacing();
					if (nextValue > slider.getMaximum())
						nextValue = slider.getMinimum();
					slider.setValue(nextValue);
				}
			};
			timer = new Timer(2000, listener);
		}
		animateBtn.setText("Stop Animation");
		slider.setValue(slider.getMinimum());
		timer.start();
	}
	
	private void stopAnimation() {
		if (timer != null && timer.isRepeats())
			timer.stop();
		animateBtn.setText("Start Animation");
	}
	
	private void getContainedEntities(RenderableComplex complex, java.util.List entities) {
		if (complex.getComponents() != null) {
			for (Iterator it = complex.getComponents().iterator(); it.hasNext();) {
				Renderable r = (Renderable) it.next();
				if (r instanceof RenderableEntity)
					entities.add(r);
				else if (r instanceof RenderableComplex)
					getContainedEntities((RenderableComplex)r, entities);
			}
		}
	}

}
