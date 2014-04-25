/*
 * Created on May 6, 2005
 */
package org.gk.slicing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.apache.batik.ext.swing.GridBagConstants;
import org.gk.database.DefaultInstanceEditHelper;
import org.gk.database.EventPanePopupManager;
import org.gk.database.EventTreeBuildHelper;
import org.gk.database.HierarchicalEventPane;
import org.gk.database.SynchronizationManager;
import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.util.GKApplicationUtilities;

/**
 * This customized JFrame is used to do a batch _doNotRelease labeling for events. It 
 * will work directly on the database.
 * @author wgm
 */
public class EventReleaseWizard extends JFrame {

    private WizardInstructionPane instructionsPane;
    private JPanel rightPane;
    private JButton nextBtn;
    private JButton backBtn;
    private JButton cancelBtn;
    // Database DBA
    private MySQLAdaptor dba;
    // selected top event to be released
    private GKInstance topEvent;
    // cache all panes to speed up the process and keep the user's selection
    private JPanel allEventPane;
    private EventReleaseCheckPane checkDNRPane;
    private PreviewPanel previewPane;
    
    public EventReleaseWizard(MySQLAdaptor dba) {
        // dba should not be null
        if (dba == null)
            throw new IllegalArgumentException("EventReleasedWizard: null database adaptor");
        init();
        this.dba = dba;
        // First start
        showPanelFor(0);
    }
    
    private void init() {
        instructionsPane = new WizardInstructionPane();
        rightPane = new JPanel();
        rightPane.setLayout(new BorderLayout());
        JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                        instructionsPane,
                                        rightPane);
        jsp.setDividerSize(0);
        jsp.setResizeWeight(0.33);
        getContentPane().add(jsp, BorderLayout.CENTER);
        getContentPane().add(createControlPanel(), BorderLayout.SOUTH);
        // Original size
        instructionsPane.setSize(220, 380);
        rightPane.setSize(440, 380);
    }
    
    private JPanel createControlPanel() {
        JPanel btnPane = new JPanel();
        btnPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.anchor = GridBagConstants.EAST;
        constraints.weightx = 0.5;
        backBtn = new JButton("Back");
        backBtn.setMnemonic('B');
        btnPane.add(backBtn, constraints);
        nextBtn = new JButton("Next");
        nextBtn.setMnemonic('N');
        constraints.gridx = 1;
        constraints.weightx = 0.0;
        btnPane.add(nextBtn, constraints);
        cancelBtn = new JButton("Cancel");
        constraints.insets = new Insets(8, 8, 8, 11);
        constraints.anchor = GridBagConstants.EAST;
        constraints.gridx = 2;
        constraints.weightx = 0.5;
        btnPane.add(cancelBtn, constraints);
        btnPane.setBorder(BorderFactory.createEtchedBorder());
        backBtn.setPreferredSize(cancelBtn.getPreferredSize());
        nextBtn.setPreferredSize(cancelBtn.getPreferredSize());
        ActionListener l = createAction();
        backBtn.addActionListener(l);
        nextBtn.addActionListener(l);
        cancelBtn.addActionListener(l);
        return btnPane;
    }
    
    private ActionListener createAction() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton btn = (JButton) e.getSource();
                if (btn == backBtn) {
                    back();
                }
                else if (btn == nextBtn) {
                    next();
                }
                else {
                    cancel();
                }
            }
        };
        return l;
    }
    
    private void next() {
        instructionsPane.next();
        int index = instructionsPane.getSelectedIndex();
        if(!showPanelFor(index))
            instructionsPane.back();
    }
    
    private void back() {
        instructionsPane.back();
        int index = instructionsPane.getSelectedIndex();
        if(!showPanelFor(index))
            instructionsPane.back();
    }
    
    /**
     * Display a Panel based on the selected index.
     * @param index
     */
    private boolean showPanelFor(int index) {
        JPanel displayingPane = null;
        switch(index) {
        	case 0 :    
        	    displayingPane = getEventCentricView();
        	    break;
        	case 1 :
        	    displayingPane = getEventPanelForReleasing();
        	    break;
        	case 2 :
        	    displayingPane = generatePreviewPane();
        	    break;
        	case 3 : // For database commit
        	    displayingPane = generateCommitPane();
        	    break;
        }
        if (displayingPane != null) {
            rightPane.removeAll();
            rightPane.add(displayingPane, BorderLayout.CENTER);
            rightPane.revalidate();
            rightPane.repaint();
            return true;
        }
        return false;
    }
    
    private JPanel generateCommitPane() {
        final List changedEvents = checkDNRPane.getChangedEvents();
        if (changedEvents.size() == 0) {
            JOptionPane.showMessageDialog(this,
                                          "Nothing changed.",
                                          "Information",
                                          JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "Are you sure you want to commit the changes?",
                                                  "Commit Changes?",
                                                  JOptionPane.YES_NO_OPTION);
        if (reply != JOptionPane.YES_OPTION)
            return null;
        // Need the default InstanceEdit
        final JPanel commitPane = new JPanel();
        commitPane.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        final JLabel label = new JLabel("Commit Changes...");
        commitPane.add(label, constraints);
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(changedEvents.size());
        constraints.gridy = 1;
        commitPane.add(progressBar, constraints);
        // disable next, back
        // Use a new thread to check in changes
        Thread t = new Thread() {
            public void run() {
                try {
                    checkInChanges(changedEvents, progressBar);
                    commitPane.remove(progressBar);
                    commitPane.revalidate();
                    label.setText("Changes are committed successfully");
                    commitPane.repaint();
                    nextBtn.setEnabled(false);
                    backBtn.setEnabled(false);
                    cancelBtn.setText("Close");
                }
                catch(Exception e) {
                    System.err.println("EventReleaseWizard.generateCommitPane(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
        return commitPane;
    }
    
    private void checkInChanges(List changedInstances, JProgressBar progressBar) throws Exception {
        progressBar.setIndeterminate(true);
        GKInstance defaultIE = SynchronizationManager.getManager().getDefaultInstanceEdit(this);
        if (defaultIE == null)
            throw new IllegalStateException("EventReleaseWizard.checkInChanges(): Cannot get default InstanceEdit.");
        DefaultInstanceEditHelper ieHelper = SynchronizationManager.getManager().getDefaultIEHelper();
        try {
            dba.startTransaction();
            GKInstance ieClone = ieHelper.attachDefaultIEToDBInstances(changedInstances, defaultIE);
            if (ieClone == null) {
                throw new IllegalStateException("Cannot attach default InstanceEdit.");
            }
            // ieClose should be stored first to get the db id
            // Have to make sure all local instances can also be saved.
            dba.storeInstance(ieClone);
            progressBar.setIndeterminate(false);
            progressBar.setMinimum(0);
            progressBar.setMaximum(changedInstances.size());
            int c = 0;
            for (Iterator it = changedInstances.iterator(); it.hasNext();) {
                GKInstance event = (GKInstance) it.next();
                dba.updateInstanceAttribute(event, "_doNotRelease");
                dba.updateInstanceAttribute(event, "modified");
                progressBar.setValue(++c);
            }
            dba.commit();
        }
        catch (Exception e) {
            System.err.println("EventReleaseWizard.checkInChanges(): " + e);
            e.printStackTrace();
            dba.rollback();
            throw e;
        }
    }
    
    private JPanel generatePreviewPane() {
        if (previewPane == null)
            previewPane = new PreviewPanel();
        previewPane.setTopLevelEvents(checkDNRPane.getReleasedTopLevelEvents());
        previewPane.setSelectedEvent(topEvent);
        return previewPane;
    }
    
    private JPanel getEventPanelForReleasing() {
        if (topEvent == null) {
            JOptionPane.showMessageDialog(this,
                                         "Please choose a top level event for release before continuing.",
                                         "No Top Event Selected",
                                         JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (checkDNRPane == null)
            checkDNRPane = new EventReleaseCheckPane();
        checkDNRPane.setTopEvent(topEvent, dba);
        return checkDNRPane;
    }
    
    private JPanel getEventCentricView() {
        if (allEventPane != null)
            return allEventPane;
        // This is a two part pane: center is the event tree
        // the bottom is the selected event.
        allEventPane = new JPanel();
        allEventPane.setLayout(new BorderLayout());
        final HierarchicalEventPane eventPane = new HierarchicalEventPane();
        eventPane.setBorder(BorderFactory.createEtchedBorder());
        eventPane.setPopupType(EventPanePopupManager.DB_AUTHOR_TOOL_TYPE);
        eventPane.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        JLabel titleLabel = GKApplicationUtilities.createTitleLabel("Select a top event for release:");
        allEventPane.add(titleLabel, BorderLayout.NORTH);
        final JLabel label = GKApplicationUtilities.createTitleLabel("No event selected");
        eventPane.addSelectionListener(new TreeSelectionListener() {
           public void  valueChanged(TreeSelectionEvent e) {
               GKInstance event = eventPane.getSelectedEvent();
               // This might be changed after back button is clicked
               if (event != null)
                   label.setText("Selected event: " + event.getExtendedDisplayName());
               else
                   label.setText("No event selected"); // To keep the space
               topEvent = event;
           }
        });
        allEventPane.add(label, BorderLayout.SOUTH);
        allEventPane.add(eventPane, BorderLayout.CENTER);
        EventTreeBuildHelper helper = new EventTreeBuildHelper(dba);
        try {
            Collection topLevelEvents = helper.getTopLevelEvents();
            eventPane.setTopLevelEvents(new ArrayList(topLevelEvents));
        }
        catch (Exception e) {
            System.err.println("EventReleasedWizard.getEventCentricView(): " + e);
            e.printStackTrace();
        }
        return allEventPane;
    }
    
    private void cancel() {
        dispose();
    }
    
    class PreviewPanel extends JPanel {
        
        private HierarchicalEventPane eventPane;
        
        public PreviewPanel() {
            init();
        }
        
        private void init() {
            setLayout(new BorderLayout());
            JLabel titleLabel = GKApplicationUtilities.createTitleLabel("Preview of released events:");
            eventPane = new HierarchicalEventPane() {
                protected void buildTree(DefaultMutableTreeNode treeNode, GKInstance event) {
            		java.util.List values = null;
            		try {
            		    if (event.getSchemClass().isValidAttribute("hasComponent")) {
            		        values = event.getAttributeValuesList("hasComponent");
            		        if (values != null) {
            		            for (Iterator it = values.iterator(); it.hasNext();) {
            		                GKInstance e = (GKInstance)it.next();
            		                if (e == null)
            		                    continue;
            		                Boolean dnr = (Boolean) e.getAttributeValue(ReactomeJavaConstants._doNotRelease);
            		                if (dnr == null || dnr.booleanValue())
            		                    continue;
            		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
            		                treeNode.add(subNode);
            		                node2Icon.put(subNode, iconPartOf);
            		                buildTree(subNode, e);
            		            }
            		        }
            		    }
            		    if (event.getSchemClass().isValidAttribute("hasInstance")) {
            		        values = event.getAttributeValuesListNoCheck("hasInstance");
            		        if (values != null) {
            		            for (Iterator it = values.iterator(); it.hasNext();) {
            		                GKInstance e = (GKInstance)it.next();
            		                if (e == null)
            		                    continue;
            		                Boolean dnr = (Boolean) e.getAttributeValue(ReactomeJavaConstants._doNotRelease);
            		                if (dnr == null || dnr.booleanValue())
            		                    continue;
            		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
            		                treeNode.add(subNode);
            		                node2Icon.put(subNode, iconIsA);
            		                buildTree(subNode, e);
            		            }
            		        }
            		    }
            		}
            		catch (Exception e) {
            			System.err.println("EventReleaseWizard.PreviewPanel.init(): " + e);
            			e.printStackTrace();
            		}
                }
            };
            eventPane.setPopupType(EventPanePopupManager.DB_AUTHOR_TOOL_TYPE);
            eventPane.setBorder(BorderFactory.createEtchedBorder());
            eventPane.setSelectedEvent(topEvent);
            add(titleLabel, BorderLayout.NORTH);
            add(eventPane, BorderLayout.CENTER);
        }
        
        public void setTopLevelEvents(List topEvents) {
            eventPane.setTopLevelEvents(topEvents);
        }
        
        public void setSelectedEvent(GKInstance event) {
            eventPane.setSelectedEvent(event);
        }
    }
}