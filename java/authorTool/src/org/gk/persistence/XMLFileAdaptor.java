/*
 * Created on Jan 11, 2005
 *
 */
package org.gk.persistence;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gk.model.Bookmark;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceCache;
import org.gk.model.InstanceUtilities;
import org.gk.model.PathwayDiagramInstance;
import org.gk.model.PersistenceAdaptor;
import org.gk.model.ReactomeJavaConstants;
import org.gk.render.RenderUtility;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.InvalidAttributeException;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.gk.util.AuthorToolAppletUtilities;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This PersistenceAdaptor is used to talk to a single xml file that contains all local instances.
 * @author wgm
 */
public class XMLFileAdaptor implements PersistenceAdaptor {
    public static final char LINE_END = '\n';
    private static final String CHARSET_NAME = "UTF-8"; // The default charset
    // These variables are for escaping
    private static final char BRACKET = '<';
    private static final String BRACKET_ESCAPE = "&lt;";
    private static final char RIGHT_BRACKET = '>';
    private static final String RIGHT_BRACKET_ESCAPE = "&gt;";
    private static final char AMPERSAND = '&';
    private static final String AMPERSAND_ESCAPE = "&amp;";
    private static final char QUOTATION = '\"';
    private static final String QUOTATION_ESCAPE = "&quot;";
    private static final String LINE_END_ESCAPE = BRACKET_ESCAPE + "br" + RIGHT_BRACKET_ESCAPE;
    // For xml header
    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"" + CHARSET_NAME + "\" ?>"; 
    // The file name
    private String sourceName;
    // Cache the schema
    private GKSchema schema;
    // Cache the fetched instances to make sure there is only one copy for each instance
    // This index is duplicate of clsMap. Methods should ensure these two data structures
    // be in sync.
    private InstanceCache cache;
    // A list of deleted instances: Key: DB_ID, values: className
    private Map<Long, String> deleteMap;
    // For update views
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    // For local DB_ID. The initial value should get from the local saved file.
    private long leastID = 0;
    // The list of instances
    private Map<SchemaClass, List<GKInstance>> clsMap;
    // To track if isDirty
    private boolean isDirty = false;
    // bookmarks
    private Bookmarks bookmarks;
    // The default person is project based now as of Nov, 2007
    private Long defaultPersonId;
    // Add a new property for project: description as of Feb, 2008
    private String projectDescription;
    // Used to save the layout information
    @Deprecated
    private Map<GKInstance, RenderablePathway> pathwayToDiagram;
    // Used to map from PathwayDiagram instance to actual layout
    private Map<GKInstance, RenderablePathway> pdInstToDiagram;
        
    /**
     * Default constructor.
     * @throws Exception an Exception might be thrown during schema loading from
     * a local file.
     */
    public XMLFileAdaptor() throws Exception {
        cache = new InstanceCache();
        deleteMap = new HashMap<Long, String>();
        clsMap = new HashMap<SchemaClass, List<GKInstance>>();
        bookmarks = new Bookmarks();
        bookmarks.addPropertyChangeListener(new PropertyChangeListener() {
        	public void propertyChange(PropertyChangeEvent e) {
        		isDirty = true; // Mark is dirty and call for saving
        		propertyChangeSupport.firePropertyChange("fileIsDirty", false, true);
        	}
        });
        fetchSchema();
    }
    
    /**
     * Register a pathway layout stored in RenderablePathway. 
     * Use {@link #addDiagramForPathwayDiagram(GKInstance, RenderablePathway)} instead.
     * The implementation of this method has been delegated to addDiagramForPathwayDiagram().
     * @param pathway pathway has a layout information.
     * @param diagram layout contained in RenderablePathway.
     */
    @Deprecated
    public void addDiagram(GKInstance pathway,
                           RenderablePathway diagram) throws Exception {
        // Get the PathwayDigram instance of this pathway
        Collection<?> c = fetchInstanceByAttribute(ReactomeJavaConstants.PathwayDiagram,
                                                   ReactomeJavaConstants.representedPathway,
                                                   "=",
                                                   pathway);
        if (c == null || c.size() == 0)
            return;
        GKInstance pdInst = (GKInstance) c.iterator().next();
        addDiagramForPathwayDiagram(pdInst, diagram);
    }
    
    /**
     * Register a RenderablePathway for its corresponding PathwayDiagram instance.
     * @param pdInst
     * @param diagram
     * @throws Exception
     */
    public void addDiagramForPathwayDiagram(GKInstance pdInst,
                                            RenderablePathway diagram) throws Exception {
        if (pdInstToDiagram == null)
            pdInstToDiagram = new HashMap<GKInstance, RenderablePathway>();
        boolean isReplace = pdInstToDiagram.containsKey(pdInst);
        pdInstToDiagram.put(pdInst, diagram);
        // Just in case 
        diagram.setReactomeDiagramId(pdInst.getDBID());
        if (isReplace) {
            // First a property change event
            propertyChangeSupport.firePropertyChange("updateDiagram",
                                                     pdInst,
                                                     diagram);
        }
    }
    
    /**
     * A helper method to get pathways represented by a RenderablePathway, which may have more than one pathway.
     * @param pathway
     * @return
     * @throws Exception
     */
    public List<GKInstance> getRepresentedPathwaysInDiagram(RenderablePathway diagram) throws Exception {
        List<GKInstance> pathways = new ArrayList<GKInstance>();
        if (diagram.getReactomeDiagramId() != null) {
            GKInstance pdInst = fetchInstance(diagram.getReactomeDiagramId());
            List<?> values = pdInst.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
            if (values != null && values.size() > 0) {
                for (Object obj : values)
                    pathways.add((GKInstance)obj);
            }
        }
        if (pathways.size() == 0 && diagram.getReactomeId() != null) {
            GKInstance pathway = fetchInstance(diagram.getReactomeId());
            if (pathway != null)
                pathways.add(pathway);
        }
        return pathways;
    }
    
    /**
     * Get the diagram for the passed pathway instance. It may be null if a pathway
     * has not be laid out.
     * @param pathway
     * @return
     */
    public RenderablePathway getDiagram(GKInstance pathway) throws Exception {
        // Search for diagram from pdInstanceToDiagram map
        for (GKInstance pdInst : pdInstToDiagram.keySet()) {
            List<?> pathways = pdInst.getAttributeValuesList(ReactomeJavaConstants.representedPathway);
            if (pathways != null && pathways.contains(pathway))
                return pdInstToDiagram.get(pdInst);
        }
        return null;
    }
    
    /**
     * Get the PathwayDiagram instance for a RenderablePathway diagram.
     * @param diagram
     * @return
     * @throws Exception
     */
    public GKInstance getPathwayDiagramInstance(RenderablePathway diagram) throws Exception {
        for (GKInstance pdInst : pdInstToDiagram.keySet()) { 
            RenderablePathway diagram1 = pdInstToDiagram.get(pdInst);
            if (diagram1 == diagram)
                return pdInst;
        }
        return null;
    }
    
    /**
     * An overloaded constructor.
     * @param source the xml file name.
     */
    public XMLFileAdaptor(String source) throws Exception {
        this();
        setSource(source);
    }
    
    public void setSource(String source) throws Exception {
    	reset();
    	// The source might be null
    	if (source != null) {
    		load(source);
    		isDirty = false;
    		propertyChangeSupport.firePropertyChange("save", false, true);
    	}
    	// Set it only after loaded successfully
    	this.sourceName = source;
    }
    
    public void setSource(String source, boolean needReload) throws Exception {
        if (needReload)
            setSource(source);
        else
            this.sourceName = source;
    }
    
    public void setProjectDescription(String desc) {
        this.projectDescription = desc;
    }
    
    public String getProjectDescription() {
        return this.projectDescription;
    }
    
    public Long getDefaultPersonId() {
        return defaultPersonId;
    }

    public void setDefaultPersonId(Long defaultPersonId) {
        this.defaultPersonId = defaultPersonId;
    }

    /**
     * Reset this XMLFileAdaptor to the empty state. All cached values will be empted.
     * The source name will also be null.
     */
    public void reset() {
        leastID = 0;
    	cache.clear();
    	clsMap.clear();
    	deleteMap.clear();
    	if (bookmarks != null)
    		bookmarks.clear();
    	this.sourceName = null;
    	if (pathwayToDiagram != null)
    	    pathwayToDiagram.clear();
    	if (pdInstToDiagram != null)
    	    pdInstToDiagram.clear();
    }
    
    private void load(String fileName) throws Exception {
        FileInputStream fis = new FileInputStream(fileName);
        load(fis);
//        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = dbf.newDocumentBuilder();
//        Document doc = builder.parse(fileName);
//        Element root = doc.getDocumentElement();
//        // The first level is the classes
//        Element reactomeElm = getReactomeElement(root);
//        NodeList classNodes = reactomeElm.getChildNodes();
//        int size = classNodes.getLength();
//        for (int i = 0; i < size; i++) {
//            Node clsNode = classNodes.item(i);
//            if (clsNode.getNodeType() != Node.ELEMENT_NODE)
//                continue; // Handle element node only
//            if (clsNode.getNodeName().equals("meta")) 
//                loadMeta(clsNode);
//            else
//                loadInstances(clsNode);
//        }
//        // This method has to be called after all instances have been loaded
//        // since some of attributes in the instances are used by Renderable objects
//        // in the diagram (e.g. _displayName)
//        loadDiagrams(root);
    }
    
    public void load(InputStream in) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(in);
        Element root = doc.getDocumentElement();
        // The first level is the classes
        Element reactomeElm = getReactomeElement(root);
        NodeList classNodes = reactomeElm.getChildNodes();
        int size = classNodes.getLength();
        for (int i = 0; i < size; i++) {
            Node clsNode = classNodes.item(i);
            if (clsNode.getNodeType() != Node.ELEMENT_NODE)
                continue; // Handle element node only
            if (clsNode.getNodeName().equals("meta")) 
                loadMeta(clsNode);
            else
                loadInstances(clsNode);
        }
        // This method has to be called after all instances have been loaded
        // since some of attributes in the instances are used by Renderable objects
        // in the diagram (e.g. _displayName)
        loadDiagrams(root);
    }
    
    /**
     * A helper method is used to load diagrams.
     * @param root
     */
    private void loadDiagrams(Element root) throws Exception {
        // For old documents
        String elmName = root.getNodeName();
        if (elmName.equals("reactome"))
            return ;
        NodeList list = root.getChildNodes();
        DiagramGKBReader diagramReader = new DiagramGKBReader();
        diagramReader.setPersistenceAdaptor(this);
        // Used to convert from DOM element to JDOM element
        DOMBuilder domBuilder = new DOMBuilder();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeName().equals("Process")) {
                Element processElm = (Element) node;
                org.jdom.Element jdomElm = domBuilder.build(processElm);
                loadDiagram(diagramReader, 
                            jdomElm);
            }
        }
    }

    private void loadDiagram(DiagramGKBReader diagramReader,
                             org.jdom.Element jdomElm) throws Exception {
        RenderablePathway diagram = diagramReader.openProcess(jdomElm);
        diagramReader.setDisplayNames(diagram, this);
        // Check DiagramInstance first
        String diagramId = jdomElm.getAttributeValue("reactomeDiagramId");
        if (diagramId != null) {
            GKInstance diagramInst = fetchInstance(new Long(diagramId));
            if (diagramInst != null) {
                addDiagramForPathwayDiagram(diagramInst, diagram);
            }
        }
        else { // Second choice
            String processId = jdomElm.getAttributeValue("reactomeId");
            GKInstance instance = fetchInstance(new Long(processId));
            addDiagram(instance, diagram);
        }
    }
    
    private Element getReactomeElement(Element docElm) {
        String elmName = docElm.getNodeName();
        if (elmName.equals("reactome"))
            return docElm;
        else if (elmName.equals("rtpj")) {
            NodeList list = docElm.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (node.getNodeName().equals("reactome"))
                    return (Element) node;
            }
        }
        return docElm;
    }
    
    private void loadMeta(Node metaNode) {
        // There are two meta types: deletion and bookmarks
        NodeList children = metaNode.getChildNodes();
        int size = children.getLength();
        for (int i = 0; i < size; i++) {
            Node node = children.item(i);
            if (node.getNodeName().equals("deletion"))
                loadDeletion(node);
            else if (node.getNodeName().equals("bookmarks")) {
                loadBookmarks(node);
            }
            else if (node.getNodeName().equals("defaultPerson")) {
                loadDefaultPerson(node);
            }
            else if (node.getNodeName().equals("description")) {
                loadProjectDescription(node);
            }
        }
    }
    
    private void loadProjectDescription(Node node) {
        Node firstChild = ((Element)node).getFirstChild();
        if (firstChild != null) {
            String text = firstChild.getNodeValue(); // Should be text
            projectDescription = text.replaceAll("<br>", LINE_END + "");
        }
    }
    
    private void loadDefaultPerson(Node node) {
        String dbId = ((Element)node).getAttribute("dbId");
        if (dbId != null)
            setDefaultPersonId(new Long(dbId));
    }
    
    private void loadDeletion(Node deletionNode) {
        NodeList children = deletionNode.getChildNodes();
        int size = children.getLength();
        for (int i = 0; i < size; i++) {
            Node instanceNode = children.item(i);
            if (instanceNode.getNodeName().equals("instance")) {
                Element elm = (Element) instanceNode;
                String dbId = elm.getAttribute("DB_ID");
                String className = elm.getAttribute("class");
                deleteMap.put(new Long(dbId), className);
            }
        }
    }
    
    public Long getNextLocalID() {
        return new Long(--leastID);
    }
    
    public Map<Long, String> getDeleteMap() {
        return deleteMap;
    }
    
    private void loadBookmarks(Node bookmarkNode) {
        java.util.List bkList = new ArrayList();
        Element bookmarkElm = (Element) bookmarkNode;
        String sortingKey = bookmarkElm.getAttribute("sortingKey");
        NodeList list = bookmarkElm.getElementsByTagName("bookmark");
        for (int i = 0; i < list.getLength(); i++) {
            Element elm = (Element) list.item(i);
            Bookmark bookmark = new Bookmark();
            bookmark.setDisplayName(elm.getAttribute("displayName"));
            bookmark.setDbID(new Long(elm.getAttribute("DB_ID")));
            bookmark.setType(elm.getAttribute("type"));
            String desc = elm.getAttribute("desc");
            if (desc != null && desc.length() > 0)
                bookmark.setDescription(desc);
            bkList.add(bookmark);
        }
        bookmarks.setSortingKey(sortingKey);
        bookmarks.setBookmarks(bkList);
    }
    
    public Bookmarks getBookmarks() {
        return this.bookmarks;
    }
    
    private void loadInstances(Node clsNode) throws Exception {
        String clsName = clsNode.getNodeName();
        SchemaClass cls = schema.getClassByName(clsName);
        if (cls == null) {
            throw new IllegalStateException("Class \"" + clsName +  "\" cannot be found!");
        }
        NodeList instanceNodes = clsNode.getChildNodes();
        int size = instanceNodes.getLength();
        List<GKInstance> instances = new ArrayList<GKInstance>(size);
        clsMap.put(cls, instances);
        for (int i = 0; i < size; i++) {
            Node instanceNode = instanceNodes.item(i);
            if (instanceNode.getNodeName().equals("instance")) {
                GKInstance instance = loadInstance((Element)instanceNode, cls);
                instances.add(instance);
            }
        }
    }
    
    private GKInstance loadInstance(Element instanceNode, 
                                    SchemaClass cls) throws Exception {
        String dbId = instanceNode.getAttribute("DB_ID");
        Long id = new Long(dbId);
        GKInstance instance = createInstance(id, cls);
        String displayName = instanceNode.getAttribute("displayName");
        String isShell = instanceNode.getAttribute("isShell");
        boolean shell = Boolean.valueOf(isShell).booleanValue();
        instance.setIsShell(shell);
        String isDirty = instanceNode.getAttribute("isDirty");
        if (isDirty != null && isDirty.length() > 0)
        	instance.setIsDirty(Boolean.valueOf(isDirty).booleanValue());
        if (!shell)  // load more attributes. Don't set displayName for it
            loadAttributes(instanceNode, instance);
        else 
            instance.setDisplayName(displayName);
        instance.setDBID(id); // Because of a bug in GKInstance, DB_ID has
                              // to be set explicitly.
        				      // Another bug: DB_ID is definied as Integer type,
                              // however, it is used as Long.
        return instance;
    }
    
    private void loadAttributes(Element instanceNode, GKInstance instance) throws Exception {
        NodeList attributeNodes = instanceNode.getChildNodes();
        int size = attributeNodes.getLength();
        SchemaClass schemaClass = instance.getSchemClass();
        for (int i = 0; i < size; i++) {
            Node attNode = attributeNodes.item(i);
            if (attNode.getNodeName().equals("attribute")) {
                Element elm = (Element) attNode;
                String name = elm.getAttribute("name");
                if (!schemaClass.isValidAttribute(name)) // Schema might be changed
                    continue;
                SchemaAttribute att = schemaClass.getAttribute(name);
                String value = null;
                Object attValue = null;
                int type = att.getTypeAsInt();
                switch (type) {
                    case SchemaAttribute.INSTANCE_TYPE :
                        String clsValue = elm.getAttribute("class");
                        value = elm.getAttribute("referTo");
                        Long dbID = new Long(value);
                        GKInstance tmpInstance = createInstance(dbID, clsValue);
                        attValue = tmpInstance;
                        break;
                    case SchemaAttribute.STRING_TYPE :
                        value = elm.getAttribute("value");
                        attValue = value;
                        break;
                    case SchemaAttribute.INTEGER_TYPE :
                        value = elm.getAttribute("value");
                        attValue = new Integer(value);
                        break;
                    case SchemaAttribute.LONG_TYPE :
                        value = elm.getAttribute("value");
                        attValue = new Long(value);
                        break;
                    case SchemaAttribute.FLOAT_TYPE :
                        value = elm.getAttribute("value");
                        attValue = new Float(value);
                        break;
                    case SchemaAttribute.BOOLEAN_TYPE :
                        value = elm.getAttribute("value");
                        attValue = new Boolean(value);
                        break;
                    default :
                        value = elm.getAttribute("value");
                        attValue = value;        
                }
                if (att.isMultiple())
                    instance.addAttributeValueNoCheck(att, attValue);
                else {
                    if (name.equals(ReactomeJavaConstants.storedATXML))
                        attValue = attValue.toString().replaceAll("<br>", LINE_END + "");
                    instance.setAttributeValueNoCheck(att, attValue);
                }
            }
        }
        instance.setIsInflated(true);
    }
    
    private GKInstance createInstance(Long dbID, String clsName) {
        SchemaClass cls = schema.getClassByName(clsName);
        return createInstance(dbID, cls);
    }
    
    public GKInstance createInstance(Long dbID, SchemaClass cls) {
        GKInstance instance = cache.get(dbID);
        if (instance != null)
            return instance;
        if (cls.isa(ReactomeJavaConstants.PathwayDiagram))
            instance = new PathwayDiagramInstance(cls, dbID, this);
        else
            instance = new GKInstance(cls, dbID, this);
        instance.setDBID(dbID);
        cache.put(instance);
        // Get the smallest id
        if (dbID.longValue() < leastID) 
            leastID = dbID.longValue();
        return instance;
    }
    
    /**
     * Usually the client should call save() first to avoid losing the changes. 
     * Otherwise, an IllegalStateException will be thrown.
     * @throws Exception
     */
    public void refresh() throws Exception {
        if (isDirty())
            throw new IllegalStateException("FileAdaptor.refresh(): Cannot refresh because there are changes not saved.");
        fetchSchema();
        setSource(sourceName);
    }
    
    public String getSourceName() {
        return this.sourceName;
    }
    
    /**
     * Create a new GKInstance located in the local repository.
     * @param clsName
     * @return
     */
    public GKInstance createNewInstance(String clsName) {
        GKInstance instance = createInstance(getNextLocalID(), clsName);
        addNewInstance(instance);
        return instance;
    }
    
    /**
     * Register a newly created Instance.
     * @param newInstance
     */
    public void addNewInstance(GKInstance newInstance) {
        SchemaClass cls = newInstance.getSchemClass();
        List<GKInstance> list = clsMap.get(cls);
        if (list == null) {
            list = new ArrayList<GKInstance>();
            clsMap.put(cls, list);
        }
        list.add(newInstance);
        List<GKInstance> newValues = new ArrayList<GKInstance>();
        newValues.add(newInstance);
        // Remove from deleteMap
        if (deleteMap.containsKey(newInstance.getDBID()))
            deleteMap.remove(newInstance.getDBID());
        if (newInstance.getDBID().longValue() < 0)
            ((GKInstance)newInstance).setIsDirty(true);
        // Have to add to cache
        // A new instance might be a shell instance attached with checked out instances.
        cache.put(newInstance);
        isDirty = true;
        propertyChangeSupport.firePropertyChange("addNewInstance", null, newValues);
    }
    
    /**
     * Switch the class type for a specified GKInstance object. Three things are processed here:
     * Make sure the old slot values are valid in the new schemaclass context, mimic the deleting 
     * operation for the instance, mimic the adding operation for the instance. Mimicing is used
     * to make data structure correct.
     * @param instance
     * @param newCls
     */
    public void switchType(GKInstance instance, GKSchemaClass newCls) {
        GKSchemaClass oldCls = (GKSchemaClass) instance.getSchemClass();
        try {
            Map<String, List<?>> values = new HashMap<String, List<?>>();
            Long dbID = instance.getDBID();
            for (Iterator<?> it = instance.getSchemaAttributes().iterator(); it.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) it.next();
                java.util.List<?> list = instance.getAttributeValuesList(att);
                if (list != null && list.size() > 0) {
                    values.put(att.getName(), 
                               new ArrayList<Object>(list));
                    list.clear();
                }
            }
            removeFromClassMap(instance);
            instance.setSchemaClass(newCls);
            for (Iterator<?> it = newCls.getAttributes().iterator(); it.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) it.next();
                java.util.List<?> list = values.get(att.getName());
                if (list == null)
                    continue;
                for (Iterator<?> it1 = list.iterator(); it1.hasNext();) {
                    Object obj = it1.next();
                    if (att.isValidValue(obj))
                        instance.addAttributeValue(att, obj);
                }
            }
            instance.setDBID(dbID); // Have to reset DB_ID since it might get lost.
            instance.setIsInflated(true);
            instance.setIsDirty(true);
            addToClasssMap(instance);
            isDirty = true;
            propertyChangeSupport.firePropertyChange("switchType", oldCls, instance);
        }
        catch (Exception e) {
            System.err.println("FileAdaptor.switchType(): " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * Remove the dirty flag from the specify GKInstance object.
     * @param instance
     */
    public void removeDirtyFlag(GKInstance instance) {
        if (!instance.isDirty())
            return; // Do nothing it is already NOT Dirty
        instance.setIsDirty(false);
        isDirty = true; // The whole file is dirty
        propertyChangeSupport.firePropertyChange("removeDirty", null, instance);
    }
    
    /**
     * Check if any of the GKInstance in the specified GKSchemaClass is dirty.
     * @param cls
     * @return 
     */
    public boolean isDirty(GKSchemaClass cls) throws Exception {
        Collection<?> instances = fetchInstancesByClass(cls.getName());
        GKInstance instance = null;
        for (Iterator<?> it = instances.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.isDirty())
                return true;
        }
        if (deleteMap != null) {
            HashSet clsNames = new HashSet(deleteMap.values());
            String clsName = cls.getName();
            String tmp = null;
            GKSchemaClass tmpCls = null;
            GKSchema schema = (GKSchema) getSchema();
            for (Iterator it = clsNames.iterator(); it.hasNext();) {
                tmp = (String) it.next();
                tmpCls = (GKSchemaClass) schema.getClassByName(tmp);
                if (tmpCls.isa(clsName))
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Mark an Instance as dirty so that it can be saved. If the specified
     * instance is a new instance, it will not be marked. However, marking
     * a dirty instance will trigger a markAsDirty property event without
     * any values.
     * @param instance
     */
    public void markAsDirty(Instance instance) {
        if (((GKInstance)instance).isDirty()) {
            isDirty = true;
            propertyChangeSupport.firePropertyChange("markAsDirty", null, null);
            return;
        }
        ((GKInstance)instance).setIsDirty(true);
		// Check if instance is a new GKInstance object. It will occur
	    // if a GKInstance is editing in the new instance dialog.
	    SchemaClass cls = instance.getSchemClass();
	    try {
	        if (fetchInstance(cls.getName(), instance.getDBID()) == null)
	            return;
	    }
	    catch(Exception e) {
	        System.err.println("XMLFileAdaptor.markAsDirty(): " + e);
	        e.printStackTrace();
	    }
        isDirty = true; // If one instance is dirty, the whole file will be dirty.
        propertyChangeSupport.firePropertyChange("markAsDirty", null, instance);
    }
    
    public void markAsDirty() {
        isDirty = true;
        propertyChangeSupport.firePropertyChange("markAsDirty", null, null);
    }
    
    /**
     * Delete a specified GKInstance object from the repository.
     * @param instance
     */
    public void deleteInstance(GKInstance instance) {
        SchemaClass schemaClass = instance.getSchemClass();
        // Have to null other instances' attribute values that refer to this instance.                                                            
        try {
            // Use a fast way
            java.util.List referrers = getReferers(instance);
            for (Iterator it = referrers.iterator(); it.hasNext();) {
                GKInstance refer = (GKInstance) it.next();
                for (Iterator it1 = refer.getSchemaAttributes().iterator(); it1.hasNext();) {
                    GKSchemaAttribute att = (GKSchemaAttribute) it1.next();
                    if (!att.isInstanceTypeAttribute())
                        continue;
                    java.util.List values = refer.getAttributeValuesList(att);
                    if (values == null || values.size() == 0)
                        continue;
                    if (!att.isValidValue(instance))
                        continue;
                    for (Iterator it2 = values.iterator(); it2.hasNext();) {
                        GKInstance value = (GKInstance) it2.next();
                        if (value == instance)
                            it2.remove();
                    }
                    markAsDirty(refer);
                }
            }
            // Need to clear referrers of this instance
            Collection<?> attributes = instance.getSchemaAttributes();
            for (Iterator<?> it = attributes.iterator(); it.hasNext();) {
                SchemaAttribute attribute = (SchemaAttribute) it.next();
                if (!attribute.isInstanceTypeAttribute())
                    continue;
                List<?> values = instance.getAttributeValuesList(attribute);
                if (values == null)
                    continue;
                for (Object obj : values) {
                    GKInstance value = (GKInstance) obj;
                    value.clearReferers();
                }
            }
        }
        catch (Exception e) {
            System.err.println("XMLFileAdaptor.deleteInstance(): " + e);
            e.printStackTrace();
        }
        // Need to keep track the deleted instance
        if (instance.getDBID() != null) {
            if (instance.getDBID().longValue() > -1) // An instance from the db
                deleteMap.put(instance.getDBID(), instance.getSchemClass().getName());
        }
        // Remove it from cache
        cache.remove(instance.getDBID());
        removeFromClassMap(instance);
        if (instance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram))
            deletePathwayDiagram(instance);
        isDirty = true;
        propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
    }
    
    private void deletePathwayDiagram(GKInstance pathwayDiagram) {
        if (pdInstToDiagram == null)
            return;
        pdInstToDiagram.remove(pathwayDiagram);
    }
    
    public Collection fetchInstanceByAttribute(String className, 
                                               String attName, 
                                               String operator, 
                                               Object value) throws Exception {
        SchemaClass schemaClass = schema.getClassByName(className);
        if (!schemaClass.isValidAttribute(attName))
            throw new InvalidAttributeException(schemaClass, attName);
        operator = operator.toUpperCase();
        // Need to check operator
        if (!(operator.equals("=") || 
              operator.equals("LIKE") || 
              operator.equals("REGEXP") ||
              operator.equals("IS NOT NULL") || 
              operator.equals("IS NULL") || 
              operator.equals("!="))) {
            throw new IllegalArgumentException("FileAdator.fetchInstanceByAttribute(): Unsupported operator: " + operator);
        }
        if ((value instanceof GKInstance) && operator.equals("REGEXP")) {
            throw new IllegalArgumentException("FileAdator.fetchInstanceByAttribute(): Unsupported operator: " + operator);
        }
        // Have to get rid of % for like
        if (operator.equals("LIKE")) {
            String tmp = value.toString();
            value = tmp.substring(1, tmp.length() - 1);
        }
        java.util.List rtnList = new ArrayList();
        // Check newly created Instance objects
        Collection instances = fetchInstancesByClass(schemaClass);
        if (instances != null && instances.size() > 0) {
            for (Iterator it = instances.iterator(); it.hasNext();) {
                GKInstance instance = (GKInstance) it.next();
                if (checkInstance(instance, attName, operator, value))
                    rtnList.add(instance);
            }
        }
        return rtnList;
    }   
    
    /**
     * A helper to check if the specified GKInstance object can be passed a comparison.
     */
    private boolean checkInstance(GKInstance instance, 
                                  String attName,
                                  String operator,
                                  Object value) throws Exception {
        // Use nocheck method to improve the method a little bit. The valid of attName
        // has been validated by the caller of this method.
        java.util.List<?> attValues = instance.getAttributeValuesListNoCheck(attName);
        if (attValues != null && attValues.size() > 0) {
            if (operator.equals("IS NOT NULL"))
                return true;
            // Based on attribute type
            if (attValues.get(0) instanceof GKInstance) {
                // value should be GKInstance
                if (!(value instanceof GKInstance))
                    return false;
                GKInstance target = (GKInstance) value;
                if (operator.equals("=")) {
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        GKInstance tmp = (GKInstance) it.next();
                        if (tmp.getDBID().equals(target.getDBID()))
                            return true;
                    }
                }
                else if (operator.equals("!=")) {
                    // Make sure no instance in the list
                    boolean isFound = false;
                    for (Iterator<?> it = attValues.iterator(); it.hasNext();) {
                        GKInstance tmp = (GKInstance) it.next();
                        if (tmp.getDBID().equals(target.getDBID())) {
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound)
                        return true;
                }
                else if (operator.equals("LIKE")) { // Contains
                    // Want to make it case insentitive
                    String displayName = target.getDisplayName().toLowerCase();
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        GKInstance tmp = (GKInstance) it.next();
                        String name1 = tmp.getDisplayName().toLowerCase();
                        if (name1.indexOf(displayName) >= 0)
                            return true;
                    }
                }
                // Cannot support regex
                else if (operator.equals("REGEXP"))
                    throw new IllegalArgumentException(
                                    "FileAdaptor.checkInstance(): Opertor is not supported: "
                                                    + operator);
            }
            else {
//              Don't call toLowerCase. It is an expensive operation. Call it if needed.
                String target = value.toString();
                if (operator.equals("=")) {
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        Object obj = it.next();
                        if (target.equalsIgnoreCase(obj.toString()))
                            return true;
                    }
                }
                else if (operator.equals("!=")) {
                    boolean isFound = false;
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        Object obj = it.next();
                        if (target.equalsIgnoreCase(obj.toString())) {
                            isFound = true;
                            break;
                        }
                    }
                    if (!isFound)
                        return true;
                }
                else if (operator.equals("LIKE")) {
                    // Want to make it case insensitive. 
                    target = target.toLowerCase();
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        String tmp = it.next().toString().toLowerCase();
                        if (tmp.indexOf(target) >= 0)
                            return true;
                    }
                }
                else if (operator.equals("REGEXP")) {
                    Pattern pattern = Pattern.compile(target);
                    for (Iterator it = attValues.iterator(); it.hasNext();) {
                        String tmp = it.next().toString();
                        Matcher matcher = pattern.matcher(tmp);
                        if (matcher.find())
                            return true;
                    }
                }
            }
        }
        else if (operator.equals("IS NULL"))
            return true;
        return false;
    }

    /**
     * A helper to fetch instance by class name and its id.
     */
    public GKInstance fetchInstance(String className, Long dbID) throws Exception {
        Collection c = fetchInstancesByClass(className);
        if (c == null || c.size() == 0)
            return null;
        GKInstance instance = null;
        for (Iterator it = c.iterator(); it.hasNext();) {
            instance = (GKInstance) it.next();
            if (instance.getDBID().equals(dbID))
                return instance;
        }
        return null;
    }
    
    /**
     * Fetch a GKInstance based on its DB_ID.
     * @param dbID
     * @return
     */
    public GKInstance fetchInstance(Long dbID) {
        return (GKInstance) cache.get(dbID);
    }
    
    /* 
     * @see org.gk.model.PersistenceAdaptor#fetchInstanceByAttribute(org.gk.schema.SchemaAttribute, java.lang.String, java.lang.Object)
     */
    public Collection fetchInstanceByAttribute(SchemaAttribute attribute, String operator, Object value)
        throws Exception {
        // Need to call from the topmost SchemaClass.
        SchemaClass origin = attribute.getOrigin();
        return fetchInstanceByAttribute(origin.getName(), attribute.getName(), operator, value);
    }

    /* 
     * @see org.gk.model.PersistenceAdaptor#fetchSchema()
     */
    public Schema fetchSchema() throws Exception {
        schema = (GKSchema) AuthorToolAppletUtilities.fetchLocalSchema();
        return schema;
    }
    
    public void saveSchema(Schema schema) throws IOException {
        AuthorToolAppletUtilities.saveLocalSchema(schema);
    }
    
    /* 
     * Counting the instance in the specified SchemaClass and its descendent classes.
     * @see org.gk.model.PersistenceAdaptor#getClassInstanceCount(org.gk.schema.SchemaClass)
     */
    public long getClassInstanceCount(SchemaClass schemaClass) throws Exception {
    	List clsList = new ArrayList();
    	InstanceUtilities.getDescendentClasses(clsList, (GKSchemaClass)schemaClass);
    	int c = 0;
    	List instanceList = null;
    	for (Iterator it = clsList.iterator(); it.hasNext();) {
    		instanceList = (List) clsMap.get(it.next());
    		if (instanceList == null)
    			continue;
    		c += instanceList.size();
    	}
    	return c;
    }
    
    private void addToClasssMap(GKInstance instance) {
        List list = (List) clsMap.get(instance.getSchemClass());
        if (list == null) {
            list = new ArrayList();
            clsMap.put(instance.getSchemClass(), list);
        }
        list.add(instance);
    }
    
    public void removeFromClassMap(GKInstance instance) {
        List list = (List) clsMap.get(instance.getSchemClass());
        if (list != null)
            list.remove(instance);
    }
    
    /**
     * Remove the delete record for a list of DB_IDs.
     * @param dbIDs a list of DB_IDs.
     * @throws IOException
     */
    public void clearDeleteRecord(List<Long> dbIDs) throws IOException {
        if (dbIDs == null || dbIDs.size() == 0)
            return;
        // Remove the specified DB_IDs from the deleteMaps
        Set<SchemaClass> clses = new HashSet<SchemaClass>();
        for (Long dbID : dbIDs) {
            String clsName = (String) deleteMap.get(dbID);
            if (clsName == null)
                continue; // Just in case the passed dbIDs are not in the deletion map.
            clses.add(schema.getClassByName(clsName));
            deleteMap.remove(dbID);
        }
        isDirty = true;
        propertyChangeSupport.firePropertyChange("clearDeleteRecord", null, clses);
        propertyChangeSupport.firePropertyChange("fileIsDirty", false, true);
        // Have to fire out something
        // propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
    }
    
    public List<?> fetchAllEvents() throws Exception {
    	return (List<?>) fetchInstancesByClass(ReactomeJavaConstants.Event);
    }
    
    /* 
     * @deprecated: PersistenceManager.getSchema() should be used.
     */
    public Schema getSchema() {
        if (schema == null) {
            try {
                fetchSchema();
            }
            catch(Exception e) {
                System.err.println("XMLFileAdaptor.getSchema(): " + e);
            }
        }
        return schema;
    }
    
    /* 
     * @see org.gk.model.PersistenceAdaptor#loadInstanceAttributeValues(org.gk.model.GKInstance, org.gk.schema.SchemaAttribute)
     */
    public void loadInstanceAttributeValues(GKInstance instance, 
                                            SchemaAttribute attribute) throws Exception {
        if (instance instanceof PathwayDiagramInstance) {
            if (!attribute.getName().equals(ReactomeJavaConstants.width) &&
                !attribute.getName().equals(ReactomeJavaConstants.height) &&
                !attribute.getName().equals(ReactomeJavaConstants.storedATXML))
                return; // Only the above attributes should be updated
            RenderablePathway diagram = pdInstToDiagram.get(instance);
            if (diagram == null)
                return;
            if (attribute.getName().equals(ReactomeJavaConstants.width)) {
                Dimension size = RenderUtility.getDimension(diagram);
                instance.setAttributeValueNoCheck(ReactomeJavaConstants.width, 
                                                  size.width);
            }
            else if (attribute.getName().equals(ReactomeJavaConstants.height)) {
                Dimension size = RenderUtility.getDimension(diagram);
                instance.setAttributeValueNoCheck(ReactomeJavaConstants.height, size.height);
            }
            else if (attribute.getName().equals(ReactomeJavaConstants.storedATXML)) {
                GKBWriter writer = new GKBWriter();
                Project project = new Project(diagram);
                String xml = writer.generateXMLString(project);
                instance.setAttributeValueNoCheck(ReactomeJavaConstants.storedATXML, 
                                                  xml);
            }
        }
        // Otherwise, do nothing. Everything is loaded.
    }
    
    /**
     * Assign GKInstance to Renderable objects in diagrams based on reactome ids
     * used. This will switch the calling of Renderable.getReactomeId() to the instance
     * model to avoid DB_ID changes.
     */
    public void assignInstancesToDiagrams() {
        if (pdInstToDiagram == null || pdInstToDiagram.size() == 0)
            return;
        for (RenderablePathway diagram : pdInstToDiagram.values()) {
            // Diagram itself
            if (diagram.getReactomeDiagramId() != null) {
                GKInstance inst = fetchInstance(diagram.getReactomeDiagramId());
                diagram.setInstance(inst);
            }
            else if (diagram.getReactomeId() != null) { // For some old format
                GKInstance inst = fetchInstance(diagram.getReactomeId());
                diagram.setInstance(inst);
            }
            List components = diagram.getComponents();
            if (components == null || components.size() == 0)
                continue;
            for (Iterator it = components.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                Long dbId = r.getReactomeId();
                if (dbId == null)
                    continue;
                GKInstance instance = fetchInstance(dbId);
                r.setInstance(instance);
            }
        }
    }
    
    /**
     * Use this method to load all attributes.
     * @param instance
     */
    public void loadInstanceAttributes(GKInstance instance) throws Exception {
        // Do nothing. Everything is loaded.
    }
    
    /**
     * Return a map of referrers to the specified GKInstance object.
     * @param instance 
     * @return key: attribute name; value: a list of GKInstances.
     * @throws Exception
     */
    public Map<String, List<GKInstance>> getReferrersMap(GKInstance instance) throws Exception {
        SchemaClass cls = instance.getSchemClass();
        Set referrerClasses = new HashSet();
        java.util.List top = new ArrayList(1);
        for (Iterator it = cls.getReferers().iterator(); it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute)it.next();
            GKSchemaClass origin = (GKSchemaClass) att.getOrigin();
            top.clear();
            top.add(origin);
            java.util.List classes = InstanceUtilities.getAllSchemaClasses(top);
            referrerClasses.addAll(classes);
        }
        Map<String, Set<GKInstance>> map = new HashMap<String, Set<GKInstance>>();
        for (Iterator<?> it = referrerClasses.iterator(); it.hasNext();) {
            SchemaClass tmpCls = (SchemaClass) it.next();
            List<GKInstance> instanceList = clsMap.get(tmpCls);
            if (instanceList != null && instanceList.size() > 0) {
                for (GKInstance tmpInstance : instanceList) {
                    for (Iterator<?> it2 = tmpInstance.getSchemaAttributes().iterator(); it2.hasNext();) {
                        SchemaAttribute att = (SchemaAttribute) it2.next();
                        if (!att.isInstanceTypeAttribute())
                            continue;
                        if (!att.isValidValue(instance))
                            continue;
                        java.util.List<?> values = tmpInstance.getAttributeValuesList(att);
                        if (values != null && values.size() > 0 && values.contains(instance)) {
                            Set<GKInstance> referrers = map.get(att.getName());
                            if (referrers == null) {
                                referrers = new HashSet<GKInstance>();
                                map.put(att.getName(), referrers);
                            }
                            referrers.add(tmpInstance);
                        }
                    }
                }
            }           
        }
        // Need to sort the referrers by converting to list
        Map<String, List<GKInstance>> rtn = new HashMap<String, List<GKInstance>>();
        for (Iterator<?> it = map.keySet().iterator(); it.hasNext();) {
            String attName = (String) it.next();
            Set<GKInstance> set = map.get(attName);
            List<GKInstance> list = new ArrayList<GKInstance>(set);
            InstanceUtilities.sortInstances(list);
            rtn.put(attName, list);
        }
        return rtn;
    }
    
    public java.util.List getReferers(GKInstance instance) throws Exception {
        Set set = new HashSet();
        SchemaClass cls = instance.getSchemClass();
        Set referrerClasses = new HashSet();
        java.util.List top = new ArrayList(1);
        for (Iterator it = cls.getReferers().iterator(); it.hasNext();) {
            GKSchemaAttribute att = (GKSchemaAttribute)it.next();
            GKSchemaClass origin = (GKSchemaClass) att.getOrigin();
            top.clear();
            top.add(origin);
            java.util.List classes = InstanceUtilities.getAllSchemaClasses(top);
            referrerClasses.addAll(classes);
        }
        for (Iterator it = referrerClasses.iterator(); it.hasNext();) {
            SchemaClass tmpCls = (SchemaClass) it.next();
            java.util.List instanceList = (java.util.List) clsMap.get(tmpCls);
            if (instanceList != null && instanceList.size() > 0) {
                for (Iterator it1 = instanceList.iterator(); it1.hasNext();) {
                    GKInstance tmpInstance = (GKInstance)it1.next();
                    for (Iterator it2 = tmpInstance.getSchemaAttributes().iterator(); it2.hasNext();) {
                        SchemaAttribute att = (SchemaAttribute) it2.next();
                        if (!att.isInstanceTypeAttribute())
                            continue;
                        if (!att.isValidValue(instance))
                            continue;
                        java.util.List values = tmpInstance.getAttributeValuesList(att);
                        if (values != null && values.size() > 0 & values.contains(instance))
                            set.add(tmpInstance);
                    }
                }
            }            
        }
        return new ArrayList(set);      
    }
    
    /* 
     * @see org.gk.model.PersistenceAdaptor#storeInstance(org.gk.model.GKInstance)
     */
    public Long storeInstance(GKInstance instance) throws Exception {
        throw new UnsupportedOperationException("XMLFileAdaptor.storeInstance(GKInstane) is not supported");
    }
    
    public void save(String destName) throws Exception {
		saveAsTemp(destName);
		// Fire the save action.
		isDirty = false;
		this.sourceName = destName;
		propertyChangeSupport.firePropertyChange("save", 
		                                         null, 
		                                         null);
	}
    
    /**
     * Use this method to save the contents only without make any changes in the
     * states of the current XMLFileAdaptor. For example, this method should be
     * used in auto-saving to another temp file to keep all GUIs correct.
     * @param destName
     * @throws Exception
     */
    public void saveAsTemp(String destName) throws Exception {
        FileOutputStream fos = new FileOutputStream(destName);
        saveAsTemp(fos);
//        String text = convertAsString();
//        // Use JDOM API
//        StringReader input = new StringReader(text);
//        SAXBuilder saxBuilder = new SAXBuilder();
//        org.jdom.Document document = saxBuilder.build(input);
//        org.jdom.Element reactomeElm = document.getRootElement();
//        document.removeContent(reactomeElm);
//        org.jdom.Document rtpjDoc = new org.jdom.Document();
//        org.jdom.Element rootElm = new org.jdom.Element("rtpj");
//        rtpjDoc.setRootElement(rootElm);
//        rootElm.addContent(reactomeElm);
//        saveDiagrams(rootElm);
//        XMLOutputter outputer = new XMLOutputter(Format.getPrettyFormat());
//        FileWriter writer = new FileWriter(destName);
//        outputer.output(rtpjDoc, writer);
    }
    
    public synchronized void saveAsTemp(OutputStream os) throws Exception {
        String text = convertAsString();
        // Use JDOM API
        StringReader input = new StringReader(text);
        SAXBuilder saxBuilder = new SAXBuilder();
        org.jdom.Document document = saxBuilder.build(input);
        org.jdom.Element reactomeElm = document.getRootElement();
        document.removeContent(reactomeElm);
        org.jdom.Document rtpjDoc = new org.jdom.Document();
        org.jdom.Element rootElm = new org.jdom.Element("rtpj");
        rtpjDoc.setRootElement(rootElm);
        rootElm.addContent(reactomeElm);
        saveDiagrams(rootElm);
        XMLOutputter outputer = new XMLOutputter(Format.getPrettyFormat());
        outputer.output(rtpjDoc, os);
    }
    
    /**
     * TODO: Move diagram related methods to another class!!!
     * @param root
     */
    private void saveDiagrams(org.jdom.Element root) {
        if (pdInstToDiagram == null || pdInstToDiagram.size() == 0)
            return;
        DiagramGKBWriter writer = new DiagramGKBWriter();
        for (RenderablePathway diagram : pdInstToDiagram.values()) {
            org.jdom.Element projectElm = writer.createRootElement(diagram);
            root.addContent(projectElm);
        }
    }

    private String convertAsString() throws Exception {
        StringBuffer buffer = new StringBuffer();
        buffer.append(XML_HEADER);
        buffer.append(LINE_END);
        buffer.append("<reactome>\n");
        GKSchemaClass cls = null;
        List instances = null;
        GKInstance instance = null;
        String indent = "    ";
        for (Iterator it = clsMap.keySet().iterator(); it.hasNext();) {
            cls = (GKSchemaClass) it.next();
            instances = (List) clsMap.get(cls);
            if (instances == null || instances.size() == 0)
                continue;
            buffer.append(indent);
            buffer.append("<");
            buffer.append(cls.getName());
            buffer.append(">\n");
            for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                instance = (GKInstance) it1.next();
                buffer.append(convertInstanceToString(instance, indent));
            }
            buffer.append(indent);
            buffer.append("</");
            buffer.append(cls.getName());
            buffer.append(">\n");
        }
        saveMeta(buffer, indent);
        buffer.append("</reactome>\n");
        return buffer.toString();
    }
    
    /**
	 * Save all changes.
	 * 
	 * @throws Exception
	 */
    public void save() throws Exception {
    	if (sourceName != null)
    		save(sourceName);
    	else
    		throw new IllegalStateException("XMLFileAdaptor.save(): No source name specified.");
    }
    
    private void saveMeta(StringBuffer buffer, String indent) {
        if ((deleteMap == null || deleteMap.size() == 0) && 
            (bookmarks == null || bookmarks.size() == 0) &&
            (defaultPersonId == null) &&
            (projectDescription == null))
            return;
        buffer.append(indent);
        buffer.append("<meta>\n");
        String indent1 = indent + indent;
        String indent2 = indent1 + indent;
        // Save deletion
        if (deleteMap != null && deleteMap.size() > 0) {
            buffer.append(indent1);
            buffer.append("<deletion>");
            buffer.append(LINE_END);
            for (Iterator it = deleteMap.keySet().iterator(); it.hasNext();) {
                Long dbID = (Long) it.next();
                String clsName = (String) deleteMap.get(dbID);
                buffer.append(indent2);
                buffer.append("<instance DB_ID=\"");
                buffer.append(dbID);
                buffer.append("\" class=\"");
                buffer.append(clsName);
                buffer.append("\" />");
                buffer.append(LINE_END);
            }
            buffer.append(indent1);
            buffer.append("</deletion>\n");
        }
        // Save bookmarks
        if (bookmarks != null && bookmarks.size() > 0) {
            buffer.append(indent1);
            if (bookmarks.getSortingKey() == null)
                buffer.append("<bookmarks>\n");
            else {
                buffer.append("<bookmarks sortingKey=\"");
                buffer.append(bookmarks.getSortingKey());
                buffer.append("\">\n");
            }
            for (Iterator it = bookmarks.getBookmarks().iterator(); it.hasNext();) {
                Bookmark bookmark = (Bookmark) it.next();
                buffer.append(indent2);
                buffer.append("<bookmark DB_ID=\"");
                buffer.append(bookmark.getDbID());
                buffer.append("\" displayName=\"");
                buffer.append(validateXMLText(bookmark.getDisplayName()));
                buffer.append("\" type=\"");
                buffer.append(bookmark.getType());
                buffer.append("\" />\n");
            }
            buffer.append(indent1);
            buffer.append("</bookmarks>\n");
        }
        if (defaultPersonId != null) {
            buffer.append(indent1).append("<defaultPerson dbId=\"");
            buffer.append(defaultPersonId).append("\" />\n");
        }
        if (projectDescription != null) {
            buffer.append(indent1).append("<description>");
            String tmp = validateXMLText(projectDescription);
            buffer.append(tmp).append("</description>\n");
        }
        buffer.append(indent);
        buffer.append("</meta>\n");
    }
    
    /**
     * Check if there is any unsaved changes.
     * @return
     */
    public boolean isDirty() {
        return this.isDirty;
    }
    
    /**
     * Save a collection of instances from the database to the local file system. 
     * @param instanceMap Keys: SchemaClass Values: a list of GKInstances that belong to
     * the SchemaClass in key.
     * @throws Exception
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void store(Map<SchemaClass, Set<GKInstance>> instanceMap) throws Exception {
        // Make sure all used db SchemaClasses existing in the local schema.
        for (Iterator it = instanceMap.keySet().iterator(); it.hasNext();) {
            GKSchemaClass dbCls = (GKSchemaClass) it.next();
            GKSchemaClass localCls = (GKSchemaClass) schema.getClassByName(dbCls.getName());
            if (localCls == null) {
                throw new IllegalStateException("XMLFileAdaptor.store(): The local schema is not compatible to the DB schema (" +
                                                dbCls.getName() + " cannot be found in the local schema.)");
            }
        }
        // Generate new Maps
        java.util.List newInstances = new ArrayList(); // For update views
        java.util.List updatedInstances = new ArrayList(); // For update views
        List dbList = new ArrayList();
        List newList = new ArrayList();
        for (Iterator it = instanceMap.keySet().iterator(); it.hasNext();) {
            // Note: This class is from the database
            GKSchemaClass schemaClass = (GKSchemaClass) it.next(); 
            Collection<GKInstance> list = instanceMap.get(schemaClass);
            if (list == null || list.size() == 0)
                continue;
            for (Iterator it1 = list.iterator(); it1.hasNext();) {
                GKInstance dbInstance = (GKInstance) it1.next();
                dbList.add(dbInstance);
                // Check if a local instance exist
                if (cache.containsKey(dbInstance.getDBID()))
                    continue;
                // Need to create a local copy
                String clsName = dbInstance.getSchemClass().getName();
                GKInstance localInstance = createInstance(dbInstance.getDBID(),
                                                          clsName);
                // As a mark so that its attributes can be copied in the following loop.
                localInstance.setIsShell(true);
                cache.put(localInstance);
                addToClasssMap(localInstance);
                newList.add(localInstance);
            }
        }
        // Have to handle attribute values
        // Defer PathwayDiagram since _displayNames in diagram depends on
        // GKInstance.
        List<GKInstance> pdInstances = new ArrayList<GKInstance>();
        for (Iterator it = dbList.iterator(); it.hasNext();) {
            GKInstance dbInstance = (GKInstance) it.next();
            if (dbInstance.getSchemClass().isa(ReactomeJavaConstants.PathwayDiagram)) {
                pdInstances.add(dbInstance);
                continue;
            }
            GKInstance localInstance = cache.get(dbInstance.getDBID());
            copyAttributesFromDBToLocal(dbInstance, localInstance);
        }
        for (GKInstance pdInstance : pdInstances) {
            GKInstance localInst = cache.get(pdInstance.getDBID());
            copyAttributesFromDBToLocal(pdInstance, localInst);
        }
        isDirty = true;
        // Have to call update instances first
        propertyChangeSupport.firePropertyChange("addNewInstance", null, newList);
    }
    
    private void copyAttributesFromDBToLocal(GKInstance dbInstance,
                                             GKInstance localInstance) throws Exception {
        if (!localInstance.isShell() && dbInstance.isShell())
            return; // Don't overwrite the local instance
        // If there are any changes in the local instance, don't make any copy to avoid
        // overwriting any local changes
        if (!localInstance.isShell() && localInstance.isDirty())
            return;
        // The following cases should use copy: local is a shell but db not; no changes in local,
        // since db might be changed.
        // Need to copy DB_ID and displayName from dbInstance even though it is a shell instance.
        localInstance.setIsShell(dbInstance.isShell());
        // Only DB_ID and displayName is needed for shell instance
        if (dbInstance.isShell()) {
            localInstance.setDBID(dbInstance.getDBID());
            localInstance.setDisplayName(dbInstance.getDisplayName());
            return;
        }
        GKSchemaAttribute dbAtt = null;
        GKSchemaAttribute localAtt = null;
        for (Iterator it = dbInstance.getSchemaAttributes().iterator(); it.hasNext();) {
            dbAtt = (GKSchemaAttribute) it.next();
            java.util.List dbList = dbInstance.getAttributeValuesList(dbAtt);
            if (dbList == null || dbList.size() == 0)
                continue;
            // This is a special case
            if (localInstance instanceof PathwayDiagramInstance &&
                dbAtt.getName().equals(ReactomeJavaConstants.storedATXML)) {
                String xml = (String) dbList.get(0);
                copyStoredATXMLFromDBToLocal(localInstance,
                                             xml);
                continue;
            }
            java.util.List localList = null;
            if (dbAtt.isInstanceTypeAttribute()) {
                // Have to convert db instance to local instance
                localList = new ArrayList(dbList.size());
                for (Iterator it1 = dbList.iterator(); it1.hasNext();) {
                    GKInstance dbTmp = (GKInstance) it1.next();
                    GKInstance localTmp = cache.get(dbTmp.getDBID());
                    localList.add(localTmp);
                }
            }
            else {
                // Just copy
                localList = new ArrayList(dbList);
            }
            String attName = getLocalAttName(dbAtt);
            localInstance.setAttributeValueNoCheck(attName, localList);
        }
    }
    
    public void copyStoredATXMLFromDBToLocal(GKInstance localPd,
                                             String xml) throws Exception {
        Reader sReader = new StringReader(xml);
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document document = builder.build(sReader);
        org.jdom.Element root = document.getRootElement();
        DiagramGKBReader reader = new DiagramGKBReader();
        reader.setPersistenceAdaptor(this);
        loadDiagram(reader, root);
    }
    
    /**
     * This helper method is used to convert attribute name from Pathway.hasComponent
     * to Pathway.hasEvent. Pathway.hasComponent has been changed to Pathway.hasEvent. 
     * However, as of March 23, 2007, databases used to hold pathways converted from
     * other databases are using hasComponent still. To avoid making schema changes to
     * these databases, which is complicated, I made the following checking.
     * @param dbAtt
     * @return
     */
    private String getLocalAttName(SchemaAttribute dbAtt) {
        SchemaClass cls = dbAtt.getOrigin();
        if (cls.getName().equals(ReactomeJavaConstants.Pathway) &&
            dbAtt.getName().equals(ReactomeJavaConstants.hasComponent))
            return ReactomeJavaConstants.hasEvent;
        return dbAtt.getName();
    }
    
    private String convertInstanceToString(GKInstance instance, 
                                           String indent) throws Exception {
        StringBuffer buffer = new StringBuffer();
        SchemaClass schemaClass = instance.getSchemClass();
        buffer.append(indent + indent);
        buffer.append("<instance ");
        buffer.append("DB_ID=\"" + instance.getDBID() + "\" ");
        buffer.append("displayName=\"" + validateXMLText(instance.getDisplayName()) + "\"");
        if (instance.isShell()) {
            buffer.append(" isShell=\"true\">");
            buffer.append(LINE_END);
        }
        else {
            buffer.append(" isShell=\"false\"");
            buffer.append(" isDirty=\"");
            buffer.append(instance.isDirty());
            buffer.append("\">\n");
            String indent1 = indent + indent + indent;
            // Put valid attributes in
            for (Iterator i1 = schemaClass.getAttributes().iterator(); i1.hasNext();) {
                SchemaAttribute att = (SchemaAttribute)i1.next();
                // Escape the storing in this case
                if ((instance instanceof PathwayDiagramInstance) &&
                    (att.getName().equals(ReactomeJavaConstants.width) ||
                     att.getName().equals(ReactomeJavaConstants.height) ||
                     att.getName().equals(ReactomeJavaConstants.storedATXML)))
                     continue;
                if (instance.isAttributeValueLoaded(att)) {
                    Collection c = instance.getAttributeValuesList(att);
                    if (c != null && c.size() > 0) {
                        if (att.isInstanceTypeAttribute()) {
                            for (Iterator it2 = c.iterator(); it2.hasNext();) {
                                GKInstance tmp = (GKInstance) it2.next();
                                if (tmp == null)
                                    continue; // Just in case
                                buffer.append(indent1);
                                buffer.append("<attribute ");
                                buffer.append("name=\"" + att.getName() + "\" ");
                                buffer.append("class=\"" + tmp.getSchemClass().getName() + "\" ");
                                buffer.append("referTo=\"" + tmp.getDBID() + "\" />");
                                buffer.append(LINE_END);
                            }
                        }
                        else {
                            for (Iterator i2 = c.iterator(); i2.hasNext();) {
                                Object value = i2.next();
                                buffer.append(indent1);
                                buffer.append("<attribute ");
                                buffer.append("name=\"" + att.getName() + "\" ");
                                buffer.append("value=\"" + validateXMLText(value.toString()) + "\" />");
                                buffer.append(LINE_END);
                            }
                        }
                    }
                }
            }
        }
        buffer.append(indent + indent);
        buffer.append("</instance>");
        buffer.append(LINE_END);
        return buffer.toString();
    }
    
    /**
     * A helper to escape the angle bracket and ampersand characters for xml text.
     */
    private String validateXMLText(String text) {
        StringBuffer buffer = new StringBuffer();
        if (text == null || text.length() == 0)
            return buffer.toString();
        char[] chars = text.toCharArray();
        char c;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];
            if (c == AMPERSAND)
                buffer.append(AMPERSAND_ESCAPE);
            else if (c == BRACKET)
                buffer.append(BRACKET_ESCAPE);
            else if (c == RIGHT_BRACKET)
                buffer.append(RIGHT_BRACKET_ESCAPE);
            else if (c == QUOTATION)
                buffer.append(QUOTATION_ESCAPE);
            else if (c == LINE_END)
                buffer.append(LINE_END_ESCAPE);
            else 
                buffer.append(c);
        }
        return buffer.toString();
    }
    
    /**
     * Update all data structures because of the DB_ID change.
     * @param instance whose DB_ID has been changed to new value.
     */
    public void dbIDUpdated(Long oldDBID, GKInstance instance) {
        // Update cache
        cache.remove(oldDBID);
        cache.put(instance);
        isDirty = true; // The file is dirty now
        updateDBIDsInDiagrams(oldDBID, instance);
    }
    
    /**
     * Update Renderable DBIDs in diagrams.
     * @param oldDbId
     * @param instance
     */
    private void updateDBIDsInDiagrams(Long oldDbId, 
                                       GKInstance instance) {
        if (pdInstToDiagram == null || pdInstToDiagram.size() == 0)
            return;
        for (RenderablePathway diagram : pdInstToDiagram.values()) {
            List<?> objects = diagram.getComponents();
            if (objects == null || objects.size() == 0)
                continue;
            for (Iterator<?> it = objects.iterator(); it.hasNext();) {
                Renderable r = (Renderable) it.next();
                if (r.getReactomeId() == null)
                    continue;
                if (r.getReactomeId().equals(oldDbId))
                    r.setReactomeId(instance.getDBID());
            }
        }
    }
    
    /* 
     * @see org.gk.model.PersistenceAdaptor#updateInstanceAttribute(org.gk.model.GKInstance, java.lang.String)
     */
    public void updateInstanceAttribute(GKInstance instance, String attributeName) throws Exception {
    }
    
    /**
     * The returned Collection is not sorted.
     */
    public Collection fetchInstancesByClass(SchemaClass class1) throws Exception {
        List clsList = new ArrayList();
        InstanceUtilities.getDescendentClasses(clsList, (GKSchemaClass)class1);
        List rtn = new ArrayList();
        for (Iterator it = clsList.iterator(); it.hasNext();) {
        	List list = (List) clsMap.get(it.next());
        	if (list == null || list.size() == 0)
        		continue;
        	rtn.addAll(list);
        }
        if (clsList.size() == 1) 
        	return rtn; // No need to sort
        //Sort is disabled since it is expensive.
        //InstanceUtilities.sortInstances(rtn);
        return rtn;
    }
    
    /**
     * An overloaded method. Fetch the instances in the specifed SchemaClass object. The descendent classes will 
     * not be considered. The returned Collection is not sorted.
     * @param cls
     * @param needDescdent
     * @return
     * @throws Exception
     */
    public Collection fetchInstancesByClass(SchemaClass cls, boolean needDescdent) throws Exception {
        if (needDescdent)
            return fetchInstancesByClass(cls);
        else
            return (List) clsMap.get(cls);
    }
    
    /**
     * The returned Collection is not sorted.
     * @param className
     * @return
     * @throws Exception
     */
    public Collection fetchInstancesByClass(String className) throws Exception {
        SchemaClass cls = schema.getClassByName(className);
        return fetchInstancesByClass(cls);
    }
    
    public SchemaClass fetchSchemaClass(String className) {
        return schema.getClassByName(className);
    }
    
   
    public void addInstanceListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    public void removeInstanceListener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
}
