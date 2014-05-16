/*
 * Created on Jan 10, 2012
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.gk.database.AttributeEditConfig;
import org.gk.database.AttributeEditManager;
import org.gk.database.NewInstanceDialog;
import org.gk.elv.InstanceCloneHelper;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;

/**
 * This customized JDialog is used to add hasModifiedResidue attribute to EWAS or DefinedSet 
 * instance. For EWAS, the user can assign a set of coordinates besides a ModifiedResidue 
 * template, the tool will create a list of ModifiedResidue instances based
 * on templates and coordinates, and assign them to the selected EWAS. 
 * @author gwu
 *
 */
public class AddHasModifiedResidueDialog extends NewInstanceDialog {
    private GKInstance ewas;
    private JTextField coordinateTF;
    
    public AddHasModifiedResidueDialog(JFrame parentFrame, String title) {
        super(parentFrame, title);
    }
    
    protected void init() {
        JPanel contentPane = createContentPane();
        getContentPane().add(contentPane, BorderLayout.CENTER);
        // Add some text
        JPanel northPane = new JPanel();
        Border outBorder = BorderFactory.createEtchedBorder();
        Border inBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        northPane.setBorder(BorderFactory.createCompoundBorder(outBorder, inBorder));
        northPane.setLayout(new BorderLayout(4, 4));
        JLabel label = new JLabel("<html><body><b><u>Add hasModifiedResidue Attribute</u></b></body></html>");
        label.setHorizontalAlignment(JLabel.CENTER);
        northPane.add(label, BorderLayout.NORTH);
        JTextArea ta = new JTextArea("Please specify coordinates for modification. If you have more than one coordinate, " + 
                                     "separate them using \",\" (e.g. 100, 120). Also specify other attribute values shared " +
                                     "by all modifications in the following attribute edit table. Leave the coordinate attribute " +
                                     "empty.");
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBackground(northPane.getBackground());
        northPane.add(ta, BorderLayout.CENTER);
        coordinateTF = new JTextField();
        northPane.add(coordinateTF, BorderLayout.SOUTH);
        getContentPane().add(northPane, BorderLayout.NORTH);
        // Create links between GUIs
        installListeners();
        setSize(450, 650);
        setLocationRelativeTo(getOwner());
    }
    
    protected void commit() {
        try {
            String text = coordinateTF.getText().trim();
            List<Integer> coordinates = new ArrayList<Integer>();
            if (text.length() > 0) {
                // Get coordinates
                String[] tokens = text.split(",");
                for (String token : tokens) {
                    coordinates.add(new Integer(token.trim()));
                }
            }
            GKInstance newInst = getNewInstance();
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            fileAdaptor.addNewInstance(newInst);
            ewas.addAttributeValue(ReactomeJavaConstants.hasModifiedResidue, 
                                   newInst);
            if (newInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate)) {
                if (coordinates.size() > 0) {
                    newInst.setAttributeValue(ReactomeJavaConstants.coordinate, coordinates.get(0));
                    InstanceDisplayNameGenerator.setDisplayName(newInst);
                }
                // Need to copy if more than one coordinates have been provided
                InstanceCloneHelper cloneHelper = new InstanceCloneHelper();
                for (int i = 1; i < coordinates.size(); i++) {
                    GKInstance copy = cloneHelper.cloneInstance(newInst, fileAdaptor);
                    copy.setAttributeValue(ReactomeJavaConstants.coordinate,
                                           coordinates.get(i));
                    InstanceDisplayNameGenerator.setDisplayName(copy);
                    ewas.addAttributeValue(ReactomeJavaConstants.hasModifiedResidue, 
                                           copy);
                }
            }
            // Need to refresh the main value
            AttributeEditManager.getManager().attributeEdit(ewas, ReactomeJavaConstants.hasModifiedResidue);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                                          "Error in adding hasModifiedResidue: " + e,
                                          "Error in Adding hasModifiedResidue",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void setInstance(GKInstance inst) {
        if (!inst.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence))
            throw new IllegalArgumentException("Only EntityWithAccessionedSequence can be used for adding hasModifiedResidue: " + inst);
        this.ewas = inst;
        try {
            // Need to specify class
            GKSchemaClass cls = (GKSchemaClass) inst.getSchemClass();
            SchemaAttribute attribute = cls.getAttribute(ReactomeJavaConstants.hasModifiedResidue);
            List<SchemaClass> classes = InstanceUtilities.getAllSchemaClasses(attribute.getAllowedClasses());
            SchemaClass selected = null;
            for (SchemaClass cls1 : classes) {
                if (cls1.getName().equals(ReactomeJavaConstants.ModifiedResidue)) { // Most common one
                    selected = cls1;
                    break; 
                }
            }
            setSchemaClasses(classes, 
                             selected);
            // Assign some attribute
            GKInstance refSeq = (GKInstance) inst.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refSeq != null) {
                GKInstance newInst = getNewInstance();
                if (newInst != null && newInst.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceSequence)) {
                    newInst.setAttributeValue(ReactomeJavaConstants.referenceSequence,
                                              refSeq);
                    attributePane.refresh();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Cannot assign EWAS: " + inst,
                                          "Error in Assigning EWAS",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
}
