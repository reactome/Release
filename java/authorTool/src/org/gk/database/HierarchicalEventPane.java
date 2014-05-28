/*
 * Created on Nov 10, 2003
 */
package org.gk.database;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.persistence.PersistenceManager;
import org.gk.persistence.XMLFileAdaptor;
import org.gk.schema.InvalidAttributeException;
import org.gk.util.AuthorToolAppletUtilities;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.TreeUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a hierarchical view of events. This class is modified from Imre's
 * PathwayBrowserPanel in org.gk.pathwayView.
 * @author wugm
 */
public class HierarchicalEventPane extends JPanel {
	protected JTree eventTree;
	// For TreeNode icon setting.
	protected Icon iconIsA;
	protected Icon iconPartOf;
	protected Icon iconIsMember;
	protected Icon iconIsSpecializedForm;
	protected Map node2Icon = new HashMap();
	// A list of species that are used
	private java.util.List species;
	// Cach the list of toplevel events
	private java.util.List projects;
	// for species
	private JPanel speciesPane;
	private JComboBox speciesJCB;
	protected JLabel titleLabel;
	// A flag to control GUIS
	private boolean isForDB = false;
	// For editing
	private boolean isEditable = false;
	// For popup actions
	private HierarchicalEventPaneActions actions;
	// For additional actions
	private java.util.List additionalActions;
	// To control popup type
	private int popupType;
	// This flag is used to control if reactions should be listed in the top level
	// To hide reactions from showing in the top-level can keep the tree clearer.
	// The default is show
	private boolean hideReactionsInTopLevel;
	
	public HierarchicalEventPane() {
		init();
	}
	
	public void setIsForDB(boolean isForDB) {
		this.isForDB = isForDB;
	}
	
	public boolean isForDB() {
		return this.isForDB;
	}
	
	public void setTopLevelEvents(java.util.List events) {
		InstanceUtilities.sortInstances(events);
		this.projects = events;
		buildTree(events);
	}
	
	public void setHideReactionsInTopLevel(boolean isHidden) {
	    this.hideReactionsInTopLevel = isHidden;
	}
	
	public boolean getHideReactionsInTopLevel() {
	    return this.hideReactionsInTopLevel;
	}
	
	/**
	 * Persist the event tree to a XML Document object.
	 * @return
	 */
	public Document convertTreeToXML() {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element docRoot = doc.createElement("events");
			doc.appendChild(docRoot);
			int size = root.getChildCount();
			DefaultMutableTreeNode child = null;
			for (int i = 0; i < size; i++) {
			    child = (DefaultMutableTreeNode) root.getChildAt(i);
			    convertTreeToXML(child, docRoot, doc);
			}
			return doc;
        }
        catch(Exception e) {
            System.err.println("HierarchicalEventPane.convertTreeToXML(): " + e);
            e.printStackTrace();
        }
		return null;
	}
	
	private void convertTreeToXML(DefaultMutableTreeNode treeNode,
	                               Element parentElm,
	                               Document doc) {
	    GKInstance instance = (GKInstance) treeNode.getUserObject();
	    Element elm = null;
	    if (instance.getSchemClass().isa("Pathway")) {
	        elm = doc.createElement("pathway");
	    }
	    else 
	        elm = doc.createElement("reaction");
	    elm.setAttribute("name", instance.getDisplayName());
	    elm.setAttribute("dbID", instance.getDBID().toString());
	    String type = null;
	    if (node2Icon.get(treeNode) == iconIsA)
	        type = "isA";
	    else if (node2Icon.get(treeNode) == iconPartOf)
	        type = "partOf";
	    else if (node2Icon.get(treeNode) == iconIsMember)
	        type = "isMember";
	    else if (node2Icon.get(treeNode) == iconIsSpecializedForm)
	        type = "isSpecialisedForm";
	    if (type != null)
	        elm.setAttribute("type", type);
	    parentElm.appendChild(elm);
	    int size = treeNode.getChildCount();
	    for (int i = 0; i < size; i++) {
	        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getChildAt(i);
	        convertTreeToXML(childNode, elm, doc);
	    }
	}
	
	/**
	 * A helper to empty the tree
	 */
	private void clear() {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();
		model.reload();
	}
	
	/**
	 * This method is called for FileAdaptor usually. The toplevel events will be extracted out
	 * from the list of events. For MySQLAdaptor, it is not needed since they can be queried directly
	 * from the database.
	 */
	public void setAllEvents(java.util.List events) {
		if (events == null || events.size() == 0) {
			clear();
			return;
		}
		// To find the top level events
		try {
		    EventTreeBuildHelper helper = new EventTreeBuildHelper();
		    List<GKInstance> topLevelEvents = helper.getTopLevelEvents(events);
		    if (topLevelEvents.size() == 0) {
		        JOptionPane.showMessageDialog(this,
		                                      "Cannot find the top-level events.",
		                                      "Error in Events",
		                                      JOptionPane.ERROR_MESSAGE);
		    }
		    else {
		        setTopLevelEvents(topLevelEvents);
		    }
		}
        catch (Exception e) {
            System.err.println("HierarchicalEventPane.setAllEvents(): " + e);
            e.printStackTrace();
        }
	}
	
	public PersistenceAdaptor getPersistenceAdaptor() {
		if (projects == null || projects.size() == 0)
			return null;
		GKInstance instance = (GKInstance) projects.get(0);
		return instance.getDbAdaptor();
	}
	
	protected void buildTree(DefaultMutableTreeNode treeNode, GKInstance event) {
	    java.util.List values = null;
		try {
		    String hasInstanceAttName = null;
		    Icon instanceIcon = null;
		    // Adapted to the new Schema -- 7/12/05
		    if (event.getSchemClass().isValidAttribute("hasInstance")) {
		        hasInstanceAttName = "hasInstance";
		        instanceIcon = iconIsA;
		    }
		    else if (event.getSchemClass().isValidAttribute("hasMember")) {
		        hasInstanceAttName = "hasMember";
		        instanceIcon = iconIsMember;
		    }
		    else if (event.getSchemClass().isValidAttribute("hasSpecialisedForm")) {
		        hasInstanceAttName = "hasSpecialisedForm";
		        instanceIcon = iconIsSpecializedForm;
		    }
            String hasComponentAttName = null;
            if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                hasComponentAttName = ReactomeJavaConstants.hasEvent;
            else if (event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasComponent))
                hasComponentAttName = ReactomeJavaConstants.hasComponent;
            if (hasComponentAttName != null) {
		        values = event.getAttributeValuesListNoCheck(hasComponentAttName);
		        if (values != null && values.size() > 0) {
		            for (Iterator it = values.iterator(); it.hasNext();) {
		                GKInstance e = (GKInstance)it.next();
		                if (e == null)
		                    continue;
		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
		                treeNode.add(subNode);
		                node2Icon.put(subNode, iconPartOf);
		                buildTree(subNode, e);
		            }
		        }
		    }
		    // Check hasInstance relationships
		    if (hasInstanceAttName != null) {
		        values = event.getAttributeValuesList(hasInstanceAttName);
		        if (values != null && values.size() > 0) {
		            for (Iterator it = values.iterator(); it.hasNext();) {
		                GKInstance e = (GKInstance)it.next();
		                if (e == null)
		                    continue;
		                DefaultMutableTreeNode subNode = new DefaultMutableTreeNode(e);
		                treeNode.add(subNode);
		                node2Icon.put(subNode, instanceIcon);
		                buildTree(subNode, e);
		            }
		        }
		    }
		}
		catch (Exception e) {
			System.err.println("HierarchicalEventPane.buildTree(): " + e);
			e.printStackTrace();
		}
	}
	
	public Map getNode2IconMap() {
	    return this.node2Icon;
	}
	
	public void addTopLevelEvent(GKInstance event) {
		if (projects == null)
			projects = new ArrayList();
		projects.add(event);
		DefaultTreeModel model = (DefaultTreeModel)eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		DefaultMutableTreeNode eventNode = new DefaultMutableTreeNode(event);
		buildTree(eventNode, event);
		// Have to call this way instead of model.insert()
		root.add(eventNode);
		// A bug in JTree
		if (root.getChildCount() == 1)
			model.nodeStructureChanged(root);
		else
			model.nodesWereInserted(root, new int[] { root.getChildCount() - 1 });
	}
	
	public void setSpeciesControlVisible(boolean isVisible) {
		speciesPane.setVisible(isVisible);
	}
	
	public String getSelectedSpecies() {
		return (String) speciesJCB.getSelectedItem();
	}
	
	public void setSelectedSpecies(String species) {
		speciesJCB.setSelectedItem(species);
	}
	
	public HierarchicalEventPaneActions getActions() {
		return this.actions;
	}
	
	public void hideTitle() {
	    titleLabel.setVisible(false);
	}
	
	private void init() {
	    actions = new HierarchicalEventPaneActions(this);
	    // Need to load the species
	    species = new ArrayList();
	    species.add("All");
	    try {
	        InputStream input = AuthorToolAppletUtilities.getResourceAsStream("Species.txt");
	        InputStreamReader reader = new InputStreamReader(input);
	        BufferedReader bfr = new BufferedReader(reader);
	        String line = null;
	        while ((line = bfr.readLine()) != null) {
	            species.add(line);
	        }
	        bfr.close();
	        reader.close();
	        input.close();
	    }
	    catch (IOException e) { // Might be other exception is used in Applet.
	        System.err.println("HierarchicalEventPane.init(): " + e);
	        e.printStackTrace();
	    }
	    setLayout(new BorderLayout());
	    // Title Pane
	    JPanel northPane = new JPanel();
	    northPane.setLayout(new BorderLayout());
	    titleLabel = new JLabel("Tree View");
	    titleLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 2));
	    titleLabel.setHorizontalAlignment(JLabel.LEFT);
	    northPane.add(titleLabel, BorderLayout.WEST);
	    speciesPane = new JPanel();
	    speciesPane.setLayout(new BoxLayout(speciesPane, BoxLayout.X_AXIS));
	    JLabel label = new JLabel("Show in ");
	    speciesPane.add(label);
	    speciesJCB = new JComboBox();
	    for (Iterator it = species.iterator(); it.hasNext();)
	        speciesJCB.addItem(it.next());
	    speciesJCB.addItemListener(new ItemListener() {
	        public void itemStateChanged(ItemEvent e) {
	            if (e.getStateChange() == ItemEvent.SELECTED) {
	                String species = (String) speciesJCB.getSelectedItem();
	                showProjectsIn(species);
	            }
	        }
	    });
	    speciesPane.add(speciesJCB);
	    northPane.add(speciesPane, BorderLayout.EAST);
	    add(northPane, BorderLayout.NORTH);
	    
	    // Add a tree for display schema
	    eventTree = new JTree();
	    eventTree.setShowsRootHandles(true);
	    eventTree.setRootVisible(false);
	    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	    DefaultTreeModel model = new DefaultTreeModel(root);
	    eventTree.setModel(model);
	    EventCellRenderer renderer = createTreeCellRenderer();
	    renderer.setNode2IconMap(node2Icon);
	    eventTree.setCellRenderer(renderer);
	    //eventTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	    add(new JScrollPane(eventTree), BorderLayout.CENTER);
	    // Create actions
	    eventTree.addMouseListener(new MouseAdapter() {
	        public void mousePressed(MouseEvent e) {
	            if (e.isPopupTrigger()) {
	                doTreePopup(e);
	                return;
	            }
	        }
	        public void mouseReleased(MouseEvent e) {
	            if (e.isPopupTrigger()) {
	                doTreePopup(e);
	                return;
	            }
	        }
	    });
	    iconIsA = GKApplicationUtilities.getIsAIcon();
	    iconPartOf = GKApplicationUtilities.getIsPartOfIcon();	
	    iconIsMember = GKApplicationUtilities.getIsMemberIcon();
	    iconIsSpecializedForm = GKApplicationUtilities.getIsSpecializedFormIcon();
	    actions.setWrappedTree(eventTree);
	}
	
	protected EventCellRenderer createTreeCellRenderer() {
		return new EventCellRenderer(true);
	}
	
	public void addAdditionalPopupAction(Action action) {
		if (additionalActions == null)
			additionalActions = new ArrayList();
		additionalActions.add(action);
	}
	
	protected void doTreePopup(MouseEvent e) {
		JPopupMenu popup = EventPanePopupManager.getManager().getPopupMenu(popupType, this);
		// Check for additional actions
		if (additionalActions != null && additionalActions.size() > 0) {
			if (popup != null)
				popup.addSeparator();
			else
				popup = new JPopupMenu();
			for (Iterator it = additionalActions.iterator(); it.hasNext();) {
				Action action = (Action) it.next();
				popup.add(action);
			}
		}
		// Want to add an option to show reactions in the top-level
		if (popup == null)
		    popup = new JPopupMenu();
		else
		    popup.addSeparator();
		String text = null;
		if (hideReactionsInTopLevel)
		    text = "Show Reactions at Top-level";
		else
		    text = "Hide Reactions at Top-level";
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideReactionsInTopLevel = !hideReactionsInTopLevel;
                // Use the follow to re-build the tree
                String species = (String) speciesJCB.getSelectedItem();
                showProjectsIn(species);
                // Fire this as a Java Bean property for others to catch
                firePropertyChange("hideReactionAtTop", 
                                   !hideReactionsInTopLevel,
                                   hideReactionsInTopLevel);
            }
        });
		popup.add(menuItem);
		if (popup != null)
			popup.show(eventTree, e.getX(), e.getY());
	}
	
	private void showProjectsIn(String species) {
		if (projects == null || projects.size() == 0)
			return;
		try {
			if (!isForDB) { 
				XMLFileAdaptor adaptor = PersistenceManager.getManager().getActiveFileAdaptor();
			}
			if (species.equals("All")) {
				buildTree(projects);
				return;
			}
			GKInstance instance = null;
			GKInstance speciesInstance = null;
			java.util.List newList = new ArrayList();
			for (Iterator it = projects.iterator(); it.hasNext();) {
				instance = (GKInstance) it.next();
				if (instance.getSchemClass().isValidAttribute("taxon"))
				    speciesInstance = (GKInstance)instance.getAttributeValue("taxon");
				else if (instance.getSchemClass().isValidAttribute("species"))
				    speciesInstance = (GKInstance) instance.getAttributeValue("species");
				if (speciesInstance != null && speciesInstance.getDisplayName().equalsIgnoreCase(species)) {
					newList.add(instance);
				}
			}
			buildTree(newList);
		}
		catch (InvalidAttributeException e) {
			System.err.println("HierarchicalEventPane.showProjectsIn(): " + e);
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println("HierarcicalEventPane.showProjectsIn() 1: " + e);
			e.printStackTrace();
		}
	}
	
	private void buildTree(Collection events) {
	    if (hideReactionsInTopLevel) {
	        // Need to quick filtering
	        // Make a copy
	        events = new ArrayList(events);
	        for (Iterator<?> it = events.iterator(); it.hasNext();) {
	            GKInstance inst = (GKInstance) it.next();
	            if (inst.getSchemClass().isa(ReactomeJavaConstants.ReactionlikeEvent))
	                it.remove();
	        }
	    }
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		root.removeAllChildren();
		String selectedSpecies = (String) speciesJCB.getSelectedItem();
		int index = 0;
		for (Iterator i = events.iterator(); i.hasNext();) {
			GKInstance event = (GKInstance) i.next();
			// Check species
			if (!selectedSpecies.equals("All")) {
				try {
				    GKInstance species = null;
				    // Taxon is used in new schema -- Guanming Wu, 7/12/05
				    if (event.getSchemClass().isValidAttribute("taxon"))
				        species = (GKInstance)event.getAttributeValue("taxon");
				    else if (event.getSchemClass().isValidAttribute("species"))
				        species = (GKInstance) event.getAttributeValue("species");
					if (species == null)
						continue;
					else if (!species.getDisplayName().equalsIgnoreCase(selectedSpecies)) 
						continue;
				}
				catch (Exception e) {
				    System.err.println("HierarchicalEventPane.buildTree(): " + e);
				    e.printStackTrace();
				}
			}
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(event);
			root.add(node);
			boolean isCirculated = checkCirculation(event);
			if (isCirculated) {
			    System.err.println(event + " has circulation reference!");
			    continue;
			}
			buildTree(node, event);
			firePropertyChange("buildTree", index - 1, index);
			index ++;
		}
		model.nodeStructureChanged(root);
	}
		
	/**
	 * Check if a child of an event is an ancestor of this event.
	 * @param event
	 * @return
	 * @throws Exception
	 */
	private boolean checkCirculation(GKInstance event) {
	    try {
	        if (!event.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
	            return false;
	        Set<GKInstance> current = new HashSet<GKInstance>();
	        current.add(event);
	        Set<GKInstance> next = new HashSet<GKInstance>();
	        Set<GKInstance> checked = new HashSet<GKInstance>();
	        while (current.size() > 0) {
	            for (GKInstance inst : current) {
	                checked.add(inst);
	                if (!(inst.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent)))
	                    continue;
	                List list = inst.getAttributeValuesList(ReactomeJavaConstants.hasEvent);
	                if (list == null || list.size() == 0)
	                    continue;
	                if (list.contains(event)) {
	                    System.err.println("Circular reference: " + event);
	                    return true;
	                }
	                for (Object obj : list) {
	                    GKInstance nextEvent = (GKInstance) obj;
	                    if (checked.contains(nextEvent)) {
	                        continue; // Avoid to check again
	                    }
	                    next.add(nextEvent);
	                }
	            }
	            current.clear();
	            current.addAll(next);
	            next.clear();
	        }
	        checked.remove(event);
	        for (GKInstance nextEvent : checked) {
	            if (checkCirculation(nextEvent))
	                return true;
	        }
	    }
	    catch(Exception e) {
	        System.err.println("HierarchicalEventPane.checkCirculation(): " + e);
	        e.printStackTrace();
	    }
	    return false;
	}
	
	public java.util.List getTopLevelEvents() {
		return projects;
	}
	
	public GKInstance getSelectedEvent() {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) eventTree.getLastSelectedPathComponent();
		if (node != null)
			return (GKInstance) node.getUserObject();
		return null;
	}
	
	public void setSelectedEvent(GKInstance event) {
		DefaultMutableTreeNode treeNode = TreeUtilities.searchNode(event, eventTree);
		if (treeNode != null) {
			DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
			TreePath path = new TreePath(model.getPathToRoot(treeNode));
			eventTree.expandPath(path);
			eventTree.setSelectionPath(path);
		}
	}
	
	/**
	 * Set selection mode for event tree.
	 */
	public void setSelectionMode(int mode) {
	    eventTree.getSelectionModel().setSelectionMode(mode);
	}
	
	public void clearSelection() {
		eventTree.clearSelection();
	}
	
	public void collapseAllNodes() {
		DefaultMutableTreeNode eventNode = null;
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) eventTree.getModel().getRoot();
		int size = root.getChildCount();
		for (int i = 0; i < size; i++) {
			eventNode = (DefaultMutableTreeNode) root.getChildAt(i);
			TreeUtilities.collapseAllNodes(eventNode, eventTree);
		}
	}
	
	/**
	 * Select a collection of event Instance objects.
	 * @param c
	 */
	public void select(Collection c) {
		doSelection(c, true);
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void doSelection(Collection c, boolean needFeedback) {
	    java.util.List nodes = new ArrayList();
	    for (Iterator it = c.iterator(); it.hasNext();) {
	        Object obj = it.next();
	        java.util.List treeNodes = TreeUtilities.searchNodes(obj, eventTree);
	        nodes.addAll(treeNodes);
	    }
	    if (nodes.size() == 0) {
	        if (needFeedback) {
	            JOptionPane.showMessageDialog(this,
	                                          "No events found.",
	                                          "Search Result",
	                                          JOptionPane.INFORMATION_MESSAGE);
	        }
	        return;
	    }
	    eventTree.clearSelection();
	    DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
	    DefaultMutableTreeNode treeNode = null;
	    // The first path should be visible
	    TreePath firstPath = null;
	    for (Iterator it = nodes.iterator(); it.hasNext();) {
	        treeNode = (DefaultMutableTreeNode) it.next();
	        TreePath path = new TreePath(model.getPathToRoot(treeNode));
	        if (firstPath == null) 
	            firstPath = path;
	        eventTree.addSelectionPath(path);
	    }
	    if (firstPath != null)
	        eventTree.scrollPathToVisible(firstPath);
	}
	
	/**
	 * Set the selected instances without popuping a dialog if nothing is found.
	 * @param instances
	 */
	public void setSelectedInstances(List<GKInstance> instances) {
	    // Clear selection first
	    eventTree.clearSelection();
	    doSelection(instances, false);
	}
	
	public void addSelectionListener(TreeSelectionListener listener) {
		eventTree.addTreeSelectionListener(listener);
	}
	
	public void removeSelectionListener(TreeSelectionListener listener) {
		eventTree.removeTreeSelectionListener(listener);
	}
	
	public TreeSelectionListener[] getSelectionListeners() {
		return eventTree.getTreeSelectionListeners();
	}
	
	public JTree getTree() {
		return this.eventTree;
	}
	
	public void setEditable(boolean editable) {
		if (this.isEditable == editable)
		    return;
	    this.isEditable = editable;
	    MouseListener dnrChecker = actions.getDNRCheckMouseAction();
	    // Only one dnrChecker is ensured because the above equal checking.
	    if (isEditable)
	        eventTree.addMouseListener(dnrChecker);
	    else
	        eventTree.removeMouseListener(dnrChecker);
	}
	
	public boolean isEditable() {
		return this.isEditable;
	}
	
	public void setPopupType(int popupType) {
		this.popupType = popupType;
	}
	
	public java.util.List getSelection() {
		java.util.List selection = new ArrayList();
		TreePath[] paths = eventTree.getSelectionPaths();
		if (paths != null) {
			for (int i = 0; i < paths.length; i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
				selection.add(treeNode.getUserObject());
			}
		}
		return selection;
	}
	
	/**
	 * Add an event Instance to the view. The integrity of data structure is not 
	 * taken care of here. 
	 * Need to check the existed events. If it is a project and should be removed from the 
	 * project list if it is the specified event's components.
	 * @param event
	 */
	public void addInstance(Instance event) {
	    if (!event.getSchemClass().isa(ReactomeJavaConstants.Event))
	        return;
		GKInstance instance = (GKInstance) event;
		if (instance.isShell())
			return; // Don't add a shell instance
		// Just added at the top-level
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(event);
		model.insertNodeInto(newNode, root, root.getChildCount());
		if (!projects.contains(event))
		    projects.add(event);
		updateInstance(instance, null);
	}
	
	/**
	 * Delete an event from the tree view. This method is just to remove the
	 * display of the event from the tree and takes care of the display of its
	 * children. The integrity of data structure is not taken care of.
	 * @param event an event Instance that should be removed from the view.
	 */
	public void deleteInstance(Instance event) {
	    if (!event.getSchemClass().isa(ReactomeJavaConstants.Event))
	        return;
		java.util.List nodes = TreeUtilities.searchNodes(event, eventTree);
		if (nodes == null || nodes.size() == 0)
			return;
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode treeNode = null;
		for (Iterator it = nodes.iterator(); it.hasNext();) {
			treeNode = (DefaultMutableTreeNode) it.next();
			model.removeNodeFromParent(treeNode);
		}
		// There should be only one version of children: Work on the last treeNode.
		if (treeNode.getChildCount() > 0) {
			// Re-link the children of the node to the root directly.
			DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
			int size = treeNode.getChildCount();
			for (int i = 0; i < size; i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeNode.getChildAt(0);
				Object userObject = node.getUserObject();
				// In case one Instance appears in several different places
				if (TreeUtilities.searchNode(userObject, eventTree) == null) {
					// model will remove node from treeNode children list.
					model.insertNodeInto(node, root, root.getChildCount());
					// No icons for node
					node2Icon.remove(node);
				}
			}
		}
		// Have to remove it from the projects list if any
		projects.remove(event);
	}
	
	/**
	 * Update the tree view for the specified event because the attribute values
	 * for slots "hasInstance", "hasMember" or "hasSpecialisedForm" or "hasComponent".
	 * @param event
	 */
	public void updateInstance(AttributeEditEvent editingEvent) {
        GKInstance instance = editingEvent.getEditingInstance();
        if (!instance.getSchemClass().isa(ReactomeJavaConstants.Event))
            return;
        String attName = editingEvent.getAttributeName();
	    if (editingEvent.getEditingType() == AttributeEditEvent.REORDER) {
	        reorder(instance, attName);
	    }
	    else {
	        updateInstance(instance, attName);
	    }
	}
	
	/**
	 * A helper to reorder the child treenodes for a specified nodes.
	 * @param instance
	 * @param attName
	 */
	private void reorder(GKInstance instance, String attName) {
	    if (!attName.equals(ReactomeJavaConstants.hasEvent) &&
            !attName.equals("hasComponent") && 
            !attName.equals("hasInstance") &&
	        !attName.equals("hasMember") && 
            !attName.equals("hasSpecialisedForm"))
	        return;
	    java.util.List newValues = null;
	    try {
	        newValues = instance.getAttributeValuesList(attName);
	    }
	    catch(Exception e) {
	        System.err.println("HierarchicalEventPane.reorder(): " + e);
	        e.printStackTrace();
	    }
	    if (newValues == null || newValues.size() == 0)
	        return;
	    java.util.List treeNodes = TreeUtilities.searchNodes(instance, eventTree);
	    if (treeNodes == null || treeNodes.size() == 0)
	        return;
	    // Get the used icon for the specialized attName
	    Icon icon = null;
	    if (attName.equals(ReactomeJavaConstants.hasEvent) ||
            attName.equals(ReactomeJavaConstants.hasComponent))
	        icon = iconPartOf;
	    else if (attName.equals("hasInstance"))
	        icon = iconIsA;
	    else if (attName.equals("hasMember"))
	        icon = iconIsMember;
	    else if (attName.equals("hasSpecialisedForm"))
	        icon = iconIsSpecializedForm;
	    // Icon is impossible to be null.
	    DefaultMutableTreeNode node = null;
	    DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
	    Map removeMap = new HashMap();
	    for (Iterator it = treeNodes.iterator(); it.hasNext();) {
	        node = (DefaultMutableTreeNode) it.next();
	        removeMap.clear();
	        for (int i = 0; i < node.getChildCount(); i++) {
	            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
	            if (node2Icon.get(childNode) == icon) {
	                removeMap.put(childNode.getUserObject(), childNode);
	            }
	        }
	        TreePath treePath = new TreePath(model.getPathToRoot(node));
	        boolean isExpanded = eventTree.isExpanded(treePath);
	        // Do removing
	        for (Iterator it1 = removeMap.values().iterator(); it1.hasNext();) {
	            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it1.next();
	            model.removeNodeFromParent(childNode);
	        }
	        // Do adding
	        int size = newValues.size();
	        for (int i = size - 1; i >= 0; i--) {
	            GKInstance childInstance = (GKInstance) newValues.get(i);
	            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode)removeMap.get(childInstance);
	            model.insertNodeInto(childNode, node, 0); // Add to the first index to keep the order
	        }
	        if (isExpanded)
	            eventTree.expandPath(treePath);
	    }
	}
	
	private void updateInstance(GKInstance instance, 
	                            String attName, 
	                            java.util.List treeNodes,
	                            Icon typeIcon) {
		java.util.List newValues = null;
		try {
			newValues = instance.getAttributeValuesList(attName);
		}
		catch(Exception e) {
			System.err.println("HierarchicalEventPane.updateInstance(): " + e);
			e.printStackTrace();
		}
		// Have to make a copy of the values to avoid to modify the instance values.
		if (newValues == null)
			newValues = new ArrayList(); // To make comparision simple
		else
			newValues = new ArrayList(newValues);
		// All nodes should have the same strucuture
		DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeNodes.get(0);
		java.util.List removedValues = new ArrayList();
		for (int i = 0; i < treeNode.getChildCount(); i++) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeNode.getChildAt(i);
			// Check if GKInstances in oldValues are still in the newValues. If true, leave it. 
			// Otherwise, delete it.
			if (node2Icon.get(tmp) == typeIcon) {
				int index = newValues.indexOf(tmp.getUserObject());
				if (index < 0) {
					removedValues.add(tmp.getUserObject());
				}
				else {
					newValues.remove(index);
				}
			} 
		}
		DefaultTreeModel treeModel = (DefaultTreeModel) eventTree.getModel();
		if (removedValues.size() > 0) {
			for (Iterator it = treeNodes.iterator(); it.hasNext();) {
				DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) it.next();
				java.util.List removingNodes = new ArrayList();
				for (int i = 0; i < tmp.getChildCount(); i++) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) tmp.getChildAt(i);
					if (removedValues.contains(childNode.getUserObject()))
						removingNodes.add(childNode);
				}
				for (Iterator it1 = removingNodes.iterator(); it1.hasNext();) {
					DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) it1.next();
					treeModel.removeNodeFromParent(childNode);
				}
			}
     		// Have to make sure Instances in removedValues still in the tree
			for (Iterator it = removedValues.iterator(); it.hasNext();) {
				GKInstance event = (GKInstance) it.next();
				if (TreeUtilities.searchNode(event, eventTree) == null)
					addProject(event);
			}
		}
		for (Iterator it = treeNodes.iterator(); it.hasNext();) {
			treeNode = (DefaultMutableTreeNode) it.next();
			// Add left GKInstance objects in newValues list.
			for (Iterator it1 = newValues.iterator(); it1.hasNext();) {
				GKInstance cmp = (GKInstance) it1.next();
				DefaultMutableTreeNode newTreeNode = new DefaultMutableTreeNode(cmp);
				node2Icon.put(newTreeNode, typeIcon);
				buildTree(newTreeNode, cmp);
				treeModel.insertNodeInto(newTreeNode, treeNode, treeNode.getChildCount());
			}
            // In case a dirty label is added
            treeModel.nodeChanged(treeNode);
		}
	}
    
    public void markAsDirty(GKInstance instance) {
        List nodes = TreeUtilities.searchNodes(instance, eventTree);
        if (nodes == null || nodes.size() == 0)
            return;
        DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) it.next();
            model.nodeChanged(treeNode);
        }
    }
	
	private void updateInstance(GKInstance instance, String attName) {
		java.util.List nodes = TreeUtilities.searchNodes(instance, eventTree);
		if (nodes == null || nodes.size() == 0) {
			validateRoot(instance); // But have to validate root.
			return; // No need to update. Instance object might not be displayed.
		}
		java.util.List list = null;
		try {
			if (attName == null) { // Means the whole instance has been changed.
                if (instance.getSchemClass().isValidAttribute(ReactomeJavaConstants.hasEvent))
                    updateInstance(instance, ReactomeJavaConstants.hasEvent,
                                   nodes, iconPartOf);
				if (instance.getSchemClass().isValidAttribute("hasComponent"))
					updateInstance(instance, "hasComponent", nodes, iconPartOf);
				if (instance.getSchemClass().isValidAttribute("hasInstance"))
					updateInstance(instance, "hasInstance", nodes, iconIsA);
				if (instance.getSchemClass().isValidAttribute("hasMember"))
				    updateInstance(instance, "hasMember", nodes, iconIsMember);
				if (instance.getSchemClass().isValidAttribute("hasSpecialisedFormm"))
				    updateInstance(instance, "hasSpecialisedForm", nodes, iconIsSpecializedForm);
                validateRoot(instance);
			}
			else if (attName.equals(ReactomeJavaConstants.hasEvent) ||
                     attName.equals(ReactomeJavaConstants.hasComponent)) {
				updateInstance(instance, attName, nodes, iconPartOf);
				validateRoot(instance);
			}
			else if (attName.equals("hasInstance")) {
			    updateInstance(instance, attName, nodes, iconIsA);
				validateRoot(instance);
			}
			else if (attName.equals("hasMember")) {
			    updateInstance(instance, attName, nodes, iconIsMember);
				validateRoot(instance);
			}
			else if (attName.equals("hasSpecialisedForm")) {
				updateInstance(instance, attName, nodes, iconIsSpecializedForm);
				validateRoot(instance);
			}
			else if (attName.equals(ReactomeJavaConstants._doRelease) ||
                     attName.equals("_displayName") ||
                     attName.equals(ReactomeJavaConstants._doNotRelease)) {
			    // Try to update the node if DNR is displayed
			    DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
			    DefaultMutableTreeNode treeNode = null;
			    for (Iterator it = nodes.iterator(); it.hasNext();) {
			        treeNode = (DefaultMutableTreeNode) it.next();
			        model.nodeChanged(treeNode);
			    }
			}
		}
		catch(Exception e) {
			System.err.println("HierarchicalEventPane.updateInstance(): " + e);
			e.printStackTrace();
		}		
	}
	
	private void validateRoot(GKInstance instance) {
		// Make sure no children is inthe projects
		java.util.List children = new ArrayList();
		try {
		    String[] attNames = new String[]{"hasEvent",
                                             "hasComponent", 
		                                     "hasInstance", 
		                                     "hasMember", 
		    			                     "hasSpecialisedForm"};
		    for (int i = 0; i < attNames.length; i++) {
		        if (instance.getSchemClass().isValidAttribute(attNames[i])) {
		            java.util.List values = instance.getAttributeValuesList(attNames[i]);
		            if (values != null)
		                children.addAll(values);
		        }
		    }
		}
		catch(Exception e) {
			System.err.println("HierachicalEventPane.validateRoot(): " + e);
			e.printStackTrace();
		}
		if (projects != null && // projects list may not be initialized.
            projects.removeAll(children)) {
			// Validate top-level treenodes. They might be different from events because of the organism.
			DefaultTreeModel model = (DefaultTreeModel)eventTree.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
			int size = root.getChildCount();
			java.util.List removedNodes = new ArrayList();
			for (int i = 0; i < size; i++) {
				DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)root.getChildAt(i);
				GKInstance rootInstance = (GKInstance)treeNode.getUserObject();
				if (children.contains(rootInstance)) {
					removedNodes.add(treeNode);
				}
			}
			if (removedNodes.size() > 0) {
				for (Iterator it = removedNodes.iterator(); it.hasNext();) {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)it.next();
					model.removeNodeFromParent(treeNode);
				}
			}
		}
	}
	
	/**
	 * Add a top level GKInstance object.
	 * @param instance
	 */
	private void addProject(GKInstance event) {
		// it should be added to projects
		projects.add(event);
		// Check the species
		String species = "All";
		if (speciesJCB.isVisible())
			species = (String)speciesJCB.getSelectedItem();
		GKInstance speciesInstance = null;
		try {
		    // Try to make it compatible with new schema. "Species" is used
		    if (event.getSchemClass().isValidAttribute("taxon"))
		        speciesInstance = (GKInstance) event.getAttributeValue("taxon");
		    else if (event.getSchemClass().isValidAttribute("species"))
		        speciesInstance = (GKInstance) event.getAttributeValue("species");
		}
		catch (Exception e) {
			System.err.println("HierarchicalEventPane.addInstance(): " + e);
			e.printStackTrace();
		}
		boolean needAdd = false;
		if (species.equals("All"))
			needAdd = true;
		else if (speciesInstance != null && speciesInstance.getDisplayName().equalsIgnoreCase(species))
			needAdd = true;
		if (needAdd) {
			DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(event);
			buildTree(treeNode, event);
			model.insertNodeInto(treeNode, root, root.getChildCount());
			//eventTree.setRootVisible(false); 
			if (root.getChildCount() == 1)
				model.nodeStructureChanged(root);
		}
	}
	
	/**
	 * Call this method to remove a GKInstance object from the root children. This
	 * happens when a new Instance object is created and then attached to other 
	 * Instance objects.
	 * @param instance
	 */
	protected void removeFromRoot(GKInstance instance) {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		int size = root.getChildCount();
		for (int i = 0; i < size; i++) {
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) root.getChildAt(i);
			if (treeNode.getUserObject() == instance) {
				model.removeNodeFromParent(treeNode);
				return;
			}
		}
	}
	
	/**
	 * Expand all tree nodes from the top levels down to any leaves.
	 *
	 */
	public void expandAllTreeNodes() {
		DefaultTreeModel model = (DefaultTreeModel) eventTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
		TreeUtilities.expandAllNodes(root, eventTree);	    
	}
}
