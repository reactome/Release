/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/** 
 *  This pane provides an overall view of any test results plus ways to get more detailed information on the results.
 * @author croft
 */
public class TestResultsPane extends JPanel {
	private TestResultsController controller;
	private TestResultsTable testResultsTable;
	private JButton curatorsBtn;
	private JButton instancesBtn;
	private IDGenerationPane iDGenerationPane;
	JLabel titleLabel;
	
	public TestResultsPane(IDGenerationPane iDGenerationPane) {
		init();
		this.iDGenerationPane = iDGenerationPane;
	}
	
	private void init() {
		controller = new TestResultsController(this);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		JTextField dummyTextField = new JTextField(5);
		int minimumTextHeight = dummyTextField.getMinimumSize().height;
		
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		titleLabel = new JLabel("Results");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		titlePanel.add(titleLabel);
		titlePanel.setMaximumSize(new Dimension(titlePanel.getMaximumSize().width, minimumTextHeight));
		add(titlePanel);
		
		JPanel showPane = new JPanel();
		showPane.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel showLabel = new JLabel("Show:    ");
		showPane.add(showLabel);
		curatorsBtn = new JButton(controller.getShowCuratorsAction());
		showPane.add(curatorsBtn);
		instancesBtn = new JButton(controller.getShowInstancesAction());
		showPane.add(instancesBtn);
		showPane.setMaximumSize(new Dimension(showPane.getMaximumSize().width, minimumTextHeight));
		add(showPane);
		
		testResultsTable = new TestResultsTable();
		testResultsTable.addMouseListener(controller.getTestResultsMouseListener());
		JScrollPane testResultsScrollPane = new JScrollPane(testResultsTable);
		add(testResultsScrollPane);
		
		setPreferredSize(new Dimension(150, getPreferredSize().height));
	}
	
	public TestResultsController getController() {
		return controller;
	}
	
	public void setTests(IDGeneratorTests tests) {
		TestResultsTableModel model = (TestResultsTableModel)testResultsTable.getModel();
		model.setTests(tests);
	}
	
	public TestResultsTable getTestResultsTable() {
		return testResultsTable;
	}
	
	public IDGenerationPane getIDGenerationPane() {
		return iDGenerationPane;
	}
	
	public void setTitleLabelTest() {
		titleLabel.setText("Test results");
	}
	
	public void setTitleLabelRun() {
		titleLabel.setText("Run results");
	}
}