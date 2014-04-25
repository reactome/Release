/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

/** 
 *  Reacts to the buttons in TestResultsPane and provides the user with the popups that give more detailed information about test results.
 * @author croft
 */
public class TestResultsController {
	private TestResultsPane testResultsPane = null;
	private Action showCuratorsAction = null;
	private Action showInstancesAction = null;
	private TestResultsCuratorPane testResultsCuratorPane = null;
	
	public TestResultsController(TestResultsPane testResultsPane) {
		this.testResultsPane = testResultsPane;
	}
	
	/** 
	 *  Pops up a dialog (TestResultsCuratorPane) showing all of the curators for instances failing all selected tests.
	 */
	private void showCurators() {
		List testList = testResultsPane.getTestResultsTable().getSelectedTests();
		if (testList.size()<1) {
            JOptionPane.showMessageDialog(testResultsPane,
                    "You have not selected any tests!",
                    "No tests selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
		}
		
		// If we already have a testResultsCuratorPane, reactivate it
		// and use it, rather than generating a new one all over
		// again (slow).
		if (testResultsCuratorPane==null)
			testResultsCuratorPane = new TestResultsCuratorPane(testList);
		else
			testResultsCuratorPane.setVisible(true);
	}
	
	public Action getShowCuratorsAction() {
		if (showCuratorsAction == null) {
			showCuratorsAction = new AbstractAction("Curators") {
				public void actionPerformed(ActionEvent e) {
					showCurators();
				}
			};
		}
		return showCuratorsAction;
	}
	
	/** 
	 *  Pops up a org.gk.database.InstanceListPane showing all of the instances failing all selected tests.
	 */
	private void showInstances() {
		List testList = testResultsPane.getTestResultsTable().getSelectedTests();
		if (testList.size()<1) {
            JOptionPane.showMessageDialog(testResultsPane,
                    "You have not selected any tests!",
                    "No tests selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
		}
		TestResultsInstancePane testResultsInstancePane = new TestResultsInstancePane(testList);
	}
	
	public Action getShowInstancesAction() {
		if (showInstancesAction == null) {
			showInstancesAction = new AbstractAction("Instances") {
				public void actionPerformed(ActionEvent e) {
					showInstances();
				}
			};
		}
		return showInstancesAction;
	}
	
	/**
	 * Waits for a mouse click and if it hears one, resets the
	 * testResultsCuratorPane to null.
	 * 
	 * @return
	 */
	public MouseListener getTestResultsMouseListener() {
		return new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				testResultsCuratorPane = null;
				
			}

			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		
		};
	}
}