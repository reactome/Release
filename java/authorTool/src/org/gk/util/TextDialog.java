/*
 * Created on Oct 30, 2003
 */
package org.gk.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A customized JDialog that can be used to display text. This dialog can be set
 * as editable dialog by passing true to two of constructors.
 * 
 * @author wugm
 */
public class TextDialog extends JDialog {
	// GUIs
	private JTextPane textPane;
	private boolean isEditable;
	private boolean isOKClicked = false;
	private boolean isChanged = false;
	
	public TextDialog(JFrame parentFrame, String title) {
		super(parentFrame, title);
		init();
	}
	
	public TextDialog(JDialog parentDialog, String title) {
		super(parentDialog, title);
		init();
	}
	
	public TextDialog(String title) {
	    super();
	    setTitle(title);
	    init();
	}
	
	public TextDialog(JFrame parentFrame, String title, boolean editable) {
		super(parentFrame, title);
		this.isEditable = editable;
		init();
	}
	
	public TextDialog(JDialog parentDialog, String title, boolean editable) {
		super(parentDialog, title);
		this.isEditable = editable;
		init();
	}
	
	public TextDialog(String title, boolean editable) {
		super();
		setTitle(title);
		this.isEditable = editable;
		init();
	}
	
	private void init() {
		// JTextPane for displaying text
		textPane = new JTextPane();
		textPane.setEditable(isEditable);
		textPane.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				isChanged = true;
			}
			public void removeUpdate(DocumentEvent e) {
				isChanged = true;
			}
			public void insertUpdate(DocumentEvent e) {
				isChanged = true;
			}
		});
		getContentPane().add(new JScrollPane(textPane), BorderLayout.CENTER);
		// Close button
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		if (isEditable) {
			JButton okBtn = new JButton("OK");
			okBtn.setMnemonic('O');
			okBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = true;
					dispose();
				}
			});
			okBtn.setDefaultCapable(true);
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.setMnemonic('C');
			cancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isOKClicked = false;
					dispose();
				}
			});
			okBtn.setPreferredSize(cancelBtn.getPreferredSize());
			controlPane.add(okBtn);
			controlPane.add(cancelBtn);
			getRootPane().setDefaultButton(okBtn);
		}
		else {
			JButton closeBtn = new JButton("Close");
			closeBtn.setMnemonic('C');
			closeBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			closeBtn.setDefaultCapable(true);
			getRootPane().setDefaultButton(closeBtn);
			controlPane.add(closeBtn);
		}
		getContentPane().add(controlPane, BorderLayout.SOUTH);
		// Configure the dialog
		setSize(400, 400);
		setLocationRelativeTo(getOwner());
		setModal(true);
	}
	
	public boolean isOKClicked() {
		return this.isOKClicked;
	}
	
	public String getText() {
		return textPane.getText();
	}
	
	public boolean isChanged() {
		return this.isChanged;
	}
	
	public void setText(String text) {
		textPane.setText(text);
	}
}