/*
 * Created on May 22, 20056
 */
package org.gk.IDGeneration;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.tree.TreePath;

/** 
 *  Reacts to the buttons in IncludeInstancesPane
 * @author croft
 */
public class IncludeInstancesController {
	private IncludeInstances includeInstances;
	private IncludeInstancesPane includeInstancesPane = null;
	private Action defaultSelectionAction = null;
	private Action deselectAllAction = null;
	private Action copyAction = null;
	private Action pasteAction = null;
	private TreePath[] copiedPaths = null;
	
	public IncludeInstancesController(IncludeInstancesPane includeInstancesPane) {
		includeInstances = new IncludeInstances();
		this.includeInstancesPane = includeInstancesPane;
	}
	
	/**
	 * Selects commonly used schema classes automatically, e.g.
	 * "Pathway".
	 *
	 */
	private void defaultSelection() {
		includeInstancesPane.setSelectedPaths(includeInstances.getDefaultClasses());
	}
	
	public Action getDefaultSelectionAction() {
		if (defaultSelectionAction == null) {
			defaultSelectionAction = new AbstractAction("Default") {
				public void actionPerformed(ActionEvent e) {
					defaultSelection();
				}
			};
		}
		return defaultSelectionAction;
	}
	
	/**
	 * Deselect all schema classes
	 *
	 */
	private void deselectAll() {
		includeInstancesPane.deselectAllClasses();
	}
	
	public Action getDeselectAllAction() {
		if (deselectAllAction == null) {
			deselectAllAction = new AbstractAction("Deselect") {
				public void actionPerformed(ActionEvent e) {
					deselectAll();
				}
			};
		}
		return deselectAllAction;
	}
	
	/**
	 * Deselect all schema classes
	 *
	 */
	private void copy() {
		copiedPaths = includeInstancesPane.getSelectedPaths();
		
		System.out.println("copiedPaths=");
		for (int i=0; i<copiedPaths.length; i++)
			System.out.println("     " + copiedPaths[i].toString());
	}
	
	public Action getCopyAction() {
		if (copyAction == null) {
			copyAction = new AbstractAction("Copy") {
				public void actionPerformed(ActionEvent e) {
					copy();
				}
			};
		}
		return copyAction;
	}
	
	/**
	 * Deselect all schema classes
	 *
	 */
	private void paste() {
		includeInstancesPane.setSelectedPaths(copiedPaths);
	}
	
	public Action getPasteAction() {
		if (pasteAction == null) {
			pasteAction = new AbstractAction("Paste") {
				public void actionPerformed(ActionEvent e) {
					paste();
				}
			};
		}
		return pasteAction;
	}
}