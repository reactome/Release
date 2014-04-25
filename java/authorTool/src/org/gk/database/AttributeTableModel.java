package org.gk.database;

import javax.swing.table.AbstractTableModel;

import org.gk.schema.SchemaClass;

/**
 * This table model is used as abstract class for AttributeTable that can be
 * used to display Instance attribute values in a multiple cell span way. The client
 * class that wants to use AttibuteTable needs implementing this
 * AttributeTableModel class.
 * @author wugm
 */
public abstract class AttributeTableModel extends AbstractTableModel {
    
	public abstract CellSpan getCellSpan();
    
    public Class getColumnClass(int col) {
        return String.class; // Default to use String as column class type.
    }
    
    /**
     * Get the SchemaClass object for this AttributeTableModel.
     * @return
     */
    public abstract SchemaClass getSchemaClass();

}