/*
 * Created on Jan 28, 2005
 *
 * This was originally a subclass of SynchronizationManager
 * but it is so useful that I decided to give it a class
 * of its own.  DC.
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * A customized JDialog to display the progress of synchronization.
 */
public abstract class AbstractProgressDialog extends JDialog {
	// Two progress bars
	public JProgressBar clsBar = null;
	public JProgressBar totalBar = null;
	public JLabel clsLabel = null;

	public AbstractProgressDialog() {
		super();
	}
	
	public AbstractProgressDialog(JFrame parentFrame, String title) {
		super(parentFrame, title);
		init();
	}
	
	private void init() {
		JPanel centerPane = new JPanel();
		centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
		centerPane.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 24, 4, 24);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0.2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		clsLabel = new JLabel("Scan class...");
		centerPane.add(clsLabel, constraints);
		clsBar = new JProgressBar();
		constraints.gridy = 1;
		centerPane.add(clsBar, constraints);
		JLabel totalLabel = new JLabel("Total Finished...");
		constraints.gridy = 2;
		centerPane.add(totalLabel, constraints);
		totalBar = new JProgressBar();
		constraints.gridy = 3;
		centerPane.add(totalBar, constraints);
		// For cancel Button
		JPanel southPane = new JPanel();
		southPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cancel(true);
				dispose();
			}
		});
		cancelBtn.setMnemonic('C');
		southPane.add(cancelBtn);
		getContentPane().add(centerPane, BorderLayout.CENTER);
		getContentPane().add(southPane, BorderLayout.SOUTH);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				int reply = JOptionPane.showConfirmDialog(AbstractProgressDialog.this,
				                                          "Close this dialog will abort the synchronization. Do you want to\n" +
				                                          "close the dialog and abort the synchronization?",
				                                          "Abort the Synchronization?",
				                                          JOptionPane.YES_NO_OPTION);
				if (reply == JOptionPane.YES_OPTION) {
					cancel(true);
					dispose();
				}
			}
		});
	}
	
	public abstract void cancel(boolean isCancelled);
}

