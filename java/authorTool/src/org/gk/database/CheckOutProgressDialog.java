/*
 * Created on Dec 4, 2006
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class CheckOutProgressDialog extends JDialog {
    boolean isCancelClicked = false;
    private JLabel label;
    private JProgressBar progressBar;
    JButton cancelBtn;
    
    public CheckOutProgressDialog(JFrame parentFrame) {
        this(parentFrame, "Checking Out");
    }
    
    public CheckOutProgressDialog(JFrame parentFrame,
                                  String title) {
        super(parentFrame);
        setTitle(title);
        init();
    }
    
    private void init() {
        JPanel centerPane = new JPanel();
        centerPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        label = new JLabel("Checking out instances...");
        centerPane.add(label, constraints);
        progressBar = new JProgressBar();
        constraints.gridy = 1;
        centerPane.add(progressBar, constraints);
        progressBar.setIndeterminate(true);
        cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        constraints.gridy = 2;
        centerPane.add(cancelBtn, constraints);
        cancelBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(cancelBtn);
        getContentPane().add(centerPane, BorderLayout.CENTER);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(getOwner());
        setModal(true);
    }
    
    public void setIsDone() {
        label.setText("Checking out is done.");
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(100);
        progressBar.setValue(100);
        cancelBtn.setText("OK");
        cancelBtn.setEnabled(true); // In case it is disabled.
    }
    
    public void disableCancel() {
        cancelBtn.setEnabled(false);
        cancelBtn.setText("Please wait...");
    }
    
    public void setIsWrong() {
        label.setText("<html>Something is wrong during checking out.<br>It is aborted.</html>");
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        cancelBtn.setText("OK");
    }
    
    public void setText(String text) {
        label.setText(text);
    }
    
    public boolean isCancelClicked() {
        return isCancelClicked;
    }
    
    private void cancel() {
        String text = cancelBtn.getText();
        if (text.equals("OK")) {
            dispose();
            return;
        }
        if (!cancelBtn.isEnabled()) {
            JOptionPane.showMessageDialog(this,
                                          "Sorry! You cannot cancel checking out right now. Please wait after checking out is done.",
                                          "Cancel Checking Out",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Confirm
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "Are you sure you want to cancel the checking out action?",
                                                  "Cancel Confirmation",
                                                  JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
            isCancelClicked = true;
            dispose();
        }
    }
}