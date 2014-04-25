/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

/** 
 *  Constructor takes a list of tests and provides methods for displaying these in a table.
 * @author croft
 */
public class TestResultsTableModel extends AbstractTableModel {
	private List testList = null;
	private IDGeneratorTests tests = null;
	
	// Caches
	private Map names = new HashMap();
	private Map instances = new HashMap();
	private Map curators = new HashMap();

	public TestResultsTableModel() {
	}
	
	/**
	 * Import the data that forms the basis for the table model.
	 * 
	 * @param tests
	 */
	public void setTests(IDGeneratorTests tests) {
		this.tests = tests;
		
		if (tests==null)
			return;
		
		// Empty cache
		names = new HashMap();
		instances = new HashMap();
		curators = new HashMap();

		testList = new ArrayList(tests.getTests().values());
		Collections.sort(testList, new TestComparator());
		fireTableDataChanged();
	}
	
	public IDGeneratorTests getTests() {
		return tests;
	}

	public Class getColumnClass(int columnIndex) {
		if (columnIndex==0)
			return String.class;
		else if (columnIndex==1)
			return Integer.class;
		else if (columnIndex==2)
			return Integer.class;
		
		return String.class; // should never get here
	}
	
	public int getColumnCount() {
		return 3;
	}

	/**
	 * First column: test name
	 * Second column: number of instances
	 * Third column: number of curators
	 */
	public int getRowCount() {
		if (testList==null)
			return 0;
		else
			return testList.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (testList==null)
			return null;
		
		// Get values from cache, if available.
		Integer integerColumnIndex = new Integer(columnIndex);		
		if (columnIndex==0) {
			if (names.get(integerColumnIndex)!=null)
				return names.get(integerColumnIndex);
			else {
				IDGeneratorTest test = getTestByRowIndex(rowIndex);
				return test.getName();
			}
		} else if (columnIndex==1) {
			if (instances.get(integerColumnIndex)!=null)
				return instances.get(integerColumnIndex);
			else {
				IDGeneratorTest test = getTestByRowIndex(rowIndex);
				return new Integer(test.getInstanceCount());
			}
		} else if (columnIndex==2) {
			if (curators.get(integerColumnIndex)!=null)
				return curators.get(integerColumnIndex);
			else {
				IDGeneratorTest test = getTestByRowIndex(rowIndex);
				return new Integer(test.getCuratorCount());
			}
		} else
			System.err.println("TestResultsTableModel.getValueAt: unknown columnIndex=" + columnIndex);
		
		return null;
	}
	
    public String getColumnName(int columnIndex) {
    	if (columnIndex==0)
    		return "Name";
    	else if (columnIndex==1)
    		return "Instances";
    	else if (columnIndex==2)
    		return "Curators";
    	else
    		return ""; // Should never get here
    }
    
    public IDGeneratorTest getTestByRowIndex(int rowIndex) {
    	return (IDGeneratorTest)testList.get(rowIndex);
    }
    
	/**
	 * Used for comparing pairs of tests, to determine
	 * their ordering in a sort.
	 * 
	 * @author croft
	 *
	 */
	class TestComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			IDGeneratorTest test1 = null;
			IDGeneratorTest test2 = null;
			try {
				test1 = (IDGeneratorTest)o1;
				test2 = (IDGeneratorTest)o2;
			} catch (ClassCastException e) {
				System.err.println("TestResultsTableModel.TestComparator.compare(): One of the objects for comparison does not have type IDGeneratorTest");
				e.printStackTrace();
				return 0;
			}
			
			String testName1 = test1.getName();
			String testName2 = test2.getName();
			
			// It doesn't make sense to try sorting things that don't
			// have names.
			if (testName1==null || testName2==null)
				return 0;
			
			// Do a simple lexicographic comparison of the attribute
			// names.
			return testName1.compareToIgnoreCase(testName2);
		}
	}
	
}