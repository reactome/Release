/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/** 
 *  This pane doesn't actually do much except to hold other panes.
 * @author croft
 */
public class IDGenerationPane extends JPanel {
	private ReleasesPane releasesPane;
	private IncludeInstancesPane includeInstancesPane;
	private TestResultsPane testResultsPane;
	private IDGenerationFrame frame;
	
	public IDGenerationPane(IDGenerationFrame frame) {
		init();
		this.frame = frame;
	}
	
	private void init() {
		Dimension miniSize = new Dimension(10, 100);

		releasesPane = new ReleasesPane(this);
		releasesPane.setMinimumSize(miniSize);
		includeInstancesPane = new IncludeInstancesPane(this);
		includeInstancesPane.setMinimumSize(miniSize);
		testResultsPane = new TestResultsPane(this);
		testResultsPane.setMinimumSize(miniSize);
		
		// Initialize schema
		releasesPane.getController().updateSchemaDisplayPane();
		
		JLabel titleLabel = new JLabel("ID Generator");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		setLayout(new BorderLayout());
		JSplitPane jsp1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, releasesPane, includeInstancesPane);
		JSplitPane jsp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jsp1, testResultsPane);
		jsp1.setResizeWeight(0.6);
		jsp2.setResizeWeight(0.6);
		jsp1.setOneTouchExpandable(true);
		jsp2.setOneTouchExpandable(true);
		add(jsp2, BorderLayout.CENTER);
	}
	
	public ReleasesPane getReleasesPane() {
		return releasesPane;
	}
	
	public IncludeInstancesPane getIncludeInstancePane() {
		return includeInstancesPane;
	}
	
	public TestResultsPane getTestResultsPane() {
		return testResultsPane;
	}

	public IDGenerationFrame getFrame() {
		return frame;
	}
}