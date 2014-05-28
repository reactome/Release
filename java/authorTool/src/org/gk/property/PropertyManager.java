/*
 * Created on Jul 31, 2003
 */
package org.gk.property;

import java.awt.Rectangle;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gk.model.ReferenceDatabase;
import org.gk.render.Renderable;
import org.gk.render.RenderableCompartment;
import org.gk.util.AuthorToolAppletUtilities;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This singleton is used to manage common properties.
 * @author wgm
 */
public class PropertyManager {
    private final String EXTRA_TAXON_KEY = "extraTaxons";
    private final String EXTRA_LOCALIZATION_KEY = "extraLocalizations";
	private static PropertyManager instance;
	// Properties
	private java.util.List taxons;
    // Added not from the default config
    private List extraTaxons;
	private java.util.List referenceDBs;
	private java.util.List localizations;
    // Added from the properties
    private List extraLocalizations;
	private java.util.List timings;
    private DefaultComboBoxModel localizationModel;
    // Model for localizations
    // Properties used to store/load extra terms
    private Properties systemProps;
    // map from compartment to membrane
    private Map<String, String> compToMembrane;
	
	private PropertyManager() {
	}
	
	/**
	 * Get the singleton.
	 * @return the singleton.
	 */
	public static PropertyManager getManager() {
		if (instance == null)
			instance = new PropertyManager();
		return instance;
	}
    
    public void setSystemProperties(Properties prop) {
        this.systemProps = prop;
    }
	
    public ComboBoxModel getTaxonModel() {
        DefaultComboBoxModel taxonModel = new DefaultComboBoxModel();
        taxons = loadTaxons();
        extraTaxons = loadExtraTerms(EXTRA_TAXON_KEY);
        initModel(taxons, 
                  extraTaxons,
                  taxonModel,
                  2);
        return taxonModel;
    }
    
    public ComboBoxModel getLocalizationModel() {
        if (localizationModel == null) {
            localizationModel = new DefaultComboBoxModel();
            localizations = loadLocalizations();
            extraLocalizations = loadExtraTerms(EXTRA_LOCALIZATION_KEY);
            initModel(localizations,
                      extraLocalizations,
                      localizationModel,
                      1);
        }
        return localizationModel;
    }
    
    private Map<String, String> getCompartmentToMembraneMap() {
        if (compToMembrane == null) {
            compToMembrane = new HashMap<String, String>();
            compToMembrane.put("cytosol", "plasma membrane");
            compToMembrane.put("endosome", "endosome membrane");
            compToMembrane.put("endoplasmic reticulum lumen",
                                "endoplasmic reticulum membrane");
            compToMembrane.put("endoplasmic reticulum",
                                "endoplasmic reticulum membrane");
            compToMembrane.put("Golgi lumen", "Golgi membrane");
            compToMembrane.put("mitochondrial matrix",
                                "mitochondrial inner membrane");
            compToMembrane.put("mitochondrial intermembrane space",
            "mitochondrial outer membrane");
            compToMembrane.put("nucleoplasm",
            "nuclear membrane");
            compToMembrane.put("peroxisomal matrix",
            "peroxisomal membrane");
        }
        return compToMembrane;
    }
    
    public String getLocalizationFromContainer(RenderableCompartment container,
                                               Renderable r) {
        Map<String, String> compToMembrane = getCompartmentToMembraneMap();
        String compName = container.getDisplayName();
        // Check if r is completely contained by container
        Rectangle rBounds = r.getBounds();
        Rectangle cBounds = container.getBounds();
        if (rBounds == null ||
            cBounds == null ||
            cBounds.contains(rBounds)) {
            return compName;
        }
        else {
            String membrane = compToMembrane.get(compName);
            if (membrane != null)
                // It should be a membrane
                return membrane;
            else
                return compName;
        }
    }
    
    public List<String> getCompartmentNames() {
        List<String> names = new ArrayList<String>();
        names.addAll(localizations);
        names.addAll(extraLocalizations);
//        // Remove all components ending with membrane
//        for (Iterator<String> it = names.iterator(); it.hasNext();) {
//            String name = it.next();
//            if (name.endsWith("membrane"))
//                it.remove();
//        }
        // Have to sort it first
        Collections.sort(names);
        return names;
    }
    
    private void storeExtraTerms(List extraTerms,
                                 String title) {
        if (systemProps == null || extraTerms == null)
            return;
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = extraTerms.iterator(); it.hasNext();) {
            buffer.append(it.next());
            if (it.hasNext())
                buffer.append(",");
        }
        systemProps.setProperty(title, buffer.toString());
    }
    
    private List loadExtraTerms(String propKey) {
        List extraTerms = new ArrayList();
        if (systemProps == null)
            return extraTerms;
        String extraTermStr = systemProps.getProperty(propKey);
        if (extraTermStr == null ||
            extraTermStr.length() == 0)
            return extraTerms;
        StringTokenizer tokens = new StringTokenizer(extraTermStr,
                                                     ",");
        while (tokens.hasMoreTokens())
            extraTerms.add(tokens.nextToken());
        return extraTerms;
    }
    
    public void ensureNewTaxon(String newTaxon) {
        // Check if this taxon has been in the list already
        if (taxons.contains(newTaxon) ||
            extraTaxons.contains(newTaxon))
            return;
        extraTaxons.add(newTaxon);
        // Need to put this list into the properties so that it can be listed
        storeExtraTerms(extraTaxons, 
                        EXTRA_TAXON_KEY);
    }
    
    public void ensureNewLocalization(String newLocalization) {
        if (localizations.contains(newLocalization) ||
            extraLocalizations.contains(newLocalization))
            return;
        extraLocalizations.add(newLocalization);
        storeExtraTerms(extraLocalizations, 
                        EXTRA_LOCALIZATION_KEY);
        insertToModel(newLocalization,
                      localizationModel,
                      1);
    }
    
    private void initModel(List data,
                           List extraTerms,
                           DefaultComboBoxModel model,
                           int startIndexForExtra) {
        // First element is null always
        model.addElement(null);
        for (Iterator it = data.iterator(); it.hasNext();) {
            model.addElement(it.next());
        }
        for (Iterator it = extraTerms.iterator(); it.hasNext();) {
            String more = (String) it.next();
            insertToModel(more, model, startIndexForExtra);
        }
    }
    
    private void insertToModel(String term,
                               DefaultComboBoxModel model,
                               int startIndex) {
        // Need to find the index
        int insertIndex = -1;
        // First is null, the second is reserved for human
        for (int i = startIndex; i < model.getSize(); i++) {
            String tmp = (String) model.getElementAt(i);
            if (tmp == null)
                continue;
            if (tmp.compareTo(term) > 0) {
                insertIndex = i;
                break;
            }
        }
        if (insertIndex == -1)
            insertIndex = model.getSize();
        model.insertElementAt(term, insertIndex);
    }
	
	private List<String> loadTaxons() {
		List<String> taxons = new ArrayList<String>();
		try {
			InputStream is = AuthorToolAppletUtilities.getResourceAsStream("Taxons.xml");
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document doc = builder.build(is);
			org.jdom.Element root = doc.getRootElement();
			List list = root.getChildren("Taxon");
			for (Iterator it = list.iterator(); it.hasNext();) {
				org.jdom.Element elm = (org.jdom.Element) it.next();
				String name = elm.getAttributeValue("name");
				if (name.length() > 0)
					taxons.add(name);
			}
		}
		catch(Exception e) {
			System.err.println("PropertyManager.loadTaxons(): " + e);
		}
		return taxons;
	}
	
	public java.util.List getReferenceDBs() {
		if (referenceDBs == null)
			referenceDBs = loadReferenceDBs();
		return referenceDBs;
	}	
	
	private java.util.List loadReferenceDBs() {
	    java.util.List referenceDatabases = new ArrayList();
	    try {
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = dbf.newDocumentBuilder();
            InputStream is = AuthorToolAppletUtilities.getResourceAsStream("ReferenceDatabases.xml");
            Document doc = builder.parse(is);
	        Element root = doc.getDocumentElement();
	        NodeList list = root.getElementsByTagName("ReferenceDatabase");
	        for (int i = 0; i < list.getLength(); i++) {
	            Element elm = (Element) list.item(i);
	            String attribute = elm.getAttribute("name");
	            if (attribute.equals("Reactome")) {
                    // Escape. Just an easy place to place the URL for Reactome.
                    // However, the database itself should not be used.
	                continue;
	            }
	            ReferenceDatabase database = new ReferenceDatabase();
	            if (attribute.length() > 0)
	                database.setName(attribute);
	            attribute = elm.getAttribute("queryURL");
	            if (attribute.length() > 0)
	                database.setQueryURL(attribute);
	            attribute = elm.getAttribute("accessURL");
	            if (attribute.length() > 0)
	                database.setAccessURL(attribute);
	            referenceDatabases.add(database);
	        }
	    }
	    catch(Exception e) {
	        System.err.println("PropertyManager.loadReferenceDBs(): " + e);
	    }
	    return referenceDatabases;
	}
	
	private java.util.List loadLocalizations() {
		java.util.List localizations = new ArrayList();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
            InputStream is = AuthorToolAppletUtilities.getResourceAsStream("Localizations.xml");
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList list = root.getElementsByTagName("Localization");
			for (int i = 0; i < list.getLength(); i++) {
				Element elm = (Element) list.item(i);
				String name = elm.getAttribute("name");
				if (name.length() > 0)
					localizations.add(name);
			}
		}
		catch(Exception e) {
			System.err.println("PropertyManager.loadLocalizations(): " + e);
		}
		return localizations;
	}
	
	public java.util.List getTimings() {
		if (timings == null)
			timings = loadTimings();
		return timings;
	}
    
    private java.util.List loadTimings() {
		java.util.List timings = new ArrayList();
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
            InputStream is = AuthorToolAppletUtilities.getResourceAsStream("Timings.xml");
			Document doc = builder.parse(is);
			Element root = doc.getDocumentElement();
			NodeList list = root.getElementsByTagName("Timing");
			for (int i = 0; i < list.getLength(); i++) {
				Element elm = (Element) list.item(i);
				String name = elm.getAttribute("name");
				if (name.length() > 0)
					timings.add(name);
			}
		}
		catch(Exception e) {
			System.err.println("PropertyManager.loadTimings(): " + e);
		}
		return timings;
	}
	
	public java.util.List getEntities() {
		return loadNames("SimpleEntity.txt");
	}
	
	public java.util.List getComplexes() {
		return loadNames("Complex.txt");
	}
	
	public java.util.List getReactions() {
		return loadNames("Reaction.txt");
	}
	
	public java.util.List getPathways() {
		return loadNames("Pathway.txt");
	}
	
	private java.util.List loadNames(String fileName) {
		java.util.List entities = new ArrayList();
//		try {
//			FileReader fr = new FileReader("resources" + File.separator + fileName);
//			BufferedReader bfr = new BufferedReader(fr);
//			String line = null;
//			while ((line = bfr.readLine()) != null)
//				entities.add(line);
//			fr.close();
//			bfr.close();
//		}
//		catch(IOException e) {
//			System.err.println("PropertyManager.loadNames(): " + e);
//			e.printStackTrace();
//		}
		return entities;
	}
	

}
