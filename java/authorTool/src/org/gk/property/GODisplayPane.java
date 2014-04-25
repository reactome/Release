/*
 * Created on Aug 1, 2003
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPane is used to display GO terms.
 * @author wgm
 */
public class GODisplayPane extends JPanel {
	// For display
	private boolean isOKClicked;
	// GUIs
	private JTree goTree;
	private JTree treeViewer;
	private JLabel idLabel;
	private JList synonymList;
	// These GUIs for search
	private JTextField searchField;
	private JCheckBox containsBox;
	private JCheckBox caseBox;
	private JCheckBox allBox;
	// Data model
	private GODisplayModel model;
	
	public GODisplayPane() {
		init();
		model = new GODisplayModel();
		goTree.setModel(model.getTreeModel());
	}
	
	/**
	 * Set the GUIs.
	 */
	private void init() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEtchedBorder());
		JPanel leftPane = createLeftPane();
		JPanel treeViewer = createTreeViewer();
		JPanel propViewer = createPropPane();
		JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeViewer, propViewer);
		JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
		jsp.setDividerLocation(320);
		rightPane.setDividerLocation(400);
		jsp.setResizeWeight(0.55);
		add(jsp, BorderLayout.CENTER);
		installListeners();
		TreeCellRenderer renderer = new GOTermCellRenderer();
		goTree.setCellRenderer(renderer);
		this.treeViewer.setCellRenderer(renderer);
	}
	
	private void installListeners() {
		goTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				GOTerm term = null;
				TreePath[] paths = goTree.getSelectionPaths();
				if (paths == null)
					term = null;
				else if (paths.length == 1) 
					term = (GOTerm) paths[0].getLastPathComponent();
				else if (paths.length > 1) {
					GOTerm term1 = null;
					term = (GOTerm) paths[0].getLastPathComponent();
					// Check if all selected GOTerms are the same. If not,
					// don't display anything in the treeview.
					for (int i = 1; i < paths.length; i++) {
						term1 = (GOTerm) paths[i].getLastPathComponent();
						if (!term1.getName().equals(term.getName())) {
							term = null;
							break;
						}
					}
				}
				if (term == null) {
					treeViewer.setModel(null);
					idLabel.setText("");
					setSynonyms(null);
				}
				else {
					TreeModel model1 = model.buildModeForTerm(term);
					treeViewer.setModel(model1);
					for (int i = 0; i < treeViewer.getRowCount(); i++)
						treeViewer.expandRow(i);
					// Do manual selections
					TreePath path = null;
					for (int i = 0; i < treeViewer.getRowCount(); i++) {
						path = treeViewer.getPathForRow(i);
						GOTerm term1 = (GOTerm) path.getLastPathComponent();
						if (term1.getChildCount() == 0)
							treeViewer.addSelectionRow(i);
					}
					idLabel.setText(term.getId());
					setSynonyms(term.getSynonyms());
				}
			}
		});
	}
	
	private void setSynonyms(java.util.List synonyms) {
		DefaultListModel model = (DefaultListModel) synonymList.getModel();
		model.removeAllElements();
		if (synonyms != null) {
			for (Iterator it = synonyms.iterator(); it.hasNext();)
				model.addElement(it.next());
		}
	}
	
	private JPanel createLeftPane() {
		JPanel leftPane = new JPanel();
		leftPane.setLayout(new BorderLayout());
		goTree = new JTree();
		goTree.setModel(null);
		goTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		goTree.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				goTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			}
		});
		leftPane.add(new JScrollPane(goTree), BorderLayout.CENTER);
		JPanel searchPane = createSearchPane();
		leftPane.add(searchPane, BorderLayout.SOUTH);
		return leftPane;
	}
	
	private JPanel createSearchPane() {
		JPanel searchPane = new JPanel();
		Border border1 = BorderFactory.createEtchedBorder();
		Border border2 = BorderFactory.createTitledBorder(border1, "Find Terms");
		searchPane.setBorder(border2);
		// Create  search pane
		searchPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(2, 4, 2, 4);
		JLabel searchLabel = new JLabel("Search:");
		searchPane.add(searchLabel, constraints);
		searchField = new JTextField();
		constraints.gridx = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 0.5;
		searchPane.add(searchField, constraints);
		JButton findBtn = new JButton("Find");
		findBtn.setMnemonic('F');
		constraints.gridx = 2;
		constraints.weightx = 0.0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.WEST;
		searchPane.add(findBtn, constraints);
		JPanel pane1 = new JPanel();
		containsBox = new JCheckBox("Contains");
		pane1.add(containsBox);
		caseBox = new JCheckBox("Case Sensitive");
		caseBox.setSelected(true);
		pane1.add(caseBox);
		allBox = new JCheckBox("All");
		pane1.add(allBox);
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.gridwidth = 3;
		searchPane.add(pane1, constraints);
		// Add searching action
		ActionListener l = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				search(searchField.getText().trim(), true, containsBox.isSelected(),
				       caseBox.isSelected(), allBox.isSelected());
			}
		};
		findBtn.addActionListener(l);
		searchField.addActionListener(l);
		return searchPane;
	}
	
	public void select(String termName) {
		search(termName, false, false, false, true);
	}
	
	private void search(String text, boolean needDialog,
	                    boolean contains, boolean caseSensitive,
	                    boolean all) {
		if (text.length() == 0)
			return;
		java.util.List terms = new ArrayList();
		GOTerm root = (GOTerm) model.getTreeModel().getRoot();
		search(text, root, terms, contains, caseSensitive, all);
		if (terms.size() == 0) {
			if (needDialog) {
				JOptionPane.showMessageDialog(
					GODisplayPane.this,
					"No Terms Found!",
					"No Terms Found",
					JOptionPane.INFORMATION_MESSAGE);
			}
			return;
		}
		// Clear the selection
		goTree.clearSelection();
		if (terms.size() > 1)
			goTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
		java.util.List path = new ArrayList();
		int index = 0;
		TreePath firstPath = null;
		for (Iterator it = terms.iterator(); it.hasNext();) {
			GOTerm term = (GOTerm)it.next();
			// Create the tree path
			path.clear();
			GOTerm parent = term.getParent();
			path.add(term);
			while (parent != null) {
				path.add(0, parent);
				parent = parent.getParent();
			}
			TreePath treePath = new TreePath(path.toArray());
			goTree.addSelectionPath(treePath);
			// Remove the last one since the found term don't need to be opened.
			path.remove(path.size() - 1);
			if (path.size() > 0) {
				treePath = new TreePath(path.toArray());
				goTree.expandPath(treePath);
			}
			if (index == 0) {
				firstPath = treePath;
			}
			index ++;
		}
		// Scroll
		int row = goTree.getRowForPath(firstPath);
		goTree.scrollRectToVisible(goTree.getRowBounds(row));
	}
	
	private boolean search(String text, GOTerm term, java.util.List list,
	                    boolean contains, boolean caseSensitive, boolean all) {
		// Comparison
		String name = term.getName();
		boolean found = false;
		if (contains) {
			if (caseSensitive) {
				if (name.indexOf(text) > -1)
					found = true;
			}
			else {
				name = name.toLowerCase();
				String text1 = text.toLowerCase();
				if (name.indexOf(text1) > -1)
					found = true;
			}
		}
		else {
			if(caseSensitive) {
				if (name.equals(text))
					found = true;
			}
			else if (name.equalsIgnoreCase(text))
				found = true;
		}
		if (found) {
			list.add(term);
			if (!all)
				return true;
		}		
		java.util.List children = term.getChildren();
		if (children != null) {
			for (Iterator it = children.iterator(); it.hasNext();) {
				GOTerm childTerm = (GOTerm) it.next();
				boolean rtn = search(text, childTerm, list, contains, caseSensitive, all);
				if (rtn && !all)
					return true;
			}
		}
		return false;
	}
	
	private JPanel createTreeViewer() {
		JPanel treeViewerPane = new JPanel();
		Border border = BorderFactory.createEtchedBorder();
		treeViewerPane.setBorder(BorderFactory.createTitledBorder(border, "Tree Viewer"));
		treeViewerPane.setLayout(new BorderLayout());
		treeViewer = new JTree() {
			protected void processMouseEvent(MouseEvent e) {
				// Do nothing to block all mouse events.
			}
		};
		treeViewer.setModel(null);
		treeViewerPane.add(new JScrollPane(treeViewer), BorderLayout.CENTER);
		return treeViewerPane;
	}
	
	private JPanel createPropPane() {
		JPanel propPane = new JPanel();
		propPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weightx = 1.0;
		constraints.weighty = 0.1;
		JPanel idPane = new JPanel();
		Border etchedBorder = BorderFactory.createEtchedBorder();
		idPane.setBorder(BorderFactory.createTitledBorder(etchedBorder, "ID"));
		idLabel = new JLabel();
		idPane.add(idLabel);
		propPane.add(idPane, constraints);
		JPanel synonymPane = new JPanel();
		Border border = BorderFactory.createEtchedBorder();
		synonymPane.setBorder(BorderFactory.createTitledBorder(border, "Synonyms"));
		synonymPane.setLayout(new BorderLayout());
		synonymList = new JList();
		DefaultListModel model = new DefaultListModel();
		synonymList.setModel(model);
		synonymPane.add(new JScrollPane(synonymList), BorderLayout.CENTER);
		constraints.gridy = 1;
		constraints.gridheight = 4;
		constraints.weighty = 1.0;
		propPane.add(synonymPane, constraints);
		return propPane;
	}
	
	public boolean showDisplay(JComponent component) {
		isOKClicked = false;
		Window window = (Window)SwingUtilities.getAncestorOfClass(Window.class, component);
		final JDialog dialog;
		if (window instanceof JDialog)
			dialog = new JDialog((JDialog)window);
		else if (window instanceof JFrame)
			dialog = new JDialog((JFrame)window);
		else
			dialog = new JDialog();
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 8));
		JButton okBtn = new JButton("OK");
		JButton cancelBtn = new JButton("Cancel");
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = true;
				dialog.dispose();
			}
		});
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		controlPane.add(okBtn);
		controlPane.add(cancelBtn);
		dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);
		dialog.setTitle("Select a GO Term");
		dialog.setModal(true);
		dialog.setLocationRelativeTo(component);
		dialog.setSize(600, 600);
		dialog.setVisible(true);
		return isOKClicked;
	}
	
	public Object getSelectedTerm() {
		TreePath path = goTree.getSelectionPath();
		if (path == null)
			return null;
		return path.getLastPathComponent();
	}
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new GODisplayPane());
		frame.setSize(500, 600);
		frame.setVisible(true);
	}
	
	class GOTermCellRenderer extends DefaultTreeCellRenderer {
		private Icon isAIcon;
		private Icon partOfIcon;
		
		public GOTermCellRenderer() {
			super();
			isAIcon = GKApplicationUtilities.createImageIcon(getClass(), "IsA.gif");
			partOfIcon = GKApplicationUtilities.createImageIcon(getClass(), "PartOf.gif");
		}
		
		public Component getTreeCellRendererComponent(JTree tree,
		                                              Object value,
		                                              boolean selected,
		                                              boolean expanded,
		                                              boolean leaf,
		                                              int row,
		                                              boolean hasFocus) {
		    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		    GOTerm term = (GOTerm) value;
		    if (term.getType() == GOTerm.PART_OF) 
		    	setIcon(partOfIcon);
		    else
		    	setIcon(isAIcon);
			return this;
		} 
	}

}
