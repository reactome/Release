/*
 * Created on Jan 6, 2004
 */
package org.gk.gkCurator;

import java.io.File;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This singleton Manager is used to control the meta data for the curator tool.
 * @author wugm
 */
public class MetaDataManager {
	// Root directory
	private final String ROOT_DIR = "resources" + File.separator;
	private static MetaDataManager manager = null;
	// Cache these information
	private String urlName;
	private Map localMap;
	private Map remoteMap;

	private MetaDataManager() {
	}
	
	public static MetaDataManager getManager() {
		if (manager == null)
			manager = new MetaDataManager();
		return manager;
	}
	
	public String getSchemaClassDesc(String clsName) {
		String fileName = ROOT_DIR + "SchemaDoc.xml";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(fileName);
			Element root = doc.getDocumentElement();
			NodeList list = root.getElementsByTagName("classes");
			// There should be one
			Element clsElm = (Element) list.item(0);
			list = clsElm.getElementsByTagName("class");
			int size = list.getLength();
			for (int i = 0; i < size; i++) {
				Element elm = (Element)list.item(i);
				if (elm.getAttribute("name").equals(clsName)) {
					// Get the desc
					NodeList list1 = elm.getElementsByTagName("desc");
					Element elm1 = (Element) list1.item(0);
					NodeList list2 = elm1.getChildNodes();
					Node textNode = list2.item(0);
					return textNode.getNodeValue(); // This should be the text
				}
			}
		}
		catch(Exception e) {
			System.err.println("MetaDataManager.getSchemaClassDesc(): " + e);
			e.printStackTrace();
		}
		return "";
	}
}
