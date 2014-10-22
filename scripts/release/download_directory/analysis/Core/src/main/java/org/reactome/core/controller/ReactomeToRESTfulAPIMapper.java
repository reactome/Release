/*
 * Created on Oct 5, 2005
 *
 */
package org.reactome.core.controller;

import org.gk.schema.SchemaClass;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.*;

/**
 * This class is used as a mapper from the Reactome schema classes and attributes
 * to caBIO org.reactome.restfulapi.domain class and fields.
 *
 * @author guanming
 */
public class ReactomeToRESTfulAPIMapper {
    private Map<String, ClassMap> clsMaps;
    // Use this map to increate the performance
    private Map<SchemaClass, ClassMap> cachedMap;

    public ReactomeToRESTfulAPIMapper() {
    }

    public void setMapFileName(String fileName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        processMapResource(is);
    }

    public void setMapFile(File file) {
        try {
            processMapResource(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println("ReactomeToCaBIOMapper.setMapFile(): " + e);
            e.printStackTrace();
        }
    }

    private void processMapResource(InputStream is) {
        clsMaps = new HashMap<String, ClassMap>();
        cachedMap = new HashMap<SchemaClass, ClassMap>();
        try {
            SAXBuilder saxbuilder = new SAXBuilder();
            Document document = saxbuilder.build(is);
            Element root = document.getRootElement();
            String defaultPackageName = root.getAttributeValue("package");
            List classMaps = root.getChildren("class");
            Element elm = null;
            for (Iterator it = classMaps.iterator(); it.hasNext(); ) {
                elm = (Element) it.next();
                processMapElement(elm, defaultPackageName);
            }
        } catch (JDOMException e) {
            System.err.println("ReactomeToCaBIOMapper.processMapResource(): " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("ReactomeToCaBIOMapper.processMapResource(): " + e);
            e.printStackTrace();
        }
    }

    private void processMapElement(Element mapElm, String defaultPackageName) {
        String clsFrom = mapElm.getAttributeValue("from");
        String clsTo = mapElm.getAttributeValue("to");
        if (!clsTo.contains(".")) {
            // Need the default package name
            clsTo = defaultPackageName + "." + clsTo;
        }
        String id = mapElm.getAttributeValue("id");
        if (id == null)
            id = clsFrom + "->" + clsTo;
        ClassMap map = clsMaps.get(id);
        if (map == null) {
            map = new ClassMap(id);
            clsMaps.put(id, map);
        }
        map.setClassFrom(clsFrom);
        map.setClassTo(clsTo);
        // Check if there is a superClass
        String superMapId = mapElm.getAttributeValue("super");
        if (superMapId != null) {
            ClassMap superMap = clsMaps.get(superMapId);
            if (superMap == null) {
                superMap = new ClassMap(superMapId);
                clsMaps.put(superMapId, superMap);
            }
            map.setSuperMap(superMap);
        }
        // Property map
        List properties = mapElm.getChildren("property");
        if (properties == null || properties.size() == 0)
            return;
        Element propElm = null;
        String propFrom = null;
        String propTo = null;
        String mapper = null;
        for (Iterator it = properties.iterator(); it.hasNext(); ) {
            propElm = (Element) it.next();
            propFrom = propElm.getAttributeValue("from");
            propTo = propElm.getAttributeValue("to");
            mapper = propElm.getAttributeValue("mapper");
            if (propTo == null)
                continue; // propTo is required
            if (propFrom != null)
                map.addPropMap(propFrom, propTo);
            else if (mapper != null)
                map.addCustomizableMapper(propTo, mapper);
        }
    }

    public Object mapPropFromCaBioToReactome(Class caBioCls,
                                             String caBioPropName,
                                             SchemaClass reactomeSrcCls) {
        ClassMap map = searchMap(reactomeSrcCls);
        if (map == null)
            return null;
        ClassMap superMap = map;
        while (superMap != null) {
            String propName = superMap.getReactomeProp(caBioPropName);
            if (propName != null)
                return propName;
            CustomizableMapper mapper = superMap.getCustomizableMap(caBioPropName);
            if (mapper != null)
                return mapper;
            superMap = superMap.getSuperMap();
        }
        return null;
    }

    public String mapClsFromReactomeToCaBIO(SchemaClass reactomeCls) {
        ClassMap clsMap = searchMap(reactomeCls);
        if (clsMap != null)
            return clsMap.getClassTo();
        return null;
    }

    public List<String> mapClsFromCaBioToReactome(String caBioClsName) {
        Collection<ClassMap> maps = clsMaps.values();
        List<String> rtn = new ArrayList<String>();
        for (ClassMap map : maps) {
            if (map.getClassTo().equals(caBioClsName))
                rtn.add(map.getClassFrom());
        }
        return rtn;
    }

    @SuppressWarnings({"unchecked"})
    private ClassMap searchMap(SchemaClass schemaCls) {
        ClassMap rtn = cachedMap.get(schemaCls);
        if (rtn != null)
            return rtn;
        List superClsList = schemaCls.getOrderedAncestors();
        List<SchemaClass> copy = new ArrayList<SchemaClass>(superClsList);
        copy.add(schemaCls);
        for (int i = copy.size() - 1; i >= 0; i--) {
            SchemaClass cls = (SchemaClass) copy.get(i);
            ClassMap map = searchMap(cls.getName());
            if (map != null) {
                rtn = map;
                break;
            }
        }
        cachedMap.put(schemaCls, rtn);
        return rtn;
    }

    private ClassMap searchMap(String schemaClsName) {
        ClassMap map = null;
        Set<String> keys = clsMaps.keySet();
        for (String key : keys) {
            map = clsMaps.get(key);
            if (map.getClassFrom().equals(schemaClsName))
                return map;
        }
        return null;
    }

    class ClassMap {
        private ClassMap superMap;
        private String id;
        private Map<String, String> propMap;
        private Map<String, CustomizableMapper> customizablePropMap;
        private String clsFrom;
        private String clsTo;

        public ClassMap(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public void setClassFrom(String clsFrom) {
            this.clsFrom = clsFrom;
        }

        public String getClassFrom() {
            return this.clsFrom;
        }

        public void setClassTo(String clsTo) {
            this.clsTo = clsTo;
        }

        public String getClassTo() {
            return this.clsTo;
        }

        public void setSuperMap(ClassMap map) {
            this.superMap = map;
        }

        public ClassMap getSuperMap() {
            return this.superMap;
        }

        public void addPropMap(String propFrom, String propTo) {
            if (propMap == null)
                propMap = new HashMap<String, String>();
            propMap.put(propFrom, propTo);
        }

        public String getReactomeProp(String caBioProp) {
            if (propMap == null)
                return null;
            Set<String> keys = propMap.keySet();
            for (String key : keys) {
                String value = propMap.get(key);
                if (value.equals(caBioProp))
                    return key;
            }
            return null;
        }

        public void addCustomizableMapper(String propTo, String mapperName) {
            if (customizablePropMap == null)
                customizablePropMap = new HashMap<String, CustomizableMapper>();
            // Try to generate a mapper
            try {
                CustomizableMapper mapper = (CustomizableMapper) Class.forName(mapperName).newInstance();
                customizablePropMap.put(propTo, mapper);
            } catch (Exception e) {
                System.err.println("ReactomeToCaBioMapper.addCustomizableMapper(): " + e);
                e.printStackTrace();
            }
        }

        public CustomizableMapper getCustomizableMap(String propTo) {
            if (customizablePropMap == null)
                return null;
            CustomizableMapper mapper = customizablePropMap.get(propTo);
            return mapper;
        }
    }
}
