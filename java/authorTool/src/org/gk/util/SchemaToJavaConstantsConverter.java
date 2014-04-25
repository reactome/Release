/*
 * Created on Oct 19, 2009
 *
 */
package org.gk.util;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

/**
 * This class is used to extract Java constants from a XML Schema so that it can be used in Java
 * coding as Java constants.
 * @author wgm
 *
 */
public class SchemaToJavaConstantsConverter extends DTDToJavaConstantsConverter {
    
    public SchemaToJavaConstantsConverter() {
    }

    @Override
    public void convert(String schemaFileName) throws Exception {
        Set<String> names = new HashSet<String>();
        File file = new File(schemaFileName);
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        Element root = document.getRootElement();
        extractNames(root, names);
        System.out.println("Total names: " + names.size());
        for (String name : names)
            System.out.println(name);
        output(names);
    }
    
    @SuppressWarnings("unchecked")
    private void extractNames(Element elm, Set<String> names) {
        String name = elm.getAttributeValue("name");
        if (name != null && name.length() > 0)
            names.add(name);
        // Want to get type from enumeration since they are constant
        if (elm.getName().equals("enumeration")) {
            String value = elm.getAttributeValue("value");
            if (value != null && value.length() > 0)
                names.add(value);
        }
        List children = elm.getChildren();
        if (children != null && children.size() > 0) {
            for (Iterator it = children.iterator(); it.hasNext();) {
                Element child = (Element) it.next();
                extractNames(child, names);
            }
        }
    }
    
    @Test
    public void testOutput() throws Exception {
        setClassName("GPMLConstants");
        setPackageName("org.gk.gpml");
        setTargetDir("src/org/gk/gpml/");
        String dtdFileName = "../../wikipathways/GPML.xsd";
        convert(dtdFileName);
    }
    
}
