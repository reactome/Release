/*
 * Created on Sep 4, 2003
 */
package org.gk.graphEditor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;

/**
 * A Transferable for ArrayList objects.
 * @author wgm
 */
public class ArrayListTransferable implements Transferable {
	private ArrayList data;
	private DataFlavor dataFlavor;

	/**
	 * The sole constructor.
	 * @param alist the first element is a String value indicate the source
	 * of the data.
	 */
	public ArrayListTransferable(ArrayList alist) {
		data = alist;
		dataFlavor = new DataFlavor(ArrayList.class, "ArrayList");
	}
	
	public void setDataFlavor(DataFlavor dataFlavor) {
	    this.dataFlavor = dataFlavor;
	}
	
	public Object getTransferData(DataFlavor flavor)
							 throws UnsupportedFlavorException {
		if (!isDataFlavorSupported(flavor)) {
			throw new UnsupportedFlavorException(flavor);
		}
		return data;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {dataFlavor};									  
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		if (dataFlavor.equals(flavor)) {
			return true;
		}
		return false;
	}
}
