/*
 * Created on Apr 15, 2005
 */
package org.gk.database;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.gk.model.GKInstance;
import org.gk.schema.GKSchemaClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This singleton class is used to control the setting in attribute editing so that
 * no static information be kept in classes related attribute edit. This client to 
 * this class should call AttributeEditConfig.getConfig() to get this sole instance. 
 * @author wgm
 */
public class AttributeEditConfig {
	// Allows this class to fire events
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    // constants
	public static final String DISABLE_AUTO_SETTING = "disableAutoSetting";
	//public static final String ALLOWED = "allowed";
	// The singleton
	private static AttributeEditConfig config;
	// The uneditable attributes list
	private List uneditableAttributes;
	// attributes should not be displayed
	private List hiddenAttributes;
	// Attributes whose settings can be propagated to descedants in the 
	// hierarchical tree
	private Map disabledAutoClsAttNamesMap;
	// This is a system wide auto propagation setting. All users should have the same
	// values of this.
	private Map systemAutoSetClsAttNamesMap;
	// Allows the user to edit attributes using a combo box.
	private boolean allowComboBoxEditor = false;
    // A map for AttributeAutoFiller
    private Map attributeAutoFillerMap;
    // Control attributes grouping
    private boolean isGroupAttByCategories;
    // For modification residue
    private Map<String, String> modificationResidues;
    private Map<String, String> modifications;
    // For PSI-MOD based modifications. New version from ModifiedResidue
    private Map<String, String> psiModificationResidues;
    private Map<String, String> psiModifications;
    // For multiple copy creation in ELV
    private Map<String, String> multipleCopyEntities;
    // A list of allowable AAs in single letters
    private String allowableAminoAcids;
    // For pathway diagram deployment
    private String pdURL;
    private String devWebELV;
	
	/**
	 * To avoid instantiatiation be other classes, keep this constructor as private.
	 */
	private AttributeEditConfig() {
		init();
	}
	
	/**
	 * A helper to initialize some default behaviors
	 */
	private void init() {
		uneditableAttributes = new ArrayList(); // All attributes are editable
		hiddenAttributes = new ArrayList();
		disabledAutoClsAttNamesMap = new HashMap();
        attributeAutoFillerMap = new HashMap();
		propertyChangeSupport.addPropertyChangeListener(FrameManager.getManager());
	}
	
	/**
	 * Get the sole AttributeEditConfig instance in the application.
	 * @return the singleton.
	 */
	public static AttributeEditConfig getConfig() {
		if (config == null)
			config = new AttributeEditConfig();
		return config;
	}
	
    public void setGroupAttributesByCategories(boolean isGrouped) {
        if (this.isGroupAttByCategories == isGrouped)
            return;
        this.isGroupAttByCategories = isGrouped;
        propertyChangeSupport.firePropertyChange("GroupAttributesByCategories", 
                                                 !isGrouped, 
                                                 isGrouped);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    public boolean isGroupAttributesByCategories() {
        return this.isGroupAttByCategories;
    }
    
    /**
	 * Set the list of uneditable attributes.
	 * @param names names of attributes that should not be editable.
	 */
	public void setUnedtiableAttNames(List names) {
		uneditableAttributes.clear();
		if (names != null)
			uneditableAttributes.addAll(names);
	}
	
	public List getUneditableAttNames() {
		return uneditableAttributes;
	}
	
	public List getHiddenAttNames() {
		return hiddenAttributes;
	}
	
	public String getPDUrl() {
	    return this.pdURL;
	}
	
	public String getDevWebELV() {
	    return this.devWebELV;
	}
	
	/**
	 * Set the system wide auto propagation attribute setting.
	 * @param map keys: names of classes; values: list of attribute names.
	 */
	public void setSysetmAutoSetClsAttNames(Map map) {
	    if (systemAutoSetClsAttNamesMap == null)
	        systemAutoSetClsAttNamesMap = new HashMap();
	    if (map != null)
	        systemAutoSetClsAttNamesMap.putAll(map);
	}
    
    /**
     * Get the configuration from an XML Document
     * @param document XML Document containing configuration information for attributes
     * editing.
     */
    public void loadConfig(Document document) {
        Element root = document.getDocumentElement();
        NodeList childNodes = root.getChildNodes();
        int size = childNodes.getLength();
        java.util.List attNameList = new ArrayList();
        for (int i = 0; i < size; i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("uneditableAttributes")) {
                NodeList list = node.getChildNodes();
                for (int j = 0; j < list.getLength(); j++) {
                    Node node1 = list.item(j);
                    if (node1.getNodeName().equals("attribute")) {
                        Element elm = (Element) node1;
                        String name = elm.getAttribute("name");
                        if (name.length() > 0)
                            attNameList.add(name);
                    }
                }
                setUnedtiableAttNames(attNameList);
            }
            else if (node.getNodeName().equals("hiddenAttributes")) {
            	NodeList list = node.getChildNodes();
            	for (int j = 0; j < list.getLength(); j++) {
            		Node node1 = list.item(j);
            		if (node1.getNodeName().equals("attribute")) {
            			Element elm = (Element) node1;
            			String name = elm.getAttribute("name");
            			if (name.length() > 0)
            				hiddenAttributes.add(name);
            		}
            	}
            }
            // auto progagate attributes
            else if (node.getNodeName().equals("autoPropagateAtts")) {
//                <schemaClass name="Event">
//                  <attribute name="taxon" />
//                  <attribute name="_doRelease" />
//                </schemaClass>
                Map map = new HashMap();
                NodeList list = node.getChildNodes();
                for (int j = 0; j < list.getLength(); j++) {
                    Node clsNameNode = list.item(j);
                    if (clsNameNode.getNodeName().equals("schemaClass")) {
                        String clsName = ((Element)clsNameNode).getAttribute("name");
                        NodeList list1 = clsNameNode.getChildNodes();
                        for (int k = 0; k < list1.getLength(); k++) {
                            Node attNameNode = list1.item(k);
                            if (attNameNode.getNodeName().equals("attribute")) {
                                String attName = ((Element)attNameNode).getAttribute("name");
                                List attNames = (List) map.get(clsName);
                                if (attNames == null) {
                                    attNames = new ArrayList();
                                    map.put(clsName, attNames);
                                }
                                attNames.add(attName);
                            }
                        }
                    }
                }
                setSysetmAutoSetClsAttNames(map);
            }
            else if (node.getNodeName().equals("attributeAutoFillers")) {
                Map map = new HashMap();
                NodeList list = ((Element)node).getElementsByTagName("autoFiller");
                for (int j = 0; j < list.getLength(); j++) {
                    Element fillerElm = (Element) list.item(j);
                    String target = fillerElm.getAttribute("target");
                    String clsName = fillerElm.getAttribute("class");
                    if (target != null && target.length() > 0 &&
                        clsName != null && clsName.length() > 0)
                        map.put(target, clsName);
                }
                setAttributeAutoFillerMap(map);
            }
            else if (node.getNodeName().equals("modifiedResidues")) {
                modificationResidues = new HashMap<String, String>();
                NodeList list = ((Element)node).getElementsByTagName("residue");
                for (int j = 0; j < list.getLength(); j++) {
                    Element elm = (Element) list.item(j);
                    String name = elm.getAttribute("name");
                    String shortName = elm.getAttribute("shortName");
                    modificationResidues.put(name, shortName);
                }
            }
            else if (node.getNodeName().equals("psiModifiedResidues")) {
                psiModificationResidues = new HashMap<String, String>();
                NodeList list = ((Element)node).getElementsByTagName("residue");
                for (int j = 0; j < list.getLength(); j++) {
                    Element elm = (Element) list.item(j);
                    String name = elm.getAttribute("name");
                    String shortName = elm.getAttribute("shortName");
                    psiModificationResidues.put(name, shortName);
                }
            }
            else if (node.getNodeName().equals("modifications")) {
                modifications = new HashMap<String, String>();
                NodeList list = ((Element)node).getElementsByTagName("modification");
                for (int j = 0; j < list.getLength(); j++) {
                    Element elm = (Element) list.item(j);
                    String name = elm.getAttribute("name");
                    String shortName = elm.getAttribute("shortName");
                    modifications.put(name, shortName);
                }
            }
            else if (node.getNodeName().equals("psiModifications")) {
                psiModifications = new HashMap<String, String>();
                NodeList list = ((Element)node).getElementsByTagName("modification");
                for (int j = 0; j < list.getLength(); j++) {
                    Element elm = (Element) list.item(j);
                    String name = elm.getAttribute("name");
                    String shortName = elm.getAttribute("shortName");
                    psiModifications.put(name, shortName);
                }
            }
            else if (node.getNodeName().equals("ensureMultipleCopies")) {
                multipleCopyEntities = new HashMap<String, String>();
                NodeList list = ((Element)node).getElementsByTagName("entity");
                for (int j = 0; j < list.getLength(); j++) {
                    Element elm = (Element) list.item(j);
                    String name = elm.getAttribute("name");
                    String type = elm.getAttribute("type");
                    multipleCopyEntities.put(name, type);
                }
            }
            else if (node.getNodeName().equals("pdURL")) {
                Element elm = (Element) node;
                pdURL = elm.getAttribute("value");
            }
            else if (node.getNodeName().equals("devWebELV")) {
                Element elm = (Element) node;
                devWebELV = elm.getAttribute("value");
            }
            else if (node.getNodeName().equals("allowableAminoAcids")) {
                // Text wrapped by this element
                allowableAminoAcids = node.getFirstChild().getNodeValue();
            }
        }
    }
    
    public Map<String, String> getMultipleCopyEntities() {
        return this.multipleCopyEntities;
    }
    
    public boolean isMultipleCopyEntity(GKInstance pe) {
        String displayName = pe.getDisplayName();
        // Remove compartment in name
        if (displayName.endsWith("]")) {
            int index = displayName.lastIndexOf("[");
            if (index > 0) {
                displayName = displayName.substring(0, index).trim();
            }
        }
        if (multipleCopyEntities.containsKey(displayName)) {
            String type = multipleCopyEntities.get(displayName);
            if (pe.getSchemClass().isa(type)) {
                return true;
            }
        }
        return false;
    }
    
    public Map<String, String> getModifications() {
        return this.modifications;
    }
    
    public Map<String, String> getModificationResidues() {
        return this.modificationResidues;
    }
    
    public Map<String, String> getPsiModifications() {
        return this.psiModifications;
    }
    
    public Map<String, String> getPsiModificationResidues() {
        return this.psiModificationResidues;
    }

    /**
     * Some setting might be user's specific, which are from the user's properties. Use
     * this method to load these settings.
     */
    public void loadProperties(Properties prop) {
        setDisabledAutoClsAttsMap(prop);
        setAllowComboBoxEditor(prop);
        String groupAttByCategories = prop.getProperty("GroupAttributesByCategories");
        if (groupAttByCategories != null) {
            isGroupAttByCategories = Boolean.valueOf(groupAttByCategories).booleanValue();
        }
    }
	
	/**
	 * @return Returns the allowComboBoxEditor.
	 */
	public boolean isAllowComboBoxEditor() {
		return allowComboBoxEditor;
	}
	
	/**
	 * @param allowComboBoxEditor The allowComboBoxEditor to set.
	 */
	public void setAllowComboBoxEditor(boolean allowComboBoxEditor) {
		// Let any interested classes know that the combo box editor
		// state has been toggled
		if (this.allowComboBoxEditor != allowComboBoxEditor)
			propertyChangeSupport.firePropertyChange("AllowComboBoxEditor", this.allowComboBoxEditor, allowComboBoxEditor);

		// Save new value
		this.allowComboBoxEditor = allowComboBoxEditor;
	}
	
	/**
	 * An overloaded method to take the setting from properties
	 * @param prop
	 */
	private void setAllowComboBoxEditor(Properties prop) {
		// Check if allowComboBoxEditor is enabled
		String allowed = prop.getProperty("AttributeEdit.allowComboBoxEditor");
		boolean newValue;
		if (allowed == null || !Boolean.valueOf(allowed).booleanValue())
		    newValue = false;
		else
		    newValue = true;
		if (allowComboBoxEditor == newValue)
		    return;
		propertyChangeSupport.firePropertyChange("AllowComboBoxEditor", allowComboBoxEditor, newValue);
		allowComboBoxEditor = newValue;
	}
	
	public String getAllowableAminoAcids() {
	    return this.allowableAminoAcids;
	}
	
	/**
	 * Set the disabled auto-propagation attributes map.
	 * @param prop
	 */
	private void setDisabledAutoClsAttsMap(Properties prop) {
	    disabledAutoClsAttNamesMap.clear();
	    String value = prop.getProperty(DISABLE_AUTO_SETTING);
	    if (value == null || value.length() == 0)
	        return;
	    StringTokenizer tokenizer = new StringTokenizer(value, ", ");
	    int index = 0;
	    String token = null;
	    while (tokenizer.hasMoreTokens()) {
	        token = tokenizer.nextToken();
	        index = token.indexOf(".");
	        // First is class name
	        String className = token.substring(0, index);
	        // Second is attribute name
	        String attName = token.substring(index + 1);
	        List list = (List) disabledAutoClsAttNamesMap.get(className);
	        if (list == null) {
	            list = new ArrayList();
	            disabledAutoClsAttNamesMap.put(className, list);
	        }
	        list.add(attName);
	    }
	}
	
	/**
	 * Check if an attribute setting can be propagated from a ancestor to its descendant
	 * in the hierarchical tree.
	 * @param clsName 
	 * @param attName
	 * @return
	 */
	public boolean isAutoPropagatable(GKSchemaClass cls, String attName) {
	    // Check if cls can be propagated
	    String clsName = null;
	    boolean isPropgatable = false;
	    for (Iterator it = systemAutoSetClsAttNamesMap.keySet().iterator(); it.hasNext();) {
	        clsName = (String) it.next();
	        if (cls.isa(clsName)) {
	            isPropgatable = true;
	            break;
	        }
	    }
	    if (!isPropgatable)
	        return false;
	    // Check if attName is in the disabled list
		List list = new ArrayList(cls.getOrderedAncestors());
        list.add(cls); // Add itself
	    // Check if it is disabled first
	    Set disableAttNames = new HashSet();
	    for (Iterator it = list.iterator(); it.hasNext();) {
	        GKSchemaClass tmp = (GKSchemaClass) it.next();
	        List attNames = (List) disabledAutoClsAttNamesMap.get(tmp.getName());
	        if (attNames != null)
	            disableAttNames.addAll(attNames);
	    }
	    if (disableAttNames.contains(attName))
	        return false;
	    // Check if it is set in the system properties
		Set autoSettableAttNames = new HashSet();
		for (Iterator it = list.iterator(); it.hasNext();) {
			GKSchemaClass tmp = (GKSchemaClass) it.next();
			List attNames = (List) systemAutoSetClsAttNamesMap.get(tmp.getName());
			if (attNames != null)
				autoSettableAttNames.addAll(attNames);
		}
		if (autoSettableAttNames == null || autoSettableAttNames.size() == 0)
			return false;
		return autoSettableAttNames.contains(attName);
	}
	
	/**
	 * Takes the internal class variables and adds them to the properties list.
	 *
	 */
	public void commit(Properties prop) {
		if (allowComboBoxEditor)
			prop.setProperty("AttributeEdit.allowComboBoxEditor", true + "");
		else
			prop.remove("AttributeEdit.allowComboBoxEditor");
        if (isGroupAttByCategories)
            prop.setProperty("GroupAttributesByCategories", "true");
        else
            prop.remove("GroupAttributesByCategories");
	}
    
    /**
     * Set the map of AttributeAutoFiller.
     * @param map key: String for target value: full class name. See curator.xml file
     * for an example.
     */
    public void setAttributeAutoFillerMap(Map map) {
        // Defensive programming
        attributeAutoFillerMap.clear();
        attributeAutoFillerMap.putAll(map);
    }
    
    public AttributeAutoFiller getAttributeAutoFiller(String clsName, String attName) {
        String target = clsName + "." + attName;
        String fillerClassName = (String) attributeAutoFillerMap.get(target);
        if (fillerClassName == null)
            return null;
        try {
            AttributeAutoFiller filler = (AttributeAutoFiller) Class.forName(fillerClassName).newInstance();
            return filler;
        }
        catch(Exception e) {
            System.err.println("AttribtueEditConfig.getAttributeAutoFiller(): " + e);
            e.printStackTrace();
            return null;
        }
    }
}
