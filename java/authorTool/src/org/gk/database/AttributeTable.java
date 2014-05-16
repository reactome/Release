/*
 * Created on Dec 8, 2003
 */
package org.gk.database;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.TableUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.gk.model.GKInstance;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;
/**
 * The multi-span table is modified from http://www.codeguru.com/java/articles/139.shtml.
 * by Nobuo Tamemasa
 */ 
public class AttributeTable extends JTable {
	
	private AttributeTableUI ui = null;
	private int[] grayRows = new int[0]; // used for emphasizing parts of table
	private int[] darkGrayRows = new int[0]; // used for emphasizing parts of table

	/**
	 * @return Returns the darkGrayRows.
	 */
	public int[] getDarkGrayRows() {
		return darkGrayRows;
	}
	/**
	 * @param darkGrayRows The darkGrayRows to set.
	 */
	public void setDarkGrayRows(int[] darkGrayRows) {
		this.darkGrayRows = darkGrayRows;
	}
	/**
	 * @return Returns the grayRows.
	 */
	public int[] getGrayRows() {
		return grayRows;
	}
	/**
	 * @param grayRows The grayRows to set.
	 */
	public void setGrayRows(int[] grayRows) {
		this.grayRows = grayRows;
	}
	
	private boolean isGrayRow(int row) {
		for (int i=0;i<grayRows.length;i++)
			if (grayRows[i]==row)
				return true;
		return false;
	}

	private boolean isDarkGrayRow(int row) {
		for (int i=0;i<darkGrayRows.length;i++)
			if (darkGrayRows[i]==row)
				return true;
		return false;
	}

	public AttributeTable() {
		super(new NullAttributeTableModel());
		init();
	}

	/**
	 * Only PropertyTableUI can be used in PropertyTable objects.
	 */
	public void setUI(TableUI ui) {
		if (ui instanceof AttributeTableUI) {
			super.setUI(ui);
		}
	}
	
	private void init() {
		ui = new AttributeTableUI();
		setUI(ui);
		getTableHeader().setReorderingAllowed(false);
		setCellSelectionEnabled(true);
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		setDefaultRenderer(String.class, new PropertyCellRenderer());
//		setDefaultEditor(String.class, new PropertyCellEditor());
//		// To popup a new AttributePane
//		addMouseListener(this.pane.getCellMouseAdaptor());
//		// Don't allow pending editing. Commit all changes when the focus
////		// is moving out of the table.
//		addFocusListener(new FocusAdapter() {
//			public void focusLost(FocusEvent e) {
//				this.pane.stopEditing();
//			}
//		});
	}

	public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
		Rectangle sRect = super.getCellRect(row, column, includeSpacing);
		if ((row < 0) || (column < 0) || (getRowCount() <= row) || (getColumnCount() <= column)) {
			return sRect;
		}
		CellSpan cellSpan = (CellSpan) ((AttributeTableModel)getModel()).getCellSpan();
		row = cellSpan.getActualRow(row, column);
		int span = cellSpan.getSpan(row, column);
		int index = 0;
		int columnMargin = getColumnModel().getColumnMargin();
		Rectangle cellFrame = new Rectangle();
		int aCellHeight = rowHeight + rowMargin;
		cellFrame.y = row * aCellHeight;
		cellFrame.height = span * aCellHeight;
		Enumeration enumeration = getColumnModel().getColumns();
		while (enumeration.hasMoreElements()) {
			TableColumn aColumn = (TableColumn)enumeration.nextElement();
			cellFrame.width = aColumn.getWidth() + columnMargin;
			if (index == column)
				break;
			cellFrame.x += cellFrame.width;
			index++;
		}

		if (!includeSpacing) {
			Dimension spacing = getIntercellSpacing();
			cellFrame.setBounds(
				cellFrame.x + spacing.width / 2,
				cellFrame.y + spacing.height / 2,
				cellFrame.width - spacing.width,
				cellFrame.height - spacing.height);
		}
		return cellFrame;
	}

	public int rowAtPoint(Point point) {
		int row = point.y / (rowHeight + rowMargin);
		if ((row < 0) || (getRowCount() <= row))
			return row;
		int column = getColumnModel().getColumnIndexAtX(point.x);
		CellSpan cellSpan = ((AttributeTableModel)getModel()).getCellSpan();
		return cellSpan.getActualRow(row, column);
	}
	
	public void columnSelectionChanged(ListSelectionEvent e) {
		repaint();
	}
	
	/**
	 * Have to use an implementation of AttributeTableModel. Otherwise ClassCastException
	 * will be thrown.
	 */
	public void setModel(TableModel model) {
		if (!(model instanceof AttributeTableModel)) {
			throw new IllegalArgumentException("AttributeTableModel.setModel() " + 
			          "model has to be an AttributeTableModel.");
		}
		super.setModel(model);
	}

	public void valueChanged(ListSelectionEvent e) {
		int firstIndex = e.getFirstIndex();
		int lastIndex = e.getLastIndex();
		if (firstIndex == -1 && lastIndex == -1) { // Selection cleared.
			repaint();
		}
		Rectangle dirtyRegion = getCellRect(firstIndex, 0, false);
		int numCoumns = getColumnCount();
		int index = firstIndex;
		for (int i = 0; i < numCoumns; i++) {
			dirtyRegion.add(getCellRect(index, i, false));
		}
		index = lastIndex;
		for (int i = 0; i < numCoumns; i++) {
			dirtyRegion.add(getCellRect(index, i, false));
		}
		repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height);
	}
	
	class PropertyCellRenderer extends DefaultTableCellRenderer {
		// Icon for table renderer
		private Icon instanceIcon = GKApplicationUtilities.createImageIcon(getClass(), "Instance.gif");
		private Icon mandatoryIcon = GKApplicationUtilities.createImageIcon(getClass(), "Mandatory.gif");
        private Icon requiredIcon = GKApplicationUtilities.createImageIcon(getClass(), "Required.gif");
        private Icon optionalIcon = GKApplicationUtilities.createImageIcon(getClass(), "Optional.gif");
        private Icon noManualEditIcon = GKApplicationUtilities.createImageIcon(getClass(), "NoManualEdit.gif");
        private Color grayColor = null;
        private Color darkGrayColor = null;
        
		public PropertyCellRenderer() {
			super();
		}

		public Component getTableCellRendererComponent(
			JTable table,
			Object value,
			boolean isSelected,
			boolean hasFocus,
			int row,
			int col) {
		    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		    // When a cell can be editable, the color cannot be set correctly by the
		    // super method, and have to reset here.
		    if (isSelected || hasFocus) {
		        comp.setBackground(table.getSelectionBackground());
		        comp.setForeground(table.getSelectionForeground());
		    }
		    else {
		        Color background = table.getBackground();
                // This method will be called multiple times. Use this lazy-initialization to avoid
                // creating two many Color objects for GC.
                if (grayColor == null || darkGrayColor == null) {
                    int red = background.getRed();
                    int green = background.getGreen();
                    int blue = background.getBlue();
                    int lightIncrement = 20;
                    int darkIncrement = 2*lightIncrement;
                    grayColor = new Color(red - lightIncrement,
                                          green - lightIncrement,
                                          blue - lightIncrement);
                    darkGrayColor = new Color(red - darkIncrement,
                                              green - darkIncrement,
                                              blue - darkIncrement);
                }
		        if (isDarkGrayRow(row)) {
		            comp.setBackground(darkGrayColor);
		        } 
                else if (isGrayRow(row)) {
		            comp.setBackground(grayColor);
		        }
                else
                    comp.setBackground(background);
		        // Original code
		        comp.setForeground(table.getForeground());
		    }
		    String text = null;
		    if (value instanceof GKInstance) {
		        GKInstance instance = (GKInstance)value;
		        text = instance.getDisplayName();
		        if (text == null || (text != null && text.length() == 0)) {
		            text = instance.getExtendedDisplayName();
		        }
		        setIcon(instanceIcon);
		    }
		    else if (value instanceof String) {
		        text = value.toString();
		        //text = InstanceUtilities.encodeLineSeparators(text);
		        setIcon(null);
		    }
		    else if (value != null) {
		        text = value.toString();
		        setIcon(null);
		    }
            else {
		        text = "";
		        setIcon(null);
		    }
		    setText(text);
		    // Use extended displayName for tooltip
		    String toolTipText = null;
		    if (value instanceof GKInstance)
		        toolTipText = ((GKInstance)value).getExtendedDisplayName();
		    else
		        toolTipText = text;
		    if (toolTipText != null && toolTipText.length() > 0 && col == 1)
		        setToolTipText(toolTipText);
		    else
		        setToolTipText(null);
		    // Handling something special for the first column, Property Name.
		    AttributeTableModel model = (AttributeTableModel) table.getModel();
		    SchemaClass cls = model.getSchemaClass();
		    if (col == 0) {
		        // Set Font
		        Font font = comp.getFont();
		        Font boldFont = font.deriveFont(Font.BOLD);
		        comp.setFont(boldFont);
		        if (cls != null) {
		            // Set icons for attributes based on their categories
		            try {
                        String attName = value.toString();
                        if (cls.isValidAttribute(attName)) {
                            SchemaAttribute att = cls.getAttribute(value.toString());
                            if (att.getCategory() == SchemaAttribute.MANDATORY)
                                setIcon(mandatoryIcon);
                            else if (att.getCategory() == SchemaAttribute.REQUIRED)
                                setIcon(requiredIcon);
                            else if (att.getCategory() == SchemaAttribute.OPTIONAL)
                                setIcon(optionalIcon);
                            else if (att.getCategory() == SchemaAttribute.NOMANUALEDIT)
                                setIcon(noManualEditIcon);
                            // null has been set above. No need to handle the default case.
                        }
		            }
		            catch(Exception e) {
		                System.err.println("AttributeTable.PropertyCellRenderer.getTableCellRenderere(): " + e);
		                e.printStackTrace();
		            }
		        }
		    }
		    return comp;
		}
	}
	
	/**
	 * A null implementaion.
	 */
	static class NullAttributeTableModel extends AttributeTableModel {
        
		public NullAttributeTableModel() {
		}
		public CellSpan getCellSpan() {
			return new CellSpan();
		}
		public int getColumnCount() {
			return 0;
		}
		public int getRowCount() {
			return 0;
		}
		public Object getValueAt(int row, int col) {
			return null;
		}
        
        public SchemaClass getSchemaClass() {
            return null;
        }
	}	
}