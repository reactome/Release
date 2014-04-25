/*
 * Created on Dec 15, 2005
 */
package org.gk.IDGeneration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.gk.model.GKInstance;
import org.gk.schema.InvalidAttributeException;

/** 
 *  Provides a table of curators who have recently edited instances that failed stable ID generation tests.  Also provides a button, "Mail" which allows an email to be sent to all selected curators.  By default, this email will simply contain a list of the tests that failed, but the user can edit or replace this default text.
 * @author croft
 */
public class TestResultsCuratorPane extends JPanel {
	private JButton mailBtn;
	private JTable testResultsTable;
	private CuratorTableModel curatorTable;
	private List testList = null;
	private TestResultsCuratorTableModel model;
	private JDialog dialog;
	private int curatorCount = 0;
	
	public TestResultsCuratorPane(List testList) {
		this.testList = testList;
		init();
	}
	
	private void init() {
		setMinimumSize(new Dimension(50, getMinimumSize().height));
		setPreferredSize(new Dimension(300, getPreferredSize().height));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		testResultsTable = new JTable();
		model = new TestResultsCuratorTableModel();
		curatorCount = model.setTestList(testList);
		testResultsTable.setModel(model);
		JScrollPane testResultsScrollPane = new JScrollPane(testResultsTable);
		add(testResultsScrollPane);
		
		setUpDialog();
	}
	
	private void setUpDialog() {
		dialog = new JDialog();
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		// Control Pane
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		JButton mailBtn = new JButton("Mail curators");
		mailBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				launchEmailAddressDialog(dialog);
			}
		});
		okBtn.setPreferredSize(mailBtn.getPreferredSize());
		controlPane.add(okBtn);
		controlPane.add(mailBtn);
		dialog.getContentPane().add(controlPane, BorderLayout.SOUTH);

		dialog.setSize(300, 270);
		dialog.setLocationRelativeTo(dialog.getOwner());
		dialog.setModal(true);
		dialog.setTitle(curatorCount + " curators in selected test(s)");
		dialog.setVisible(true);
	}
	
	public void setVisible(boolean visible) {
		dialog.setVisible(visible);
	}
	
	private String extractEmailAddressesFromSelections() {
		int[] selectedRows = testResultsTable.getSelectedRows();
		if (selectedRows.length<1)
			return null;

		String emailAddress;
		GKInstance author;
		List emailAddressList = new ArrayList();
		for (int i=0; i<selectedRows.length; i++) {
			author = model.getCuratorByRowIndex(selectedRows[i]);
			try {
				emailAddress = (String)author.getAttributeValue("eMailAddress");
				if (emailAddress!=null && !emailAddressList.contains(emailAddress)) {
					emailAddressList.add(emailAddress);
				}
			} catch (Exception e) {
				System.err.println("TestResultsCuratorPane.extractEmailAddressesFromTestList: WARNING - could not get curator email address");
				e.printStackTrace();
			}
		}
		
		String emailAddresses = "";
		for (Iterator ite = emailAddressList.iterator(); ite.hasNext();) {
			emailAddress = (String)ite.next();
			if (!emailAddresses.equals(""))
				emailAddresses += ",\n";
			emailAddresses += emailAddress;
		}

		return emailAddresses;
	}
	
	private void launchEmailAddressDialog(JDialog dialog) {
		String emailAddresses = extractEmailAddressesFromSelections();
		if (emailAddresses==null) {
            JOptionPane.showMessageDialog(this,
                    "You have not selected any curators!",
                    "No curators selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
		}
		if (emailAddresses.equals("")) {
            JOptionPane.showMessageDialog(this,
                    "The selected curators do not have any associated email addresses!",
                    "No email addresses",
                    JOptionPane.WARNING_MESSAGE);
            return;
		}
		JTextArea textField = new JTextArea(emailAddresses);
		textField.setEditable(false);
		JScrollPane testResultsScrollPane = new JScrollPane(textField);

		final JDialog emailAddressDialog = new JDialog();
		emailAddressDialog.getContentPane().add(testResultsScrollPane, BorderLayout.CENTER);
		emailAddressDialog.setSize(150, 200);
		emailAddressDialog.setLocationRelativeTo(emailAddressDialog.getOwner());
		emailAddressDialog.setModal(true);
		emailAddressDialog.setTitle("Curator emails");
		emailAddressDialog.setVisible(true);
	}	
}