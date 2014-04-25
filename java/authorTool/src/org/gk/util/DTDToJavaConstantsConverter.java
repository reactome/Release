/*
 * Created on Jun 18, 2009
 *
 */
package org.gk.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Create a Java source code based on a DTD file. This class should be moved to a generic package in the future.
 * @author wgm
 *
 */
public class DTDToJavaConstantsConverter {
    private String packageName;
    private String className;
    private String targetDir;
    
    public DTDToJavaConstantsConverter() {
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTargetDir() {
        return targetDir;
    }

    public void setTargetDir(String targetDir) {
        this.targetDir = targetDir;
    }
    
    public void convert(String dtdFileName) throws Exception {
        FileUtilities fu = new FileUtilities();
        fu.setInput(dtdFileName);
        String line = null;
        Set<String> names = new HashSet<String>();
        while ((line = fu.readLine()) != null) {
            if (line.startsWith("<!ELEMENT")) {
                // Element name
                String[] tokens = line.split(" ");
                names.add(tokens[1]);
            }
            else if (line.startsWith("<!ATTLIST")) {
                // Attribute name
                String[] tokens = line.split(" ");
                names.add(tokens[2]);
            }
        }
        fu.close();
        System.out.println("Total names: " + names.size());
        output(names);
    }

    protected void output(Set<String> names) throws IOException {
        FileUtilities fu = new FileUtilities();
        // Need to generate a Java file
        // Print package
        String outFileName = targetDir + className + ".java";
        fu.setOutput(outFileName);
        fu.printLine("package " + packageName + ";");
        fu.printLine("");
        fu.printLine("public class " + className + " {\n");
        for (String name : names) {
            // have to make sure name is a valid Java token
            String variable = name;
            variable = variable.replaceAll("-", "_");
            fu.printLine("    public static final String " + variable + " = \"" + name + "\";");
        }
        fu.printLine("}\n");
        fu.close();
    }
    
    @Test
    public void runConvert() throws Exception {
        setClassName("BioSystemsConstants");
        setPackageName("org.gk.biosystems");
        setTargetDir("src/org/gk/biosystems/");
        String dtdFileName = "../../BioSystems/rssm.dtd";
        convert(dtdFileName);
    }
    
}
