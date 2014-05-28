/*
 * Created on Nov 13, 2007
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.gk.model.GKInstance;
import org.gk.persistence.PersistenceManager;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;

/**
 * This custmoized JDialog is used to set a Person instance for
 * the default InstanceEdit for each new project. Several cases should
 * be handled in this class: when the user starts the curator tool for 
 * the first time, no default person is specified, the user needs to choose
 * a person; the user has used a person in other project, the used person 
 * should be confirmed to be used in a new project.
 * @author guanming
 *
 */
public class PersonChooseDialog extends JDialog {
    private JButton yesBtn;
    private JButton cancelBtn;
    private JButton viewBtn;
    private JButton chooseBtn;
    private JTextArea ta;
    // A flag to show is OK is clicked
    private boolean isOkClicked;
    private Long personId;
    
    public PersonChooseDialog(JFrame parent) {
        super(parent);
        init();
    }
    
    public void setUsedPersonId(Long dbId) {
        ta.setText("You have used Person " + dbId + " for the default InstanceEdit for previous projects. " +
        "Do you want to use this same instance for this new project?" );
        personId = dbId;
        getRootPane().setDefaultButton(yesBtn);
        yesBtn.setVisible(true);
        viewBtn.setVisible(true);
    }
    
    public void setNewPersonId(GKInstance person) {
        ta.setText("The chosen Person instance for the new project is: " +
                    person);
        personId = person.getDBID();
        yesBtn.setVisible(true);
        viewBtn.setVisible(true);
        getRootPane().setDefaultButton(yesBtn);
    }
    
    public Long getPersonId() {
        return this.personId;
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    private void init() {
        setTitle("Choose Person for New Project");
        JPanel contentPane = new JPanel();
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        ta = new JTextArea();
        ta.setBorder(BorderFactory.createEtchedBorder());
        ta.setEditable(false);
        ta.setBackground(getBackground());
        ta.setText("You need to choose a Person instance for the new project. Please click " +
                "\"Choose Person\" for a Person instance.");
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = 2;
        constraints.gridheight = 3;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        contentPane.add(ta, constraints);
        // Button panesl
        yesBtn = new JButton("Yes");
        yesBtn.setDefaultCapable(true);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.EAST;
        contentPane.add(yesBtn, constraints);
        viewBtn = new JButton("View Person"); 
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        contentPane.add(viewBtn, constraints);
        chooseBtn = new JButton("Choose Person");
        chooseBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(chooseBtn);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        contentPane.add(chooseBtn, constraints);
        cancelBtn = new JButton("Cancel");
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        contentPane.add(cancelBtn, constraints);
        getContentPane().add(contentPane, BorderLayout.CENTER);
        ImageIcon icon = AuthorToolAppletUtilities.createImageIcon("VerticalHelix.png");
        JLabel image = new JLabel(icon);
        image.setBorder(BorderFactory.createEtchedBorder());
        image.setBackground(Color.WHITE);
        image.setOpaque(true);
        getContentPane().add(image, BorderLayout.WEST);
        // The best size
        setSize(420, 245);
        ActionListener l = createActionListener();
        yesBtn.addActionListener(l);
        cancelBtn.addActionListener(l);
        viewBtn.addActionListener(l);
        chooseBtn.addActionListener(l);
        yesBtn.setVisible(false);
        viewBtn.setVisible(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
    }
    
    private ActionListener createActionListener() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton src = (JButton) e.getSource();
                if (src == yesBtn) {
                    commit();
                }
                else if (src == cancelBtn) {
                    cancel();
                }
                else if (src == viewBtn) {
                    view();
                }
                else if (src == chooseBtn) {
                    choose();
                }
            }
        };
        return l;
    }
    
    private void commit() {
        isOkClicked = true;
        setVisible(false);
        dispose();
    }
    
    private void cancel() {
        JOptionPane.showMessageDialog(this, 
                                      "You have chosen cancelling the action of selecting a Person\n" +
                                      "for the new project. You will be asked to select Person when\n" +
                                      "you commit your project to the database.",
                                      "Cancel Information",
                                      JOptionPane.INFORMATION_MESSAGE);
        isOkClicked = false;
        setVisible(false);
        dispose();
        personId = null; // Null the personId so that it will be asked again.
    }
    
    private void view() {
        new PersonChoosingHelper().viewPerson(personId,
                                              this,
                                              true);
    }
    
    private void choose() {
        GKInstance newPerson = new PersonChoosingHelper().choosePerson(this);
        if (newPerson != null)
            setNewPersonId(newPerson);
    }
    
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("dbName", "gk_central_082807");
        properties.put("dbHost", "localhost");
        properties.put("dbPort", "3306");
        properties.put("dbUser", "root");
        properties.put("dbPwd", "macmysql01");
        PersistenceManager.getManager().setDBConnectInfo(properties);
        JFrame frame = new JFrame();
        PersonChooseDialog dialog = new PersonChooseDialog(frame);
        dialog.setUsedPersonId(new Long(140537L));
        GKApplicationUtilities.center(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        dialog.setVisible(true);
    }
    
}
