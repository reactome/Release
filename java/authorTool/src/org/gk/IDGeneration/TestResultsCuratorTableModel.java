/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.gk.model.GKInstance;

/** 
 * Extracts the curators from a set of tests and presents the
 * information in a form suitable for generating a table.
 * @author croft
 */
public class TestResultsCuratorTableModel extends AbstractTableModel {
	private List testList;
	private List curators;
	private Map instanceCounts; // cache
	
	public TestResultsCuratorTableModel() {
		instanceCounts = new HashMap();
	}
	
	/**
	 * Import the data that forms the basis for the table model.
	 * 
	 * @param tests
	 * 
	 * Returns the number of curators found in the supplied tests.
	 */
	public int setTestList(List testList) {
		this.testList = testList;
		
		if (testList==null)
			return 0;
		
		IDGeneratorTest test;
		GKInstance testCurator;
		List testCurators;
		curators = new ArrayList();
		for (Iterator it = testList.iterator(); it.hasNext();) {
			test = (IDGeneratorTest)it.next();
			testCurators = test.getCurators();
			for (Iterator itc = testCurators.iterator(); itc.hasNext();) {
				testCurator = (GKInstance)itc.next();
				if (!curators.contains(testCurator))
					curators.add(testCurator);
			}
		}

		Collections.sort(curators, new CuratorComparator());
		fireTableDataChanged();
		
		return curators.size();
	}

	public Class getColumnClass(int columnIndex) {
		if (columnIndex==0)
			return String.class;
		else if (columnIndex==1)
			return Integer.class;
		
		return String.class; // should never get here
	}
	
	public int getColumnCount() {
		return 2;
	}

	/**
	 * First column: test name
	 * Second column: number of instances
	 * Third column: number of curators
	 */
	public int getRowCount() {
		if (curators==null)
			return 0;
		else
			return curators.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (curators==null)
			return null;
		
		GKInstance curator = getCuratorByRowIndex(rowIndex);
		
		if (columnIndex==0)
			return curator.getDisplayName();
		else if (columnIndex==1) {
			// Use cached value, if available
			Integer integerInstanceCount = null;
			try {
				integerInstanceCount = (Integer)instanceCounts.get(new Integer(rowIndex));
			} catch (IndexOutOfBoundsException e1) {
				// This exception gets thrown if rowIndex has not yet been encountered
			}
			if (integerInstanceCount!=null)
				return integerInstanceCount;

			if (curator.getDBID()==null)
				return new Integer(0);
			
			long dbId = curator.getDBID().longValue();
			Long instanceDbId;
			List dbIds;
			IDGeneratorTest test;
			GKInstance instance;
			List curators = new ArrayList();
			GKInstance instanceCurator;
			GKInstance author;
			List authors;
			int instanceCount = 0;
			boolean authorFound;
			Map instanceMap = new HashMap();
			for (Iterator itt = testList.iterator(); itt.hasNext();) {
				test = (IDGeneratorTest)itt.next();
				dbIds = test.getDbIds();
				for (Iterator iti = dbIds.iterator(); iti.hasNext();) {
					instanceDbId = (Long)iti.next();
					instance = IDGeneratorTest.fetchInstance(instanceDbId);
					if (instance==null) {
						System.err.println("TestResultsTableModel.getValueAt: WARNING - instance is null, skipping!!");
						continue;
					}
					// Has this instance been seen already?
					// If yes, ignore.
					if (instanceMap.get(instance)!=null)
						continue;
					instanceMap.put(instance, instance);
					
					try {
						curators = IDGenerationUtils.getInstanceEdits(instance);
						
						// Bump up the instance counter each time the author matches curator.
						authors = new ArrayList();
						authorFound = false;
						for (Iterator itc = curators.iterator(); itc.hasNext();) {
							instanceCurator = (GKInstance)itc.next();
							authors = instanceCurator.getAttributeValuesList("author");
							for (Iterator ita = authors.iterator(); ita.hasNext();) {
								author = (GKInstance)ita.next();
								if (author!=null && author.getDBID()!=null && author.getDBID().longValue()==dbId) {
									instanceCount++;
									authorFound = true;
									break;
								}
							}
							if (authorFound)
								break;
						}
					} catch (Exception e) {
						System.err.println("TestResultsTableModel.getValueAt: WARNING - problem getting curators from created or edited slots");
						e.printStackTrace();
					}
				}				
			}
			
			integerInstanceCount = new Integer(instanceCount);
			instanceCounts.put(new Integer(rowIndex), integerInstanceCount); // Add to cache
			return integerInstanceCount;
		} else
			System.err.println("TestResultsTableModel.getValueAt: unknown columnIndex=" + columnIndex);
		
		return null;
	}
	
    public String getColumnName(int columnIndex) {
    	if (columnIndex==0)
    		return "Curator";
    	else if (columnIndex==1)
    		return "Number of instances";
    	else
    		return ""; // Should never get here
    }
    
    public GKInstance getCuratorByRowIndex(int rowIndex) {
    	return (GKInstance)curators.get(rowIndex);
    }
    
	/**
	 * Used for comparing pairs of tests, to determine
	 * their ordering in a sort.
	 * 
	 * @author croft
	 *
	 */
	class CuratorComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			GKInstance test1 = null;
			GKInstance test2 = null;
			try {
				test1 = (GKInstance)o1;
				test2 = (GKInstance)o2;
			} catch (ClassCastException e) {
				System.err.println("TestResultsTableModel.TestComparator.compare(): One of the objects for comparison does not have type IDGeneratorTest");
				e.printStackTrace();
				return 0;
			}
			
			String testName1 = test1.getDisplayName();
			String testName2 = test2.getDisplayName();
			
			// It doesn't make sense to try sorting things that don't
			// have names.
			if (testName1==null || testName2==null)
				return 0;
			
			// Do a simple lexicographic comparison of the attribute
			// names.
			return testName1.compareToIgnoreCase(testName2);
		}
	}

	public List getTestList() {
		return testList;
	}
}