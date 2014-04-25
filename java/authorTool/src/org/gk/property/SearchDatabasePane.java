/*
 * Created on Nov 6, 2006
 *
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.PersistenceManager;
import org.gk.render.RenderableRegistry;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JPanel is used to search database for reactions or complexes.
 * @author guanming
 *
 */
public class SearchDatabasePane extends JPanel {
    protected MySQLAdaptor dbAdaptor;
    // GUI Controls
    protected JButton searchBtn;
    protected JButton cancelBtn;
    protected JButton okBtn;
    protected JTextField tf;
    protected JComboBox typeBox;
    protected JCheckBox checkbox;
    protected JList instanceList;
    protected JLabel htmlLabel;
    protected JLabel listLabel;
    protected GKInstanceHTMLPropertyPane htmlPane;
    protected boolean isInDisplayMode = false;
    // types
    private SearchDBTypeHelper typeHelper;
    
    public SearchDatabasePane() {
        init();
    }
    
    private void init() {
        typeHelper = new SearchDBTypeHelper();
        JPanel searchPane = createSearchPane();
        setLayout(new BorderLayout());
        add(searchPane, BorderLayout.CENTER);
        setPreferredSize(new Dimension(400, 200));
        installListeners();
    }
    
    protected JPanel createSearchPane() {
        JPanel searchPane = new JPanel();
        searchPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        if (isInDisplayMode)
            constraints.insets = new Insets(0, 4, 0, 4);
        else
            constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel("Search database for: ");
        if (typeBox == null) {
            typeBox = new JComboBox(typeHelper.getTypes());
            typeBox.setEditable(false);
        }
        constraints.gridwidth = 2;
        searchPane.add(label, constraints);
        constraints.gridx = 2;
        constraints.gridwidth = 1;
        searchPane.add(typeBox, constraints);
        JLabel label1 = new JLabel("With name: ");
        if (tf == null)
            tf = new JTextField();
        constraints.gridx = 0;
        constraints.gridy = 1;
        searchPane.add(label1, constraints);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        searchPane.add(tf, constraints);
        if (checkbox == null)
            checkbox = new JCheckBox("Match whole name only");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        searchPane.add(checkbox, constraints);
        if (searchBtn == null) {
            searchBtn = new JButton("Search DB");
            searchBtn.setEnabled(false);
        }
        if (!isInDisplayMode) {
            // Add two buttons
            JPanel btnPane = new JPanel();
            btnPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));
            cancelBtn = new JButton("Cancel");
            cancelBtn.setPreferredSize(searchBtn.getPreferredSize());
            btnPane.add(searchBtn);
            btnPane.add(cancelBtn);
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 3;
            searchPane.add(btnPane, constraints);
        }
        else {
            constraints.gridx = 3;
            constraints.gridy = 1;
            constraints.insets = new Insets(2, 8, 2, 4);
            searchPane.add(searchBtn, constraints);
            Border border1 = BorderFactory.createEmptyBorder(4, 0, 4, 0);
            Border border2 = BorderFactory.createEtchedBorder();
            searchPane.setBorder(BorderFactory.createCompoundBorder(border2, border1));
        }
        return searchPane;
    }
    
    private void installListeners() {
        tf.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent e) {
            }

            public void insertUpdate(DocumentEvent e) {
                validateSearchBtn();
            }

            public void removeUpdate(DocumentEvent e) {
                validateSearchBtn();
            }
        });
        tf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = tf.getText().trim();
                if (txt.length() > 0)
                    search();
            }
        });
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                search();
            }
        });
    }
    
    public void focusTextField() {
        tf.requestFocus();
    }
    
    private void validateSearchBtn() {
        String text = tf.getText().trim();
        if (text.length() > 0)
            searchBtn.setEnabled(true);
        else
            searchBtn.setEnabled(false);
    }
    
    public void addCancelAction(ActionListener l) {
        cancelBtn.addActionListener(l);
    }
    
    public void addOKAction(ActionListener l) {
        if (okBtn == null) {
            okBtn = new JButton("Download");
        }
        okBtn.addActionListener(l);
    }
    
    protected List getSchemaClassNames() {
        String type = (String) typeBox.getSelectedItem();
        List clsNames = typeHelper.mapTypeToReactomeCls(type);
        return clsNames;
    }
    
    protected String getAttributeName() {
        return ReactomeJavaConstants._displayName;
    }
    
    protected String[] getQueryParameters() {
        String operator = null;
        String query = null;
        if (!checkbox.isSelected()) {
            query = "%" + tf.getText().trim() + "%";
            operator = "LIKE";
        }
        else {
            query = tf.getText().trim();
            operator = "=";
        }
        return new String[]{query, operator};
    }
    
    protected void filterSearchResults(List instances) throws Exception {
        String type = (String) typeBox.getSelectedItem();
        // Need to filter out based on types
        if (type.equals("Protein") || type.equals("Gene") || type.equals("RNA")) {
            dbAdaptor.loadInstanceAttributeValues(instances, 
                                                  new String[]{ReactomeJavaConstants.referenceEntity});
            for (Iterator it = instances.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                if (!instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.referenceEntity)) {
                    // Just use it
                    continue;
                }
                GKInstance refPepSeq = (GKInstance) instance.getAttributeValue(ReactomeJavaConstants.referenceEntity);
                if (refPepSeq == null)
                    continue;
                if (type.equals("Protein")) { 
                    if (!refPepSeq.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
                        !refPepSeq.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct))
                        it.remove();
                }
                else if (type.equals("Gene")) {
                    if (!refPepSeq.getSchemClass().isa(ReactomeJavaConstants.ReferenceDNASequence))
                        it.remove();
                }
                else if (type.equals("RNA")) {
                    if (!refPepSeq.getSchemClass().isa(ReactomeJavaConstants.ReferenceRNASequence))
                        it.remove();
                }
            }
        }
    }
    
    private void search() {
        if (dbAdaptor == null) {
            JOptionPane.showMessageDialog(this,
                                          "Database is not connected!",
                                          "Error in Search",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Check if like is needed
        String[] paras = getQueryParameters();
        List clsNames = getSchemaClassNames();
        String attName = getAttributeName();
        List instances = new ArrayList();
        try {
            for (Iterator it = clsNames.iterator(); it.hasNext();) {
                String clsName = (String) it.next();
                if (!dbAdaptor.getSchema().isValidClass(clsName))
                    continue; // Just a sanity check to avoid any invalid class name.
                Collection c = dbAdaptor.fetchInstanceByAttribute(clsName, 
                                                                  attName, 
                                                                  paras[1], 
                                                                  paras[0]);
                if (c != null)
                    instances.addAll(c);
            }
            filterSearchResults(instances);
            displayInstances(instances);
        }
        catch(Exception e) {
            System.err.println("SearchDatabasePane.search(): " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                                          "Error in search: " + e,
                                          "Error in Search",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void displayInstances(List instances) {
        InstanceUtilities.sortInstances(instances);
        // Clean up a little bit in case some of instances have been selected before.
        // isSelected are cached in instances
        for (Iterator it = instances.iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Object obj = instance.getAttributeValueNoCheck("isSelected");
            if (obj != null)
                instance.removeAttributeValueNoCheck("isSelected", obj);
        }
        // Rearrange GUIs
        initGUIsForDisplay();
        listLabel.setText("Found Instances: " + instances.size());
        DefaultListModel model = (DefaultListModel) instanceList.getModel();
        model.clear();
        for (Iterator it = instances.iterator(); it.hasNext();) {
            model.addElement(it.next());
        }
    }
    
    public List getSelectedObjects() {
        DefaultListModel model = (DefaultListModel) instanceList.getModel();
        List instances = new ArrayList();
        int size = model.getSize();
        GKInstance instance = null;
        Boolean isSelected = null;
        for (int i = 0; i < size; i++) {
            instance = (GKInstance) model.getElementAt(i);
            isSelected = (Boolean) instance.getAttributeValueNoCheck("isSelected");
            if (isSelected != null && isSelected.booleanValue()) {
                instances.add(instance);
            }
        }
        checkDuplications(instances);
        // Convert instances to Renderable
        String type = (String) typeBox.getSelectedItem();
        List rtn = typeHelper.mapGKInstanceToRenderable(type, instances);
        return rtn;
    }
    
    private void checkDuplications(List objects) {
        List existing = new ArrayList();
        for (Iterator it = objects.iterator(); it.hasNext();) {
            GKInstance r = (GKInstance) it.next();
            String name = r.getDisplayName();
            if (RenderableRegistry.getRegistry().contains(name)) {
                it.remove();
                existing.add(name);
            }
        }
        // Generate reports
        if (existing.size() == 0)
            return;
        StringBuffer message = new StringBuffer();
        if (existing.size() == 1) {
            String name = (String) existing.get(0);
            message.append("Object with this name \"" + name + "\" has existed in the editing pathway.\n");
            message.append("This object cannnot be added to the pathway.");
        }
        else {
            message.append("Objects with the following names have existed in the editing pathway.\n");
            message.append("These objects cannot be added to the pathway:\n\n");
            for (Iterator it = existing.iterator(); it.hasNext();) {
                message.append(it.next());
                if (it.hasNext())
                    message.append("\n");
            }
        }
        JOptionPane.showMessageDialog(this, 
                                      message.toString(),
                                      "Duplication Checking",
                                      JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void initGUIsForDisplay() {
        // Use this flag to initialize this GUI once, only once
        if (isInDisplayMode)
            return;
        isInDisplayMode = true;
        removeAll();
        // Search Pane
        JPanel searchPane = createSearchPane();
        setLayout(new BorderLayout());
        add(searchPane, BorderLayout.NORTH);
        JPanel leftPane = new JPanel();
        leftPane.setBorder(BorderFactory.createEtchedBorder());
        // Have to set this size to enable scrolling
        leftPane.setMinimumSize(new Dimension(50, 50));
        listLabel = GKApplicationUtilities.createTitleLabel("Found Instances");
        instanceList = new JList();
        instanceList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instanceList.setModel(new DefaultListModel());
        instanceList.setCellRenderer(new ListCellRenderer());
        instanceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                GKInstance instance = (GKInstance) instanceList.getSelectedValue();
                if (instance == null) {
                    htmlLabel.setText("Properties");
                    htmlPane.clean();
                }
                else {
                    htmlLabel.setText("Properties: " + instance.getDisplayName());
                    htmlPane.setInstance(instance);
                }
            }
        });
        instanceList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int row = instanceList.locationToIndex(e.getPoint());
                if (row < 0)
                    return;
                if (x < 16)
                    toggleSelection(row);
            }
        });
        leftPane.setLayout(new BorderLayout());
        leftPane.add(listLabel, BorderLayout.NORTH);
        leftPane.add(new JScrollPane(instanceList), BorderLayout.CENTER);
        JPanel rightPane = new JPanel();
        rightPane.setMinimumSize(new Dimension(50, 50));
        rightPane.setBorder(BorderFactory.createEtchedBorder());
        htmlLabel = GKApplicationUtilities.createTitleLabel("Properties");
        htmlPane = new GKInstanceHTMLPropertyPane();
        rightPane.setLayout(new BorderLayout());
        rightPane.add(htmlLabel, BorderLayout.NORTH);
        rightPane.add(new JScrollPane(htmlPane), BorderLayout.CENTER);
        JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                        leftPane,
                                        rightPane);
        jsp.setDividerSize(3);
        add(jsp, BorderLayout.CENTER);
        // Try to determine the size based on its parent
        JDialog dialog = (JDialog) SwingUtilities.getRoot(this);
        int width = 600; // Initialize size
        if (dialog != null) {
            Window owner = dialog.getOwner();
            Dimension size = owner.getSize();
            // Want to take 2/3 size of the parent
            int x = (int) (size.getWidth() / 6);
            x += owner.getX();
            int y = (int) (size.getHeight() / 6);
            y += owner.getY();
            width = (int) (size.width * 0.67);
            int height = (int) (size.height * 0.67);
            invalidate();
            dialog.setSize(width, height);
            dialog.setLocation(x, y);
            dialog.validate();
        }
        else {
            Component parent = getParent();
            Dimension size = parent.getSize();
            // Want to take 2/3 size of the parent
            int x = (int) (size.getWidth() / 6);
            int y = (int) (size.getHeight() / 6);
            width = (int) (size.width * 0.67);
            int height = (int) (size.height * 0.67);
            setBounds(new Rectangle(x, y, width, height));
        }
        jsp.setDividerLocation(width / 2);
        // Add a control pane
        JPanel controlPane = createControlPane();
        add(controlPane, BorderLayout.SOUTH);
        validate();
        repaint();
    }

    protected JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        if (okBtn == null)
            okBtn = new JButton("Download");
        //okBtn.setPreferredSize(cancelBtn.getPreferredSize());
        // okBtn is bigger than cancelBtn
        cancelBtn.setPreferredSize(okBtn.getPreferredSize());
        controlPane.add(okBtn);
        controlPane.add(cancelBtn);
        okBtn.setEnabled(false);
        return controlPane;
    }
    
    private void validateOKBtn() {
        DefaultListModel model = (DefaultListModel) instanceList.getModel();
        int size = model.getSize();
        GKInstance instance = null;
        Boolean isSelected = null;
        for (int i = 0; i < size; i++) {
            instance = (GKInstance) model.getElementAt(i);
            isSelected = (Boolean) instance.getAttributeValueNoCheck("isSelected");
            if (isSelected != null && isSelected.booleanValue()) {
                okBtn.setEnabled(true);
                return;
            }
        }
        okBtn.setEnabled(false);
    }
    
    protected void toggleSelection(int row) {
        GKInstance instance = (GKInstance) instanceList.getModel().getElementAt(row);
        Boolean isSelected = (Boolean) instance.getAttributeValueNoCheck("isSelected");
        if (isSelected == null || !isSelected.booleanValue())
            instance.setAttributeValueNoCheck("isSelected", Boolean.TRUE);
        else
            instance.setAttributeValueNoCheck("isSelected", Boolean.FALSE);
        // A simple repaint: It is possible to figure out the extact location needs to 
        // be repainted.
        instanceList.repaint(instanceList.getVisibleRect());
        validateOKBtn();
    }
    
    /**
     * Have to initialize a database connection first. If a database connection
     * cannot be established, false will be returned. Otherwise, true will be 
     * returned.
     * @return
     */
    public boolean initDatabaseConnection() {
        PersistenceManager.getManager().initDatabaseConnection(this);
        dbAdaptor = PersistenceManager.getManager().getActiveMySQLAdaptor(this);
        return dbAdaptor != null;
    }
    
    private class ListCellRenderer extends DefaultListCellRenderer {
        private ImageIcon selectIcon;
        private ImageIcon unselectIcon;
        
        public ListCellRenderer() {
            selectIcon = AuthorToolAppletUtilities.createImageIcon("Selected.png");
            unselectIcon = AuthorToolAppletUtilities.createImageIcon("Unselected.png");
        }
        
        public Component getListCellRendererComponent(JList list, 
                                                      Object value, 
                                                      int index, 
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, 
                                                                value, 
                                                                index, 
                                                                isSelected,
                                                                cellHasFocus);
            GKInstance instance = (GKInstance) value;
            try {
                setText(instance.getDisplayName());
                Boolean selectedValue = (Boolean) instance.getAttributeValueNoCheck("isSelected");
                if (selectedValue == null || !selectedValue.booleanValue())
                    setIcon(unselectIcon);
                else
                    setIcon(selectIcon);
            }
            catch(Exception e) {
                System.err.println("SearchDatabasePane.listCellRenderer(): " + e);
                e.printStackTrace();
            }
            return comp;
        }
        
    }
}
