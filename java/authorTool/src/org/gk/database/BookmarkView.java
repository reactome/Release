/*
 * Created on Feb 17, 2004
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.gk.graphEditor.ArrayListTransferable;
import org.gk.model.Bookmark;
import org.gk.model.GKInstance;
import org.gk.persistence.Bookmarks;
import org.gk.util.GKApplicationUtilities;

/**
 * This class is used to list the bookmarks for the Instance objects.
 * @author wugm
 */
public class BookmarkView extends JPanel {
	private final String[] tableHeaders = {"Display Name", "DB_ID", "Type", "Description"};
	private JTable table;
	private JButton closeBtn;
	private JComboBox sortingKeyBox;
	private String sortingKey;
	// Actions
	private Action deleteAction;
	private Action goToAction;
	private Bookmarks bookmarks;
	
	public BookmarkView() {
		init();
		sortingKey = "displayName";
	}
	
	private void init() {
		initActions();
		setLayout(new BorderLayout());
		JPanel northPane = new JPanel();
		northPane.setLayout(new BorderLayout());
		JLabel titleLabel = new JLabel("Bookmarks");
		titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
		northPane.add(titleLabel, BorderLayout.WEST);
		// Add action buttons
		Dimension btnSize = new Dimension(20, 20);
		JToolBar toolbar = new JToolBar();
		JLabel label = new JLabel("Sort by ");
		sortingKeyBox = new JComboBox();
		for (int i = 0; i < tableHeaders.length; i++)
			sortingKeyBox.addItem(tableHeaders[i]);
		sortingKeyBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					int selectedIndex = sortingKeyBox.getSelectedIndex();
					sort(selectedIndex);
				}
			}
		});
		toolbar.add(label);
		toolbar.add(sortingKeyBox);
		toolbar.addSeparator();
		JButton btn = toolbar.add(goToAction);
		btn.setPreferredSize(btnSize);
		btn = toolbar.add(deleteAction);
		btn.setPreferredSize(btnSize);
		closeBtn = new JButton(GKApplicationUtilities.createImageIcon(getClass(),"Close.gif"));
		closeBtn.setToolTipText("Close");
		closeBtn.setPreferredSize(btnSize);
		toolbar.add(closeBtn);
		northPane.add(toolbar, BorderLayout.EAST);
		add(northPane, BorderLayout.NORTH);
		// Add the table
		table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = table.rowAtPoint(e.getPoint());
					Bookmark bookmark = bookmarks.getBookmark(row);
					firePropertyChange("bookmark", null, bookmark);
				}
			}
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					doTablePopup(e);
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					doTablePopup(e);
			}
		});
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (table.getSelectedRowCount() == 0) {
					deleteAction.setEnabled(false);
					goToAction.setEnabled(false);
				}
				else {
					deleteAction.setEnabled(true);
					goToAction.setEnabled(true);
				}
			}
		});
		table.getTableHeader().setReorderingAllowed(false);
		table.setDragEnabled(true);
		table.setTransferHandler(new BookmarkTransferHandler());
		// Click to sorting
		table.getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int col = table.columnAtPoint(e.getPoint());
				sortingKeyBox.setSelectedIndex(col);
			}
		});
		BookmarkTableModel model = new BookmarkTableModel();
		table.setModel(model);
		add(new JScrollPane(table), BorderLayout.CENTER);
		int width = 800;
		TableColumn col = table.getColumnModel().getColumn(0);
		col.setPreferredWidth((int)(width * 0.4));
		col = table.getColumnModel().getColumn(1);
		col.setPreferredWidth((int)(width * 0.15));
		col = table.getColumnModel().getColumn(2);
		col.setPreferredWidth((int)(width * 0.15));
		col = table.getColumnModel().getColumn(3);
		col.setPreferredWidth((int)(width * 0.3));
	}
	
	private void initActions() {
		deleteAction = new AbstractAction("Delete",
		                GKApplicationUtilities.createImageIcon(getClass(),"Remove16.gif")) {
			public void actionPerformed(ActionEvent e) {
				delete();
			}
		};
		deleteAction.putValue(Action.SHORT_DESCRIPTION, "Delete");
		goToAction = new AbstractAction("Go To",
		                GKApplicationUtilities.createImageIcon(getClass(),"GoTo.gif")) {
			public void actionPerformed(ActionEvent e) {
				int row = table.getSelectedRow();
				if (row == -1)
					return;
				Bookmark bookmark = bookmarks.getBookmark(row);
				BookmarkView.this.firePropertyChange("bookmark", null, bookmark);
			}
		};
		goToAction.putValue(Action.SHORT_DESCRIPTION, "Go To");
		// Default should be disabled
		deleteAction.setEnabled(false);
		goToAction.setEnabled(false);
	}
	
	private void delete() {
		int row = table.getSelectedRow();
		if (row == -1)
			return;
		bookmarks.removeBookmark(row);
		BookmarkTableModel model = (BookmarkTableModel) table.getModel();
		model.fireTableRowsDeleted(row, row);
		if (row >= 0)
			table.getSelectionModel().addSelectionInterval(row, row);
	}
	
	private void doTablePopup(MouseEvent e) {
		if (table.getSelectedRow() == -1)
			return;
		JPopupMenu popup = new JPopupMenu();
		popup.add(goToAction);
		popup.add(deleteAction);
		popup.show(table, e.getX(), e.getY());
	}
	
	public void addCloseAction(ActionListener l) {
		closeBtn.addActionListener(l);
	}
	
	public void addBookmark(GKInstance instance) {
		int index = bookmarks.addBookmark(instance);
		BookmarkTableModel model = (BookmarkTableModel) table.getModel();
		model.fireTableRowsInserted(index, index);
		table.getSelectionModel().addSelectionInterval(index, index);
	}
	
	/**
	 * Delete a Bookmark for the specified GKInstance object.
	 * @param instance
	 */
	public void deleteBookmark(GKInstance instance) {
		int index = bookmarks.deleteBookmark(instance);
		if (index >= 0) {
			BookmarkTableModel model = (BookmarkTableModel) table.getModel();
			model.fireTableRowsDeleted(index, index);
		}
	}
	
	/**
	 * Set the bookmarks for displaying. Sorting is not performed. The supplied
	 * list should be sorted already.
	 * @param newBookmarks
	 */
	public void setBookmarks(Bookmarks bks) {
		bookmarks = bks;
		refresh();
	}
	
	public void refresh() {
		BookmarkTableModel model = (BookmarkTableModel) table.getModel();
		model.fireTableDataChanged();
	}
	
	public Bookmarks getBookmarks() {
		return bookmarks;
	}
	
	public String getSortingKey() {
		return sortingKey;
	}
	
	public void setSortingKey(String sortingKey) {
		this.sortingKey = sortingKey;
		int selectedIndex = 0;
		if (sortingKey.equals("displayName"))
			selectedIndex = 0;
		else if (sortingKey.equals("DB_ID"))
			selectedIndex = 1;
		else if (sortingKey.equals("type"))
			selectedIndex = 2;
		else if (sortingKey.equals("desc"))
		    selectedIndex = 3;
		sortingKeyBox.setSelectedIndex(selectedIndex);
	}
	
	public void sort(int col) {
		Comparator sorter = null;
		if (col == 0) {
			sortingKey = "displayName";
			sorter = new Comparator() {
				public int compare(Object obj1, Object obj2) {
					Bookmark mark1 = (Bookmark) obj1;
					Bookmark mark2 = (Bookmark) obj2;
					return mark1.getDisplayName().compareTo(mark2.getDisplayName());
				}
			};
		}
		else if (col == 1) {
			sortingKey = "DB_ID";
			sorter = new Comparator() {
				public int compare(Object obj1, Object obj2) {
					Bookmark mark1 = (Bookmark) obj1;
					Bookmark mark2 = (Bookmark) obj2;
					return mark1.getDbID().compareTo(mark2.getDbID());
				}
			};
		}
		else if (col == 2) {
			sortingKey = "type";
			sorter = new Comparator() {
				public int compare(Object obj1, Object obj2) {
					Bookmark mark1 = (Bookmark) obj1;
					Bookmark mark2 = (Bookmark) obj2;
					return mark1.getType().compareTo(mark2.getType());
				}
			};
		}
		else if (col == 3) {
		    sortingKey = "desc";
		    sorter = new Comparator() {
		        public int compare(Object obj1, Object obj2) {
		            Bookmark mark1 = (Bookmark) obj1;
		            Bookmark mark2 = (Bookmark) obj2;
		            String desc1 = mark1.getDescription();
		            if (desc1 == null)
		                desc1 = "";
		            String desc2 = mark2.getDescription();
		            if (desc2 == null)
		                desc2 = "";
		            return desc1.compareTo(desc2);
		        }
		    };
		}
		bookmarks.sort(sorter);
		BookmarkTableModel model = (BookmarkTableModel) table.getModel();
		model.fireTableDataChanged();
	}
	
	class BookmarkTableModel extends AbstractTableModel {
		BookmarkTableModel() {
		}
		
		public String getColumnName(int col) {
			return tableHeaders[col];
		}	
		
		public int getColumnCount() {
			return tableHeaders.length;
		}
		
		public int getRowCount() {
			return bookmarks == null ? 0 : bookmarks.size();
		}
		
		public boolean isCellEditable(int row, int col) {
		    if (col == 3)
		        return true;
		    return false;
		}
		
		public Object getValueAt(int row, int col) {
			if (row < 0 || row > bookmarks.size() - 1)
				return null;
			Bookmark bookmark = (Bookmark) bookmarks.getBookmark(row);
			switch (col) {
				case 0 :
					return bookmark.getDisplayName();
				case 1 :
					return bookmark.getDbID();
				case 2 :
					return bookmark.getType();
				case 3 :
				    return bookmark.getDescription();
			}
			return null;
		}
		
		public void setValueAt(Object value, int row, int col) {
		    if (col == 3) {
		        Bookmark bookmark = (Bookmark) bookmarks.getBookmark(row);
		        String str = value.toString().trim();
		        if (str.length() == 0)
		            bookmark.setDescription(null);
		        else
		            bookmark.setDescription(str);
		    }
		}
	}
	
	/**
	 * To enable DnD in the table for adding values to AttributePane.
	 */
	class BookmarkTransferHandler extends TransferHandler {
	    
	    /**
	     *@return an ArrayListTransferable object containing a list of
	     *Renderable objects that are converted from GKInstances.
	     */
	    protected Transferable createTransferable(JComponent c) {
	        JTable table = (JTable) c;
	        int[] selectedRows = table.getSelectedRows();
	        // Get the selected bookmarks
	        if (selectedRows != null && selectedRows.length > 0) {
	            ArrayList selectedBookmarks = new ArrayList(selectedRows.length);
	            Bookmarks allBookmarks = getBookmarks();
	            Bookmark bookmark = null;
	            for (int i = 0; i < selectedRows.length; i++) {
	                bookmark = (Bookmark) allBookmarks.getBookmark(selectedRows[i]);
	                selectedBookmarks.add(bookmark);
	            }
	            ArrayListTransferable transferrable = new ArrayListTransferable(selectedBookmarks);
	            DataFlavor dataFlavor = new DataFlavor(Bookmarks.class, "Bookmarks");
	            transferrable.setDataFlavor(dataFlavor);
	            return transferrable;
	        }
	        return null;
	    }
	    
	    /**
	     * Only copy is allowed for DnD.
	     */
	    public int getSourceActions(JComponent c) {
	        return COPY; 
	    }
	}
}