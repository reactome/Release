/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;

/** 
 * Table for displaying test results.
 * 
 * Inherits from JTable, sets appropriate table model and provides
 * some special methods.
 * @author croft
 */
public class TestResultsTable extends JTable {
	public TestResultsTable() {
		TestResultsTableModel model = new TestResultsTableModel();
		setModel(model);
		
		// Readjust column widths
		TableColumn column = null;
		String columnName;
		JTextField dummyTextField;
		int testColumnMinWidth = 100;
		for (int i = 0; i < model.getColumnCount(); i++) {
		    column = getColumnModel().getColumn(i);
		    columnName = model.getColumnName(i);
		    dummyTextField = new JTextField(columnName);
		    column.setPreferredWidth(dummyTextField.getMinimumSize().width);
	    	if (i==0 && dummyTextField.getMinimumSize().width<testColumnMinWidth)
	    		column.setPreferredWidth(testColumnMinWidth);
		}
	}

	public List getSelectedTests() {
		int[] selectedIndex = getSelectedRows();
		TestResultsTableModel model = (TestResultsTableModel)getModel();
		List selectedTests = new ArrayList();
		
		for (int i=0; i<selectedIndex.length; i++)
			selectedTests.add(model.getTestByRowIndex(selectedIndex[i]));
			
		return selectedTests;
	}
}