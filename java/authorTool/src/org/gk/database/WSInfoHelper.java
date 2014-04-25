/*
 * Created on Mar 11, 2011
 *
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * This helper class is used to handle WS information management.
 * @author wgm
 *
 */
public class WSInfoHelper {
    
    public WSInfoHelper() {
        
    }
    
    /**
     * Return a two element String array: the first one is WSUser
     * and the second WSKey.
     * @return
     */
    public String[] getWSInfo(Component parentComp) throws UnsupportedEncodingException {
        String wsUser = GKApplicationUtilities.getApplicationProperties().getProperty("wsUser");
        String wsKey = GKApplicationUtilities.getApplicationProperties().getProperty("wsKey");
        if (wsUser == null || wsKey == null) {
            Window owner = SwingUtilities.getWindowAncestor(parentComp);
            WSInfoDialog infoDialog = new WSInfoDialog(owner);
            infoDialog.setSize(325, 225);
            infoDialog.setLocationRelativeTo(owner);
            infoDialog.setModal(true);
            infoDialog.setVisible(true);
            if (!infoDialog.isOkClicked) {
                return null;
            }
            wsUser = infoDialog.getWSUser();
            wsKey = infoDialog.getWsKey();
            GKApplicationUtilities.getApplicationProperties().setProperty("wsUser", wsUser);
            GKApplicationUtilities.getApplicationProperties().setProperty("wsKey", wsKey);
        }
        wsUser = URLEncoder.encode(wsUser, "UTF-8");
        wsKey = URLEncoder.encode(wsKey, "UTF-8");
        return new String[]{wsUser, wsKey};
    }
    
    /**
     * A customized JDialog to get information for web service user and key information.
     * @author wgm
     *
     */
    private class WSInfoDialog extends JDialog {
        boolean isOkClicked;
        JTextField wsUserField;
        JTextField wsKeyField;
        
        public WSInfoDialog(Window owner) {
            super(owner);
            init();
        }
        
        public String getWSUser() {
            return wsUserField.getText().trim();
        }
        
        public String getWsKey() {
            return wsKeyField.getText().trim();
        }
        
        private void init() {
            setTitle("Enter Information");
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            JLabel label = new JLabel("<html><b><u>Please enter connect information</u></b></html>");
            constraints.gridwidth = 2;
            panel.add(label, constraints);
            JLabel userLabel = new JLabel("User: ");
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            panel.add(userLabel, constraints);
            wsUserField = new JTextField();
            wsUserField.setColumns(10);
            constraints.gridx = 1;
            panel.add(wsUserField, constraints);
            JLabel keyLabel = new JLabel("Key: ");
            constraints.gridx = 0;
            constraints.gridy = 2;
            panel.add(keyLabel, constraints);
            wsKeyField = new JPasswordField();
            wsKeyField.setColumns(10);
            constraints.gridx = 1;
            panel.add(wsKeyField, constraints);
            getContentPane().add(panel, BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
        }
        
    }    
}
