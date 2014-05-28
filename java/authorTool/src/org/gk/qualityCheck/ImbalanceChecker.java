/*
 * Created on Apr 5, 2005
 *
 */
package org.gk.qualityCheck;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.gk.database.AttributePane;
import org.gk.database.InstanceListPane;
import org.gk.model.GKInstance;
import org.gk.model.InstanceUtilities;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.MySQLAdaptor;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.GKApplicationUtilities;

/**
 * This QualityCheck is used to check imbalance in an Reaction instance.
 * This class is modifed from Esther's QA_collection.pm script.
 * @author wgm
 *
 */
//@SuppressWarnings({"unchecked", "rawtypes"})
public class ImbalanceChecker extends ClassBasedQualityCheck {
    // A list of attributes should be checked
    protected final String[] checkedAttNames = new String[] {
            ReactomeJavaConstants.hasMember,
            ReactomeJavaConstants.hasCandidate,
            ReactomeJavaConstants.hasComponent,
            ReactomeJavaConstants.repeatedUnit
    };
    // Used to display checking results
    private ImbalanceDisplayPane displayPane;
    
    /**
     * Default constructor. It is required to load an instance dynamically.
     */
    public ImbalanceChecker() {     
    }
    
    public void check() {
        validateDataSource();
        if (!checkIsNeedEscape())
            return;
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    // Get the Reaction class
                    Schema schema = dataSource.getSchema();
                    SchemaClass cls = schema.getClassByName(ReactomeJavaConstants.Reaction);
                    initProgressPane("Check Reaction Imbalance");
                    progressPane.setText("Fetch Reactions...");
                    progressPane.setIndeterminate(true);
                    Collection reactions = dataSource.fetchInstancesByClass(cls);
                    checkReactions(reactions,
                                   dataSource instanceof MySQLAdaptor);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("ImbalanceChecker.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    public void checkProject(final GKInstance event) {
        validateDataSource();
        Thread t = new Thread() {
            public void run() {
                try {
                    initProgressPane("Check Reaction Imbalance");
                    progressPane.setText("Load contained events...");
                    progressPane.setIndeterminate(true);
                    Set<GKInstance> allEvents = InstanceUtilities.getContainedEvents(event);
                    allEvents.add(event);
                    // Get the list of reactions
                    List<GKInstance> reactions = new ArrayList<GKInstance>();
                    for (GKInstance event : allEvents) {
                        if (event.getSchemClass().isa(ReactomeJavaConstants.Reaction))
                            reactions.add(event);
                    }
                    checkReactions(reactions, 
                                   dataSource instanceof MySQLAdaptor);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("ImbalanceChecker.checkProject(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
    
    public void check(final GKSchemaClass cls) {
        // Only reactions should be checked for imbalance
        if (!cls.isa("Reaction")) {
            showErrorMessage();
            return;
        }
        check();
    }
    
    /**
     * Load reactions' attributes for quick performance.
     * @param reactions
     * @param reaction
     * @throws Exception
     */
    private void loadAttributes(Collection<GKInstance> reactions,
                                GKInstance reaction) throws Exception {
        MySQLAdaptor dba = (MySQLAdaptor) reaction.getDbAdaptor();
        Schema schema = dba.getSchema();
        SchemaClass cls = reaction.getSchemClass();
        progressPane.setText("Load inputs/outputs for Reactions...");
        SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.input);
        dba.loadInstanceAttributeValues(reactions, att);
        att = cls.getAttribute(ReactomeJavaConstants.output);
        dba.loadInstanceAttributeValues(reactions, att);
        // To speed up the performance, here all instances are loaded. In the future,
        // if the database becomes bigger, a server-side application should be used.
        if (progressPane.isCancelled())
            return;
        progressPane.setText("Load attribtues for Complexes...");
        loadAttributes(ReactomeJavaConstants.Complex, 
                       ReactomeJavaConstants.hasComponent, 
                       dba);
        if (progressPane.isCancelled())
            return;
        progressPane.setText("Load attribtues for EntitiSets...");
        loadAttributes(ReactomeJavaConstants.EntitySet, 
                       ReactomeJavaConstants.hasMember, 
                       dba);
        loadAttributes(ReactomeJavaConstants.CandidateSet, 
                       ReactomeJavaConstants.hasCandidate, 
                       dba);
        loadAttributes(ReactomeJavaConstants.Polymer, 
                       ReactomeJavaConstants.repeatedUnit,
                       dba);
        // Sort inputs and outputs and load different types differently
        Set<GKInstance> ewases = new HashSet<GKInstance>();
        for (Iterator<?> it = reactions.iterator(); it.hasNext();) {
            GKInstance tmp = (GKInstance) it.next();
            List<GKInstance> set1 = getEWASFromReaction(tmp, ReactomeJavaConstants.input);
            List<GKInstance> set2 = getEWASFromReaction(tmp, ReactomeJavaConstants.output);
            ewases.addAll(set1);
            ewases.addAll(set2);
        }
        cls = schema.getClassByName(ReactomeJavaConstants.EntityWithAccessionedSequence);
        att = cls.getAttribute(ReactomeJavaConstants.referenceEntity);
        if (progressPane.isCancelled())
            return;
        progressPane.setText("Load attribtues for EWAS...");
        dba.loadInstanceAttributeValues(ewases, att);
        att = cls.getAttribute(ReactomeJavaConstants.hasModifiedResidue);
        dba.loadInstanceAttributeValues(ewases,
                                        att);
        loadAttributes(ReactomeJavaConstants.GroupModifiedResidue,
                       ReactomeJavaConstants.modification,
                       dba);
        // Start and end coordinates will be used for checking if a reaction is a cleavage reaction.
        att = cls.getAttribute(ReactomeJavaConstants.startCoordinate);
        dba.loadInstanceAttributeValues(ewases,
                                        att);
        att = cls.getAttribute(ReactomeJavaConstants.endCoordinate);
        dba.loadInstanceAttributeValues(ewases,
                                        att);
    }
    
    private List<GKInstance> getEWASFromReaction(GKInstance reaction,
                                     String attName) throws Exception {
        List<GKInstance> rtn = new ArrayList<GKInstance>();
        List<?> valueList = reaction.getAttributeValuesList(attName);
        if (valueList == null)
            return rtn;
        for (Iterator<?> it = valueList.iterator(); it.hasNext();) {
            GKInstance value = (GKInstance) it.next();
            getEWASFromReaction(value, checkedAttNames, rtn);
        }
        return rtn;
    }
    
    private void getEWASFromReaction(GKInstance instance,
                                     String[] attNames,
                                     List<GKInstance> rtn) throws Exception {
        List current = new ArrayList();
        current.add(instance);
        List next = new ArrayList();
        List<GKInstance> tmp = new ArrayList<GKInstance>();
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                GKInstance value = (GKInstance) it.next();
                if (value.isShell()) {
                    tmp.add(value); // Should push a shell instance directly. It works more like
                                    // a marker
                    continue;
                }
                SchemaClass cls = value.getSchemClass();
                if (cls.isa(ReactomeJavaConstants.EntityWithAccessionedSequence)) {
                    tmp.add(value);
                    continue;
                }
                //Check if components in complex is found!
                for (int i = 0; i < attNames.length; i++) {
                    if (cls.isValidAttribute(attNames[i])) {
                        List list = value.getAttributeValuesList(attNames[i]);
                        if (list != null)
                            next.addAll(list);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
            // Avoid any self-containing: e.g. an EntitySet contains itself as its member
            // The following statement has been commented out as of Sept 1, 2011. An EWAS may be
            // contained by a complex, which is used as a subunit of another higher level complex.
            // At the same time, this higher level complex can have the same EWAS as its subunit.
            // However, commenting this out may make it going into an infinite loop if any self-containing
            // exits!
//            current.removeAll(tmp);
        }
        rtn.addAll(tmp);
    }
    
    private void checkReactions(Collection<GKInstance> reactions,
                                boolean needLoadAttributes) throws Exception {
        escapeInstances(reactions);
        // Nothing to check
        if (reactions == null || reactions.size() == 0) {
            hideProgressPane();
            return;
        }
        GKInstance reaction = (GKInstance) reactions.iterator().next();
        if (needLoadAttributes) {
            progressPane.setIndeterminate(true);
            loadAttributes(reactions, reaction);
        }
        if (progressPane.isCancelled())
            return;
        List<GKInstance> imbalanceList = new ArrayList<GKInstance>();
        int count = 0;
        progressPane.setText("Checking reaction imbalance...");
        progressPane.setIndeterminate(false);
        progressPane.setMinimum(0);
        progressPane.setMaximum(reactions.size());
        List<GKInstance> cleavageReactions = new ArrayList<GKInstance>();
        for (Iterator<GKInstance> it = reactions.iterator(); it.hasNext() && !progressPane.isCancelled();) {
            reaction = (GKInstance) it.next();
            List<GKInstance> inputEWASes= getEWASFromReaction(reaction, ReactomeJavaConstants.input);
            List<GKInstance> outputEWASes = getEWASFromReaction(reaction, ReactomeJavaConstants.output);
            // Check if there is any shell instances. If true, escape it!
            if (containShellInstances(inputEWASes) ||
                containShellInstances(outputEWASes))
                continue;
            // Need to convert ewas to RefPepSeqes
            List<GKInstance> inputRefPeps = grepReferenceEntities(inputEWASes);
            List<GKInstance> outputRefPeps = grepReferenceEntities(outputEWASes);
            if (!inputRefPeps.equals(outputRefPeps)) {
                imbalanceList.add(reaction);
                // Check if this reaction is a cleavage one
                if (checkCleavage(reaction)) 
                    cleavageReactions.add(reaction);
            }
            progressPane.setValue(++count);
        }
        if (progressPane.isCancelled())
            return;
        hideProgressPane();
        // Display 
        displayResults(imbalanceList, cleavageReactions);
    }
    
    /**
     * This method is used to check if a reaction is a cleavage reaction. The code here is ported
     * from Esther's Perl script: QA_Collection.pm.
     * @param reaction
     * @return
     * @throws Exception
     */
    private boolean checkCleavage(GKInstance reaction) throws Exception {
        Map refPepSeqMap = getImbalanceRefPepSeqMap(reaction);
        List inputEWAS = getEWASFromReaction(reaction, ReactomeJavaConstants.input);
        List outputEWAS = getEWASFromReaction(reaction, ReactomeJavaConstants.output);
        Set rxtEWAS = new HashSet();
        rxtEWAS.addAll(inputEWAS);
        rxtEWAS.addAll(outputEWAS);
        // KEY:Value RefPepSeq:List<Integer>
        Map possibleCleavedRefPeps = new HashMap();
        for (Iterator it = rxtEWAS.iterator(); it.hasNext();) {
            GKInstance ewas = (GKInstance) it.next();
            GKInstance refEntity = (GKInstance) ewas.getAttributeValue(ReactomeJavaConstants.referenceEntity);
            if (!refPepSeqMap.containsKey(refEntity))
                continue; // Only need to check imbalance RefPepSeq
            Integer end = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.endCoordinate);
            if (end != null && end.intValue() > 1) {
                Set endList = (Set) possibleCleavedRefPeps.get(refEntity);
                if (endList == null) {
                    endList = new HashSet();
                    possibleCleavedRefPeps.put(refEntity, endList);
                }
                endList.add(end);
                continue;
            }
            Integer start = (Integer) ewas.getAttributeValue(ReactomeJavaConstants.startCoordinate);
            if (start != null && start.intValue() > 1) {
                // Still take the end value though it is null here or maybe 1.
                Set endList = (Set) possibleCleavedRefPeps.get(refEntity);
                if (endList == null) {
                    endList = new HashSet();
                    possibleCleavedRefPeps.put(refEntity, endList);
                }
                endList.add(end);
            }
        }
        if (possibleCleavedRefPeps.size() == 0)
            return false;
        // Check if it is a cleavage now
        for (Iterator it1 = possibleCleavedRefPeps.keySet().iterator(); it1.hasNext();) {
            GKInstance refPepSeq = (GKInstance) it1.next();
            Set endList = (Set) possibleCleavedRefPeps.get(refPepSeq);
            if (endList.size() == 1) // at least should have 1. One means input and output have the same endcoodinate.
                return false;
        }
        return true;
    }
    
    /**
     * Reactions in the cleavageList are still contained by the imbalanceList.
     * @param imbalanceList
     * @param cleavageList
     */
    protected void displayResults(List imbalanceList,
                                  List cleavageList) {
        if (imbalanceList == null || imbalanceList.size() == 0) {
            String msg = null;
            if (dataSource instanceof XMLFileAdaptor) {
                msg = "Checked reactions are input-output balanced.\nNote: Reactions containing shell instances are not checked.";
            }
            else
                msg = "Checked reactions are input-output balanced.";
            JOptionPane.showMessageDialog(parentComp,
                                          msg,
                                          "Balance Checking Result",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Check if it is editable
        GKInstance first = (GKInstance) imbalanceList.get(0);
        boolean isEditable = false;
        if (first.getDbAdaptor() instanceof XMLFileAdaptor)
            isEditable = true;
        // Construct a JFrame for results
        final JFrame frame = new JFrame("Results for checking input-output imbalance for reactions");
        // Divide the total imbalance list into two parts: true imbalance and cleavage reactions.
        List list1 = new ArrayList(imbalanceList);
        list1.removeAll(cleavageList);
        displayPane = new ImbalanceDisplayPane(list1, cleavageList);
        // Add another pane to display the checking result for the selected GKInstance
        final ResultPane resultPane = new ResultPane();
        resultPane.setPreferredSize(new Dimension(650, 150));
        ImbalanceTableModel tableModel = new ImbalanceTableModel();
        resultPane.setTableModel(tableModel);
        frame.getContentPane().add(resultPane, BorderLayout.SOUTH);
        // default should hide resultPane
        resultPane.setVisible(false);
        
        // Add listPane and resultPane together
        final JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                              displayPane,
                                              resultPane);
        jsp.setResizeWeight(0.75);
        frame.getContentPane().add(jsp, BorderLayout.CENTER);
        // Need to add a control pane
        CheckOutControlPane controlPane = createControlPane(frame);
        final JButton checkOutBtn = controlPane.getCheckOutBtn();
        // Need to link all GUIs together
        ListSelectionListener selectionListener = generateListSelectionListener(resultPane, 
                                                                                jsp, 
                                                                                checkOutBtn,
                                                                                "Imbalance Check");
        displayPane.addSelectionListener(selectionListener);
        
        if (displayPane.tabbedPane != null) {
            displayPane.tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    List selection = displayPane.getListPane().getSelection();
                    if (selection.size() > 0)
                        checkOutBtn.setEnabled(true);
                    else
                        checkOutBtn.setEnabled(false);
                }
            });
        };
        frame.getContentPane().add(controlPane, BorderLayout.SOUTH);
        
        if (isEditable) {
            displayPane.attributePane.setEditable(true);
            registerAttributePane(displayPane.attributePane, frame);
        }
        else {
            displayPane.attributePane.setEditable(false);
        }
        showResultFrame(frame);
    }
    
    protected InstanceListPane getDisplayedList() {
        if (displayPane != null)
            return displayPane.getListPane();
        return null;
    }
    
    /**
     * Override this method to load attribtues if dataSource have been switched.
     * Also, make sure all reaction participants are checked out in full (Complex, Set).
     */
    @Override
    protected void checkOutSelectedInstances(JFrame parentFrame,
                                             List selected) {
        // Check out instances from the active MySQLAdaptor
        Window parentDialog = (Window) SwingUtilities.getRoot(parentFrame);
        try {
            GKInstance instance = (GKInstance) selected.iterator().next();
            if (instance.getDbAdaptor() != dataSource) {
                // The database connection has been switched. Load instance attributes if
                // there is a long instances
                if (selected.size() > SIZE_TO_LOAD_ATTS)
                    loadAttributes(selected, instance);
            }
            Set checkOutInstances = new HashSet();
            checkOutInstances.addAll(selected);
            for (Iterator it = selected.iterator(); it.hasNext();) {
                GKInstance reaction = (GKInstance) it.next();
                grepCheckOutInstances(reaction, checkOutInstances);
            }
            // load all values first before checking out
            for (Iterator it = checkOutInstances.iterator(); it.hasNext();) {
                instance = (GKInstance) it.next();
                MySQLAdaptor dba = (MySQLAdaptor) instance.getDbAdaptor();
                dba.fastLoadInstanceAttributeValues(instance);
            }
            // Want to do a full check out: get all participants for reactions
            checkOut(new ArrayList(checkOutInstances), parentDialog);
        }
        catch(Exception e) {
            System.err.println("ImbalanceChecker.checkOutSelectedInstances() 1: " + e);
            e.printStackTrace();
            JOptionPane.showMessageDialog(parentFrame,
                                          "Cannot check out instanes: " + e.getMessage(),
                                          "Error in Checking Out",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    protected void grepCheckOutInstances(GKInstance reaction,
                                            Set checkOutInstances) throws Exception {
        List inputs = reaction.getAttributeValuesList(ReactomeJavaConstants.input);
        List outputs = reaction.getAttributeValuesList(ReactomeJavaConstants.output);
        Set current = new HashSet();
        current.addAll(inputs);
        current.addAll(outputs);
        Set next = new HashSet();
        while (current.size() > 0) {
            for (Iterator it = current.iterator(); it.hasNext();) {
                GKInstance tmp = (GKInstance) it.next();
                checkOutInstances.add(tmp);
                for (int i = 0; i < checkedAttNames.length; i++) {
                    if (tmp.getSchemClass().isValidAttribute(checkedAttNames[i])) {
                        List values = tmp.getAttributeValuesList(checkedAttNames[i]);
                        if (values != null)
                            next.addAll(values);
                    }
                }
            }
            current.clear();
            current.addAll(next);
            next.clear();
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<GKInstance> grepReferenceEntities(List<GKInstance> ewases) throws Exception {
        List<GKInstance> entities = new ArrayList<GKInstance>();
        for (GKInstance instance : ewases) {
            List<GKInstance> values = instance.getAttributeValuesList(ReactomeJavaConstants.referenceEntity);
            if (values != null) {
                for (GKInstance tmp : values) {
                    if (tmp.getSchemClass().isa(ReactomeJavaConstants.ReferencePeptideSequence) ||
                        tmp.getSchemClass().isa(ReactomeJavaConstants.ReferenceGeneProduct)) // New schema as of 2/12/09
                        entities.add(tmp);
                }
            }
            // Polymer that is used as modification for an EWAS should be checked too
            values = instance.getAttributeValuesList(ReactomeJavaConstants.hasModifiedResidue);
            if (values == null || values.size() == 0)
                continue;
            for (GKInstance hasModifiedResidue : values) {
                if (!hasModifiedResidue.getSchemClass().isa(ReactomeJavaConstants.GroupModifiedResidue))
                    continue;
                GKInstance modification = (GKInstance) hasModifiedResidue.getAttributeValue(ReactomeJavaConstants.modification);
                if (modification == null || !modification.getSchemClass().isa(ReactomeJavaConstants.Polymer))
                    continue;
                // We want to check Polymer only for UniProt
                List<GKInstance> polymerRefEntities = grepReferenceEntitiesFromPolymer(modification);
                if (polymerRefEntities != null)
                    entities.addAll(polymerRefEntities);
            }
        }
        // Do a sorting based on DB_IDs only to avoid a length database call to get
        // display names
        Collections.sort(entities, new Comparator<GKInstance>() {
            public int compare(GKInstance instance1, GKInstance instance2) {
                return instance1.getDBID().compareTo(instance2.getDBID());
            }
        });
        return entities;
    }
    
    private List<GKInstance> grepReferenceEntitiesFromPolymer(GKInstance polymer) throws Exception {
        GKInstance repeatedUnit = (GKInstance) polymer.getAttributeValue(ReactomeJavaConstants.repeatedUnit);
        if (repeatedUnit == null)
            return new ArrayList<GKInstance>();
        List<GKInstance> ewases = new ArrayList<GKInstance>();
        getEWASFromReaction(polymer, checkedAttNames, ewases);
        List<GKInstance> refEntities = grepReferenceEntities(ewases);
        Integer min = (Integer) polymer.getAttributeValue(ReactomeJavaConstants.minUnitCount);
        if (min == null)
            return refEntities;
        List<GKInstance> rtn = new ArrayList<GKInstance>();
        for (int i = 0; i < min; i++) {
            rtn.addAll(refEntities); // Have to do a repeat
        }
        return rtn;
    }
    
    public void check(GKInstance instance) {
    	if (!instance.getSchemClass().isa("Reaction")) {
    		showErrorMessage();
    		return;
    	}
    	List<GKInstance> list = new ArrayList<GKInstance>(1);
    	list.add(instance);
    	check(list);
    }
    
    /**
	 * A helper method.
	 */
	protected void showErrorMessage() {
		JOptionPane.showMessageDialog(parentComp,
		                              "Only Reaction instances should be checked for imbalance.",
				                      "Error in Imbalance Check",
									  JOptionPane.ERROR_MESSAGE);
	}

	public void check(List<GKInstance> instances) {   	
    	// Need to sort out Reaction instances since only Reaction instances should be
    	// checked for imbalance
    	final List<GKInstance> reactions = new ArrayList<GKInstance>();
    	GKInstance instance = null;
    	for (Iterator<?> it = instances.iterator(); it.hasNext();) {
    		instance = (GKInstance) it.next();
    		if (instance.getSchemClass().isa(ReactomeJavaConstants.Reaction))
    			reactions.add(instance);
    	}
    	if (reactions.size() == 0) {
    		showErrorMessage();
    		return;
    	}
    	if (reactions.size() < instances.size()) {
    		int reply = JOptionPane.showConfirmDialog(parentComp,
    				                      "Some of selected instances are not Reaction. Only Reaction instances should be" +
    				                      " checked for imbalance.\n Do you want to continue to check those Reaction instances?",
										  "Continue Checking?",
										  JOptionPane.YES_NO_OPTION);
    		if (reply == JOptionPane.NO_OPTION)
    			return;
    	}
        // Use a new thread so that the progress can be monitored
        Thread t = new Thread() {
            public void run() {
                try {
                    initProgressPane("Check Reaction Imbalance");
                    // If reactions size less than 20, use a simple way to load attributes
                    checkReactions(reactions, 
                                   dataSource instanceof MySQLAdaptor && reactions.size() > SIZE_TO_LOAD_ATTS);
                }
                catch(Exception e) {
                    hideProgressPane();
                    System.err.println("ImbalanceChecker.check(): " + e);
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }
	
	private Map<GKInstance, Integer[]> getImbalanceRefPepSeqMap(GKInstance reaction) throws Exception {
        Map<GKInstance, Integer[]> imbalanceMap = new HashMap<GKInstance, Integer[]>();
	    List<GKInstance> inputEWASes= getEWASFromReaction(reaction, ReactomeJavaConstants.input);
        List<GKInstance> outputEWASes = getEWASFromReaction(reaction, ReactomeJavaConstants.output);
        // Need to convert ewas to RefPepSeqes
        List<GKInstance> inputRefPeps = grepReferenceEntities(inputEWASes);
        Map<GKInstance, Integer> inputRefPepSeqMap = convertListToMap(inputRefPeps);
        List<GKInstance> outputRefPeps = grepReferenceEntities(outputEWASes);
        Map<GKInstance, Integer> outputRefPepSeqMap = convertListToMap(outputRefPeps);
        // The above two lists should have been sorted based on DB_IDs already
        // Find the differences between these two lists
        // First check from input side
        for (Iterator<GKInstance> it = inputRefPepSeqMap.keySet().iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Integer count = (Integer) inputRefPepSeqMap.get(instance);
            Integer outputCount = (Integer) outputRefPepSeqMap.get(instance);
            if (outputCount == null)
                outputCount = new Integer(0);
            else {
                // To avoid checking next loop
                outputRefPepSeqMap.remove(instance);
            }
            if (count.equals(outputCount)) 
                continue;
            imbalanceMap.put(instance, new Integer[]{count, outputCount}); 
        }
        // Second check from the output side
        for (Iterator<GKInstance> it = outputRefPepSeqMap.keySet().iterator(); it.hasNext();) {
            GKInstance instance = (GKInstance) it.next();
            Integer outputCount = (Integer) outputRefPepSeqMap.get(instance);
            // There should be no entry in the inputMap. Otherwise, it should
            // be checked already
            imbalanceMap.put(instance, new Integer[]{new Integer(0), outputCount});
        }
        return imbalanceMap;
	}
	
    private Map<GKInstance, Integer> convertListToMap(List<GKInstance> refPepSeqs) {
        Map<GKInstance, Integer> map = new HashMap<GKInstance, Integer>();
        for (GKInstance instance : refPepSeqs) {
            Integer count = (Integer) map.get(instance);
            if (count == null)
                map.put(instance, 1);
            else
                map.put(instance, count + 1);
        }
        return map;
    }
    
    class ImbalanceDisplayPane extends JPanel {
        private JTabbedPane tabbedPane;
        private InstanceListPane imbalanceListPane;
        private InstanceListPane cleavageListPane;
        private AttributePane attributePane;
        
        public ImbalanceDisplayPane(List imbalanceReactions,
                                    List cleavageReactions) {
            init(imbalanceReactions, 
                 cleavageReactions);
        }
        
        private void init(List imbalanceReactions,
                          List cleavageReactions) {
            setLayout(new BorderLayout());
            int totalSize = imbalanceReactions.size() + cleavageReactions.size();
            JLabel titleLabel = GKApplicationUtilities.createTitleLabel("The following reactions are not balanced: " + totalSize + " instances");
            add(titleLabel, BorderLayout.NORTH);
            imbalanceListPane = null;
            cleavageListPane = null;
            if (imbalanceReactions.size() > 0) {
                imbalanceListPane = new InstanceListPane();
                InstanceUtilities.sortInstances(imbalanceReactions);
                imbalanceListPane.setDisplayedInstances(imbalanceReactions);
                imbalanceListPane.setTitle("Imbalance Reaction: " + imbalanceReactions.size());
            }
            if (cleavageReactions.size() > 0) {
                cleavageListPane = new InstanceListPane();
                InstanceUtilities.sortInstances(cleavageReactions);
                cleavageListPane.setDisplayedInstances(cleavageReactions);
                cleavageListPane.setTitle("Cleavage Reaction: " + cleavageReactions.size());
            }
            JComponent leftPane = null;
            if (imbalanceListPane != null && cleavageListPane != null) {
                tabbedPane = new JTabbedPane();
                tabbedPane.addTab("Imbalance", imbalanceListPane);
                tabbedPane.addTab("Cleavage", cleavageListPane);
                leftPane = tabbedPane;
            }
            else if (imbalanceListPane != null)
                leftPane = imbalanceListPane;
            else 
                leftPane = cleavageListPane;
            attributePane = new AttributePane();
            JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                            leftPane,
                                            attributePane);
            jsp.setResizeWeight(0.5);
            jsp.setDividerLocation(320); 
            add(jsp, BorderLayout.CENTER);
            addSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    java.util.List selectedInstances = getListPane().getSelection();
                    if (selectedInstances != null && selectedInstances.size() == 1) {
                        GKInstance instance = (GKInstance) selectedInstances.get(0);
                        attributePane.setInstance(instance);
                    }
                    else
                        attributePane.setInstance(null);
                }
            });
        }
        
        private InstanceListPane getListPane() {
            if (tabbedPane == null) {
                if (imbalanceListPane != null)
                    return imbalanceListPane;
                return cleavageListPane;
            }
            else {
                InstanceListPane listPane = (InstanceListPane) tabbedPane.getSelectedComponent();
                return listPane;
            }
        }
        
        public void addSelectionListener(ListSelectionListener l) {
            if (imbalanceListPane != null)
                imbalanceListPane.addSelectionListener(l);
            if (cleavageListPane != null)
                cleavageListPane.addSelectionListener(l);
        }
        
    }
    
    /**
	 * This table model is used for imbalance checking result.
	 */
	class ImbalanceTableModel extends ResultTableModel {
	    
	    // Actual contents: GKInstance -> String[2] (input, output)
	    private Map<GKInstance, Integer[]> results = null;
	    private List<GKInstance> refPepSeqs = null;
	    
	    public ImbalanceTableModel() {
	        setColNames(new String[] {"", "Input", "Output"});
	    }

	    public void setInstance(GKInstance reaction) {
	        try {
	            results = getImbalanceRefPepSeqMap(reaction);
	            // Don't forget keys
	            refPepSeqs = new ArrayList<GKInstance>(results.keySet());
	            if (dataSource instanceof MySQLAdaptor) {
	                MySQLAdaptor dba = (MySQLAdaptor) dataSource;
	                // TO use new schema with ReferenceGeneProuct class
	                String clsName = null;
	                if (dba.getSchema().isValidClass(ReactomeJavaConstants.ReferenceGeneProduct))
	                    clsName = ReactomeJavaConstants.ReferenceGeneProduct;
	                else
	                    clsName = ReactomeJavaConstants.ReferencePeptideSequence;
	                SchemaClass cls = dba.getSchema().getClassByName(clsName);
	                SchemaAttribute att = cls.getAttribute(ReactomeJavaConstants.identifier);
	                dba.loadInstanceAttributeValues(refPepSeqs, att);
	                if (cls.isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
	                    att = cls.getAttribute(ReactomeJavaConstants.variantIdentifier);
	                    dba.loadInstanceAttributeValues(refPepSeqs, att);
	                }
	            }
	            if (refPepSeqs.size() > 1) {
	                Collections.sort(refPepSeqs, new Comparator<GKInstance>() {
	                    public int compare(GKInstance inst1, GKInstance inst2) {
	                        try {
	                            String id1 = null;
	                            if (inst1.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
	                                id1 = (String) inst1.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
	                            if (id1 == null)
	                                id1 = (String) inst1.getAttributeValue(ReactomeJavaConstants.identifier);
	                            String id2 = null;
	                            if (inst2.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier))
	                                id2 = (String) inst2.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
	                            if (id2 == null)
	                                id2 = (String) inst2.getAttributeValue(ReactomeJavaConstants.identifier);
	                            return id1.compareTo(id2);
	                        }
	                        catch(Exception e) {} // really don't care!
	                        return 0;
	                    }
	                });
	            }
	            fireTableStructureChanged();
	        }
	        catch(Exception e) {
	            System.err.println("ImbalanceChecker.setReaction(): " + e);
	            e.printStackTrace();
	        }
	    }
	    
        public int getRowCount() {
            if (results == null)
                return 0;
            return results.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            GKInstance refPepSeq = (GKInstance) refPepSeqs.get(rowIndex);
            if (columnIndex == 0) {
                try {
                    String value = null;
                    if (refPepSeq.getSchemClass().isValidAttribute(ReactomeJavaConstants.variantIdentifier)) {
                        value = (String) refPepSeq.getAttributeValue(ReactomeJavaConstants.variantIdentifier);
                    }
                    if (value == null && 
                        refPepSeq.getSchemClass().isValidAttribute(ReactomeJavaConstants.identifier))
                        value = (String) refPepSeq.getAttributeValue(ReactomeJavaConstants.identifier);
                    if (value == null) // In case a shell instance for local project
                        value = "DB_ID: " + refPepSeq.getDBID();
                    return value;
                }
                catch(Exception e) {
                    
                }
                return "DB_ID: " + refPepSeq.getDBID();
            }
            Integer[] numbers = (Integer[]) results.get(refPepSeq);
            return numbers[columnIndex - 1].toString();
        }
	}
	
	public static void main(String[] args) {
	    String fileName = "/Users/wgm/Documents/gkteam/gopal/Imbalance_Cleavage.txt";
	    try {
	        FileReader fileReader = new FileReader(fileName);
	        BufferedReader bufferedReader = new BufferedReader(fileReader);
	        List lines = new ArrayList();
	        String line = null;
	        int index = 0;
	        while ((line = bufferedReader.readLine()) != null) {
	            index = line.indexOf("[");
	            lines.add(line.substring(index));
	        }
	        bufferedReader.close();
	        fileReader.close();
	        Collections.sort(lines);
	        // Output
	        fileName = "/Users/wgm/Documents/gkteam/gopal/Imbalance_Cleavage_Sorted.txt";
	        FileWriter fileWriter = new FileWriter(fileName);
	        PrintWriter printWriter = new PrintWriter(fileWriter);
	        for (Iterator it = lines.iterator(); it.hasNext();) {
	            line = (String) it.next();
	            System.out.println("Line: " + line);
	            printWriter.println(line);
	        }
	        printWriter.close();
	        fileWriter.close();
	    }
	    catch(IOException e) {
	        e.printStackTrace();
	    }
	}
	
}
