/*
 * Created on Sep 23, 2003
 */
package org.gk.persistence;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * This customized JPane for input db connection info.
 * @author wgm
 */
public class DBConnectionPane extends JPanel {
	
    private JLabel hostLbl;
	private JTextField hostTF;
    private JLabel nameLbl;
	private JTextField nameTF;
	private JTextField portTF;
    private JLabel portLbl;
	private JTextField userTF;
	private JPasswordField pwdTF;
	private JCheckBox useTransactionBox;
	// For properties
	private Properties prop;
	// A flag
	private boolean isOKClicked = false;
	private boolean isDirty = false;
	// To display transaction checkbox
	private boolean needTransactionBox = false;

	public DBConnectionPane() {
		init();
	}
	
	public DBConnectionPane(boolean useTransactionBox) {
		this.needTransactionBox = useTransactionBox;
		init();
	}
	
	private void init() {
		DocumentListener listener = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				isDirty = true;
			}
			public void insertUpdate(DocumentEvent e) {
				isDirty = true;
			}
			public void removeUpdate(DocumentEvent e) {
				isDirty = true;
			}
		};
		// Give some extra space around this component
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		hostLbl = new JLabel("Database Host:");
		constraints.weightx = 0.0d;
		add(hostLbl, constraints);
//		Dimension tfSize = new Dimension(100, 23);
		hostTF = new JTextField();
		hostTF.getDocument().addDocumentListener(listener);
//		hostTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		constraints.weightx = 0.7d;
		add(hostTF, constraints);
		nameLbl = new JLabel("Database Name:");
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.0d;
		add(nameLbl, constraints);
		nameTF = new JTextField();
		nameTF.getDocument().addDocumentListener(listener);
//		nameTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		constraints.weightx = 0.7d;
		add(nameTF, constraints);
		portLbl = new JLabel("Database Port:");
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.weightx = 0.0d;
		add(portLbl, constraints);
		portTF = new JTextField();
		portTF.getDocument().addDocumentListener(listener);
//		portTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		constraints.weightx = 0.7d;
		add(portTF, constraints);
		JLabel userLbl = new JLabel("User Name:");
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.weightx = 0.0d;
		add(userLbl, constraints);
		userTF = new JTextField();
		userTF.getDocument().addDocumentListener(listener);
//		userTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		constraints.weightx = 0.7d;
		add(userTF, constraints);
		JLabel pwdLbl = new JLabel("User Password:");
		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.weightx = 0.0d;
		add(pwdLbl, constraints);
		pwdTF = new JPasswordField();
		pwdTF.getDocument().addDocumentListener(listener);
//		pwdTF.setPreferredSize(tfSize);
		constraints.gridx = 1;
		constraints.weightx = 0.7d;
		add(pwdTF, constraints);
		if (needTransactionBox) {
			useTransactionBox = new JCheckBox("Use Transaction for Updating");
			constraints.gridy = 5;
			constraints.gridx = 0;
			constraints.gridwidth = 2;
			add(useTransactionBox, constraints);
		}
	}
	
	/**
	 * Initialize all values based on the properties.
	 * @param prop
	 */
	public void setValues(Properties prop) {
		String value = prop.getProperty("dbHost", "");
		hostTF.setText(value);
		value = prop.getProperty("dbName", "");
		nameTF.setText(value);
		value = prop.getProperty("dbPort", "");
		portTF.setText(value);
		value = prop.getProperty("dbUser", "");
		userTF.setText(value);
		value = prop.getProperty("dbPwd", "");
		pwdTF.setText(value);
		if (needTransactionBox) {
			value = prop.getProperty("useTransaction");
			if (value != null && value.equals("false"))
				useTransactionBox.setSelected(false);
			else
				useTransactionBox.setSelected(true);
		}
		this.prop = prop;
	}
	
	public boolean showInDialog(Component comp) {
        JDialog dialog1 = null;
        Component root = SwingUtilities.getRoot(comp);
        if (root instanceof Frame)
            dialog1 = new JDialog((Frame)root);
        else if (root instanceof Dialog)
            dialog1 = new JDialog((Dialog)root);
        else
            dialog1 = new JDialog();
        Border innerBorder = getBorder();
        Border outBorder = BorderFactory.createRaisedBevelBorder();
        if (innerBorder == null)
            setBorder(outBorder);
        else
            setBorder(BorderFactory.createCompoundBorder(outBorder, innerBorder));
		// Have to be final
		final JDialog dialog = dialog1;
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		// Control Pane
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (commit()) {
					isOKClicked = true;
					dialog.dispose();
				}
			}
		});
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isOKClicked = false;
				dialog.dispose();
			}
		});
		okBtn.setPreferredSize(cancelBtn.getPreferredSize());
		controlPane.add(okBtn);
		controlPane.add(cancelBtn);
		dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);

		dialog.setSize(350, 270);
		dialog.setLocationRelativeTo(dialog.getOwner());
		dialog.setModal(true);
		dialog.setTitle("Database Connecting Info");
		// Control the focus
		dialog.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				// Find the first empty text field and give the focus to it
				if (hostTF.getText().length() == 0) {
					hostTF.requestFocus();
					return;
				}
				if (nameTF.getText().length() == 0) {
					nameTF.requestFocus();
					return;
				}
				if (portTF.getText().length() == 0) {
					portTF.requestFocus();
					return;
				}
				if (userTF.getText().length() == 0) {
					userTF.requestFocus();
					return;
				}
				if (pwdTF.getPassword().length == 0) {
					pwdTF.requestFocus();
					return;
				}
			}
		});
		dialog.setVisible(true);
		return isOKClicked;
	}
    
    public void showUserAndNameOnly() {
        hostLbl.setVisible(false);
        hostTF.setVisible(false);
        nameLbl.setVisible(false);
        nameTF.setVisible(false);
        portLbl.setVisible(false);
        portTF.setVisible(false);
    }
	
	public boolean commitForTab() {
		if (!isDirty || prop == null)
			return true;
		String value = hostTF.getText().trim();
		if (value.length() == 0)
			prop.remove("dbHost");
		else
			prop.setProperty("dbHost", value);
		value = nameTF.getText().trim();
		if (value.length() == 0)
			prop.remove("dbName");
		else
			prop.setProperty("dbName", value);
		value = portTF.getText().trim();
		if (value.length() == 0)
			prop.remove("dbPort");
		else {
			try {
				int port = Integer.parseInt(value);
				if (port <= 0) {
					JOptionPane.showMessageDialog(this, "Port should be a positive integer.",
					                              "Input Error", JOptionPane.ERROR_MESSAGE);
					portTF.requestFocus();
					return false;                             
				}
				else
					prop.setProperty("dbPort", value);
			}
			catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Port should be a positive integer.",
				                              "Input Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		value = userTF.getText().trim();
		if (value.length() == 0)
			prop.remove("dbUser");
		else
			prop.setProperty("dbUser", value);
		value = new String(pwdTF.getPassword());
		if (value.length() == 0)
			prop.remove("dbPwd");
		else
			prop.setProperty("dbPwd", value);
		// Check useTransaction
		if (needTransactionBox) {
			prop.setProperty("useTransaction", useTransactionBox.isSelected() + "");
		}
		return true;
	}
	
	public boolean commit() {
		String value = hostTF.getText().trim();
		if (value.length() == 0) {
			JOptionPane.showMessageDialog(this, "Please input host name.",
			                              "Input Error", JOptionPane.ERROR_MESSAGE);
			hostTF.requestFocus();
			return false;	
		}
		else
			prop.setProperty("dbHost", value);
		value = nameTF.getText().trim();
		if (value.length() == 0) {
			JOptionPane.showMessageDialog(this, "Please input database name.",
										  "Input Error", JOptionPane.ERROR_MESSAGE);
			nameTF.requestFocus();
			return false;
		}
		else
			prop.setProperty("dbName", value);
		// Check port
		value = portTF.getText().trim();
		if (value.length() == 0)
			prop.remove("dbPort");
		else {
			try {
				int port = Integer.parseInt(value);
				if (port <= 0) {
					JOptionPane.showMessageDialog(this, "Port should be a positive integer.",
												  "Input Error", JOptionPane.ERROR_MESSAGE);
					return false;
				}
				prop.setProperty("dbPort", value);
			}
			catch(NumberFormatException e) {
				JOptionPane.showMessageDialog(this, "Port should be a positive integer.",
											  "Input Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		value = userTF.getText().trim();
//		if (value.length() == 0) {
//			JOptionPane.showMessageDialog(this, "Please input user name.",
//										  "Input Error", JOptionPane.ERROR_MESSAGE);
//			userTF.requestFocus();
//			return false;
//		}
//		else
			prop.setProperty("dbUser", value);
		value = new String(pwdTF.getPassword());
//		if (value.length() == 0) {
//			JOptionPane.showMessageDialog(this, "Please input password.",
//										  "Input Error", JOptionPane.ERROR_MESSAGE);
//			pwdTF.requestFocus();
//			return false;
//		}
//		else
			prop.setProperty("dbPwd", value);
		// Check useTransaction
		if (needTransactionBox) {
			prop.setProperty("useTransaction", useTransactionBox.isSelected() + "");
		}
		return true;
	}
}
