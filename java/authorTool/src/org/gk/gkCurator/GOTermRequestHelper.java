/*
 * Created on Jul 29, 2005
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.gk.util.AutoCompletable;
import org.gk.util.AutoCompletionPane;
import org.gk.util.BrowserLauncher;
import org.gk.util.DialogControlPane;

/**
 * This class is used to help to request a new GO term and track the requests via
 * invoking web browser.
 * @author guanming
 *
 */
public class GOTermRequestHelper {
    private final String NEW_REQUEST_URL = "https://sourceforge.net/tracker/?func=add&group_id=36855&atid=605890";
    private final String TRACK_REQUEST_URL = "https://sourceforge.net/tracker/index.php?func=detail&group_id=36855&atid=605890&aid=";
    private final String BROWSE_REQUEST_URL = "https://sourceforge.net/tracker/?func=browse&group_id=36855&atid=605890";
    private final String REQUEST_ID_PROP_KEY = "goRequestIDs";
    
    public GOTermRequestHelper() {
    }
    
    public void requestNewTerm(Component parentComp) {
        try {
            BrowserLauncher.displayURL(NEW_REQUEST_URL, parentComp);
        } catch (IOException e) {
            System.err.println("GOTermRequestHelper.requestNewTerm(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Display the tracker of a request to GO in the web browser.
     * @param parentComp
     * @param prop provide a list of ID history.
     */
    public void trackRequest(Component parentComp, Properties prop) {
        // Get the IDs from the user
        List ids = new ArrayList();
        String idString = prop.getProperty(REQUEST_ID_PROP_KEY);
        if (idString != null) {
            // "," should be used as the delimiter
            StringTokenizer tokenizer = new StringTokenizer(idString, ", ");
            String id = null;
            while (tokenizer.hasMoreTokens()) {
                id = tokenizer.nextToken();
                ids.add(id);
            }
        }
        Collections.sort(ids);
        // Use a dialog
        JFrame parentFrame = (JFrame) SwingUtilities.getRoot(parentComp);
        RequestIDDialog idDialog = new RequestIDDialog(parentFrame);
        idDialog.setPreIDList(ids);
        idDialog.setModal(true);
        idDialog.setVisible(true);
        if (idDialog.isOKClicked()) {
            String id = idDialog.getInputID();
            if (id == null || id.length() == 0) {
                // Browse
                try {
                    BrowserLauncher.displayURL(BROWSE_REQUEST_URL, parentComp);
                }
                catch(IOException e) {
                    System.err.println("GOTermRequestHelper.trackRequest(): " + e);
                    e.printStackTrace();
                }
            }
            else {
                // Save the id if it is new
                if (!ids.contains(id)) {
                    ids.add(id);
                    prop.setProperty(REQUEST_ID_PROP_KEY, convertListToString(ids));
                }
                String url = TRACK_REQUEST_URL + id;
                try {
                    BrowserLauncher.displayURL(url, parentComp);
                }
                catch(IOException e) {
                    System.err.println("GOTermRequestHelper.trackRequest(): " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private String convertListToString(List ids) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = ids.iterator(); it.hasNext();) {
            buffer.append(it.next());
            if (it.hasNext())
                buffer.append(",");
        }
        return buffer.toString();
    }
    
    class RequestIDDialog extends JDialog {
        private AutoCompletionPane autoPane;
        private JTextField idField;
        private boolean isOKClicked;
        
        RequestIDDialog(JFrame parentFrame) {
            super(parentFrame);
            init();
        }
        
        private void init() {
            JPanel centerPane = new JPanel();
            centerPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            JLabel label = new JLabel("Please Input Request (leave it empty to browse):");
            centerPane.add(label, constraints);
            idField = new JTextField();
            autoPane = new AutoCompletionPane();
            autoPane.setTarget(new AutoCompletable() {
                public void setText(String text) {
                    idField.setText(text);
                }
            });
            autoPane.setInvokingComponent(idField);
            idField.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        autoPane.requestFocus();
                        autoPane.start();
                    }
                    else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        // Force to disappear
                        autoPane.setDisplay(false);
                    }
                    else {
                        if (autoPane.getTextBounds() == null)
                            autoPane.setTextBounds(idField.getBounds());
                        autoPane.setStartText(idField.getText());
                    }
                }
            });
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            centerPane.add(idField, constraints);
            getContentPane().add(centerPane, BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setTitle("Input Request ID");
            setSize(400, 300);
            setLocationRelativeTo(getOwner());
        }
        
        public void setPreIDList(List ids) {
            autoPane.setData(ids);
        }
        
        public boolean isOKClicked() {
            return this.isOKClicked;
        }
        
        public String getInputID() {
            return idField.getText().trim();
        }
        
    }
    
}
