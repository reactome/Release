/*
 * Created on Jan 21, 2008
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JDialog is used to display information for a project opened
 * in the curator tool.
 * @author guanming
 *
 */
public class ProjectInfoDialog extends JDialog {
    private final String PERSON_NOT_DEFINED = "Not Defined";
    private JTextPane descTA;
    private JLabel personValueLabel;
    
    public ProjectInfoDialog(Frame owner) {
        super(owner);
        init();
    }
    
    private void init() {
        setTitle("Project Info");
        // ContentPane
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridLayout(2, 1, 6, 2));
        JPanel personPanel = createPersonPanel();
        contentPane.add(personPanel);
        JPanel descPane = createProjectDescPanel();
        contentPane.add(descPane);
        getContentPane().add(contentPane, BorderLayout.CENTER);
        // Need to add a control panel
        DialogControlPane controlPane = new DialogControlPane();
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doOkAction();
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancelAction();
            }
        });
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        setSize(400, 300);
    }
    
    private void doOkAction() {
        String desc = descTA.getText().trim();
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        String oldDesc = fileAdaptor.getProjectDescription();
        if (!desc.equals(oldDesc)) {
            fileAdaptor.setProjectDescription(desc);
            fileAdaptor.markAsDirty();
        }
        String dbId = personValueLabel.getText();
        if (!dbId.equals(fileAdaptor.getDefaultPersonId() + "")) {
            Long newPersonId = new Long(dbId);
            fileAdaptor.setDefaultPersonId(newPersonId);
            SynchronizationManager.getManager().setDefaultPerson(newPersonId);
            PersistenceManager.getManager().getActiveFileAdaptor().setDefaultPersonId(newPersonId);
        }
        dispose();
    }
    
    private void doCancelAction() {
        dispose();
    }
    
    private JPanel createPersonPanel() {
        JPanel personPanel = new JPanel();
        personPanel.setBorder(BorderFactory.createEtchedBorder());
        personPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        JLabel personLabel = new JLabel("Project Person Id: ");
        // Get person ID
        personValueLabel = new JLabel();
        Long personId = PersistenceManager.getManager().getActiveFileAdaptor().getDefaultPersonId();
        if (personId != null)
            personValueLabel.setText(personId.toString());
        else
            personValueLabel.setText(PERSON_NOT_DEFINED);
        personPanel.add(personLabel, constraints);
        constraints.gridx = 1;
        personPanel.add(personValueLabel, constraints);
        // If a person is not defined, give a chance to define a person.
        // Otherwise, give an option to show person.
        JButton viewPersonBtn = new JButton("View Person");
        viewPersonBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewPerson();
            }
        });
        JButton choosePersonBtn = new JButton("Choose Person");
        choosePersonBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                choosePerson();
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 1;
        personPanel.add(viewPersonBtn, constraints);
        constraints.gridx = 1;
        personPanel.add(choosePersonBtn, constraints);
        return personPanel;
    }
    
    private void choosePerson() {
        GKInstance person = new PersonChoosingHelper().choosePerson(this);
        if (person != null) {
            personValueLabel.setText(person.getDBID() + "");
        }
    }
    
    private void viewPerson() {
        String text = personValueLabel.getText();
        if (text.equals(PERSON_NOT_DEFINED))
            return; // Cannot view
        Long dbId = new Long(text);
        new PersonChoosingHelper().viewPerson(dbId, this, false);
    }
    
    private JPanel createProjectDescPanel() {
        JPanel descPanel = new JPanel();
        descPanel.setBorder(BorderFactory.createEtchedBorder());
        descPanel.setLayout(new BorderLayout(8, 4));
        JLabel label = GKApplicationUtilities.createTitleLabel("Project Description");
        descTA = new JTextPane();
        descTA.setContentType("text/plain");
        String desc = PersistenceManager.getManager().getActiveFileAdaptor().getProjectDescription();
        if (desc != null)
            descTA.setText(desc);
        descPanel.add(label, BorderLayout.NORTH);
        descPanel.add(new JScrollPane(descTA), BorderLayout.CENTER);
        return descPanel;
    }
    
}
