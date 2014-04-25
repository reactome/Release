/*
 * Created on Dec 8, 2003
 */
package org.gk.database;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * This class is used to define the cell span. This is a very specialized version 
 * for our property table.  It provides a table view that allows attributes to
 * have multiple values.
 */
public class CellSpan {

	private int[][] spans;

	public CellSpan() {
		spans = new int[1][1];
	}

	public CellSpan(int row, int col) {
		spans = new int[row][col];
	}
	
	public void reset() {
		spans = new int[1][1];
	}

	public int getSpan(int row, int column) {
		if (row < 0 || row > spans.length - 1)
			return 1; // Default
		if (column < 0 || column > spans[row].length - 1)
			return 1;
		return spans[row][column];
	}

	public void setSpan(int span, int row, int column) {
		spans[row][column] = span;
	}

	public boolean isVisible(int row, int column) {
		if (row < 0 || row > spans.length - 1)
			return false;
		if (column <0 || column > spans[row].length - 1)
			return false;
		if (spans[row][column] == 0)
			return false;
		return true;
	}

	public int getActualRow(int row, int col) {
		if (isVisible(row, col))
			return row;
		for (int i = row - 1; i >= 0; i--) {
			int span = spans[i][col];
			if (span > 0)
				return i;
		}
		return 0;
	}
	
	public void initSpans(Map valueMap1, Map valueMap2, java.util.List keys) {
		int col = 3;
		int row = 0;
		// Calculate the total row number
		for (Iterator it = keys.iterator(); it.hasNext();) {
			Object key = it.next();
			Object value1 = valueMap1.get(key);
			Object value2 = valueMap2.get(key);
			int row1 = 1;
			if (value1 instanceof Collection) {
				Collection c = (Collection) value1;
				if (c.size() > row1)
					row1 = c.size();
			}
			if (value2 instanceof Collection) {
				Collection c = (Collection) value2;
				if (c.size() > row1)
					row1 = c.size();
			}
			row += row1;
		}
		spans = new int[row][col];
		// Set the cell spans
		row = 0;
		for (Iterator it = keys.iterator(); it.hasNext();) {
			Object key = it.next();
			Object value1 = valueMap1.get(key);
			Object value2 = valueMap2.get(key);
			int size = 1;
			if (value1 instanceof Collection) {
				Collection c = (Collection) value1;
				if (c.size() > size)
					size = c.size();
			}
			if (value2 instanceof Collection) {
				Collection c = (Collection) value2;
				if (c.size() > size)
					size = c.size();
			}
			spans[row][0] = size;
			for (int i = 0; i < size; i++) {
				spans[row + i][1] = spans[row + i][2] = 1;
			}
			row += size;
		}	
	}

	public void initSpans(Map valueMap, java.util.List keys) {
		initSpansWithEmptyCells(valueMap, null, keys);
	}
	
	/**
	 * If emptyCellMap is non-null, then extra space will be made for empty
	 * cells.
	 * 
	 * TODO: there could be conflicts between tables constructed this way
	 * and tables constructed with the
	 * 
	 * initSpans(Map valueMap1, Map valueMap2, java.util.List keys)
	 * 
	 * method.
	 * 
	 * TODO: this method is *really* similar to the setValues method of
	 * AttributePane.PropertyTableModel. Couldn't a common method be
	 * made available underlying both?
	 * 
	 * @param valueMap
	 * @param emptyCellMap
	 * @param keys
	 */
	public void initSpansWithEmptyCells(Map valueMap, Map emptyCellMap, java.util.List keys) {
		int col = 2;
		int row = 0;
		// Calculate the total row number
		for (Iterator it = valueMap.keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			Object value = valueMap.get(key);
			if (value instanceof Collection) {
				Collection c = (Collection)value;
				row += (c.size() == 0 ? 1 : c.size());
			}
			else
				row++;
			
			// Increment row counter if an empty cell is present
			if (emptyCellMap!=null && emptyCellMap.get(key)!=null)
				row++;
		}
		spans = new int[row][col];
		// Set the cell spans
		row = 0;
		int row0 = (-1);
		for (Iterator it = keys.iterator(); it.hasNext();) {
			Object key = it.next();
			Object value = valueMap.get(key);
			row0 = row;
			if (value instanceof Collection) {
				Collection c = (Collection)value;
				int size = c.size() == 0 ? 1 : c.size();
				spans[row][0] = size;
				for (int i = 0; i < size; i++) {
					spans[row + i][1] = 1;
				}
				row += size;
			}
			else {
				spans[row][0] = 1;
				spans[row][1] = 1;
				row++;
			}
            
            // Put in an empty cell, if one is required
			if (emptyCellMap!=null && emptyCellMap.get(key)!=null) {
				if (row0 != (-1))
					spans[row0][0]++;
				spans[row][1] = 1;
				row++;
			}
		}
	}
	
	public int getRowNumber() {
		return spans.length;
	}
	
	public int getColumNumber() {
		return spans[0].length;
	}
}