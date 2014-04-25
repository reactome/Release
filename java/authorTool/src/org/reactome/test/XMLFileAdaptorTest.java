/*
 * Created on Jan 11, 2005
 *
 */
package org.reactome.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.gk.persistence.GKBReader;
import org.gk.persistence.Project;
import org.gk.persistence.XMLFileAdaptor;

/**
 * 
 * @author wgm
 */
public class XMLFileAdaptorTest extends TestCase {
    private final String fileName = "/home/wgm/gkteam/wgm/oneFile.xml";
    
    public void testObjectSerialization() throws Exception {
        String file = "/Users/guanming/Documents/gkteam/Apoptosis.gkb";
        GKBReader persistence = new GKBReader();
        Project project = persistence.open(file);
        String out = "test.obj";
        File temp = new File(out);
        FileOutputStream fos = new FileOutputStream(temp);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(project);
        oos.close();
        fos.close();
        FileInputStream fis = new FileInputStream(temp);
        ObjectInputStream ois = new ObjectInputStream(fis);
        project = (Project) ois.readObject();
        ois.close();
        fis.close();
        System.out.println("Project: " + project.getProcess().getDisplayName());
    }
    
    public void testLoad() {
        try {
            System.out.println("----- Before loading -----");
            memoryUsage();
            XMLFileAdaptor adaptor = new XMLFileAdaptor(fileName);
            Collection instances = adaptor.fetchInstancesByClass("GenericReaction");
            System.out.println("GenericReaction: " + instances.size());
            System.out.println("----- After loading -----");
            memoryUsage();
        }
        catch(Exception e) {
            System.err.println("XMLFileAdaptorTest.testLoad(): " + e);
            e.printStackTrace();
        }
    }
    
    public void testREGEXP() {
        Pattern pattern = Pattern.compile("^Formation");
        String value = "Formation of Complex";
        Matcher matcher = pattern.matcher(value);
        assertTrue(matcher.find());
     }
    
    private void memoryUsage() {
        System.out.println("Total memory: " + Runtime.getRuntime().totalMemory());
        System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
        System.out.println("Used memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }
    
}
