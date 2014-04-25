/*
 * Created on May 27, 2004
 */
package org.gk.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A customized JPanel for getting criteria for searching.
 * @author wugm
 */
public class TextSearchPane extends JPanel {
	private JLabel captionLabel;
	private JTextField searchField;
	private JCheckBox wholeNameBox;
	private JCheckBox caseBox;
	private DialogControlPane controlPane;
	// To be used for a JDialog
	private boolean isOKClicked;

	public TextSearchPane() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		JPanel centralPane = new JPanel();
		centralPane.setBorder(BorderFactory.createRaisedBevelBorder());
		centralPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(3, 16, 3, 16);
		constraints.anchor = GridBagConstraints.WEST;
		captionLabel = new JLabel("Find object with name:");
		centralPane.add(captionLabel, constraints);
		searchField = new JTextField();
		constraints.gridy = 1;
		constraints.weightx = 0.5;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		centralPane.add(searchField, constraints);
		wholeNameBox = new JCheckBox("Match whole name only");
		wholeNameBox.setSelected(true); // As default
		wholeNameBox.setMnemonic('w');
		constraints.gridy = 2;
		centralPane.add(wholeNameBox, constraints);
		caseBox = new JCheckBox("Match case");
		//caseBox.setSelected(true); // As default
		caseBox.setMnemonic('s');
		constraints.gridy = 3;
		centralPane.add(caseBox, constraints);
		add(centralPane, BorderLayout.CENTER);
		// Add a control pane
		controlPane = new DialogControlPane();
		add(controlPane, BorderLayout.SOUTH);
		// To control enabiliy of OK button
		JButton okBtn = controlPane.getOKBtn();
		okBtn.setEnabled(false);
		searchField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				validateOKBtn();
			}
			public void removeUpdate(DocumentEvent e) {
				validateOKBtn();
			}
			public void changedUpdate(DocumentEvent e) {
				validateOKBtn();
			}
		});
	}
	
	private void validateOKBtn() {
		JButton okBtn = controlPane.getOKBtn();
		String text = searchField.getText().trim();
		if (text.length() == 0)
			okBtn.setEnabled(false);
		else
			okBtn.setEnabled(true);
	}
	
	public String getSearchKey() {
		return searchField.getText().trim();
	}
	
	public boolean isCaseSensitive() {
		return caseBox.isSelected();
	}
	
	/**
	 * Wrap this TextSearchPane in a JDialog.
	 * @param parentComp
	 * @param title
	 * @return true for OK is clicked while false for clicking cancel.
	 */
	public boolean showSearchDialog(Component parentComp, String title) {
		// Creat a Dialog
		final JDialog dialog = GKApplicationUtilities.createDialog(parentComp, title);
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		dialog.setLocationRelativeTo(parentComp);
		dialog.setSize(320, 220);
		// Add actionListeners
		controlPane.getCancelBtn().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				isOKClicked = false;
			}
		});
		controlPane.getOKBtn().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				isOKClicked = true;
			}
		});
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = searchField.getText().trim();
				if (text.length() == 0)
					return;
				isOKClicked = true;
				dialog.dispose();
			}
		});
		dialog.setModal(true);
		dialog.setVisible(true);
		return isOKClicked;
	}
	
	public boolean isWholeNameOnly() {
		return wholeNameBox.isSelected();
	}
}
