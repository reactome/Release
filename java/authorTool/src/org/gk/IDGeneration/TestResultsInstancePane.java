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

import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;

/** 
 *  Provides a table of instances that failed stable ID generation tests.  Also provides a button, "Mail" which allows an email to be sent to all selected curators.  By default, this email will simply contain a list of the tests that failed, but the user can edit or replace this default text.
 * @author croft
 */
public class TestResultsInstancePane extends JPanel {
	private JButton mailBtn;
	private JButton okBtn;
	private List testList = null;
	private InstanceListPane instancePane;
	
	public TestResultsInstancePane(List testList) {
		this.testList = testList;
		init();
	}
	
	private void init() {
		setMinimumSize(new Dimension(50, getMinimumSize().height));
		setPreferredSize(new Dimension(300, getPreferredSize().height));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		List dbIds = extractInstancesDbIdsFromTestList();
		instancePane = new InstanceListPane();
		instancePane.setDisplayedInstanceDbIds(dbIds);
		instancePane.setTitle("Instances: " + dbIds.size());
		add(instancePane);
		
		setUpDialog();
	}
	
	private void setUpDialog() {
		final JDialog dialog = new JDialog();
		dialog.getContentPane().add(this, BorderLayout.CENTER);
		// Control Pane
		JPanel controlPane = new JPanel();
		controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		okBtn.setDefaultCapable(true);
		getRootPane().setDefaultButton(okBtn);
		mailBtn = new JButton("Mail curators");
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
		dialog.setTitle("Instances found in tests");
		dialog.setVisible(true);
	}
	
	private List extractInstancesDbIdsFromTestList() {
		List dbIds = new ArrayList();
		List testDbIds;
		Long dbId;
		IDGeneratorTest test;
		for (Iterator it = testList.iterator(); it.hasNext();) {
			test = (IDGeneratorTest)it.next();
			testDbIds = test.getDbIds();
			for (Iterator it1 = testDbIds.iterator(); it1.hasNext();) {
				dbId = (Long)it1.next();
				if (dbId==null) {
					System.err.println("TestResultsTableModel.getValueAt: WARNING - dbId is null, skipping!!");
					continue;
				}
				if (!dbIds.contains(dbId))
					dbIds.add(dbId);
			}
		}
		
		return dbIds;
	}
	
	private String extractEmailAddressesFromSelections() {
		List instances = instancePane.getSelection();
		if (instances.size()<1)
			return null;
		
		List instanceEdits;
		List authors;
		GKInstance instance;
		GKInstance instanceCurator;
		GKInstance author;
		String emailAddress;
		List emailAddressList = new ArrayList();
		for (Iterator it = instances.iterator(); it.hasNext();) {
			instance = (GKInstance)it.next();
			
			try {
				instanceEdits = IDGenerationUtils.getInstanceEdits(instance);
				
				authors = new ArrayList();
				for (Iterator itc = instanceEdits.iterator(); itc.hasNext();) {
					instanceCurator = (GKInstance)itc.next();
					authors = instanceCurator.getAttributeValuesList("author");
					for (Iterator ita = authors.iterator(); ita.hasNext();) {
						author = (GKInstance)ita.next();
						emailAddress = (String)author.getAttributeValue("eMailAddress");
						if (emailAddress!=null && !emailAddressList.contains(emailAddress)) {
							emailAddressList.add(emailAddress);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("TestResultsInstancePane.extractEmailAddressesFromTestList: WARNING - could not get curator email address");
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
                    "You have not selected any instances!",
                    "No instances selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
		}
		if (emailAddresses.equals("")) {
            JOptionPane.showMessageDialog(this,
                    "The selected instances do not have any associated email addresses!",
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