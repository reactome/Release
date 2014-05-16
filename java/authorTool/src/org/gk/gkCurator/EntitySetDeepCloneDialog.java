/*
 * Created on Jan 10, 2012
 *
 */
package org.gk.gkCurator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.gk.database.AttributeEditManager;
import org.gk.database.FrameManager;
import org.gk.database.InstanceListPane;
import org.gk.database.InstanceSelectDialog;
import org.gk.database.NewInstanceDialog;
import org.gk.database.SynchronizationManager;
import org.gk.elv.InstanceCloneHelper;
import org.gk.model.GKInstance;
import org.gk.model.InstanceDisplayNameGenerator;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.InvalidAttributeValueException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JDialog is used to create a reaction for DefinedSet instance modification.
 * Only a DefinedSet instance having using EWAS instances as its members can be used in this tool.
 * The tool is used to clone members, add modifications for cloned new EWAS instances, create a new
 * DefinedSet to hold these newly created EWASs, and create a reaction to use the selected DefinedSet
 * as input, and newly created DefinedSet as output. The final reaction creation step is an option.
 * @author gwu
 *
 */
public class EntitySetDeepCloneDialog extends NewInstanceDialog {
    private GKInstance definedSet;
    private JLabel instLabel;
    private JTextField findTF;
    private JTextField replaceTF;
    private JTextField prefixTF;
    private JCheckBox createReactionBox;
    // Track in order to set visial
    private JPanel modificationPane;
    private JCheckBox addModification;
    private JPanel compartmentPane;
    private JCheckBox setCompartment;
    private JButton setMemberPositionBtn;
    private JCheckBox setMemberPositionBox;
    private JButton selectCompartmentBtn;
    private JButton individualNameBtn;
    private JCheckBox individualNameCheckBox;
    private InstanceListPane compartmentList;
    // Some cached values
    private List<GKInstance> selectedCompartments;
    private Map<GKInstance, Integer> memberToCoordinate;
    private Map<GKInstance, String> memberToName;
    
    public EntitySetDeepCloneDialog(JFrame parentFrame) {
        super(parentFrame, "DefinedSet Clone Utility");
        selectedCompartments = new ArrayList<GKInstance>();
        memberToCoordinate = new HashMap<GKInstance, Integer>();
        memberToName = new HashMap<GKInstance, String>();
    }
    
    protected void init() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        JLabel titleLabel = new JLabel("<html><b><u>Clone DefinedSet and its Members</u></b></html>");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel titlePane = new JPanel();
        titlePane.add(titleLabel);
        contentPane.add(titlePane);
        contentPane.add(Box.createVerticalStrut(8));
        
        JPanel selectedInstancePane = createSelectedInstancePane();
        contentPane.add(selectedInstancePane);
        contentPane.add(Box.createVerticalStrut(8));
        
        addModification = new JCheckBox("Add Modification (enter shared attributes below)");
        addModification.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPane.add(addModification);
        
        modificationPane = createModificationPanel();
        contentPane.add(modificationPane);
        contentPane.add(Box.createVerticalStrut(8));
        
        setCompartment = new JCheckBox("Set Compartment");
        setCompartment.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPane.add(setCompartment);
        
        compartmentPane = createCompartmentPanel();
        contentPane.add(compartmentPane);
        contentPane.add(Box.createVerticalStrut(8));
        
        JPanel nameChangePane = createNameChangePane();
        contentPane.add(nameChangePane);
        contentPane.add(Box.createVerticalStrut(8));
        
        createReactionBox = new JCheckBox("Create Reaction");
        createReactionBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPane.add(createReactionBox);
        contentPane.add(Box.createVerticalStrut(8));
        
        JTextArea noteTA = createTextNote(contentPane);
        contentPane.add(noteTA);
        
        JPanel controlPane = createControlPane();
        contentPane.add(controlPane);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        setSize(600, 800);
        setLocationRelativeTo(getOwner());
        installListeners();
    }
    
    @Override
    protected void installListeners() {
        super.installListeners();
        setMemberPositionBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                setMemberPosition();
            }
        });
        selectCompartmentBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                selectCompartments();
            }
        });
        individualNameBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                setMemberNames();
            }
        });
        
        setMemberPositionBox.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                setMemberPositionBtn.setEnabled(setMemberPositionBox.isSelected());
            }
        });
        
        individualNameCheckBox.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                individualNameBtn.setEnabled(individualNameCheckBox.isSelected());
            }
        });
        
        setCompartment.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                selectCompartmentBtn.setEnabled(setCompartment.isSelected());
            }
        });
        
        addModification.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                attributePane.setEditable(addModification.isSelected());
                setMemberPositionBox.setEnabled(addModification.isSelected());
            }
        });
    }
    
    private void setMemberNames() {
        // Need to generate an InstanceToName map
        // Cannot auto-generate names based on patterns
        if (memberToName.size() == 0) {
            try {
                List<?> values = definedSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                for (Iterator<?> it = values.iterator(); it.hasNext();) {
                    GKInstance inst = (GKInstance) it.next();
                    String name = (String) inst.getAttributeValue(ReactomeJavaConstants.name);
                    memberToName.put(inst, name);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                                              "Cannot get members from the selected DefinedSet: " + e,
                                              "Error in DefineSet",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        MemberPropertyDialog memberDialog = new MemberPropertyDialog(this,
                                                                     new String[]{"Instance", "Final Name"});
        memberDialog.setInstanceToValue(memberToName);
        memberDialog.setTitle("Set Member Names");
        memberDialog.setVisible(true);
        if (memberDialog.isOKClicked) {
            Map<GKInstance, String> instToValue = memberDialog.instToValue;
            memberToName.putAll(instToValue);
        }
    }
    
    private void setMemberPosition() {
        MemberPropertyDialog memberDialog = new MemberPropertyDialog(this,
                                                                     new String[]{"Instance", "Coordinate"});
        memberDialog.setInstanceToValue(memberToCoordinate);
        memberDialog.setTitle("Set Member Modification Coordinates");
        memberDialog.setVisible(true);
        if (memberDialog.isOKClicked) {
            // Need to copy member to position
            // In case there is a number formation error
            Map<GKInstance, String> instToValue = memberDialog.instToValue;
            try {
                for (GKInstance inst : instToValue.keySet()) {
                    String value = instToValue.get(inst);
                    if (value == null || value.length() == 0)
                        memberToCoordinate.put(inst, null);
                    else
                        memberToCoordinate.put(inst, new Integer(value));
                }
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Please maker sure all entered coordinates are integer!",
                                              "Number Format Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void selectCompartments() {
        try {
            InstanceSelectDialog dialog1 = new InstanceSelectDialog(this, "Select Instance for Compartment");
            XMLFileAdaptor fileAdpator = PersistenceManager.getManager().getActiveFileAdaptor();
            SchemaClass ewas = fileAdpator.getSchema().getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
            SchemaAttribute att = ewas.getAttribute(ReactomeJavaConstants.compartment);
            dialog1.setTopLevelSchemaClasses(att.getAllowedClasses());
            dialog1.setIsMultipleValue(att.isMultiple());
            dialog1.setModal(true);
            dialog1.setSize(1000, 700);
            GKApplicationUtilities.center(dialog1);
            dialog1.setVisible(true); 
            selectedCompartments.clear();
            if (dialog1.isOKClicked()) {
                selectedCompartments.addAll(dialog1.getSelectedInstances());
            }
            compartmentList.setDisplayedInstances(selectedCompartments);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                                          "Error in selecting compartment: " + e,
                                          "Error in Selecting Compartment",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JPanel createNameChangePane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEtchedBorder());
        JLabel label = new JLabel("Name Change in Cloned Members");
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(label);
        // Display find and replace
        JPanel replaceByPane = new JPanel();
        replaceByPane.add(new JLabel("Replace "));
        findTF = new JTextField();
        findTF.setColumns(6);
        replaceByPane.add(findTF);
        replaceByPane.add(new JLabel(" by "));
        replaceTF = new JTextField();
        replaceTF.setColumns(6);
        replaceByPane.add(replaceTF);
        panel.add(replaceByPane);
        // Add a prefix pane
        JPanel prefixPane = new JPanel();
        prefixPane.add(new JLabel("And prefix "));
        prefixTF = new JTextField();
        prefixTF.setColumns(6);
        prefixPane.add(prefixTF);
        panel.add(prefixPane);
        // Add a button
        JPanel individualPane = new JPanel();
        individualPane.add(new JLabel("Or "));
        individualNameCheckBox = new JCheckBox("Use Individual Name");
        individualPane.add(individualNameCheckBox);
        individualNameBtn = new JButton("Set...");
        individualNameBtn.setEnabled(false);
        individualPane.add(individualNameBtn);
        panel.add(individualPane);
        return panel;
    }
    
    private JPanel createModificationPanel() {
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEtchedBorder());
        pane.setLayout(new BorderLayout());
        JPanel titlePane = createTitlePane();
        pane.add(titlePane, BorderLayout.NORTH);
        setUpAttributePane();
        pane.add(attributePane, BorderLayout.CENTER);
        // Add a button to set individual position
        JPanel southPane = new JPanel();
        setMemberPositionBox = new JCheckBox("Set Individual Coordinate");
        setMemberPositionBtn = new JButton("Set...");
        setMemberPositionBtn.setEnabled(false);
        southPane.add(setMemberPositionBox);
        southPane.add(setMemberPositionBtn);
        pane.add(southPane, BorderLayout.SOUTH);
        attributePane.setEditable(false);
        setMemberPositionBox.setEnabled(false);
        return pane;
    }
    
    private JPanel createCompartmentPanel() {
        JPanel pane = new JPanel();
        pane.setBorder(BorderFactory.createEtchedBorder());
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        selectCompartmentBtn = new JButton("Select Compartment");
        selectCompartmentBtn.setEnabled(false);
        selectCompartmentBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        compartmentList = new InstanceListPane();
        compartmentList.hideTitle();
        pane.add(selectCompartmentBtn);
        pane.add(compartmentList);
        return pane;
    }
    
    private JPanel createSelectedInstancePane() {
        // Display the selected DefinedSet
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Selected DefinedSet: ");
        panel.add(label);
        instLabel = new JLabel("Instance");
        instLabel.setForeground(Color.BLUE);
        instLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        instLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                FrameManager.getManager().showInstance(definedSet, 
                                                       EntitySetDeepCloneDialog.this, 
                                                       true);
            }
        });
        panel.add(instLabel);
        return panel;
    }
    
    protected JTextArea createTextNote(JPanel northPane) {
        JTextArea ta = new JTextArea("Note: Clicking OK will clone members in the selected DefinedSet, replace text " +
        		"in names as specified, generate " +
                "a new DefinedSet containing cloned members, and create a new Reaction with the selected DefinedSet " +
                "as input, new DefinedSet as output if create reaction is checked. If add modification is checked, " +
                "the utility will auto-generate ModifiedResidue instances, assign ModifiedResidues to cloned members. " +
                "If set (not add!) compartment is checked, compartment will be assigned to cloned members.");
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setBackground(northPane.getBackground());
        Font font = ta.getFont();
        font = font.deriveFont(Font.ITALIC);
        font = font.deriveFont(font.getSize() - 2.0f);
        ta.setFont(font);
        return ta;
    }
    
    @Override
    protected void commit() {
        try {
            List<GKInstance> members = definedSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (members == null || members.size() == 0)
                return;
            InstanceCloneHelper cloneHelper = new InstanceCloneHelper();
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            // Clone the original members
            // Map back to the orignal member
            Map<GKInstance, GKInstance> originalToClone = new HashMap<GKInstance, GKInstance>();
            List<GKInstance> clonedMembers = new ArrayList<GKInstance>(members.size());
            for (GKInstance member : members) {
                GKInstance clonedMember = cloneHelper.cloneInstance(member, fileAdaptor);
                clonedMembers.add(clonedMember);
                originalToClone.put(member, clonedMember);
            }
            // Create a new DefinedSet to hold these cloned members
            GKInstance newDefinedSet = cloneHelper.cloneInstance(definedSet, fileAdaptor);
            newDefinedSet.setAttributeValue(ReactomeJavaConstants.hasMember, clonedMembers);
            handleNames(originalToClone);
            handleModifications(cloneHelper, 
                                fileAdaptor, 
                                originalToClone);
            handleCompartments(originalToClone,
                               newDefinedSet);
            handleReaction(fileAdaptor, newDefinedSet);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                                          "Error in running DefinedSet clone utility:\n" + e,
                                          "Error in DefinedSet Clone Utility",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReaction(XMLFileAdaptor fileAdaptor,
                                GKInstance newDefinedSet)
            throws InvalidAttributeException, InvalidAttributeValueException,
            Exception {
        // Check if a reaction should be created
        if (createReactionBox.isSelected()) {
            // Create a new Reaction
            GKInstance reaction = fileAdaptor.createNewInstance(ReactomeJavaConstants.Reaction);
            reaction.addAttributeValue(ReactomeJavaConstants.input, definedSet);
            reaction.addAttributeValue(ReactomeJavaConstants.output, newDefinedSet);
            // Give it a fancy name
            String name = (String) definedSet.getAttributeValue(ReactomeJavaConstants.name);
            reaction.addAttributeValue(ReactomeJavaConstants.name, "(auto-generated) " + name + " Modification");
            InstanceDisplayNameGenerator.setDisplayName(reaction);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleCompartments(Map<GKInstance, GKInstance> originalToClone,
                                    GKInstance newDefinedSet) throws Exception {
        if (!setCompartment.isSelected())
            return;
        List<GKInstance> compartments = compartmentList.getDisplayedInstances();
        if (compartments == null || compartments.size() == 0)
            return;
        for (GKInstance original : originalToClone.keySet()) {
            GKInstance clone = originalToClone.get(original);
            if (!clone.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment))
                continue;
            // This should be set
            clone.setAttributeValueNoCheck(ReactomeJavaConstants.compartment,
                                           new ArrayList<GKInstance>(compartments));
            InstanceDisplayNameGenerator.setDisplayName(clone);
        }
        if (newDefinedSet.getSchemClass().isValidAttribute(ReactomeJavaConstants.compartment)) {
            newDefinedSet.setAttributeValue(ReactomeJavaConstants.compartment,
                                            new ArrayList<GKInstance>(compartments));
            InstanceDisplayNameGenerator.setDisplayName(newDefinedSet);
        }
    }
    
    private void handleModifications(InstanceCloneHelper cloneHelper,
                                       XMLFileAdaptor fileAdaptor,
                                       Map<GKInstance, GKInstance> originalToClone) throws Exception {
        if (!addModification.isSelected())
            return;
        // Handle modifications
        // Modification template
        GKInstance modification = getNewInstance();
        fileAdaptor.addNewInstance(modification); // Need to add this to local project
        // Create a list of modification for each cloned member
        List<GKInstance> modifications = new ArrayList<GKInstance>(originalToClone.size());
        modifications.add(modification);
        for (int i = 1; i < originalToClone.size(); i++) {
            GKInstance clonedModification = cloneHelper.cloneInstance(modification, fileAdaptor);
            modifications.add(clonedModification);
        }
        int index = 0;
        for (GKInstance original : originalToClone.keySet()) {
            GKInstance clonedMember = originalToClone.get(original);
            modification = modifications.get(index);
            clonedMember.addAttributeValue(ReactomeJavaConstants.hasModifiedResidue,
                                           modification);
            GKInstance refSeq = (GKInstance) clonedMember.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (refSeq != null && modification.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceSequence)) {
                modification.setAttributeValue(ReactomeJavaConstants.referenceSequence, 
                                                     refSeq);
            }
            if (setMemberPositionBox.isSelected() && memberToCoordinate.containsKey(original)) {
                Integer coordinate = memberToCoordinate.get(original);
                if (coordinate != null && modification.getSchemClass().isValidAttribute(ReactomeJavaConstants.coordinate))
                    modification.setAttributeValue(ReactomeJavaConstants.coordinate,
                                                   coordinate);
            }
            InstanceDisplayNameGenerator.setDisplayName(modification);
            InstanceDisplayNameGenerator.setDisplayName(clonedMember);
            index ++;
        }
    }

    private void handleNames(Map<GKInstance, GKInstance> memberToCloned) throws Exception {
        // Two cases
        if (individualNameCheckBox.isSelected()) {
            if (memberToName.size() > 0) {
                for (GKInstance original : memberToName.keySet()) {
                    String name = memberToName.get(original);
                    if (name == null || name.length() == 0)
                        continue;
                    GKInstance cloned = memberToCloned.get(original);
                    List names = cloned.getAttributeValuesList(ReactomeJavaConstants.name);
                    if (!names.contains(names)) {
                        names.add(0, name);
                        cloned.setAttributeValue(ReactomeJavaConstants.name, names);
                    }
                }
            }
        }
        else {
            String prefix = prefixTF.getText();
            if (prefix.trim().length() == 0)
                prefix = "";
            // Replace names
            if (findTF.getText().trim().length() > 0) {
                String find = findTF.getText().trim();
                String replace = replaceTF.getText();
                for (GKInstance original : memberToCloned.keySet()) {
                    GKInstance cloned = memberToCloned.get(original);
                    String name = (String) original.getAttributeValue(ReactomeJavaConstants.name);
                    if (name != null) {
                        name = name.replaceAll(find, replace);
                        name = prefix + name;
                        List list = cloned.getAttributeValuesList(ReactomeJavaConstants.name);
                        list.add(0, name);
                        cloned.setAttributeValue(ReactomeJavaConstants.name, list);
                    }
                }
            }
            else if (prefix.trim().length() > 0) { // Should have something there. Not just an empty
                for (GKInstance original : memberToCloned.keySet()) {
                    GKInstance cloned = memberToCloned.get(original);
                    String name = (String) original.getAttributeValue(ReactomeJavaConstants.name);
                    if (name != null) {
                        name = prefix + name;
                        List list = cloned.getAttributeValuesList(ReactomeJavaConstants.name);
                        list.add(0, name);
                        cloned.setAttributeValue(ReactomeJavaConstants.name, list);
                    }
                }
            }
        }
    }

    public void setInstance(GKInstance set) {
        this.definedSet = set;
        XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
        Schema schema = fileAdaptor.getSchema();
        SchemaClass top = schema.getClassByName(ReactomeJavaConstants.AbstractModifiedResidue);
        List<SchemaClass> classes = new ArrayList<SchemaClass>();
        classes.add(top);
        classes = InstanceUtilities.getAllSchemaClasses(classes);
        SchemaClass selectedClass = schema.getClassByName(ReactomeJavaConstants.ModifiedResidue);
        setSchemaClasses(classes, selectedClass);
        instLabel.setText(definedSet.getDisplayName());
    }
    
    /**
     * Make sure the passed DefinedSet instance can be used in this utility. Only DefinedSet
     * having EWAS instances as its members can be used in this utility since only EWAS can 
     * have hasModifiedResidue value.
     * @param inst
     * @return
     */
    public boolean validEntitySet(GKInstance inst) {
        // Make sure this is a DefinedSet
        if (!inst.getSchemClass().isa(ReactomeJavaConstants.DefinedSet)) {
            JOptionPane.showMessageDialog(getOwner(),
                                          "Only DefinedSet instance can be used in the EntitySet modification utility!",
                                          "Class Not Supported",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (inst.isShell()) {
            JOptionPane.showMessageDialog(getOwner(),
                                          "The DefinedSet modification utility cannot work for a shell instance.\n" +
                                          "Please download the instance from DB first.",
                                          "Shell Instance",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Make sure only EWAS instances as members
        try {
            List<GKInstance> members = inst.getAttributeValuesList(ReactomeJavaConstants.hasMember);
            if (members == null || members.size() == 0) {
                JOptionPane.showMessageDialog(getOwner(), 
                                              "No member has been specified in the selected DefinedSet instance: " + inst,
                                              "No Member in DefinedSet", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            for (GKInstance inst1 : members) {
                if (!inst1.getSchemClass().isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                    JOptionPane.showMessageDialog(getOwner(),
                                                  "Only DefinedSet having EWAS as its members can be used in the EntitySet modification utility!",
                                                  "Instance Nor Supported",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            // Have to make sure all hasMembers have been fully checked out so that cloning can work
            boolean hasShell = false;
            for (GKInstance inst1 : members) {
                if (inst1.isShell()) {
                    hasShell = true;
                    break;
                }
            }
            if (hasShell) {
                int reply = JOptionPane.showConfirmDialog(getOwner(), 
                                                          "Some of members in the selected DefinedSet are shell instances. \n" + 
                                                          "You have to check out members fully from DB first. Do you want\n" + 
                                                          "the utility to check out for you?",
                                                          "Shell DefinedSet Member",
                                                          JOptionPane.OK_CANCEL_OPTION);
                if (reply == JOptionPane.CANCEL_OPTION)
                    return false;
                // Need to check out shell instances
                for (GKInstance inst1 : members) {
                    if (inst1.isShell()) {
                        boolean downloaded = downloadShellInstance(inst1);
                        if (!downloaded)
                            return false;
                    }
                }
            }
            return true;
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(getOwner(),
                                          "Error in instance validation: " + e,
                                          "Error in Modification Creation",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private boolean downloadShellInstance(GKInstance shellInstance) {
        // Get the mysqlAdaptor
        MySQLAdaptor adaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(getOwner());
        if (adaptor == null) {
            JOptionPane.showMessageDialog(getOwner(),
                                          "Cannot connect to the database",
                                          "Error in DB Connecting",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            GKInstance dbInstance = adaptor.fetchInstance(shellInstance.getSchemClass().getName(),
                                                          shellInstance.getDBID());
            if (dbInstance == null) {
                JOptionPane.showMessageDialog(getOwner(),
                                              "Cannot find the instance in the database. " +
                                              "The instance \nmight be deleted in the database.",
                                              "Error in Downloading",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            SynchronizationManager.getManager().updateFromDB(shellInstance, dbInstance);
            // Update the view etc.
            AttributeEditManager.getManager().attributeEdit(shellInstance);
            // The above call will add dirty flag to shellInstance. Have to remove it explicity.
            XMLFileAdaptor fileAdaptor = PersistenceManager.getManager().getActiveFileAdaptor();
            fileAdaptor.removeDirtyFlag(shellInstance);
            return true;
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(getOwner(), 
                                          "Cannot download a shell instance: " + shellInstance,
                                          "Eror in Downloading",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private class MemberPropertyDialog extends JDialog {
        private boolean isOKClicked;
        private Map<GKInstance, String> instToValue;
        private JTable table;
        
        public MemberPropertyDialog(JDialog parentDialog,
                                    String[] tableHeaders) {
            super(parentDialog);
            init(tableHeaders);
        }
        
        public void setInstanceToValue(Map<GKInstance, ?> newInstanceToValue) {
            if (instToValue != null)
                instToValue.clear();
            for (GKInstance inst : newInstanceToValue.keySet()) {
                Object value = newInstanceToValue.get(inst);
                if (value != null)
                    instToValue.put(inst, value.toString());
            }
        }
        
        private void init(String[] tableHeaders) {
            table = new JTable();
            MemberPropertyTableModel tableModel = new MemberPropertyTableModel();
            try {
                List<?> members = definedSet.getAttributeValuesList(ReactomeJavaConstants.hasMember);
                tableModel.setMembers(members);
                instToValue = new HashMap<GKInstance, String>();
                tableModel.instanceToValue = instToValue;
                tableModel.setColumnHeaders(tableHeaders);
            }
            catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(EntitySetDeepCloneDialog.this,
                                              "Cannot get hasMember values from the selected DefinedSet: " + e, 
                                              "Error in DefinedSet", 
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            table.setModel(tableModel);
            table.setDefaultRenderer(GKInstance.class, new InstanceTableCellRenderer());
            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Check where the mouse point is
                        Point p = e.getPoint();
                        int col = table.columnAtPoint(p);
                        if (col != 0)
                            return;
                        int row = table.rowAtPoint(p);
                        GKInstance inst = (GKInstance) table.getValueAt(row, col);
                        FrameManager.getManager().showInstance(inst, 
                                                               MemberPropertyDialog.this,
                                                               inst.getDbAdaptor() instanceof XMLFileAdaptor);
                    }
                }
            });
            getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    setVisible(false);
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    setVisible(false);
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setModal(true);
            setLocationRelativeTo(getOwner());
            setSize(400, 500);
        }
        
        public Map<GKInstance, String> getInstanceToValue() {
            return instToValue;
        }
    }
    
    private class MemberPropertyTableModel extends AbstractTableModel {
        private String[] columnHeaders;
        private Map<GKInstance, String> instanceToValue;
        private List<GKInstance> members;
        
        public MemberPropertyTableModel() {
            
        }
        
        @Override
        public String getColumnName(int column) {
            return columnHeaders[column];
        }

        public void setColumnHeaders(String[] headers) {
            this.columnHeaders = headers;
        }
        
        public void setMembers(List<?> members) {
            this.members = new ArrayList<GKInstance>();
            for (Iterator<?> it = members.iterator(); it.hasNext();) {
                this.members.add((GKInstance)it.next());
            }
        }

        @Override
        public int getRowCount() {
            return members.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            GKInstance inst = members.get(rowIndex);
            if (columnIndex == 0)
                return inst;
            else
                return instanceToValue.get(inst);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == 1)
                return true;
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return GKInstance.class;
            return String.class;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            GKInstance inst = members.get(rowIndex);
            instanceToValue.put(inst, aValue.toString());
        }
        
        
    }
    
    private class InstanceTableCellRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
        // We only need one icon. Use it as a static variable.
        private Icon icon = AuthorToolAppletUtilities.createImageIcon("Instance.gif");
        
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            super.setIcon(icon);
            GKInstance inst = (GKInstance) value;
            super.setText(inst.getDisplayName());
            return comp;
        }
        
    }
    
}
