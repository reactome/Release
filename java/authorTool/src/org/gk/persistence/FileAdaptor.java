/*
 * Created on Nov 13, 2003
 */
package org.gk.persistence;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.gk.model.Bookmark;
import org.gk.model.GKInstance;
import org.gk.model.Instance;
import org.gk.model.InstanceCache;
import org.gk.model.InstanceUtilities;
import org.gk.model.PersistenceAdaptor;
import org.gk.schema.GKSchema;
import org.gk.schema.GKSchemaAttribute;
import org.gk.schema.GKSchemaClass;
import org.gk.schema.Schema;
import org.gk.schema.SchemaAttribute;
import org.gk.schema.SchemaClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * An implementation of PersistenceAdaptor for the local file system.
 * @author wugm
 */
public class FileAdaptor implements PersistenceAdaptor {
	public static final char LINE_END = '\n';
	private static final String PROJECT_FILE = "project.xml";
	private static final String DELETION_FILE = "deletion";
	private static final String BOOKMARK_FILE = "bookmarks.xml";
	private static final String INDEX_FILE_NAME = "index";
	private static final String CHARSET_NAME = "UTF-8"; // The default charset
	private static final String LAST_DB_ID_FILE = "dbID";
	private static final int BUFFER_SIZE = 1024; // 1 k
	private static final String BACKUP_EXT_NAME = ".bak";
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
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"" + CHARSET_NAME + "\" ?>"; 
	// The file name
	private String dir;
	// Cache the schama
	private GKSchema schema;
	// Cache the indexMap. An indexMap is a Map of Map: the first level map
	// Key SchemaClass Name Value Map; The second level map: Key instance DB_ID 
	// value: The position of instance in the file.
	// Note: the key in this map is the name of the SchemaClass for persistence.
	private Map indexMap;
	// Cache the decoder
	private CharsetDecoder decoder;
	// Cache the ByteBuffer: Key for SchemaClass Name, Value for ByteBuffer for files.
	private Map clsBBMap;
	// Cache the fetched instances to make sure there is only one copy for each instance
	private InstanceCache cache;
	// A list of all Instances that are changed or newly created
	private Map dirtyMap;
	// A list of newly created instances
	private Map newInstanceMap;
	// A list of deleted instances: Key: DB_ID, values: className
	private Map deleteMap;
	// For update views
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	// For local DB_ID. The initial value should get from the local saved file.
	private long nextID = -1;
	private boolean doDelete = false;
	
	public FileAdaptor(String dir) throws Exception {
		this.dir = dir;
		// Check if dir exists. If not, create one
		File file = new File(dir);
		if (!file.exists())
			file.mkdir();
		fetchSchema();
		loadIndexMap();
		decoder = Charset.forName(CHARSET_NAME).newDecoder();
		if (indexMap == null) {
			indexMap = generateIndexMap();
		    saveIndexMap();
		}
		clsBBMap = new HashMap();
		cache = new InstanceCache();
		dirtyMap = new HashMap();
		newInstanceMap = new HashMap();
		loadLastDBID();
		loadDeleteMap();
		if (deleteMap == null)
			deleteMap = new HashMap(); // In case it is null
	}
	
    /**
     * Generate a new index map from the local repository. This is a safe-guide
     * method in case the index map is out of synchronization of the files. 
     * @return
     */
    public Map generateIndexMap() throws IOException {
        HashMap map = new HashMap();
        GKSchemaClass cls;
        for (Iterator it = schema.getClasses().iterator(); it.hasNext();) {
            cls = (GKSchemaClass) it.next();
            String fileName = getFileName(cls.getName());
            File file = new File(fileName);
            if (!file.exists())
                continue;
            Map instanceMap = generateIndexMap(file);
            map.put(cls.getName(), instanceMap);
        }
        return map;
    }
    
    private Map generateIndexMap(File clsFile) throws IOException {
        FileInputStream fis = new FileInputStream(clsFile);
        //FileOutputStream fos = new FileOutputStream(clsFile.getAbsolutePath() + ".new");
        InputStreamReader reader = new InputStreamReader(fis, CHARSET_NAME);
        BufferedReader br = new BufferedReader(reader);
        String line = null;
        StringBuffer buffer = new StringBuffer();
        Map instanceMap = new HashMap();
        String firstLine = null;
        int offset = 0;
        boolean isAfterBreak = true;
        boolean isAfterEnd = false;
        boolean isInInstance = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("<instance")) {
                if (isInInstance || !isAfterBreak) {
                    break; // Bad element, possible left over from deleting
                }
                isInInstance = true;
                isAfterBreak = false;
                isAfterEnd = false;
                firstLine = line;
                buffer.append(line);
                buffer.append(LINE_END);
                //buffer = new StringBuffer();
            }
            else if (line.startsWith("</instance>")) {
                if (!isInInstance) {
                    break; // Bad element, possible left over from deleting
                }
                buffer.append(line);
                buffer.append(LINE_END);
                isAfterEnd = true;
                isAfterBreak = false;
            }
            else if (line.length() == 0) { // Instance ending
                if (!isInInstance || !isAfterEnd)
                    break;
                isInInstance = false;
                isAfterBreak = true;
                buffer.append(LINE_END);
                if (clsFile.getName().equals("ConcreteReaction"))
                    System.out.print(buffer.toString());
                byte[] bytes = buffer.toString().getBytes(CHARSET_NAME);
                String test = new String(bytes, CHARSET_NAME);
                if (clsFile.getName().equals("ConcreteReaction"))
                    System.out.print(test);
                IndexInfo indexInfo = new IndexInfo();
                indexInfo.pos = offset;
                indexInfo.length = bytes.length;
                instanceMap.put(getDBID(firstLine), indexInfo);
                offset += bytes.length;
                buffer.setLength(0);
                //fos.write(bytes);
                //fos.flush();
            }
            else {
                if (!isInInstance)
                    break;
                buffer.append(line);
                buffer.append(LINE_END);
                isAfterBreak = false;
            }
        }
        br.close();
        reader.close();
        fis.close();
        //fos.close();
        // Remove the original one and use the new one
        //clsFile.delete();
        //File newFile = new File(clsFile.getAbsolutePath() + ".new");
        //newFile.renameTo(clsFile);
        return instanceMap;
    }
    
    private Long getDBID(String line) {
        int index = line.indexOf("DB_ID=");
        int index1 = line.indexOf("\"", index);
        int index2 = line.indexOf("\"", index1 + 1);
        return new Long(line.substring(index1 + 1, index2));
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
		cache.clear();
	}
	
	public String getPath() {
		return this.dir;
	}
	
	private void loadLastDBID() {
		try {
			File file = new File(getFileName(LAST_DB_ID_FILE));
			if (file.exists()) {
				FileReader fis = new FileReader(file);
				BufferedReader reader = new BufferedReader(fis);
				String line = reader.readLine();
				nextID = Long.parseLong(line);
				reader.close();
				fis.close();
			}
		}
		catch(IOException e) {
			System.err.println("FileAdaptor.loadLastDBID(): " + e);
			e.printStackTrace();
		}
	}
	
	private void saveLastDBID() {
		try {
			FileWriter writer = new FileWriter(getFileName(LAST_DB_ID_FILE));
			PrintWriter printWriter = new PrintWriter(writer);
			printWriter.println(nextID);
			printWriter.flush();
			printWriter.close();
			writer.close();
		}
		catch(IOException e) {
			System.err.println("FileAdaptor.saveLastDBID(): " + e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Mark an Instance as dirty so that it can be saved. If the specified
	 * instance is a new instance, it will not be marked.
	 * @param instance
	 */
	public void markAsDirty(Instance instance) {
		// Check if instance is a new GKInstance object. It will occur
	    // if a GKInstance is editing in the new instanceo dialog.
	    SchemaClass cls = instance.getSchemClass();
	    try {
	        if (fetchInstance(cls.getName(), instance.getDBID()) == null)
	            return;
	    }
	    catch(Exception e) {
	        System.err.println("FileAdaptor.markAsDirty(): " + e);
	        e.printStackTrace();
	    }
	    // Check if instance is a new object
		//java.util.List newInstances = (java.util.List) newInstanceMap.get(cls);
		//if (newInstances != null && newInstances.contains(instance))
		//	return;
		java.util.List list = (java.util.List) dirtyMap.get(cls);
		if (list == null) {
			list = new ArrayList();
			dirtyMap.put(cls, list);
		}
		if (!list.contains(instance))
			list.add(instance);
		propertyChangeSupport.firePropertyChange("markAsDirty", null, instance);
	}
	
	/**
	 * Create a new GKInstance located in the local repository.
	 * @param clsName
	 * @return
	 */
	public GKInstance createNewInstance(String clsName) {
	    GKInstance instance = new GKInstance();
	    instance.setDbAdaptor(this);
	    // Call the method to get nextLocalID otherwise
	    // nextID should be decreased in this method.
	    instance.setDBID(getNextLocalID());
	    instance.setSchemaClass(schema.getClassByName(clsName));
	    instance.setIsInflated(true); // To block searching
	    addNewInstance(instance);
	    return instance;
	}
	
	/**
	 * Regirster a newly created Instance.
	 * @param newInstance
	 */
	public void addNewInstance(Instance newInstance) {
		cache.put(newInstance);
		SchemaClass cls = newInstance.getSchemClass();
		java.util.List list = (java.util.List) newInstanceMap.get(cls);
		if (list == null) {
			list = new ArrayList();
			newInstanceMap.put(cls, list);
		}
		list.add(newInstance);
		java.util.List newValues = new ArrayList();
		newValues.add(newInstance);
		// Remove from deleteMap
		if (deleteMap.containsKey(newInstance.getDBID()))
		    deleteMap.remove(newInstance.getDBID());
		propertyChangeSupport.firePropertyChange("addNewInstance", null, newValues);
	}
	
	/**
	 * Switch the class type for a specified GKInstance object. Three things are processes here:
	 * Make sure the old slot values are valid in the new schemaclass context, mimic the deleting 
	 * operation for the instance, mimic the adding operation for the instance. Mimicing is used
	 * to make data structure correct.
	 * @param instance
	 * @param newCls
	 */
	public void switchType(GKInstance instance, GKSchemaClass newCls) {
		GKSchemaClass oldCls = (GKSchemaClass) instance.getSchemClass();
	    try {
	        //propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
	        // backuping values
            Map values = new HashMap();
            Long dbID = instance.getDBID();
            for (Iterator it = instance.getSchemaAttributes().iterator(); it.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) it.next();
                java.util.List list = instance.getAttributeValuesList(att);
                if (list != null && list.size() > 0) {
                    values.put(att.getName(), new ArrayList(list));
                    list.clear();
                }
            }
            instance.setSchemaClass(newCls);
            for (Iterator it = newCls.getAttributes().iterator(); it.hasNext();) {
                GKSchemaAttribute att = (GKSchemaAttribute) it.next();
                java.util.List list = (java.util.List) values.get(att.getName());
                if (list == null)
                    continue;
                for (Iterator it1 = list.iterator(); it1.hasNext();) {
                    Object obj = it1.next();
                    if (att.isValidValue(obj))
                        instance.addAttributeValue(att, obj);
                }
            }
            instance.setDBID(dbID); // Have to reset DB_ID since it might get lost.
            instance.setIsInflated(true);
            // Remove it from the old list related data structure
            // Check if it is a new instance
            java.util.List list = (java.util.List) newInstanceMap.get(oldCls);
            if (list != null) {
                int index = list.indexOf(instance);
                if (index > -1) {
                    list.remove(index);
                    //propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
                }
            }
            else {
                // It is from the saved repository
                // Remove it from indexMap first
                Map map = (Map) indexMap.get(oldCls.getName());
                map.remove(instance.getDBID());
                // It will be listed under different category
                //if (instance.getDBID().longValue() > -1)
                //    deleteMap.put(instance.getDBID(), oldCls.getName());
                //doDelete = true;
            }
            // Add it back to new instance related data structures
            list = (java.util.List) newInstanceMap.get(newCls);
            if (list == null) {
                list = new ArrayList();
                newInstanceMap.put(newCls, list);
            }
            list.add(instance);
            //java.util.List newValues = new ArrayList();
            //newValues.add(instance);
            //propertyChangeSupport.firePropertyChange("addNewInstance", null, newValues);
            propertyChangeSupport.firePropertyChange("switchType", oldCls, instance);
			// Use a fast way
			java.util.List referrers = getReferers(instance);
			for (Iterator it = referrers.iterator(); it.hasNext();) {
				GKInstance refer = (GKInstance) it.next();
				markAsDirty(refer);
			}
	    }
        catch (Exception e) {
            System.err.println("FileAdaptor.switchType(): " + e);
            e.printStackTrace();
        }
	}
	
	public Long getNextLocalID() {
		// There should be no chance nextID hit the Long.MAX.
	    Long rtn = new Long(nextID);
	    nextID --;
	    return rtn;
	}
	
	/**
	 * Delete a specified from the repository.
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
		}
		catch (Exception e) {
			System.err.println("FileAdaptor.deleteInstance(): " + e);
			e.printStackTrace();
		}
		// Check if it is a new instance
		java.util.List list = (java.util.List) newInstanceMap.get(schemaClass);
		if (list != null) {
			int index = list.indexOf(instance);
			if (index > -1) {
				list.remove(index);
				propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
				return;
			}
		}
		// It is from the saved repository
		// Remove it from indexMap first
		Map map = (Map) indexMap.get(schemaClass.getName());
		map.remove(instance.getDBID());
		// Remove it from cache
		cache.remove(instance.getDBID());
		if (instance.getDBID() != null) {
            if (instance.getDBID().longValue() > -1)
                deleteMap.put(instance.getDBID(), instance.getSchemClass().getName());
        }
		doDelete = true;
		propertyChangeSupport.firePropertyChange("deleteInstance", null, instance);
	}
	
	public Collection fetchInstanceByAttribute(String className, String attName, String operator, Object value)
		throws Exception {
		operator = operator.toUpperCase();
		// Need to check operator
		if (!(operator.equals("=") || operator.equals("LIKE") || operator.equals("REGEXP") ||
		      operator.equals("IS NOT NULL") || operator.equals("IS NULL"))) {
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
		// A special case
		if (attName.equals("DB_ID") && operator.equals("=")) {
			GKInstance instance = fetchInstance(className, new Long(value.toString()));
			if (instance != null) {
				rtnList.add(instance);
			}
			return rtnList;
		}
		SchemaClass schemaClass = schema.getClassByName(className);
		// Check newly created Instance objects
		java.util.List newInstances = (java.util.List) newInstanceMap.get(schemaClass);
		if (newInstances != null && newInstances.size() > 0) {
			for (Iterator it = newInstances.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				if (checkInstance(instance, attName, operator, value))
					rtnList.add(instance);
			}
		}
		// Have to check modified instance objects first
		Map checkedInstances = new HashMap();
		java.util.List modifiedInstances = (java.util.List) dirtyMap.get(schemaClass);
		if (modifiedInstances != null && modifiedInstances.size() > 0) {
			for (Iterator it = modifiedInstances.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				checkedInstances.put(instance.getDBID(), instance);
				if (checkInstance(instance, attName, operator, value))
					rtnList.add(instance);
			}
		}
		// Get the index info.
		Map posMap = (Map)indexMap.get(className);
		if (posMap == null || posMap.size() == 0)
			return rtnList;
		// To optimize the regexp
		Pattern pattern = null;
		if (operator.equals("REGEXP"))
			pattern = Pattern.compile(value.toString());
		boolean isInstanceType = schemaClass.getAttribute(attName).isInstanceTypeAttribute();
		String targetValue = null;
		GKInstance targetInstance = null;
		if (isInstanceType) {
			targetValue = ((GKInstance)value).getDisplayName();
			targetInstance = (GKInstance)value;
		}
		else
			targetValue = value.toString();
		ByteBuffer bb = getByteBuffer(className);
		// Another special case for _displayName. Don't need go to attributes nodes.
		if (attName.equals("_displayName")) {
			String line;
			String name;
			boolean isFound = false;
			for (Iterator it = posMap.keySet().iterator(); it.hasNext();) {
				Long dbID = (Long) it.next();
				isFound = false;
				if (checkedInstances.containsKey(dbID))
					continue;
				GKInstance instance = null;
				IndexInfo info = (IndexInfo) posMap.get(dbID);
				bb.limit(info.pos + info.length);
				bb.position(info.pos);
				line = readLine(bb); // Escape the first line.
				name = parseName(line);
				if (name == null)
					name = "";
				if (operator.equals("is NULL")) {
					if (name.length() == 0)
						isFound = true;
				}
				else if (operator.equals("IS NOT NULL")) {
					if (name.length() > 0)
						isFound = true;
				}
				else if (operator.equals("=")) {
					if (name.equals(targetValue))
						isFound = true;
				}
				else if (operator.equals("LIKE")) {
					if (name.indexOf(targetValue) >= 0)
						isFound = true;
				}
				else if (operator.equals("REGEXP")) {
					Matcher matcher = pattern.matcher(name);
					if (matcher.find()) {
						isFound = true;
					}
				}
				if (isFound) {
					instance = cache.get(dbID);
					if (instance == null) {
						instance = new GKInstance();
						instance.setIsShell(parseIsShell(line));
						instance.setDisplayName(name);
						instance.setDbAdaptor(this);
						instance.setDBID(dbID);
						instance.setSchemaClass(schemaClass);
						cache.put(instance);
					}
					rtnList.add(instance);
				}
			}
			return rtnList;
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		for (Iterator it = posMap.keySet().iterator(); it.hasNext();) {
			Long dbID = (Long) it.next();
			if (checkedInstances.containsKey(dbID))
				continue;
			GKInstance instance = null;
			IndexInfo info = (IndexInfo) posMap.get(dbID);
			bb.limit(info.pos + info.length);
			bb.position(info.pos);
			CharBuffer cb = decoder.decode(bb);
			String xmlStr = XML_HEADER + LINE_END + cb.toString();
			StringReader reader = new StringReader(xmlStr);
			Document document = builder.parse(new InputSource(reader));
			Element root = document.getDocumentElement();
			// Check if it is a shell instance
			String isShell = root.getAttribute("isShell");
			if (isShell.equals("true")) {
				continue; // Nothing in the instance. Cannot be compared.
			}
			NodeList attList = root.getElementsByTagName("attribute");
			int size = attList.getLength();
			boolean isTouched = false;
			for (int i = 0; i < size; i++) {
				Element elm = (Element)attList.item(i);
				String name = elm.getAttribute("name");
				if (!attName.equals(name))
					continue;
				isTouched = true;
				if (!isInstanceType) {
					String valueStr = elm.getAttribute("value");
					if (valueStr.length() > 0) { // This is non-instance type attribute
						if (operator.equals("=")) {
							if (valueStr.equals(targetValue)) {
								instance = extractInstance(root, className);
								rtnList.add(instance);
								break;
							}
						}
						else if (operator.equals("IS NOT NULL")) {
							instance = extractInstance(root, className);
							rtnList.add(instance);
							break;
						}
						else if (operator.equals("LIKE")) {
							if (valueStr.indexOf(targetValue) >= 0) {
								instance = extractInstance(root, className);
								rtnList.add(instance);
								break;
							}
						}
						else if (operator.equals("REGEXP")) {
							Matcher matcher = pattern.matcher(valueStr);
							if (matcher.find()) {
								instance = extractInstance(root, className);
								rtnList.add(instance);
								break;
							}
						}
					}
				}
				else {
					// Now check if it is an Instance
					String clsName = elm.getAttribute("class");
					// It will be very slow for this type of searching.
					String refIDStr = elm.getAttribute("referTo");
					if (clsName.length() == 0 || refIDStr.length() == 0)
						continue;
					Long refID = new Long(refIDStr);
					if (operator.equals("IS NOT NULL")) {
						instance = extractInstance(root, className);
						rtnList.add(instance);
						break;
					}
					else if (operator.equals("=")) {
						if (targetInstance.getDBID().equals(refID)) {
							instance = extractInstance(root, className);
							rtnList.add(instance);
							break;
						}
					}
					else if (operator.equals("LIKE")) {
						GKInstance refInstance = fetchInstance(clsName, refID);
						String displayName = refInstance.getDisplayName();
						if (displayName.indexOf(targetValue) >= 0) {
							instance = extractInstance(root, className);
							rtnList.add(instance);
							break;
						}
					}
					// REGEXP is not supported for GKInstance type.
				}
			}
			if (!isTouched && operator.equals("IS NULL")) {
				instance = extractInstance(root, className);
				rtnList.add(instance);
			}
		}
		return rtnList;
	}	
	
	/**
	 * A fast implementation for search GKInstance objects based on attribute values.
	 * @param clsName
	 * @param attName
	 * @param operator
	 * @param value
	 * @return
	 * @throws Exception
	 * @see fetchInstanceByAttribute(String, String, String, Object).
	 */
	public Collection search(String className, String attName, String operator, String value) throws Exception {
		operator = operator.toUpperCase();
		// Need to check operator
		if (!(operator.equals("=") || operator.equals("LIKE") || operator.equals("REGEXP") ||
			  operator.equals("IS NOT NULL") || operator.equals("IS NULL"))) {
			throw new IllegalArgumentException("FileAdator.fetchInstanceByAttribute(): Unsupported operator: " + operator);
		}
		// Have to get rid of % for like
		if (operator.equals("LIKE")) {
			String tmp = value.toString();
			value = tmp.substring(1, tmp.length() - 1);
		}
		java.util.List rtnList = new ArrayList();
		// A special case
		if (attName.equals("DB_ID") && operator.equals("=")) {
			GKInstance instance = fetchInstance(className, new Long(value.toString()));
			if (instance != null) {
				rtnList.add(instance);
			}
			return rtnList;
		}
		SchemaClass schemaClass = schema.getClassByName(className);
		// Check newly created Instance objects
		java.util.List newInstances = (java.util.List) newInstanceMap.get(schemaClass);
		if (newInstances != null && newInstances.size() > 0) {
			for (Iterator it = newInstances.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				if (checkInstanceForSearch(instance, attName, operator, value))
					rtnList.add(instance);
			}
		}
		// Have to check modified instance objects first
		Map checkedInstances = new HashMap();
		java.util.List modifiedInstances = (java.util.List) dirtyMap.get(schemaClass);
		if (modifiedInstances != null && modifiedInstances.size() > 0) {
			for (Iterator it = modifiedInstances.iterator(); it.hasNext();) {
				GKInstance instance = (GKInstance) it.next();
				checkedInstances.put(instance.getDBID(), instance);
				if (checkInstanceForSearch(instance, attName, operator, value))
					rtnList.add(instance);
			}
		}
		// Get the index info.
		Map posMap = (Map)indexMap.get(className);
		if (posMap == null || posMap.size() == 0)
			return rtnList;
		// To optimize the regexp
		Pattern pattern = null;
		if (operator.equals("REGEXP"))
			pattern = Pattern.compile(value.toString());
		boolean isInstanceType = schemaClass.getAttribute(attName).isInstanceTypeAttribute();
		boolean isTouched = false;
		GKInstance targetInstance = null;
		GKInstance matchedInstance = null;
		String valueStr = null;
		ByteBuffer bb = getByteBuffer(className);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		for (Iterator it = posMap.keySet().iterator(); it.hasNext();) {
			Long dbID = (Long) it.next();
			if (checkedInstances.containsKey(dbID))
				continue;
			IndexInfo info = (IndexInfo) posMap.get(dbID);
			bb.limit(info.pos + info.length);
			bb.position(info.pos);
			CharBuffer cb = decoder.decode(bb);
			String xmlStr = XML_HEADER + LINE_END + cb.toString();
			StringReader reader = new StringReader(xmlStr);
			Document document = builder.parse(new InputSource(reader));
			Element root = document.getDocumentElement();
			// Check if it is a shell instance
			String isShell = root.getAttribute("isShell");
			if (isShell.equals("true")) {
				continue; // Nothing in the instance. Cannot be compared.
			}
			NodeList attList = root.getElementsByTagName("attribute");
			int size = attList.getLength();
			isTouched = false;
			for (int i = 0; i < size; i++) {
				Element elm = (Element)attList.item(i);
				String name = elm.getAttribute("name");
				if (!attName.equals(name))
					continue;
				isTouched = true;
				if (isInstanceType) {
					String clsName = elm.getAttribute("class");
					// It will be very slow for this type of searching.
					String refIDStr = elm.getAttribute("referTo");
					if (clsName.length() == 0 || refIDStr.length() == 0) {
						System.err.println("Bad Data in " + className);
						continue; // bad data: something is wrong.
					}
					Long refID = new Long(refIDStr);
					targetInstance = fetchInstance(clsName, refID);
					valueStr = targetInstance.getDisplayName();
				}
				else
					valueStr = elm.getAttribute("value");
				if (valueStr.length() > 0) {
					boolean isMatched = false;
					if (operator.equals("=")) {
						if (valueStr.equals(value)) {
							isMatched = true;
						}
					}
					else if (operator.equals("IS NOT NULL")) {
						isMatched = true;
					}
					else if (operator.equals("LIKE")) {
						if (valueStr.indexOf(value) >= 0) {
							isMatched = true;
						}
					}
					else if (operator.equals("REGEXP")) {
						Matcher matcher = pattern.matcher(valueStr);
						if (matcher.find()) {
							isMatched = true;
						}
					}
					if (isMatched) {
						matchedInstance = extractInstance(root, className);
						rtnList.add(matchedInstance);
						break;
					}
				}
			}
			if (!isTouched && operator.equals("IS NULL")) {
				matchedInstance = extractInstance(root, className);
				rtnList.add(matchedInstance);
			}
		}
		return rtnList;
	}
	
	private boolean checkInstanceForSearch(GKInstance instance, String attName, String operator, String targetValue) {
		try {
			java.util.List attValues = instance.getAttributeValuesList(attName);
			if (attValues != null && attValues.size() > 0) {
				if (operator.equals("IS NOT NULL"))
					return true;
				java.util.List values = new ArrayList(attValues.size());
				// Convert Instance to String
				if (attValues.get(0) instanceof GKInstance) {
					for (Iterator it = attValues.iterator(); it.hasNext();) {
						GKInstance tmp = (GKInstance) it.next();
						values.add(tmp.getDisplayName());
					}
				}
				else { 
					for (Iterator it = attValues.iterator(); it.hasNext();)
						values.add(it.next().toString());
				}
				String tmp = null;
				if (operator.equals("=")) {
					for (Iterator it = values.iterator(); it.hasNext();) {
						tmp = it.next().toString();
						if (targetValue.equals(tmp))
							return true;
					}
				}
				else if (operator.equals("LIKE")) {
					for (Iterator it = values.iterator(); it.hasNext();) {
						tmp = it.next().toString();
						if (tmp.indexOf(targetValue) >= 0)
							return true;
					}
				}
				else if (operator.equals("REGEXP")) {
					Pattern pattern = Pattern.compile(targetValue);
					for (Iterator it = values.iterator(); it.hasNext();) {
						tmp = it.next().toString();
						Matcher matcher = pattern.matcher(tmp);
						if (matcher.matches())
							return true;
					}
				}
			}
			else if (operator.equals("IS NULL"))
				return true;
		}
		catch (Exception e) {
			System.err.println("FileAdaptor.checkInstance(): " + e);
			e.printStackTrace();
		}
		return false;

	}
	
	/**
	 * A helper to check if the specified GKInstance object can be passed
	 * the comparasion.
	 */
	private boolean checkInstance(GKInstance instance, 
	                              String attName,
	                              String operator,
	                              Object value) {
	    try {
			java.util.List attValues = instance.getAttributeValuesList(attName);
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
	    			else if (operator.equals("LIKE")) { // Contains
	    				String displayName = target.getDisplayName();
	    				for (Iterator it = attValues.iterator(); it.hasNext();) {
	    					GKInstance tmp = (GKInstance) it.next();
	    					String name1 = tmp.getDisplayName();
	    					if (name1.indexOf(displayName) >= 0)
	    						return true;
	    				}
	    			}
	    			// Cannot support regex
	    			else if (operator.equals("REGEXP"))
	    				throw new IllegalArgumentException("FileAdaptor.checkInstance(): Opertor is not supported: " + operator);
	    		}
	    		else {
	    			String target = value.toString();
	    			if (operator.equals("=")) {
	    				for (Iterator it = attValues.iterator(); it.hasNext();) {
	    					Object obj = it.next();
	    					if (target.equals(obj.toString()))
	    						return true;
	    				}
	    			}
	    			else if (operator.equals("LIKE")) {
	    				for (Iterator it = attValues.iterator(); it.hasNext();) {
	    					String tmp = it.next().toString();
	    					if (tmp.indexOf(target) >= 0)
	    						return true;
	    				}
	    			}
	    			else if (operator.equals("REGEXP")) {
	    				Pattern pattern = Pattern.compile(target);
	    				for (Iterator it = attValues.iterator(); it.hasNext();) {
	    					String tmp = it.next().toString();
	    					Matcher matcher = pattern.matcher(tmp);
	    					if (matcher.matches())
	    						return true;
	    				}
	    			}
	    		}
	    	}
	    	else if (operator.equals("IS NULL"))
	    		return true;
	    }
	    catch(Exception e) {
	    	System.err.println("FileAdaptor.checkInstance(): " + e);
	    	e.printStackTrace();
	    }
		return false;
	}

	private GKInstance extractInstance(Element instanceElm, String clsName) {
		String dbIDStr = instanceElm.getAttribute("DB_ID");
		Long dbID = new Long(dbIDStr);
		GKInstance instance = (GKInstance) cache.get(dbID);
		if (instance != null)
			return instance;
		String name = instanceElm.getAttribute("displayName");
		instance = new GKInstance();
		instance.setAttributeValueNoCheck("_displayName", name);
		instance.setDbAdaptor(this);
		instance.setDBID(dbID);
		instance.setSchemaClass(schema.getClassByName(clsName));
		cache.put(instance);
		return instance;
	}
	
	/**
	 * A helper to fetch instance by class name and its id.
	 */
	public GKInstance fetchInstance(String className, Long dbID) throws Exception {
		// Try to get from the cache first
		GKInstance instance = cache.get(dbID);
		if (instance != null) {
			if (instance.getSchemClass().getName().equals(className))
				return instance;
			else
				return null; // DBID should be unique
		}
		// Get the position on the file
		Map posMap = (Map)indexMap.get(className);
		if (posMap == null)
			return null;
		IndexInfo info = (IndexInfo) posMap.get(dbID);
		if (info == null)
			return null; // It is not in the local file system.
		// Get the info from the file
		ByteBuffer byteBuffer = getByteBuffer(className);
		byteBuffer.limit(info.pos + info.length);
		byteBuffer.position(info.pos);
		// Load the contents for the instance
		instance = new GKInstance();
		SchemaClass schemaClass = schema.getClassByName(className);
		instance.setSchemaClass(schemaClass);
		instance.setDbAdaptor(this);
		instance.setDBID(dbID);
		String line = readLine(byteBuffer); // Escape the first line.
		String name = parseName(line);
		instance.setIsShell(parseIsShell(line));
		instance.setAttributeValue("_displayName", name);
		// Cache it
		cache.put(instance);
		return instance;		
	}
	
	private String parseName(String line) {
	    //System.out.println("parseName:" + line);
	    int index = line.indexOf("displayName=");
		int index1 = line.lastIndexOf("isShell");
		//if (index1 < 0 || index < 0 || (index + 1) > line.length()) {
		//    System.out.println(line);
		//    return null;
		//    }
		String name = line.substring(index + 1, index1).trim();
		index = name.indexOf("\"");
		index1 = name.lastIndexOf("\"");
		name = name.substring(index + 1, index1);
		name = name.replaceAll(AMPERSAND_ESCAPE, AMPERSAND + "");
		name = name.replaceAll(BRACKET_ESCAPE, BRACKET + "");
		name = name.replaceAll(RIGHT_BRACKET_ESCAPE, RIGHT_BRACKET + "");
		name = name.replaceAll(QUOTATION_ESCAPE, QUOTATION + "");
		return name;		
	}
	
	private boolean parseIsShell(String line) {
		int index = line.indexOf("isShell=");
		int index1 = line.lastIndexOf(">");
		//if (index < 0 || index1 < 0 || (index + 8) > line.length()) {
		//   System.out.println(line);
		//    return false;
		//  }
		String value = line.substring(index + 8, index1).trim();
		index = value.indexOf("\"");
		index1 = value.lastIndexOf("\"");
		value = value.substring(index + 1, index1);
		return new Boolean(value).booleanValue();
	}
	
	/* 
	 * @see org.gk.model.PersistenceAdaptor#fetchInstanceByAttribute(org.gk.schema.SchemaAttribute, java.lang.String, java.lang.Object)
	 */
	public Collection fetchInstanceByAttribute(SchemaAttribute attribute, String operator, Object value)
		throws Exception {
		// Need to call from the topmost SchemaClass.
		SchemaClass origin = attribute.getOrigin();
		java.util.List top = new ArrayList(1);
		top.add(origin);
		java.util.List c = InstanceUtilities.getAllSchemaClasses(top);
		java.util.List l = new ArrayList();
		if (c != null && c.size() > 0) {
			for (Iterator it = c.iterator(); it.hasNext();) {
				GKSchemaClass cls = (GKSchemaClass)it.next();
				Collection instances = fetchInstanceByAttribute(cls.getName(), attribute.getName(), operator, value);
				if (instances != null && instances.size() > 0)
					l.addAll(instances);
			}
		}
		return l;
	}

	/* 
	 * @see org.gk.model.PersistenceAdaptor#fetchSchema()
	 */
	public Schema fetchSchema() throws Exception {
		FileInputStream fis = new FileInputStream("resources" + File.separator + "schema");
		ObjectInputStream ois = new ObjectInputStream(fis);
		schema = (GKSchema) ois.readObject();
		for (Iterator it = schema.getClasses().iterator(); it.hasNext();) {
			GKSchemaClass cls = (GKSchemaClass) it.next();
			for (Iterator it1 = cls.getReferers().iterator(); it1.hasNext();) {
				GKSchemaAttribute att = (GKSchemaAttribute) it1.next();
				att.getAllowedClasses();
			}
		}
		// Remember to close these two streams
		ois.close();
		fis.close();
		return schema;
	}
	
	/* 
	 * @see org.gk.model.PersistenceAdaptor#getClassInstanceCount(org.gk.schema.SchemaClass)
	 */
	public long getClassInstanceCount(SchemaClass schemaClass) throws Exception {
		if (indexMap != null) {
			Map map = (Map) indexMap.get(schemaClass.getName());
			int c = 0;
			if (map != null)
				c = map.size();
			java.util.List newList = (java.util.List)newInstanceMap.get(schemaClass);
			if (newList != null)
				c += newList.size();
			return c;
		}
		return 0;
	}
	
	private void loadIndexMap() throws Exception {
		String indexFileName = getFileName(INDEX_FILE_NAME);
		File file = new File(indexFileName);
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(indexFileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			indexMap = (HashMap) ois.readObject();
		}
	}
	
	private void loadDeleteMap() throws Exception {
		String deleteFileName = getFileName(DELETION_FILE);
		File file = new File(deleteFileName);
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(deleteFileName);
			ObjectInputStream ois = new ObjectInputStream(fis);
			deleteMap = (HashMap) ois.readObject();
		}
	}
	
	/**
	 * Get a list of deleted instances registry.
	 * @return Keys: DB_IDs; values: Class Names
	 */
	public Map getDeleteMap() {
		return deleteMap;
	}
	
	private void saveIndexMap() throws IOException {
		// Save the index
		String fileName = getFileName(INDEX_FILE_NAME);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(indexMap);
		oos.close();
		fos.close();		
	}	
	
	private void saveDeleteMap() throws IOException {
		// Save the index
		String fileName = getFileName(DELETION_FILE);
		FileOutputStream fos = new FileOutputStream(fileName);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(deleteMap);
		oos.close();
		fos.close();		
	}	
	
	/**
	 * Remove the delete record for a list of DB_IDs.
	 * @param dbIDs a list of DB_IDs.
	 * @throws IOException
	 */
	public void clearDeleteRecord(java.util.List dbIDs) throws IOException {
		if (dbIDs == null || dbIDs.size() == 0)
			return;
		// Remove the specified DB_IDs from the deleteMaps
		for (Iterator it = dbIDs.iterator(); it.hasNext();) {
			deleteMap.remove(it.next());
		}
		// Save the changes
		saveDeleteMap();
	}
	
	public java.util.List fetchAllEvents() {
		// Have to load all event objects
		SchemaClass eventClass = schema.getClassByName("Event");
		java.util.List list = new ArrayList(1);
		list.add(eventClass);
		java.util.List classes = InstanceUtilities.getAllSchemaClasses(list);
		java.util.List eventInstances = new ArrayList();
		for (Iterator it = classes.iterator(); it.hasNext();) {
			SchemaClass cls = (SchemaClass) it.next();
			try {
				Collection c = fetchInstancesByClass(cls);
				if (c != null && c.size() > 0)
					eventInstances.addAll(c);
			}
			catch (Exception e) {
				System.err.println("FileAdaptor.buildProject(): " + e);
				e.printStackTrace();
			}
		}
		return eventInstances;
	}
	
	private GKInstance extractInstance(Element elm) {
		String dbIDStr = elm.getAttribute("DB_ID");
		Long dbID = new Long(dbIDStr);
		GKInstance instance = cache.get(dbID);
		if (instance == null) {
			String cls = elm.getAttribute("class");
			String name = elm.getAttribute("displayName");
			SchemaClass schemaClass = schema.getClassByName(cls);
			instance = new GKInstance();
			instance.setDBID(dbID);
			instance.setAttributeValueNoCheck("_displayName", name);
			instance.setDbAdaptor(this);
			instance.setSchemaClass(schemaClass);
			cache.put(instance);
		}	
		return instance;	
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
				System.err.println("FileAdaptor.getSchema(): " + e);
			}
		}
		return schema;
	}
	
	/* 
	 * @see org.gk.model.PersistenceAdaptor#loadInstanceAttributeValues(org.gk.model.GKInstance, org.gk.schema.SchemaAttribute)
	 */
	public void loadInstanceAttributeValues(GKInstance instance, SchemaAttribute attribute) throws Exception {
		if (instance.isInflated() || instance.isShell())
			return;
		loadInstanceAttributes(instance);
		instance.setIsInflated(true);
	}
	
	/**
	 * Use this method to load all attributes.
	 * @param instance
	 */
	public void loadInstanceAttributes(GKInstance instance) throws Exception {
		// List all attribute that have been loaded
		Map loadedAtts = new HashMap();
		for (Iterator it = instance.getSchemaAttributes().iterator(); it.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute) it.next();
			if (instance.isAttributeValueLoaded(att)) {
				loadedAtts.put(att, null);
			}
		}
		// Get the index info.
		SchemaClass schemaClass = instance.getSchemClass();
		String clsName = schemaClass.getName();
		Map posMap = (Map)indexMap.get(clsName);
		IndexInfo info = (IndexInfo)posMap.get(instance.getDBID());
		ByteBuffer bb = getByteBuffer(clsName);
		bb.limit(info.pos + info.length);
		bb.position(info.pos);
		CharBuffer cb = decoder.decode(bb);
		String xmlStr = XML_HEADER + LINE_END + cb.toString();
		//System.out.println(xmlStr);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		StringReader reader = new StringReader(xmlStr);
		Document document = builder.parse(new InputSource(reader));
		Element root = document.getDocumentElement();
		NodeList attList = root.getElementsByTagName("attribute");
		int size = attList.getLength();
		for (int i = 0; i < size; i++) {
			Element elm = (Element)attList.item(i);
			String name = elm.getAttribute("name");
			if (!schemaClass.isValidAttribute(name)) // Schema might be changed
				continue;
			SchemaAttribute att = schemaClass.getAttribute(name);
			if (loadedAtts.containsKey(att))
				continue;
			String value = null;
			int type = att.getTypeAsInt();
			switch (type) {
				case SchemaAttribute.INSTANCE_TYPE :
					String clsValue = elm.getAttribute("class");
					value = elm.getAttribute("referTo");
					Long dbID = new Long(value);
					GKInstance tmpInstance = fetchInstance(clsValue, dbID);
					// There is a bug in SchemaAttribute: e.g. startTime can be either Time or GenericPathway
					// instance. However, isValidAttribute checking will return false for GenericPathway.
					if (att.isMultiple())
					    instance.addAttributeValueNoCheck(att, tmpInstance);
					else
					    instance.setAttributeValueNoCheck(att, tmpInstance);
					break;
				case SchemaAttribute.STRING_TYPE :
					value = elm.getAttribute("value");
					instance.addAttributeValueNoCheck(att, value);
					break;
				case SchemaAttribute.INTEGER_TYPE :
					value = elm.getAttribute("value");
					Integer intValue = new Integer(value);
					instance.addAttributeValueNoCheck(att, intValue);
					break;
				case SchemaAttribute.LONG_TYPE :
					value = elm.getAttribute("value");
					Long longValue = new Long(value);
					instance.addAttributeValueNoCheck(att, longValue);
					break;
				case SchemaAttribute.FLOAT_TYPE :
					value = elm.getAttribute("value");
					Float floatValue = new Float(value);
					instance.addAttributeValueNoCheck(att, floatValue);
					break;
				case SchemaAttribute.BOOLEAN_TYPE :
					value = elm.getAttribute("value");
					Boolean booleanValue = new Boolean(value);
					instance.addAttributeValueNoCheck(att, booleanValue);
					break;
				default :
					value = elm.getAttribute("value");
					instance.addAttributeValue(att, value);
			}
		}
	}
	
	public java.util.List getReferers(GKInstance instance) throws Exception {
		Set set = new HashSet();
		// Check newly created Instance objects
		for (Iterator it = newInstanceMap.keySet().iterator(); it.hasNext();) {
			SchemaClass cls = (SchemaClass)it.next();
			java.util.List newInstances = (java.util.List)newInstanceMap.get(cls);
			if (newInstances != null && newInstances.size() > 0) {
				for (Iterator it1 = newInstances.iterator(); it1.hasNext();) {
					GKInstance newInstance = (GKInstance)it1.next();
					for (Iterator it2 = newInstance.getSchemaAttributes().iterator(); it2.hasNext();) {
						SchemaAttribute att = (SchemaAttribute) it2.next();
						if (!att.isInstanceTypeAttribute())
							continue;
						java.util.List values = newInstance.getAttributeValuesList(att);
						if (values != null && values.size() > 0 && values.contains(instance))
							set.add(newInstance);
					}
				}
			}
		}
		// Have to check modified instance objects
		Map checkedInstances = new HashMap();
		for (Iterator it = dirtyMap.keySet().iterator(); it.hasNext();) {
			SchemaClass cls = (SchemaClass)it.next();
			java.util.List changedInstances = (java.util.List)dirtyMap.get(cls);
			if (changedInstances != null && changedInstances.size() > 0) {
				for (Iterator it1 = changedInstances.iterator(); it1.hasNext();) {
					GKInstance changedInstance = (GKInstance)it1.next();
					checkedInstances.put(changedInstance.getDBID(), changedInstance);
					for (Iterator it2 = changedInstance.getSchemaAttributes().iterator(); it2.hasNext();) {
						SchemaAttribute att = (SchemaAttribute) it2.next();
						if (!att.isInstanceTypeAttribute())
							continue;
						java.util.List values = changedInstance.getAttributeValuesList(att);
						if (values != null && values.size() > 0 & values.contains(instance))
							set.add(changedInstance);
					}
				}
			}
		}
		SchemaClass cls = instance.getSchemClass();
		// Note: This pattern requires that there is no space in the class name.
		//Pattern pattern = Pattern.compile("class=\"\\w+\" value=\"" + instance.getDBID() + "\"");
		//Matcher matcher = null;
		String pattern = "referTo=\"" + instance.getDBID() + "\"";
		byte[] pb = pattern.getBytes(CHARSET_NAME);
		java.util.List top = new ArrayList(1);
		for (Iterator it = cls.getReferers().iterator(); it.hasNext();) {
			GKSchemaAttribute att = (GKSchemaAttribute)it.next();
			GKSchemaClass origin = (GKSchemaClass) att.getOrigin();
			top.clear();
			top.add(origin);
			java.util.List classes = InstanceUtilities.getAllSchemaClasses(top);
			for (Iterator it1 = classes.iterator(); it1.hasNext();) {
				GKSchemaClass schemaClass = (GKSchemaClass)it1.next();
				String className = schemaClass.getName();
				// Get the index info.
				Map posMap = (Map)indexMap.get(className);
				if (posMap == null || posMap.size() == 0)
					continue;
				ByteBuffer bb = getByteBuffer(className);
				for (Iterator it2 = posMap.keySet().iterator(); it2.hasNext();) {
					Long dbID = (Long)it2.next();
					if (checkedInstances.containsKey(dbID))
						continue;
					IndexInfo info = (IndexInfo)posMap.get(dbID);
					bb.limit(info.pos + info.length);
					bb.position(info.pos);
					byte[] sb = new byte[info.length];
					bb.get(sb);
					if (match(sb, pb)) {
						GKInstance referer = fetchInstance(className, dbID);
						if (referer != null)
						    set.add(referer);
					}
				}
			}
		}
		return new ArrayList(set);		
	}
	
	private boolean match(byte[] sb, byte[] tb) {
		int l1 = sb.length;
		int l2 = tb.length;
		int l = l1 - l2;
		boolean found = true;
		for (int i = 0; i < l; i++) {
			found = true;
			for (int j = 0; j < l2; j++) {
				if (sb[i + j] != tb[j]) {
					found = false;
					break;
				}
			}
			if (found)
				return true;
		}
		return false;
	}
	
	/* 
	 * @see org.gk.model.PersistenceAdaptor#storeInstance(org.gk.model.GKInstance)
	 */
	public Long storeInstance(GKInstance instance) throws Exception {
		throw new UnsupportedOperationException("FileAdaptor.storeInstance(GKInstane) is not supported");
	}
	
	/**
	 * Save all changes.
	 * @throws Exception
	 */
	public void save() throws Exception {
		// Nothing changed
		if (dirtyMap.size() == 0 && newInstanceMap.size() == 0 && !doDelete)
			return;
		// First list all SchemaClasses that have be changed
		java.util.List classes = new ArrayList(dirtyMap.keySet());
		for (Iterator it = newInstanceMap.keySet().iterator(); it.hasNext();) {
			Object cls = it.next();
			if (!classes.contains(cls))
				classes.add(cls);
		}
		//create a copy of the files for recovered
		try {
			prepareForSave(classes);
		}
		catch(Exception e) {
			cleanUpBackup(classes);
			System.err.println("FileAdaptor.save(): " + e);
			e.printStackTrace();
			throw new Exception("FileAdaptor.save(): Cannot create a backup copy: " + e);
		}
		try {
			int bufferSize = BUFFER_SIZE * 100;
			for (Iterator it = classes.iterator(); it.hasNext();) {
				SchemaClass cls = (SchemaClass)it.next();
				java.util.List dirtyInstances = (java.util.List)dirtyMap.get(cls);
				Map map = new HashMap();
				// Create a Map for dirty instances to speed up
				if (dirtyInstances != null) {
					for (Iterator it1 = dirtyInstances.iterator(); it1.hasNext();) {
						GKInstance instance = (GKInstance)it1.next();
						map.put(instance.getDBID(), instance);
					}
				}
				ByteBuffer bb = getByteBuffer(cls.getName());
				byte[] newByte = null;
				if (bb != null)
					newByte = new byte[bb.capacity()];
				else
					newByte = new byte[bufferSize]; // Initial size
				Map clsIndexMap = (Map)indexMap.get(cls.getName());
				Map newClsIndexMap = new HashMap();
				// Save the changed Instances
				int offset = 0;
				if (clsIndexMap != null && clsIndexMap.size() > 0) {
					// Load the contents to a memory buffer
					byte[] bbArray = new byte[bb.capacity()];
					// Have to reset these info to get the correct copy.
					bb.position(0);
					bb.limit(bb.capacity());
					bb.get(bbArray);
					bb = null; // Mark for gc
					for (Iterator it1 = clsIndexMap.keySet().iterator(); it1.hasNext();) {
						Long dbID = (Long)it1.next();
						IndexInfo info = (IndexInfo)clsIndexMap.get(dbID);
						GKInstance instance = (GKInstance)map.get(dbID);
						if (instance != null) { // Instance has been changed. 
							// Need to regenerate the text.
							String instanceStr = convertInstanceToString(instance);
							byte[] instanceArray = instanceStr.getBytes(CHARSET_NAME);
							int length = instanceArray.length;
							newByte = ensureArrayCapacity(newByte, offset, length, bufferSize);
							System.arraycopy(instanceArray, 0, newByte, offset, length);
							IndexInfo newInfo = new IndexInfo(offset, length);
							newClsIndexMap.put(dbID, newInfo);
							offset += length;
						}
						else { // Nothing changed. Just copy it.
							newByte = ensureArrayCapacity(newByte, offset, info.length, bufferSize);
							System.arraycopy(bbArray, info.pos, newByte, offset, info.length);
							info.pos = offset;
							newClsIndexMap.put(dbID, info);
							offset += info.length;
						}
					}
				}
				// Handle on new Instances
				java.util.List newInstances = (java.util.List)newInstanceMap.get(cls);
				if (newInstances != null && newInstances.size() > 0) {
					for (Iterator it1 = newInstances.iterator(); it1.hasNext();) {
						GKInstance instance = (GKInstance)it1.next();
						String instanceStr = convertInstanceToString(instance);
						byte[] instanceArray = instanceStr.getBytes(CHARSET_NAME);
						int length = instanceArray.length;
						newByte = ensureArrayCapacity(newByte, offset, length, bufferSize);
						System.arraycopy(instanceArray, 0, newByte, offset, length);
						IndexInfo newInfo = new IndexInfo(offset, length);
						newClsIndexMap.put(instance.getDBID(), newInfo);
						offset += length;
					}
				}
				// Save to the file
				// Have to use a RandomAccessFile. FileOutputStream cannot work here since
				// it seems to be locked by the file system.
				RandomAccessFile raf = new RandomAccessFile(getFileName(cls.getName()), "rw");
				FileChannel channel = raf.getChannel();
				channel.write(ByteBuffer.wrap(newByte, 0, offset));
				channel.close();
				raf.close();
				// Update the indexMap
				indexMap.put(cls.getName(), newClsIndexMap);
			}
			// Save the indexMap
			saveIndexMap();
			saveDeleteMap();
			doDelete = false; // Reset the flag
			// clear these data maps
			dirtyMap.clear();
			// Need to recorde all deletion for comparison with db.
			newInstanceMap.clear();
			clsBBMap.clear();
			// Have to save nextID here
			saveLastDBID();
			// Fire the save action.
			propertyChangeSupport.firePropertyChange("save", null, null);
		}
		catch (Exception e) {
			rollback(classes);
			cleanUpBackup(classes); // Remove backup copy
			throw e;
		}
		cleanUpBackup(classes); // Remove backup copy
	}
	
	private void prepareForSave(java.util.List classes) throws Exception {
		// Create a copy for classes
		GKSchemaClass cls = null;
		ByteBuffer bb = null;
		String bkName = null;
		FileChannel bkChannel = null;
		for (Iterator it = classes.iterator(); it.hasNext();) {
			cls = (GKSchemaClass) it.next();
			String fileName = getFileName(cls.getName());
			copyFile(fileName, fileName + BACKUP_EXT_NAME);
		}
		// create a copy for IndexMap and DeleteMap
		String fileName = getFileName(INDEX_FILE_NAME);
		copyFile(fileName, fileName + BACKUP_EXT_NAME);
		fileName = getFileName(DELETION_FILE);
		copyFile(fileName, fileName + BACKUP_EXT_NAME);
	}
	
	private void rollback(java.util.List classes) throws IOException {
		// Copy the backed files back
		GKSchemaClass cls = null;
		String fileName = null;
		for (Iterator it = classes.iterator(); it.hasNext();) {
			cls = (GKSchemaClass) it.next();
			fileName = getFileName(cls.getName());
			copyFile(fileName + BACKUP_EXT_NAME, fileName);
		}
		fileName = getFileName(INDEX_FILE_NAME);
		copyFile(fileName + BACKUP_EXT_NAME, fileName);
		fileName = getFileName(DELETION_FILE);
		copyFile(fileName + BACKUP_EXT_NAME, fileName);
		clsBBMap.clear(); // Reset cache
	}
	
	private void cleanUpBackup(java.util.List classes) {
		GKSchemaClass cls = null;
		String fileName = null;
		File file = null;
		for (Iterator it = classes.iterator(); it.hasNext();) {
			cls = (GKSchemaClass) it.next();
			fileName = getFileName(cls.getName());
			file = new File(fileName + BACKUP_EXT_NAME);
			if (file.exists())
				file.delete();
		}
		fileName = getFileName(INDEX_FILE_NAME);
		file = new File(fileName + BACKUP_EXT_NAME);
		if (file.exists())
			file.delete();
		fileName = getFileName(DELETION_FILE);
		file = new File(fileName + BACKUP_EXT_NAME);
		if (file.exists())
			file.delete();
	}
	
	private void copyFile(String srcName, String targetName) throws IOException {
		File file = new File(srcName);
		if (!file.exists())
			return;
		int buffer = BUFFER_SIZE * 100;
		ByteBuffer bb = ByteBuffer.allocate(buffer);
		RandomAccessFile srcFile = new RandomAccessFile(srcName, "rw");
		FileChannel inChannel = srcFile.getChannel();
		RandomAccessFile targetFile = new RandomAccessFile(targetName, "rw");
		FileChannel outChannel = targetFile.getChannel();
		int size = 0;
		while ((size = inChannel.read(bb)) > 0) {
			bb.flip();
			outChannel.write(bb);
			// Reset bb
			bb.clear();
		}
		inChannel.close();
		srcFile.close();
		outChannel.close();
		targetFile.close();
	}
	
	/**
	 * A helper to check the length of the specified byte array. If it is enought
	 * for the specified lenght, return the passed array. Otherwise, a new array
	 * is needed to created.
	 */
	private byte[] ensureArrayCapacity(byte[] array, int offset, int length, int bufferSize) {
		if (offset + length <= array.length)
			return array;
		int extra = length > bufferSize ? length : bufferSize;
		byte[] newArray = new byte[array.length + extra];
		System.arraycopy(array, 0, newArray, 0, offset);
		return newArray;
	}
	
	/**
	 * A helper to add a GKInstance to a Map.
	 * @param instance
	 * @param map
	 */
	private void addToMap(SchemaClass schemaClass, GKInstance instance, Map map) {
		java.util.List list = (java.util.List) map.get(schemaClass);
		if (list == null) {
			list = new ArrayList();
			map.put(schemaClass, list);
		}
		list.add(instance);
	}
	
	/**
	 * Check if there is any unsaved changes.
	 * @return
	 */
	public boolean isDirty() {
		return (newInstanceMap.size() > 0 || dirtyMap.size() > 0 ||
		        doDelete) ? true : false;
	}
	
	/**
	 * Save the a collection of instances from the database to the local file system. The client to this method should 
	 * make sure the Instance objects in the map should not be in the local file system.
	 * @param instanceMap Keys: SchemaClass Values: a list of GKInstances that belong to
	 * the SchemaClass in key.
	 * @throws Exception
	 */
	public void store(Map instanceMap) throws Exception {
		if (isDirty()) {
			throw new IllegalStateException("FileAdaptor.store(): Changes should be saved before storing.");
		}
		// Make sure there are local SchemaClass available in case new SchemaClass are added
		for (Iterator it = instanceMap.keySet().iterator(); it.hasNext();) {
			GKSchemaClass dbCls = (GKSchemaClass) it.next();
			GKSchemaClass localCls = (GKSchemaClass) schema.getClassByName(dbCls.getName());
			if (localCls == null) {
				throw new IllegalStateException("FileAdaptor.store(): The local schema is not compatible to the DB schema.");
			}
		}
		// Generate new Maps
		java.util.List newInstances = new ArrayList(); // For update views
		java.util.List updatedInstances = new ArrayList(); // For update views
		for (Iterator it = instanceMap.keySet().iterator(); it.hasNext();) {
			// Note: This class is from the database
			GKSchemaClass schemaClass = (GKSchemaClass) it.next(); 
			java.util.List list = (java.util.List) instanceMap.get(schemaClass);
			if (list == null || list.size() == 0)
				continue;
			// Convert to local schemaClass. It seems not necessary.
			SchemaClass localSchemaCls = schema.getClassByName(schemaClass.getName());
			for (Iterator it1 = list.iterator(); it1.hasNext();) {
				// Note: This Instance is from the database
				GKInstance instance = (GKInstance) it1.next();
				// Check if there is a local copy for it
				GKInstance localCopy = fetchInstance(schemaClass.getName(), instance.getDBID());
				if (localCopy != null) {
					// Keep the local no-shell copy or replace a shell copy with a non-shell copy from database
					if (localCopy.isShell() && !instance.isShell()) {
						addToMap(localSchemaCls, instance, dirtyMap);
						updatedInstances.add(instance);
					}	
				}
				else {
					instance.clearReferers();
					addToMap(localSchemaCls, instance, newInstanceMap);
					newInstances.add(instance);
					if (deleteMap.containsKey(instance.getDBID())) {
						deleteMap.remove(instance.getDBID());
					}
				}
			}
		}
		try {
			save(); // call save before the following block so that the local instances
		            // can be passed to the property listener.
		}
		catch(Exception e) {
			dirtyMap.clear();
			newInstanceMap.clear();
			throw e;
		}
		java.util.List localList = new ArrayList();
		for (Iterator it = newInstances.iterator(); it.hasNext();) {
			GKInstance instance = (GKInstance)it.next();
			String clsName = instance.getSchemClass().getName();
			Long dbID = instance.getDBID();
			GKInstance localCopy = fetchInstance(clsName, dbID);
			if (localCopy != null)
				localList.add(localCopy);
		}
		// Need to update local instances
		java.util.List localList1 = new ArrayList();
		for (Iterator it = updatedInstances.iterator(); it.hasNext();) {
			GKInstance dbCopy = (GKInstance) it.next();
			String clsName = dbCopy.getSchemClass().getName();
			Long dbID = dbCopy.getDBID();
			GKInstance localCopy = fetchInstance(clsName, dbID);
			loadInstanceAttributes(localCopy);
			localCopy.setIsShell(false);
		}
		// Have to call update instances first
		propertyChangeSupport.firePropertyChange("addNewInstance", null, localList);
		// explicitly call this method since there is nothing changed in the local repository
		// This calling is used to validate GUIs (e.g. save button).
		propertyChangeSupport.firePropertyChange("save", null, null);
	}
	
	public String convertInstanceToString(GKInstance instance) throws Exception {
		StringBuffer buffer = new StringBuffer();
		SchemaClass schemaClass = instance.getSchemClass();
		buffer.append("<instance ");
		buffer.append("DB_ID=\"" + instance.getDBID() + "\" ");
		buffer.append("displayName=\"" + validateXMLText(instance.getDisplayName()) + "\" ");
		if (instance.isShell()) {
			buffer.append(" isShell=\"true\">");
			buffer.append(LINE_END);
		}
		else {
			buffer.append(" isShell=\"false\">");
			buffer.append(LINE_END);
			// Put valid attributes in
			for (Iterator i1 = schemaClass.getAttributes().iterator(); i1.hasNext();) {
				SchemaAttribute att = (SchemaAttribute)i1.next();
				if (instance.isAttributeValueLoaded(att)) {
					Collection c = instance.getAttributeValuesList(att);
					if (c != null && c.size() > 0) {
						if (att.isInstanceTypeAttribute()) {
							for (Iterator it2 = c.iterator(); it2.hasNext();) {
								GKInstance tmp = (GKInstance) it2.next();
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
		buffer.append("</instance>");
		buffer.append(LINE_END);
		buffer.append(LINE_END); // As a separator between instances.
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
		// Update indexMap if this Instance it not new
		Map classMap = (Map) indexMap.get(instance.getSchemClass().getName());
		if (classMap != null) {
			Object obj = classMap.remove(oldDBID);
			classMap.put(instance.getDBID(), obj);
		}
		// Mark it as dirty
		markAsDirty(instance);
	}
	
	/* 
	 * @see org.gk.model.PersistenceAdaptor#updateInstanceAttribute(org.gk.model.GKInstance, java.lang.String)
	 */
	public void updateInstanceAttribute(GKInstance instance, String attributeName) throws Exception {
	}
	
	public Collection fetchInstancesByClass(SchemaClass class1) throws Exception {
		return fetchInstancesByClass(class1.getName());
	}
	
	public Collection fetchInstancesByClass(String className) throws Exception {
		// Check if indexMap is there
		if (indexMap == null)
			loadIndexMap();
		Map posMap = (Map) indexMap.get(className);
		java.util.List list = new ArrayList();
		// Add newlys created instances
		java.util.List newInstances = (java.util.List) newInstanceMap.
													   get(schema.getClassByName(className));
		if (newInstances != null)
			list.addAll(newInstances);
		if (posMap == null || posMap.size() == 0)
			return list;
		String fileName = getFileName(className);	
		// A read-only file
		ByteBuffer byteBuffer = getByteBuffer(className);
		// Use only one charBuffer
		CharBuffer charBuffer = CharBuffer.allocate(BUFFER_SIZE);
		IndexInfo info = null;
		String line = null;
		for (Iterator it = posMap.keySet().iterator(); it.hasNext();) {
			Long dbID = (Long)it.next();
			GKInstance instance = cache.get(dbID);
			if (instance != null)
				list.add(instance);
			else {
				instance = new GKInstance();
				instance.setDBID(dbID);
				instance.setDbAdaptor(this);
				instance.setSchemaClass(schema.getClassByName(className));
				info = (IndexInfo)posMap.get(dbID);
				byteBuffer.limit(info.pos + info.length);
				byteBuffer.position(info.pos);
				line = readLine(byteBuffer, charBuffer);
				charBuffer.clear();
				instance.setAttributeValueNoCheck("_displayName", parseName(line));
				instance.setIsShell(parseIsShell(line));
				list.add(instance);
				cache.put(instance);
			}
		}
		return list;
	}
	
	private String getFileName(String fileName) {
		if (dir != null)
			return dir + File.separator + fileName;
		return fileName;
	}
	
	/**
	 * Read a line from the specified ByteBuffer. Reading starts from the position.
	 * @param bb
	 * @return
	 */
	private String readLine(ByteBuffer bb) {
		CharBuffer cb = CharBuffer.allocate(BUFFER_SIZE);
		return readLine(bb, cb);
	}
	
	/**
	 * An overloaded method to reuse a CharBuffer.
	 * @param bb
	 * @param cb
	 * @return
	 */
	private String readLine(ByteBuffer bb, CharBuffer cb) {
		StringBuffer sb = new StringBuffer();
		decoder.decode(bb, cb, true);
		cb.flip();
		char c;
		boolean isLineEnd = false;
		while (cb.hasRemaining()) {
			while (cb.hasRemaining()) {
				c = cb.get();
				if (c == LINE_END) {
					isLineEnd = true;
					break;
				}
				sb.append(c);
			}
			if (isLineEnd)
				break;
			cb.clear();
			decoder.decode(bb, cb, true);
		}
		return sb.toString();		
	}
	
	/**
	 * A helper to load instance files for a specified SchemaClass.
	 */
	private ByteBuffer getByteBuffer(String className) {
		ByteBuffer byteBuffer = (ByteBuffer) clsBBMap.get(className);
		if (byteBuffer == null) {
			String fileName = getFileName(className);
			FileChannel channel = null;
			try {
				File file = new File(fileName);
				if (!file.exists())
					return null;
				// Make sure there is a file existing
				RandomAccessFile raf = new RandomAccessFile(fileName, "r");
				channel = raf.getChannel();
				byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
				clsBBMap.put(className, byteBuffer);
				raf.close();
			}
			catch (Exception e) {
				System.err.println("FileAdaptor.getByteBuffer(): " + e);
				e.printStackTrace();
			}
			finally {
				if (channel != null) {
					try {
						channel.close();
					}
					catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return byteBuffer;
	}
	
	/**
	 * Load the bookmarks from the local repository.
	 * @return a Bookmarks object.
	 */
	public Bookmarks loadBookmarks() {
	    File file = new File(getFileName(BOOKMARK_FILE));
		if (!file.exists())	
			return null;
		Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			document = builder.parse(file);
		}
		catch(Exception e) {
			System.err.println("GKCuratorFrame.saveBookmarks(): " + e);
			e.printStackTrace();
			return null;
		}	
		Bookmarks rnt = new Bookmarks();
		java.util.List bookmarks = new ArrayList();
		Element root = document.getDocumentElement();
		String sortingKey = root.getAttribute("sortingKey");
		NodeList list = root.getElementsByTagName("bookmark");
		for (int i = 0; i < list.getLength(); i++) {
			Element elm = (Element) list.item(i);
			Bookmark bookmark = new Bookmark();
			bookmark.setDisplayName(elm.getAttribute("displayName"));
			bookmark.setDbID(new Long(elm.getAttribute("DB_ID")));
			bookmark.setType(elm.getAttribute("type"));
			String desc = elm.getAttribute("desc");
			if (desc != null && desc.length() > 0)
			    bookmark.setDescription(desc);
			bookmarks.add(bookmark);
		}
		return new Bookmarks(sortingKey, bookmarks);
	}
	
	public void saveBookmarks(java.util.List bookmarks, String sortingKey) {
		if (bookmarks == null || bookmarks.size() == 0) {
		    // Delete the file
		    File file = new File(getFileName(BOOKMARK_FILE));
		    file.delete();
		    return;
		}
	    Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			document = builder.newDocument();
		}
		catch(Exception e) {
			System.err.println("FileAdaptor.saveBookmarks(): " + e);
			e.printStackTrace();
			return;
		}
		Element root = document.createElement("bookmarks");
		root.setAttribute("sortingKey", sortingKey);
		for (Iterator it = bookmarks.iterator(); it.hasNext();) {
			Bookmark bookmark = (Bookmark) it.next();
			Element elm = document.createElement("bookmark");
			elm.setAttribute("displayName", bookmark.getDisplayName());
			elm.setAttribute("DB_ID", bookmark.getDbID() + "");
			elm.setAttribute("type", bookmark.getType());
			if (bookmark.getDescription() != null && 
			    bookmark.getDescription().length() > 0)
			    elm.setAttribute("desc", bookmark.getDescription());
			root.appendChild(elm);
		}
		document.appendChild(root);
		try {
			String dest = getFileName(BOOKMARK_FILE);
			FileOutputStream xmlOut = new FileOutputStream(dest);
			TransformerFactory tffactory = TransformerFactory.newInstance();
			Transformer transformer = tffactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(xmlOut);
			transformer.transform(source, result);
		}
		catch (Exception e) {
			System.err.println("FileAdator.saveBookmarks() 1: " + e);
			e.printStackTrace();
		}
	}
	
	public void addAddInstanceListener(PropertyChangeListener l) {
		propertyChangeSupport.addPropertyChangeListener(l);
	}
	
	public void removeInstanceListener(PropertyChangeListener l) {
		propertyChangeSupport.removePropertyChangeListener(l);
	}
	
	public static void main(String[] args) {
	    try {
	        String fileName = "/home/wgm/gkteam/lisa/010605";
            FileAdaptor adaptor = new FileAdaptor(fileName);
	        GKSchema schema = (GKSchema) adaptor.getSchema();
            Collection classes = schema.getClasses();
            GKSchemaClass cls = null;
            GKInstance instance = null;
            StringBuffer buffer = new StringBuffer();
            buffer.append(XML_HEADER);
            buffer.append(LINE_END);
            buffer.append("<reactome>\n");
            for (Iterator it = classes.iterator(); it.hasNext();) {
                cls = (GKSchemaClass) it.next();
                Collection instances = adaptor.fetchInstancesByClass(cls);
                if (instances == null || instances.size() == 0)
                    continue;
                buffer.append("<");
                buffer.append(cls.getName());
                buffer.append(">\n");
                for (Iterator it1 = instances.iterator(); it1.hasNext();) {
                    instance = (GKInstance) it1.next();
                    adaptor.loadInstanceAttributes(instance);
                    buffer.append(adaptor.convertInstanceToString(instance));
                }
                buffer.append("</");
                buffer.append(cls.getName());
                buffer.append(">\n");
            }
            buffer.append("</reactome>\n");
            FileWriter writer = new FileWriter(fileName + File.separator + "oneFile.xml");
            writer.write(buffer.toString());
            writer.flush();
            writer.close();
	    }
	    catch(Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public GKInstance fetchInstance(Long dbId) throws Exception {
	    return null;
	}
	
	/**
	 * This inner class is for storing index information.
	 */
	static class IndexInfo implements Serializable {
		int pos;
		int length;
		
		public IndexInfo() {
		}
		
		public IndexInfo(int pos, int length) {
			this.pos = pos;
			this.length = length;
		}
	}
}
