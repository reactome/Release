/*
 * Created on Jun 5, 2012
 *
 */
package org.reactome.core.controller;

import org.gk.model.GKInstance;
import org.gk.schema.SchemaClass;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Some attributes in converted RESTful model classes cannot be mapped simply based on
 * Reflection, and should be handled class by class. This factory class is used to manage
 * these classes for doing this class by class mapping after generic mapping.
 * @author gwu
 *
 */
public class ReactomeModelPostMapperFactory {
    
    private Map<String, ReactomeModelPostMapper> mappers;
    
    public ReactomeModelPostMapperFactory() {
        mappers = new HashMap<String, ReactomeModelPostMapper>();
    }
    
    public ReactomeModelPostMapper getPostMapper(GKInstance instance) {
        // Search for mapper. The lowest mapper should be used.
        ReactomeModelPostMapper mapper = mappers.get(instance.getSchemClass().getName());
        if (mapper != null)
            return mapper;
        List<SchemaClass> classes = (List<SchemaClass>) instance.getSchemClass().getOrderedAncestors();
        for (SchemaClass cls : classes) {
            mapper = mappers.get(cls.getName());
            if (mapper != null)
                return mapper;
        }
        return null;
    }
    
    /**
     * Set up configuration by providing a configuration file name. The configuration file
     * is an XML and used to configure post-map classes.
     * @param confFileName
     * @throws java.io.IOException
     */
    @SuppressWarnings("unchecked")
    public void setConfiguration(String confFileName) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        InputStream url = getClass().getClassLoader().getResourceAsStream(confFileName);
        Document document = builder.build(url);
        Element root = document.getRootElement();
        List<Element> children = root.getChildren("PostMapper");
        for (Element mapper : children) {
            String name = mapper.getAttributeValue("class");
            String mapperClsName = mapper.getAttributeValue("mapper");
            Class<ReactomeModelPostMapper> cls = (Class<ReactomeModelPostMapper>) Class.forName(mapperClsName);
            ReactomeModelPostMapper instance = cls.newInstance();
            instance.setClassName(name);
            mappers.put(name, instance);
        }
    }
    
}
