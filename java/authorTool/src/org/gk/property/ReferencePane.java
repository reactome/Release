/*
 * Created on Jun 24, 2003
 *
 */
package org.gk.property;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.model.Reference;
import org.gk.render.ReactionNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderablePropertyNames;
import org.gk.render.RenderableReaction;
import org.gk.render.Shortcut;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.BrowserLauncher;
import org.gk.util.GKApplicationUtilities;

/**
 * @author wgm
 * This customized JPanel is used to edit literature references.
 */
public class ReferencePane extends RenderablePropertyPane implements ActionListener {
    // For title
    private JLabel titleLabel;
    // These control buttons
    private JButton addBtn;
    private JButton removeBtn;
    private JButton abstractBtn;
    // To hold pubmed
    private JList pmidList;
    
    public ReferencePane() {
        init();
    }
    
    private void init() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        //constraints.insets = new Insets(4, 4, 4, 4);
        titleLabel = new JLabel("References (PubMed IDs):");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 2));
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.WEST;
        add(titleLabel, constraints);
        // Add buttons
        addBtn = new JButton("Add", AuthorToolAppletUtilities.createImageIcon("Add16.gif"));
        removeBtn = new JButton("Delete", AuthorToolAppletUtilities.createImageIcon("Remove16.gif"));
        abstractBtn = new JButton("View", AuthorToolAppletUtilities.createImageIcon("Abstract.gif"));
        addBtn.setToolTipText("Click to add references");
        addBtn.setActionCommand("add");
        addBtn.addActionListener(this);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        add(addBtn, constraints);
        
        removeBtn.setActionCommand("remove");
        removeBtn.addActionListener(this);
        removeBtn.setToolTipText("Click to remove the selected references");
        constraints.gridx = 1;
        add(removeBtn, constraints);
        
        abstractBtn.setActionCommand("abstract");
        abstractBtn.addActionListener(this);
        abstractBtn.setToolTipText("Click to view the abstracts for the selected references");
        constraints.gridx = 2;
        add(abstractBtn, constraints);
        
        removeBtn.setEnabled(false);
        abstractBtn.setEnabled(false);
        
        // Add a list to hold PMIDs
        pmidList = createPMIDList();
        JScrollPane jsp = new JScrollPane(pmidList);
        //pmidList.setVisibleRowCount(3);
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 3;
        constraints.insets = new Insets(2, 16, 2, 4);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 0.5;
        constraints.weighty = 0.5;
        add(jsp, constraints);
    }
    
    private void doReferencesChanged() {
        if (r == null || duringSetting)
            return;
        List references = getReferences();
        List oldRef = (List) r.getAttributeValue(RenderablePropertyNames.REFERENCE);
        r.setAttributeValue(RenderablePropertyNames.REFERENCE, references);
        fireRenderablePropertyChange(r, 
                                     RenderablePropertyNames.REFERENCE, 
                                     oldRef, 
                                     references);
    }
    
    private JList createPMIDList() {
        final JList list = new JList();
        DefaultListModel model = new DefaultListModel();
        model.addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {
                doReferencesChanged();
            }
            public void intervalAdded(ListDataEvent e) {
                doReferencesChanged();
            }
            public void intervalRemoved(ListDataEvent e) {
                doReferencesChanged();
            }   
        });
        list.setModel(model);
        // Set renderer
        ListCellRenderer renderer = new DefaultListCellRenderer() {

            public Component getListCellRendererComponent(JList arg0, 
                                                          Object arg1,
                                                          int arg2, 
                                                          boolean arg3,
                                                          boolean arg4) {
                Reference ref = (Reference) arg1;
                return super.getListCellRendererComponent(arg0, ref.getPmid() + "", arg2, arg3, arg4);
            }
        };
        list.setCellRenderer(renderer);
        // Add these listeners to control the buttons
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) {
                    removeBtn.setEnabled(false);
                    abstractBtn.setEnabled(false);
                }
                else {
                    removeBtn.setEnabled(true);
                    abstractBtn.setEnabled(true);
                }
            }
        });
        list.setTransferHandler(new ListTransferHandler());
        // Enable to take pmid directly
        return list;
    }
    
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("add")) {
            addReferences();
        }
        else if (cmd.equals("remove")) {
            deleteReferences();
        }
        else if (cmd.equals("abstract")) {
            showAbstract();
        }
    }
    
    private void showAbstract() {
        Object[] values = pmidList.getSelectedValues();
        if (values == null || values.length == 0)
            return ;
        StringBuffer listIds = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
            Reference ref = (Reference) values[i];
            if (ref != null) {
                if (listIds.length() > 0)
                    listIds.append(","); // For multiple pmids
                listIds.append(ref.getPmid());
            }
        }
        String url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&list_uids=" + listIds + "&dopt=Abstract";
        if (AuthorToolAppletUtilities.isInApplet) {
            AuthorToolAppletUtilities.displayURL(url, "pubmed");
        }
        else {
            try {
                BrowserLauncher.displayURL(url,
                                           ReferencePane.this);
            }
            catch (IOException e1) {
                System.err.println("ReferencePane.init() 1: " + e1);
            }
        }
    }
    
    private void deleteReferences() {
        Object[] selected = pmidList.getSelectedValues();
        if (selected == null || selected.length == 0)
            return;
        DefaultListModel model = (DefaultListModel) pmidList.getModel();
        for (int i = 0; i < selected.length; i++)
            model.removeElement(selected[i]);
    }
    
    private void addReferences() {
        AddReferencePane panel = new AddReferencePane();
        Component container = SwingUtilities.getRoot(this);
        JDialog dialog = GKApplicationUtilities.createDialog(this, "Add PubMed Identifiers");
        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(panel);
        dialog.pack();
        dialog.setVisible(true);
    }
    
    public void setRenderable(Renderable newRenderable) {
        duringSetting = true;
        if (newRenderable instanceof Shortcut) 
            r = ((Shortcut)newRenderable).getTarget();
        else
            r = newRenderable;
        // Set title
        if (r instanceof ReactionNode ||
            r instanceof RenderableReaction)
            titleLabel.setText("References for Reaction");
        else if (r instanceof RenderablePathway)
            titleLabel.setText("References for Pathway");
        else
            titleLabel.setText("References");
        if (r != null) {
            java.util.List references = (java.util.List) r.getAttributeValue("references");
            setReferences(references);
        }
        duringSetting = false;
    }
    
    public void setReferences(java.util.List references) {
        DefaultListModel model = (DefaultListModel) pmidList.getModel();
        model.clear();
        if (references != null) {
            for (Iterator it = references.iterator(); it.hasNext();) {
                model.addElement(it.next());
            }
        }
    }
    
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    public java.util.List getReferences() {
        DefaultListModel model = (DefaultListModel) pmidList.getModel();
        java.util.List list = new ArrayList();
        for (int i = 0; i < model.getSize(); i++) {
            list.add(model.getElementAt(i));
        }
        if (list.size() == 0)
            return null;
        return list;
    }
    
    public void addReferenceDataListener(ListDataListener l) {
        DefaultListModel model = (DefaultListModel) pmidList.getModel();
        model.addListDataListener(l);
    }
    
    /**
     * To enable DnD to take PMID directly
     */
    private class ListTransferHandler extends TransferHandler {
        public boolean canImport(JComponent receptor, 
                                 DataFlavor[] flavors) {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(DataFlavor.stringFlavor))
                    return true;
            }
            return false;
        }

        public boolean importData(JComponent receptor, 
                                  Transferable data) {
            try {
                String text = (String) data.getTransferData(DataFlavor.stringFlavor);
                JList list = (JList) receptor;
                DefaultListModel model = (DefaultListModel) list.getModel();
                Reference ref = new Reference();
                ref.setPmid(Long.parseLong(text));
                model.addElement(ref);
            }
            catch(Exception e) {
                JOptionPane.showMessageDialog(receptor, 
                                              "Pubmed ID should be an integer!",
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
            return true;
        }
    }
    
    /**
     * This customized JPanel will be used to add PMIDs. This JPanel will
     * be used as a glass pane to block all other events.
     * @author guanming
     *
     */
    private class AddReferencePane extends JPanel {
        private JButton browseBtn;
        // Paste cannot work for a non-signed Java Applet
        private JButton pasteBtn;
        private JButton okBtn;
        private JButton cancelBtn;
        private JTextField pmidBox;
        
        public AddReferencePane() {
            init();
        }
        
        private void init() {
            JPanel contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.gridwidth = 4;
            constraints.gridheight = 2;
            constraints.anchor = GridBagConstraints.WEST;
            JTextArea label = new JTextArea("Please enter PubMed IDs in the following box. " +
                                             "Use commas to separate multiple IDs, e.g. 7559393, 15702989.");
            label.setBackground(getBackground());
            label.setLineWrap(true);
            label.setWrapStyleWord(true);
            label.setEditable(false);
            constraints.fill = GridBagConstraints.BOTH;
            contentPane.add(label, constraints);
            pmidBox = new JTextField();
            constraints.gridy = 2;
            constraints.gridheight = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            contentPane.add(pmidBox, constraints);
            // Add four buttons
            browseBtn = new JButton("Browse Pubmed");
            pasteBtn = new JButton("Paste");
            okBtn = new JButton("OK");
            cancelBtn = new JButton("Cancel");
            okBtn.setPreferredSize(cancelBtn.getPreferredSize());
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.EAST;
            contentPane.add(browseBtn, constraints);
            constraints.gridx = 1;
            contentPane.add(pasteBtn, constraints);
            constraints.gridx = 2;
            contentPane.add(okBtn, constraints);
            constraints.gridx = 3;
            contentPane.add(cancelBtn, constraints);
            installListeners();
            setLayout(new GridBagLayout());
            add(contentPane, new GridBagConstraints());
            contentPane.setPreferredSize(new Dimension(400, 200));
        }
        
        private void addReferences() {
            String text = pmidBox.getText().trim();
            if (text.length() == 0)
                return;
            String[] pmids = text.split(",");
            DefaultListModel model = (DefaultListModel) pmidList.getModel();
            for (int i = 0; i < pmids.length; i++) {
                Reference ref = new Reference();
                ref.setPmid(Long.parseLong(pmids[i].trim()));
                model.addElement(ref);
            }
        }
                
        public void dispose() {
            JDialog dialog = (JDialog) SwingUtilities.getRoot(this);
            if (dialog != null)
                dialog.dispose();
            else
                setVisible(false);
        }
        
        private void installListeners() {
            okBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    addReferences();
                }
            });
            cancelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            pasteBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    pmidBox.paste();
                    // Want to add "," to the end of the text
                    String text = pmidBox.getText();
                    if (text.trim().length() == 0)
                        return;
                    pmidBox.setText(text + ", ");
                }
            });
            browseBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (AuthorToolAppletUtilities.isInApplet) {
                        AuthorToolAppletUtilities.displayURL("http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", 
                                                             AuthorToolAppletUtilities.PUBMED_BROWSER_NAME);
                    }
                    else {
                        try {
                            BrowserLauncher.displayURL("http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=PubMed", 
                                                       AddReferencePane.this);
                        }
                        catch(IOException e1) {
                            System.err.println("AddReferencePane.installListeners(): " + e1);
                            e1.printStackTrace();
                        }
                    }
                }
            });
        }
    }
}
